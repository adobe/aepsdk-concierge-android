/*
 Copyright 2025 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Converts a Compose Color to a hex string in the format #RRGGBB or #RRGGBBAA
 */
internal fun Color.toHexString(): String {
    val red = (this.red * 255).toInt()
    val green = (this.green * 255).toInt()
    val blue = (this.blue * 255).toInt()
    val alpha = (this.alpha * 255).toInt()
    
    return if (alpha == 255) {
        "#%02X%02X%02X".format(red, green, blue)
    } else {
        "#%02X%02X%02X%02X".format(red, green, blue, alpha)
    }
}

/**
 * Converts CSS values to Android/Compose equivalents.
 * Handles color parsing, dimension extraction, and other CSS value transformations.
 */
internal object CSSValueConverter {
    
    /**
     * Parses a CSS color value into a Compose Color.
     * Supports: hex (#RRGGBB, #RRGGBBAA), rgb(), rgba()
     */
    fun parseColor(cssValue: String): Color {
        val trimmed = cssValue.trim()
        
        return when {
            // Hex color with # prefix
            trimmed.startsWith("#") -> {
                val hex = trimmed.substring(1)
                when (hex.length) {
                    6 -> {
                        // #RRGGBB
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        Color(r, g, b)
                    }
                    8 -> {
                        // #RRGGBBAA
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        val a = hex.substring(6, 8).toInt(16)
                        Color(r, g, b, a)
                    }
                    else -> Color.Unspecified
                }
            }
            // rgb() or rgba()
            trimmed.startsWith("rgb") -> {
                val values = trimmed.substringAfter("(").substringBefore(")")
                    .split(",").map { it.trim() }
                
                when (values.size) {
                    3 -> {
                        // rgb(r, g, b)
                        val r = values[0].toIntOrNull() ?: return Color.Unspecified
                        val g = values[1].toIntOrNull() ?: return Color.Unspecified
                        val b = values[2].toIntOrNull() ?: return Color.Unspecified
                        Color(r, g, b)
                    }
                    4 -> {
                        // rgba(r, g, b, a)
                        val r = values[0].toIntOrNull() ?: return Color.Unspecified
                        val g = values[1].toIntOrNull() ?: return Color.Unspecified
                        val b = values[2].toIntOrNull() ?: return Color.Unspecified
                        val a = values[3].toFloatOrNull() ?: return Color.Unspecified
                        Color(r, g, b, (a * 255).toInt())
                    }
                    else -> Color.Unspecified
                }
            }
            else -> Color.Unspecified
        }
    }
    
    /**
     * Parses a CSS pixel value (e.g., "16px") into a Double.
     * Returns null if parsing fails.
     */
    fun parsePxValue(cssValue: String): Double? {
        val trimmed = cssValue.trim()
        return if (trimmed.endsWith("px")) {
            trimmed.substringBefore("px").trim().toDoubleOrNull()
        } else {
            trimmed.toDoubleOrNull()
        }
    }
    
    /**
     * Parses a CSS font family value.
     * Returns the font family string without quotes.
     */
    fun parseFontFamily(cssValue: String): String {
        return cssValue.trim().removeSurrounding("\"").removeSurrounding("'")
    }
    
    /**
     * Parses a CSS line-height value.
     * Can be unitless (multiplier) or px value.
     */
    fun parseLineHeight(cssValue: String): Double {
        val trimmed = cssValue.trim()
        return if (trimmed.endsWith("px")) {
            parsePxValue(trimmed) ?: 1.5
        } else {
            trimmed.toDoubleOrNull() ?: 1.5
        }
    }
    
    /**
     * Parses a CSS padding value into a list of doubles.
     * Supports: "10px", "10px 20px", "10px 20px 30px", "10px 20px 30px 40px"
     * Returns list with [top, right, bottom, left] order
     */
    fun parsePadding(cssValue: String): List<Double> {
        val values = cssValue.trim().split(Regex("\\s+"))
            .mapNotNull { parsePxValue(it) }
        
        return when (values.size) {
            1 -> listOf(values[0], values[0], values[0], values[0])
            2 -> listOf(values[0], values[1], values[0], values[1])
            3 -> listOf(values[0], values[1], values[2], values[1])
            4 -> values
            else -> listOf(0.0, 0.0, 0.0, 0.0)
        }
    }
    
    /**
     * Parses a CSS width value.
     * Supports: "100%", "320px", or numeric values
     */
    fun parseWidth(cssValue: String): Double? {
        val trimmed = cssValue.trim()
        return when {
            trimmed.endsWith("%") -> {
                trimmed.substringBefore("%").trim().toDoubleOrNull()?.let { it / 100.0 }
            }
            trimmed.endsWith("px") -> parsePxValue(trimmed)
            else -> trimmed.toDoubleOrNull()
        }
    }
    
    /**
     * Parses a CSS box-shadow value.
     * Format: "offset-x offset-y blur-radius spread-radius color"
     * Returns a map with shadow properties.
     */
    fun parseBoxShadow(cssValue: String): Map<String, Any>? {
        if (cssValue.trim().equals("none", ignoreCase = true)) {
            return null
        }
        
        val trimmed = cssValue.trim()
        
        // Simple parser for box-shadow
        // Example: "0 2px 8px rgba(0, 0, 0, 0.1)"
        val parts = mutableListOf<String>()
        var current = StringBuilder()
        var inParens = 0
        
        for (char in trimmed) {
            when (char) {
                '(' -> {
                    inParens++
                    current.append(char)
                }
                ')' -> {
                    inParens--
                    current.append(char)
                }
                ' ' -> {
                    if (inParens == 0) {
                        if (current.isNotEmpty()) {
                            parts.add(current.toString())
                            current = StringBuilder()
                        }
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        if (current.isNotEmpty()) {
            parts.add(current.toString())
        }
        
        if (parts.size < 3) return null
        
        val offsetX = parsePxValue(parts[0]) ?: 0.0
        val offsetY = parsePxValue(parts[1]) ?: 0.0
        val blurRadius = parsePxValue(parts[2]) ?: 0.0
        
        var spreadRadius = 0.0
        var colorStr = ""
        
        if (parts.size == 4) {
            // Could be spread + color or just color
            val maybePx = parsePxValue(parts[3])
            if (maybePx != null) {
                spreadRadius = maybePx
            } else {
                colorStr = parts[3]
            }
        } else if (parts.size >= 5) {
            spreadRadius = parsePxValue(parts[3]) ?: 0.0
            colorStr = parts[4]
        }
        
        val color = if (colorStr.isNotEmpty()) parseColor(colorStr) else Color.Black.copy(alpha = 0.1f)
        
        return mapOf(
            "offsetX" to offsetX,
            "offsetY" to offsetY,
            "blurRadius" to blurRadius,
            "spreadRadius" to spreadRadius,
            "color" to color
        )
    }
    
    /**
     * Parses a CSS font-weight value.
     * Returns numeric weight (400, 500, 600, 700, etc.)
     */
    fun parseFontWeight(cssValue: String): Int {
        val trimmed = cssValue.trim()
        return when (trimmed.lowercase()) {
            "normal" -> 400
            "bold" -> 700
            "lighter" -> 300
            "bolder" -> 700
            else -> trimmed.toIntOrNull() ?: 400
        }
    }
    
    /**
     * Parses a CSS order value for flexbox.
     * Returns the numeric order value.
     */
    fun parseOrder(cssValue: String): Int {
        return cssValue.trim().toIntOrNull() ?: 0
    }
}

