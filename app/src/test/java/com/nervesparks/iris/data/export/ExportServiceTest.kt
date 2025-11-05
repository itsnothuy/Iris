package com.nervesparks.iris.data.export

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Instant

/**
 * Unit tests for ExportService.
 */
class ExportServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockConversationRepository: ConversationRepository
    private lateinit var mockMessageRepository: MessageRepository
    private lateinit var exportService: ExportService
    private lateinit var exportDir: File

    @Before
    fun setup() {
        mockConversationRepository = mock()
        mockMessageRepository = mock()
        exportService = ExportService(mockConversationRepository, mockMessageRepository)
        exportDir = tempFolder.newFolder("exports")
    }

    @Test
    fun exportAllConversations_withJsonFormat_createsValidFile() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "test-id",
            title = "Test Conversation",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 2
        )
        val messages = listOf(
            Message(
                id = "msg-1",
                content = "Hello",
                role = MessageRole.USER,
                timestamp = timestamp
            ),
            Message(
                id = "msg-2",
                content = "Hi there!",
                role = MessageRole.ASSISTANT,
                timestamp = timestamp.plusSeconds(1)
            )
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(messages)

        val result = exportService.exportAllConversations(exportDir, ExportFormat.JSON)

        assertTrue(result.success)
        assertNotNull(result.filePath)
        assertNotNull(result.checksum)
        assertTrue(File(result.filePath!!).exists())
        assertTrue(File(result.filePath!!).readText().contains("Test Conversation"))
    }

    @Test
    fun exportAllConversations_withMarkdownFormat_createsValidFile() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "test-id",
            title = "Markdown Test",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 1
        )
        val messages = listOf(
            Message(
                id = "msg-1",
                content = "Test message",
                role = MessageRole.USER,
                timestamp = timestamp
            )
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(messages)

        val result = exportService.exportAllConversations(exportDir, ExportFormat.MARKDOWN)

        assertTrue(result.success)
        assertNotNull(result.filePath)
        assertTrue(File(result.filePath!!).name.endsWith(".md"))
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("# Iris Conversations Export"))
        assertTrue(content.contains("Markdown Test"))
    }

    @Test
    fun exportAllConversations_withPlainTextFormat_createsValidFile() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "test-id",
            title = "Plain Text Test",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 1
        )
        val messages = listOf(
            Message(
                id = "msg-1",
                content = "Plain text content",
                role = MessageRole.USER,
                timestamp = timestamp
            )
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(messages)

        val result = exportService.exportAllConversations(exportDir, ExportFormat.PLAIN_TEXT)

        assertTrue(result.success)
        assertNotNull(result.filePath)
        assertTrue(File(result.filePath!!).name.endsWith(".txt"))
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("IRIS CONVERSATIONS EXPORT"))
        assertTrue(content.contains("Plain Text Test"))
    }

    @Test
    fun exportConversations_withSpecificIds_exportsOnlyThose() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "conv-1",
            title = "Specific Conversation",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 0
        )

        whenever(mockConversationRepository.getConversationById("conv-1"))
            .thenReturn(conversation)
        whenever(mockMessageRepository.getMessagesForConversationList("conv-1"))
            .thenReturn(emptyList())

        val result = exportService.exportConversations(
            listOf("conv-1"),
            exportDir,
            ExportFormat.JSON
        )

        assertTrue(result.success)
        assertNotNull(result.filePath)
    }

    @Test
    fun exportConversations_withEmptyList_createsEmptyExport() = runTest {
        val result = exportService.exportConversations(
            emptyList(),
            exportDir,
            ExportFormat.JSON
        )

        assertTrue(result.success)
        assertNotNull(result.filePath)
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("\"conversationCount\": 0"))
    }

    @Test
    fun exportConversationsByDateRange_filtersCorrectly() = runTest {
        val oldDate = Instant.parse("2023-01-01T00:00:00Z")
        val recentDate = Instant.parse("2024-01-01T00:00:00Z")
        
        val oldConversation = Conversation(
            id = "old-id",
            title = "Old",
            createdAt = oldDate,
            lastModified = oldDate,
            messageCount = 0
        )
        val recentConversation = Conversation(
            id = "recent-id",
            title = "Recent",
            createdAt = recentDate,
            lastModified = recentDate,
            messageCount = 0
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(oldConversation, recentConversation)))
        whenever(mockMessageRepository.getMessagesForConversationList(any()))
            .thenReturn(emptyList())

        val startDate = Instant.parse("2023-12-01T00:00:00Z")
        val endDate = Instant.parse("2024-02-01T00:00:00Z")
        
        val result = exportService.exportConversationsByDateRange(
            startDate,
            endDate,
            exportDir,
            ExportFormat.JSON
        )

        assertTrue(result.success)
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("Recent"))
        assertFalse(content.contains("Old"))
    }

    @Test
    fun exportConversationsByDateRange_inclusiveBoundaries() = runTest {
        val exactStartDate = Instant.parse("2024-01-01T00:00:00Z")
        val exactEndDate = Instant.parse("2024-01-31T23:59:59Z")
        
        val conversationAtStart = Conversation(
            id = "start-id",
            title = "Start",
            createdAt = exactStartDate,
            lastModified = exactStartDate,
            messageCount = 0
        )
        val conversationAtEnd = Conversation(
            id = "end-id",
            title = "End",
            createdAt = exactEndDate,
            lastModified = exactEndDate,
            messageCount = 0
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversationAtStart, conversationAtEnd)))
        whenever(mockMessageRepository.getMessagesForConversationList(any()))
            .thenReturn(emptyList())
        
        val result = exportService.exportConversationsByDateRange(
            exactStartDate,
            exactEndDate,
            exportDir,
            ExportFormat.JSON
        )

        assertTrue(result.success)
        val content = File(result.filePath!!).readText()
        // Both conversations at exact boundaries should be included
        assertTrue(content.contains("Start"))
        assertTrue(content.contains("End"))
    }

    @Test
    fun exportService_createsChecksumForExport() = runTest {
        val conversation = Conversation(
            id = "test-id",
            title = "Checksum Test",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
            messageCount = 0
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(emptyList())

        val result = exportService.exportAllConversations(exportDir, ExportFormat.JSON)

        assertTrue(result.success)
        assertNotNull(result.checksum)
        // SHA-256 checksum should be 64 hex characters
        assertEquals(64, result.checksum!!.length)
    }

    @Test
    fun exportService_handlesMessageMetadata() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "test-id",
            title = "Metadata Test",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 1
        )
        val message = Message(
            id = "msg-1",
            content = "Test",
            role = MessageRole.ASSISTANT,
            timestamp = timestamp,
            processingTimeMs = 1500L,
            tokenCount = 100
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(listOf(message))

        val result = exportService.exportAllConversations(exportDir, ExportFormat.JSON)

        assertTrue(result.success)
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("processingTimeMs"))
        assertTrue(content.contains("1500"))
        assertTrue(content.contains("tokenCount"))
        assertTrue(content.contains("100"))
    }

    @Test
    fun exportService_handlesMultipleRoles() = runTest {
        val timestamp = Instant.now()
        val conversation = Conversation(
            id = "test-id",
            title = "Roles Test",
            createdAt = timestamp,
            lastModified = timestamp,
            messageCount = 3
        )
        val messages = listOf(
            Message(id = "1", content = "User", role = MessageRole.USER, timestamp = timestamp),
            Message(id = "2", content = "System", role = MessageRole.SYSTEM, timestamp = timestamp),
            Message(id = "3", content = "Assistant", role = MessageRole.ASSISTANT, timestamp = timestamp)
        )

        whenever(mockConversationRepository.getAllConversationsIncludingArchived())
            .thenReturn(flowOf(listOf(conversation)))
        whenever(mockMessageRepository.getMessagesForConversationList("test-id"))
            .thenReturn(messages)

        val result = exportService.exportAllConversations(exportDir, ExportFormat.JSON)

        assertTrue(result.success)
        val content = File(result.filePath!!).readText()
        assertTrue(content.contains("USER"))
        assertTrue(content.contains("SYSTEM"))
        assertTrue(content.contains("ASSISTANT"))
    }
}
