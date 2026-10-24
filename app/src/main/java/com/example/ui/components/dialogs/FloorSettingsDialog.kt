package com.example.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.Floor

@Composable
fun FloorSettingsDialog(
    floor: Floor,
    totalFloors: Int,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var floorName by remember { mutableStateOf(floor.name) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${floor.name}?") },
            text = { Text("Are you sure you want to delete this floor and all its partitions and furniture? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Floor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Floor Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = floorName,
                    onValueChange = { floorName = it },
                    label = { Text("Floor Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_floor_input")
                )

                Text(
                    "Dimensions: ${String.format(java.util.Locale.US, "%.1f m × %.1f m", floor.length, floor.breadth)} | ${floor.rooms.size} Rooms, ${floor.furniture.size} Furniture items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider()

                OutlinedButton(
                    onClick = {
                        onDuplicate()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("duplicate_floor_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Duplicate Floor")
                }

                if (totalFloors > 1) {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("delete_floor_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Floor")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (floorName.isNotBlank() && floorName != floor.name) {
                        onRename(floorName.trim())
                    }
                    onDismiss()
                },
                modifier = Modifier.testTag("save_floor_settings_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
