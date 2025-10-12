package com.nervesparks.iris.util

/**
 * Utility class for redacting personally identifiable information (PII) from text.
 * Detects and replaces emails, phone numbers, and ID-like patterns before sending
 * messages to the AI model.
 */
object PrivacyGuard {
    
    // Email pattern: basic email format validation
    private val emailPattern = Regex(
        pattern = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        option = RegexOption.IGNORE_CASE
    )
    
    // Phone number patterns: various formats including international
    private val phonePatterns = listOf(
        // US/International: +1-234-567-8900, +1 (234) 567-8900, +12345678900
        Regex("\\+?\\d{1,3}[-\\s.]?\\(?\\d{1,4}\\)?[-\\s.]?\\d{1,4}[-\\s.]?\\d{1,9}"),
        // Simple: 123-456-7890, (123) 456-7890, 1234567890
        Regex("\\(?\\d{3}\\)?[-\\s.]?\\d{3}[-\\s.]?\\d{4}"),
        // International with country code: +44 20 1234 5678
        Regex("\\+\\d{1,3}\\s?\\d{1,4}\\s?\\d{1,4}\\s?\\d{1,9}")
    )
    
    // ID-like patterns: SSN, credit card, account numbers
    private val idPatterns = listOf(
        // SSN: 123-45-6789 or 123456789
        Regex("\\b\\d{3}-?\\d{2}-?\\d{4}\\b"),
        // Credit card: 16 digits with optional spaces/hyphens
        Regex("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b"),
        // Generic ID: sequences of 8+ digits
        Regex("\\b\\d{8,}\\b")
    )
    
    /**
     * Data class representing redaction results.
     * 
     * @param redactedText The text with PII replaced by placeholders
     * @param wasRedacted Whether any redactions were performed
     * @param redactionCount Number of items redacted
     */
    data class RedactionResult(
        val redactedText: String,
        val wasRedacted: Boolean,
        val redactionCount: Int
    )
    
    /**
     * Redacts PII from the input text.
     * Replaces emails, phone numbers, and ID-like patterns with placeholders.
     * 
     * @param text The input text to redact
     * @return RedactionResult containing the redacted text and metadata
     */
    fun redactPII(text: String): RedactionResult {
        var redactedText = text
        var redactionCount = 0
        
        // Redact emails
        val emailMatches = emailPattern.findAll(redactedText).count()
        if (emailMatches > 0) {
            redactedText = emailPattern.replace(redactedText, "[EMAIL_REDACTED]")
            redactionCount += emailMatches
        }
        
        // Redact phone numbers
        phonePatterns.forEach { pattern ->
            val phoneMatches = pattern.findAll(redactedText).count()
            if (phoneMatches > 0) {
                redactedText = pattern.replace(redactedText, "[PHONE_REDACTED]")
                redactionCount += phoneMatches
            }
        }
        
        // Redact ID-like patterns
        idPatterns.forEach { pattern ->
            val idMatches = pattern.findAll(redactedText).count()
            if (idMatches > 0) {
                redactedText = pattern.replace(redactedText, "[ID_REDACTED]")
                redactionCount += idMatches
            }
        }
        
        return RedactionResult(
            redactedText = redactedText,
            wasRedacted = redactionCount > 0,
            redactionCount = redactionCount
        )
    }
    
    /**
     * Checks if the text contains any PII without redacting it.
     * Useful for warning users before they send sensitive information.
     * 
     * @param text The text to check
     * @return true if PII is detected, false otherwise
     */
    fun containsPII(text: String): Boolean {
        // Check for emails
        if (emailPattern.containsMatchIn(text)) return true
        
        // Check for phone numbers
        if (phonePatterns.any { it.containsMatchIn(text) }) return true
        
        // Check for IDs
        if (idPatterns.any { it.containsMatchIn(text) }) return true
        
        return false
    }
}
