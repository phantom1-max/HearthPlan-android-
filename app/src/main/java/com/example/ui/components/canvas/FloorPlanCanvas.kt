package com.example.ui.components.canvas

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.round

enum class ResizeHandle {
    NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT,
    TOP_EDGE, BOTTOM_EDGE, LEFT_EDGE, RIGHT_EDGE,
    ROTATE
}

sealed class SelectedElement {
    data class Room(val id: String) : SelectedElement()
    data class Furniture(val id: String) : SelectedElement()
}

@Composable
fun FloorPlanCanvas(
    project: Project,
    activeFloor: Floor,
    lowerFloor: Floor?,
    showLowerFloorOverlay: Boolean,
    snapToGrid: Boolean,
    selectedElement: SelectedElement?,
    onSelectElement: (SelectedElement?) -> Unit,
    onUpdateRoom: (RoomItem) -> Unit,
    onUpdateFurniture: (FurnitureItem) -> Unit,
    zoomScale: Float,
    panOffset: Offset,
    onTransform: (zoomChange: Float, panChange: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    // Current dragging handle
    var activeHandle by remember { mutableStateOf(ResizeHandle.NONE) }
    var isMovingElement by remember { mutableStateOf(false) }
    var dragStartWorldPos by remember { mutableStateOf(Offset.Zero) }
    var initialElementBounds by remember { mutableStateOf<Pair<Offset, Size>?>(null) }
    var initialRotation by remember { mutableStateOf(0f) }

    // Canvas size
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    // Pixels per meter at scale = 1.0f
    val basePixelsPerMeter = remember(canvasSize, project.plotLength, project.plotBreadth) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val fitX = (canvasSize.width * 0.8f) / maxOf(project.plotLength, 1f)
            val fitY = (canvasSize.height * 0.75f) / maxOf(project.plotBreadth, 1f)
            minOf(fitX, fitY).coerceIn(20f, 120f)
        } else {
            40f
        }
    }

    val currentPixelsPerMeter = basePixelsPerMeter * zoomScale

    // Coordinate conversions
    fun worldToScreen(worldX: Float, worldY: Float): Offset {
        val centerX = canvasSize.width / 2f + panOffset.x
        val centerY = canvasSize.height / 2f + panOffset.y
        val plotOriginX = centerX - (project.plotLength * currentPixelsPerMeter) / 2f
        val plotOriginY = centerY - (project.plotBreadth * currentPixelsPerMeter) / 2f
        return Offset(
            plotOriginX + worldX * currentPixelsPerMeter,
            plotOriginY + worldY * currentPixelsPerMeter
        )
    }

    fun screenToWorld(screenPos: Offset): Offset {
        val centerX = canvasSize.width / 2f + panOffset.x
        val centerY = canvasSize.height / 2f + panOffset.y
        val plotOriginX = centerX - (project.plotLength * currentPixelsPerMeter) / 2f
        val plotOriginY = centerY - (project.plotBreadth * currentPixelsPerMeter) / 2f
        val wx = (screenPos.x - plotOriginX) / currentPixelsPerMeter
        val wy = (screenPos.y - plotOriginY) / currentPixelsPerMeter
        return Offset(wx, wy)
    }

    fun applyGridSnap(value: Float): Float {
        if (!snapToGrid) return value
        val step = if (project.unit == MeasurementUnit.FEET) 0.1524f else 0.1f // ~6 inches or 10cm
        return (round(value / step) * step)
    }

    // Helper to find handle at screen position
    fun hitTestHandle(screenPos: Offset, elemX: Float, elemY: Float, elemW: Float, elemH: Float, isFurniture: Boolean): ResizeHandle {
        val handleRadius = 24f // large touch target (48dp diameter)

        val topLeft = worldToScreen(elemX, elemY)
        val topRight = worldToScreen(elemX + elemW, elemY)
        val bottomLeft = worldToScreen(elemX, elemY + elemH)
        val bottomRight = worldToScreen(elemX + elemW, elemY + elemH)

        val topEdge = worldToScreen(elemX + elemW / 2f, elemY)
        val bottomEdge = worldToScreen(elemX + elemW / 2f, elemY + elemH)
        val leftEdge = worldToScreen(elemX, elemY + elemH / 2f)
        val rightEdge = worldToScreen(elemX + elemW, elemY + elemH / 2f)

        if (isFurniture) {
            val rotHandle = worldToScreen(elemX + elemW / 2f, elemY - 0.6f)
            if ((screenPos - rotHandle).getDistance() <= handleRadius) return ResizeHandle.ROTATE
        }

        if ((screenPos - topLeft).getDistance() <= handleRadius) return ResizeHandle.TOP_LEFT
        if ((screenPos - topRight).getDistance() <= handleRadius) return ResizeHandle.TOP_RIGHT
        if ((screenPos - bottomLeft).getDistance() <= handleRadius) return ResizeHandle.BOTTOM_LEFT
        if ((screenPos - bottomRight).getDistance() <= handleRadius) return ResizeHandle.BOTTOM_RIGHT

        if ((screenPos - topEdge).getDistance() <= handleRadius) return ResizeHandle.TOP_EDGE
        if ((screenPos - bottomEdge).getDistance() <= handleRadius) return ResizeHandle.BOTTOM_EDGE
        if ((screenPos - leftEdge).getDistance() <= handleRadius) return ResizeHandle.LEFT_EDGE
        if ((screenPos - rightEdge).getDistance() <= handleRadius) return ResizeHandle.RIGHT_EDGE

        return ResizeHandle.NONE
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // Multi-touch gestures (zoom & pan with two fingers)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        onTransform(zoom, pan)
                    }
                }
                // Tap detection (selection)
                .pointerInput(activeFloor, selectedElement) {
                    detectTapGestures { tapScreenPos ->
                        val worldTap = screenToWorld(tapScreenPos)

                        // 1. Check Furniture first (top layer)
                        val hitFurniture = activeFloor.furniture.findLast { it.containsPoint(worldTap.x, worldTap.y) }
                        if (hitFurniture != null) {
                            onSelectElement(SelectedElement.Furniture(hitFurniture.id))
                            return@detectTapGestures
                        }

                        // 2. Check Rooms
                        val hitRoom = activeFloor.rooms.findLast { it.containsPoint(worldTap.x, worldTap.y) }
                        if (hitRoom != null) {
                            onSelectElement(SelectedElement.Room(hitRoom.id))
                            return@detectTapGestures
                        }

                        // 3. Clicked empty canvas: clear selection
                        onSelectElement(null)
                    }
                }
                // Single-touch Drag gestures (dragging element or resizing via handles)
                .pointerInput(activeFloor, selectedElement, snapToGrid) {
                    detectDragGestures(
                        onDragStart = { startScreenPos ->
                            val worldStart = screenToWorld(startScreenPos)
                            dragStartWorldPos = worldStart

                            when (selectedElement) {
                                is SelectedElement.Room -> {
                                    val room = activeFloor.rooms.find { it.id == selectedElement.id }
                                    if (room != null) {
                                        val handle = hitTestHandle(startScreenPos, room.x, room.y, room.length, room.breadth, false)
                                        if (handle != ResizeHandle.NONE) {
                                            activeHandle = handle
                                            initialElementBounds = Pair(Offset(room.x, room.y), Size(room.length, room.breadth))
                                        } else if (room.containsPoint(worldStart.x, worldStart.y)) {
                                            isMovingElement = true
                                            initialElementBounds = Pair(Offset(room.x, room.y), Size(room.length, room.breadth))
                                        }
                                    }
                                }
                                is SelectedElement.Furniture -> {
                                    val furn = activeFloor.furniture.find { it.id == selectedElement.id }
                                    if (furn != null) {
                                        val handle = hitTestHandle(startScreenPos, furn.x, furn.y, furn.length, furn.breadth, true)
                                        if (handle != ResizeHandle.NONE) {
                                            activeHandle = handle
                                            initialElementBounds = Pair(Offset(furn.x, furn.y), Size(furn.length, furn.breadth))
                                            initialRotation = furn.rotationDegrees
                                        } else if (furn.containsPoint(worldStart.x, worldStart.y)) {
                                            isMovingElement = true
                                            initialElementBounds = Pair(Offset(furn.x, furn.y), Size(furn.length, furn.breadth))
                                        }
                                    }
                                }
                                null -> {
                                    activeHandle = ResizeHandle.NONE
                                    isMovingElement = false
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val curWorld = screenToWorld(change.position)
                            val deltaX = curWorld.x - dragStartWorldPos.x
                            val deltaY = curWorld.y - dragStartWorldPos.y

                            val initBounds = initialElementBounds ?: return@detectDragGestures
                            val initPos = initBounds.first
                            val initSize = initBounds.second

                            when (selectedElement) {
                                is SelectedElement.Room -> {
                                    val room = activeFloor.rooms.find { it.id == selectedElement.id } ?: return@detectDragGestures
                                    if (isMovingElement) {
                                        val newX = applyGridSnap(initPos.x + deltaX)
                                        val newY = applyGridSnap(initPos.y + deltaY)
                                        onUpdateRoom(room.copy(x = newX.coerceAtLeast(0f), y = newY.coerceAtLeast(0f)))
                                    } else if (activeHandle != ResizeHandle.NONE) {
                                        var newX = room.x
                                        var newY = room.y
                                        var newL = room.length
                                        var newB = room.breadth
                                        val minSize = 0.8f

                                        when (activeHandle) {
                                            ResizeHandle.RIGHT_EDGE, ResizeHandle.BOTTOM_RIGHT -> {
                                                newL = applyGridSnap((initSize.width + deltaX).coerceAtLeast(minSize))
                                            }
                                            ResizeHandle.LEFT_EDGE, ResizeHandle.BOTTOM_LEFT -> {
                                                val proposedW = (initSize.width - deltaX).coerceAtLeast(minSize)
                                                newX = applyGridSnap(initPos.x + (initSize.width - proposedW))
                                                newL = proposedW
                                            }
                                            else -> {}
                                        }

                                        when (activeHandle) {
                                            ResizeHandle.BOTTOM_EDGE, ResizeHandle.BOTTOM_RIGHT, ResizeHandle.BOTTOM_LEFT -> {
                                                newB = applyGridSnap((initSize.height + deltaY).coerceAtLeast(minSize))
                                            }
                                            ResizeHandle.TOP_EDGE, ResizeHandle.TOP_LEFT, ResizeHandle.TOP_RIGHT -> {
                                                val proposedH = (initSize.height - deltaY).coerceAtLeast(minSize)
                                                newY = applyGridSnap(initPos.y + (initSize.height - proposedH))
                                                newB = proposedH
                                            }
                                            else -> {}
                                        }

                                        onUpdateRoom(room.copy(x = newX, y = newY, length = newL, breadth = newB))
                                    }
                                }
                                is SelectedElement.Furniture -> {
                                    val furn = activeFloor.furniture.find { it.id == selectedElement.id } ?: return@detectDragGestures
                                    if (isMovingElement) {
                                        val newX = applyGridSnap(initPos.x + deltaX)
                                        val newY = applyGridSnap(initPos.y + deltaY)
                                        onUpdateFurniture(furn.copy(x = newX, y = newY))
                                    } else if (activeHandle == ResizeHandle.ROTATE) {
                                        val centerWorldX = furn.x + furn.length / 2f
                                        val centerWorldY = furn.y + furn.breadth / 2f
                                        val angleRad = atan2(curWorld.y - centerWorldY, curWorld.x - centerWorldX)
                                        val angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat() + 90f
                                        val snappedDeg = if (snapToGrid) (round(angleDeg / 15f) * 15f) else angleDeg
                                        onUpdateFurniture(furn.copy(rotationDegrees = (snappedDeg + 360f) % 360f))
                                    } else if (activeHandle != ResizeHandle.NONE) {
                                        var newL = furn.length
                                        var newB = furn.breadth
                                        val minSize = 0.3f
                                        when (activeHandle) {
                                            ResizeHandle.RIGHT_EDGE, ResizeHandle.BOTTOM_RIGHT -> {
                                                newL = applyGridSnap((initSize.width + deltaX).coerceAtLeast(minSize))
                                            }
                                            ResizeHandle.BOTTOM_EDGE -> {
                                                newB = applyGridSnap((initSize.height + deltaY).coerceAtLeast(minSize))
                                            }
                                            else -> {}
                                        }
                                        onUpdateFurniture(furn.copy(length = newL, breadth = newB))
                                    }
                                }
                                null -> {}
                            }
                        },
                        onDragEnd = {
                            activeHandle = ResizeHandle.NONE
                            isMovingElement = false
                            initialElementBounds = null
                        },
                        onDragCancel = {
                            activeHandle = ResizeHandle.NONE
                            isMovingElement = false
                            initialElementBounds = null
                        }
                    )
                }
        ) {
            canvasSize = size

            // 1. Draw Paper Background
            drawRect(Color(0xFFFBFBFA), Offset.Zero, size)

            // 2. Draw Measurement Grid
            drawMeasurementGrid(
                this,
                project,
                currentPixelsPerMeter,
                worldToScreen(0f, 0f),
                worldToScreen(project.plotLength, project.plotBreadth),
                size
            )

            // 3. Draw Plot Boundary
            val plotOrigin = worldToScreen(0f, 0f)
            val plotW = project.plotLength * currentPixelsPerMeter
            val plotH = project.plotBreadth * currentPixelsPerMeter
            drawPlotBoundary(this, plotOrigin, plotW, plotH, project)

            // 4. Draw Lower Floor Reference (Ghost Overlay)
            if (showLowerFloorOverlay && lowerFloor != null && activeFloor.level > 0) {
                drawLowerFloorGhost(this, lowerFloor, ::worldToScreen, currentPixelsPerMeter)
            }

            // 5. Draw Active Floor Rooms
            for (room in activeFloor.rooms) {
                val isSelected = (selectedElement as? SelectedElement.Room)?.id == room.id
                drawRoomItem(this, room, ::worldToScreen, currentPixelsPerMeter, isSelected, project.unit, project)
            }

            // 6. Draw Active Floor Furniture
            for (item in activeFloor.furniture) {
                val isSelected = (selectedElement as? SelectedElement.Furniture)?.id == item.id
                val pos = worldToScreen(item.x, item.y)
                val w = item.length * currentPixelsPerMeter
                val h = item.breadth * currentPixelsPerMeter

                ArchitecturalVectorRenderer.drawFurniture(
                    drawScope = this,
                    item = item,
                    screenX = pos.x,
                    screenY = pos.y,
                    screenWidth = w,
                    screenHeight = h,
                    isSelected = isSelected
                )

                if (isSelected) {
                    drawSelectionHandles(this, pos.x, pos.y, w, h, isFurniture = true)
                }
            }

            // 7. Draw Room Selection Handles on top
            val selectedRoom = (selectedElement as? SelectedElement.Room)?.let { sel ->
                activeFloor.rooms.find { it.id == sel.id }
            }
            if (selectedRoom != null) {
                val rPos = worldToScreen(selectedRoom.x, selectedRoom.y)
                val rW = selectedRoom.length * currentPixelsPerMeter
                val rH = selectedRoom.breadth * currentPixelsPerMeter
                drawSelectionHandles(this, rPos.x, rPos.y, rW, rH, isFurniture = false)
            }
        }
    }
}

// ---------------- Helper Drawing Functions ----------------

private fun drawMeasurementGrid(
    scope: DrawScope,
    project: Project,
    pixelsPerMeter: Float,
    plotTopLeft: Offset,
    plotBottomRight: Offset,
    canvasSize: Size
) {
    val majorStep = 1.0f // 1 meter or ~3 feet
    val minorStep = 0.2f // 20cm

    val minorLineColor = Color(0xFFF1F5F9)
    val majorLineColor = Color(0xFFE2E8F0)

    val minX = 0f
    val maxX = canvasSize.width
    val minY = 0f
    val maxY = canvasSize.height

    // Calculate grid alignment relative to plot origin
    val originX = plotTopLeft.x
    val originY = plotTopLeft.y

    // Minor lines
    val minorPx = minorStep * pixelsPerMeter
    if (minorPx >= 8f) {
        var x = originX % minorPx
        while (x < maxX) {
            scope.drawLine(minorLineColor, Offset(x, minY), Offset(x, maxY), strokeWidth = 0.8f)
            x += minorPx
        }
        var y = originY % minorPx
        while (y < maxY) {
            scope.drawLine(minorLineColor, Offset(minX, y), Offset(maxX, y), strokeWidth = 0.8f)
            y += minorPx
        }
    }

    // Major lines
    val majorPx = majorStep * pixelsPerMeter
    if (majorPx >= 15f) {
        var x = originX % majorPx
        while (x < maxX) {
            scope.drawLine(majorLineColor, Offset(x, minY), Offset(x, maxY), strokeWidth = 1.2f)
            x += majorPx
        }
        var y = originY % majorPx
        while (y < maxY) {
            scope.drawLine(majorLineColor, Offset(minX, y), Offset(maxX, y), strokeWidth = 1.2f)
            y += majorPx
        }
    }
}

private fun drawPlotBoundary(
    scope: DrawScope,
    plotOrigin: Offset,
    plotW: Float,
    plotH: Float,
    project: Project
) {
    val plotBorderColor = Color(0xFF64748B)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    val textPaint = Paint().apply {
        color = android.graphics.Color.rgb(100, 116, 139)
        textSize = 28f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    val badgePaint = Paint().apply {
        color = android.graphics.Color.rgb(71, 85, 105)
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    val pxPerMeter = if (project.plotLength > 0.01f) plotW / project.plotLength else 50f

    if (project.isIrregularPlot) {
        val wRear = project.plotWidth2
        val wFront = project.plotWidth1
        val lLeft = project.plotLength1
        val lRight = project.plotLength2

        val pTopLeft = Offset(plotOrigin.x, plotOrigin.y)
        val pTopRight = Offset(plotOrigin.x + wRear * pxPerMeter, plotOrigin.y)
        val pBottomRight = Offset(plotOrigin.x + wFront * pxPerMeter, plotOrigin.y + lRight * pxPerMeter)
        val pBottomLeft = Offset(plotOrigin.x, plotOrigin.y + lLeft * pxPerMeter)

        val polyPath = Path().apply {
            moveTo(pTopLeft.x, pTopLeft.y)
            lineTo(pTopRight.x, pTopRight.y)
            lineTo(pBottomRight.x, pBottomRight.y)
            lineTo(pBottomLeft.x, pBottomLeft.y)
            close()
        }

        // Faint fill for irregular polygon
        scope.drawPath(
            path = polyPath,
            color = Color(0x0C2563EB)
        )

        // Bounding box outline in light dashed grey
        scope.drawRect(
            color = Color(0xFFCBD5E1),
            topLeft = plotOrigin,
            size = Size(plotW, plotH),
            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
        )

        // Irregular perimeter stroke
        scope.drawPath(
            path = polyPath,
            color = plotBorderColor,
            style = Stroke(width = 2.2f, pathEffect = dashEffect)
        )

        // Corner crosshairs on the 4 corners
        val crossSize = 10f
        listOf(pTopLeft, pTopRight, pBottomRight, pBottomLeft).forEach { pt ->
            scope.drawLine(plotBorderColor, Offset(pt.x - crossSize, pt.y), Offset(pt.x + crossSize, pt.y), strokeWidth = 2f)
            scope.drawLine(plotBorderColor, Offset(pt.x, pt.y - crossSize), Offset(pt.x, pt.y + crossSize), strokeWidth = 2f)
        }

        // Top measurement badge (Rear)
        scope.drawContext.canvas.nativeCanvas.drawText(
            "Rear: ${project.unit.format(wRear)}",
            (pTopLeft.x + pTopRight.x) / 2f,
            pTopLeft.y - 12f,
            textPaint
        )

        // Bottom measurement badge (Front)
        scope.drawContext.canvas.nativeCanvas.drawText(
            "Front: ${project.unit.format(wFront)}",
            (pBottomLeft.x + pBottomRight.x) / 2f,
            maxOf(pBottomLeft.y, pBottomRight.y) + 30f,
            textPaint
        )

        // Left measurement badge
        scope.drawContext.canvas.nativeCanvas.save()
        scope.drawContext.canvas.nativeCanvas.rotate(-90f, pTopLeft.x - 12f, (pTopLeft.y + pBottomLeft.y) / 2f)
        scope.drawContext.canvas.nativeCanvas.drawText(
            "Left: ${project.unit.format(lLeft)}",
            pTopLeft.x - 12f,
            (pTopLeft.y + pBottomLeft.y) / 2f,
            textPaint
        )
        scope.drawContext.canvas.nativeCanvas.restore()

        // Right measurement badge
        val rightMidX = (pTopRight.x + pBottomRight.x) / 2f
        val rightMidY = (pTopRight.y + pBottomRight.y) / 2f
        scope.drawContext.canvas.nativeCanvas.save()
        scope.drawContext.canvas.nativeCanvas.rotate(90f, rightMidX + 16f, rightMidY)
        scope.drawContext.canvas.nativeCanvas.drawText(
            "Right: ${project.unit.format(lRight)}",
            rightMidX + 16f,
            rightMidY,
            textPaint
        )
        scope.drawContext.canvas.nativeCanvas.restore()
    } else {
        // Standard rectangular plot
        scope.drawRect(
            color = plotBorderColor,
            topLeft = plotOrigin,
            size = Size(plotW, plotH),
            style = Stroke(width = 2f, pathEffect = dashEffect)
        )

        // Corner crosshairs
        val crossSize = 10f
        scope.drawLine(plotBorderColor, Offset(plotOrigin.x - crossSize, plotOrigin.y), Offset(plotOrigin.x + crossSize, plotOrigin.y), strokeWidth = 2f)
        scope.drawLine(plotBorderColor, Offset(plotOrigin.x, plotOrigin.y - crossSize), Offset(plotOrigin.x, plotOrigin.y + crossSize), strokeWidth = 2f)

        scope.drawLine(plotBorderColor, Offset(plotOrigin.x + plotW - crossSize, plotOrigin.y), Offset(plotOrigin.x + plotW + crossSize, plotOrigin.y), strokeWidth = 2f)
        scope.drawLine(plotBorderColor, Offset(plotOrigin.x + plotW, plotOrigin.y - crossSize), Offset(plotOrigin.x + plotW, plotOrigin.y + crossSize), strokeWidth = 2f)

        // Top measurement badge
        val topText = "Plot Width: ${project.unit.format(project.plotLength)}"
        scope.drawContext.canvas.nativeCanvas.drawText(
            topText,
            plotOrigin.x + plotW / 2f,
            plotOrigin.y - 12f,
            textPaint
        )

        val sideText = "Plot Depth: ${project.unit.format(project.plotBreadth)}"
        scope.drawContext.canvas.nativeCanvas.save()
        scope.drawContext.canvas.nativeCanvas.rotate(-90f, plotOrigin.x - 12f, plotOrigin.y + plotH / 2f)
        scope.drawContext.canvas.nativeCanvas.drawText(
            sideText,
            plotOrigin.x - 12f,
            plotOrigin.y + plotH / 2f,
            textPaint
        )
        scope.drawContext.canvas.nativeCanvas.restore()
    }
}

private fun drawLowerFloorGhost(
    scope: DrawScope,
    lowerFloor: Floor,
    worldToScreen: (Float, Float) -> Offset,
    pixelsPerMeter: Float
) {
    val ghostColor = Color(0xFFCBD5E1)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

    for (room in lowerFloor.rooms) {
        val pos = worldToScreen(room.x, room.y)
        val w = room.length * pixelsPerMeter
        val h = room.breadth * pixelsPerMeter

        scope.drawRect(
            color = ghostColor,
            topLeft = pos,
            size = Size(w, h),
            style = Stroke(width = 1.5f, pathEffect = dashEffect)
        )
    }
}

private fun drawRoomItem(
    scope: DrawScope,
    room: RoomItem,
    worldToScreen: (Float, Float) -> Offset,
    pixelsPerMeter: Float,
    isSelected: Boolean,
    unit: MeasurementUnit,
    project: Project
) {
    val pos = worldToScreen(room.x, room.y)
    val w = room.length * pixelsPerMeter
    val h = room.breadth * pixelsPerMeter

    // Background architectural tint
    val fillColor = try {
        Color(android.graphics.Color.parseColor(room.colorHex ?: room.type.colorHex))
    } catch (_: Exception) {
        Color(0xFFF8FAFC)
    }
    scope.drawRect(fillColor, pos, Size(w, h))

    // Wall Outline (Thick 3dp solid architectural slate)
    val isOutOfBounds = room.isOutOfBounds(project.plotLength, project.plotBreadth)
    val wallColor = if (isOutOfBounds) Color(0xFFEF4444) else if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A)
    scope.drawRect(wallColor, pos, Size(w, h), style = Stroke(width = if (isSelected) 3.5f else 2.5f))

    // Doors
    for (door in room.doors) {
        val dw = door.widthMeters * pixelsPerMeter
        val doff = door.offsetMeters * pixelsPerMeter
        val doorColor = Color(0xFF2563EB)

        when (door.wall) {
            WallEdge.BOTTOM -> {
                val dx = pos.x + doff
                val dy = pos.y + h
                // Erase wall segment with white
                scope.drawLine(Color.White, Offset(dx, dy), Offset(dx + dw, dy), strokeWidth = 3.5f)
                // Door swing arc
                scope.drawArc(
                    doorColor,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(dx, dy - dw),
                    size = Size(dw * 2f, dw * 2f),
                    style = Stroke(1.5f)
                )
            }
            WallEdge.TOP -> {
                val dx = pos.x + doff
                val dy = pos.y
                scope.drawLine(Color.White, Offset(dx, dy), Offset(dx + dw, dy), strokeWidth = 3.5f)
                scope.drawArc(
                    doorColor,
                    startAngle = 90f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(dx, dy - dw),
                    size = Size(dw * 2f, dw * 2f),
                    style = Stroke(1.5f)
                )
            }
            WallEdge.LEFT -> {
                val dx = pos.x
                val dy = pos.y + doff
                scope.drawLine(Color.White, Offset(dx, dy), Offset(dx, dy + dw), strokeWidth = 3.5f)
                scope.drawArc(
                    doorColor,
                    startAngle = 270f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(dx - dw, dy),
                    size = Size(dw * 2f, dw * 2f),
                    style = Stroke(1.5f)
                )
            }
            WallEdge.RIGHT -> {
                val dx = pos.x + w
                val dy = pos.y + doff
                scope.drawLine(Color.White, Offset(dx, dy), Offset(dx, dy + dw), strokeWidth = 3.5f)
                scope.drawArc(
                    doorColor,
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = true,
                    topLeft = Offset(dx - dw, dy),
                    size = Size(dw * 2f, dw * 2f),
                    style = Stroke(1.5f)
                )
            }
        }
    }

    // Windows
    for (win in room.windows) {
        val ww = win.widthMeters * pixelsPerMeter
        val woff = win.offsetMeters * pixelsPerMeter
        val winColor = Color(0xFF0284C7)

        when (win.wall) {
            WallEdge.TOP -> {
                scope.drawLine(Color.White, Offset(pos.x + woff, pos.y), Offset(pos.x + woff + ww, pos.y), strokeWidth = 4f)
                scope.drawLine(winColor, Offset(pos.x + woff, pos.y - 2f), Offset(pos.x + woff + ww, pos.y - 2f), strokeWidth = 2f)
                scope.drawLine(winColor, Offset(pos.x + woff, pos.y + 2f), Offset(pos.x + woff + ww, pos.y + 2f), strokeWidth = 2f)
            }
            WallEdge.BOTTOM -> {
                scope.drawLine(Color.White, Offset(pos.x + woff, pos.y + h), Offset(pos.x + woff + ww, pos.y + h), strokeWidth = 4f)
                scope.drawLine(winColor, Offset(pos.x + woff, pos.y + h - 2f), Offset(pos.x + woff + ww, pos.y + h - 2f), strokeWidth = 2f)
                scope.drawLine(winColor, Offset(pos.x + woff, pos.y + h + 2f), Offset(pos.x + woff + ww, pos.y + h + 2f), strokeWidth = 2f)
            }
            WallEdge.LEFT -> {
                scope.drawLine(Color.White, Offset(pos.x, pos.y + woff), Offset(pos.x, pos.y + woff + ww), strokeWidth = 4f)
                scope.drawLine(winColor, Offset(pos.x - 2f, pos.y + woff), Offset(pos.x - 2f, pos.y + woff + ww), strokeWidth = 2f)
                scope.drawLine(winColor, Offset(pos.x + 2f, pos.y + woff), Offset(pos.x + 2f, pos.y + woff + ww), strokeWidth = 2f)
            }
            WallEdge.RIGHT -> {
                scope.drawLine(Color.White, Offset(pos.x + w, pos.y + woff), Offset(pos.x + w, pos.y + woff + ww), strokeWidth = 4f)
                scope.drawLine(winColor, Offset(pos.x + w - 2f, pos.y + woff), Offset(pos.x + w - 2f, pos.y + woff + ww), strokeWidth = 2f)
                scope.drawLine(winColor, Offset(pos.x + w + 2f, pos.y + woff), Offset(pos.x + w + 2f, pos.y + woff + ww), strokeWidth = 2f)
            }
        }
    }

    // Room Label & Dimensions
    if (w > 60f && h > 40f) {
        val cx = pos.x + w / 2f
        val cy = pos.y + h / 2f

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(15, 23, 42)
            textSize = (minOf(w / 8f, h / 6f)).coerceIn(24f, 34f)
            isAntiAlias = true
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint().apply {
            color = android.graphics.Color.rgb(71, 85, 105)
            textSize = (minOf(w / 10f, h / 8f)).coerceIn(18f, 26f)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        scope.drawContext.canvas.nativeCanvas.drawText(room.name, cx, cy - 8f, titlePaint)
        val dimText = "${unit.format(room.length)} × ${unit.format(room.breadth)} (${unit.formatArea(room.areaSqMeters)})"
        scope.drawContext.canvas.nativeCanvas.drawText(dimText, cx, cy + 22f, subPaint)
    }
}

private fun drawSelectionHandles(
    scope: DrawScope,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    isFurniture: Boolean
) {
    val handleColor = Color(0xFF2563EB)
    val handleStrokeColor = Color.White
    val handleRadius = 7f // visually 14px circle, touch target is 48px+

    // Selection border
    scope.drawRect(
        color = handleColor,
        topLeft = Offset(x - 2f, y - 2f),
        size = Size(w + 4f, h + 4f),
        style = Stroke(width = 2f)
    )

    fun drawHandle(cx: Float, cy: Float) {
        scope.drawCircle(handleStrokeColor, radius = handleRadius + 2f, center = Offset(cx, cy))
        scope.drawCircle(handleColor, radius = handleRadius, center = Offset(cx, cy))
    }

    // 4 Corner handles
    drawHandle(x, y) // TL
    drawHandle(x + w, y) // TR
    drawHandle(x, y + h) // BL
    drawHandle(x + w, y + h) // BR

    // 4 Edge handles
    drawHandle(x + w / 2f, y) // Top
    drawHandle(x + w / 2f, y + h) // Bottom
    drawHandle(x, y + h / 2f) // Left
    drawHandle(x + w, y + h / 2f) // Right

    // Rotation handle for furniture (top stem with knob)
    if (isFurniture) {
        val stemTop = y - 28f
        scope.drawLine(handleColor, Offset(x + w / 2f, y), Offset(x + w / 2f, stemTop), strokeWidth = 2f)
        scope.drawCircle(handleStrokeColor, radius = handleRadius + 2f, center = Offset(x + w / 2f, stemTop))
        scope.drawCircle(Color(0xFFD97706), radius = handleRadius, center = Offset(x + w / 2f, stemTop))
    }
}
