package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ProcessingIndicator component.
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
}
