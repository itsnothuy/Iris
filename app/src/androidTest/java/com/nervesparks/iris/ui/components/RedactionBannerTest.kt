package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for RedactionBanner component.
 * Tests the display of redaction information and dismiss functionality.
 */
@RunWith(AndroidJUnit4::class)
class RedactionBannerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun redactionBanner_displays_privacyProtectedTitle() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 1,
                onDismiss = {}
            )
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Privacy Protected").assertExists()
    }

    @Test
    fun redactionBanner_displays_singleItemRedactionMessage() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 1,
                onDismiss = {}
            )
        }

        // Verify singular message format
        composeTestRule.onNodeWithText("Redacted 1 item (emails, phones, or IDs)").assertExists()
    }

    @Test
    fun redactionBanner_displays_multipleItemsRedactionMessage() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 3,
                onDismiss = {}
            )
        }

        // Verify plural message format
        composeTestRule.onNodeWithText("Redacted 3 items (emails, phones, or IDs)").assertExists()
    }

    @Test
    fun redactionBanner_displays_dismissButton() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 2,
                onDismiss = {}
            )
        }

        // Verify dismiss button is displayed
        composeTestRule.onNodeWithText("Dismiss").assertExists()
    }

    @Test
    fun redactionBanner_dismissButton_isClickable() {
        var dismissClicked = false

        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 1,
                onDismiss = { dismissClicked = true }
            )
        }

        // Click dismiss button
        composeTestRule.onNodeWithText("Dismiss").performClick()

        // Verify callback was invoked
        assert(dismissClicked)
    }

    @Test
    fun redactionBanner_displays_infoIcon() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 1,
                onDismiss = {}
            )
        }

        // Verify info icon is displayed
        composeTestRule.onNodeWithContentDescription("Privacy Info").assertExists()
    }

    @Test
    fun redactionBanner_isDisplayed_correctly() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 2,
                onDismiss = {}
            )
        }

        // Verify all main elements are displayed
        composeTestRule.onNodeWithText("Privacy Protected").assertIsDisplayed()
        composeTestRule.onNodeWithText("Redacted 2 items (emails, phones, or IDs)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed()
    }

    @Test
    fun redactionBanner_multipleClicks_callsCallbackMultipleTimes() {
        var dismissCount = 0

        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 1,
                onDismiss = { dismissCount++ }
            )
        }

        // Click dismiss button multiple times
        composeTestRule.onNodeWithText("Dismiss").performClick()
        composeTestRule.onNodeWithText("Dismiss").performClick()

        // Verify callback was invoked correct number of times
        assert(dismissCount == 2)
    }

    @Test
    fun redactionBanner_withZeroCount_stillDisplaysCorrectly() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 0,
                onDismiss = {}
            )
        }

        // Verify displays with zero count (edge case)
        composeTestRule.onNodeWithText("Redacted 0 items (emails, phones, or IDs)").assertExists()
    }

    @Test
    fun redactionBanner_withLargeCount_displaysCorrectly() {
        composeTestRule.setContent {
            RedactionBanner(
                redactionCount = 10,
                onDismiss = {}
            )
        }

        // Verify displays with large count
        composeTestRule.onNodeWithText("Redacted 10 items (emails, phones, or IDs)").assertExists()
    }
}
