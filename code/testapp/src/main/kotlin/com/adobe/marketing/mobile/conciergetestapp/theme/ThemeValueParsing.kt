/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.conciergetestapp.theme

import androidx.compose.ui.graphics.Color

/**
 * Parses the custom card theme values in `ConciergeThemeTokens.cssVariables`, accepting the same
 * color notations as the SDK's own theme parser. Malformed or out-of-range input returns null so
 * the caller's default applies.
 */
object ThemeValueParsing {

    /** Upper bound for any dimension, so one bad value can't blow up a layout. */
    const val MAX_DP = 1000f

    private val rgbPattern = Regex(
        """rgba?\(\s*(\d{1,3})\s*,\s*(\d{1,3})\s*,\s*(\d{1,3})\s*(?:,\s*(\d*\.?\d+)\s*)?\)""",
        RegexOption.IGNORE_CASE
    )

    /** `#rgb`, `#rgba`, `#rrggbb`, `#rrggbbaa`, `rgb(r, g, b)`, `rgba(r, g, b, a)` or `transparent`. */
    fun parseColor(raw: String?): Color? {
        val value = raw?.trim() ?: return null
        if (value.equals("transparent", ignoreCase = true)) return Color.Transparent
        if (value.startsWith("#")) return parseHexColor(value.substring(1))
        return parseRgbColor(value)
    }

    /** `12px`, `12dp` or a bare `12`, in dp, within 0..[MAX_DP]. */
    fun parseDp(raw: String?): Float? {
        val value = raw?.trim()?.lowercase()?.removeSuffix("px")?.removeSuffix("dp")?.trim() ?: return null
        return value.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..MAX_DP }
    }

    private fun parseHexColor(hex: String): Color? {
        if (hex.any { it.digitToIntOrNull(16) == null }) return null
        val expanded = when (hex.length) {
            3, 4 -> hex.map { "$it$it" }.joinToString("")
            6, 8 -> hex
            else -> return null
        }
        val rgb = expanded.substring(0, 6).toLong(16)
        val alpha = if (expanded.length == 8) expanded.substring(6, 8).toLong(16) else 0xFF
        return Color((alpha shl 24) or rgb)
    }

    private fun parseRgbColor(value: String): Color? {
        val match = rgbPattern.matchEntire(value) ?: return null
        val (r, g, b) = match.groupValues.subList(1, 4).map { it.toInt() }
        if (r > 255 || g > 255 || b > 255) return null
        val alpha = match.groupValues[4].takeIf { it.isNotEmpty() }?.toFloat() ?: 1f
        if (alpha > 1f) return null
        return Color(red = r, green = g, blue = b, alpha = (alpha * 255).toInt())
    }
}
