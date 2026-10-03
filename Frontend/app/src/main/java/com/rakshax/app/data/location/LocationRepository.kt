package com.rakshax.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepository(context: Context) {
    private val appContext = context.applicationContext
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val _state = MutableStateFlow(LocationState())
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let(::publishLocation)
        }
    }

    val state: StateFlow<LocationState> = _state.asStateFlow()

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    fun refreshStatus() {
        if (!hasPermission()) {
            _state.value = _state.value.copy(
                status = LocationStatus.PERMISSION_REQUIRED,
                message = "Allow location access to capture an SOS position"
            )
            return
        }

        val gpsEnabled = runCatching {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        }.getOrDefault(false)

        if (!gpsEnabled) {
            _state.value = _state.value.copy(
                status = LocationStatus.GPS_DISABLED,
                message = "Turn on Location in system settings"
            )
        } else if (_state.value.latitude == null) {
            _state.value = _state.value.copy(
                status = LocationStatus.SEARCHING,
                message = "Waiting for a GPS fix"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startUpdates() {
        if (!hasPermission()) {
            refreshStatus()
            return
        }
        refreshStatus()
        if (_state.value.status == LocationStatus.GPS_DISABLED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L)
            .setMinUpdateIntervalMillis(5_000L)
            .setWaitForAccurateLocation(false)
            .build()

        runCatching {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }.onFailure {
            _state.value = _state.value.copy(
                status = LocationStatus.UNAVAILABLE,
                message = "Location provider is unavailable"
            )
        }
    }

    fun stopUpdates() {
        fusedClient.removeLocationUpdates(callback)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): SosLocationPayload? {
        if (!hasPermission()) {
            refreshStatus()
            return null
        }
        refreshStatus()
        if (_state.value.status == LocationStatus.GPS_DISABLED) return null

        val current = suspendCancellableCoroutine<Location?> { continuation ->
            val tokenSource = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        }

        val location = current ?: suspendCancellableCoroutine<Location?> { continuation ->
            fusedClient.lastLocation
                .addOnSuccessListener { last ->
                    if (continuation.isActive) continuation.resume(last)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }

        return location?.let {
            publishLocation(it)
            it.toPayload()
        } ?: run {
            _state.value = _state.value.copy(
                status = LocationStatus.UNAVAILABLE,
                message = "No location fix is available"
            )
            null
        }
    }

    private fun publishLocation(location: Location) {
        val status = if (location.accuracy > 100f) {
            LocationStatus.POOR_ACCURACY
        } else {
            LocationStatus.READY
        }
        _state.value = LocationState(
            status = status,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            timestampMillis = location.time,
            message = if (status == LocationStatus.READY) {
                "GPS fix captured"
            } else {
                "GPS fix is approximate (${location.accuracy.toInt()} m)"
            }
        )
    }

    private fun Location.toPayload(): SosLocationPayload = SosLocationPayload(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        timestampMillis = time
    )
}
