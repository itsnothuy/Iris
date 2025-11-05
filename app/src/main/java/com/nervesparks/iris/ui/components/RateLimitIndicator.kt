package com.nervesparks.iris.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Rate limiting indicator with countdown timer.
 * Shows remaining cooldown time when rate limited.
 * 
 * @param isRateLimited Whether rate limiting is active
 * @param initialCooldownSeconds Initial countdown value in seconds
 * @param modifier Optional modifier for the component
 */
@Composable
fun RateLimitIndicator(
    isRateLimited: Boolean,
    initialCooldownSeconds: Int,
    modifier: Modifier = Modifier
) {
    // Track cooldown countdown locally
    var remainingSeconds by remember { mutableStateOf(initialCooldownSeconds) }
    
    // Update when initial value changes - always sync with backend
    LaunchedEffect(initialCooldownSeconds, isRateLimited) {
        if (isRateLimited) {
            remainingSeconds = initialCooldownSeconds
        }
    }
    
    // Countdown timer - only count down when rate limited
    LaunchedEffect(isRateLimited) {
        if (isRateLimited) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds = maxOf(0, remainingSeconds - 1)
            }
        }
    }
    
    AnimatedVisibility(
        visible = isRateLimited && remainingSeconds > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = Color(0xFFFFA500).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
                .testTag("rate-limit-indicator")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassEmpty,
                    contentDescription = "Rate limit cooldown",
                    tint = Color(0xFFFFA500),
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Rate limit active",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "Cooldown: ${remainingSeconds}s remaining",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}
