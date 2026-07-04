package com.ruinkogr.chatapp.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import com.ruinkogr.chatapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val currentUserId: Int = checkNotNull(savedStateHandle["currentUserId"])
    private val chatWithUserId: Int = checkNotNull(savedStateHandle["chatWithUserId"])

    val messagesState: StateFlow<Resource<List<MessageDto>>> = repository
        .getMessagesWithCache(currentUserId, chatWithUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Resource.Loading
        )

    // errors flow for Toast or SnakeBar
    private val _errorChannel = Channel<String>()
    val errorSignal = _errorChannel.receiveAsFlow()

    fun sendMessage(content: String) {
        viewModelScope.launch {
            // sendMessageApi + insert into DB !!!
            when (val result = repository.sendMessage(content, chatWithUserId)) {
                is Resource.Success -> {
                    // Nothing to do because Room's @Insert will notice flow
                }

                is Resource.Error -> {
                    _errorChannel.send(result.message ?: "Send error")
                }

                is Resource.Loading -> { /* Optional: for sending */
                }
            }
        }
    }
}