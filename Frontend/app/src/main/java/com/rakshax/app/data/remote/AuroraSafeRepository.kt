package com.rakshax.app.data.remote

import android.content.Context
import com.rakshax.app.data.location.SosLocationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.rakshax.app.data.model.SafetyFacility
import com.rakshax.app.data.model.SafetyArea
import com.rakshax.app.data.model.SafetyCase

class AuroraSafeRepository(context: Context) {
    private val api = AuroraSafeApi(context)
    private val pendingActions = PendingActionStore(context)

    suspend fun login(email: String, password: String) = api.login(email, password)

    suspend fun loginWithFirebase(firebaseIdToken: String) = api.loginWithFirebase(firebaseIdToken)

    fun restoreSession() = api.restoreSession()

    suspend fun fetchCurrentUser() = api.fetchCurrentUser()

    suspend fun register(username: String, email: String, password: String, phone: String, age: Int) =
        api.register(username, email, password, phone, age)

    suspend fun triggerSos(
        location: SosLocationPayload?,
        locationText: String,
        contacts: List<Pair<String, String>>,
        userId: String?
    ): ApiResult<RemoteSosResult> {
        val result = api.triggerSos(location, locationText, contacts, userId)
        if (!result.isSuccess) {
            pendingActions.enqueue(
                PendingAction(
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
                    }.toString()
                )
            )
        }
        return result
    }

    suspend fun acknowledgeSos(eventId: String, contactName: String) =
        api.acknowledgeSos(eventId, contactName)

    suspend fun fetchSosStatus(eventId: String) = api.fetchSosStatus(eventId)

    suspend fun fetchRiskScore(latitude: Double, longitude: Double) =
        api.fetchRiskScore(latitude, longitude)

    suspend fun fetchIncidents(latitude: Double, longitude: Double, radiusKm: Double) =
        api.fetchIncidents(latitude, longitude, radiusKm)

    suspend fun fetchHotspots() = api.fetchHotspots()

    suspend fun fetchSafeRoute(origin: RoutePoint, destination: RoutePoint) =
        api.fetchSafeRoute(origin, destination)

    // Safety Map caching & network access
    private val cachedFacilities = mutableListOf<SafetyFacility>()
    private val cachedRiskAreas = mutableListOf<SafetyArea>()
    private val cachedCases = mutableListOf<SafetyCase>()

    suspend fun getMapFacilities(
        type: String? = null,
        q: String? = null,
        lat: Double? = null,
        lng: Double? = null,
        radiusKm: Double? = null
    ): List<SafetyFacility> {
        val result = api.fetchMapFacilities(type, q, lat, lng, radiusKm)
        if (result.isSuccess && result.value != null) {
            cachedFacilities.clear()
            cachedFacilities.addAll(result.value)
            return result.value
        }
        return cachedFacilities
    }

    suspend fun getMapRiskAreas(timeRange: String = "all"): List<SafetyArea> {
        val result = api.fetchMapRiskAreas(timeRange)
        if (result.isSuccess && result.value != null) {
            cachedRiskAreas.clear()
            cachedRiskAreas.addAll(result.value)
            return result.value
        }
        return cachedRiskAreas
    }

    suspend fun getMapCases(category: String? = null, timeRange: String = "all"): List<SafetyCase> {
        val result = api.fetchMapCases(category, timeRange)
        if (result.isSuccess && result.value != null) {
            cachedCases.clear()
            cachedCases.addAll(result.value)
            return result.value
        }
        return cachedCases
    }

    suspend fun getNearbyFacilities(
        lat: Double,
        lng: Double,
        type: String? = null,
        limit: Int = 20
    ): List<SafetyFacility> {
        val result = api.fetchNearbyFacilities(lat, lng, type, limit)
        return if (result.isSuccess && result.value != null) result.value else cachedFacilities
    }

    suspend fun getMapConfig() = api.fetchMapConfig()

    suspend fun flushPendingActions(): Int = withContext(Dispatchers.IO) {
        var flushed = 0
        pendingActions.all().forEach { action ->
            if (action.path == "/sos/trigger") {
                val body = JSONObject(action.body)
                val result = api.triggerSos(
                    location = if (body.isNull("latitude") || body.isNull("longitude")) null else SosLocationPayload(
                        latitude = body.getDouble("latitude"),
                        longitude = body.getDouble("longitude"),
                        accuracyMeters = 0f,
                        timestampMillis = System.currentTimeMillis()
                    ),
                    locationText = body.optString("location_text"),
                    contacts = body.optJSONArray("contacts")?.let { array ->
                        (0 until array.length()).map { index ->
                            val contact = array.getJSONObject(index)
                            contact.optString("name") to contact.optString("phone")
                        }
                    } ?: emptyList(),
                    userId = body.optString("user_id").takeIf { it.isNotBlank() }
                )
                if (result.isSuccess) {
                    pendingActions.remove(action.id)
                    flushed++
                }
            }
        }
        flushed
    }

    fun clearSession() = api.clearSession()
}
