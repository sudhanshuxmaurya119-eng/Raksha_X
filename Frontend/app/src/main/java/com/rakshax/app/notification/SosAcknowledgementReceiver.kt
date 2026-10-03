package com.rakshax.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rakshax.app.data.remote.AuroraSafeRepository
import com.rakshax.app.data.repository.MockDataRepository
import com.rakshax.app.service.SosEscalationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SosAcknowledgementReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID).orEmpty()
        if (eventId.isBlank()) return
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Trusted contact"
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AuroraSafeRepository(context).acknowledgeSos(eventId, contactName)
                if (result.isSuccess) {
                    MockDataRepository.acknowledgeSos(contactName)
                    SosEscalationService.stop(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_CONTACT_NAME = "contact_name"
    }
}
