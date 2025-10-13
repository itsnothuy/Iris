package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ThermalHint component.
 * Tests cover visibility, content, and behavior under different thermal/rate-limit states.
 */
@RunWith(AndroidJUnit4::class)
class ThermalHintTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun thermalHint_notVisible_whenBothFalse() {
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = false,
                isThermalThrottled = false
            )
        }

        // Verify hint is not displayed
        composeTestRule.onNodeWithContentDescription("Performance hint").assertDoesNotExist()
    }

    @Test
    fun thermalHint_visible_whenRateLimited() {
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = true,
                isThermalThrottled = false
            )
        }

        // Verify hint is displayed
        composeTestRule.onNodeWithContentDescription("Performance hint").assertIsDisplayed()
        
        // Verify correct message for rate-limited state
        composeTestRule.onNodeWithText("High activity detected").assertExists()
        composeTestRule.onNodeWithText("Streaming slowed to maintain stability").assertExists()
    }

    @Test
    fun thermalHint_visible_whenThermalThrottled() {
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = false,
                isThermalThrottled = true
            )
        }

        // Verify hint is displayed
        composeTestRule.onNodeWithContentDescription("Performance hint").assertIsDisplayed()
        
        // Verify correct message for thermal throttle state
        composeTestRule.onNodeWithText("Device warming up").assertExists()
        composeTestRule.onNodeWithText("Streaming slowed to cool down device").assertExists()
    }

    @Test
    fun thermalHint_visible_whenBothTrue() {
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = true,
                isThermalThrottled = true
            )
        }

        // Verify hint is displayed
        composeTestRule.onNodeWithContentDescription("Performance hint").assertIsDisplayed()
        
        // Verify correct message for both conditions
        composeTestRule.onNodeWithText("Device warming up • Slowing down").assertExists()
        composeTestRule.onNodeWithText("Reducing speed to prevent overheating").assertExists()
    }

    @Test
    fun thermalHint_showsWarningIcon_whenVisible() {
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = true,
                isThermalThrottled = false
            )
        }

        // Verify warning icon is displayed
        composeTestRule.onNodeWithContentDescription("Performance hint").assertIsDisplayed()
    }

    @Test
    fun thermalHint_hidesWhenStateChanges_toAllFalse() {
        var isRateLimited = true
        var isThermalThrottled = false
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }

        // Initially visible
        composeTestRule.onNodeWithContentDescription("Performance hint").assertIsDisplayed()
        
        // Change state to all false
        isRateLimited = false
        isThermalThrottled = false
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }
        
        // Should no longer be visible
        composeTestRule.onNodeWithContentDescription("Performance hint").assertDoesNotExist()
    }

    @Test
    fun thermalHint_transitionsFromRateLimited_toThermalThrottled() {
        var isRateLimited = true
        var isThermalThrottled = false
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }

        // Initially shows rate-limited message
        composeTestRule.onNodeWithText("High activity detected").assertExists()
        
        // Change to thermal throttle
        isRateLimited = false
        isThermalThrottled = true
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }
        
        // Now shows thermal throttle message
        composeTestRule.onNodeWithText("Device warming up").assertExists()
        composeTestRule.onNodeWithText("High activity detected").assertDoesNotExist()
    }

    @Test
    fun thermalHint_messageChanges_whenBothBecomesTrue() {
        var isRateLimited = true
        var isThermalThrottled = false
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }

        // Initially shows rate-limited message
        composeTestRule.onNodeWithText("High activity detected").assertExists()
        
        // Change to both true
        isRateLimited = true
        isThermalThrottled = true
        
        composeTestRule.setContent {
            ThermalHint(
                isRateLimited = isRateLimited,
                isThermalThrottled = isThermalThrottled
            )
        }
        
        // Now shows combined message
        composeTestRule.onNodeWithText("Device warming up • Slowing down").assertExists()
        composeTestRule.onNodeWithText("High activity detected").assertDoesNotExist()
    }
}
