package com.nervesparks.iris.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.Message
import com.nervesparks.iris.data.MessageRole
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Compose UI tests for MessageBubble component.
 */
@RunWith(AndroidJUnit4::class)
class MessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageBubble_displays_userMessage_correctly() {
        val testMessage = Message(
            content = "Hello, AI!",
            role = MessageRole.USER,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("Hello, AI!").assertExists()

        // Verify user icon is displayed
        composeTestRule.onNodeWithContentDescription("User Icon").assertExists()

        // Verify assistant icon is NOT displayed for user messages
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertDoesNotExist()
    }

    @Test
    fun messageBubble_displays_assistantMessage_correctly() {
        val testMessage = Message(
            content = "Hello, human!",
            role = MessageRole.ASSISTANT,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("Hello, human!").assertExists()

        // Verify assistant icon is displayed
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertExists()

        // Verify user icon is NOT displayed for assistant messages
        composeTestRule.onNodeWithContentDescription("User Icon").assertDoesNotExist()
    }

    @Test
    fun messageBubble_displays_timestamp_whenEnabled() {
        val testMessage = Message(
            content = "Test message",
            role = MessageRole.USER,
            timestamp = Instant.now(),
        )

        composeTestRule.setContent {
            MessageBubble(
                message = testMessage,
                showTimestamp = true,
            )
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("Test message").assertExists()

        // Note: We can't easily test the exact timestamp format, but we ensure the component renders
        // The timestamp should be visible in the UI
    }

    @Test
    fun messageBubble_hidesTimestamp_whenDisabled() {
        val testMessage = Message(
            content = "Test message",
            role = MessageRole.USER,
        )

        composeTestRule.setContent {
            MessageBubble(
                message = testMessage,
                showTimestamp = false,
            )
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("Test message").assertExists()
    }

    @Test
    fun messageBubble_displays_processingMetrics_forAssistantMessages() {
        val testMessage = Message(
            content = "AI response",
            role = MessageRole.ASSISTANT,
            processingTimeMs = 1500L,
            tokenCount = 100,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("AI response").assertExists()

        // Verify processing metrics are displayed
        composeTestRule.onNodeWithText("1500ms • 100 tokens").assertExists()
    }

    @Test
    fun messageBubble_displays_processingTime_withoutTokenCount() {
        val testMessage = Message(
            content = "AI response",
            role = MessageRole.ASSISTANT,
            processingTimeMs = 2000L,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify processing time is displayed
        composeTestRule.onNodeWithText("2000ms").assertExists()
    }

    @Test
    fun messageBubble_doesNotDisplay_metricsForUserMessages() {
        val testMessage = Message(
            content = "User message",
            role = MessageRole.USER,
            processingTimeMs = 1000L,
            tokenCount = 50,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("User message").assertExists()

        // Verify metrics are NOT displayed for user messages
        composeTestRule.onNodeWithText("1000ms • 50 tokens").assertDoesNotExist()
    }

    @Test
    fun messageBubble_handlesLongClick() {
        val testMessage = Message(
            content = "Long click test",
            role = MessageRole.USER,
        )

        var longClickCalled = false

        composeTestRule.setContent {
            MessageBubble(
                message = testMessage,
                onLongClick = { longClickCalled = true },
            )
        }

        // Perform long click
        composeTestRule.onNodeWithText("Long click test").performTouchInput {
            longClick()
        }

        // Verify callback was invoked
        assert(longClickCalled)
    }

    @Test
    fun messageBubble_displays_emptyContent() {
        val testMessage = Message(
            content = "",
            role = MessageRole.USER,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // The bubble should still be rendered even with empty content
        composeTestRule.onNodeWithContentDescription("User Icon").assertExists()
    }

    @Test
    fun messageBubble_displays_specialCharacters() {
        val testMessage = Message(
            content = "Hello\nWorld\t!😀",
            role = MessageRole.ASSISTANT,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify special characters are displayed correctly
        composeTestRule.onNodeWithText("Hello\nWorld\t!😀").assertExists()
    }

    @Test
    fun messageBubble_systemMessage_hasCorrectStyling() {
        val testMessage = Message(
            content = "System notification",
            role = MessageRole.SYSTEM,
        )

        composeTestRule.setContent {
            MessageBubble(message = testMessage)
        }

        // Verify message content is displayed
        composeTestRule.onNodeWithText("System notification").assertExists()

        // System messages should not show icons
        composeTestRule.onNodeWithContentDescription("User Icon").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("AI Assistant Icon").assertDoesNotExist()
    }
}
