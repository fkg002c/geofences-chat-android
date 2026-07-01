package com.ruinkogr.chatapp.data.remote

import com.ruinkogr.chatapp.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.GET

interface UsersService {

    @GET("api/users")
    suspend fun getUsers(): Response<List<UserDto>>
}