package com.nervesparks.iris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nervesparks.iris.R
import kotlinx.coroutines.delay

/**
 * Displays a loading indicator for when the AI is processing a response.
 * Now supports streaming text display with debounced updates.
 *
 * @param modifier Optional modifier for the composable
 * @param showMetrics Whether to show "Thinking..." text or streaming content
 * @param streamingText The current streaming text to display (optional)
 */
@Composable
fun ProcessingIndicator(
    modifier: Modifier = Modifier,
    showMetrics: Boolean = true,
    streamingText: String? = null
) {
    // Debounce streaming text updates to prevent excessive recomposition
    var debouncedText by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(streamingText) {
        if (streamingText != null && streamingText.isNotEmpty()) {
            // Debounce: wait 50ms before updating to reduce recomposition
            delay(50)
            debouncedText = streamingText
        } else {
            debouncedText = null
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Assistant icon
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = R.drawable.logo),
            contentDescription = "AI Assistant Icon",
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Processing bubble
        Box(
            modifier = Modifier
                .background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            if (debouncedText != null && debouncedText!!.isNotEmpty()) {
                // Show streaming text with typing indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Assistant is typing…",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFA0A0A5)
                        )
                    )
                }
            } else {
                // Show default "Thinking" state
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showMetrics) {
                        Text(
                            text = "Thinking",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFFA0A0A5)
                            )
                        )
                    }
                    
                    // Animated dots
                    AnimatedDots()
                }
            }
        }
    }
}

/**
 * Animated three-dot loading indicator.
 */
@Composable
private fun AnimatedDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 600,
                        delayMillis = index * 200,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot_$index"
            )
            
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(
                        color = Color(0xFFA0A0A5),
                        shape = CircleShape
                    )
            )
        }
    }
}
