package com.ruinkogr.chatapp.data.repository

import com.ruinkogr.chatapp.data.Resource
import retrofit2.Response
import java.io.IOException

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Resource.Success(body)
                } else {
                    Resource.Error("Empty response from Server")
                }
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown server error"
                Resource.Error("Error ${response.code()}: $errorMessage")
            }
        } catch (e: IOException) {
            Resource.Error("No internet connection. Check", e)
        } catch (e: Exception) {
            Resource.Error("An error occurred while processing data.", e)
        }
    }
}