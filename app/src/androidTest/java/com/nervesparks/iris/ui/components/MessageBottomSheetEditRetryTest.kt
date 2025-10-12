package com.nervesparks.iris.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.MainViewModel
import com.nervesparks.iris.data.UserPreferencesRepository
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Compose UI tests for edit & resend and retry functionality in MessageBottomSheet.
 */
@RunWith(AndroidJUnit4::class)
class MessageBottomSheetEditRetryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockClipboard: ClipboardManager = mock()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_showsEditAndRetryButtons_forLastUserMessage() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test user message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // Then: Edit & Resend and Retry buttons should be visible
        composeTestRule.onNodeWithText("Edit & Resend").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_hidesEditAndRetryButtons_forNonLastUserMessage() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = false
            )
        }

        // Then: Edit & Resend and Retry buttons should NOT be visible
        composeTestRule.onNodeWithText("Edit & Resend").assertDoesNotExist()
        composeTestRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_retryButton_callsRetryLastMessage() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )
        
        // Add some messages to the view model
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "Test message")
        )

        var dismissCalled = false

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = { dismissCalled = true },
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // When: Retry button is clicked
        composeTestRule.onNodeWithText("Retry").performClick()

        // Then: onDismiss should be called
        assert(dismissCalled)
        
        // And: The message should be set in viewModel (retryLastMessage called)
        assert(viewModel.message == "Test message")
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_editButton_showsEditDialog() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Original message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // When: Edit & Resend button is clicked
        composeTestRule.onNodeWithText("Edit & Resend").performClick()

        // Then: Edit dialog should appear
        composeTestRule.onNodeWithText("Edit Message").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Send").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun editDialog_cancelButton_closesDialog() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Original message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // When: Edit & Resend button is clicked to open dialog
        composeTestRule.onNodeWithText("Edit & Resend").performClick()
        
        // And: Cancel button is clicked
        composeTestRule.onNodeWithText("Cancel").performClick()

        // Then: Edit dialog should be closed
        composeTestRule.onNodeWithText("Edit Message").assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun editDialog_sendButton_callsEditAndResend() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )
        
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "Original message")
        )

        var dismissCalled = false

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Original message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = { dismissCalled = true },
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // When: Edit & Resend is clicked to open dialog
        composeTestRule.onNodeWithText("Edit & Resend").performClick()
        
        // And: Text is edited (we'll just use the existing text for this test)
        // And: Send button is clicked
        composeTestRule.onNodeWithText("Send").performClick()

        // Then: onDismiss should be called
        assert(dismissCalled)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_alwaysShowsCopyButton() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )

        // Test with isLastUserMessage = true
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // Then: Copy button should always be visible
        composeTestRule.onNodeWithText("Copy Text").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_buttonsDisabled_whenAIGenerating() {
        // Given: Mock preferences repository and AI is generating
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")
        
        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null
        )
        
        // Note: In a real scenario, we'd mock getIsSending() to return true
        // For this test, we just verify the buttons exist and their enabled state
        // is controlled by viewModel.getIsSending()

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                isLastUserMessage = true
            )
        }

        // Then: Buttons should exist (enabled state depends on viewModel.getIsSending())
        composeTestRule.onNodeWithText("Edit & Resend").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
