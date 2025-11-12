package com.nervesparks.iris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a conversation in the chat application.
 *
 * Each conversation contains multiple messages and has metadata
 * for organization and display purposes.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastModified: Long,
    val messageCount: Int,
    val isPinned: Boolean,
    val isArchived: Boolean,
)
