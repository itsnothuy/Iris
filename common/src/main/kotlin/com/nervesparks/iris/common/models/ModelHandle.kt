package com.nervesparks.iris.common.models

/**
 * Handle for a loaded AI model
 * @property id Unique identifier for this model instance
 * @property modelPath File path to the model
 * @property contextSize Maximum context window size
 * @property vocabSize Vocabulary size of the model
 * @property backend Backend type being used for inference
 */
data class ModelHandle(
    val id: String,
    val modelPath: String,
    val contextSize: Int,
    val vocabSize: Int,
    val backend: BackendType
)

/**
 * Information about a loaded model
 * @property name Model name
 * @property parameterCount Parameter count (e.g., "7B")
 * @property contextSize Context window size
 * @property vocabSize Vocabulary size
 */
data class ModelInfo(
    val name: String,
    val parameterCount: String,
    val contextSize: Int,
    val vocabSize: Int
)

/**
 * Parameters for text generation
 */
data class GenerationParams(
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val maxTokens: Int = 512,
    val stopTokens: List<String> = emptyList(),
    val repeatPenalty: Float = 1.1f,
    val seed: Long = -1L
)

/**
 * Backend types for AI inference
 */
enum class BackendType {
    CPU_NEON,
    OPENCL_ADRENO,
    VULKAN_MALI,
    QNN_HEXAGON
}

/**
 * Compute task types for backend routing
 */
enum class ComputeTask {
    LLM_INFERENCE,
    EMBEDDING_GENERATION,
    SAFETY_CHECK,
    ASR_TRANSCRIPTION
}
