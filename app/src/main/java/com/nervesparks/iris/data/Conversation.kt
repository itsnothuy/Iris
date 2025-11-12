package com.nervesparks.iris.data

import java.time.Instant
import java.util.UUID

/**
 * Represents a conversation in the chat application.
 *
 * @property id Unique identifier for the conversation
 * @property title Display title for the conversation
 * @property createdAt When the conversation was created
 * @property lastModified When the conversation was last updated
 * @property messageCount Number of messages in the conversation
 * @property isPinned Whether the conversation is pinned to the top
 * @property isArchived Whether the conversation is archived
 */
data class Conversation(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val createdAt: Instant = Instant.now(),
    val lastModified: Instant = Instant.now(),
    val messageCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
)
