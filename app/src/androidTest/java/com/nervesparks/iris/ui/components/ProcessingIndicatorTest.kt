package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ProcessingIndicator component.
 * Tests cover both static "Thinking" state and streaming "Assistant is typing…" state.
 */
@RunWith(AndroidJUnit4::class)
class ProcessingIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun processingIndicator_displays_assistantIcon() {
        composeTestRule.setContent {
            ProcessingIndicator()
        }

        // Verify assistant icon is displayed
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertExists()
    }

    @Test
    fun processingIndicator_displays_thinkingText_whenMetricsEnabled() {
        composeTestRule.setContent {
            ProcessingIndicator(showMetrics = true)
        }

        // Verify "Thinking" text is displayed
        composeTestRule.onNodeWithText("Thinking").assertExists()
    }

    @Test
    fun processingIndicator_hidesText_whenMetricsDisabled() {
        composeTestRule.setContent {
            ProcessingIndicator(showMetrics = false)
        }

        // Verify "Thinking" text is NOT displayed
        composeTestRule.onNodeWithText("Thinking").assertDoesNotExist()
    }

    @Test
    fun processingIndicator_isVisible() {
        composeTestRule.setContent {
            ProcessingIndicator()
        }

        // Verify the indicator is rendered and visible
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertIsDisplayed()
    }
    
    @Test
    fun processingIndicator_showsTypingIndicator_whenStreamingTextProvided() {
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = "Hello, how can I"
            )
        }

        // Wait for debounce delay (50ms) plus some buffer
        composeTestRule.waitForIdle()
        
        // Verify "Assistant is typing…" is displayed when streaming
        composeTestRule.onNodeWithText("Assistant is typing…").assertExists()
        
        // Verify "Thinking" is NOT displayed during streaming
        composeTestRule.onNodeWithText("Thinking").assertDoesNotExist()
    }
    
    @Test
    fun processingIndicator_showsThinking_whenNoStreamingText() {
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = null
            )
        }

        // Verify "Thinking" is displayed when no streaming text
        composeTestRule.onNodeWithText("Thinking").assertExists()
        
        // Verify typing indicator is NOT displayed
        composeTestRule.onNodeWithText("Assistant is typing…").assertDoesNotExist()
    }
    
    @Test
    fun processingIndicator_showsThinking_whenEmptyStreamingText() {
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = ""
            )
        }

        // Verify "Thinking" is displayed when streaming text is empty
        composeTestRule.onNodeWithText("Thinking").assertExists()
        
        // Verify typing indicator is NOT displayed
        composeTestRule.onNodeWithText("Assistant is typing…").assertDoesNotExist()
    }
    
    @Test
    fun processingIndicator_transitionsFromThinkingToTyping() {
        var streamingText: String? = null
        
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = streamingText
            )
        }

        // Initially shows "Thinking"
        composeTestRule.onNodeWithText("Thinking").assertExists()
        composeTestRule.onNodeWithText("Assistant is typing…").assertDoesNotExist()
        
        // Update to show streaming text
        streamingText = "Starting to respond"
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = streamingText
            )
        }
        
        // Wait for debounce
        composeTestRule.waitForIdle()
        
        // Now shows "Assistant is typing…"
        composeTestRule.onNodeWithText("Assistant is typing…").assertExists()
        composeTestRule.onNodeWithText("Thinking").assertDoesNotExist()
    }
    
    @Test
    fun processingIndicator_consolidatesAfterStreaming() {
        var streamingText: String? = "This is a complete response"
        
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = streamingText
            )
        }

        // Wait for debounce
        composeTestRule.waitForIdle()
        
        // Shows typing indicator during streaming
        composeTestRule.onNodeWithText("Assistant is typing…").assertExists()
        
        // Simulate completion by removing the indicator entirely
        // In real usage, the ProcessingIndicator would be removed from composition
        // when viewModel.getIsSending() becomes false
        streamingText = null
        composeTestRule.setContent {
            ProcessingIndicator(
                showMetrics = true,
                streamingText = streamingText
            )
        }
        
        // After completion, returns to "Thinking" state (or would be removed from UI)
        composeTestRule.onNodeWithText("Thinking").assertExists()
    }
}
