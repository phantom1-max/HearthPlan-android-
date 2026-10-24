package com.example.model

data class RoomItem(
    val id: String,
    val name: String,
    val type: RoomType,
    val x: Float, // Meters relative to floor origin (top-left)
    val y: Float, // Meters
    val length: Float, // Width along X axis in meters
    val breadth: Float, // Height along Y axis in meters
    val doors: List<DoorOpening> = emptyList(),
    val windows: List<WindowOpening> = emptyList(),
    val colorHex: String? = null
) {
    val areaSqMeters: Float get() = length * breadth
    val right: Float get() = x + length
    val bottom: Float get() = y + breadth

    fun containsPoint(px: Float, py: Float): Boolean {
        return px in x..right && py in y..bottom
    }

    fun isOutOfBounds(floorLength: Float, floorBreadth: Float): Boolean {
        return x < -0.01f || y < -0.01f || right > floorLength + 0.01f || bottom > floorBreadth + 0.01f
    }
}
