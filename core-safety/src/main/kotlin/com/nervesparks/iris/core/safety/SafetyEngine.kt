package com.nervesparks.iris.core.safety

import com.nervesparks.iris.common.config.SafetyLevel

/**
 * Safety check result
 */
data class SafetyResult(
    val isAllowed: Boolean,
    val reason: String? = null,
    val confidence: Float = 1.0f
)

/**
 * Interface for content safety checks
 */
interface SafetyEngine {
    /**
     * Check if input text is safe
     */
    suspend fun checkInput(text: String): SafetyResult
    
    /**
     * Check if output text is safe
     */
    suspend fun checkOutput(text: String): SafetyResult
    
    /**
     * Update safety level
     */
    fun updateSafetyLevel(level: SafetyLevel)
    
    /**
     * Get current safety level
     */
    fun getSafetyLevel(): SafetyLevel
}
