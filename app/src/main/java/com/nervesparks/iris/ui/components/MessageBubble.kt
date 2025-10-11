package com.nervesparks.iris.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.R
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays a single message bubble in the chat interface.
 *
 * @param message The message to display
 * @param onLongClick Optional callback for long-click actions
 * @param showTimestamp Whether to show the timestamp below the message
 * @param modifier Optional modifier for the composable
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onLongClick: (() -> Unit)? = null,
    showTimestamp: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isUserMessage = message.role == MessageRole.USER
    
    Row(
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Show assistant icon on the left for assistant messages
        if (message.role == MessageRole.ASSISTANT) {
            MessageIcon(iconRes = R.drawable.logo, description = "AI Assistant Icon")
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(
            horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            // Message bubble
            Box(
                modifier = Modifier
                    .background(
                        color = when (message.role) {
                            MessageRole.USER -> Color(0xFF171E2C)
                            MessageRole.ASSISTANT -> Color.Transparent
                            MessageRole.SYSTEM -> Color(0xFF2C2C2E)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .combinedClickable(
                        onLongClick = {
                            copyMessageToClipboard(context, message.content)
                            onLongClick?.invoke()
                        },
                        onClick = {}
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFFA0A0A5)
                    ),
                    overflow = TextOverflow.Clip
                )
            }
            
            // Timestamp
            if (showTimestamp) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTimestamp(message.timestamp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6C6C70)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            
            // Processing metrics for assistant messages
            if (message.role == MessageRole.ASSISTANT && message.processingTimeMs != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = buildString {
                        append("${message.processingTimeMs}ms")
                        if (message.tokenCount != null) {
                            append(" • ${message.tokenCount} tokens")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF6C6C70)
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Show user icon on the right for user messages
        if (isUserMessage) {
            Spacer(modifier = Modifier.width(4.dp))
            MessageIcon(iconRes = R.drawable.user_icon, description = "User Icon")
        }
    }
}

/**
 * Copies message content to the system clipboard.
 */
private fun copyMessageToClipboard(context: Context, content: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("message", content)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
}

/**
 * Formats a timestamp for display.
 */
private fun formatTimestamp(timestamp: java.time.Instant): String {
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(java.time.ZoneId.systemDefault())
    return formatter.format(timestamp)
}

/**
 * Displays an icon for messages.
 */
@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(24.dp)
    )
}
