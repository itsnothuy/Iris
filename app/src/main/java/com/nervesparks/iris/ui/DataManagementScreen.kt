package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.R
import com.nervesparks.iris.ui.components.ExportDialog
import com.nervesparks.iris.ui.components.PrivacyAuditDialog
import com.nervesparks.iris.ui.components.DeleteConfirmDialog

/**
 * Data Management Screen
 * Provides export, import, privacy audit, and data deletion capabilities.
 */
@Composable
fun DataManagementScreen(
    onExportClicked: () -> Unit,
    onImportClicked: () -> Unit,
    onPrivacyAuditClicked: () -> Unit,
    onDeleteDataClicked: () -> Unit
) {
    var showExportDialog by remember { mutableStateOf(false) }
    var showPrivacyAuditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            item {
                Text(
                    text = "Data Management",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Export & Import section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                ) {
                    DataManagementRow(
                        text = "Export Conversations",
                        description = "Export your data to JSON, Markdown, or plain text",
                        iconRes = R.drawable.data_exploration_models_svgrepo_com,
                        onClick = { showExportDialog = true }
                    )

                    DataManagementDivider()

                    DataManagementRow(
                        text = "Import Conversations",
                        description = "Restore conversations from an export file",
                        iconRes = R.drawable.data_exploration_models_svgrepo_com,
                        onClick = onImportClicked
                    )
                }
            }

            // Privacy section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                ) {
                    DataManagementRow(
                        text = "Privacy Audit",
                        description = "View data usage and storage information",
                        iconRes = R.drawable.information_outline_svgrepo_com,
                        onClick = { showPrivacyAuditDialog = true }
                    )
                }
            }

            // Danger zone
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xff0f172a),
                            shape = RoundedCornerShape(12.dp),
                        )
                ) {
                    DataManagementRow(
                        text = "Clear All Data",
                        description = "Permanently delete all conversations and messages",
                        iconRes = R.drawable.setting_4_svgrepo_com,
                        onClick = { showDeleteConfirmDialog = true },
                        isDanger = true
                    )
                }
            }
        }
    }

    // Show dialogs
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format ->
                showExportDialog = false
                onExportClicked()
            }
        )
    }

    if (showPrivacyAuditDialog) {
        PrivacyAuditDialog(
            onDismiss = { showPrivacyAuditDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = {
                showDeleteConfirmDialog = false
                onDeleteDataClicked()
            }
        )
    }
}

@Composable
private fun DataManagementRow(
    text: String,
    description: String,
    iconRes: Int,
    onClick: () -> Unit,
    isDanger: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            tint = if (isDanger) Color(0xFFEF4444) else Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                color = if (isDanger) Color(0xFFEF4444) else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color(0xff94a3b8),
                fontSize = 14.sp
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.setting_4_svgrepo_com),
            contentDescription = "Navigate",
            tint = Color(0xff64748b),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DataManagementDivider() {
    Divider(
        color = Color(0xff1e293b),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
