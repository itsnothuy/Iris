package com.nervesparks.iris.core.llm

/**
 * Parameters for loading a model via JNI
 * @property contextSize Maximum context window size
 * @property threads Number of threads to use
 * @property seed Random seed (-1 for time-based)
 */
data class ModelLoadParams(
    val contextSize: Int,
    val threads: Int,
    val seed: Long
)

/**
 * Exception thrown when LLM operations fail
 */
class LLMException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when text generation fails
 */
class GenerationException(message: String, cause: Throwable? = null) : LLMException(message, cause)

/**
 * Exception thrown when embedding generation fails
 */
class EmbeddingException(message: String, cause: Throwable? = null) : LLMException(message, cause)
