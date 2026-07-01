package com.ruinkogr.chatapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruinkogr.chatapp.data.remote.dto.MessageDto
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MessageBubble(message: MessageDto, currentUserId: Int) {
    val isMyMessage = message.senderId == currentUserId
    val formattedTime = remember(message.createdAt) {
        TimeFormatter.formatToHoursAndMinutes(message.createdAt)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = if (isMyMessage) Color(0xFFDCF8C6) else Color(0xFFFFFFFF),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(text = message.content, fontSize = 16.sp, color = Color.Black)

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(end = 4.dp)
                )

                if (isMyMessage) {
                    Text(
                        text = if (message.isRead) "✓✓" else "✓",
                        fontSize = 11.sp,
                        color = if (message.isRead) Color(0xFF34B7F1) else Color.Gray
                    )
                }
            }
        }
    }
}

object TimeFormatter {
    private val inputFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val outputFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun formatToHoursAndMinutes(isoString: String): String {
        return try {
            val utcDateTime = ZonedDateTime.parse(isoString, inputFormatter)
            val localDateTime = utcDateTime.withZoneSameInstant(ZoneId.systemDefault())
            localDateTime.format(outputFormatter)
        } catch (e: Exception) {
            ""
        }
    }
}