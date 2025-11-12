package com.nervesparks.iris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Displays a loading skeleton for messages that are being processed.
 * Shows animated placeholders where message content will appear.
 *
 * @param isUserMessage Whether this is a user message skeleton
 * @param modifier Optional modifier for the composable
 */
@Composable
fun LoadingSkeleton(
    isUserMessage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeleton_alpha",
    )

    Row(
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        // Show assistant icon placeholder for assistant messages
        if (!isUserMessage) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(alpha)
                    .background(Color(0xFF3C3C3E), CircleShape),
            )
            Spacer(modifier = Modifier.width(4.dp))
        }

        Column(
            horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            // Message content placeholder
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(60.dp)
                    .alpha(alpha)
                    .background(
                        color = if (isUserMessage) Color(0xFF171E2C) else Color(0xFF2C2C2E),
                        shape = RoundedCornerShape(12.dp),
                    ),
            )
        }

        // Show user icon placeholder for user messages
        if (isUserMessage) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(alpha)
                    .background(Color(0xFF3C3C3E), CircleShape),
            )
        }
    }
}
