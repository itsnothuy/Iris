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
import com.nervesparks.iris.ui.MessageBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Compose UI tests for message action functionality (copy, share, delete) in MessageBottomSheet.
 */
@RunWith(AndroidJUnit4::class)
class MessageActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockClipboard: ClipboardManager = mock()
    private val mockUserPreferencesRepository: UserPreferencesRepository = mock()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_showsCopyButton() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
            )
        }

        // Then: Copy button should be visible
        composeTestRule.onNodeWithText("Copy Text").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_showsShareButton() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
            )
        }

        // Then: Share button should be visible
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_showsDeleteButton_whenMessageIndexProvided() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
                messageIndex = 5,
            )
        }

        // Then: Delete button should be visible
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_hidesDeleteButton_whenMessageIndexNotProvided() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
                messageIndex = -1,
            )
        }

        // Then: Delete button should not be visible
        composeTestRule.onNodeWithText("Delete").assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_deleteButton_showsConfirmationDialog() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
                messageIndex = 5,
            )
        }

        // When: Delete button is clicked
        composeTestRule.onNodeWithText("Delete").performClick()

        // Then: Confirmation dialog should appear
        composeTestRule.onNodeWithText("Delete Message").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Are you sure you want to delete this message? This action cannot be undone.",
        ).assertIsDisplayed()
        // Check for dialog buttons
        composeTestRule.onAllNodesWithText("Delete").assertCountEquals(2) // One in sheet, one in dialog
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_deleteConfirmation_cancelButton_closesDialog() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
                messageIndex = 5,
            )
        }

        // When: Delete button is clicked and then Cancel is clicked
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Then: Confirmation dialog should be dismissed
        composeTestRule.onNodeWithText("Delete Message").assertDoesNotExist()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_deleteConfirmation_deleteButton_deletesMessage() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
        )

        // Add test messages to the viewModel
        viewModel.messages = listOf(
            mapOf("role" to "user", "content" to "Message 1"),
            mapOf("role" to "assistant", "content" to "Response 1"),
            mapOf("role" to "user", "content" to "Message 2"),
        )

        var dismissCalled = false

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Message 2",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = { dismissCalled = true },
                sheetState = sheetState,
                messageIndex = 2,
            )
        }

        val initialMessageCount = viewModel.messages.size

        // When: Delete button is clicked and confirmed
        composeTestRule.onNodeWithText("Delete").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Delete")[1].performClick() // Click the second Delete button (in dialog)
        composeTestRule.waitForIdle()

        // Then: Message should be deleted
        assert(viewModel.messages.size == initialMessageCount - 1)
        assert(dismissCalled)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_deleteButton_disabledWhenAIGenerating() {
        // Given: Mock preferences repository and set AI generating state
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
        )
        // Simulate AI generating by setting the isSending state
        viewModel.isSending = true

        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState()
            MessageBottomSheet(
                message = "Test message",
                clipboard = mockClipboard,
                context = context,
                viewModel = viewModel,
                onDismiss = {},
                sheetState = sheetState,
                messageIndex = 5,
            )
        }

        // Then: Delete button should be disabled
        composeTestRule.onNodeWithText("Delete").assertIsNotEnabled()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun messageBottomSheet_copyAndShareAndDeleteButtons_allDisplayed() {
        // Given: Mock preferences repository
        whenever(mockUserPreferencesRepository.getDefaultModelName()).thenReturn("")

        val viewModel = MainViewModel(
            userPreferencesRepository = mockUserPreferencesRepository,
            messageRepository = null,
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
                messageIndex = 3,
            )
        }

        // Then: All action buttons should be visible
        composeTestRule.onNodeWithText("Copy Text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }
}
