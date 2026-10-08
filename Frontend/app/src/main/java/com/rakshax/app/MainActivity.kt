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

    private val callPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
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
                val contacts = MockDataRepository.contacts.value
                    .filter { it.isEnabled }
                    .map { it.name to it.phone }
                val result = auroraSafeRepository.triggerSos(
                    location = location,
                    locationText = location?.let { "${it.latitude}, ${it.longitude}" } ?: "Location unavailable",
                    contacts = contacts,
                    userId = MockDataRepository.currentUser.value.id
                )
                MockDataRepository.triggerSos(
                    source = source,
                    location = location,
                    backendStatus = if (result.isSuccess) "SENT" else "QUEUED",
                    backendEventId = result.value?.eventId,
                    backendError = result.error
                )
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
