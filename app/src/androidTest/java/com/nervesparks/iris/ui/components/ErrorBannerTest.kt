package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ErrorBanner component.
 * Tests the display of error messages and retry functionality.
 */
@RunWith(AndroidJUnit4::class)
class ErrorBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorBanner_displays_errorMessage() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Failed to process message",
                onRetry = {}
            )
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Error").assertExists()
        composeTestRule.onNodeWithText("Failed to process message").assertExists()
    }

    @Test
    fun errorBanner_displays_retryButton() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Something went wrong",
                onRetry = {}
            )
        }

        // Verify retry button is displayed
        composeTestRule.onNodeWithText("Retry").assertExists()
    }

    @Test
    fun errorBanner_retryButton_isClickable() {
        var retryClicked = false

        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Connection failed",
                onRetry = { retryClicked = true }
            )
        }

        // Click retry button
        composeTestRule.onNodeWithText("Retry").performClick()

        // Verify callback was invoked
        assert(retryClicked)
    }

    @Test
    fun errorBanner_displays_dismissButton_whenProvided() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Error occurred",
                onRetry = {},
                onDismiss = {}
            )
        }

        // Verify dismiss button is displayed
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }

    @Test
    fun errorBanner_dismissButton_isClickable() {
        var dismissClicked = false

        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Error occurred",
                onRetry = {},
                onDismiss = { dismissClicked = true }
            )
        }

        // Click dismiss button
        composeTestRule.onNodeWithText("Dismiss").performClick()

        // Verify callback was invoked
        assert(dismissClicked)
    }

    @Test
    fun errorBanner_noDismissButton_whenNotProvided() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Error occurred",
                onRetry = {},
                onDismiss = null
            )
        }

        // Verify dismiss button does NOT exist
        composeTestRule.onNodeWithText("Dismiss").assertDoesNotExist()
    }

    @Test
    fun errorBanner_displays_errorIcon() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Test error",
                onRetry = {}
            )
        }

        // Verify error icon is displayed
        composeTestRule.onNodeWithContentDescription("Error").assertExists()
    }

    @Test
    fun errorBanner_isVisible() {
        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Visible error",
                onRetry = {}
            )
        }

        // Verify the banner is displayed
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Visible error").assertIsDisplayed()
    }

    @Test
    fun errorBanner_multipleClicks_callsCallbackMultipleTimes() {
        var retryCount = 0

        composeTestRule.setContent {
            ErrorBanner(
                errorMessage = "Test error",
                onRetry = { retryCount++ }
            )
        }

        // Click retry button multiple times
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.onNodeWithText("Retry").performClick()
        composeTestRule.onNodeWithText("Retry").performClick()

        // Verify callback was invoked correct number of times
        assert(retryCount == 3)
    }
}
