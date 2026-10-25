package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FurnitureCatalog
import com.example.model.FurnitureCategory
import com.example.model.FurnitureTemplate
import com.example.model.MeasurementUnit
import com.example.ui.components.canvas.ArchitecturalVectorRenderer

@Composable
fun FurniturePreviewCanvas(
    template: FurnitureTemplate,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val dummyItem = com.example.model.FurnitureItem(
            id = "preview",
            templateId = template.templateId,
            name = template.name,
            category = template.category,
            x = 0f,
            y = 0f,
            length = template.defaultLengthMeters,
            breadth = template.defaultBreadthMeters,
            rotationDegrees = 0f
        )
        val pad = 4f
        val availW = size.width - pad * 2
        val availH = size.height - pad * 2
        val maxDim = maxOf(template.defaultLengthMeters, template.defaultBreadthMeters, 0.4f)
        val scale = minOf(availW / maxDim, availH / maxDim)

        val drawW = template.defaultLengthMeters * scale
        val drawH = template.defaultBreadthMeters * scale
        val drawX = (size.width - drawW) / 2f
        val drawY = (size.height - drawH) / 2f

        ArchitecturalVectorRenderer.drawFurniture(
            drawScope = this,
            item = dummyItem,
            screenX = drawX,
            screenY = drawY,
            screenWidth = drawW,
            screenHeight = drawH,
            isSelected = false
        )
    }
}

@Composable
fun FurnitureLibrarySideDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    unit: MeasurementUnit,
    onItemTapped: (FurnitureTemplate) -> Unit,
    onDragItemStart: (FurnitureTemplate, Offset) -> Unit,
    onDragItemMove: (Offset) -> Unit,
    onDragItemEnd: (FurnitureTemplate, Offset) -> Unit,
    onOpenAiCustomDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FurnitureCategory?>(null) }

    val filteredItems = remember(searchQuery, selectedCategory) {
        FurnitureCatalog.items.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                item.name.contains(searchQuery, ignoreCase = true) ||
                item.category.displayName.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(280)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(240)) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(min = 280.dp, max = 340.dp)
                .shadow(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Weekend,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Furniture Library",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Drag & drop onto canvas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_furniture_drawer")) {
                        Icon(Icons.Default.Close, contentDescription = "Close library")
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search beds, tables, chairs...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("furniture_search_input")
                )

                // Category Chips (Horizontal Scrollable)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("All (${FurnitureCatalog.items.size})") }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.BEDS,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.BEDS) null else FurnitureCategory.BEDS },
                        label = { Text("Beds") },
                        leadingIcon = { Icon(Icons.Default.Bed, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.TABLES_DESKS,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.TABLES_DESKS) null else FurnitureCategory.TABLES_DESKS },
                        label = { Text("Tables") },
                        leadingIcon = { Icon(Icons.Default.TableRestaurant, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.SOFAS_CHAIRS,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.SOFAS_CHAIRS) null else FurnitureCategory.SOFAS_CHAIRS },
                        label = { Text("Chairs & Sofas") },
                        leadingIcon = { Icon(Icons.Default.Chair, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.STORAGE,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.STORAGE) null else FurnitureCategory.STORAGE },
                        label = { Text("Storage") }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.KITCHEN,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.KITCHEN) null else FurnitureCategory.KITCHEN },
                        label = { Text("Kitchen") }
                    )
                    FilterChip(
                        selected = selectedCategory == FurnitureCategory.BATHROOM,
                        onClick = { selectedCategory = if (selectedCategory == FurnitureCategory.BATHROOM) null else FurnitureCategory.BATHROOM },
                        label = { Text("Bathroom") }
                    )
                }

                // Drag instruction banner
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Hold & drag to position on canvas, or tap to place at center",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                // Furniture Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(filteredItems, key = { it.templateId }) { template ->
                        FurnitureDrawerItemCard(
                            template = template,
                            unit = unit,
                            onTap = { onItemTapped(template) },
                            onDragStart = { rootPos -> onDragItemStart(template, rootPos) },
                            onDragMove = { rootPos -> onDragItemMove(rootPos) },
                            onDragEnd = { rootPos -> onDragItemEnd(template, rootPos) }
                        )
                    }

                    item {
                        // AI Custom Furniture Action at bottom of list
                        OutlinedCard(
                            onClick = onOpenAiCustomDialog,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .testTag("drawer_custom_ai_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Custom Dimension / AI Item", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Define custom size & furniture parameters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FurnitureDrawerItemCard(
    template: FurnitureTemplate,
    unit: MeasurementUnit,
    onTap: () -> Unit,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit
) {
    var itemRootOffset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                itemRootOffset = coordinates.positionInRoot()
            }
            .pointerInput(template) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        onDragStart(itemRootOffset + localOffset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDragMove(itemRootOffset + change.position)
                    },
                    onDragEnd = {
                        onDragEnd(itemRootOffset)
                    },
                    onDragCancel = {
                        onDragEnd(itemRootOffset)
                    }
                )
            }
            .clickable { onTap() }
            .testTag("drawer_item_${template.templateId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle indicator
            Icon(
                Icons.Default.DragIndicator,
                contentDescription = "Drag to place",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // 2D Architectural Preview
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                color = Color(0xFFFBFBFA)
            ) {
                FurniturePreviewCanvas(
                    template = template,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Item Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = "${unit.format(template.defaultLengthMeters)} × ${unit.format(template.defaultBreadthMeters)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quick Add '+' Button
            IconButton(
                onClick = onTap,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("add_item_${template.templateId}")
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add to canvas", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
