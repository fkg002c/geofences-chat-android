package com.ruinkogr.chatapp.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String
)