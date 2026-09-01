package com.ruinkogr.chatapp.data

import com.ruinkogr.chatapp.util.UiText

sealed interface Resource<out T> {
    object Loading : Resource<Nothing>
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: UiText, val exception: Throwable? = null) : Resource<Nothing>
}