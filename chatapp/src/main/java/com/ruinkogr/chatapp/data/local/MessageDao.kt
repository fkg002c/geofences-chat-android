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
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM cached_messages")
    suspend fun clearAllMessages()
}