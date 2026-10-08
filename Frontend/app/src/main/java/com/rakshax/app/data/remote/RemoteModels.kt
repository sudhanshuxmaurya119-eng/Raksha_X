package com.rakshax.app.data.remote

data class ApiResult<T>(
    val value: T? = null,
    val error: String? = null,
    val statusCode: Int? = null
) {
    val isSuccess: Boolean get() = error == null && value != null
}

data class AuthSession(
    val token: String,
    val userId: String?,
    val username: String?,
    val email: String?,
    val phone: String?,
    val age: Int?
)

data class RemoteSosResult(
    val eventId: String?,
    val status: String,
    val nearbyNgoCount: Int
)

data class RemoteSosStatus(
    val status: String,
    val acknowledgedContact: String?
)

data class RemoteIncident(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val description: String,
    val incidentType: String,
    val severity: Int,
    val zone: String,
    val address: String?,
    val createdAt: String?
)

data class RemoteRiskScore(
    val score: Float,
    val confidence: Float,
    val zone: String,
    val nearbyIncidents: Int,
    val message: String
)

data class RemoteHotspot(
    val id: String,
    val name: String,
    val riskLevel: String,
    val radiusKm: Float,
    val incidentCount: Int,
    val latitude: Double,
    val longitude: Double
)

data class RoutePoint(val latitude: Double, val longitude: Double)

data class RemoteSafeRoute(
    val waypoints: List<RoutePoint>,
    val distanceKm: Float,
    val estimatedTimeMinutes: Int,
    val safetyScore: Float,
    val avoidedZones: Int,
    val routeType: String
)
