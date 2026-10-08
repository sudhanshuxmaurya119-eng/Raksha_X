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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshax.app.MainActivity
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlin.coroutines.coroutineContext
import android.app.ForegroundServiceStartNotAllowedException

/** Keeps the three-stage trusted-circle timer alive while the UI is backgrounded. */
class SosEscalationService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var escalationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!startAsForegroundService()) return START_NOT_STICKY
        if (escalationJob?.isActive != true) {
            escalationJob = serviceScope.launch { runEscalationLoop() }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        escalationJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun runEscalationLoop() {
        while (coroutineContext.isActive) {
            val state = MockDataRepository.sosState.value
            if (!state.isActive || state.acknowledgedContactName != null) break
            val deadline = state.escalationDeadlineMillis ?: break
            delay((deadline - System.currentTimeMillis()).coerceAtLeast(250L))

            val latest = MockDataRepository.sosState.value
            if (!latest.isActive || latest.acknowledgedContactName != null) break
            if (latest.escalationDeadlineMillis != deadline) continue

            MockDataRepository.advanceEscalation()
            com.rakshax.app.data.sos.EmergencyCallHelper.initiateEmergencyCall(applicationContext, force = true)
            val updated = MockDataRepository.sosState.value
            updateNotification(
                if (updated.escalationStage >= 3) {
                    "Priority 3 notified. Awaiting trusted-circle response."
                } else {
                    "Escalated to trusted-circle priority ${updated.escalationStage}."
                }
            )
        }
        stopSelf()
    }

    private fun startAsForegroundService(): Boolean = runCatching {
        val notification = buildNotification("Trusted-circle escalation is active")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        true
    }.getOrElse {
        stopSelf()
        false
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
            Intent(this, SosEscalationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RakshaX SOS escalation")
            .setContentText(message)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
    }

    private fun updateNotification(message: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "SOS escalation",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shows trusted-circle escalation progress for an active SOS"
                }
            )
        }
    }

    companion object {
        private const val CHANNEL_ID = "rakshax_sos_escalation"
        private const val NOTIFICATION_ID = 402
        const val ACTION_START = "com.rakshax.app.action.START_SOS_ESCALATION"
        const val ACTION_STOP = "com.rakshax.app.action.STOP_SOS_ESCALATION"

        fun start(context: Context) {
            val intent = Intent(context, SosEscalationService::class.java)
                .setAction(ACTION_START)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context, intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: ForegroundServiceStartNotAllowedException) {
                android.util.Log.e(
                    "RakshaX_SOS",
                    "Android blocked SOS escalation foreground service",
                    e
                )
            } catch (e: SecurityException) {
                android.util.Log.e(
                    "RakshaX_SOS",
                    "Unable to start SOS escalation service",
                    e
                )
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SosEscalationService::class.java))
        }
    }
}
