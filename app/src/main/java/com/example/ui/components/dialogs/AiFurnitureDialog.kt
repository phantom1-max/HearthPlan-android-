package com.example.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.FurnitureCategory
import com.example.model.MeasurementUnit

@Composable
fun AiFurnitureDialog(
    unit: MeasurementUnit,
    onDismiss: () -> Unit,
    onAddCustomFurniture: (name: String, category: FurnitureCategory, length: Float, breadth: Float, description: String) -> Unit
) {
    var prompt by remember {
        mutableStateOf("Compact L-shaped corner study desk with 3 filing drawers and space for a dual monitor setup")
    }
    var customName by remember { mutableStateOf("Custom Study Desk") }
    var selectedCategory by remember { mutableStateOf(FurnitureCategory.TABLES_DESKS) }

    var lengthDisplay by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(1.6f)))
    }
    var breadthDisplay by remember {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(1.2f)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Furniture Design", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Honest Security Notice Banner
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "Security & Service Status",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                "To protect sensitive credentials, client-side API keys are withheld from APK distribution. Fully automated generative diffusion models require a secure proxy server. You can specify exact custom architectural dimensions and descriptions below for immediate placement.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = prompt,
                    onValueChange = {
                        prompt = it
                        if (it.length > 5 && customName.startsWith("Custom")) {
                            customName = it.take(24).trim().capitalize(java.util.Locale.US)
                        }
                    },
                    label = { Text("Furniture Description") },
                    placeholder = { Text("e.g. Ergonomic standing desk with monitor riser...") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input")
                )

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text("Item Name on Plan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("ai_custom_name_input")
                )

                Text("Furniture Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val cats = listOf(FurnitureCategory.TABLES_DESKS, FurnitureCategory.STORAGE, FurnitureCategory.SOFAS_CHAIRS, FurnitureCategory.CUSTOM)
                    for (cat in cats) {
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName.take(12)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Footprint Dimensions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = lengthDisplay,
                        onValueChange = { lengthDisplay = it },
                        label = { Text("Width (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = breadthDisplay,
                        onValueChange = { breadthDisplay = it },
                        label = { Text("Depth (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lVal = lengthDisplay.toFloatOrNull() ?: 1.5f
                    val bVal = breadthDisplay.toFloatOrNull() ?: 1.0f
                    val lMeters = unit.fromDisplayValue(lVal).coerceAtLeast(0.3f)
                    val bMeters = unit.fromDisplayValue(bVal).coerceAtLeast(0.3f)
                    onAddCustomFurniture(
                        customName.ifBlank { "Custom Furniture" },
                        selectedCategory,
                        lMeters,
                        bMeters,
                        prompt
                    )
                },
                modifier = Modifier.testTag("confirm_create_custom_furniture")
            ) {
                Text("Place Custom Item")
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
