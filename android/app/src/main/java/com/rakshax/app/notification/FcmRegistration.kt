package com.rakshax.app.notification

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.rakshax.app.data.remote.AuroraSafeApi
import com.rakshax.app.data.repository.MockDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FcmRegistration {
    fun registerCurrentDevice(context: Context) {
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                CoroutineScope(Dispatchers.IO).launch {
                    AuroraSafeApi(context).registerPushToken(
                        token = token,
                        userId = MockDataRepository.currentUser.value.id
                    )
                }
            }
        }
    }
}
