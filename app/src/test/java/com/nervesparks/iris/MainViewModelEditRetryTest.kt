package com.nervesparks.iris

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for edit & resend and retry last message functionality in MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelEditRetryTest {

    private lateinit var viewModel: MainViewModel
    private val mockLlamaAndroid: LLamaAndroid = mock()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mock()
    private val mockMessageRepository: MessageRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock the UserPreferencesRepository to return an empty default model name
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        // Mock LLamaAndroid methods
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        
        viewModel = MainViewModel(
            llamaAndroid = mockLlamaAndroid,
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null // Not testing persistence here
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun retryLastMessage_withNoMessages_doesNothing() {
        // Given: Empty messages list
        assertEquals(0, viewModel.messages.size)
        
        // When: Retry is called
        viewModel.retryLastMessage()
        
        // Then: No messages are added (message field should be empty)
        assertEquals("", viewModel.message)
    }

    @Test
    fun retryLastMessage_withUserMessage_resendsSameMessage() {
        // Given: A user message exists
        viewModel.messages = listOf(
            mapOf("role" to "system", "content" to "System prompt"),
            mapOf("role" to "user", "content" to "Hi"),
            mapOf("role" to "assistant", "content" to "Hello!"),
            mapOf("role" to "user", "content" to "Tell me a joke")
        )
        
        // When: Retry is called
        viewModel.retryLastMessage()
        
        // Then: The last user message is set to be sent
        assertEquals("Tell me a joke", viewModel.message)
    }

    @Test
    fun retryLastMessage_removesLastAssistantResponse() {
        // Given: Messages ending with an assistant response
        viewModel.messages = listOf(
            mapOf("role" to "system", "content" to "System prompt"),
            mapOf("role" to "user", "content" to "Hi"),
            mapOf("role" to "assistant", "content" to "Hello!"),
            mapOf("role" to "user", "content" to "Tell me a joke"),
            mapOf("role" to "assistant", "content" to "Why did...")
        )
        
        val initialSize = viewModel.messages.size
        
        // When: Retry is called
        viewModel.retryLastMessage()
        
        // Then: Last assistant message is removed
        assertEquals(initialSize - 1, viewModel.messages.size)
        assertEquals("user", viewModel.messages.last()["role"])
    }

    @Test
    fun editAndResend_withBlankMessage_doesNothing() {
        // Given: Some existing messages
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "Original message")
        )
        val originalSize = viewModel.messages.size
        
        // When: Edit and resend with blank message
        viewModel.editAndResend("")
        
        // Then: Messages remain unchanged and message field is empty
        assertEquals(originalSize, viewModel.messages.size)
        assertEquals("", viewModel.message)
    }

    @Test
    fun editAndResend_withValidMessage_removesLastUserAndAssistantMessages() {
        // Given: Messages ending with user and assistant messages
        viewModel.messages = listOf(
            mapOf("role" to "system", "content" to "System prompt"),
            mapOf("role" to "user", "content" to "Hi"),
            mapOf("role" to "assistant", "content" to "Hello!"),
            mapOf("role" to "user", "content" to "Tell me a joke"),
            mapOf("role" to "assistant", "content" to "Why did...")
        )
        
        val initialSize = viewModel.messages.size
        
        // When: Edit and resend with new message
        viewModel.editAndResend("Tell me a different joke")
        
        // Then: Last two messages (user + assistant) are removed
        assertEquals(initialSize - 2, viewModel.messages.size)
        assertEquals("assistant", viewModel.messages.last()["role"])
        assertEquals("Hello!", viewModel.messages.last()["content"])
        
        // And: New message is set to be sent
        assertEquals("Tell me a different joke", viewModel.message)
    }

    @Test
    fun editAndResend_withOnlyUserMessage_removesLastUserMessage() {
        // Given: Messages ending with user message (no assistant response yet)
        viewModel.messages = listOf(
            mapOf("role" to "system", "content" to "System prompt"),
            mapOf("role" to "user", "content" to "Hi"),
            mapOf("role" to "assistant", "content" to "Hello!"),
            mapOf("role" to "user", "content" to "Tell me a joke")
        )
        
        val initialSize = viewModel.messages.size
        
        // When: Edit and resend
        viewModel.editAndResend("Tell me a story instead")
        
        // Then: Only the last user message is removed
        assertEquals(initialSize - 1, viewModel.messages.size)
        assertEquals("assistant", viewModel.messages.last()["role"])
        
        // And: New message is set to be sent
        assertEquals("Tell me a story instead", viewModel.message)
    }

    @Test
    fun editAndResend_preservesEarlierMessages() {
        // Given: Multiple conversation turns
        viewModel.messages = listOf(
            mapOf("role" to "system", "content" to "System prompt"),
            mapOf("role" to "user", "content" to "First question"),
            mapOf("role" to "assistant", "content" to "First answer"),
            mapOf("role" to "user", "content" to "Second question"),
            mapOf("role" to "assistant", "content" to "Second answer")
        )
        
        // When: Edit and resend
        viewModel.editAndResend("Modified second question")
        
        // Then: First conversation turn is preserved
        assertEquals(3, viewModel.messages.size)
        assertEquals("First question", viewModel.messages[1]["content"])
        assertEquals("First answer", viewModel.messages[2]["content"])
    }

    @Test
    fun retryLastMessage_findsCorrectLastUserMessage() {
        // Given: Messages with multiple user messages
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "First message"),
            mapOf("role" to "assistant", "content" to "First response"),
            mapOf("role" to "user", "content" to "Second message"),
            mapOf("role" to "assistant", "content" to "Second response")
        )
        
        // When: Retry is called
        viewModel.retryLastMessage()
        
        // Then: The last user message ("Second message") is set
        assertEquals("Second message", viewModel.message)
    }

    @Test
    fun editAndResend_withWhitespaceOnly_doesNothing() {
        // Given: Some existing messages
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "Original message")
        )
        val originalSize = viewModel.messages.size
        
        // When: Edit and resend with only whitespace
        viewModel.editAndResend("   ")
        
        // Then: Messages remain unchanged
        assertEquals(originalSize, viewModel.messages.size)
        assertEquals("", viewModel.message)
    }
}
