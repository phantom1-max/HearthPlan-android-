package com.example.ui.components.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.*

@Composable
fun FurnitureLibraryDialog(
    unit: MeasurementUnit,
    onDismiss: () -> Unit,
    onOpenAiGenerator: () -> Unit,
    onAddFurniture: (templateId: String, name: String, category: FurnitureCategory, length: Float, breadth: Float) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FurnitureCategory?>(null) }
    var selectedItem by remember { mutableStateOf(FurnitureCatalog.items.first()) }

    var lengthDisplay by remember(selectedItem, unit) {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(selectedItem.defaultLengthMeters)))
    }
    var breadthDisplay by remember(selectedItem, unit) {
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(selectedItem.defaultBreadthMeters)))
    }

    val filteredItems = remember(searchQuery, selectedCategory) {
        FurnitureCatalog.items.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == null || item.category == selectedCategory
            matchesQuery && matchesCat
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Weekend, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Furniture Library", fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // AI Furniture Banner / Action
                Surface(
                    onClick = {
                        onDismiss()
                        onOpenAiGenerator()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Generate Custom Furniture", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("Describe unique items or custom built-ins", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search beds, sofas, desks, appliances...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Items") }
                        )
                    }
                    items(FurnitureCategory.values().filterNot { it == FurnitureCategory.CUSTOM }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.displayName) }
                        )
                    }
                }

                // Furniture Catalog List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredItems) { item ->
                        val isSelected = selectedItem.templateId == item.templateId
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItem = item
                                    lengthDisplay = String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(item.defaultLengthMeters))
                                    breadthDisplay = String.format(java.util.Locale.US, "%.2f", unit.toDisplayValue(item.defaultBreadthMeters))
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${item.category.displayName} • ${unit.format(item.defaultLengthMeters)} × ${unit.format(item.defaultBreadthMeters)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Dimension customization inputs before placement
                Text("Dimensions for Placement", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = lengthDisplay,
                        onValueChange = { lengthDisplay = it },
                        label = { Text("Length (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("furniture_length_input")
                    )
                    OutlinedTextField(
                        value = breadthDisplay,
                        onValueChange = { breadthDisplay = it },
                        label = { Text("Breadth (${unit.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("furniture_breadth_input")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lVal = lengthDisplay.toFloatOrNull() ?: unit.toDisplayValue(selectedItem.defaultLengthMeters)
                    val bVal = breadthDisplay.toFloatOrNull() ?: unit.toDisplayValue(selectedItem.defaultBreadthMeters)
                    val lMeters = unit.fromDisplayValue(lVal).coerceAtLeast(0.3f)
                    val bMeters = unit.fromDisplayValue(bVal).coerceAtLeast(0.3f)
                    onAddFurniture(selectedItem.templateId, selectedItem.name, selectedItem.category, lMeters, bMeters)
                },
                modifier = Modifier.testTag("confirm_place_furniture_button")
            ) {
                Text("Place on Plan")
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
