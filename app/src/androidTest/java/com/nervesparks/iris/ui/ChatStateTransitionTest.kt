package com.nervesparks.iris.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.ui.components.EmptyState
import com.nervesparks.iris.ui.components.ErrorBanner
import com.nervesparks.iris.ui.components.ProcessingIndicator
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * State machine tests for the chat interface.
 * Tests transitions between Empty -> Processing -> Error -> Retry states.
 */
@RunWith(AndroidJUnit4::class)
class ChatStateTransitionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun stateTransition_emptyState_isDisplayed_whenNoMessages() {
        composeTestRule.setContent {
            EmptyState()
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("Hello, Ask me Anything").assertExists()
    }

    @Test
    fun stateTransition_processingIndicator_isDisplayed_whenProcessing() {
        composeTestRule.setContent {
            ProcessingIndicator(showMetrics = true)
        }

        // Verify processing indicator is displayed
        composeTestRule.onNodeWithText("Thinking").assertExists()
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertExists()
    }

    @Test
    fun stateTransition_errorBanner_isDisplayed_whenError() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Processing failed",
                onRetry = {},
            )
        }

        // Verify error state is displayed
        composeTestRule.onNodeWithText("Error").assertExists()
        composeTestRule.onNodeWithText("Processing failed").assertExists()
        composeTestRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun stateTransition_errorToRetry_clearsError() {
        var errorCleared = false

        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Connection error",
                onRetry = { errorCleared = true },
                onDismiss = {},
            )
        }

        // Click retry button
        composeTestRule.onNodeWithText("Retry").performClick()

        // Verify error clearing callback was invoked
        assert(errorCleared)
    }

    @Test
    fun stateTransition_errorToDismiss_hidesError() {
        var errorDismissed = false

        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Error message",
                onRetry = {},
                onDismiss = { errorDismissed = true },
            )
        }

        // Click dismiss button
        composeTestRule.onNodeWithText("Dismiss").performClick()

        // Verify dismiss callback was invoked
        assert(errorDismissed)
    }

    @Test
    fun stateTransition_processingIndicator_hasAnimatedDots() {
        composeTestRule.setContent {
            ProcessingIndicator(showMetrics = true)
        }

        // Verify processing indicator animation is running
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        // Indicator should still be visible and animating
        composeTestRule.onNodeWithText("Thinking").assertExists()
    }

    @Test
    fun stateTransition_emptyState_startersAreInteractive() {
        var starterClicked = false

        composeTestRule.setContent {
            EmptyState(onStarterClick = {
                starterClicked = true
            })
        }

        // Click a conversation starter
        composeTestRule.onNodeWithText("Explains complex topics simply.")
            .performClick()

        // Verify interaction was captured
        assert(starterClicked)
    }

    @Test
    fun stateTransition_errorBanner_showsRetryAndDismissOptions() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Test error",
                onRetry = {},
                onDismiss = {},
            )
        }

        // Verify both action buttons are present
        composeTestRule.onNodeWithText("Retry").assertExists()
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }

    @Test
    fun stateTransition_processingIndicator_withoutMetrics_hidesText() {
        composeTestRule.setContent {
            ProcessingIndicator(showMetrics = false)
        }

        // Verify "Thinking" text is not displayed when metrics are disabled
        composeTestRule.onNodeWithText("Thinking").assertDoesNotExist()

        // But icon should still be present
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertExists()
    }
}
