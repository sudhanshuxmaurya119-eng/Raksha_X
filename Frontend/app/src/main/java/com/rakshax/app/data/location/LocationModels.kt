package com.rakshax.app.data.location

enum class LocationStatus {
    PERMISSION_REQUIRED,
    GPS_DISABLED,
    SEARCHING,
    READY,
    POOR_ACCURACY,
    UNAVAILABLE
}

data class SosLocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

data class LocationState(
    val status: LocationStatus = LocationStatus.PERMISSION_REQUIRED,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val timestampMillis: Long? = null,
    val message: String = "Location permission is required"
)
