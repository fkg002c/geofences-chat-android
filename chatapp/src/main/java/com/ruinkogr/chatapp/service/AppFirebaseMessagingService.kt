package com.ruinkogr.chatapp.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ruinkogr.chatapp.data.local.MessageDao
import com.ruinkogr.chatapp.data.local.MessageEntity
import com.ruinkogr.chatapp.data.remote.FcmService
import com.ruinkogr.chatapp.data.remote.dto.FcmTokenRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var messageDao: MessageDao

    @Inject
    lateinit var fcmService: FcmService
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "onMessageReceived data: ${remoteMessage.data}, notif: ${remoteMessage.notification} ")

        if (remoteMessage.data.isNotEmpty()) {
            val senderId = remoteMessage.data["senderId"]?.toIntOrNull() ?: return
            val receiverId = remoteMessage.data["receiverId"]?.toIntOrNull() ?: return
            val content = remoteMessage.data["content"] ?: return
            val id = remoteMessage.data["id"]?.toIntOrNull() ?: return
            val createdAt = remoteMessage.data["createdAt"] ?: return

            val incomingMessage = MessageEntity(
                id = id,
                senderId = senderId,
                receiverId = receiverId,
                content = content,
                isRead = false,
                createdAt = createdAt
            )

            // Direct DB insert
            serviceScope.launch {
                messageDao.insertMessages(listOf(incomingMessage))
                // if chat screen is open then new message appears
            }

            // TODO : Notification
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "onNewToken: $token")

        serviceScope.launch {
            try {
                fcmService.updateFcmToken(FcmTokenRequest(fcmToken = token))
            } catch (e: Exception) {
                Log.e(TAG, "updateFcmToken error: ${e.message}")
            }
        }
    }

    override fun onRegistered(installationId: String) {
        super.onRegistered(installationId)
        Log.i(TAG, "onRegistered: $installationId")
    }

    override fun onUnregistered(installationId: String) {
        super.onUnregistered(installationId)
        Log.i(TAG, "onUnregistered: $installationId")
        // TODO inform app server
    }

    private companion object {
        private const val TAG = "FMS"
    }
}
