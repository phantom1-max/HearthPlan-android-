package com.example.model

import kotlin.math.abs

data class Project(
    val id: String,
    val name: String,
    val unit: MeasurementUnit = MeasurementUnit.METERS,
    val plotLength: Float, // Max bounding length / depth in meters
    val plotBreadth: Float, // Max bounding breadth / width in meters
    val plotWidth1: Float = plotBreadth, // Front width (meters)
    val plotWidth2: Float = plotBreadth, // Rear / back width (meters)
    val plotLength1: Float = plotLength, // Left length / depth (meters)
    val plotLength2: Float = plotLength, // Right length / depth (meters)
    val floors: List<Floor>,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isBuiltinTemplate: Boolean = false,
    val description: String = "",
    val author: String = "Local Designer"
) {
    val isIrregularPlot: Boolean
        get() = abs(plotWidth1 - plotWidth2) > 0.01f || abs(plotLength1 - plotLength2) > 0.01f

    val totalPlotAreaSqMeters: Float
        get() = ((plotWidth1 + plotWidth2) / 2f) * ((plotLength1 + plotLength2) / 2f)

    val totalFloorCount: Int get() = floors.size
    val totalRoomCount: Int get() = floors.sumOf { it.rooms.size }
}
