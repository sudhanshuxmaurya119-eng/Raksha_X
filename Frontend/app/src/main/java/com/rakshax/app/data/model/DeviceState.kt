package com.rakshax.app.data.model

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

data class DeviceState(
    val name: String = "RakshaX-SOS-01",
    val macAddress: String = "Not paired",
    val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val batteryLevel: Int = 0,
    val signalStrengthRssi: Int = 0,
    val firmwareVersion: String = "v1.2.0-esp32c3",
    val buttonGpio: String = "GPIO4 (Held 2s)",
    val lastError: String? = null
)
