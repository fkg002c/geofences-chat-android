package com.ruinkogr.chatapp.ui.users

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ruinkogr.chatapp.R

@Composable
fun ServerStatusBanner() {
    val isOnline by ServerStatusMonitor.isServerOnline.collectAsState()

    AnimatedVisibility(
        visible = isOnline == false,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE57373))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.message_server_disconnected),
                color = Color.White
            )
        }
    }
}

