package com.ruinkogr.chatapp.data.remote

import com.ruinkogr.chatapp.data.remote.dto.FcmTokenRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FcmService {
    @POST("api/auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    @POST("api/auth/logout")
    suspend fun logout(): Response<Unit>
}