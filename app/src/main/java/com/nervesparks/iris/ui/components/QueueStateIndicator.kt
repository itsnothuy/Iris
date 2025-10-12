package com.nervesparks.iris.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * Queue state indicator component.
 * Displays the number of messages currently in the send queue.
 * 
 * @param isQueued Whether there are messages in the queue
 * @param queueSize Number of messages in the queue
 * @param modifier Optional modifier for the component
 */
@Composable
fun QueueStateIndicator(
    isQueued: Boolean,
    queueSize: Int,
    modifier: Modifier = Modifier
) {
    if (isQueued && queueSize > 0) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = MaterialTheme.shapes.small,
            tonalElevation = 1.dp,
            modifier = modifier
                .testTag("queue-indicator")
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Queue indicator",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                val messageText = if (queueSize == 1) {
                    "$queueSize message in queue"
                } else {
                    "$queueSize messages in queue"
                }
                
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
