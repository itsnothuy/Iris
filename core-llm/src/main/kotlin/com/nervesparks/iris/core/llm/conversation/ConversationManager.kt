package com.nervesparks.iris.core.llm.conversation

import com.nervesparks.iris.core.llm.inference.GenerationParameters
import com.nervesparks.iris.core.llm.inference.InferenceResult
import kotlinx.coroutines.flow.Flow

/**
 * Message in a conversation
 */
data class Message(
    val id: String,
    val content: String,
    val role: MessageRole,
    val timestamp: Long,
    val tokenCount: Int? = null,
    val processingTimeMs: Long? = null
)

/**
 * Role of a message sender
 */
enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

/**
 * Conversation metadata
 */
data class ConversationMetadata(
    val id: String,
    val title: String,
    val createdAt: Long,
    val lastModified: Long,
    val messageCount: Int,
    val totalTokens: Int
)

/**
 * Result of sending a message
 */
sealed class MessageSendResult {
    data class Success(
        val message: Message,
        val inferenceMetrics: InferenceMetrics
    ) : MessageSendResult()
    
    data class Error(
        val error: String,
        val cause: Throwable? = null
    ) : MessageSendResult()
}

/**
 * Metrics for an inference operation
 */
data class InferenceMetrics(
    val tokenCount: Int,
    val processingTimeMs: Long,
    val tokensPerSecond: Double
)

/**
 * Interface for managing conversations with AI models
 * Coordinates between inference sessions and conversation state
 */
interface ConversationManager {
    
    /**
     * Create a new conversation
     * @param title Optional title for the conversation
     * @return Conversation ID
     */
    suspend fun createConversation(title: String? = null): Result<String>
    
    /**
     * Get conversation metadata
     * @param conversationId Conversation identifier
     * @return Conversation metadata or null if not found
     */
    suspend fun getConversation(conversationId: String): ConversationMetadata?
    
    /**
     * Get all messages in a conversation
     * @param conversationId Conversation identifier
     * @return Flow of messages in the conversation
     */
    fun getMessages(conversationId: String): Flow<List<Message>>
    
    /**
     * Send a message in a conversation and get streaming response
     * @param conversationId Conversation identifier
     * @param message User message content
     * @param parameters Optional generation parameters
     * @return Flow of inference results (streaming tokens, completion, errors)
     */
    suspend fun sendMessage(
        conversationId: String,
        message: String,
        parameters: GenerationParameters = GenerationParameters()
    ): Flow<InferenceResult>
    
    /**
     * Delete a conversation and all its messages
     * @param conversationId Conversation identifier
     * @return true if deleted, false if not found
     */
    suspend fun deleteConversation(conversationId: String): Boolean
    
    /**
     * Clear all messages from a conversation
     * @param conversationId Conversation identifier
     * @return true if cleared, false if not found
     */
    suspend fun clearConversation(conversationId: String): Boolean
    
    /**
     * Get list of all conversations
     * @return Flow of conversation metadata
     */
    fun getAllConversations(): Flow<List<ConversationMetadata>>
}
