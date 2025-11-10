package com.nervesparks.iris.core.llm.inference

import com.nervesparks.iris.common.models.BackendType

/**
 * Parameters for inference engine initialization
 */
data class InferenceParameters(
    val contextSize: Int = 2048,
    val batchSize: Int = 2,
    val threadsCount: Int = 4,
    val seed: Long = -1L
)

/**
 * Parameters for text generation
 */
data class GenerationParameters(
    val maxTokens: Int = 512,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repeatPenalty: Float = 1.1f,
    val stopSequences: List<String> = emptyList()
)

/**
 * Model descriptor for loading
 */
data class ModelDescriptor(
    val id: String,
    val path: String,
    val name: String,
    val deviceRequirements: DeviceRequirements
)

/**
 * Device requirements for a model
 */
data class DeviceRequirements(
    val minRamMB: Int = 4096,
    val supportedBackends: List<String> = listOf("CPU_NEON", "OPENCL_ADRENO", "VULKAN_MALI", "QNN_HEXAGON")
)

/**
 * Result of model loading operation
 */
data class ModelLoadResult(
    val modelId: String,
    val backend: BackendType,
    val contextSize: Int,
    val loadTime: Long
)

/**
 * Context for an active inference session
 */
data class InferenceSessionContext(
    val sessionId: String,
    val modelId: String,
    val isActive: Boolean,
    val createdAt: Long,
    val lastActivity: Long = createdAt,
    val tokenCount: Int = 0,
    val conversationTurns: Int = 0
)

/**
 * Inference results sealed class hierarchy
 */
sealed class InferenceResult {
    data class GenerationStarted(val sessionId: String) : InferenceResult()
    
    data class TokenGenerated(
        val sessionId: String,
        val token: String,
        val partialText: String,
        val tokenIndex: Int,
        val confidence: Float = 1.0f
    ) : InferenceResult()
    
    data class GenerationCompleted(
        val sessionId: String,
        val fullText: String,
        val tokenCount: Int,
        val generationTime: Long,
        val tokensPerSecond: Double,
        val finishReason: FinishReason
    ) : InferenceResult()
    
    data class SafetyViolation(val reason: String) : InferenceResult()
    
    data class Error(
        val sessionId: String,
        val error: String,
        val cause: Throwable? = null
    ) : InferenceResult()
}

/**
 * Reason for generation completion
 */
enum class FinishReason {
    COMPLETED, 
    MAX_TOKENS, 
    STOP_SEQUENCE, 
    SAFETY_FILTER, 
    ERROR
}

/**
 * Native token generation results
 */
sealed class NativeTokenResult {
    data class Token(val text: String, val confidence: Float = 1.0f) : NativeTokenResult()
    data class Finished(val reason: FinishReason) : NativeTokenResult()
    data class Error(val message: String, val cause: Throwable? = null) : NativeTokenResult()
}

/**
 * Exception for inference operations
 */
class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Session context for tracking conversation state
 */
internal data class SessionContext(
    val conversationId: String,
    val modelId: String,
    val createdAt: Long,
    var lastActivity: Long,
    var tokenCount: Int,
    val conversationHistory: MutableList<ConversationExchange>,
    var systemPrompt: String?
)

/**
 * Single conversation exchange
 */
internal data class ConversationExchange(
    val userMessage: String,
    val assistantResponse: String,
    val timestamp: Long,
    val tokenCount: Int
)
