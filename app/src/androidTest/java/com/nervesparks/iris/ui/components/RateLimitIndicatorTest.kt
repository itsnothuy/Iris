package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for RateLimitIndicator component.
 * Tests cover visibility, countdown behavior, and content.
 */
@RunWith(AndroidJUnit4::class)
class RateLimitIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rateLimitIndicator_notVisible_whenNotRateLimited() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = false,
                initialCooldownSeconds = 30,
            )
        }

        // Verify indicator is not displayed
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertDoesNotExist()
    }

    @Test
    fun rateLimitIndicator_notVisible_whenCooldownIsZero() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 0,
            )
        }

        // Verify indicator is not displayed when cooldown is 0
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertDoesNotExist()
    }

    @Test
    fun rateLimitIndicator_visible_whenRateLimitedWithCooldown() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 30,
            )
        }

        // Verify indicator is displayed
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertIsDisplayed()
    }

    @Test
    fun rateLimitIndicator_showsCorrectTitle() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 30,
            )
        }

        // Verify title is displayed
        composeTestRule.onNodeWithText("Rate limit active").assertExists()
    }

    @Test
    fun rateLimitIndicator_showsCooldownTime() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 45,
            )
        }

        // Verify cooldown message is displayed with correct seconds
        composeTestRule.onNodeWithText("Cooldown: 45s remaining").assertExists()
    }

    @Test
    fun rateLimitIndicator_showsIcon() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 30,
            )
        }

        // Verify icon is displayed
        composeTestRule.onNodeWithContentDescription("Rate limit cooldown").assertIsDisplayed()
    }

    @Test
    fun rateLimitIndicator_updatesCooldownValue() {
        var cooldown = 30

        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = cooldown,
            )
        }

        // Initially shows 30 seconds
        composeTestRule.onNodeWithText("Cooldown: 30s remaining").assertExists()

        // Update cooldown value
        cooldown = 15
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = cooldown,
            )
        }

        // Now shows 15 seconds
        composeTestRule.onNodeWithText("Cooldown: 15s remaining").assertExists()
    }

    @Test
    fun rateLimitIndicator_hidesWhenRateLimitClears() {
        var isRateLimited = true

        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = isRateLimited,
                initialCooldownSeconds = 30,
            )
        }

        // Initially visible
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertIsDisplayed()

        // Change state to not rate limited
        isRateLimited = false
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = isRateLimited,
                initialCooldownSeconds = 30,
            )
        }

        // Should no longer be visible
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertDoesNotExist()
    }

    @Test
    fun rateLimitIndicator_transitionsFromHiddenToVisible() {
        var isRateLimited = false
        var cooldown = 0

        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = isRateLimited,
                initialCooldownSeconds = cooldown,
            )
        }

        // Initially not visible
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertDoesNotExist()

        // Transition to rate limited with cooldown
        isRateLimited = true
        cooldown = 60
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = isRateLimited,
                initialCooldownSeconds = cooldown,
            )
        }

        // Now visible
        composeTestRule.onNodeWithTag("rate-limit-indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cooldown: 60s remaining").assertExists()
    }

    @Test
    fun rateLimitIndicator_handlesVariousCooldownValues() {
        val testCases = listOf(1, 10, 30, 60, 120)

        testCases.forEach { seconds ->
            composeTestRule.setContent {
                RateLimitIndicator(
                    isRateLimited = true,
                    initialCooldownSeconds = seconds,
                )
            }

            // Verify correct cooldown is displayed
            composeTestRule.onNodeWithText("Cooldown: ${seconds}s remaining").assertExists()
        }
    }

    @Test
    fun rateLimitIndicator_showsVisualStyling() {
        composeTestRule.setContent {
            RateLimitIndicator(
                isRateLimited = true,
                initialCooldownSeconds = 30,
            )
        }

        // Verify the indicator is actually rendered on screen
        composeTestRule.onNodeWithTag("rate-limit-indicator")
            .assertIsDisplayed()
    }
}
