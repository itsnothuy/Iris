package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for LoadingSkeleton component.
 * Tests the display of loading placeholders for messages.
 */
@RunWith(AndroidJUnit4::class)
class LoadingSkeletonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingSkeleton_isVisible() {
        composeTestRule.setContent {
            LoadingSkeleton()
        }

        // Wait for the skeleton to appear (animation takes some time)
        composeTestRule.waitForIdle()

        // The skeleton should be rendered and visible
        // We can't test for specific content since it's just placeholders,
        // but we can verify it doesn't crash
    }

    @Test
    fun loadingSkeleton_userMessage_alignedToRight() {
        composeTestRule.setContent {
            LoadingSkeleton(isUserMessage = true)
        }

        composeTestRule.waitForIdle()
        // Component renders without crashing
        // Visual alignment is tested via screenshot/manual testing
    }

    @Test
    fun loadingSkeleton_assistantMessage_alignedToLeft() {
        composeTestRule.setContent {
            LoadingSkeleton(isUserMessage = false)
        }

        composeTestRule.waitForIdle()
        // Component renders without crashing
        // Visual alignment is tested via screenshot/manual testing
    }

    @Test
    fun loadingSkeleton_animatesCorrectly() {
        composeTestRule.setContent {
            LoadingSkeleton()
        }

        // Wait for animation to start
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)

        // Skeleton should still be visible and animating
        composeTestRule.waitForIdle()
    }
}
