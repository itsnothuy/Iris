package com.nervesparks.iris.data.local

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for MessageEntity data class.
 * Tests creation, equality, and field values.
 */
class MessageEntityTest {

    @Test
    fun messageEntity_creation_withAllFields() {
        val id = "test-id-123"
        val content = "Test message content"
        val role = "USER"
        val timestamp = Instant.now().toEpochMilli()
        val processingTimeMs = 1500L
        val tokenCount = 100
        
        val entity = MessageEntity(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = processingTimeMs,
            tokenCount = tokenCount
        )
        
        assertEquals(id, entity.id)
        assertEquals(content, entity.content)
        assertEquals(role, entity.role)
        assertEquals(timestamp, entity.timestamp)
        assertEquals(processingTimeMs, entity.processingTimeMs)
        assertEquals(tokenCount, entity.tokenCount)
    }

    @Test
    fun messageEntity_creation_withNullOptionalFields() {
        val id = "test-id"
        val content = "Message"
        val role = "ASSISTANT"
        val timestamp = Instant.now().toEpochMilli()
        
        val entity = MessageEntity(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals(id, entity.id)
        assertEquals(content, entity.content)
        assertEquals(role, entity.role)
        assertEquals(timestamp, entity.timestamp)
        assertNull(entity.processingTimeMs)
        assertNull(entity.tokenCount)
    }

    @Test
    fun messageEntity_equality_sameValues() {
        val id = "same-id"
        val content = "Same content"
        val role = "USER"
        val timestamp = 1234567890L
        
        val entity1 = MessageEntity(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = null,
            tokenCount = null
        )
        
        val entity2 = MessageEntity(
            id = id,
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals(entity1, entity2)
        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun messageEntity_equality_differentIds() {
        val content = "Same content"
        val role = "USER"
        val timestamp = Instant.now().toEpochMilli()
        
        val entity1 = MessageEntity(
            id = "id-1",
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = null,
            tokenCount = null
        )
        
        val entity2 = MessageEntity(
            id = "id-2",
            content = content,
            role = role,
            timestamp = timestamp,
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertNotEquals(entity1, entity2)
    }

    @Test
    fun messageEntity_copy_modifiesOnlySpecifiedFields() {
        val original = MessageEntity(
            id = "original-id",
            content = "Original content",
            role = "USER",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = 1000L,
            tokenCount = 50
        )
        
        val modified = original.copy(content = "Modified content")
        
        assertEquals("Modified content", modified.content)
        assertEquals(original.id, modified.id)
        assertEquals(original.role, modified.role)
        assertEquals(original.timestamp, modified.timestamp)
        assertEquals(original.processingTimeMs, modified.processingTimeMs)
        assertEquals(original.tokenCount, modified.tokenCount)
    }

    @Test
    fun messageEntity_withEmptyContent() {
        val entity = MessageEntity(
            id = "id",
            content = "",
            role = "USER",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals("", entity.content)
    }

    @Test
    fun messageEntity_withLongContent() {
        val longContent = "A".repeat(10000)
        val entity = MessageEntity(
            id = "id",
            content = longContent,
            role = "ASSISTANT",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals(longContent, entity.content)
        assertEquals(10000, entity.content.length)
    }

    @Test
    fun messageEntity_withSpecialCharacters() {
        val specialContent = "Hello\nWorld\t!\r\n😀"
        val entity = MessageEntity(
            id = "id",
            content = specialContent,
            role = "USER",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals(specialContent, entity.content)
    }

    @Test
    fun messageEntity_withAllRoleTypes() {
        val timestamp = Instant.now().toEpochMilli()
        
        val userEntity = MessageEntity("1", "User msg", "USER", timestamp, null, null)
        val assistantEntity = MessageEntity("2", "Assistant msg", "ASSISTANT", timestamp, null, null)
        val systemEntity = MessageEntity("3", "System msg", "SYSTEM", timestamp, null, null)
        
        assertEquals("USER", userEntity.role)
        assertEquals("ASSISTANT", assistantEntity.role)
        assertEquals("SYSTEM", systemEntity.role)
    }

    @Test
    fun messageEntity_withZeroTimestamp() {
        val entity = MessageEntity(
            id = "id",
            content = "Message",
            role = "USER",
            timestamp = 0L,
            processingTimeMs = null,
            tokenCount = null
        )
        
        assertEquals(0L, entity.timestamp)
    }

    @Test
    fun messageEntity_withNegativeProcessingTime() {
        val entity = MessageEntity(
            id = "id",
            content = "Message",
            role = "ASSISTANT",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = -1L,
            tokenCount = null
        )
        
        assertEquals(-1L, entity.processingTimeMs)
    }

    @Test
    fun messageEntity_withZeroTokenCount() {
        val entity = MessageEntity(
            id = "id",
            content = "Message",
            role = "ASSISTANT",
            timestamp = Instant.now().toEpochMilli(),
            processingTimeMs = 100L,
            tokenCount = 0
        )
        
        assertEquals(0, entity.tokenCount)
    }

    @Test
    fun messageEntity_toString_containsAllFields() {
        val entity = MessageEntity(
            id = "test-id",
            content = "Test content",
            role = "USER",
            timestamp = 1234567890L,
            processingTimeMs = 500L,
            tokenCount = 25
        )
        
        val result = entity.toString()
        
        assertTrue(result.contains("test-id"))
        assertTrue(result.contains("Test content"))
        assertTrue(result.contains("USER"))
        assertTrue(result.contains("1234567890"))
        assertTrue(result.contains("500"))
        assertTrue(result.contains("25"))
    }
}
