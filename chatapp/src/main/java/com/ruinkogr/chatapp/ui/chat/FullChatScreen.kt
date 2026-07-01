package com.ruinkogr.chatapp.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ruinkogr.chatapp.data.Resource

@Composable
fun FullChatScreen(
    viewModel: ChatViewModel,
    currentUserId: Int,
    chatWithUserId: Int
) {
    // LaunchedEffect run one time until key1 changed
    LaunchedEffect(key1 = chatWithUserId) {
        viewModel.loadChatHistory(currentUserId = currentUserId, chatWithUserId = chatWithUserId)
    }

    Scaffold(
        bottomBar = {
            MessageInputBar(
                modifier = Modifier.navigationBarsPadding(),
                onSendMessage = { text ->
                    viewModel.sendMessage(text, chatWithUserId)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val messagesState by viewModel.messagesState.collectAsState()

            when (val state = messagesState) {
                is Resource.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is Resource.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        reverseLayout = true // Newest messages at bottom
                    ) {
                        items(state.data) { message ->
                            MessageBubble(message = message, currentUserId = currentUserId)
                        }
                    }
                }
                is Resource.Error -> Text(
                    text = state.message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
