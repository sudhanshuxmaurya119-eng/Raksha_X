package com.rakshax.app.data.ble

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int
)
