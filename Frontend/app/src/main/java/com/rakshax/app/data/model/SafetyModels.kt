package com.rakshax.app.data.model

data class RiskScoreInfo(
    val score: Float = 0.74f,
    val zone: String = "YELLOW", // GREEN, YELLOW, RED
    val confidence: Float = 0.82f,
    val nearbyIncidentsCount: Int = 4,
    val nearestHotspotDistanceKm: Float = 0.6f,
    val message: String = "Moderate risk zone in Delhi NCR. Exercise heightened situational awareness."
)

data class IncidentMarker(
    val id: String,
    val title: String,
    val incidentType: String,
    val severity: Int,
    val zone: String,
    val latitude: Double,
    val longitude: Double,
    val timeAgo: String,
    val address: String
)

data class HotspotZone(
    val id: String,
    val name: String,
    val riskLevel: String,
    val radiusKm: Float,
    val incidentCount: Int,
    val latitude: Double,
    val longitude: Double
)
