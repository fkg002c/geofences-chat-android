package com.ruinkogr.chatapp.data

sealed interface Resource<out T> {
    object Loading : Resource<Nothing>
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: String, val exception: Throwable? = null) : Resource<Nothing>
}