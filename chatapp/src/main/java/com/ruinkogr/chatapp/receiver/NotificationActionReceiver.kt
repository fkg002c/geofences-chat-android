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

        // Получаем доступ к вашей Room БД (подставьте ваш синглтон БД)
//        val messageDao = AppDatabase.getInstance(context).messageDao()
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        when (intent.action) {
            "ACTION_MARK_AS_READ" -> {
                coroutineScope.launch {
                    messageDao.markAsRead(messageId) // Псевдокод вашей БД
                    // Закрываем шторку уведомления
                    val notificationManager = context.getSystemService(NotificationManager::class.java)
                    notificationManager.cancel(senderId) // ID уведомления равен senderId, чтобы гасить пуши от конкретного юзера
                }
            }

            "ACTION_REPLY" -> {
                // Извлекаем текст быстрого ответа из шторки
                val remoteInput = RemoteInput.getResultsFromIntent(intent)
                val replyText = remoteInput?.getCharSequence("KEY_TEXT_REPLY")?.toString()

                if (!replyText.isNullOrBlank()) {
                    coroutineScope.launch {
                        // 1. Сохраняем отправленное сообщение локально в БД
                        // 2. Вызываем сетевой запрос к вашему Node.js REST API для отправки сообщения

                        // 3. Обновляем уведомление, показывая, что ответ отправлен (UX стандарт)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.cancel(senderId)
                    }
                }
            }
        }
    }
}
