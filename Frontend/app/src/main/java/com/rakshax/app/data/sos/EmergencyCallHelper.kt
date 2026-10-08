package com.rakshax.app.data.sos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rakshax.app.data.model.Contact
import com.rakshax.app.data.repository.MockDataRepository

object EmergencyCallHelper {
    private const val TAG = "EmergencyCallHelper"
    private const val CHANNEL_ID = "rakshax_emergency_calls"
    private const val NOTIFICATION_ID = 403
    private const val COOLDOWN_MS = 4000L

    private var lastCallTimestamp: Long = 0L

    /**
     * Resolves the target contact to call based on current escalation stage or priority.
     */
    fun resolveTargetContact(): Contact? {
        val contacts = MockDataRepository.contacts.value.filter { it.isEnabled }
        if (contacts.isEmpty()) return null

        val currentStage = MockDataRepository.sosState.value.escalationStage
        // Try finding contact configured for the current priority stage first
        val stageContact = contacts.find { it.priority == currentStage }
        if (stageContact != null) return stageContact

        // Otherwise pick the highest priority enabled contact (priority 1 > 2 > 3)
        return contacts.minByOrNull { it.priority }
    }

    /**
     * Initiates a phone call to the primary emergency contact.
     * Uses Intent.ACTION_CALL if CALL_PHONE permission is granted, otherwise falls back to Intent.ACTION_DIAL.
     * Also posts a high-priority emergency notification with fullScreenIntent so that the call intent
     * displays even when the phone is locked or the app is running in the background.
     */
    fun initiateEmergencyCall(context: Context, force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        if (!force && (now - lastCallTimestamp) < COOLDOWN_MS) {
            Log.d(TAG, "Call skipped due to debounce cooldown ($COOLDOWN_MS ms)")
            return false
        }
        lastCallTimestamp = now

        val target = resolveTargetContact()
        if (target == null) {
            Log.w(TAG, "No enabled emergency contact available to call")
            return false
        }

        val rawPhone = target.phone.trim()
        val sanitizedPhone = rawPhone.filter { it.isDigit() || it == '+' }
        if (sanitizedPhone.isBlank()) {
            Log.w(TAG, "Emergency contact phone is blank: $rawPhone")
            return false
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intentAction = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val callIntent = Intent(intentAction).apply {
            data = Uri.parse("tel:$sanitizedPhone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        var launchSuccess = false

        // 1. Attempt direct activity launch
        try {
            context.startActivity(callIntent)
            launchSuccess = true
            Log.i(TAG, "Successfully launched call activity ($intentAction) for ${target.name}")
        } catch (e: SecurityException) {
            Log.w(TAG, "CALL_PHONE permission check failed at runtime; falling back to ACTION_DIAL", e)
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$sanitizedPhone")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(dialIntent)
                launchSuccess = true
            } catch (dialEx: Exception) {
                Log.e(TAG, "Failed to launch fallback dialer", dialEx)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start call activity directly (may be blocked by background restrictions)", e)
        }

        // 2. Post high-priority notification with fullScreenIntent
        // Essential for Android 10+ background activity launch restrictions and locked screen
        postEmergencyCallNotification(context, target, callIntent)

        // 3. Record emergency call in SOS state
        MockDataRepository.recordEmergencyCall(target.name)

        return launchSuccess
    }

    /**
     * Directly calls a specific contact (e.g. for testing or one-tap calling from contact card).
     */
    fun callContact(context: Context, contact: Contact) {
        val sanitizedPhone = contact.phone.filter { it.isDigit() || it == '+' }
        if (sanitizedPhone.isBlank()) return

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intentAction = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val callIntent = Intent(intentAction).apply {
            data = Uri.parse("tel:$sanitizedPhone")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(callIntent)
        } catch (e: SecurityException) {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$sanitizedPhone")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to call contact", e)
        }
    }

    private fun postEmergencyCallNotification(
        context: Context,
        contact: Contact,
        callIntent: Intent
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        createCallNotificationChannel(notificationManager)

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_call,
            "CALL ${contact.name.uppercase()}",
            pendingIntent
        ).build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 Emergency SOS Call: ${contact.name}")
            .setContentText("Calling ${contact.name} (${contact.relation}) • ${contact.phone}")
            .setSubText("Emergency Lifeline")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .addAction(callAction)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createCallNotificationChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergency SOS Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority emergency call notification for trusted contacts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
