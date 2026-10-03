package com.rakshax.app.data.ble

import android.content.Context

object BleServicePreferences {
    private const val PREFS_NAME = "rakshax_ble_service"
    private const val KEY_ENABLED = "background_monitoring_enabled"
    private const val KEY_DEVICE_ADDRESS = "device_address"

    fun isEnabled(context: Context): Boolean = preferences(context)
        .getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun deviceAddress(context: Context): String? = preferences(context)
        .getString(KEY_DEVICE_ADDRESS, null)

    fun saveDeviceAddress(context: Context, address: String) {
        preferences(context).edit().putString(KEY_DEVICE_ADDRESS, address).apply()
    }

    fun clearDeviceAddress(context: Context) {
        preferences(context).edit().remove(KEY_DEVICE_ADDRESS).apply()
    }

    private fun preferences(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
