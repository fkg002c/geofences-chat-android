package com.ruinkogr.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruinkogr.chatapp.data.Resource
import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import com.ruinkogr.chatapp.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _messagesState = MutableStateFlow<Resource<List<MessageDto>>>(Resource.Loading)
    val messagesState: StateFlow<Resource<List<MessageDto>>> = _messagesState

    fun loadChatHistory(currentUserId: Int, chatWithUserId: Int) {
        viewModelScope.launch {
            repository.getMessagesWithCache(
                currentUserId = currentUserId,
                chatWithUserId = chatWithUserId
            ).collect { resource ->
                _messagesState.value = resource
            }
        }
    }

    fun sendMessage(content: String, receiverId: Int) {
        viewModelScope.launch {
            // TODO post message with refresh on response
            when (val result = repository.sendMessage(content, receiverId)) {
                is Resource.Success -> {
                    // Refresh data
                    _messagesState.value = Resource.Success(result.data)
                }
                is Resource.Error -> {
                    // TODO send message error
                }
                is Resource.Loading -> { /* TODO optional */ }
            }
        }
    }
}