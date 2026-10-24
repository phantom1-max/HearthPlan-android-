package com.example.ui

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.model.*
import com.example.ui.components.canvas.SelectedElement
import com.example.util.PdfExporter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class HearthplanViewModel(
    private val repository: ProjectRepository
) : ViewModel() {

    val userProjects: StateFlow<List<Project>> = repository.userProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val builtinTemplates: StateFlow<List<Project>> = repository.builtinTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _activeFloorIndex = MutableStateFlow(0)
    val activeFloorIndex: StateFlow<Int> = _activeFloorIndex.asStateFlow()

    val activeFloor: StateFlow<Floor?> = combine(_currentProject, _activeFloorIndex) { project, index ->
        project?.floors?.getOrNull(index) ?: project?.floors?.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lowerFloor: StateFlow<Floor?> = combine(_currentProject, _activeFloorIndex) { project, index ->
        if (project != null && index > 0) project.floors.getOrNull(index - 1) else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedElement = MutableStateFlow<SelectedElement?>(null)
    val selectedElement: StateFlow<SelectedElement?> = _selectedElement.asStateFlow()

    // Undo / Redo stacks
    private val undoStack = mutableListOf<Project>()
    private val redoStack = mutableListOf<Project>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Canvas navigation
    private val _zoomScale = MutableStateFlow(1.0f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _panOffset = MutableStateFlow(Offset.Zero)
    val panOffset: StateFlow<Offset> = _panOffset.asStateFlow()

    private val _snapToGrid = MutableStateFlow(true)
    val snapToGrid: StateFlow<Boolean> = _snapToGrid.asStateFlow()

    private val _showLowerFloorOverlay = MutableStateFlow(true)
    val showLowerFloorOverlay: StateFlow<Boolean> = _showLowerFloorOverlay.asStateFlow()

    private val _saveStatus = MutableStateFlow("Saved locally")
    val saveStatus: StateFlow<String> = _saveStatus.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeBuiltinTemplatesIfEmpty()
        }
    }

    private fun pushUndoState(project: Project) {
        undoStack.add(project)
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
    }

    private fun scheduleAutoSave(project: Project) {
        _saveStatus.value = "Saving..."
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(500)
            repository.saveProject(project)
            _saveStatus.value = "Saved locally"
        }
    }

    // ---------------- Project Setup & Switching ----------------

    fun createBlankProject(
        name: String,
        width1: Float,
        width2: Float,
        length1: Float,
        length2: Float,
        unit: MeasurementUnit,
        floorCount: Int
    ) {
        val count = floorCount.coerceIn(1, 30)
        val maxW = maxOf(width1, width2).coerceAtLeast(1f) // X dimension (Width)
        val maxL = maxOf(length1, length2).coerceAtLeast(1f) // Y dimension (Depth/Length)

        val floors = (0 until count).map { level ->
            Floor(
                id = UUID.randomUUID().toString(),
                name = when (level) {
                    0 -> "Ground Floor"
                    1 -> "First Floor"
                    2 -> "Second Floor"
                    3 -> "Third Floor"
                    else -> "Floor ${level + 1}"
                },
                level = level,
                length = maxW,
                breadth = maxL,
                rooms = emptyList(),
                furniture = emptyList()
            )
        }

        val newProject = Project(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "My House Plan" },
            unit = unit,
            plotLength = maxW,
            plotBreadth = maxL,
            plotWidth1 = width1.coerceAtLeast(1f),
            plotWidth2 = width2.coerceAtLeast(1f),
            plotLength1 = length1.coerceAtLeast(1f),
            plotLength2 = length2.coerceAtLeast(1f),
            floors = floors,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _activeFloorIndex.value = 0
        _selectedElement.value = null
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
        _currentProject.value = newProject

        scheduleAutoSave(newProject)
    }

    fun openProject(project: Project) {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
        _activeFloorIndex.value = 0
        _selectedElement.value = null
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
        _currentProject.value = project
        _saveStatus.value = "Saved locally"
    }

    fun cloneTemplate(template: Project) {
        viewModelScope.launch {
            val cloned = repository.cloneAsNewProject(template, "${template.name} (My Plan)")
            openProject(cloned)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
            if (_currentProject.value?.id == id) {
                _currentProject.value = null
            }
        }
    }

    fun closeProject() {
        _currentProject.value = null
        _selectedElement.value = null
    }

    // ---------------- Floor Navigation ----------------

    fun selectFloor(index: Int) {
        val proj = _currentProject.value ?: return
        if (index in proj.floors.indices) {
            _activeFloorIndex.value = index
            _selectedElement.value = null
        }
    }

    fun addFloor(name: String, length: Float, breadth: Float) {
        val proj = _currentProject.value ?: return
        pushUndoState(proj)

        val nextLevel = proj.floors.size
        val newFloor = Floor(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Floor ${nextLevel + 1}" },
            level = nextLevel,
            length = length.coerceAtLeast(1f),
            breadth = breadth.coerceAtLeast(1f),
            rooms = emptyList(),
            furniture = emptyList()
        )

        val updated = proj.copy(
            floors = proj.floors + newFloor,
            updatedAt = System.currentTimeMillis()
        )
        _currentProject.value = updated
        _activeFloorIndex.value = updated.floors.lastIndex
        _selectedElement.value = null
        scheduleAutoSave(updated)
    }

    fun renameFloor(floorId: String, newName: String) {
        val proj = _currentProject.value ?: return
        pushUndoState(proj)

        val updatedFloors = proj.floors.map { floor ->
            if (floor.id == floorId) floor.copy(name = newName) else floor
        }
        val updated = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())
        _currentProject.value = updated
        scheduleAutoSave(updated)
    }

    fun duplicateFloor(floorId: String) {
        val proj = _currentProject.value ?: return
        val target = proj.floors.find { it.id == floorId } ?: return
        pushUndoState(proj)

        val nextLevel = proj.floors.size
        val duplicatedRooms = target.rooms.map { r ->
            r.copy(id = UUID.randomUUID().toString())
        }
        val duplicatedFurniture = target.furniture.map { f ->
            f.copy(id = UUID.randomUUID().toString())
        }
        val duplicated = target.copy(
            id = UUID.randomUUID().toString(),
            name = "${target.name} (Copy)",
            level = nextLevel,
            rooms = duplicatedRooms,
            furniture = duplicatedFurniture
        )

        val updated = proj.copy(floors = proj.floors + duplicated, updatedAt = System.currentTimeMillis())
        _currentProject.value = updated
        _activeFloorIndex.value = updated.floors.lastIndex
        scheduleAutoSave(updated)
    }

    fun deleteFloor(floorId: String) {
        val proj = _currentProject.value ?: return
        if (proj.floors.size <= 1) return // Must keep at least 1 floor
        pushUndoState(proj)

        val updatedFloors = proj.floors.filterNot { it.id == floorId }
        val updated = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())
        _currentProject.value = updated
        _activeFloorIndex.value = 0.coerceAtMost(updated.floors.lastIndex)
        _selectedElement.value = null
        scheduleAutoSave(updated)
    }

    // ---------------- Project Info & Settings ----------------

    fun updateProjectName(name: String) {
        val proj = _currentProject.value ?: return
        val updated = proj.copy(name = name.ifBlank { "Untitled Plan" }, updatedAt = System.currentTimeMillis())
        _currentProject.value = updated
        scheduleAutoSave(updated)
    }

    fun toggleMeasurementUnit() {
        val proj = _currentProject.value ?: return
        val newUnit = if (proj.unit == MeasurementUnit.METERS) MeasurementUnit.FEET else MeasurementUnit.METERS
        val updated = proj.copy(unit = newUnit, updatedAt = System.currentTimeMillis())
        _currentProject.value = updated
        scheduleAutoSave(updated)
    }

    fun toggleSnapToGrid() {
        _snapToGrid.value = !_snapToGrid.value
    }

    fun toggleLowerFloorOverlay() {
        _showLowerFloorOverlay.value = !_showLowerFloorOverlay.value
    }

    // ---------------- Room Editing ----------------

    fun addRoom(
        roomType: RoomType,
        customName: String? = null,
        length: Float? = null,
        breadth: Float? = null
    ) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        pushUndoState(proj)

        val rLength = length ?: roomType.defaultLengthMeters
        val rBreadth = breadth ?: roomType.defaultBreadthMeters

        // Position intelligently inside the floor
        val existingCount = curFloor.rooms.size
        val posX = 1.0f + (existingCount % 3) * 1.5f
        val posY = 1.0f + (existingCount / 3) * 2.0f

        val newRoom = RoomItem(
            id = UUID.randomUUID().toString(),
            name = customName?.ifBlank { roomType.label } ?: roomType.label,
            type = roomType,
            x = posX.coerceAtMost(curFloor.length - rLength - 0.2f).coerceAtLeast(0.5f),
            y = posY.coerceAtMost(curFloor.breadth - rBreadth - 0.2f).coerceAtLeast(0.5f),
            length = rLength,
            breadth = rBreadth,
            doors = listOf(DoorOpening(UUID.randomUUID().toString(), WallEdge.BOTTOM, 0.5f, 0.9f, true)),
            windows = listOf(WindowOpening(UUID.randomUUID().toString(), WallEdge.TOP, 0.8f, 1.2f))
        )

        val updatedFloor = curFloor.copy(rooms = curFloor.rooms + newRoom)
        val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
        val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())

        _currentProject.value = updatedProj
        _selectedElement.value = SelectedElement.Room(newRoom.id)
        scheduleAutoSave(updatedProj)
    }

    fun updateRoom(updatedRoom: RoomItem) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return

        val updatedRooms = curFloor.rooms.map { if (it.id == updatedRoom.id) updatedRoom else it }
        val updatedFloor = curFloor.copy(rooms = updatedRooms)
        val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
        val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())

        _currentProject.value = updatedProj
        scheduleAutoSave(updatedProj)
    }

    fun setRoomDimensions(roomId: String, length: Float, breadth: Float) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        val room = curFloor.rooms.find { it.id == roomId } ?: return
        pushUndoState(proj)

        val updatedRoom = room.copy(
            length = length.coerceAtLeast(0.8f),
            breadth = breadth.coerceAtLeast(0.8f)
        )
        updateRoom(updatedRoom)
    }

    fun addDoorToRoom(roomId: String, wall: WallEdge, width: Float) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        val room = curFloor.rooms.find { it.id == roomId } ?: return
        pushUndoState(proj)

        val newDoor = DoorOpening(UUID.randomUUID().toString(), wall, 0.5f, width, true)
        updateRoom(room.copy(doors = room.doors + newDoor))
    }

    fun addWindowToRoom(roomId: String, wall: WallEdge, width: Float) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        val room = curFloor.rooms.find { it.id == roomId } ?: return
        pushUndoState(proj)

        val newWin = WindowOpening(UUID.randomUUID().toString(), wall, 0.8f, width)
        updateRoom(room.copy(windows = room.windows + newWin))
    }

    // ---------------- Furniture Editing ----------------

    fun addFurniture(
        templateId: String,
        name: String,
        category: FurnitureCategory,
        length: Float,
        breadth: Float,
        isCustom: Boolean = false,
        desc: String? = null
    ) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        pushUndoState(proj)

        // Try placing near selected room or floor center
        val centerPos = (selectedElement.value as? SelectedElement.Room)?.let { sel ->
            curFloor.rooms.find { it.id == sel.id }?.let { r ->
                Offset(r.x + (r.length - length) / 2f, r.y + (r.breadth - breadth) / 2f)
            }
        } ?: Offset(curFloor.length / 2f - length / 2f, curFloor.breadth / 2f - breadth / 2f)

        val newItem = FurnitureItem(
            id = UUID.randomUUID().toString(),
            templateId = templateId,
            name = name,
            category = category,
            x = centerPos.x.coerceAtLeast(0.5f),
            y = centerPos.y.coerceAtLeast(0.5f),
            length = length,
            breadth = breadth,
            rotationDegrees = 0f,
            isCustom = isCustom,
            customDescription = desc
        )

        val updatedFloor = curFloor.copy(furniture = curFloor.furniture + newItem)
        val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
        val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())

        _currentProject.value = updatedProj
        _selectedElement.value = SelectedElement.Furniture(newItem.id)
        scheduleAutoSave(updatedProj)
    }

    fun updateFurniture(updatedItem: FurnitureItem) {
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return

        val updatedFurn = curFloor.furniture.map { if (it.id == updatedItem.id) updatedItem else it }
        val updatedFloor = curFloor.copy(furniture = updatedFurn)
        val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
        val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())

        _currentProject.value = updatedProj
        scheduleAutoSave(updatedProj)
    }

    // ---------------- Selection & Deletion ----------------

    fun selectElement(element: SelectedElement?) {
        _selectedElement.value = element
    }

    fun deleteSelectedElement() {
        val sel = _selectedElement.value ?: return
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        pushUndoState(proj)

        val updatedFloor = when (sel) {
            is SelectedElement.Room -> curFloor.copy(rooms = curFloor.rooms.filterNot { it.id == sel.id })
            is SelectedElement.Furniture -> curFloor.copy(furniture = curFloor.furniture.filterNot { it.id == sel.id })
        }

        val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
        val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())

        _currentProject.value = updatedProj
        _selectedElement.value = null
        scheduleAutoSave(updatedProj)
    }

    fun duplicateSelectedElement() {
        val sel = _selectedElement.value ?: return
        val proj = _currentProject.value ?: return
        val curFloor = activeFloor.value ?: return
        pushUndoState(proj)

        when (sel) {
            is SelectedElement.Room -> {
                val room = curFloor.rooms.find { it.id == sel.id } ?: return
                val newRoom = room.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${room.name} (Copy)",
                    x = room.x + 0.5f,
                    y = room.y + 0.5f
                )
                val updatedFloor = curFloor.copy(rooms = curFloor.rooms + newRoom)
                val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
                val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())
                _currentProject.value = updatedProj
                _selectedElement.value = SelectedElement.Room(newRoom.id)
                scheduleAutoSave(updatedProj)
            }
            is SelectedElement.Furniture -> {
                val item = curFloor.furniture.find { it.id == sel.id } ?: return
                val newItem = item.copy(
                    id = UUID.randomUUID().toString(),
                    name = "${item.name} (Copy)",
                    x = item.x + 0.3f,
                    y = item.y + 0.3f
                )
                val updatedFloor = curFloor.copy(furniture = curFloor.furniture + newItem)
                val updatedFloors = proj.floors.map { if (it.id == curFloor.id) updatedFloor else it }
                val updatedProj = proj.copy(floors = updatedFloors, updatedAt = System.currentTimeMillis())
                _currentProject.value = updatedProj
                _selectedElement.value = SelectedElement.Furniture(newItem.id)
                scheduleAutoSave(updatedProj)
            }
        }
    }

    // ---------------- Zoom & Pan ----------------

    fun transform(zoomChange: Float, panChange: Offset) {
        val newZoom = (_zoomScale.value * zoomChange).coerceIn(0.4f, 3.5f)
        _zoomScale.value = newZoom
        _panOffset.value = _panOffset.value + panChange
    }

    fun zoomIn() {
        _zoomScale.value = (_zoomScale.value * 1.25f).coerceAtMost(3.5f)
    }

    fun zoomOut() {
        _zoomScale.value = (_zoomScale.value * 0.8f).coerceAtLeast(0.4f)
    }

    fun resetZoom() {
        _zoomScale.value = 1.0f
        _panOffset.value = Offset.Zero
    }

    // ---------------- Undo & Redo ----------------

    fun undo() {
        val current = _currentProject.value ?: return
        if (undoStack.isEmpty()) return

        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(current)
        _currentProject.value = previous
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        scheduleAutoSave(previous)
    }

    fun redo() {
        val current = _currentProject.value ?: return
        if (redoStack.isEmpty()) return

        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(current)
        _currentProject.value = next
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        scheduleAutoSave(next)
    }

    // ---------------- Export & Import ----------------

    fun exportPdf(context: Context): File? {
        val proj = _currentProject.value ?: return null
        return exportProjectToPdf(context, proj)
    }

    fun exportProjectToPdf(context: Context, project: Project): File? {
        return try {
            PdfExporter.exportProjectToPdf(context, project)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportJson(): String? {
        val proj = _currentProject.value ?: return null
        return repository.exportProjectToJson(proj)
    }

    fun importProjectFromJson(jsonStr: String): Boolean {
        val result = repository.importProjectFromJson(jsonStr)
        return if (result.isSuccess) {
            val imported = result.getOrThrow()
            viewModelScope.launch {
                repository.saveProject(imported)
                openProject(imported)
            }
            true
        } else {
            false
        }
    }
}
