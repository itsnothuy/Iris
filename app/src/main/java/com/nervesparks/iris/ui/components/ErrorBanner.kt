package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Displays an error banner with a retry option.
 * Shows error messages and provides a way to retry the failed operation.
 *
 * @param errorMessage The error message to display
 * @param onRetry Callback when the retry button is clicked
 * @param onDismiss Optional callback when the dismiss button is clicked
 * @param modifier Optional modifier for the composable
 */
@Composable
fun ErrorBanner(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3D1F1F),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Error icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = Color(0xFFFF6B6B),
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Error message
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFFFF6B6B),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFFFCCCC),
                    ),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Retry button
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF88D0FF),
                    ),
                ) {
                    Text("Retry")
                }

                // Dismiss button
                if (onDismiss != null) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFA0A0A5),
                        ),
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }
    }
}
