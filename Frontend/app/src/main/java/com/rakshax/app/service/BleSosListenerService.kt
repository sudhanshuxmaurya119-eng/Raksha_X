package com.rakshax.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshax.app.MainActivity
import com.rakshax.app.data.ble.BleRepository
import com.rakshax.app.data.ble.BleServicePreferences
import com.rakshax.app.data.location.LocationRepository
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class BleSosListenerService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var locationRepository: LocationRepository
    private lateinit var bleRepository: BleRepository
    private lateinit var auroraSafeRepository: AuroraSafeRepository
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        locationRepository = LocationRepository(applicationContext)
        auroraSafeRepository = AuroraSafeRepository(applicationContext)
        bleRepository = BleRepository(
            context = applicationContext,
            notifyBackgroundService = false,
            onSosSignal = ::handleSosSignal
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            BleServicePreferences.setEnabled(applicationContext, false)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startAsForegroundService()) return START_NOT_STICKY
        if (intent?.action == ACTION_CONNECT || intent?.action == ACTION_START || intent == null) {
            BleServicePreferences.deviceAddress(applicationContext)?.let(bleRepository::connect)
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // START_STICKY and the ongoing notification keep the listener eligible for restart.
    }

    override fun onDestroy() {
        releaseWakeLock()
        bleRepository.close()
        locationRepository.stopUpdates()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun handleSosSignal(source: String) {
        acquireWakeLock()
        updateNotification("Emergency signal received. Capturing location…")
        serviceScope.launch {
            try {
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
                SosEscalationService.start(applicationContext)
                updateNotification(
                    if (location == null) {
                        "SOS received. Location fix unavailable."
                    } else {
                        "SOS active. Location captured."
                    }
                )
            } finally {
                releaseWakeLock()
            }
        }
    }

    private fun startAsForegroundService(): Boolean {
        val notification = buildNotification("Listening for the RakshaX button")
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var foregroundTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                if (locationRepository.hasPermission()) {
                    foregroundTypes = foregroundTypes or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                }
                startForeground(NOTIFICATION_ID, notification, foregroundTypes)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        }.getOrElse {
            BleServicePreferences.setEnabled(applicationContext, false)
            stopSelf()
            false
        }
    }

    private fun buildNotification(message: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BleSosListenerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RakshaX background protection")
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Background SOS listener",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the RakshaX ESP32 button listener available when the screen is locked"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(PowerManager::class.java)
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "$packageName:BleSosListener"
            )?.apply { setReferenceCounted(false) }
        }
        if (wakeLock?.isHeld != true) wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
    }

    companion object {
        private const val CHANNEL_ID = "rakshax_ble_listener"
        private const val NOTIFICATION_ID = 401
        private const val WAKE_LOCK_TIMEOUT_MS = 15_000L
        const val ACTION_START = "com.rakshax.app.action.START_BLE_LISTENER"
        const val ACTION_CONNECT = "com.rakshax.app.action.CONNECT_BLE_LISTENER"
        const val ACTION_STOP = "com.rakshax.app.action.STOP_BLE_LISTENER"

        fun start(context: Context, action: String = ACTION_START) {
            val intent = Intent(context, BleSosListenerService::class.java).setAction(action)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BleSosListenerService::class.java))
        }

        fun refreshDevice(context: Context) {
            if (BleServicePreferences.isEnabled(context)) start(context, ACTION_CONNECT)
        }
    }
}
