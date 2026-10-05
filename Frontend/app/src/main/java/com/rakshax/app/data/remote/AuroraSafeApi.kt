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

class AuroraSafeApi(context: Context) {
    private val secureStore = SecureStore(context)
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .addInterceptor(Interceptor { chain ->
            val token = secureStore.getString(TOKEN_KEY)
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .apply { if (!token.isNullOrBlank()) header("Authorization", "Bearer $token") }
                .build()
            chain.proceed(request)
        })
        .build()
    private val baseUrl = BuildConfig.AURORA_SAFE_BASE_URL.trimEnd('/')

    suspend fun login(email: String, password: String): ApiResult<AuthSession> = request(
        method = "POST",
        path = "/auth/login",
        body = JSONObject().apply {
            put("email", email)
            put("password", password)
        }
    ) { json ->
        AuthSession(
            token = json.getString("access_token"),
            userId = json.optJSONObject("user")?.optString("id"),
            username = json.optJSONObject("user")?.optString("username"),
            email = json.optJSONObject("user")?.optString("email")
        ).also { secureStore.putString(TOKEN_KEY, it.token) }
    }

    suspend fun loginWithFirebase(firebaseIdToken: String): ApiResult<AuthSession> = request(
        method = "POST",
        path = "/auth/google",
        body = JSONObject().apply { put("id_token", firebaseIdToken) }
    ) { json ->
        AuthSession(
            token = json.getString("access_token"),
            userId = json.optJSONObject("user")?.optString("id"),
            username = json.optJSONObject("user")?.optString("username"),
            email = json.optJSONObject("user")?.optString("email")
        ).also { secureStore.putString(TOKEN_KEY, it.token) }
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

    fun clearSession() = secureStore.putString(TOKEN_KEY, null)

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

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        const val TOKEN_KEY = "aurora_access_token"
    }
}
