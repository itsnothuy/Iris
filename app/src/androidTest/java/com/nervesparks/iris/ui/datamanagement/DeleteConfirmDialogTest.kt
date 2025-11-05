package com.nervesparks.iris.ui.datamanagement

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.ui.DeleteConfirmDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for DeleteConfirmDialog.
 * Tests deletion confirmation dialog display and actions.
 */
@RunWith(AndroidJUnit4::class)
class DeleteConfirmDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun deleteConfirmDialog_displaysTitle() {
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("⚠️ Clear All Data").assertExists()
    }

    @Test
    fun deleteConfirmDialog_displaysWarningText() {
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("This action will permanently delete:").assertExists()
    }

    @Test
    fun deleteConfirmDialog_displaysItemsToDelete() {
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("• All conversations", substring = true).assertExists()
        composeTestRule.onNodeWithText("• All messages", substring = true).assertExists()
        composeTestRule.onNodeWithText("• All conversation history", substring = true).assertExists()
    }

    @Test
    fun deleteConfirmDialog_displaysCannotUndoWarning() {
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText(
            "This action cannot be undone",
            substring = true
        ).assertExists()
    }

    @Test
    fun deleteConfirmDialog_displaysActionButtons() {
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Cancel").assertExists()
        composeTestRule.onNodeWithText("Delete All").assertExists()
    }

    @Test
    fun deleteConfirmDialog_cancelButton_callsDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = { dismissed = true },
                onConfirm = {}
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun deleteConfirmDialog_deleteButton_callsConfirm() {
        var confirmed = false
        composeTestRule.setContent {
            DeleteConfirmDialog(
                onDismiss = {},
                onConfirm = { confirmed = true }
            )
        }

        composeTestRule.onNodeWithText("Delete All").performClick()
        assert(confirmed)
    }
}
