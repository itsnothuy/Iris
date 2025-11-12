package com.nervesparks.iris.data.export

import com.nervesparks.iris.data.Conversation
import com.nervesparks.iris.data.repository.ConversationRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Instant

/**
 * Unit tests for ImportService.
 */
class ImportServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockConversationRepository: ConversationRepository
    private lateinit var mockMessageRepository: MessageRepository
    private lateinit var importService: ImportService

    @Before
    fun setup() {
        mockConversationRepository = mock()
        mockMessageRepository = mock()
        importService = ImportService(mockConversationRepository, mockMessageRepository)
    }

    @Test
    fun importFromJson_withValidFile_importsSuccessfully() = runTest {
        val jsonContent = """
        {
            "version": "1.0",
            "exportedAt": "2024-01-01T00:00:00Z",
            "conversationCount": 1,
            "conversations": [
                {
                    "id": "test-id",
                    "title": "Test Conversation",
                    "createdAt": "2024-01-01T00:00:00Z",
                    "lastModified": "2024-01-01T00:00:00Z",
                    "messageCount": 1,
                    "isPinned": false,
                    "isArchived": false,
                    "messages": [
                        {
                            "id": "msg-1",
                            "content": "Hello",
                            "role": "USER",
                            "timestamp": "2024-01-01T00:00:00Z"
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val file = tempFolder.newFile("test.json")
        file.writeText(jsonContent)

        whenever(mockConversationRepository.getConversationById("test-id")).thenReturn(null)

        val result = importService.importFromJson(file)

        assertTrue(result.success)
        assertEquals(1, result.conversationsImported)
        assertEquals(1, result.messagesImported)
        assertEquals(0, result.duplicatesSkipped)
        verify(mockConversationRepository).createConversation(any())
        verify(mockMessageRepository).saveMessages(any(), eq("test-id"))
    }

    @Test
    fun importFromJson_withDuplicates_skipsThem() = runTest {
        val jsonContent = """
        {
            "version": "1.0",
            "conversationCount": 1,
            "conversations": [
                {
                    "id": "existing-id",
                    "title": "Existing",
                    "createdAt": "2024-01-01T00:00:00Z",
                    "lastModified": "2024-01-01T00:00:00Z",
                    "messageCount": 0,
                    "messages": []
                }
            ]
        }
        """.trimIndent()

        val file = tempFolder.newFile("test.json")
        file.writeText(jsonContent)

        val existingConversation = Conversation(
            id = "existing-id",
            title = "Existing",
            createdAt = Instant.now(),
            lastModified = Instant.now(),
        )
        whenever(mockConversationRepository.getConversationById("existing-id"))
            .thenReturn(existingConversation)

        val result = importService.importFromJson(file, skipDuplicates = true)

        assertTrue(result.success)
        assertEquals(0, result.conversationsImported)
        assertEquals(1, result.duplicatesSkipped)
    }

    @Test
    fun importFromJson_withNonExistentFile_returnsError() = runTest {
        val file = File("non-existent.json")

        val result = importService.importFromJson(file)

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("does not exist"))
    }

    @Test
    fun importFromJson_withInvalidFormat_returnsError() = runTest {
        val jsonContent = """
        {
            "invalid": "format"
        }
        """.trimIndent()

        val file = tempFolder.newFile("invalid.json")
        file.writeText(jsonContent)

        val result = importService.importFromJson(file)

        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Invalid export file format"))
    }

    @Test
    fun importFromJson_withNonJsonFile_returnsError() = runTest {
        val file = tempFolder.newFile("test.txt")
        file.writeText("not json")

        val result = importService.importFromJson(file)

        assertFalse(result.success)
        assertNotNull(result.error)
    }

    @Test
    fun importFromJson_withMetadata_importsCorrectly() = runTest {
        val jsonContent = """
        {
            "version": "1.0",
            "conversationCount": 1,
            "conversations": [
                {
                    "id": "test-id",
                    "title": "Test",
                    "createdAt": "2024-01-01T00:00:00Z",
                    "lastModified": "2024-01-01T00:00:00Z",
                    "messageCount": 1,
                    "messages": [
                        {
                            "id": "msg-1",
                            "content": "Test",
                            "role": "ASSISTANT",
                            "timestamp": "2024-01-01T00:00:00Z",
                            "processingTimeMs": 1500,
                            "tokenCount": 100
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val file = tempFolder.newFile("metadata.json")
        file.writeText(jsonContent)

        whenever(mockConversationRepository.getConversationById("test-id")).thenReturn(null)

        val result = importService.importFromJson(file)

        assertTrue(result.success)
        assertEquals(1, result.conversationsImported)
        verify(mockMessageRepository).saveMessages(any(), eq("test-id"))
    }

    @Test
    fun validateExportFile_withValidFile_returnsValid() {
        val jsonContent = """
        {
            "version": "1.0",
            "conversationCount": 2,
            "conversations": [
                {
                    "id": "1",
                    "title": "First",
                    "messages": [{"id": "m1", "content": "Hi", "role": "USER", "timestamp": "2024-01-01T00:00:00Z"}]
                },
                {
                    "id": "2",
                    "title": "Second",
                    "messages": []
                }
            ]
        }
        """.trimIndent()

        val file = tempFolder.newFile("valid.json")
        file.writeText(jsonContent)

        val result = importService.validateExportFile(file)

        assertTrue(result.valid)
        assertEquals(2, result.conversationCount)
        assertEquals(1, result.messageCount)
    }

    @Test
    fun validateExportFile_withInvalidFile_returnsInvalid() {
        val jsonContent = """
        {
            "invalid": "format"
        }
        """.trimIndent()

        val file = tempFolder.newFile("invalid.json")
        file.writeText(jsonContent)

        val result = importService.validateExportFile(file)

        assertFalse(result.valid)
        assertNotNull(result.error)
    }

    @Test
    fun validateExportFile_withNonExistentFile_returnsInvalid() {
        val file = File("non-existent.json")

        val result = importService.validateExportFile(file)

        assertFalse(result.valid)
        assertTrue(result.error!!.contains("does not exist"))
    }

    @Test
    fun validateExportFile_withNonJsonFile_returnsInvalid() {
        val file = tempFolder.newFile("test.txt")
        file.writeText("not json")

        val result = importService.validateExportFile(file)

        assertFalse(result.valid)
        assertTrue(result.error!!.contains("Only JSON files are supported"))
    }
}
