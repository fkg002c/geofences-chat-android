package com.ruinkogr.chatapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ruinkogr.chatapp.data.remote.dto.MessageDto

@Entity(tableName = "cached_messages")
data class MessageEntity(
    @PrimaryKey val id: Int,
    val senderId: Int,
    val receiverId: Int,
    val content: String,
    val isRead: Boolean,
    val createdAt: String
)

fun MessageDto.toEntity() = MessageEntity(
    id = this.id,
    senderId = this.senderId,
    receiverId = this.receiverId,
    content = this.content,
    isRead = this.isRead,
    createdAt = this.createdAt
)

fun MessageEntity.toDto() = MessageDto(
    id = this.id,
    senderId = this.senderId,
    receiverId = this.receiverId,
    content = this.content,
    isRead = this.isRead,
    createdAt = this.createdAt
)
