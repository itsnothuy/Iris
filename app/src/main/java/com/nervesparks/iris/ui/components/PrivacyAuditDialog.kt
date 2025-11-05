package com.nervesparks.iris.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nervesparks.iris.data.privacy.PrivacyAuditInfo
import kotlinx.coroutines.launch

/**
 * Dialog displaying privacy audit information.
 * Shows data usage, storage, and privacy status.
 */
@Composable
fun PrivacyAuditDialog(
    onDismiss: () -> Unit,
    privacyAuditInfo: PrivacyAuditInfo? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xff1e293b)
            )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                item {
                    Text(
                        text = "Privacy Audit",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (privacyAuditInfo != null) {
                    item {
                        AuditSection(title = "Data Storage") {
                            AuditItem(
                                label = "Total Conversations",
                                value = privacyAuditInfo.totalConversations.toString()
                            )
                            AuditItem(
                                label = "Total Messages",
                                value = privacyAuditInfo.totalMessages.toString()
                            )
                            AuditItem(
                                label = "Storage Used",
                                value = formatBytes(privacyAuditInfo.storageBytes)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        AuditSection(title = "Privacy Status") {
                            AuditItem(
                                label = "Data Encrypted",
                                value = if (privacyAuditInfo.dataEncrypted) "Yes" else "No",
                                isSuccess = privacyAuditInfo.dataEncrypted
                            )
                            AuditItem(
                                label = "Network Activity",
                                value = if (privacyAuditInfo.networkActivity) "Yes" else "None",
                                isSuccess = !privacyAuditInfo.networkActivity
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All data is stored locally on your device. No information is transmitted to external servers.",
                            color = Color(0xff94a3b8),
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                } else {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(32.dp),
                            color = Color(0xff3b82f6)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xff3b82f6)
                        )
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xff0f172a),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun AuditItem(
    label: String,
    value: String,
    isSuccess: Boolean? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xff94a3b8),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = when (isSuccess) {
                true -> Color(0xff10b981)
                false -> Color(0xFFEF4444)
                null -> Color.White
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    
    while (value >= 1024 && unitIndex < units.size - 1) {
        value /= 1024
        unitIndex++
    }
    
    return "%.2f %s".format(value, units[unitIndex])
}
