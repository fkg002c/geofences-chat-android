package com.ruinkogr.chatapp.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UsersViewModel @Inject constructor(
    repository: ChatRepository,
) : ViewModel() {

    // Single flow creation on ViewModel start.
    // repository.getUsers() method is called ONE time.
    val usersState: StateFlow<UsersUiState> = repository.getUsers()
        .map { resource ->
            when (resource) {
                is Resource.Loading -> UsersUiState.Loading
                is Resource.Success -> UsersUiState.Success(users = resource.data)
                is Resource.Error -> UsersUiState.Error(message = resource.message)
                is Resource.LoggedOut -> UsersUiState.Error(message = "logged out")
            }
        }
        .distinctUntilChanged() // <-- Skip emit if data is not changed
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UsersUiState.Loading
        )
}
