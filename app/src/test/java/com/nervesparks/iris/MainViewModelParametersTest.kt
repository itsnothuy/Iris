package com.nervesparks.iris

import android.llama.cpp.LLamaAndroid
import com.nervesparks.iris.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Unit tests for model parameter management in MainViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelParametersTest {

    private lateinit var viewModel: MainViewModel
    private val mockLlamaAndroid: LLamaAndroid = mock()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mock()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock the UserPreferencesRepository defaults
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        whenever(mockUserPreferencesRepository.getTemperature()).thenReturn(1.0f)
        whenever(mockUserPreferencesRepository.getTopP()).thenReturn(0.9f)
        whenever(mockUserPreferencesRepository.getTopK()).thenReturn(40)
        whenever(mockUserPreferencesRepository.getContextLength()).thenReturn(2048)
        
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
    fun getTemperature_returnsValueFromRepository() {
        // Given: Repository returns a temperature value
        whenever(mockUserPreferencesRepository.getTemperature()).thenReturn(1.5f)
        
        // When: Get temperature is called
        val result = viewModel.getTemperature()
        
        // Then: Value from repository is returned
        assertEquals(1.5f, result, 0.001f)
    }

    @Test
    fun setTemperature_storesValueInRepository() {
        // When: Set temperature is called
        viewModel.setTemperature(0.7f)
        
        // Then: Value is stored in repository
        verify(mockUserPreferencesRepository).setTemperature(0.7f)
    }

    @Test
    fun getTopP_returnsValueFromRepository() {
        // Given: Repository returns a top_p value
        whenever(mockUserPreferencesRepository.getTopP()).thenReturn(0.8f)
        
        // When: Get top_p is called
        val result = viewModel.getTopP()
        
        // Then: Value from repository is returned
        assertEquals(0.8f, result, 0.001f)
    }

    @Test
    fun setTopP_storesValueInRepository() {
        // When: Set top_p is called
        viewModel.setTopP(0.95f)
        
        // Then: Value is stored in repository
        verify(mockUserPreferencesRepository).setTopP(0.95f)
    }

    @Test
    fun getTopK_returnsValueFromRepository() {
        // Given: Repository returns a top_k value
        whenever(mockUserPreferencesRepository.getTopK()).thenReturn(50)
        
        // When: Get top_k is called
        val result = viewModel.getTopK()
        
        // Then: Value from repository is returned
        assertEquals(50, result)
    }

    @Test
    fun setTopK_storesValueInRepository() {
        // When: Set top_k is called
        viewModel.setTopK(60)
        
        // Then: Value is stored in repository
        verify(mockUserPreferencesRepository).setTopK(60)
    }

    @Test
    fun getContextLength_returnsValueFromRepository() {
        // Given: Repository returns a context length value
        whenever(mockUserPreferencesRepository.getContextLength()).thenReturn(4096)
        
        // When: Get context length is called
        val result = viewModel.getContextLength()
        
        // Then: Value from repository is returned
        assertEquals(4096, result)
    }

    @Test
    fun setContextLength_storesValueInRepository() {
        // When: Set context length is called
        viewModel.setContextLength(3072)
        
        // Then: Value is stored in repository
        verify(mockUserPreferencesRepository).setContextLength(3072)
    }

    @Test
    fun applyParameterPreset_conservative_appliesCorrectValues() {
        // When: Conservative preset is applied
        viewModel.applyParameterPreset(ParameterPreset.CONSERVATIVE)
        
        // Then: Conservative values are set
        verify(mockUserPreferencesRepository).setTemperature(0.5f)
        verify(mockUserPreferencesRepository).setTopP(0.7f)
        verify(mockUserPreferencesRepository).setTopK(20)
    }

    @Test
    fun applyParameterPreset_balanced_appliesCorrectValues() {
        // When: Balanced preset is applied
        viewModel.applyParameterPreset(ParameterPreset.BALANCED)
        
        // Then: Balanced values are set
        verify(mockUserPreferencesRepository).setTemperature(1.0f)
        verify(mockUserPreferencesRepository).setTopP(0.9f)
        verify(mockUserPreferencesRepository).setTopK(40)
    }

    @Test
    fun applyParameterPreset_creative_appliesCorrectValues() {
        // When: Creative preset is applied
        viewModel.applyParameterPreset(ParameterPreset.CREATIVE)
        
        // Then: Creative values are set
        verify(mockUserPreferencesRepository).setTemperature(1.5f)
        verify(mockUserPreferencesRepository).setTopP(0.95f)
        verify(mockUserPreferencesRepository).setTopK(60)
    }

    @Test
    fun resetParametersToDefaults_callsRepositoryMethod() {
        // When: Reset parameters is called
        viewModel.resetParametersToDefaults()
        
        // Then: Repository reset method is called
        verify(mockUserPreferencesRepository).resetParametersToDefaults()
    }

    @Test
    fun parameterPreset_enumValues() {
        // Verify all preset values exist
        val values = ParameterPreset.values()
        
        assertEquals(3, values.size)
        assertTrue(values.contains(ParameterPreset.CONSERVATIVE))
        assertTrue(values.contains(ParameterPreset.BALANCED))
        assertTrue(values.contains(ParameterPreset.CREATIVE))
    }
}
