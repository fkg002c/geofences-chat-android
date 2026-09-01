package com.ruinkogr.chatapp.data.repository

import com.ruinkogr.chatapp.R
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.util.UiText
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
                    Resource.Error(UiText.StringResource(R.string.error_empty_response))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = if (errorBody != null) {
                    UiText.StringResource(R.string.error_server_response_format, listOf(response.code(), errorBody))
                } else {
                    UiText.StringResource(R.string.error_server_unknown_format, listOf(response.code()))
                }
                Resource.Error(message)
            }
        } catch (e: IOException) {
            Resource.Error(UiText.StringResource(R.string.error_no_internet), e)
        } catch (e: Exception) {
            Resource.Error(UiText.StringResource(R.string.error_processing_data), e)
        }
    }
}