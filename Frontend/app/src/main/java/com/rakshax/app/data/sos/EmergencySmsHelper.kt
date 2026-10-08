package com.rakshax.app.data.sos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.rakshax.app.BuildConfig
import com.rakshax.app.data.model.Contact
import com.rakshax.app.data.repository.MockDataRepository

object EmergencySmsHelper {
    private const val TAG = "EmergencySmsHelper"

    /**
     * Constructs the web confirmation URL for a contact.
     * When opened, records acknowledgement in the backend and displays the victim's location on Google Maps.
     */
    fun buildConfirmationUrl(eventId: String, contactName: String): String {
        val baseUrl = BuildConfig.AURORA_SAFE_BASE_URL.trimEnd('/')
        val encodedName = java.net.URLEncoder.encode(contactName.trim(), "UTF-8")
        return "$baseUrl/sos/$eventId/acknowledge?contact_name=$encodedName"
    }

    /**
     * Constructs the emergency SMS body containing the victim's alert, location, and acknowledgement link.
     */
    fun buildEmergencyMessage(
        victimName: String,
        contactName: String,
        eventId: String,
        locationText: String?
    ): String {
        val confirmUrl = buildConfirmationUrl(eventId, contactName)
        val locPart = if (!locationText.isNullOrBlank() && locationText != "Location unavailable") {
            "Location: $locationText\n"
        } else {
            ""
        }
        return "EMERGENCY ALERT: $victimName triggered an SOS on RakshaX!\n" +
                locPart +
                "Confirm response & view location:\n$confirmUrl"
    }

    @Suppress("DEPRECATION")
    private fun getSmsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        } else {
            SmsManager.getDefault()
        }
    }

    /**
     * Sends emergency SMS messages containing the acknowledgement link to trusted contacts.
     * If SEND_SMS permission is granted, sends directly in the background.
     * If not granted, falls back to launching the SMS compose intent for the primary contact.
     */
    fun sendEmergencySms(
        context: Context,
        eventId: String,
        locationText: String? = null,
        targetContacts: List<Contact>? = null
    ): Int {
        val contacts = targetContacts ?: MockDataRepository.contacts.value.filter { it.isEnabled }
        if (contacts.isEmpty()) {
            Log.w(TAG, "No enabled contacts available for emergency SMS")
            return 0
        }

        val victimName = MockDataRepository.currentUser.value.username.ifBlank { "User" }
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        var sentCount = 0

        if (hasSmsPermission) {
            val smsManager = getSmsManager(context)
            for (contact in contacts) {
                val sanitizedPhone = contact.phone.filter { it.isDigit() || it == '+' }
                if (sanitizedPhone.isBlank()) continue

                val message = buildEmergencyMessage(
                    victimName = victimName,
                    contactName = contact.name,
                    eventId = eventId,
                    locationText = locationText
                )

                try {
                    val parts = smsManager.divideMessage(message)
                    if (parts.size > 1) {
                        smsManager.sendMultipartTextMessage(sanitizedPhone, null, parts, null, null)
                    } else {
                        smsManager.sendTextMessage(sanitizedPhone, null, message, null, null)
                    }
                    sentCount++
                    Log.i(TAG, "Emergency SMS dispatched to ${contact.name} ($sanitizedPhone)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SMS to ${contact.name} ($sanitizedPhone)", e)
                }
            }
        } else {
            Log.w(TAG, "SEND_SMS permission not granted; launching fallback SMS composer for primary contact")
            val primary = contacts.minByOrNull { it.priority } ?: contacts.first()
            val sanitizedPhone = primary.phone.filter { it.isDigit() || it == '+' }
            val message = buildEmergencyMessage(
                victimName = victimName,
                contactName = primary.name,
                eventId = eventId,
                locationText = locationText
            )

            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:$sanitizedPhone")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
                sentCount++
                Log.i(TAG, "Launched fallback SMS composer for ${primary.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch SMS composer intent", e)
            }
        }

        return sentCount
    }
}
