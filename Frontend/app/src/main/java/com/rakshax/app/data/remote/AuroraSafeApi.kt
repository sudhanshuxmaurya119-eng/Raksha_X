package com.rakshax.app.data.remote

import android.content.Context
import com.rakshax.app.BuildConfig
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.secure.SecureStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.rakshax.app.data.model.FacilityType
import com.rakshax.app.data.model.MapThresholdConfig
import com.rakshax.app.data.model.RiskLevel
import com.rakshax.app.data.model.SafetyArea
import com.rakshax.app.data.model.SafetyCase
import com.rakshax.app.data.model.SafetyFacility

class AuroraSafeApi(context: Context) {
    private val secureStore = SecureStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val token = secureStore.getString(TOKEN_KEY)
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
                .build()
            chain.proceed(request)
        })
        .build()
    private val baseUrl = BuildConfig.AURORA_SAFE_BASE_URL
        .ifBlank { DEFAULT_BASE_URL }
        .trimEnd('/')

    suspend fun login(email: String, password: String): ApiResult<AuthSession> = request(
        method = "POST",
        path = "/auth/login",
        body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
    ) { json ->
        parseAuthSession(json).also(::persistSession)
    }

    suspend fun loginWithFirebase(firebaseIdToken: String): ApiResult<AuthSession> = request(
        method = "POST",
        path = "/auth/google",
        body = JSONObject().apply { put("id_token", firebaseIdToken) }
    ) { json ->
        parseAuthSession(json).also(::persistSession)
    }

    fun restoreSession(): AuthSession? = secureStore.getString(SESSION_KEY)
        ?.let { raw -> runCatching { parseStoredSession(JSONObject(raw)) }.getOrNull() }

    suspend fun fetchCurrentUser(): ApiResult<AuthSession> {
        val token = secureStore.getString(TOKEN_KEY)
        if (token.isNullOrBlank()) return ApiResult(error = "No saved session")

        val result = request(
            method = "GET",
            path = "/auth/me"
        ) { json ->
            parseUserSession(json, token).also(::persistSession)
        }
        if (result.statusCode == 401) clearSession()
        return result
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        phone: String,
        age: Int
    ): ApiResult<AuthSession> {
        val registration: ApiResult<JSONObject> = request(
            method = "POST",
            path = "/auth/register",
            body = JSONObject().apply {
                put("username", username)
                put("email", email)
                put("password", password)
                put("phone", phone)
                put("age", age)
            },
            parser = { it }
        )
        return if (registration.isSuccess) login(email, password) else {
            ApiResult(error = registration.error, statusCode = registration.statusCode)
        }
    }

    suspend fun triggerSos(
        location: SosLocationPayload?,
        locationText: String,
        contacts: List<Pair<String, String>>,
        userId: String?
    ): ApiResult<RemoteSosResult> = request(
        method = "POST",
        path = "/sos/trigger",
        body = JSONObject().apply {
            put("latitude", location?.latitude ?: JSONObject.NULL)
            put("longitude", location?.longitude ?: JSONObject.NULL)
            put("location_text", locationText)
            put("user_id", userId ?: JSONObject.NULL)
            put("contacts", JSONArray().apply {
                contacts.forEach { (name, phone) ->
                    put(JSONObject().apply { put("name", name); put("phone", phone) })
                }
            })
        }
    ) { json ->
        RemoteSosResult(
            eventId = json.optString("event_id").takeIf { it.isNotBlank() },
            status = json.optString("status", "success"),
            nearbyNgoCount = json.optJSONArray("nearby_ngos")?.length() ?: 0
        )
    }

    suspend fun acknowledgeSos(eventId: String, contactName: String): ApiResult<Boolean> = request(
        method = "POST",
        path = "/sos/$eventId/acknowledge",
        body = JSONObject().apply { put("contact_name", contactName) }
    ) { true }

    suspend fun fetchSosStatus(eventId: String): ApiResult<RemoteSosStatus> = request(
        method = "GET",
        path = "/sos/$eventId/status"
    ) { json ->
        val acknowledgements = json.optJSONArray("acknowledgements")
        RemoteSosStatus(
            status = json.optString("status", "pending"),
            acknowledgedContact = acknowledgements
                ?.takeIf { it.length() > 0 }
                ?.getJSONObject(0)
                ?.optString("contact_name")
        )
    }

    suspend fun registerPushToken(token: String, userId: String): ApiResult<Boolean> = request(
        method = "POST",
        path = "/notifications/token",
        body = JSONObject().apply {
            put("token", token)
            put("user_id", userId)
            put("platform", "android")
        }
    ) { true }

    suspend fun fetchRiskScore(latitude: Double, longitude: Double): ApiResult<RemoteRiskScore> = request(
        method = "GET",
        path = "/safety/risk-score?lat=$latitude&lng=$longitude"
    ) { json ->
        RemoteRiskScore(
            score = json.optDouble("score", 0.5).toFloat(),
            confidence = json.optDouble("confidence", 0.0).toFloat(),
            zone = json.optString("zone", "yellow").uppercase(),
            nearbyIncidents = json.optInt("nearby_incidents", 0),
            message = json.optString("message", "Risk data unavailable")
        )
    }

    suspend fun fetchIncidents(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): ApiResult<List<RemoteIncident>> = requestArray(
        method = "GET",
        path = "/incidents/?lat=$latitude&lng=$longitude&radius_km=$radiusKm&limit=50"
    ) { json -> parseIncidents(json) }

    suspend fun fetchHotspots(): ApiResult<List<RemoteHotspot>> = requestArray(
        method = "GET",
        path = "/safety/zones"
    ) { json ->
        (0 until json.length()).map { index ->
            val item = json.getJSONObject(index)
            RemoteHotspot(
                id = item.optString("id", "hotspot-$index"),
                name = item.optString("name", "Safety hotspot"),
                riskLevel = item.optString("risk_level", "yellow").uppercase(),
                radiusKm = item.optDouble("radius_km", 0.5).toFloat(),
                incidentCount = item.optInt("incident_count", 0),
                latitude = item.optDouble("latitude"),
                longitude = item.optDouble("longitude")
            )
        }
    }

    suspend fun fetchSafeRoute(
        origin: RoutePoint,
        destination: RoutePoint
    ): ApiResult<RemoteSafeRoute> = request(
        method = "POST",
        path = "/routes/safe",
        body = JSONObject().apply {
            put("origin", JSONObject().apply { put("lat", origin.latitude); put("lng", origin.longitude) })
            put("destination", JSONObject().apply { put("lat", destination.latitude); put("lng", destination.longitude) })
        }
    ) { json ->
        val waypoints = json.optJSONArray("waypoints") ?: JSONArray()
        RemoteSafeRoute(
            waypoints = (0 until waypoints.length()).map { index ->
                val point = waypoints.getJSONObject(index)
                RoutePoint(point.optDouble("lat"), point.optDouble("lng"))
            },
            distanceKm = json.optDouble("distance_km", 0.0).toFloat(),
            estimatedTimeMinutes = json.optInt("estimated_time_min", 0),
            safetyScore = json.optDouble("safety_score", 0.0).toFloat(),
            avoidedZones = json.optInt("avoided_zones", 0),
            routeType = json.optString("route_type", "fallback_safest")
        )
    }

    suspend fun fetchMapFacilities(
        type: String? = null,
        q: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double? = null
    ): ApiResult<List<SafetyFacility>> {
        val queryParams = mutableListOf<String>()
        if (!type.isNullOrBlank()) queryParams.add("facility_type=$type")
        if (!q.isNullOrBlank()) queryParams.add("q=$q")
        if (lat != null && lng != null) {
            queryParams.add("lat=$lat")
            queryParams.add("lng=$lng")
        }
        if (radiusKm != null) queryParams.add("radius_km=$radiusKm")
        val queryString = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
        return requestArray(
            method = "GET",
            path = "/map/facilities$queryString"
        ) { array ->
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                val servicesArray = obj.optJSONArray("services")
                val servicesList = if (servicesArray != null) {
                    (0 until servicesArray.length()).map { servicesArray.getString(it) }
                } else emptyList()
                SafetyFacility(
                    id = obj.optString("id", "fac-$index"),
                    name = obj.optString("name"),
                    type = FacilityType.fromString(obj.optString("facility_type")),
                    address = obj.optString("address"),
                    phone = obj.optString("phone").takeIf { it.isNotBlank() },
                    latitude = obj.optDouble("latitude"),
                    longitude = obj.optDouble("longitude"),
                    services = servicesList,
                    emergencyAvailable = obj.optBoolean("emergency_available", true),
                    verified = obj.optBoolean("verified", true),
                    area = obj.optString("area").takeIf { it.isNotBlank() },
                    distanceKm = if (obj.has("distance_km") && !obj.isNull("distance_km")) obj.getDouble("distance_km").toFloat() else null
                )
            }
        }
    }

    suspend fun fetchMapRiskAreas(timeRange: String = "all"): ApiResult<List<SafetyArea>> = requestArray(
        method = "GET",
        path = "/map/risk-areas?time_range=$timeRange"
    ) { array ->
        (0 until array.length()).map { index ->
            val obj = array.getJSONObject(index)
            SafetyArea(
                id = obj.optString("id", "zone-$index"),
                name = obj.optString("name"),
                centerLat = obj.optDouble("center_lat"),
                centerLng = obj.optDouble("center_lng"),
                radiusMeters = obj.optDouble("radius_meters", 1500.0),
                riskLevel = RiskLevel.fromString(obj.optString("risk_level", "LOW")),
                registeredCases = obj.optInt("registered_cases", 0),
                casesThisMonth = obj.optInt("cases_this_month", 0),
                topCategory = obj.optString("top_category", "Public Safety"),
                safetyScore = obj.optInt("safety_score", 85),
                nearbyPoliceCount = obj.optInt("nearby_police_count", 0),
                nearbyNgoCount = obj.optInt("nearby_ngo_count", 0),
                nearbyHospitalCount = obj.optInt("nearby_hospital_count", 0)
            )
        }
    }

    suspend fun fetchMapCases(category: String? = null, timeRange: String = "all"): ApiResult<List<SafetyCase>> {
        val queryParams = mutableListOf("time_range=$timeRange")
        if (!category.isNullOrBlank()) queryParams.add("category=$category")
        val query = "?" + queryParams.joinToString("&")
        return requestArray(
            method = "GET",
            path = "/map/cases$query"
        ) { array ->
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                SafetyCase(
                    caseId = obj.optString("case_id", "RX-$index"),
                    latitude = obj.optDouble("latitude"),
                    longitude = obj.optDouble("longitude"),
                    category = obj.optString("category", "General Safety"),
                    severity = obj.optString("severity", "Medium"),
                    status = obj.optString("status", "Verified"),
                    createdAt = obj.optString("created_at", ""),
                    area = obj.optString("area", "")
                )
            }
        }
    }

    suspend fun fetchNearbyFacilities(
        lat: Double,
        lng: Double,
        type: String? = null,
        limit: Int = 20
    ): ApiResult<List<SafetyFacility>> {
        val queryParams = mutableListOf("lat=$lat", "lng=$lng", "limit=$limit")
        if (!type.isNullOrBlank()) queryParams.add("facility_type=$type")
        val queryString = "?" + queryParams.joinToString("&")
        return requestArray(
            method = "GET",
            path = "/map/nearby$queryString"
        ) { array ->
            (0 until array.length()).map { index ->
                val obj = array.getJSONObject(index)
                val servicesArray = obj.optJSONArray("services")
                val servicesList = if (servicesArray != null) {
                    (0 until servicesArray.length()).map { servicesArray.getString(it) }
                } else emptyList()
                SafetyFacility(
                    id = obj.optString("id", "nearby-$index"),
                    name = obj.optString("name"),
                    type = FacilityType.fromString(obj.optString("facility_type")),
                    address = obj.optString("address"),
                    phone = obj.optString("phone").takeIf { it.isNotBlank() },
                    latitude = obj.optDouble("latitude"),
                    longitude = obj.optDouble("longitude"),
                    services = servicesList,
                    emergencyAvailable = obj.optBoolean("emergency_available", true),
                    verified = obj.optBoolean("verified", true),
                    area = obj.optString("area").takeIf { it.isNotBlank() },
                    distanceKm = if (obj.has("distance_km") && !obj.isNull("distance_km")) obj.getDouble("distance_km").toFloat() else null
                )
            }
        }
    }

    suspend fun fetchMapConfig(): ApiResult<MapThresholdConfig> = request(
        method = "GET",
        path = "/map/config"
    ) { json ->
        MapThresholdConfig(
            lowMaxCases = json.optInt("low_max_cases", 5),
            mediumMaxCases = json.optInt("medium_max_cases", 15),
            highMinCases = json.optInt("high_min_cases", 16)
        )
    }

    fun clearSession() {
        secureStore.putString(TOKEN_KEY, null)
        secureStore.putString(SESSION_KEY, null)
    }

    private suspend fun <T> request(
        method: String,
        path: String,
        body: JSONObject? = null,
        parser: (JSONObject) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(baseUrl + path)
            if (body != null) {
                requestBuilder.method(method, body.toString().toRequestBody(JSON_MEDIA_TYPE))
            } else {
                requestBuilder.method(method, null)
            }
            client.newCall(requestBuilder.build()).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(text).optString("detail") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Request failed (${response.code})"
                    return@withContext ApiResult(error = detail, statusCode = response.code)
                }
                ApiResult(value = parser(JSONObject(text)), statusCode = response.code)
            }
        } catch (error: Exception) {
            ApiResult(error = error.message ?: "Network unavailable")
        }
    }

    private suspend fun <T> requestArray(
        method: String,
        path: String,
        parser: (JSONArray) -> T
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(baseUrl + path)
                .method(method, null)
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching { JSONObject(text).optString("detail") }
                        .getOrNull()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Request failed (${response.code})"
                    return@withContext ApiResult(error = detail, statusCode = response.code)
                }
                ApiResult(value = parser(JSONArray(text)), statusCode = response.code)
            }
        } catch (error: Exception) {
            ApiResult(error = error.message ?: "Network unavailable")
        }
    }

    private fun parseIncidents(array: JSONArray): List<RemoteIncident> = (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        RemoteIncident(
            id = item.optString("id", "incident-$index"),
            latitude = item.optDouble("latitude"),
            longitude = item.optDouble("longitude"),
            description = item.optString("description", "Safety report"),
            incidentType = item.optString("incident_type", "other"),
            severity = item.optInt("severity", 1),
            zone = item.optString("zone", "green").uppercase(),
            address = item.optString("address").takeIf { it.isNotBlank() },
            createdAt = item.optString("created_at").takeIf { it.isNotBlank() }
        )
    }

    private fun parseAuthSession(json: JSONObject): AuthSession {
        val token = json.getString("access_token")
        val user = json.optJSONObject("user") ?: JSONObject()
        return parseUserSession(user, token)
    }

    private fun parseUserSession(user: JSONObject, token: String): AuthSession = AuthSession(
        token = token,
        userId = user.optionalString("id"),
        username = user.optionalString("username"),
        email = user.optionalString("email"),
        phone = user.optionalString("phone"),
        age = if (user.isNull("age")) null else user.optInt("age")
    )

    private fun parseStoredSession(json: JSONObject): AuthSession = AuthSession(
        token = json.getString("token"),
        userId = json.optionalString("userId"),
        username = json.optionalString("username"),
        email = json.optionalString("email"),
        phone = json.optionalString("phone"),
        age = if (json.isNull("age")) null else json.optInt("age")
    )

    private fun persistSession(session: AuthSession) {
        secureStore.putString(TOKEN_KEY, session.token)
        secureStore.putString(
            SESSION_KEY,
            JSONObject().apply {
                put("token", session.token)
                put("userId", session.userId ?: JSONObject.NULL)
                put("username", session.username ?: JSONObject.NULL)
                put("email", session.email ?: JSONObject.NULL)
                put("phone", session.phone ?: JSONObject.NULL)
                put("age", session.age ?: JSONObject.NULL)
            }.toString()
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private companion object {
        const val DEFAULT_BASE_URL = "https://rakshax-api-sudhanshu.onrender.com"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val TOKEN_KEY = "aurora_access_token"
        const val SESSION_KEY = "aurora_auth_session"
    }
}
