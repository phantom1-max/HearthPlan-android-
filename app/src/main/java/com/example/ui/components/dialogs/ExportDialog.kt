package com.example.ui.components.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.model.Project
import java.io.File
import java.io.FileOutputStream

@Composable
fun ExportDialog(
    project: Project,
    onDismiss: () -> Unit,
    onExportPdf: () -> File?,
    onExportJson: () -> String?,
    onSaveAsTemplate: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }

    fun sharePdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${project.name} - Hearthplan Drawing")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Floor Plan PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openPdfFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Floor Plan PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found. Sharing file instead...", Toast.LENGTH_SHORT).show()
            sharePdfFile(file)
        }
    }

    fun shareJsonBackup(jsonContent: String) {
        try {
            val sanitized = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val backupFile = File(context.cacheDir, "${sanitized}_backup.hearthplan")
            FileOutputStream(backupFile).use { it.write(jsonContent.toByteArray()) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "${project.name} - Hearthplan Project Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Project Backup"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error exporting backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export & Share", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // PDF Export Option
                Surface(
                    onClick = {
                        isExporting = true
                        val file = onExportPdf()
                        isExporting = false
                        if (file != null && file.exists()) {
                            generatedPdfFile = file
                            exportStatusMessage = "PDF generated: ${file.name} (${file.length() / 1024} KB)"
                            sharePdfFile(file)
                        } else {
                            exportStatusMessage = "Failed to generate PDF document."
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().testTag("export_pdf_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Vector PDF Plan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            Text("A4 printable blueprint with all ${project.floors.size} floor(s), room dimensions, doors & furniture", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }

                // If PDF already generated, show quick action buttons
                if (generatedPdfFile != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "✓ PDF Ready: ${generatedPdfFile?.name}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { generatedPdfFile?.let { openPdfFile(it) } },
                                    modifier = Modifier.weight(1f).testTag("open_pdf_action_button")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open PDF")
                                }
                                OutlinedButton(
                                    onClick = { generatedPdfFile?.let { sharePdfFile(it) } },
                                    modifier = Modifier.weight(1f).testTag("share_pdf_action_button")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share")
                                }
                            }
                        }
                    }
                }

                // JSON Backup Option
                Surface(
                    onClick = {
                        val json = onExportJson()
                        if (json != null) {
                            shareJsonBackup(json)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("export_json_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Project File (.hearthplan)", fontWeight = FontWeight.Bold)
                            Text("Portable offline JSON backup for restoring on any device", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }

                // Save to Local Templates
                Surface(
                    onClick = {
                        onSaveAsTemplate()
                        Toast.makeText(context, "Saved to your local template library!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().testTag("save_as_template_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Save as Starter Template", fontWeight = FontWeight.Bold)
                            Text("Add this design to your local templates gallery", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }

                if (exportStatusMessage != null) {
                    Text(
                        exportStatusMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Planning Disclaimer
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Architectural Disclaimer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Exported diagrams and calculations are intended solely for spatial floor planning and conceptual design. They do not constitute certified construction or structural engineering documentation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
