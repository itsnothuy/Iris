package com.nervesparks.iris.data

import java.time.Instant
import java.util.UUID

/**
 * Represents a message in the chat conversation.
 *
 * @property id Unique identifier for the message
 * @property content The text content of the message
 * @property role The role of the message sender (user or assistant)
 * @property timestamp When the message was created
 * @property processingTimeMs Optional processing time in milliseconds for AI responses
 * @property tokenCount Optional token count for AI responses
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val role: MessageRole,
    val timestamp: Instant = Instant.now(),
    val processingTimeMs: Long? = null,
    val tokenCount: Int? = null,
) {
    /**
     * Convenience property for backward compatibility with existing code.
     */
    val isFromUser: Boolean
        get() = role == MessageRole.USER
}

/**
 * Enum representing the role of a message sender in the conversation.
 */
enum class MessageRole {
    /** Message from the user */
    USER,

    /** Message from the AI assistant */
    ASSISTANT,

    /** System message (instructions, context, etc.) */
    SYSTEM,
}
