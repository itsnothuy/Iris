package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for EmptyState component.
 * Tests the display of welcome message and conversation starters.
 */
@RunWith(AndroidJUnit4::class)
class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyState_displays_welcomeMessage() {
        composeTestRule.setContent {
            EmptyState()
        }

        // Verify welcome message is displayed
        composeTestRule.onNodeWithText("Hello, Ask me Anything").assertExists()
    }

    @Test
    fun emptyState_displays_conversationStarters() {
        composeTestRule.setContent {
            EmptyState()
        }

        // Verify all conversation starters are displayed
        composeTestRule.onNodeWithText("Explains complex topics simply.").assertExists()
        composeTestRule.onNodeWithText("Remembers previous inputs.").assertExists()
        composeTestRule.onNodeWithText("May sometimes be inaccurate.").assertExists()
    }

    @Test
    fun emptyState_conversationStarter_isClickable() {
        var clickedStarter: String? = null

        composeTestRule.setContent {
            EmptyState(onStarterClick = { starter ->
                clickedStarter = starter
            })
        }

        // Click on first conversation starter
        composeTestRule.onNodeWithText("Explains complex topics simply.")
            .performClick()

        // Verify callback was invoked with correct starter
        assert(clickedStarter == "Explains complex topics simply.")
    }

    @Test
    fun emptyState_multipleStarters_canBeClicked() {
        val clickedStarters = mutableListOf<String>()

        composeTestRule.setContent {
            EmptyState(onStarterClick = { starter ->
                clickedStarters.add(starter)
            })
        }

        // Click on all starters
        composeTestRule.onNodeWithText("Explains complex topics simply.")
            .performClick()
        composeTestRule.onNodeWithText("Remembers previous inputs.")
            .performClick()
        composeTestRule.onNodeWithText("May sometimes be inaccurate.")
            .performClick()

        // Verify all callbacks were invoked
        assert(clickedStarters.size == 3)
        assert(clickedStarters.contains("Explains complex topics simply."))
        assert(clickedStarters.contains("Remembers previous inputs."))
        assert(clickedStarters.contains("May sometimes be inaccurate."))
    }

    @Test
    fun emptyState_isVisibleInViewport() {
        composeTestRule.setContent {
            EmptyState()
        }

        // Verify the component is displayed
        composeTestRule.onNodeWithText("Hello, Ask me Anything")
            .assertIsDisplayed()
    }
}
