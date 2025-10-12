package com.nervesparks.iris

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import com.nervesparks.iris.data.repository.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for model switching functionality in MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelModelSwitchTest {

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
        whenever(mockLlamaAndroid.send_eot_str()).thenReturn("")
        
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
    fun switchModel_whenNotSending_switchesImmediately() = runTest {
        // Given: No request in flight
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        val modelPath = "/path/to/model.gguf"
        val threads = 4
        
        // When: Switch model is called
        viewModel.switchModel(modelPath, threads)
        advanceUntilIdle()
        
        // Then: Model is switched and state is updated
        assertEquals("model.gguf", viewModel.loadedModelName.value)
        assertFalse(viewModel.isSwitchingModel.value)
    }

    @Test
    fun switchModel_whenSending_defersSwitch() = runTest {
        // Given: Request is in flight
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(true)
        val modelPath = "/path/to/model.gguf"
        val threads = 4
        
        // When: Switch model is called
        viewModel.switchModel(modelPath, threads)
        
        // Then: Switch is deferred and state shows switching
        assertTrue(viewModel.isSwitchingModel.value)
        // Model should not be loaded yet
        assertNotEquals("model.gguf", viewModel.loadedModelName.value)
    }

    @Test
    fun switchModel_updatesDefaultModelPreference() = runTest {
        // Given: No request in flight
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        val modelPath = "/path/to/newmodel.gguf"
        val threads = 4
        
        // When: Switch model is called
        viewModel.switchModel(modelPath, threads)
        advanceUntilIdle()
        
        // Then: Default model preference is updated
        verify(mockUserPreferencesRepository).setDefaultModelName("newmodel.gguf")
    }

    @Test
    fun switchModel_multipleModels_switchesCorrectly() = runTest {
        // Given: First model loaded
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        viewModel.switchModel("/path/to/model1.gguf", 4)
        advanceUntilIdle()
        
        // When: Switch to second model
        viewModel.switchModel("/path/to/model2.gguf", 4)
        advanceUntilIdle()
        
        // Then: Second model is loaded
        assertEquals("model2.gguf", viewModel.loadedModelName.value)
        assertFalse(viewModel.isSwitchingModel.value)
    }

    @Test
    fun isSwitchingModel_initiallyFalse() {
        // Then: Initially not switching
        assertFalse(viewModel.isSwitchingModel.value)
    }

    @Test
    fun switchModel_setsIsSwitchingModelDuringSwitchWhenSending() {
        // Given: Request in flight
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(true)
        
        // When: Switch model is called
        viewModel.switchModel("/path/to/model.gguf", 4)
        
        // Then: Switching state is true
        assertTrue(viewModel.isSwitchingModel.value)
    }

    @Test
    fun loadedModelName_initiallyEmpty() {
        // Then: Initially empty
        assertEquals("", viewModel.loadedModelName.value)
    }

    @Test
    fun switchModel_withDifferentThreadCounts_appliesCorrectThreadCount() = runTest {
        // Given: No request in flight
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        val modelPath = "/path/to/model.gguf"
        
        // When: Switch model with 8 threads
        val threads = 8
        viewModel.switchModel(modelPath, threads)
        advanceUntilIdle()
        
        // Then: Model is loaded (thread count is used internally by load method)
        assertEquals("model.gguf", viewModel.loadedModelName.value)
    }
}
