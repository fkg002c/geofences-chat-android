package com.ruinkogr.chatapp.data.remote.dto

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

@Serializable
data class TokenResponse(
    @SerializedName("accessToken") val accessToken: String
)

@Serializable
data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("refreshToken") val refreshToken: String
)

@Serializable
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

@Serializable
data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)