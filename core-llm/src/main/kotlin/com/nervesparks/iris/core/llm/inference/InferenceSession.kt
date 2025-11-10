package com.nervesparks.iris.core.llm.inference

import kotlinx.coroutines.flow.Flow

/**
 * Interface for managing inference sessions with models
 * Provides high-level API for model loading, session management, and text generation
 */
interface InferenceSession {
    
    /**
     * Load a model with specified parameters
     * @param modelDescriptor Descriptor containing model information
     * @param parameters Inference parameters for model configuration
     * @return Result containing ModelLoadResult on success
     */
    suspend fun loadModel(
        modelDescriptor: ModelDescriptor,
        parameters: InferenceParameters
    ): Result<ModelLoadResult>
    
    /**
     * Create a new inference session for a conversation
     * @param conversationId Unique identifier for the conversation
     * @return Result containing InferenceSessionContext on success
     */
    suspend fun createSession(conversationId: String): Result<InferenceSessionContext>
    
    /**
     * Generate a response for a prompt within a session
     * @param sessionId Session identifier
     * @param prompt User prompt/message
     * @param parameters Generation parameters
     * @return Flow of InferenceResult events (tokens, completion, errors)
     */
    suspend fun generateResponse(
        sessionId: String,
        prompt: String,
        parameters: GenerationParameters
    ): Flow<InferenceResult>
    
    /**
     * Get context information for an active session
     * @param sessionId Session identifier
     * @return InferenceSessionContext or null if session doesn't exist
     */
    suspend fun getSessionContext(sessionId: String): InferenceSessionContext?
    
    /**
     * Get count of active sessions
     * @return Number of active inference sessions
     */
    suspend fun getActiveSessionCount(): Int
    
    /**
     * Close an inference session
     * @param sessionId Session identifier
     * @return true if session was closed, false if not found
     */
    suspend fun closeSession(sessionId: String): Boolean
    
    /**
     * Close all active sessions
     */
    suspend fun closeAllSessions()
    
    /**
     * Unload the currently loaded model
     * @return Result indicating success or failure
     */
    suspend fun unloadModel(): Result<Unit>
}
