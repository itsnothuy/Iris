package com.nervesparks.iris.ui.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.R
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import java.time.Instant

@Composable
fun ChatMessageList(viewModel: MainViewModel, scrollState: LazyListState) {
    val messages = viewModel.messages
    val context = LocalContext.current

    LazyColumn(state = scrollState) {
        itemsIndexed(messages.drop(3)) { index, messageMap ->
            val role = messageMap["role"] ?: ""
            val content = (messageMap["content"] ?: "").trimEnd()

            if (role != "system") {
                when (role) {
                    "codeBlock" -> CodeBlockMessage(content)
                    else -> UserOrAssistantMessage(
                        role = role,
                        message = content,
                        onLongClick = {
                            if (viewModel.getIsSending()) {
                                Toast.makeText(
                                    context,
                                    "Wait till generation is done!",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                viewModel.toggler = true
                            }
                        },
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(1.dp).fillMaxWidth()) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserOrAssistantMessage(role: String, message: String, onLongClick: () -> Unit) {
    Row(
        horizontalArrangement = if (role == "user") Arrangement.End else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        if (role == "assistant") MessageIcon(iconRes = R.drawable.logo, description = "Bot Icon")

        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .background(
                    color = if (role == "user") Color(0xFF171E2C) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp),
                )
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {},
                )
                .padding(8.dp),
        ) {
            Text(
                text = message.removePrefix("```"),
                style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFA0A0A5)),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (role == "user") MessageIcon(iconRes = R.drawable.user_icon, description = "User Icon")
    }
}

@Composable
private fun CodeBlockMessage(content: String) {
    Box(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .background(Color.Black, shape = RoundedCornerShape(8.dp))
            .fillMaxWidth(),
    ) {
        Text(
            text = content.removePrefix("```"),
            style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFA0A0A5)),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun MessageIcon(iconRes: Int, description: String) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.res.painterResource(id = iconRes),
        contentDescription = description,
        modifier = Modifier.size(20.dp),
    )
}

/**
 * Helper function to convert legacy Map-based message format to Message data class.
 * This enables gradual migration to the new Message model.
 *
 * Example usage in ChatMessageList:
 * ```
 * itemsIndexed(messages.drop(3)) { index, messageMap ->
 *     val message = messageMap.toMessage()
 *     if (message != null && message.role != MessageRole.SYSTEM) {
 *         MessageBubble(
 *             message = message,
 *             showTimestamp = true,
 *             onLongClick = { /* handle long click */ }
 *         )
 *     }
 * }
 * ```
 */
private fun Map<String, String>.toMessage(): Message? {
    val role = this["role"] ?: return null
    val content = this["content"] ?: return null

    val messageRole = when (role) {
        "user" -> MessageRole.USER
        "assistant" -> MessageRole.ASSISTANT
        "system" -> MessageRole.SYSTEM
        else -> return null // Skip codeBlock and other types for now
    }

    return Message(
        content = content.trimEnd(),
        role = messageRole,
        timestamp = Instant.now(),
    )
}
