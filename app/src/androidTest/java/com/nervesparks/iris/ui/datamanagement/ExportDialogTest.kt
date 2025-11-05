package com.nervesparks.iris.ui.datamanagement

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.export.ExportFormat
import com.nervesparks.iris.ui.components.ExportDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for ExportDialog.
 * Tests dialog display and format selection.
 */
@RunWith(AndroidJUnit4::class)
class ExportDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun exportDialog_displaysTitle() {
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = {}
            )
        }

        composeTestRule.onNodeWithText("Export Conversations").assertExists()
    }

    @Test
    fun exportDialog_displaysAllFormats() {
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = {}
            )
        }

        // Verify all format options are displayed
        composeTestRule.onNodeWithText("JSON").assertExists()
        composeTestRule.onNodeWithText("Markdown").assertExists()
        composeTestRule.onNodeWithText("Plain Text").assertExists()
    }

    @Test
    fun exportDialog_displaysFormatDescriptions() {
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = {}
            )
        }

        // Verify descriptions
        composeTestRule.onNodeWithText("Machine-readable with full metadata").assertExists()
        composeTestRule.onNodeWithText("Human-readable with formatting").assertExists()
        composeTestRule.onNodeWithText("Simple text for universal compatibility").assertExists()
    }

    @Test
    fun exportDialog_displaysActionButtons() {
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = {}
            )
        }

        composeTestRule.onNodeWithText("Cancel").assertExists()
        composeTestRule.onNodeWithText("Export").assertExists()
    }

    @Test
    fun exportDialog_cancelButton_callsDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = { dismissed = true },
                onExport = {}
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(dismissed)
    }

    @Test
    fun exportDialog_exportButton_callsOnExport() {
        var exported = false
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = { exported = true }
            )
        }

        composeTestRule.onNodeWithText("Export").performClick()
        assert(exported)
    }

    @Test
    fun exportDialog_formatSelection_isClickable() {
        composeTestRule.setContent {
            ExportDialog(
                onDismiss = {},
                onExport = {}
            )
        }

        // Click on Markdown format
        composeTestRule.onNodeWithText("Markdown").performClick()
        
        // Should still display the dialog
        composeTestRule.onNodeWithText("Export Conversations").assertExists()
    }
}
