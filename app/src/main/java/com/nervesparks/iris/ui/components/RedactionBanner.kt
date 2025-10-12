package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Displays a banner when PII redaction has been applied to a message.
 * Shows information about what was redacted and provides a dismiss option.
 *
 * @param redactionCount The number of PII items that were redacted
 * @param onDismiss Callback when the dismiss button is clicked
 * @param modifier Optional modifier for the composable
 */
@Composable
fun RedactionBanner(
    redactionCount: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C3E50)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info icon
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Privacy Info",
                tint = Color(0xFF64B5F6),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Privacy Protected",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = Color(0xFF64B5F6),
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append("Redacted $redactionCount ")
                        append(if (redactionCount == 1) "item" else "items")
                        append(" (emails, phones, or IDs)")
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFB3E5FC)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Dismiss button
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFFA0A0A5)
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}
