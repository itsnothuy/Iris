package com.nervesparks.iris.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a non-blocking hint when inference is rate-limited or device is thermally throttled.
 * Shows appropriate message and icon to inform user of degraded performance.
 *
 * @param isRateLimited Whether the inference is currently rate-limited
 * @param isThermalThrottled Whether the device is thermally throttled
 * @param modifier Optional modifier for the composable
 */
@Composable
fun ThermalHint(
    isRateLimited: Boolean,
    isThermalThrottled: Boolean,
    modifier: Modifier = Modifier
) {
    val shouldShow = isRateLimited || isThermalThrottled
    
    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(
                    color = when {
                        isThermalThrottled && isRateLimited -> Color(0xFFFF6B35) // Orange-red for both
                        isThermalThrottled -> Color(0xFFFF8C42) // Orange for thermal
                        isRateLimited -> Color(0xFFFFA500) // Amber for rate-limit
                        else -> Color.Transparent
                    }.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Performance hint",
                    tint = when {
                        isThermalThrottled && isRateLimited -> Color(0xFFFF6B35)
                        isThermalThrottled -> Color(0xFFFF8C42)
                        isRateLimited -> Color(0xFFFFA500)
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = when {
                            isThermalThrottled && isRateLimited -> "Device warming up • Slowing down"
                            isThermalThrottled -> "Device warming up"
                            isRateLimited -> "High activity detected"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = when {
                            isThermalThrottled && isRateLimited -> 
                                "Reducing speed to prevent overheating"
                            isThermalThrottled -> 
                                "Streaming slowed to cool down device"
                            isRateLimited -> 
                                "Streaming slowed to maintain stability"
                            else -> ""
                        },
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
