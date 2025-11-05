package com.nervesparks.iris.data.local

import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/**
 * Unit tests for MessageMapper.
 */
class MessageMapperTest {
    
    @Test
    fun toEntity_convertsMessageToEntity_correctly() {
        val timestamp = Instant.now()
        val message = Message(
            id = "test-id",
            content = "Test content",
            role = MessageRole.USER,
            timestamp = timestamp,
            processingTimeMs = 1000L,
            tokenCount = 50
        )
        
        val entity = MessageMapper.toEntity(message)
        
        assertEquals(message.id, entity.id)
        assertEquals(message.content, entity.content)
        assertEquals(message.role.name, entity.role)
        assertEquals(message.timestamp.toEpochMilli(), entity.timestamp)
        assertEquals(message.processingTimeMs, entity.processingTimeMs)
        assertEquals(message.tokenCount, entity.tokenCount)
        assertEquals("default", entity.conversationId) // Default conversationId
    }
    
    @Test
    fun toEntity_withCustomConversationId_usesProvidedId() {
        val message = Message(
            id = "test-id",
            content = "Test content",
            role = MessageRole.USER
        )
        
        val entity = MessageMapper.toEntity(message, "custom-conversation-id")
        
        assertEquals("custom-conversation-id", entity.conversationId)
    }
    
    @Test
    fun toDomain_convertsEntityToMessage_correctly() {
        val timestampMillis = System.currentTimeMillis()
        val entity = MessageEntity(
            id = "test-id",
            content = "Test content",
            role = "ASSISTANT",
            timestamp = timestampMillis,
            processingTimeMs = 2000L,
            tokenCount = 100
        )
        
        val message = MessageMapper.toDomain(entity)
        
        assertEquals(entity.id, message.id)
        assertEquals(entity.content, message.content)
        assertEquals(MessageRole.ASSISTANT, message.role)
        assertEquals(Instant.ofEpochMilli(timestampMillis), message.timestamp)
        assertEquals(entity.processingTimeMs, message.processingTimeMs)
        assertEquals(entity.tokenCount, message.tokenCount)
    }
    
    @Test
    fun toEntity_withNullOptionalFields() {
        val message = Message(
            content = "Test",
            role = MessageRole.USER,
            processingTimeMs = null,
            tokenCount = null
        )
        
        val entity = MessageMapper.toEntity(message)
        
        assertNull(entity.processingTimeMs)
        assertNull(entity.tokenCount)
    }
    
    @Test
    fun toDomain_withNullOptionalFields() {
        val entity = MessageEntity(
            id = "test-id",
            content = "Test",
            role = "SYSTEM",
            timestamp = System.currentTimeMillis(),
            processingTimeMs = null,
            tokenCount = null
        )
        
        val message = MessageMapper.toDomain(entity)
        
        assertNull(message.processingTimeMs)
        assertNull(message.tokenCount)
        assertEquals(MessageRole.SYSTEM, message.role)
    }
    
    @Test
    fun roundTrip_preservesAllData() {
        val originalMessage = Message(
            id = "original-id",
            content = "Original content",
            role = MessageRole.ASSISTANT,
            timestamp = Instant.now(),
            processingTimeMs = 1500L,
            tokenCount = 75
        )
        
        val entity = MessageMapper.toEntity(originalMessage)
        val restoredMessage = MessageMapper.toDomain(entity)
        
        assertEquals(originalMessage.id, restoredMessage.id)
        assertEquals(originalMessage.content, restoredMessage.content)
        assertEquals(originalMessage.role, restoredMessage.role)
        assertEquals(originalMessage.processingTimeMs, restoredMessage.processingTimeMs)
        assertEquals(originalMessage.tokenCount, restoredMessage.tokenCount)
        // Note: Timestamps lose nanosecond precision when converted to milliseconds
        assertEquals(
            originalMessage.timestamp.toEpochMilli(),
            restoredMessage.timestamp.toEpochMilli()
        )
    }
    
    @Test
    fun toDomainList_convertsMultipleEntities() {
        val entities = listOf(
            MessageEntity("1", "Content 1", "USER", System.currentTimeMillis(), null, null),
            MessageEntity("2", "Content 2", "ASSISTANT", System.currentTimeMillis(), 1000L, 50),
            MessageEntity("3", "Content 3", "SYSTEM", System.currentTimeMillis(), null, null)
        )
        
        val messages = MessageMapper.toDomainList(entities)
        
        assertEquals(3, messages.size)
        assertEquals("Content 1", messages[0].content)
        assertEquals(MessageRole.USER, messages[0].role)
        assertEquals("Content 2", messages[1].content)
        assertEquals(MessageRole.ASSISTANT, messages[1].role)
        assertEquals("Content 3", messages[2].content)
        assertEquals(MessageRole.SYSTEM, messages[2].role)
    }
    
    @Test
    fun toEntityList_convertsMultipleMessages() {
        val messages = listOf(
            Message(content = "Message 1", role = MessageRole.USER),
            Message(content = "Message 2", role = MessageRole.ASSISTANT),
            Message(content = "Message 3", role = MessageRole.SYSTEM)
        )
        
        val entities = MessageMapper.toEntityList(messages)
        
        assertEquals(3, entities.size)
        assertEquals("Message 1", entities[0].content)
        assertEquals("USER", entities[0].role)
        assertEquals("Message 2", entities[1].content)
        assertEquals("ASSISTANT", entities[1].role)
        assertEquals("Message 3", entities[2].content)
        assertEquals("SYSTEM", entities[2].role)
    }
    
    @Test
    fun toEntity_handlesSpecialCharacters() {
        val message = Message(
            content = "Special: \n\t\r😀",
            role = MessageRole.USER
        )
        
        val entity = MessageMapper.toEntity(message)
        
        assertEquals("Special: \n\t\r😀", entity.content)
    }
    
    @Test
    fun toDomain_handlesAllMessageRoles() {
        val roles = listOf("USER", "ASSISTANT", "SYSTEM")
        
        roles.forEach { roleString ->
            val entity = MessageEntity(
                id = "test-$roleString",
                content = "Test",
                role = roleString,
                timestamp = System.currentTimeMillis(),
                processingTimeMs = null,
                tokenCount = null
            )
            
            val message = MessageMapper.toDomain(entity)
            assertEquals(MessageRole.valueOf(roleString), message.role)
        }
    }
}
