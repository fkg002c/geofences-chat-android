package com.ruinkogr.chatapp.data.remote

import com.ruinkogr.chatapp.data.remote.dto.LoginRequest
import com.ruinkogr.chatapp.data.remote.dto.LoginResponse
import com.ruinkogr.chatapp.data.remote.dto.RefreshTokenRequest
import com.ruinkogr.chatapp.data.remote.dto.RegisterRequest
import com.ruinkogr.chatapp.data.remote.dto.TokenResponse
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Sync request with Call response for inner .execute() call in Authenticator
    @POST("api/auth/refresh")
    fun refreshTokensSync(@Body request: RefreshTokenRequest): Call<TokenResponse>

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): Response<Unit>
}