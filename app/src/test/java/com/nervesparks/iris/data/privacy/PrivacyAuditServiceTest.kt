package com.nervesparks.iris.data.privacy

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Unit tests for PrivacyAuditService.
 */
class PrivacyAuditServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockConversationRepository: ConversationRepository
    private lateinit var mockMessageRepository: MessageRepository
    private lateinit var privacyAuditService: PrivacyAuditService
    private lateinit var databasePath: String

    @Before
    fun setup() {
        mockConversationRepository = mock()
        mockMessageRepository = mock()
        val dbFile = tempFolder.newFile("test.db")
        databasePath = dbFile.absolutePath
        privacyAuditService = PrivacyAuditService(
            mockConversationRepository,
            mockMessageRepository,
            databasePath,
        )
    }

    @Test
    fun generateAuditReport_withData_returnsCorrectCounts() = runTest {
        val oldDate = Instant.parse("2023-01-01T00:00:00Z")
        val newDate = Instant.parse("2024-01-01T00:00:00Z")

        val conversations = listOf(
            Conversation(
                id = "1",
                title = "Old",
                createdAt = oldDate,
                lastModified = oldDate,
                messageCount = 5,
            ),
            Conversation(
                id = "2",
                title = "New",
                createdAt = newDate,
                lastModified = newDate,
                messageCount = 10,
            ),
        )

        whenever(mockConversationRepository.getConversationCount()).thenReturn(2)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(15)
        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(conversations))

        val report = privacyAuditService.generateAuditReport()

        assertEquals(2, report.totalConversations)
        assertEquals(15, report.totalMessages)
        assertEquals(oldDate, report.oldestConversation)
        assertEquals(newDate, report.newestConversation)
        assertFalse(report.networkActivity)
    }

    @Test
    fun generateAuditReport_withNoData_returnsZeros() = runTest {
        whenever(mockConversationRepository.getConversationCount()).thenReturn(0)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(0)
        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(emptyList()))

        val report = privacyAuditService.generateAuditReport()

        assertEquals(0, report.totalConversations)
        assertEquals(0, report.totalMessages)
        assertNull(report.oldestConversation)
        assertNull(report.newestConversation)
        assertFalse(report.networkActivity)
    }

    @Test
    fun getStorageBreakdown_withDatabaseFile_returnsBreakdown() = runTest {
        val breakdown = privacyAuditService.getStorageBreakdown()

        assertNotNull(breakdown)
        assertTrue(breakdown.containsKey("Database"))
    }

    @Test
    fun verifyDataIntegrity_withMatchingCounts_returnsTrue() = runTest {
        val conversation = Conversation(
            id = "test-id",
            title = "Test",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
            messageCount = 5,
        )

        whenever(mockConversationRepository.getConversationCount()).thenReturn(1)
        whenever(mockConversationRepository.getAllConversations())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessageCountForConversation("test-id"))
            .thenReturn(5)

        val result = privacyAuditService.verifyDataIntegrity()

        assertTrue(result)
    }

    @Test
    fun verifyDataIntegrity_withMismatchedCounts_returnsFalse() = runTest {
        val conversation = Conversation(
            id = "test-id",
            title = "Test",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
            messageCount = 5,
        )

        whenever(mockConversationRepository.getConversationCount()).thenReturn(1)
        whenever(mockConversationRepository.getAllConversations())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessageCountForConversation("test-id"))
            .thenReturn(3) // Mismatch

        val result = privacyAuditService.verifyDataIntegrity()

        assertFalse(result)
    }

    @Test
    fun formatBytes_formatsCorrectly() {
        assertEquals("0.00 B", privacyAuditService.formatBytes(0))
        assertEquals("100.00 B", privacyAuditService.formatBytes(100))
        assertEquals("1.00 KB", privacyAuditService.formatBytes(1024))
        assertEquals("1.00 MB", privacyAuditService.formatBytes(1024 * 1024))
        assertEquals("1.50 MB", privacyAuditService.formatBytes((1024 * 1024 * 1.5).toLong()))
        assertEquals("1.00 GB", privacyAuditService.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun generateAuditReport_networkActivityAlwaysFalse() = runTest {
        whenever(mockConversationRepository.getConversationCount()).thenReturn(10)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(100)
        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(emptyList()))

        val report = privacyAuditService.generateAuditReport()

        // Network activity should always be false for on-device app
        assertFalse(report.networkActivity)
    }

    @Test
    fun generateAuditReport_exportHistoryInitiallyEmpty() = runTest {
        whenever(mockConversationRepository.getConversationCount()).thenReturn(0)
        whenever(mockMessageRepository.getMessageCount()).thenReturn(0)
        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(emptyList()))

        val report = privacyAuditService.generateAuditReport()

        assertTrue(report.exportHistory.isEmpty())
    }
}
