package com.nervesparks.iris.core.safety

import com.nervesparks.iris.common.config.SafetyLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-ready implementation of SafetyEngine
 * Uses rule-based filtering for content safety checks
 */
@Singleton
class SafetyEngineImpl @Inject constructor() : SafetyEngine {
    
    private var currentSafetyLevel: SafetyLevel = SafetyLevel.MEDIUM
    
    // Prompt injection patterns - common jailbreak attempts
    private val promptInjectionPatterns = listOf(
        // Direct instruction override attempts
        "ignore previous instructions",
        "ignore all previous",
        "disregard previous",
        "forget previous",
        "new instructions:",
        "system:",
        "admin:",
        "developer mode",
        "god mode",
        // Role-play attempts
        "pretend you are",
        "act as if",
        "roleplay as",
        "simulate being",
        // System prompt extraction
        "repeat your instructions",
        "what are your rules",
        "show your prompt",
        "reveal your system",
        // Encoding tricks
        "base64:",
        "rot13:",
        "\\x",
        "\\u",
        // Delimiter confusion
        "---END INSTRUCTIONS---",
        "###SYSTEM###",
        "<<<ADMIN>>>",
    )
    
    // Harmful content patterns
    private val harmfulContentPatterns = mapOf(
        "violence" to listOf(
            "how to kill",
            "how to murder",
            "build a bomb",
            "make explosives",
            "create weapon",
            "hurt someone",
            "attack plan",
        ),
        "self_harm" to listOf(
            "how to commit suicide",
            "ways to die",
            "self-harm methods",
            "end my life",
            "kill myself",
        ),
        "hate_speech" to listOf(
            "racial slurs",
            "ethnic hatred",
            "discriminatory language",
            // Note: Actual implementation would include specific slurs
            // Omitted here for brevity
        ),
        "illegal_activity" to listOf(
            "how to steal",
            "hack into",
            "break into",
            "illegal drugs",
            "forge documents",
            "commit fraud",
            "launder money",
        ),
        "privacy_violation" to listOf(
            "ssn:",
            "social security",
            "credit card:",
            "password:",
            "access token",
            "api key:",
        ),
    )
    
    // Output safety patterns - check model responses
    private val unsafeOutputPatterns = listOf(
        // Leaked system information
        "as a language model",
        "i cannot actually",
        "i don't have the ability to",
        "my training data",
        // Hallucinated credentials
        "username:",
        "password:",
        "token:",
        // Harmful instructions in response
        "step 1:",
        "first, you should",
    )
    
    override suspend fun checkInput(text: String): SafetyResult {
        val normalizedText = text.lowercase().trim()
        
        // NONE level - no filtering
        if (currentSafetyLevel == SafetyLevel.NONE) {
            return SafetyResult(isAllowed = true)
        }
        
        // Check for prompt injection at all levels except NONE
        val promptInjectionResult = detectPromptInjection(normalizedText)
        if (!promptInjectionResult.isAllowed) {
            return promptInjectionResult
        }
        
        // Privacy violation checks at LOW and above
        if (currentSafetyLevel != SafetyLevel.NONE) {
            val privacyResult = detectPrivacyViolation(normalizedText)
            if (!privacyResult.isAllowed) {
                return privacyResult
            }
        }
        
        // Harmful content checks at MEDIUM and above
        if (currentSafetyLevel == SafetyLevel.MEDIUM || currentSafetyLevel == SafetyLevel.HIGH) {
            val harmfulResult = detectHarmfulContent(normalizedText)
            if (!harmfulResult.isAllowed) {
                return harmfulResult
            }
        }
        
        return SafetyResult(isAllowed = true)
    }
    
    override suspend fun checkOutput(text: String): SafetyResult {
        val normalizedText = text.lowercase().trim()
        
        // NONE and LOW levels - no output filtering
        if (currentSafetyLevel == SafetyLevel.NONE || currentSafetyLevel == SafetyLevel.LOW) {
            return SafetyResult(isAllowed = true)
        }
        
        // Check for unsafe patterns in model output
        for (pattern in unsafeOutputPatterns) {
            if (normalizedText.contains(pattern)) {
                return SafetyResult(
                    isAllowed = false,
                    reason = "Output contains potentially unsafe content",
                    confidence = 0.8f
                )
            }
        }
        
        // Check for harmful content in output at HIGH level
        if (currentSafetyLevel == SafetyLevel.HIGH) {
            val harmfulResult = detectHarmfulContent(normalizedText)
            if (!harmfulResult.isAllowed) {
                return harmfulResult
            }
        }
        
        return SafetyResult(isAllowed = true)
    }
    
    override fun updateSafetyLevel(level: SafetyLevel) {
        currentSafetyLevel = level
    }
    
    override fun getSafetyLevel(): SafetyLevel {
        return currentSafetyLevel
    }
    
    private fun detectPromptInjection(text: String): SafetyResult {
        for (pattern in promptInjectionPatterns) {
            if (text.contains(pattern)) {
                return SafetyResult(
                    isAllowed = false,
                    reason = "Potential prompt injection detected",
                    confidence = 0.9f
                )
            }
        }
        return SafetyResult(isAllowed = true)
    }
    
    private fun detectHarmfulContent(text: String): SafetyResult {
        for ((category, patterns) in harmfulContentPatterns) {
            for (pattern in patterns) {
                if (text.contains(pattern)) {
                    return SafetyResult(
                        isAllowed = false,
                        reason = "Potentially harmful content detected: $category",
                        confidence = 0.85f
                    )
                }
            }
        }
        return SafetyResult(isAllowed = true)
    }
    
    private fun detectPrivacyViolation(text: String): SafetyResult {
        val privacyPatterns = harmfulContentPatterns["privacy_violation"] ?: emptyList()
        for (pattern in privacyPatterns) {
            if (text.contains(pattern)) {
                return SafetyResult(
                    isAllowed = false,
                    reason = "Potential privacy violation detected",
                    confidence = 0.75f
                )
            }
        }
        return SafetyResult(isAllowed = true)
    }
}
