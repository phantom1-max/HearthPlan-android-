package com.example.model

data class FurnitureItem(
    val id: String,
    val templateId: String,
    val name: String,
    val category: FurnitureCategory,
    val x: Float, // Meters relative to floor origin
    val y: Float, // Meters
    val length: Float, // Width along primary axis in meters
    val breadth: Float, // Depth along secondary axis in meters
    val rotationDegrees: Float = 0f,
    val isCustom: Boolean = false,
    val customDescription: String? = null
) {
    val right: Float get() = x + length
    val bottom: Float get() = y + breadth

    fun containsPoint(px: Float, py: Float): Boolean {
        // Simple bounding box check (unrotated or rough hit test)
        val cx = x + length / 2f
        val cy = y + breadth / 2f
        val rad = -Math.toRadians(rotationDegrees.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        val dx = px - cx
        val dy = py - cy
        val rx = cos * dx - sin * dy + cx
        val ry = sin * dx + cos * dy + cy
        return rx in x..right && ry in y..bottom
    }
}
