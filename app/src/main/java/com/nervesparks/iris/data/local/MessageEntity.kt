package com.nervesparks.iris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted message in the chat conversation.
 * 
 * This entity maps directly to the messages table in the local database,
 * enabling conversation history to persist across app sessions.
 */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val processingTimeMs: Long?,
    val tokenCount: Int?
)
