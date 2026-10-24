package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.ui.HearthplanViewModel
import com.example.ui.components.canvas.FloorPlanCanvas
import com.example.ui.components.canvas.SelectedElement
import com.example.ui.components.dialogs.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: HearthplanViewModel,
    onBackToHome: () -> Unit
) {
    val context = LocalContext.current

    val project by viewModel.currentProject.collectAsState()
    val activeFloor by viewModel.activeFloor.collectAsState()
    val lowerFloor by viewModel.lowerFloor.collectAsState()
    val activeFloorIndex by viewModel.activeFloorIndex.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val zoomScale by viewModel.zoomScale.collectAsState()
    val panOffset by viewModel.panOffset.collectAsState()
    val snapToGrid by viewModel.snapToGrid.collectAsState()
    val showLowerOverlay by viewModel.showLowerFloorOverlay.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    // Dialog visibility states
    var showAddRoomDialog by remember { mutableStateOf(false) }
    var showFurnitureLibrary by remember { mutableStateOf(false) }
    var showAiFurnitureDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showFloorSettingsDialog by remember { mutableStateOf(false) }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var showRenameProjectDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }

    val curProj = project ?: return

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
                // Main Toolbar
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showRenameProjectDialog = true }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = curProj.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Rename Project",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackToHome, modifier = Modifier.testTag("editor_back_button")) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Home")
                        }
                    },
                    actions = {
                        // Undo & Redo
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Undo")
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(Icons.Default.Redo, contentDescription = "Redo")
                        }

                        // Zoom Controls
                        IconButton(onClick = { viewModel.zoomOut() }, modifier = Modifier.testTag("zoom_out_button")) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
                        }
                        TextButton(onClick = { viewModel.resetZoom() }, modifier = Modifier.testTag("reset_zoom_button")) {
                            Text(String.format(java.util.Locale.US, "%d%%", (zoomScale * 100).toInt()))
                        }
                        IconButton(onClick = { viewModel.zoomIn() }, modifier = Modifier.testTag("zoom_in_button")) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
                        }

                        // Unit Switcher
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.toggleMeasurementUnit() },
                            label = { Text(curProj.unit.symbol.uppercase()) },
                            modifier = Modifier.testTag("unit_toggle_button")
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        // Export PDF / Share Button
                        FilledTonalButton(
                            onClick = { showExportDialog = true },
                            modifier = Modifier.testTag("export_menu_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PDF / Export", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    }
                )

                // Chrome-Style Horizontal Floor Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for ((index, floor) in curProj.floors.withIndex()) {
                        val isActive = index == activeFloorIndex
                        Surface(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clickable { viewModel.selectFloor(index) }
                                .testTag("floor_tab_$index"),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp),
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = if (isActive) 4.dp else 1.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = floor.name,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Floor Settings",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { showFloorSettingsDialog = true }
                                            .testTag("floor_settings_button")
                                    )
                                }
                            }
                        }
                    }

                    // "+" Tab to add floor
                    Surface(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { showAddFloorDialog = true }
                            .testTag("add_floor_tab_button"),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Floor",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Ghost Lower Floor Overlay Toggle (if on upper floor)
                    if (activeFloorIndex > 0) {
                        FilterChip(
                            selected = showLowerOverlay,
                            onClick = { viewModel.toggleLowerFloorOverlay() },
                            leadingIcon = {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            label = { Text("Lower Floor Ghost") },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }

                    // Snap to Grid Toggle
                    FilterChip(
                        selected = snapToGrid,
                        onClick = { viewModel.toggleSnapToGrid() },
                        leadingIcon = {
                            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        label = { Text("Snap") },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Auto-save Status Indicator
                    Text(
                        text = saveStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline)
            }
        },
        floatingActionButton = {
            // Google Keep-inspired Floating Action Button in bottom-right corner
            Column(horizontalAlignment = Alignment.End) {
                // Expanded Popover Menu
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .widthIn(min = 180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(6.dp)) {
                            // 1. Add Floor
                            DropdownMenuItem(
                                text = { Text("Add Floor", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showFabMenu = false
                                    showAddFloorDialog = true
                                },
                                modifier = Modifier.testTag("fab_menu_add_floor")
                            )
                            // 2. Add Room
                            DropdownMenuItem(
                                text = { Text("Add Room", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showFabMenu = false
                                    showAddRoomDialog = true
                                },
                                modifier = Modifier.testTag("fab_menu_add_room")
                            )
                            // 3. Add Furniture
                            DropdownMenuItem(
                                text = { Text("Add Furniture", fontWeight = FontWeight.SemiBold) },
                                leadingIcon = { Icon(Icons.Default.Weekend, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showFabMenu = false
                                    showFurnitureLibrary = true
                                },
                                modifier = Modifier.testTag("fab_menu_add_furniture")
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("floating_plus_button")
                ) {
                    Icon(
                        if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add Floor Plan Element"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val curFloor = activeFloor
            if (curFloor != null) {
                // Interactive 2D Canvas
                FloorPlanCanvas(
                    project = curProj,
                    activeFloor = curFloor,
                    lowerFloor = lowerFloor,
                    showLowerFloorOverlay = showLowerOverlay,
                    snapToGrid = snapToGrid,
                    selectedElement = selectedElement,
                    onSelectElement = { viewModel.selectElement(it) },
                    onUpdateRoom = { viewModel.updateRoom(it) },
                    onUpdateFurniture = { viewModel.updateFurniture(it) },
                    zoomScale = zoomScale,
                    panOffset = panOffset,
                    onTransform = { z, p -> viewModel.transform(z, p) }
                )

                // Live Floating Dimension Toolbar when Room is selected
                val selectedRoom = (selectedElement as? SelectedElement.Room)?.let { sel ->
                    curFloor.rooms.find { it.id == sel.id }
                }
                if (selectedRoom != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .testTag("room_dimension_hud"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    selectedRoom.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "Area: ${curProj.unit.formatArea(selectedRoom.areaSqMeters)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Quick Length & Breadth Dimension Display / Steppers
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("W: ${curProj.unit.format(selectedRoom.length)}", fontWeight = FontWeight.SemiBold)
                                IconButton(
                                    onClick = {
                                        viewModel.setRoomDimensions(selectedRoom.id, selectedRoom.length + 0.2f, selectedRoom.breadth)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "W+")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.setRoomDimensions(selectedRoom.id, (selectedRoom.length - 0.2f).coerceAtLeast(1f), selectedRoom.breadth)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "W-")
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("D: ${curProj.unit.format(selectedRoom.breadth)}", fontWeight = FontWeight.SemiBold)
                                IconButton(
                                    onClick = {
                                        viewModel.setRoomDimensions(selectedRoom.id, selectedRoom.length, selectedRoom.breadth + 0.2f)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "D+")
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.setRoomDimensions(selectedRoom.id, selectedRoom.length, (selectedRoom.breadth - 0.2f).coerceAtLeast(1f))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "D-")
                                }
                            }

                            // Add Door & Window actions
                            IconButton(
                                onClick = { viewModel.addDoorToRoom(selectedRoom.id, WallEdge.BOTTOM, 0.9f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.SensorDoor, contentDescription = "Add Door", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = { viewModel.addWindowToRoom(selectedRoom.id, WallEdge.TOP, 1.2f) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Window, contentDescription = "Add Window", tint = MaterialTheme.colorScheme.tertiary)
                            }

                            // Duplicate & Delete
                            IconButton(onClick = { viewModel.duplicateSelectedElement() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                            }
                            IconButton(onClick = { viewModel.deleteSelectedElement() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { viewModel.selectElement(null) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close HUD")
                            }
                        }
                    }
                }

                // Live Floating Dimension Toolbar when Furniture is selected
                val selectedFurn = (selectedElement as? SelectedElement.Furniture)?.let { sel ->
                    curFloor.furniture.find { it.id == sel.id }
                }
                if (selectedFurn != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp)
                            .testTag("furniture_dimension_hud"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column {
                                Text(
                                    selectedFurn.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    "${curProj.unit.format(selectedFurn.length)} × ${curProj.unit.format(selectedFurn.breadth)} • Rot: ${selectedFurn.rotationDegrees.toInt()}°",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Rotate 45 button
                            FilledTonalButton(
                                onClick = {
                                    val newRot = (selectedFurn.rotationDegrees + 45f) % 360f
                                    viewModel.updateFurniture(selectedFurn.copy(rotationDegrees = newRot))
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("45°")
                            }

                            // Duplicate & Delete
                            IconButton(onClick = { viewModel.duplicateSelectedElement() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                            }
                            IconButton(onClick = { viewModel.deleteSelectedElement() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                            IconButton(onClick = { viewModel.selectElement(null) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close HUD")
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------------- Dialogs ----------------

    if (showAddRoomDialog) {
        AddRoomDialog(
            unit = curProj.unit,
            onDismiss = { showAddRoomDialog = false },
            onAddRoom = { type, name, length, breadth ->
                showAddRoomDialog = false
                viewModel.addRoom(type, name, length, breadth)
            }
        )
    }

    if (showFurnitureLibrary) {
        FurnitureLibraryDialog(
            unit = curProj.unit,
            onDismiss = { showFurnitureLibrary = false },
            onOpenAiGenerator = { showAiFurnitureDialog = true },
            onAddFurniture = { templateId, name, category, length, breadth ->
                showFurnitureLibrary = false
                viewModel.addFurniture(templateId, name, category, length, breadth)
            }
        )
    }

    if (showAiFurnitureDialog) {
        AiFurnitureDialog(
            unit = curProj.unit,
            onDismiss = { showAiFurnitureDialog = false },
            onAddCustomFurniture = { name, category, length, breadth, description ->
                showAiFurnitureDialog = false
                viewModel.addFurniture(
                    templateId = "custom_ai",
                    name = name,
                    category = category,
                    length = length,
                    breadth = breadth,
                    isCustom = true,
                    desc = description
                )
            }
        )
    }

    if (showExportDialog) {
        ExportDialog(
            project = curProj,
            onDismiss = { showExportDialog = false },
            onExportPdf = { viewModel.exportPdf(context) },
            onExportJson = { viewModel.exportJson() },
            onSaveAsTemplate = {
                viewModel.cloneTemplate(curProj.copy(isBuiltinTemplate = true, name = "${curProj.name} (Template)"))
            }
        )
    }

    if (showFloorSettingsDialog && activeFloor != null) {
        FloorSettingsDialog(
            floor = activeFloor!!,
            totalFloors = curProj.floors.size,
            onDismiss = { showFloorSettingsDialog = false },
            onRename = { newName -> viewModel.renameFloor(activeFloor!!.id, newName) },
            onDuplicate = { viewModel.duplicateFloor(activeFloor!!.id) },
            onDelete = { viewModel.deleteFloor(activeFloor!!.id) }
        )
    }

    if (showAddFloorDialog) {
        var floorName by remember { mutableStateOf("Floor ${curProj.floors.size + 1}") }
        var lengthDisplay by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", curProj.unit.toDisplayValue(curProj.plotLength))) }
        var breadthDisplay by remember { mutableStateOf(String.format(java.util.Locale.US, "%.1f", curProj.unit.toDisplayValue(curProj.plotBreadth))) }

        AlertDialog(
            onDismissRequest = { showAddFloorDialog = false },
            title = { Text("Add Floor", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = floorName,
                        onValueChange = { floorName = it },
                        label = { Text("Floor Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = lengthDisplay,
                            onValueChange = { lengthDisplay = it },
                            label = { Text("Length (${curProj.unit.symbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = breadthDisplay,
                            onValueChange = { breadthDisplay = it },
                            label = { Text("Breadth (${curProj.unit.symbol})") },
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
                        val lVal = lengthDisplay.toFloatOrNull() ?: curProj.unit.toDisplayValue(curProj.plotLength)
                        val bVal = breadthDisplay.toFloatOrNull() ?: curProj.unit.toDisplayValue(curProj.plotBreadth)
                        viewModel.addFloor(
                            floorName.trim(),
                            curProj.unit.fromDisplayValue(lVal),
                            curProj.unit.fromDisplayValue(bVal)
                        )
                        showAddFloorDialog = false
                    }
                ) {
                    Text("Add Floor")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFloorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRenameProjectDialog) {
        var newTitle by remember { mutableStateOf(curProj.name) }
        AlertDialog(
            onDismissRequest = { showRenameProjectDialog = false },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Project Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.updateProjectName(newTitle.trim())
                        }
                        showRenameProjectDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameProjectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
