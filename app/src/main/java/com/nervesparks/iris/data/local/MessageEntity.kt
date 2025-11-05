package com.nervesparks.iris.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a persisted message in the chat conversation.
 * 
 * This entity maps directly to the messages table in the local database,
 * enabling conversation history to persist across app sessions.
 * 
 * Messages are linked to conversations via conversationId with cascade delete,
 * meaning when a conversation is deleted, all its messages are also deleted.
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val content: String,
    val role: String,
    val timestamp: Long,
    val processingTimeMs: Long?,
    val tokenCount: Int?,
    val conversationId: String = "default" // Default conversation for backward compatibility
)
