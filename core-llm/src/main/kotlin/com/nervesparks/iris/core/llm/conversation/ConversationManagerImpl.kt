package com.nervesparks.iris.core.llm.conversation

import android.util.Log
import com.nervesparks.iris.core.llm.inference.FinishReason
import com.nervesparks.iris.core.llm.inference.GenerationParameters
import com.nervesparks.iris.core.llm.inference.InferenceResult
import com.nervesparks.iris.core.llm.inference.InferenceSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation of ConversationManager
 * 
 * This is a simplified implementation that manages conversation state in memory.
 * For persistent storage, this should be extended or replaced with an implementation
 * that integrates with the app's Room database.
 */
@Singleton
class ConversationManagerImpl @Inject constructor(
    private val inferenceSession: InferenceSession
) : ConversationManager {
    
    companion object {
        private const val TAG = "ConversationManager"
        private const val MAX_CONVERSATIONS = 100
        private const val MAX_MESSAGES_PER_CONVERSATION = 1000
    }
    
    private val conversations = mutableMapOf<String, ConversationState>()
    private val conversationsFlow = MutableStateFlow<List<ConversationMetadata>>(emptyList())
    
    override suspend fun createConversation(title: String?): Result<String> {
        return try {
            val conversationId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            
            // Check conversation limit
            if (conversations.size >= MAX_CONVERSATIONS) {
                // Remove oldest conversation
                val oldest = conversations.values.minByOrNull { it.metadata.createdAt }
                oldest?.let { conversations.remove(it.metadata.id) }
            }
            
            val conversationTitle = title ?: "New Conversation"
            val metadata = ConversationMetadata(
                id = conversationId,
                title = conversationTitle,
                createdAt = now,
                lastModified = now,
                messageCount = 0,
                totalTokens = 0
            )
            
            val state = ConversationState(
                metadata = metadata,
                messages = mutableListOf(),
                inferenceSessionId = null
            )
            
            conversations[conversationId] = state
            updateConversationsFlow()
            
            Log.i(TAG, "Created conversation: $conversationId")
            Result.success(conversationId)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create conversation", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getConversation(conversationId: String): ConversationMetadata? {
        return conversations[conversationId]?.metadata
    }
    
    override fun getMessages(conversationId: String): Flow<List<Message>> = flow {
        val state = conversations[conversationId]
        if (state != null) {
            emit(state.messages.toList())
        } else {
            emit(emptyList())
        }
    }
    
    override suspend fun sendMessage(
        conversationId: String,
        message: String,
        parameters: GenerationParameters
    ): Flow<InferenceResult> = flow {
        val state = conversations[conversationId]
            ?: throw IllegalArgumentException("Conversation not found: $conversationId")
        
        try {
            // Create user message
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                content = message,
                role = MessageRole.USER,
                timestamp = System.currentTimeMillis()
            )
            
            state.messages.add(userMessage)
            
            // Create or reuse inference session
            val sessionId = state.inferenceSessionId ?: run {
                val result = inferenceSession.createSession(conversationId)
                if (result.isFailure) {
                    throw result.exceptionOrNull() ?: Exception("Failed to create session")
                }
                val newSessionId = result.getOrNull()!!.sessionId
                state.inferenceSessionId = newSessionId
                newSessionId
            }
            
            // Track response building
            val responseTokens = mutableListOf<String>()
            val startTime = System.currentTimeMillis()
            
            // Generate response
            inferenceSession.generateResponse(sessionId, message, parameters)
                .collect { result ->
                    when (result) {
                        is InferenceResult.GenerationStarted -> {
                            emit(result)
                        }
                        
                        is InferenceResult.TokenGenerated -> {
                            responseTokens.add(result.token)
                            emit(result)
                        }
                        
                        is InferenceResult.GenerationCompleted -> {
                            val processingTime = System.currentTimeMillis() - startTime
                            
                            // Create assistant message
                            val assistantMessage = Message(
                                id = UUID.randomUUID().toString(),
                                content = result.fullText,
                                role = MessageRole.ASSISTANT,
                                timestamp = System.currentTimeMillis(),
                                tokenCount = result.tokenCount,
                                processingTimeMs = processingTime
                            )
                            
                            state.messages.add(assistantMessage)
                            
                            // Update metadata
                            state.metadata = state.metadata.copy(
                                lastModified = System.currentTimeMillis(),
                                messageCount = state.messages.size,
                                totalTokens = state.metadata.totalTokens + result.tokenCount
                            )
                            
                            // Trim messages if needed
                            trimMessagesIfNeeded(state)
                            
                            updateConversationsFlow()
                            
                            emit(result)
                        }
                        
                        is InferenceResult.SafetyViolation -> {
                            Log.w(TAG, "Safety violation: ${result.reason}")
                            emit(result)
                        }
                        
                        is InferenceResult.Error -> {
                            Log.e(TAG, "Inference error: ${result.error}", result.cause)
                            emit(result)
                        }
                    }
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            emit(InferenceResult.Error(
                sessionId = conversationId,
                error = "Failed to send message: ${e.message}",
                cause = e
            ))
        }
    }
    
    override suspend fun deleteConversation(conversationId: String): Boolean {
        val state = conversations.remove(conversationId)
        if (state != null) {
            // Close inference session if exists
            state.inferenceSessionId?.let { sessionId ->
                inferenceSession.closeSession(sessionId)
            }
            updateConversationsFlow()
            Log.i(TAG, "Deleted conversation: $conversationId")
            return true
        }
        return false
    }
    
    override suspend fun clearConversation(conversationId: String): Boolean {
        val state = conversations[conversationId]
        if (state != null) {
            state.messages.clear()
            state.metadata = state.metadata.copy(
                lastModified = System.currentTimeMillis(),
                messageCount = 0,
                totalTokens = 0
            )
            
            // Close and recreate inference session
            state.inferenceSessionId?.let { sessionId ->
                inferenceSession.closeSession(sessionId)
            }
            state.inferenceSessionId = null
            
            updateConversationsFlow()
            Log.i(TAG, "Cleared conversation: $conversationId")
            return true
        }
        return false
    }
    
    override fun getAllConversations(): Flow<List<ConversationMetadata>> {
        return conversationsFlow.asStateFlow()
    }
    
    // Private helper methods
    
    private fun updateConversationsFlow() {
        val metadataList = conversations.values
            .map { it.metadata }
            .sortedByDescending { it.lastModified }
        conversationsFlow.value = metadataList
    }
    
    private fun trimMessagesIfNeeded(state: ConversationState) {
        if (state.messages.size > MAX_MESSAGES_PER_CONVERSATION) {
            val toRemove = state.messages.size - MAX_MESSAGES_PER_CONVERSATION
            repeat(toRemove) {
                state.messages.removeAt(0)
            }
            Log.d(TAG, "Trimmed $toRemove messages from conversation ${state.metadata.id}")
        }
    }
    
    /**
     * Internal state for a conversation
     */
    private data class ConversationState(
        var metadata: ConversationMetadata,
        val messages: MutableList<Message>,
        var inferenceSessionId: String?
    )
}
