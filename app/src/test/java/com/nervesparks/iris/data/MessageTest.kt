package com.nervesparks.iris.data

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for the Message data class.
 */
class MessageTest {

    @Test
    fun message_creation_withDefaultValues() {
        val content = "Hello, world!"
        val role = MessageRole.USER
        
        val message = Message(
            content = content,
            role = role
        )
        
        assertNotNull(message.id)
        assertEquals(content, message.content)
        assertEquals(role, message.role)
        assertNotNull(message.timestamp)
        assertNull(message.processingTimeMs)
        assertNull(message.tokenCount)
    }

    @Test
    fun message_creation_withAllValues() {
        val id = "test-id-123"
        val content = "AI response"
        val role = MessageRole.ASSISTANT
        val timestamp = Instant.now()
        val processingTime = 1500L
        val tokenCount = 100
        
        val message = Message(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = processingTime,
            tokenCount = tokenCount
        )
        
        assertEquals(id, message.id)
        assertEquals(content, message.content)
        assertEquals(role, message.role)
        assertEquals(timestamp, message.timestamp)
        assertEquals(processingTime, message.processingTimeMs)
        assertEquals(tokenCount, message.tokenCount)
    }

    @Test
    fun message_isFromUser_returnsTrue_whenRoleIsUser() {
        val message = Message(
            content = "User message",
            role = MessageRole.USER
        )
        
        assertTrue(message.isFromUser)
    }

    @Test
    fun message_isFromUser_returnsFalse_whenRoleIsAssistant() {
        val message = Message(
            content = "AI response",
            role = MessageRole.ASSISTANT
        )
        
        assertFalse(message.isFromUser)
    }

    @Test
    fun message_isFromUser_returnsFalse_whenRoleIsSystem() {
        val message = Message(
            content = "System message",
            role = MessageRole.SYSTEM
        )
        
        assertFalse(message.isFromUser)
    }

    @Test
    fun message_equality_sameValues() {
        val id = "same-id"
        val content = "Same content"
        val role = MessageRole.USER
        val timestamp = Instant.now()
        
        val message1 = Message(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp
        )
        
        val message2 = Message(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp
        )
        
        assertEquals(message1, message2)
        assertEquals(message1.hashCode(), message2.hashCode())
    }

    @Test
    fun message_equality_differentIds() {
        val content = "Same content"
        val role = MessageRole.USER
        
        val message1 = Message(
            id = "id-1",
            content = content,
            role = role
        )
        
        val message2 = Message(
            id = "id-2",
            content = content,
            role = role
        )
        
        assertNotEquals(message1, message2)
    }

    @Test
    fun message_copy_modifiesOnlySpecifiedFields() {
        val original = Message(
            content = "Original content",
            role = MessageRole.USER
        )
        
        val modified = original.copy(
            content = "Modified content"
        )
        
        assertEquals("Modified content", modified.content)
        assertEquals(original.id, modified.id)
        assertEquals(original.role, modified.role)
        assertEquals(original.timestamp, modified.timestamp)
    }

    @Test
    fun messageRole_enumValues() {
        val roles = MessageRole.values()
        
        assertEquals(3, roles.size)
        assertTrue(roles.contains(MessageRole.USER))
        assertTrue(roles.contains(MessageRole.ASSISTANT))
        assertTrue(roles.contains(MessageRole.SYSTEM))
    }

    @Test
    fun message_withProcessingMetrics() {
        val message = Message(
            content = "Response",
            role = MessageRole.ASSISTANT,
            processingTimeMs = 2500L,
            tokenCount = 150
        )
        
        assertEquals(2500L, message.processingTimeMs)
        assertEquals(150, message.tokenCount)
    }

    @Test
    fun message_emptyContent() {
        val message = Message(
            content = "",
            role = MessageRole.USER
        )
        
        assertEquals("", message.content)
    }

    @Test
    fun message_longContent() {
        val longContent = "A".repeat(10000)
        val message = Message(
            content = longContent,
            role = MessageRole.ASSISTANT
        )
        
        assertEquals(longContent, message.content)
        assertEquals(10000, message.content.length)
    }

    @Test
    fun message_contentWithSpecialCharacters() {
        val specialContent = "Hello\nWorld\t!\r\n😀"
        val message = Message(
            content = specialContent,
            role = MessageRole.USER
        )
        
        assertEquals(specialContent, message.content)
    }
}
