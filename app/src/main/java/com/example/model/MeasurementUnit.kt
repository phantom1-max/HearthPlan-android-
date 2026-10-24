package com.example.model

import java.util.Locale

enum class MeasurementUnit(
    val symbol: String,
    val displayName: String,
    val metersMultiplier: Float // 1 meter * multiplier = unit value
) {
    METERS("m", "Meters", 1.0f),
    FEET("ft", "Feet", 3.28084f),
    INCHES("in", "Inches", 39.3701f),
    CENTIMETERS("cm", "Centimeters", 100.0f),
    MILLIMETERS("mm", "Millimeters", 1000.0f),
    YARDS("yd", "Yards", 1.09361f);

    fun format(valueInMeters: Float, targetUnit: MeasurementUnit = this): String {
        val converted = valueInMeters * targetUnit.metersMultiplier
        val formatStr = when (targetUnit) {
            MILLIMETERS -> "%.0f %s"
            CENTIMETERS, INCHES -> "%.1f %s"
            else -> "%.2f %s"
        }
        return String.format(Locale.US, formatStr, converted, targetUnit.symbol)
    }

    fun formatArea(areaInSqMeters: Float, targetUnit: MeasurementUnit = this): String {
        val converted = areaInSqMeters * (targetUnit.metersMultiplier * targetUnit.metersMultiplier)
        val unitStr = when (targetUnit) {
            METERS -> "m²"
            FEET -> "sq ft"
            INCHES -> "sq in"
            CENTIMETERS -> "cm²"
            MILLIMETERS -> "mm²"
            YARDS -> "sq yd"
        }
        val formatStr = if (targetUnit == MILLIMETERS) "%.0f %s" else "%.1f %s"
        return String.format(Locale.US, formatStr, converted, unitStr)
    }

    fun toDisplayValue(valueInMeters: Float): Float {
        return valueInMeters * metersMultiplier
    }

    fun fromDisplayValue(displayValue: Float): Float {
        return if (metersMultiplier != 0f) displayValue / metersMultiplier else displayValue
    }
}
