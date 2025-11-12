package com.nervesparks.iris.app.core

import com.nervesparks.iris.common.models.GenerationParams

/**
 * User input for processing
 */
data class UserInput(
    val text: String,
    val enableRAG: Boolean = false,
    val params: GenerationParams = GenerationParams(),
)

/**
 * Application state
 */
sealed class AppState {
    object Initializing : AppState()
    object Ready : AppState()
    data class Error(val exception: Throwable) : AppState()
}

/**
 * Processing result types
 */
sealed class ProcessingResult {
    object Started : ProcessingResult()
    data class TokenGenerated(val token: String) : ProcessingResult()
    data class Blocked(val reason: String) : ProcessingResult()
    object Completed : ProcessingResult()
    data class Error(val exception: Throwable) : ProcessingResult()
}
