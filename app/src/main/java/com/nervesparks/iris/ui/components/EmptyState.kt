package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.R

/**
 * Displays the empty state when no messages are present in the conversation.
 * Shows a welcome message and conversation starters.
 *
 * @param onStarterClick Callback when a conversation starter is clicked
 * @param modifier Optional modifier for the composable
 */
@Composable
fun EmptyState(
    onStarterClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val conversationStarters = listOf(
        "Explains complex topics simply.",
        "Remembers previous inputs.",
        "May sometimes be inaccurate.",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Welcome header
        Text(
            text = "Hello, Ask me Anything",
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontWeight = FontWeight.W300,
                letterSpacing = 1.sp,
                fontSize = 50.sp,
                lineHeight = 60.sp,
            ),
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
        )

        // Conversation starters
        conversationStarters.forEach { starter ->
            ConversationStarterCard(
                text = starter,
                onClick = { onStarterClick?.invoke(starter) },
            )
        }
    }
}

/**
 * Individual conversation starter card.
 */
@Composable
private fun ConversationStarterCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(8.dp)
            .background(
                Color(0xFF010825),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable { onClick() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            // Info icon
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(Color.White, shape = CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.info_svgrepo_com),
                    contentDescription = null,
                    tint = Color.Black,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Starter text
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                textAlign = TextAlign.Start,
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
            )
        }
    }
}
