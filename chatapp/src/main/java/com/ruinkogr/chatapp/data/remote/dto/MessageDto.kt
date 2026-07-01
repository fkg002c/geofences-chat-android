package com.ruinkogr.chatapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SendMessageRequest(
    @SerializedName("content") val content: String,
    @SerializedName("receiverId") val receiverId: Int
)

data class MessageDto(
    @SerializedName("id") val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String
)

data class ReadMessagesRequest(
    @SerializedName("fromUserId") val fromUserId: Int
)

data class StatusResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)
