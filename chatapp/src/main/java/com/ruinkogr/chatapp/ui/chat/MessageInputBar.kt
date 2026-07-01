package com.ruinkogr.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun MessageInputBar(
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var textState by remember { mutableStateOf("") }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textState,
            onValueChange = { textState = it },
            placeholder = { Text("Message") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
        )

        IconButton(
            onClick = {
                if (textState.trim().isNotEmpty()) {
                    onSendMessage(textState.trim())
                    textState = "" // Clean on Send
                }
            },
            enabled = textState.trim().isNotEmpty(),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = Color.Gray
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send"
            )
        }
    }
}
