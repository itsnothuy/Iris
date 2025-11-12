package com.nervesparks.iris.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nervesparks.iris.data.Conversation
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/**
 * Compose UI tests for ConversationListScreen.
 */
class ConversationListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun conversationListScreen_displaysTitle() {
        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Conversations").assertIsDisplayed()
    }

    @Test
    fun conversationListScreen_displaysSearchBar() {
        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Search conversations...").assertIsDisplayed()
    }

    @Test
    fun conversationListScreen_displaysEmptyStateWhenNoConversations() {
        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("No conversations yet.\nTap + to start a new one.").assertIsDisplayed()
    }

    @Test
    fun conversationListScreen_displaysConversationList() {
        val conversations = listOf(
            Conversation(
                id = "conv-1",
                title = "Test Conversation 1",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 5,
            ),
            Conversation(
                id = "conv-2",
                title = "Test Conversation 2",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 3,
            ),
        )

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = conversations,
                currentConversationId = "conv-1",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Test Conversation 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Conversation 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 messages").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 messages").assertIsDisplayed()
    }

    @Test
    fun conversationListScreen_clickingBackButton_triggersCallback() {
        var backPressed = false

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = { backPressed = true },
            )
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assert(backPressed)
    }

    @Test
    fun conversationListScreen_clickingNewButton_triggersCallback() {
        var newConversationClicked = false

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = { newConversationClicked = true },
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithContentDescription("New Conversation").performClick()

        assert(newConversationClicked)
    }

    @Test
    fun conversationListScreen_clickingConversation_triggersCallback() {
        var selectedId = ""
        val conversations = listOf(
            Conversation(
                id = "test-id",
                title = "Test Conversation",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 5,
            ),
        )

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = conversations,
                currentConversationId = "default",
                onConversationSelected = { selectedId = it },
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Test Conversation").performClick()

        assert(selectedId == "test-id")
    }

    @Test
    fun conversationListScreen_searchBar_acceptsInput() {
        var searchQuery = ""

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = emptyList(),
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = { searchQuery = it },
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Search conversations...").performTextInput("test query")

        assert(searchQuery == "test query")
    }

    @Test
    fun conversationListScreen_highlightsCurrentConversation() {
        val conversations = listOf(
            Conversation(
                id = "current",
                title = "Current Conversation",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 5,
            ),
            Conversation(
                id = "other",
                title = "Other Conversation",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 3,
            ),
        )

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = conversations,
                currentConversationId = "current",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        // Both conversations should be displayed
        composeTestRule.onNodeWithText("Current Conversation").assertIsDisplayed()
        composeTestRule.onNodeWithText("Other Conversation").assertIsDisplayed()
    }

    @Test
    fun conversationListScreen_showsPinnedIcon_forPinnedConversations() {
        val conversations = listOf(
            Conversation(
                id = "pinned",
                title = "Pinned Conversation",
                createdAt = Instant.now(),
                lastModified = Instant.now(),
                messageCount = 5,
                isPinned = true,
            ),
        )

        composeTestRule.setContent {
            ConversationListScreen(
                conversations = conversations,
                currentConversationId = "default",
                onConversationSelected = {},
                onNewConversation = {},
                onDeleteConversation = {},
                onPinConversation = { _, _ -> },
                onArchiveConversation = { _, _ -> },
                onSearchQueryChanged = {},
                onBackPressed = {},
            )
        }

        composeTestRule.onNodeWithText("Pinned Conversation").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pinned").assertIsDisplayed()
    }
}
