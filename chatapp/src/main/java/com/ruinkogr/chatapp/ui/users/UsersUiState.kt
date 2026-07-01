package com.ruinkogr.chatapp.ui.users

import com.ruinkogr.chatapp.data.remote.dto.UserDto

sealed interface UsersUiState {
    object Loading : UsersUiState
    data class Success(val users: List<UserDto>) : UsersUiState
    data class Error(val message: String) : UsersUiState
}