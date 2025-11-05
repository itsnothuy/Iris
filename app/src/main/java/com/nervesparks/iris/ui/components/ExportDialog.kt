package com.nervesparks.iris.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nervesparks.iris.data.export.ExportFormat

/**
 * Dialog for selecting export format and options.
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.JSON) }
    var exportAll by remember { mutableStateOf(true) }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Export Conversations",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Format",
                    fontSize = 14.sp,
                    color = Color(0xff94a3b8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Format options
                FormatOption(
                    title = "JSON",
                    description = "Machine-readable with full metadata",
                    isSelected = selectedFormat == ExportFormat.JSON,
                    onClick = { selectedFormat = ExportFormat.JSON }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOption(
                    title = "Markdown",
                    description = "Human-readable with formatting",
                    isSelected = selectedFormat == ExportFormat.MARKDOWN,
                    onClick = { selectedFormat = ExportFormat.MARKDOWN }
                )

                Spacer(modifier = Modifier.height(8.dp))

                FormatOption(
                    title = "Plain Text",
                    description = "Simple text for universal compatibility",
                    isSelected = selectedFormat == ExportFormat.PLAIN_TEXT,
                    onClick = { selectedFormat = ExportFormat.PLAIN_TEXT }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xff94a3b8))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onExport(selectedFormat) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xff3b82f6)
                        )
                    ) {
                        Text("Export", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Color(0xff3b82f6).copy(alpha = 0.1f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xff3b82f6),
                unselectedColor = Color(0xff64748b)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = Color(0xff94a3b8),
                fontSize = 14.sp
            )
        }
    }
}
