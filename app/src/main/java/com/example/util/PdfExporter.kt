package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private const val PAGE_WIDTH = 842 // A4 Landscape points (approx 11.69 inches @ 72 dpi)
    private const val PAGE_HEIGHT = 595 // A4 Landscape height (approx 8.27 inches)

    fun exportProjectToPdf(context: Context, project: Project): File {
        val pdfDocument = PdfDocument()

        val bgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subheaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(71, 85, 105)
            textSize = 12f
        }
        val disclaimerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val wallFillPaint = Paint().apply {
            color = Color.rgb(248, 250, 252)
            style = Paint.Style.FILL
        }
        val plotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        val furniturePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val furnitureFillPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val dimTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }

        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date())

        for ((index, floor) in project.floors.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

            // Header Banner
            canvas.drawText("HEARTHPLAN — ${project.name.uppercase()}", 40f, 45f, headerPaint)
            val plotDimStr = if (project.isIrregularPlot) {
                "Plot: Front ${project.unit.format(project.plotWidth1)}, Rear ${project.unit.format(project.plotWidth2)}, Left ${project.unit.format(project.plotLength1)}, Right ${project.unit.format(project.plotLength2)}"
            } else {
                "Plot: ${project.unit.format(project.plotLength)} × ${project.unit.format(project.plotBreadth)}"
            }
            canvas.drawText(
                "${floor.name} (Level ${floor.level + 1}) | $plotDimStr | Date: $dateStr",
                40f,
                65f,
                subheaderPaint
            )

            // Divider line
            canvas.drawLine(40f, 75f, (PAGE_WIDTH - 40).toFloat(), 75f, Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            })

            // Calculate drawing area and scale factor
            val drawingLeft = 40f
            val drawingTop = 90f
            val drawingWidth = (PAGE_WIDTH - 80).toFloat()
            val drawingHeight = (PAGE_HEIGHT - 160).toFloat()

            val maxDimensionX = maxOf(project.plotLength, floor.length, 1f)
            val maxDimensionY = maxOf(project.plotBreadth, floor.breadth, 1f)
            val scale = minOf(drawingWidth / maxDimensionX, drawingHeight / maxDimensionY) * 0.9f

            val originX = drawingLeft + (drawingWidth - maxDimensionX * scale) / 2f
            val originY = drawingTop + (drawingHeight - maxDimensionY * scale) / 2f

            // Draw Plot Boundary
            if (project.isIrregularPlot) {
                val poly = android.graphics.Path().apply {
                    val pTopLeft = Pair(originX, originY)
                    val pTopRight = Pair(originX + project.plotWidth2 * scale, originY)
                    val pBottomRight = Pair(originX + project.plotWidth1 * scale, originY + project.plotLength2 * scale)
                    val pBottomLeft = Pair(originX, originY + project.plotLength1 * scale)
                    moveTo(pTopLeft.first, pTopLeft.second)
                    lineTo(pTopRight.first, pTopRight.second)
                    lineTo(pBottomRight.first, pBottomRight.second)
                    lineTo(pBottomLeft.first, pBottomLeft.second)
                    close()
                }
                canvas.drawPath(poly, plotBorderPaint)
            } else {
                val plotRight = originX + project.plotLength * scale
                val plotBottom = originY + project.plotBreadth * scale
                canvas.drawRect(originX, originY, plotRight, plotBottom, plotBorderPaint)
            }

            // Draw Rooms
            for (room in floor.rooms) {
                val rx = originX + room.x * scale
                val ry = originY + room.y * scale
                val rw = room.length * scale
                val rh = room.breadth * scale

                // Room Fill & Outline
                canvas.drawRect(rx, ry, rx + rw, ry + rh, wallFillPaint)
                canvas.drawRect(rx, ry, rx + rw, ry + rh, wallPaint)

                // Room Labels
                if (rw > 50f && rh > 35f) {
                    val cx = rx + rw / 2f
                    val cy = ry + rh / 2f
                    canvas.drawText(room.name, cx, cy - 4f, textPaint)
                    val dimStr = "${project.unit.format(room.length)} × ${project.unit.format(room.breadth)} (${project.unit.formatArea(room.areaSqMeters)})"
                    canvas.drawText(dimStr, cx, cy + 12f, dimTextPaint)
                }

                // Draw Doors
                for (door in room.doors) {
                    val dw = door.widthMeters * scale
                    val doff = door.offsetMeters * scale
                    val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(37, 99, 235)
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                    }
                    when (door.wall) {
                        WallEdge.BOTTOM -> {
                            val dx = rx + doff
                            val dy = ry + rh
                            canvas.drawArc(dx, dy - dw, dx + dw * 2f, dy + dw, 180f, 90f, true, arcPaint)
                        }
                        WallEdge.TOP -> {
                            val dx = rx + doff
                            val dy = ry
                            canvas.drawArc(dx, dy - dw, dx + dw * 2f, dy + dw, 90f, 90f, true, arcPaint)
                        }
                        WallEdge.LEFT -> {
                            val dx = rx
                            val dy = ry + doff
                            canvas.drawArc(dx - dw, dy, dx + dw, dy + dw * 2f, 270f, 90f, true, arcPaint)
                        }
                        WallEdge.RIGHT -> {
                            val dx = rx + rw
                            val dy = ry + doff
                            canvas.drawArc(dx - dw, dy, dx + dw, dy + dw * 2f, 180f, 90f, true, arcPaint)
                        }
                    }
                }

                // Draw Windows
                for (win in room.windows) {
                    val ww = win.widthMeters * scale
                    val woff = win.offsetMeters * scale
                    val winPaint = Paint().apply {
                        color = Color.rgb(14, 165, 233)
                        style = Paint.Style.STROKE
                        strokeWidth = 2.5f
                    }
                    when (win.wall) {
                        WallEdge.TOP -> canvas.drawLine(rx + woff, ry, rx + woff + ww, ry, winPaint)
                        WallEdge.BOTTOM -> canvas.drawLine(rx + woff, ry + rh, rx + woff + ww, ry + rh, winPaint)
                        WallEdge.LEFT -> canvas.drawLine(rx, ry + woff, rx, ry + woff + ww, winPaint)
                        WallEdge.RIGHT -> canvas.drawLine(rx + rw, ry + woff, rx + rw, ry + woff + ww, winPaint)
                    }
                }
            }

            // Draw Furniture
            for (item in floor.furniture) {
                val fx = originX + item.x * scale
                val fy = originY + item.y * scale
                val fw = item.length * scale
                val fh = item.breadth * scale

                canvas.save()
                canvas.rotate(item.rotationDegrees, fx + fw / 2f, fy + fh / 2f)
                canvas.drawRect(fx, fy, fx + fw, fy + fh, furnitureFillPaint)
                canvas.drawRect(fx, fy, fx + fw, fy + fh, furniturePaint)

                // Simple top-down symbol indications
                if (fw > 30f && fh > 20f) {
                    val label = item.name.take(16)
                    val fnLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(71, 85, 105)
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(label, fx + fw / 2f, fy + fh / 2f + 3f, fnLabelPaint)
                }
                canvas.restore()
            }

            // Footer Section
            val footerY = (PAGE_HEIGHT - 35).toFloat()
            canvas.drawLine(40f, footerY - 15f, (PAGE_WIDTH - 40).toFloat(), footerY - 15f, Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 1f
            })

            canvas.drawText(
                "DISCLAIMER: This document is a floor planning diagram for conceptual visualization and spatial layout, and is not a certified structural, architectural, or construction engineering drawing.",
                40f,
                footerY,
                disclaimerPaint
            )

            val pageLabel = "Page ${index + 1} of ${project.floors.size}"
            canvas.drawText(pageLabel, (PAGE_WIDTH - 100).toFloat(), footerY, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 9.5f
            })

            pdfDocument.finishPage(page)
        }

        val sanitizedName = project.name.replace("[^a-zA-Z0-9]".toRegex(), "_").ifEmpty { "Plan" }
        val outputFile = File(context.cacheDir, "${sanitizedName}_Hearthplan.pdf")
        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
