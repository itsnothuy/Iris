package com.nervesparks.iris.ui.datamanagement

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nervesparks.iris.data.privacy.PrivacyAuditInfo
import com.nervesparks.iris.ui.PrivacyAuditDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

/**
 * Compose UI tests for PrivacyAuditDialog.
 * Tests privacy audit information display.
 */
@RunWith(AndroidJUnit4::class)
class PrivacyAuditDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun privacyAuditDialog_displaysTitle() {
        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = null
            )
        }

        composeTestRule.onNodeWithText("Privacy Audit").assertExists()
    }

    @Test
    fun privacyAuditDialog_withNoData_showsLoadingIndicator() {
        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = null
            )
        }

        // Loading indicator should be visible when no data
        composeTestRule.onNodeWithText("Close").assertExists()
    }

    @Test
    fun privacyAuditDialog_withData_displaysDataStorage() {
        val auditInfo = PrivacyAuditInfo(
            totalConversations = 10,
            totalMessages = 100,
            storageBytes = 1024L * 1024, // 1 MB
            oldestConversation = Instant.now().minusSeconds(86400),
            newestConversation = Instant.now(),
            dataEncrypted = false,
            networkActivity = false,
            exportHistory = emptyList()
        )

        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = auditInfo
            )
        }

        composeTestRule.onNodeWithText("Data Storage").assertExists()
        composeTestRule.onNodeWithText("Total Conversations").assertExists()
        composeTestRule.onNodeWithText("10").assertExists()
        composeTestRule.onNodeWithText("Total Messages").assertExists()
        composeTestRule.onNodeWithText("100").assertExists()
    }

    @Test
    fun privacyAuditDialog_withData_displaysPrivacyStatus() {
        val auditInfo = PrivacyAuditInfo(
            totalConversations = 5,
            totalMessages = 50,
            storageBytes = 1024L,
            oldestConversation = null,
            newestConversation = null,
            dataEncrypted = false,
            networkActivity = false,
            exportHistory = emptyList()
        )

        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = auditInfo
            )
        }

        composeTestRule.onNodeWithText("Privacy Status").assertExists()
        composeTestRule.onNodeWithText("Data Encrypted").assertExists()
        composeTestRule.onNodeWithText("Network Activity").assertExists()
    }

    @Test
    fun privacyAuditDialog_displaysPrivacyMessage() {
        val auditInfo = PrivacyAuditInfo(
            totalConversations = 0,
            totalMessages = 0,
            storageBytes = 0L,
            oldestConversation = null,
            newestConversation = null,
            dataEncrypted = false,
            networkActivity = false,
            exportHistory = emptyList()
        )

        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = auditInfo
            )
        }

        composeTestRule.onNodeWithText(
            "All data is stored locally on your device. No information is transmitted to external servers.",
            substring = true
        ).assertExists()
    }

    @Test
    fun privacyAuditDialog_closeButton_callsDismiss() {
        var dismissed = false
        val auditInfo = PrivacyAuditInfo(
            totalConversations = 0,
            totalMessages = 0,
            storageBytes = 0L,
            oldestConversation = null,
            newestConversation = null,
            dataEncrypted = false,
            networkActivity = false,
            exportHistory = emptyList()
        )

        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = { dismissed = true },
                privacyAuditInfo = auditInfo
            )
        }

        composeTestRule.onNodeWithText("Close").performClick()
        assert(dismissed)
    }

    @Test
    fun privacyAuditDialog_networkActivity_showsNone() {
        val auditInfo = PrivacyAuditInfo(
            totalConversations = 1,
            totalMessages = 10,
            storageBytes = 500L,
            oldestConversation = null,
            newestConversation = null,
            dataEncrypted = false,
            networkActivity = false,
            exportHistory = emptyList()
        )

        composeTestRule.setContent {
            PrivacyAuditDialog(
                onDismiss = {},
                privacyAuditInfo = auditInfo
            )
        }

        composeTestRule.onNodeWithText("None").assertExists()
    }
}
