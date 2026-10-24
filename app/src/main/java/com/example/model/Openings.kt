package com.example.model

enum class WallEdge {
    TOP, BOTTOM, LEFT, RIGHT
}

data class DoorOpening(
    val id: String,
    val wall: WallEdge,
    val offsetMeters: Float = 0.5f,
    val widthMeters: Float = 0.9f,
    val swingsInward: Boolean = true
)

data class WindowOpening(
    val id: String,
    val wall: WallEdge,
    val offsetMeters: Float = 0.8f,
    val widthMeters: Float = 1.2f
)
