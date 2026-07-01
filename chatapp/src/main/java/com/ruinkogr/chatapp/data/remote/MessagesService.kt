package com.ruinkogr.chatapp.data.remote

import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import com.ruinkogr.chatapp.data.remote.dto.ReadMessagesRequest
import com.ruinkogr.chatapp.data.remote.dto.SendMessageRequest
import com.ruinkogr.chatapp.data.remote.dto.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface MessagesService {

    @POST("api/messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<List<MessageDto>>

    @GET("api/messages")
    suspend fun getMessages(
        @Query("chatWith") chatWithUserId: Int? = null
    ): Response<List<MessageDto>>

    @PUT("api/messages/read")
    suspend fun markAsRead(@Body request: ReadMessagesRequest): Response<StatusResponse>
}