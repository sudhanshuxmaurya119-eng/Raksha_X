package com.rakshax.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rakshax.app.MainActivity
import com.rakshax.app.R
import com.rakshax.app.data.location.SosLocationPayload
import com.rakshax.app.data.remote.AuroraSafeApi
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RakshaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            AuroraSafeApi(applicationContext).registerPushToken(
                token = token,
                userId = MockDataRepository.currentUser.value.id
            )
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (message.data["type"] != "sos") return

        val latitude = message.data["latitude"]?.toDoubleOrNull()
        val longitude = message.data["longitude"]?.toDoubleOrNull()
        val location = if (latitude != null && longitude != null) {
            SosLocationPayload(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = 0f,
                timestampMillis = System.currentTimeMillis()
            )
        } else null
        MockDataRepository.triggerSos(
            source = "AuroraSafe FCM",
            location = location,
            backendStatus = "SENT",
            backendEventId = message.data["event_id"]
        )
        showNotification(message.data["event_id"].orEmpty())
    }

    private fun showNotification(eventId: String) {
        val channelId = "rakshax_sos_alerts"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "SOS alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acknowledgeIntent = PendingIntent.getBroadcast(
            this,
            11,
            Intent(this, SosAcknowledgementReceiver::class.java).apply {
                putExtra(SosAcknowledgementReceiver.EXTRA_EVENT_ID, eventId)
                putExtra(SosAcknowledgementReceiver.EXTRA_CONTACT_NAME, MockDataRepository.currentUser.value.username)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("RakshaX emergency alert")
            .setContentText("A trusted contact needs acknowledgement")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(android.R.drawable.ic_menu_send, "Acknowledge", acknowledgeIntent)
            .build()
        getSystemService(NotificationManager::class.java).notify(SOS_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val SOS_NOTIFICATION_ID = 402
    }
}
