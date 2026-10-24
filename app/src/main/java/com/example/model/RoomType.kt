package com.example.model

enum class RoomType(
    val label: String,
    val defaultLengthMeters: Float,
    val defaultBreadthMeters: Float,
    val colorHex: String
) {
    LIVING_ROOM("Living Room", 5.0f, 4.0f, "#F8FAFC"),
    BEDROOM("Bedroom", 4.0f, 3.5f, "#EFF6FF"),
    KITCHEN("Kitchen", 3.5f, 3.0f, "#FFFBEB"),
    BATHROOM("Bathroom", 2.4f, 2.0f, "#F0FDF4"),
    DINING_ROOM("Dining Room", 4.0f, 3.2f, "#FFF7ED"),
    STUDY("Study Room", 3.2f, 2.8f, "#FAF5FF"),
    BALCONY("Balcony", 3.0f, 1.5f, "#F0FDFA"),
    CORRIDOR("Corridor", 4.0f, 1.2f, "#F1F5F9"),
    CUSTOM("Custom Room", 4.0f, 3.0f, "#F8FAFC")
}
