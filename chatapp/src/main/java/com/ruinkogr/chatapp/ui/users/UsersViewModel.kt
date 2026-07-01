package com.ruinkogr.chatapp.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _usersState = MutableStateFlow<UsersUiState>(UsersUiState.Loading)
    val usersState: StateFlow<UsersUiState> = _usersState

    fun loadUsers() {
        viewModelScope.launch {
            repository.getUsers().collect { resource ->
                // TODO Resource-to-UsersUiState conversion
                _usersState.value = when (resource) {
                    is Resource.Loading -> UsersUiState.Loading
                    is Resource.Success -> UsersUiState.Success(users = resource.data)
                    is Resource.Error -> UsersUiState.Error(message = resource.message)
                }
            }
        }
    }
}