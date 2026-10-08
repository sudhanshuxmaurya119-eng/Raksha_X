package com.rakshax.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rakshax.app.data.ble.BleRepository
import com.rakshax.app.data.ble.BleServicePreferences
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.network.NetworkStatusMonitor
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.data.sos.EmergencyCallHelper
import com.rakshax.app.notification.FcmRegistration
import com.rakshax.app.service.BleSosListenerService
import com.rakshax.app.service.SosEscalationService
import com.rakshax.app.ui.navigation.RakshaXNavGraph
import com.rakshax.app.ui.theme.BackgroundDark
import com.rakshax.app.ui.theme.RakshaXTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var locationRepository: LocationRepository
    private lateinit var bleRepository: BleRepository
    private lateinit var auroraSafeRepository: AuroraSafeRepository
    private lateinit var networkStatusMonitor: NetworkStatusMonitor

    private val emergencyPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permission results handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val missingPermissions = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.SEND_SMS)
        }
        if (missingPermissions.isNotEmpty()) {
            emergencyPermissionsLauncher.launch(missingPermissions.toTypedArray())
        }
        com.rakshax.app.ui.theme.ThemeManager.init(applicationContext)
        locationRepository = LocationRepository(applicationContext)
        auroraSafeRepository = AuroraSafeRepository(applicationContext)
        auroraSafeRepository.restoreSession()?.let { session ->
            MockDataRepository.setCurrentUser(session)
            FcmRegistration.registerCurrentDevice(applicationContext)
        }
        lifecycleScope.launch {
            val result = auroraSafeRepository.fetchCurrentUser()
            result.value?.let { session ->
                MockDataRepository.setCurrentUser(session)
                FcmRegistration.registerCurrentDevice(applicationContext)
            }
        }
        networkStatusMonitor = NetworkStatusMonitor(applicationContext)
        lifecycleScope.launch {
            networkStatusMonitor.isOnline.collect { online ->
                    if (online) auroraSafeRepository.flushPendingActions()
                }
        }
        bleRepository = BleRepository(applicationContext) { source ->
            EmergencyCallHelper.initiateEmergencyCall(this@MainActivity)
            lifecycleScope.launch {
                val location = locationRepository.getCurrentLocation()
                val allContacts = MockDataRepository.contacts.value.filter { it.isEnabled }
                val contacts = allContacts.map { it.name to it.phone }
                val locationText = location?.let { "${it.latitude}, ${it.longitude}" } ?: "Location unavailable"
                val result = auroraSafeRepository.triggerSos(
                    location = location,
                    locationText = locationText,
                    contacts = contacts,
                    userId = MockDataRepository.currentUser.value.id
                )
                val effectiveEventId = result.value?.eventId ?: "sos_${System.currentTimeMillis()}"
                MockDataRepository.triggerSos(
                    source = source,
                    location = location,
                    backendStatus = if (result.isSuccess) "SENT" else "QUEUED",
                    backendEventId = result.value?.eventId,
                    backendError = result.error
                )
                // Only SMS priority-1 contact initially; after 45s escalation handles the rest
                val priority1Contact = allContacts.minByOrNull { it.priority }
                if (priority1Contact != null) {
                    com.rakshax.app.data.sos.EmergencySmsHelper.sendEmergencySms(
                        context = applicationContext,
                        eventId = effectiveEventId,
                        locationText = locationText,
                        targetContacts = listOf(priority1Contact)
                    )
                }
                SosEscalationService.start(this@MainActivity)
            }
        }
        if (BleServicePreferences.isEnabled(this)) {
            BleSosListenerService.start(this)
        }
        setContent {
            RakshaXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    RakshaXNavGraph(
                        locationRepository = locationRepository,
                        bleRepository = bleRepository,
                        auroraSafeRepository = auroraSafeRepository,
                        networkStatusMonitor = networkStatusMonitor
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        bleRepository.close()
        locationRepository.stopUpdates()
        networkStatusMonitor.close()
        super.onDestroy()
    }
}
