package com.rakshax.app.data.ble

import java.nio.charset.StandardCharsets

object BleProtocol {
    const val DEVICE_NAME_PREFIX = "RakshaX"
    const val SERVICE_UUID = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    const val SOS_CHARACTERISTIC_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e"

    fun isSosSignal(value: ByteArray): Boolean = value
        .toString(StandardCharsets.UTF_8)
        .trim()
        .equals("SOS", ignoreCase = true)
}
