package com.nervesparks.iris.data.privacy

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit tests for DataDeletionService.
 */
class DataDeletionServiceTest {

    private lateinit var mockConversationRepository: ConversationRepository
    private lateinit var mockMessageRepository: MessageRepository
    private lateinit var dataDeletionService: DataDeletionService

    @Before
    fun setup() {
        mockConversationRepository = mock()
        mockMessageRepository = mock()
        dataDeletionService = DataDeletionService(
            mockConversationRepository,
            mockMessageRepository,
        )
    }

    @Test
    fun deleteAllData_callsRepositoryMethods() = runTest {
        whenever(mockConversationRepository.getConversationCount()).thenReturn(5)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(20)

        val result = dataDeletionService.deleteAllData()

        assertTrue(result.success)
        assertEquals(5, result.conversationsDeleted)
        assertEquals(20, result.messagesDeleted)
        verify(mockConversationRepository).deleteAllConversations()
        verify(mockMessageRepository).deleteAllMessages()
    }

    @Test
    fun deleteAllData_withNoData_returnsZeros() = runTest {
        whenever(mockConversationRepository.getConversationCount()).thenReturn(0)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(0)

        val result = dataDeletionService.deleteAllData()

        assertTrue(result.success)
        assertEquals(0, result.conversationsDeleted)
        assertEquals(0, result.messagesDeleted)
    }

    @Test
    fun deleteConversations_deletesSpecificOnes() = runTest {
        val conversationIds = listOf("conv-1", "conv-2", "conv-3")

        whenever(mockMessageRepository.getMessageCountForConversation("conv-1")).thenReturn(5)
        whenever(mockMessageRepository.getMessageCountForConversation("conv-2")).thenReturn(10)
        whenever(mockMessageRepository.getMessageCountForConversation("conv-3")).thenReturn(3)

        val result = dataDeletionService.deleteConversations(conversationIds)

        assertTrue(result.success)
        assertEquals(3, result.conversationsDeleted)
        assertEquals(18, result.messagesDeleted)
        verify(mockConversationRepository).deleteConversations(conversationIds)
    }

    @Test
    fun deleteConversations_withEmptyList_succeeds() = runTest {
        val result = dataDeletionService.deleteConversations(emptyList())

        assertTrue(result.success)
        assertEquals(0, result.conversationsDeleted)
        assertEquals(0, result.messagesDeleted)
    }

    @Test
    fun deleteOldMessages_filtersCorrectly() = runTest {
        val now = Instant.now()
        val oldMessage = Message(
            id = "old-1",
            content = "Old message",
            role = MessageRole.USER,
            timestamp = now.minusSeconds(100 * 24 * 60 * 60), // 100 days ago
        )
        val recentMessage = Message(
            id = "recent-1",
            content = "Recent message",
            role = MessageRole.USER,
            timestamp = now.minusSeconds(10 * 24 * 60 * 60), // 10 days ago
        )

        whenever(mockMessageRepository.getAllMessagesList())
            .thenReturn(listOf(oldMessage, recentMessage))

        val result = dataDeletionService.deleteOldMessages(30) // Delete older than 30 days

        assertTrue(result.success)
        assertEquals(0, result.conversationsDeleted)
        assertEquals(1, result.messagesDeleted)
        verify(mockMessageRepository).deleteMessage("old-1")
    }

    @Test
    fun deleteOldMessages_withNoOldMessages_deletesNothing() = runTest {
        val recentMessage = Message(
            id = "recent-1",
            content = "Recent",
            role = MessageRole.USER,
            timestamp = Instant.now().minusSeconds(5 * 24 * 60 * 60),
        )

        whenever(mockMessageRepository.getAllMessagesList())
            .thenReturn(listOf(recentMessage))

        val result = dataDeletionService.deleteOldMessages(30)

        assertTrue(result.success)
        assertEquals(0, result.messagesDeleted)
    }

    @Test
    fun deleteArchivedConversations_deletesOnlyArchived() = runTest {
        val archivedConversations = listOf(
            Conversation(
                id = "arch-1",
                title = "Archived 1",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 5,
                isArchived = true,
            ),
            Conversation(
                id = "arch-2",
                title = "Archived 2",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 3,
                isArchived = true,
            ),
        )

        whenever(mockConversationRepository.getArchivedConversations())
            .thenReturn(flowOf(archivedConversations))
        whenever(mockMessageRepository.getMessageCountForConversation("arch-1")).thenReturn(5)
        whenever(mockMessageRepository.getMessageCountForConversation("arch-2")).thenReturn(3)

        val result = dataDeletionService.deleteArchivedConversations()

        assertTrue(result.success)
        assertEquals(2, result.conversationsDeleted)
        assertEquals(8, result.messagesDeleted)
        verify(mockConversationRepository).deleteConversations(listOf("arch-1", "arch-2"))
    }

    @Test
    fun deleteArchivedConversations_withNoArchived_deletesNothing() = runTest {
        whenever(mockConversationRepository.getArchivedConversations())
            .thenReturn(flowOf(emptyList()))

        val result = dataDeletionService.deleteArchivedConversations()

        assertTrue(result.success)
        assertEquals(0, result.conversationsDeleted)
        assertEquals(0, result.messagesDeleted)
    }

    @Test
    fun vacuumDatabase_withExistingDatabase_returnsTrue() = runTest {
        val tempFile = java.io.File.createTempFile("test", ".db")
        tempFile.deleteOnExit()

        val result = dataDeletionService.vacuumDatabase(tempFile.absolutePath)

        assertTrue(result)
    }

    @Test
    fun vacuumDatabase_withNonExistentDatabase_returnsFalse() = runTest {
        val result = dataDeletionService.vacuumDatabase("/non/existent/path.db")

        assertFalse(result)
    }
}
