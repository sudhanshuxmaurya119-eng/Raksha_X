package com.rakshax.app.data.model

data class SosState(
    val isActive: Boolean = false,
    val triggerSource: String = "Physical ESP32 Button",
    val buttonDetected: Boolean = true,
    val locationFound: Boolean = true,
    val alertSent: Boolean = true,
    val latitude: Double = 28.6139,
    val longitude: Double = 77.2090,
    val locationAccuracyMeters: Float? = null,
    val locationTimestampMillis: Long? = null,
    val locationAddress: String = "Connaught Place, Central Delhi",
    val timestamp: String = "Just now",
    val acknowledgedContactName: String? = "Rahul Sharma (Brother)",
    val calledContactName: String? = null,
    val pendingContactsCount: Int = 1,
    val escalationStage: Int = 1,
    val backendStatus: String = "DEMO",
    val backendEventId: String? = null,
    val backendError: String? = null,
    val escalationDeadlineMillis: Long? = null
)
