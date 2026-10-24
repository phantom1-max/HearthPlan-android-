package com.example.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.MeasurementUnit
import com.example.model.RoomType

@Composable
fun AddRoomDialog(
    unit: MeasurementUnit,
    onDismiss: () -> Unit,
    onAddRoom: (type: RoomType, name: String, lengthMeters: Float, breadthMeters: Float) -> Unit
) {
    var selectedType by remember { mutableStateOf(RoomType.BEDROOM) }
    var roomName by remember { mutableStateOf(RoomType.BEDROOM.label) }

    var lengthDisplay by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", unit.toDisplayValue(RoomType.BEDROOM.defaultLengthMeters)))
    }
    var breadthDisplay by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", unit.toDisplayValue(RoomType.BEDROOM.defaultBreadthMeters)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Room", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Select Room Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(RoomType.values()) { type ->
                        val isSelected = selectedType == type
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedType = type
                                    roomName = type.label
                                    lengthDisplay = String.format(java.util.Locale.US, "%.1f", unit.toDisplayValue(type.defaultLengthMeters))
                                    breadthDisplay = String.format(java.util.Locale.US, "%.1f", unit.toDisplayValue(type.defaultBreadthMeters))
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isSelected) 4.dp else 1.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = roomName,
                    onValueChange = { roomName = it },
                    label = { Text("Room Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("room_name_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = lengthDisplay,
                        onValueChange = { lengthDisplay = it },
                        label = { Text("Length (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("room_length_input")
                    )
                    OutlinedTextField(
                        value = breadthDisplay,
                        onValueChange = { breadthDisplay = it },
                        label = { Text("Breadth (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("room_breadth_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lVal = lengthDisplay.toFloatOrNull() ?: unit.toDisplayValue(selectedType.defaultLengthMeters)
                    val bVal = breadthDisplay.toFloatOrNull() ?: unit.toDisplayValue(selectedType.defaultBreadthMeters)
                    val lMeters = unit.fromDisplayValue(lVal).coerceAtLeast(1.0f)
                    val bMeters = unit.fromDisplayValue(bVal).coerceAtLeast(1.0f)
                    onAddRoom(selectedType, roomName.trim(), lMeters, bMeters)
                },
                modifier = Modifier.testTag("confirm_add_room_button")
            ) {
                Text("Add to Floor")
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
