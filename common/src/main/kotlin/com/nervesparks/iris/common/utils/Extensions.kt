package com.nervesparks.iris.common.utils

/**
 * Constant values used across the application
 */
object Constants {
    const val DEFAULT_CONTEXT_SIZE = 4096
    const val DEFAULT_MAX_TOKENS = 512
    const val DEFAULT_TEMPERATURE = 0.7f
    const val DEFAULT_TOP_K = 40
    const val DEFAULT_TOP_P = 0.9f
    const val DEFAULT_REPEAT_PENALTY = 1.1f
    
    const val MIN_RAM_MB = 4096L
    const val RECOMMENDED_RAM_MB = 8192L
    
    const val MODEL_FILE_EXTENSION = ".gguf"
    const val EMBEDDING_DIMENSION = 768
}

/**
 * Extension functions for common operations
 */

/**
 * Convert bytes to megabytes
 */
fun Long.toMB(): Long = this / (1024 * 1024)

/**
 * Convert bytes to gigabytes
 */
fun Long.toGB(): Double = this.toDouble() / (1024 * 1024 * 1024)

/**
 * Check if RAM is sufficient for operation
 */
fun Long.isSufficientRAM(): Boolean = this.toMB() >= Constants.MIN_RAM_MB
