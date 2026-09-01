package com.ruinkogr.chatapp.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.ruinkogr.chatapp.data.local.MessageDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var messageDao: MessageDao
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val messageId = intent.getIntExtra("MESSAGE_ID", -1)
        val senderId = intent.getIntExtra("SENDER_ID", -1)

        // Get access to your Room DB (plug in your DB singleton here)
//        val messageDao = AppDatabase.getInstance(context).messageDao()
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            "ACTION_MARK_AS_READ" -> {
                coroutineScope.launch {
                    messageDao.markAsRead(messageId) // Pseudocode for your DB
                    // Dismiss the notification
                    val notificationManager = context.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(senderId) // Notification ID equals senderId, so it dismisses pushes from a specific user
                }
            }

            "ACTION_REPLY" -> {
                // Extract the quick-reply text from the notification
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("KEY_TEXT_REPLY")?.toString()

                if (!replyText.isNullOrBlank()) {
                    coroutineScope.launch {
                        // 1. Save the sent message locally in the DB
                        // 2. Call the network request to your Node.js REST API to send the message

                        // 3. Update the notification to show the reply was sent (UX standard)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(senderId)
                    }
                }
            }
        }
    }
}
