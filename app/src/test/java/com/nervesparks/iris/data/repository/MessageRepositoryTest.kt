package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.local.AppDatabase
import com.nervesparks.iris.data.local.MessageDao
import com.nervesparks.iris.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit tests for MessageRepository.
 * Tests CRUD operations and Flow conversions.
 */
class MessageRepositoryTest {

    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockDao: MessageDao
    private lateinit var repository: MessageRepository

    @Before
    fun setup() {
        mockDatabase = mock()
        mockDao = mock()
        whenever(mockDatabase.messageDao()).thenReturn(mockDao)
        repository = MessageRepository(mockDatabase)
    }

    @Test
    fun saveMessage_callsDaoInsert() = runTest {
        val message = Message(
            id = "test-id",
            content = "Test message",
            role = MessageRole.USER,
            timestamp = Instant.now()
        )
        
        repository.saveMessage(message)
        
        verify(mockDao).insertMessage(any())
    }

    @Test
    fun saveMessages_callsDaoInsertMultiple() = runTest {
        val messages = listOf(
            Message(
                id = "id-1",
                content = "Message 1",
                role = MessageRole.USER
            ),
            Message(
                id = "id-2",
                content = "Message 2",
                role = MessageRole.ASSISTANT
            )
        )
        
        repository.saveMessages(messages)
        
        verify(mockDao).insertMessages(any())
    }

    @Test
    fun getAllMessages_returnsFlowOfMessages() = runTest {
        val entities = listOf(
            MessageEntity(
                id = "id-1",
                content = "Message 1",
                role = "USER",
                timestamp = Instant.now().toEpochMilli()
            )
        )
        val flow: Flow<List<MessageEntity>> = flowOf(entities)
        whenever(mockDao.getAllMessages()).thenReturn(flow)
        
        val result = repository.getAllMessages()
        
        assertNotNull(result)
        verify(mockDao).getAllMessages()
    }

    @Test
    fun getAllMessagesList_returnsListOfMessages() = runTest {
        val timestamp = Instant.now()
        val entities = listOf(
            MessageEntity(
                id = "id-1",
                content = "Message 1",
                role = "USER",
                timestamp = timestamp.toEpochMilli()
            ),
            MessageEntity(
                id = "id-2",
                content = "Message 2",
                role = "ASSISTANT",
                timestamp = timestamp.plusSeconds(1).toEpochMilli()
            )
        )
        whenever(mockDao.getAllMessagesList()).thenReturn(entities)
        
        val result = repository.getAllMessagesList()
        
        assertEquals(2, result.size)
        assertEquals("id-1", result[0].id)
        assertEquals("Message 1", result[0].content)
        assertEquals(MessageRole.USER, result[0].role)
        verify(mockDao).getAllMessagesList()
    }

    @Test
    fun deleteAllMessages_callsDaoDelete() = runTest {
        repository.deleteAllMessages()
        
        verify(mockDao).deleteAllMessages()
    }

    @Test
    fun deleteMessage_callsDaoDeleteWithId() = runTest {
        val messageId = "test-id-123"
        
        repository.deleteMessage(messageId)
        
        verify(mockDao).deleteMessage(messageId)
    }

    @Test
    fun getMessageCount_returnsDaoCount() = runTest {
        val expectedCount = 42
        whenever(mockDao.getMessageCount()).thenReturn(expectedCount)
        
        val result = repository.getMessageCount()
        
        assertEquals(expectedCount, result)
        verify(mockDao).getMessageCount()
    }

    @Test
    fun saveMessage_withProcessingMetrics_storesCorrectly() = runTest {
        val message = Message(
            id = "metric-id",
            content = "Response",
            role = MessageRole.ASSISTANT,
            processingTimeMs = 1500L,
            tokenCount = 100
        )
        
        repository.saveMessage(message)
        
        verify(mockDao).insertMessage(any())
    }

    @Test
    fun saveMessages_withEmptyList_callsDao() = runTest {
        val emptyList = emptyList<Message>()
        
        repository.saveMessages(emptyList)
        
        verify(mockDao).insertMessages(emptyList())
    }

    @Test
    fun getAllMessagesList_withEmptyDatabase_returnsEmptyList() = runTest {
        whenever(mockDao.getAllMessagesList()).thenReturn(emptyList())
        
        val result = repository.getAllMessagesList()
        
        assertTrue(result.isEmpty())
        verify(mockDao).getAllMessagesList()
    }

    @Test
    fun saveMessage_withSystemRole_storesCorrectly() = runTest {
        val message = Message(
            id = "system-id",
            content = "System message",
            role = MessageRole.SYSTEM
        )
        
        repository.saveMessage(message)
        
        verify(mockDao).insertMessage(any())
    }

    @Test
    fun deleteMessage_withNonExistentId_callsDao() = runTest {
        val nonExistentId = "non-existent-id"
        
        repository.deleteMessage(nonExistentId)
        
        verify(mockDao).deleteMessage(nonExistentId)
    }

    @Test
    fun getMessageCount_withNoMessages_returnsZero() = runTest {
        val expectedCount = 0
        whenever(mockDao.getMessageCount()).thenReturn(expectedCount)
        
        val result = repository.getMessageCount()
        
        assertEquals(0, result)
        verify(mockDao).getMessageCount()
    }

    @Test
    fun saveMessages_withMultipleRoles_storesAll() = runTest {
        val messages = listOf(
            Message(content = "User msg", role = MessageRole.USER),
            Message(content = "System msg", role = MessageRole.SYSTEM),
            Message(content = "Assistant msg", role = MessageRole.ASSISTANT)
        )
        
        repository.saveMessages(messages)
        
        verify(mockDao).insertMessages(any())
    }
}
