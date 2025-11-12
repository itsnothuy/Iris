package com.nervesparks.iris.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PrivacyGuard scrubber functionality.
 * Tests email, phone number, and ID redaction patterns.
 */
class PrivacyGuardTest {

    @Test
    fun redactPII_withEmail_redactsEmail() {
        val input = "Contact me at john.doe@example.com for details"
        val result = PrivacyGuard.redactPII(input)

        assertEquals("Contact me at [EMAIL_REDACTED] for details", result.redactedText)
        assertTrue(result.wasRedacted)
        assertEquals(1, result.redactionCount)
    }

    @Test
    fun redactPII_withMultipleEmails_redactsAllEmails() {
        val input = "Email john@test.com or jane@example.org"
        val result = PrivacyGuard.redactPII(input)

        assertEquals("Email [EMAIL_REDACTED] or [EMAIL_REDACTED]", result.redactedText)
        assertTrue(result.wasRedacted)
        assertEquals(2, result.redactionCount)
    }

    @Test
    fun redactPII_withUSPhone_redactsPhone() {
        val input = "Call me at 555-123-4567"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[PHONE_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withParenthesesPhone_redactsPhone() {
        val input = "My number is (555) 123-4567"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[PHONE_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withInternationalPhone_redactsPhone() {
        val input = "Call +1-555-123-4567 for support"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[PHONE_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withSSN_redactsID() {
        val input = "My SSN is 123-45-6789"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[ID_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withCreditCard_redactsID() {
        val input = "Card number: 1234-5678-9012-3456"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[ID_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withLongNumberSequence_redactsID() {
        val input = "Account ID: 12345678901"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[ID_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount > 0)
    }

    @Test
    fun redactPII_withMixedPII_redactsAll() {
        val input = "Contact john@test.com or call 555-1234 with ID 123456789"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.contains("[EMAIL_REDACTED]"))
        assertTrue(result.redactedText.contains("[PHONE_REDACTED]") || result.redactedText.contains("[ID_REDACTED]"))
        assertTrue(result.wasRedacted)
        assertTrue(result.redactionCount >= 2)
    }

    @Test
    fun redactPII_withNoPII_returnsOriginalText() {
        val input = "Hello, how can I help you today?"
        val result = PrivacyGuard.redactPII(input)

        assertEquals(input, result.redactedText)
        assertFalse(result.wasRedacted)
        assertEquals(0, result.redactionCount)
    }

    @Test
    fun redactPII_withEmptyString_returnsEmptyResult() {
        val input = ""
        val result = PrivacyGuard.redactPII(input)

        assertEquals("", result.redactedText)
        assertFalse(result.wasRedacted)
        assertEquals(0, result.redactionCount)
    }

    @Test
    fun containsPII_withEmail_returnsTrue() {
        val input = "My email is test@example.com"
        assertTrue(PrivacyGuard.containsPII(input))
    }

    @Test
    fun containsPII_withPhone_returnsTrue() {
        val input = "Call 555-123-4567"
        assertTrue(PrivacyGuard.containsPII(input))
    }

    @Test
    fun containsPII_withSSN_returnsTrue() {
        val input = "SSN: 123-45-6789"
        assertTrue(PrivacyGuard.containsPII(input))
    }

    @Test
    fun containsPII_withNoPII_returnsFalse() {
        val input = "Just a normal message"
        assertFalse(PrivacyGuard.containsPII(input))
    }

    @Test
    fun containsPII_withShortNumber_returnsFalse() {
        val input = "I have 123 items"
        assertFalse(PrivacyGuard.containsPII(input))
    }

    @Test
    fun redactPII_preservesContextAroundRedactions() {
        val input = "Please send documents to alice@company.com by Friday"
        val result = PrivacyGuard.redactPII(input)

        assertTrue(result.redactedText.startsWith("Please send documents to"))
        assertTrue(result.redactedText.endsWith("by Friday"))
        assertTrue(result.redactedText.contains("[EMAIL_REDACTED]"))
    }
}
