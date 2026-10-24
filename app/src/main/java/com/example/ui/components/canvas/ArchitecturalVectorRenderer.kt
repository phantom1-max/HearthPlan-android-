package com.example.ui.components.canvas

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.model.FurnitureCategory
import com.example.model.FurnitureItem
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BlueprintBlue

object ArchitecturalVectorRenderer {

    private val StrokeColor = Color(0xFF334155)
    private val FillColor = Color(0xFFF8FAFC)
    private val AccentColor = Color(0xFF64748B)
    private val DetailColor = Color(0xFF94A3B8)

    fun drawFurniture(
        drawScope: DrawScope,
        item: FurnitureItem,
        screenX: Float,
        screenY: Float,
        screenWidth: Float,
        screenHeight: Float,
        isSelected: Boolean
    ) {
        val cx = screenX + screenWidth / 2f
        val cy = screenY + screenHeight / 2f

        drawScope.rotate(item.rotationDegrees, pivot = Offset(cx, cy)) {
            val strokeWidth = if (isSelected) 2.5f else 1.5f
            val baseStroke = Stroke(width = strokeWidth)

            when (item.category) {
                FurnitureCategory.BEDS -> drawBed(this, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.SOFAS_CHAIRS -> drawSofaOrChair(this, item.templateId, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.TABLES_DESKS -> drawTableOrDesk(this, item.templateId, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.STORAGE -> drawStorage(this, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.KITCHEN -> drawKitchenItem(this, item.templateId, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.BATHROOM -> drawBathroomItem(this, item.templateId, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.LIGHTING -> drawLighting(this, screenX, screenY, screenWidth, screenHeight, baseStroke)
                FurnitureCategory.CUSTOM -> drawCustomFurniture(this, screenX, screenY, screenWidth, screenHeight, baseStroke)
            }
        }
    }

    private fun drawBed(scope: DrawScope, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        // Mattress base
        scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f))
        scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f), style = stroke)

        // Headboard (top)
        val headboardH = h * 0.12f
        scope.drawRect(StrokeColor.copy(alpha = 0.15f), Offset(x, y), Size(w, headboardH))
        scope.drawRect(StrokeColor, Offset(x, y), Size(w, headboardH), style = stroke)

        // Pillows
        val pillowW = w * 0.38f
        val pillowH = h * 0.22f
        val pillowY = y + headboardH + h * 0.04f
        // Left pillow
        scope.drawRoundRect(Color.White, Offset(x + w * 0.08f, pillowY), Size(pillowW, pillowH), CornerRadius(4f, 4f))
        scope.drawRoundRect(DetailColor, Offset(x + w * 0.08f, pillowY), Size(pillowW, pillowH), CornerRadius(4f, 4f), style = Stroke(1.2f))
        // Right pillow (if wide enough)
        if (w > 50f) {
            scope.drawRoundRect(Color.White, Offset(x + w * 0.54f, pillowY), Size(pillowW, pillowH), CornerRadius(4f, 4f))
            scope.drawRoundRect(DetailColor, Offset(x + w * 0.54f, pillowY), Size(pillowW, pillowH), CornerRadius(4f, 4f), style = Stroke(1.2f))
        }

        // Blanket fold
        val blanketY = y + h * 0.45f
        scope.drawLine(DetailColor, Offset(x, blanketY), Offset(x + w, blanketY), strokeWidth = 1.2f)
    }

    private fun drawSofaOrChair(scope: DrawScope, templateId: String, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(6f, 6f))
        scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(6f, 6f), style = stroke)

        // Backrest (top)
        val backH = h * 0.25f
        scope.drawRect(StrokeColor.copy(alpha = 0.12f), Offset(x, y), Size(w, backH))
        scope.drawRect(StrokeColor, Offset(x, y), Size(w, backH), style = stroke)

        // Armrests
        val armW = w * 0.15f
        scope.drawRect(StrokeColor.copy(alpha = 0.08f), Offset(x, y), Size(armW, h))
        scope.drawRect(StrokeColor, Offset(x, y), Size(armW, h), style = stroke)
        scope.drawRect(StrokeColor.copy(alpha = 0.08f), Offset(x + w - armW, y), Size(armW, h))
        scope.drawRect(StrokeColor, Offset(x + w - armW, y), Size(armW, h), style = stroke)

        // Cushions
        val seatX = x + armW
        val seatW = w - 2 * armW
        val seatY = y + backH
        val seatH = h - backH

        if (w > 70f) {
            val midX = seatX + seatW / 2f
            scope.drawLine(DetailColor, Offset(midX, seatY), Offset(midX, seatY + seatH), strokeWidth = 1.2f)
        }
    }

    private fun drawTableOrDesk(scope: DrawScope, templateId: String, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        if (templateId == "round_table") {
            scope.drawOval(FillColor, Offset(x, y), Size(w, h))
            scope.drawOval(StrokeColor, Offset(x, y), Size(w, h), style = stroke)
            return
        }

        // Tabletop
        scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f))
        scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f), style = stroke)

        if (templateId.contains("desk")) {
            // Draw drawer stack on right
            val drawerW = w * 0.32f
            scope.drawRect(StrokeColor.copy(alpha = 0.1f), Offset(x + w - drawerW, y), Size(drawerW, h))
            scope.drawRect(StrokeColor, Offset(x + w - drawerW, y), Size(drawerW, h), style = stroke)
            // Chair notch
            val chairW = w * 0.45f
            val chairH = h * 0.45f
            scope.drawRoundRect(Color.White, Offset(x + (w - drawerW - chairW) / 2f, y + h * 0.25f), Size(chairW, chairH), CornerRadius(4f, 4f), style = Stroke(1.2f))
        } else if (templateId.contains("dining")) {
            // Subtle wood grain lines or center runner
            val runnerX = x + w * 0.2f
            val runnerW = w * 0.6f
            scope.drawRect(Color(0xFFE2E8F0), Offset(runnerX, y), Size(runnerW, h))
            scope.drawRect(StrokeColor, Offset(runnerX, y), Size(runnerW, h), style = Stroke(1f))
        }
    }

    private fun drawStorage(scope: DrawScope, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        scope.drawRect(FillColor, Offset(x, y), Size(w, h))
        scope.drawRect(StrokeColor, Offset(x, y), Size(w, h), style = stroke)

        // Internal divider or diagonal clothes rod representation
        val third = w / 3f
        scope.drawLine(DetailColor, Offset(x + third, y), Offset(x + third, y + h), strokeWidth = 1f)
        scope.drawLine(DetailColor, Offset(x + 2 * third, y), Offset(x + 2 * third, y + h), strokeWidth = 1f)

        // Door swing indicator or hanger cross line
        scope.drawLine(AccentColor.copy(alpha = 0.6f), Offset(x, y), Offset(x + w, y + h), strokeWidth = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    }

    private fun drawKitchenItem(scope: DrawScope, templateId: String, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        scope.drawRect(FillColor, Offset(x, y), Size(w, h))
        scope.drawRect(StrokeColor, Offset(x, y), Size(w, h), style = stroke)

        if (templateId.contains("sink")) {
            // Basin
            val basinPad = 4f
            scope.drawRoundRect(Color(0xFFE0F2FE), Offset(x + basinPad, y + basinPad), Size(w - 2 * basinPad, h - 2 * basinPad), CornerRadius(6f, 6f))
            scope.drawRoundRect(StrokeColor, Offset(x + basinPad, y + basinPad), Size(w - 2 * basinPad, h - 2 * basinPad), CornerRadius(6f, 6f), style = Stroke(1.2f))
            // Drain circle
            scope.drawCircle(StrokeColor, radius = minOf(w, h) * 0.08f, center = Offset(x + w / 2f, y + h / 2f), style = Stroke(1.5f))
        } else if (templateId.contains("cooktop")) {
            // 4 burners
            val r = minOf(w, h) * 0.16f
            scope.drawCircle(StrokeColor, radius = r, center = Offset(x + w * 0.28f, y + h * 0.3f), style = Stroke(1.5f))
            scope.drawCircle(StrokeColor, radius = r, center = Offset(x + w * 0.72f, y + h * 0.3f), style = Stroke(1.5f))
            scope.drawCircle(StrokeColor, radius = r * 0.8f, center = Offset(x + w * 0.28f, y + h * 0.7f), style = Stroke(1.5f))
            scope.drawCircle(StrokeColor, radius = r * 0.8f, center = Offset(x + w * 0.72f, y + h * 0.7f), style = Stroke(1.5f))
        } else {
            // Countertop line
            scope.drawLine(DetailColor, Offset(x, y + h * 0.15f), Offset(x + w, y + h * 0.15f), strokeWidth = 1f)
        }
    }

    private fun drawBathroomItem(scope: DrawScope, templateId: String, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        if (templateId.contains("toilet")) {
            // Cistern tank (rectangle at top)
            val tankH = h * 0.32f
            scope.drawRoundRect(FillColor, Offset(x, y), Size(w, tankH), CornerRadius(3f, 3f))
            scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, tankH), CornerRadius(3f, 3f), style = stroke)

            // Bowl (oval at bottom)
            val bowlW = w * 0.78f
            val bowlH = h * 0.65f
            val bowlX = x + (w - bowlW) / 2f
            val bowlY = y + tankH * 0.7f
            scope.drawOval(Color.White, Offset(bowlX, bowlY), Size(bowlW, bowlH))
            scope.drawOval(StrokeColor, Offset(bowlX, bowlY), Size(bowlW, bowlH), style = stroke)
        } else if (templateId.contains("bathtub")) {
            // Outer tub
            scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(10f, 10f))
            scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(10f, 10f), style = stroke)
            // Inner water cavity
            val pad = 5f
            scope.drawRoundRect(Color(0xFFE0F2FE), Offset(x + pad, y + pad), Size(w - 2 * pad, h - 2 * pad), CornerRadius(8f, 8f))
            scope.drawRoundRect(DetailColor, Offset(x + pad, y + pad), Size(w - 2 * pad, h - 2 * pad), CornerRadius(8f, 8f), style = Stroke(1.2f))
        } else {
            // Washbasin / Shower
            scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f))
            scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f), style = stroke)
            val pad = 4f
            scope.drawOval(Color(0xFFE0F2FE), Offset(x + pad, y + pad), Size(w - 2 * pad, h - 2 * pad))
            scope.drawOval(DetailColor, Offset(x + pad, y + pad), Size(w - 2 * pad, h - 2 * pad), style = Stroke(1.2f))
        }
    }

    private fun drawLighting(scope: DrawScope, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        val cx = x + w / 2f
        val cy = y + h / 2f
        val radius = minOf(w, h) / 2f
        scope.drawCircle(Color(0xFFFEF3C7), radius = radius, center = Offset(cx, cy))
        scope.drawCircle(StrokeColor, radius = radius, center = Offset(cx, cy), style = stroke)
        // Crosshair rays
        scope.drawLine(AmberAccent, Offset(cx - radius * 0.8f, cy), Offset(cx + radius * 0.8f, cy), strokeWidth = 1.2f)
        scope.drawLine(AmberAccent, Offset(cx, cy - radius * 0.8f), Offset(cx, cy + radius * 0.8f), strokeWidth = 1.2f)
    }

    private fun drawCustomFurniture(scope: DrawScope, x: Float, y: Float, w: Float, h: Float, stroke: Stroke) {
        // Architectural custom cabinet / furniture vector
        scope.drawRoundRect(FillColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f))
        scope.drawRoundRect(StrokeColor, Offset(x, y), Size(w, h), CornerRadius(4f, 4f), style = stroke)

        // Internal architectural hatching / compartments
        val margin = 4f
        scope.drawRect(BlueprintBlue.copy(alpha = 0.08f), Offset(x + margin, y + margin), Size(w - 2 * margin, h - 2 * margin))
        scope.drawRect(BlueprintBlue.copy(alpha = 0.6f), Offset(x + margin, y + margin), Size(w - 2 * margin, h - 2 * margin), style = Stroke(1f))

        // Center crosshair / technical indicator
        scope.drawLine(DetailColor, Offset(x, y), Offset(x + w, y + h), strokeWidth = 0.8f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
    }
}
