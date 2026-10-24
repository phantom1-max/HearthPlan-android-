package com.example.model

data class Floor(
    val id: String,
    val name: String,
    val level: Int,
    val length: Float, // Meters
    val breadth: Float, // Meters
    val rooms: List<RoomItem> = emptyList(),
    val furniture: List<FurnitureItem> = emptyList()
) {
    val totalFloorAreaSqMeters: Float get() = length * breadth
    val totalUsableRoomAreaSqMeters: Float get() = rooms.sumOf { it.areaSqMeters.toDouble() }.toFloat()
}
