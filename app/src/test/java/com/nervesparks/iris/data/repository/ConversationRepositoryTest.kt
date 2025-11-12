package com.nervesparks.iris.data.repository

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.local.AppDatabase
import com.nervesparks.iris.data.local.ConversationDao
import com.nervesparks.iris.data.local.ConversationEntity
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
 * Unit tests for ConversationRepository.
 * Tests CRUD operations and Flow conversions.
 */
class ConversationRepositoryTest {

    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockDao: ConversationDao
    private lateinit var repository: ConversationRepository

    @Before
    fun setup() {
        mockDatabase = mock()
        mockDao = mock()
        whenever(mockDatabase.conversationDao()).thenReturn(mockDao)
        repository = ConversationRepository(mockDatabase)
    }

    @Test
    fun createConversation_callsDaoInsert() = runTest {
        val conversation = Conversation(
            id = "test-id",
            title = "Test Conversation",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
        )

        repository.createConversation(conversation)

        verify(mockDao).insertConversation(any())
    }

    @Test
    fun updateConversation_callsDaoUpdate() = runTest {
        val conversation = Conversation(
            id = "test-id",
            title = "Updated Conversation",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
        )

        repository.updateConversation(conversation)

        verify(mockDao).updateConversation(any())
    }

    @Test
    fun getAllConversations_returnsFlowOfConversations() = runTest {
        val timestamp = Instant.now()
        val entities = listOf(
            ConversationEntity(
                id = "id-1",
                title = "Conversation 1",
                createdAt = timestamp.toEpochMilli(),
                lastModified = timestamp.toEpochMilli(),
                messageCount = 5,
                isPinned = false,
                isArchived = false,
            ),
        )
        val flow: Flow<List<ConversationEntity>> = flowOf(entities)
        whenever(mockDao.getAllConversations()).thenReturn(flow)

        val result = repository.getAllConversations()

        assertNotNull(result)
        verify(mockDao).getAllConversations()
    }

    @Test
    fun getConversationById_returnsConversation() = runTest {
        val timestamp = Instant.now()
        val entity = ConversationEntity(
            id = "test-id",
            title = "Test Conversation",
            createdAt = timestamp.toEpochMilli(),
            lastModified = timestamp.toEpochMilli(),
            messageCount = 3,
            isPinned = false,
            isArchived = false,
        )
        whenever(mockDao.getConversationById("test-id")).thenReturn(entity)

        val result = repository.getConversationById("test-id")

        assertNotNull(result)
        assertEquals("test-id", result?.id)
        assertEquals("Test Conversation", result?.title)
        verify(mockDao).getConversationById("test-id")
    }

    @Test
    fun getConversationById_withNonExistentId_returnsNull() = runTest {
        whenever(mockDao.getConversationById("non-existent")).thenReturn(null)

        val result = repository.getConversationById("non-existent")

        assertNull(result)
        verify(mockDao).getConversationById("non-existent")
    }

    @Test
    fun deleteConversation_callsDaoDelete() = runTest {
        val conversationId = "test-id-123"

        repository.deleteConversation(conversationId)

        verify(mockDao).deleteConversation(conversationId)
    }

    @Test
    fun deleteConversations_callsDaoDeleteMultiple() = runTest {
        val conversationIds = listOf("id-1", "id-2", "id-3")

        repository.deleteConversations(conversationIds)

        verify(mockDao).deleteConversations(conversationIds)
    }

    @Test
    fun deleteAllConversations_callsDaoDeleteAll() = runTest {
        repository.deleteAllConversations()

        verify(mockDao).deleteAllConversations()
    }

    @Test
    fun getConversationCount_returnsDaoCount() = runTest {
        val expectedCount = 10
        whenever(mockDao.getConversationCount()).thenReturn(expectedCount)

        val result = repository.getConversationCount()

        assertEquals(expectedCount, result)
        verify(mockDao).getConversationCount()
    }

    @Test
    fun searchConversations_returnsFilteredResults() = runTest {
        val query = "test"
        val entities = listOf(
            ConversationEntity(
                id = "id-1",
                title = "Test Conversation",
                createdAt = Instant.now().toEpochMilli(),
                lastModified = Instant.now().toEpochMilli(),
                messageCount = 2,
                isPinned = false,
                isArchived = false,
            ),
        )
        val flow: Flow<List<ConversationEntity>> = flowOf(entities)
        whenever(mockDao.searchConversations(query)).thenReturn(flow)

        val result = repository.searchConversations(query)

        assertNotNull(result)
        verify(mockDao).searchConversations(query)
    }

    @Test
    fun updateConversationMetadata_callsDaoUpdate() = runTest {
        val conversationId = "test-id"
        val messageCount = 15

        repository.updateConversationMetadata(conversationId, messageCount)

        verify(mockDao).updateConversationMetadata(any(), any(), any())
    }

    @Test
    fun togglePin_callsDaoUpdatePinStatus() = runTest {
        val conversationId = "test-id"

        repository.togglePin(conversationId, true)

        verify(mockDao).updatePinStatus(conversationId, true)
    }

    @Test
    fun toggleArchive_callsDaoUpdateArchiveStatus() = runTest {
        val conversationId = "test-id"

        repository.toggleArchive(conversationId, true)

        verify(mockDao).updateArchiveStatus(conversationId, true)
    }

    @Test
    fun getAllConversationsIncludingArchived_returnsAllConversations() = runTest {
        val entities = listOf(
            ConversationEntity(
                id = "id-1",
                title = "Active",
                createdAt = Instant.now().toEpochMilli(),
                lastModified = Instant.now().toEpochMilli(),
                messageCount = 5,
                isPinned = false,
                isArchived = false,
            ),
            ConversationEntity(
                id = "id-2",
                title = "Archived",
                createdAt = Instant.now().toEpochMilli(),
                lastModified = Instant.now().toEpochMilli(),
                messageCount = 3,
                isPinned = false,
                isArchived = true,
            ),
        )
        val flow: Flow<List<ConversationEntity>> = flowOf(entities)
        whenever(mockDao.getAllConversationsIncludingArchived()).thenReturn(flow)

        val result = repository.getAllConversationsIncludingArchived()

        assertNotNull(result)
        verify(mockDao).getAllConversationsIncludingArchived()
    }

    @Test
    fun getArchivedConversations_returnsOnlyArchived() = runTest {
        val entities = listOf(
            ConversationEntity(
                id = "id-1",
                title = "Archived 1",
                createdAt = Instant.now().toEpochMilli(),
                lastModified = Instant.now().toEpochMilli(),
                messageCount = 2,
                isPinned = false,
                isArchived = true,
            ),
        )
        val flow: Flow<List<ConversationEntity>> = flowOf(entities)
        whenever(mockDao.getArchivedConversations()).thenReturn(flow)

        val result = repository.getArchivedConversations()

        assertNotNull(result)
        verify(mockDao).getArchivedConversations()
    }
}
