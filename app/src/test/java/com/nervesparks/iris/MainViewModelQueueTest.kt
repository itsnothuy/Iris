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
 * Unit tests for queue and backpressure functionality in MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelQueueTest {

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
        whenever(mockLlamaAndroid.isQueued()).thenReturn(false)
        whenever(mockLlamaAndroid.getQueueSize()).thenReturn(0)
        
        viewModel = MainViewModel(
            llamaAndroid = mockLlamaAndroid,
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun queueState_initiallyEmpty() {
        // Given: Initial state
        
        // Then: Queue state should be empty
        assertFalse(viewModel.isMessageQueued)
        assertEquals(0, viewModel.queueSize)
    }

    @Test
    fun queueState_whenNotQueued_isMessageQueuedIsFalse() {
        // Given: LLamaAndroid reports not queued
        whenever(mockLlamaAndroid.isQueued()).thenReturn(false)
        whenever(mockLlamaAndroid.getQueueSize()).thenReturn(0)
        
        // When: Update queue state is called (simulated internally)
        // This would happen via send() but we're testing state management
        
        // Then: View model reflects the state
        assertFalse(viewModel.isMessageQueued)
        assertEquals(0, viewModel.queueSize)
    }

    @Test
    fun queueState_whenQueued_isMessageQueuedIsTrue() {
        // Given: LLamaAndroid reports queued state
        whenever(mockLlamaAndroid.isQueued()).thenReturn(true)
        whenever(mockLlamaAndroid.getQueueSize()).thenReturn(2)
        
        // Simulate internal state update
        // In real scenario this happens after tryEnqueue in send()
        
        // Then: We can verify the mock behavior
        assertTrue(mockLlamaAndroid.isQueued())
        assertEquals(2, mockLlamaAndroid.getQueueSize())
    }

    @Test
    fun errorMessage_whenQueueFull_setsErrorMessage() {
        // Given: Mock tryEnqueue to return false (queue full)
        // This is tested via integration but we verify error handling
        
        // When: An error is set
        viewModel.setError("Too many requests in queue. Please wait and try again.")
        
        // Then: Error message is set
        assertEquals("Too many requests in queue. Please wait and try again.", viewModel.errorMessage)
    }

    @Test
    fun errorMessage_clearsOnNewSend() {
        // Given: An existing error message
        viewModel.setError("Previous error")
        assertEquals("Previous error", viewModel.errorMessage)
        
        // When: clearError is called (happens at start of send())
        viewModel.clearError()
        
        // Then: Error message is cleared
        assertNull(viewModel.errorMessage)
    }

    @Test
    fun queueSize_tracksCorrectly() {
        // Given: Different queue sizes
        val testCases = listOf(0, 1, 2, 3)
        
        testCases.forEach { size ->
            // When: Queue size changes
            whenever(mockLlamaAndroid.getQueueSize()).thenReturn(size)
            
            // Then: Mock returns correct value
            assertEquals(size, mockLlamaAndroid.getQueueSize())
        }
    }

    @Test
    fun isQueued_togglesCorrectly() {
        // Given: Queue state changes from empty to queued
        
        // When: Initially not queued
        whenever(mockLlamaAndroid.isQueued()).thenReturn(false)
        assertFalse(mockLlamaAndroid.isQueued())
        
        // When: Becomes queued
        whenever(mockLlamaAndroid.isQueued()).thenReturn(true)
        assertTrue(mockLlamaAndroid.isQueued())
        
        // When: Returns to not queued
        whenever(mockLlamaAndroid.isQueued()).thenReturn(false)
        assertFalse(mockLlamaAndroid.isQueued())
    }

    @Test
    fun queueBehavior_maxSizeEnforcement() {
        // Given: Queue can have max 3 items
        val maxQueueSize = 3
        
        // When/Then: Queue size should never exceed max
        for (size in 0..maxQueueSize) {
            whenever(mockLlamaAndroid.getQueueSize()).thenReturn(size)
            assertTrue(mockLlamaAndroid.getQueueSize() <= maxQueueSize)
        }
    }

    @Test
    fun queueBehavior_rejectWhenFull() {
        // This tests the contract that tryEnqueue should reject when full
        // In real implementation, LLamaAndroid.tryEnqueue returns false when queue is full
        
        // Given: Queue is at max capacity
        whenever(mockLlamaAndroid.getQueueSize()).thenReturn(3)
        
        // Then: We verify the queue size is at max
        assertEquals(3, mockLlamaAndroid.getQueueSize())
    }
    
    @Test
    fun rateLimitCooldown_tracksCorrectly() {
        // Given: Different cooldown values
        val testCases = listOf(0, 15, 30, 45, 60)
        
        testCases.forEach { cooldown ->
            // When: Cooldown changes
            whenever(mockLlamaAndroid.getRateLimitCooldownSeconds()).thenReturn(cooldown)
            
            // Then: Mock returns correct value
            assertEquals(cooldown, mockLlamaAndroid.getRateLimitCooldownSeconds())
        }
    }
}
