package com.nervesparks.iris.ui

import android.llama.cpp.LLamaAndroid
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.UserPreferencesRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Compose UI tests for Settings bottom sheet parameter controls.
 * Tests slider interactions, preset buttons, and reset functionality.
 */
@RunWith(AndroidJUnit4::class)
class SettingsParametersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createMockViewModel(): MainViewModel {
        val mockLlamaAndroid: LLamaAndroid = mock()
        val mockUserPreferencesRepository: UserPreferencesRepository = mock()

        // Mock repository methods
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("test-model.gguf")
        whenever(mockUserPreferencesRepository.getTemperature()).thenReturn(1.0f)
        whenever(mockUserPreferencesRepository.getTopP()).thenReturn(0.9f)
        whenever(mockUserPreferencesRepository.getTopK()).thenReturn(40)
        whenever(mockUserPreferencesRepository.getContextLength()).thenReturn(2048)

        // Mock LlamaAndroid methods
        whenever(mockLlamaAndroid.getIsSending()).thenReturn(false)
        whenever(mockLlamaAndroid.send_eot_str()).thenReturn("")

        return MainViewModel(
            llamaAndroid = mockLlamaAndroid,
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
        )
    }

    @Test
    fun settingsBottomSheet_displays_modelParametersSection() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Model Parameters section is displayed
        composeTestRule.onNodeWithText("Model Parameters").assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_parameterDescription() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify parameter description is displayed
        composeTestRule.onNodeWithText(
            "Adjust model inference parameters. Changes apply on next model load.",
        ).assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_presetButtons() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify all preset buttons are displayed
        composeTestRule.onNodeWithText("Conservative").assertExists()
        composeTestRule.onNodeWithText("Balanced").assertExists()
        composeTestRule.onNodeWithText("Creative").assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_temperatureSlider() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Temperature slider and its help text
        composeTestRule.onNodeWithText("Temperature").assertExists()
        composeTestRule.onNodeWithText(
            "Controls randomness. Lower values make output more focused and deterministic, higher values make it more creative.",
            substring = true,
        ).assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_topPSlider() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Top P slider and its help text
        composeTestRule.onNodeWithText("Top P").assertExists()
        composeTestRule.onNodeWithText(
            "Nucleus sampling. Considers tokens with cumulative probability up to this value.",
            substring = true,
        ).assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_topKSlider() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Top K slider and its help text
        composeTestRule.onNodeWithText("Top K").assertExists()
        composeTestRule.onNodeWithText(
            "Limits sampling to the top K most likely tokens.",
            substring = true,
        ).assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_contextLengthSlider() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Context Length slider and its help text
        composeTestRule.onNodeWithText("Context Length").assertExists()
        composeTestRule.onNodeWithText("Maximum conversation context length.", substring = true).assertExists()
    }

    @Test
    fun settingsBottomSheet_displays_resetButton() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Reset to Defaults button is displayed
        composeTestRule.onNodeWithText("Reset to Defaults").assertExists()
    }

    @Test
    fun settingsBottomSheet_presetButtons_areClickable() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Click each preset button
        composeTestRule.onNodeWithText("Conservative").performClick()
        composeTestRule.onNodeWithText("Balanced").performClick()
        composeTestRule.onNodeWithText("Creative").performClick()

        // If we reach here without exceptions, buttons are clickable
    }

    @Test
    fun settingsBottomSheet_resetButton_isClickable() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Click reset button
        composeTestRule.onNodeWithText("Reset to Defaults").performClick()

        // If we reach here without exceptions, button is clickable
    }

    @Test
    fun settingsBottomSheet_displays_quickPresetsLabel() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify Quick Presets label is displayed
        composeTestRule.onNodeWithText("Quick Presets").assertExists()
    }

    @Test
    fun settingsBottomSheet_allParameterSliders_areVisible() {
        val viewModel = createMockViewModel()

        composeTestRule.setContent {
            SettingsBottomSheet(
                viewModel = viewModel,
                onDismiss = {},
            )
        }

        // Verify all parameter sliders are visible (may need to scroll)
        composeTestRule.onNodeWithText("Temperature").assertExists()
        composeTestRule.onNodeWithText("Top P").assertExists()
        composeTestRule.onNodeWithText("Top K").assertExists()
        composeTestRule.onNodeWithText("Context Length").assertExists()
    }
}
