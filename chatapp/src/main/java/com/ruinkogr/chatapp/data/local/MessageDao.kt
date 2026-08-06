package com.ruinkogr.chatapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM cached_messages WHERE (senderId = :currentUserId AND receiverId = :chatWithId) OR (senderId = :chatWithId AND receiverId = :currentUserId) ORDER BY createdAt DESC")
    fun getChatHistoryFlow(currentUserId: Int, chatWithId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>

    @Query("DELETE FROM cached_messages")
    suspend fun clearAllMessages()

    @Query("UPDATE cached_messages SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM cached_messages WHERE id = :messageId LIMIT 1)")
    suspend fun isMessageExists(messageId: Int): Boolean
}