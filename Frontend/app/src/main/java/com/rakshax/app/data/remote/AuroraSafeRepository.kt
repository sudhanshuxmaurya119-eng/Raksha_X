package com.rakshax.app.data.remote

import android.content.Context
import com.rakshax.app.data.location.SosLocationPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AuroraSafeRepository(context: Context) {
    private val api = AuroraSafeApi(context)
    private val pendingActions = PendingActionStore(context)

    suspend fun login(email: String, password: String) = api.login(email, password)

    suspend fun register(username: String, email: String, password: String, phone: String) =
        api.register(username, email, password, phone)

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
