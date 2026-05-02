package com.farmconnect.app.services

import android.util.Log
import com.farmconnect.app.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Send token to backend if user is logged in
        serviceScope.launch {
            try {
                val isLoggedIn = authRepository.isLoggedIn().first()
                if (isLoggedIn) {
                    authRepository.updateFcmToken(token)
                    Log.d(TAG, "FCM token sent to backend successfully")
                } else {
                    Log.d(TAG, "User not logged in, token will be sent on next login")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send FCM token to backend", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Handle data payload
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            // Show notification
        }
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
