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

package com.adobe.marketing.mobile.conciergetestapp.fnb.renderers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/**
 * Theme keys the F&B widgets read from the `"theme"` block of theme.json (retained by the SDK in
 * `ConciergeThemeTokens.cssVariables`). See Documentation/fnb-theme-keys.md.
 */
object FnbThemeKeys {
    // Colors: hex (#RGB, #RRGGBB, #RRGGBBAA) or "transparent"
    const val CARD_BACKGROUND = "--fnb-card-background-color"
    const val CARD_BORDER = "--fnb-card-border-color"
    const val TEXT_PRIMARY = "--fnb-text-primary-color"
    const val TEXT_SECONDARY = "--fnb-text-secondary-color"
    const val ACCENT = "--fnb-accent-color"
    const val ON_ACCENT = "--fnb-on-accent-color"
    const val DISABLED = "--fnb-disabled-color"
    const val TAB_BACKGROUND = "--fnb-tab-background-color"
    const val TAB_TEXT = "--fnb-tab-text-color"
    const val TILE_BACKGROUND = "--fnb-tile-background-color"
    const val TILE_IMAGE_PLACEHOLDER = "--fnb-tile-image-placeholder-color"
    const val TAG_BACKGROUND = "--fnb-tag-background-color"
    const val TAG_TEXT = "--fnb-tag-text-color"
    const val ERROR = "--fnb-error-color"
    const val STATUS_ONLINE = "--fnb-status-online-color"
    const val STATUS_OFFLINE = "--fnb-status-offline-color"
    const val SHEET_BACKGROUND = "--fnb-sheet-background-color"

    // Dimensions: "12px", "12dp" or "12" (dp)
    const val CARD_CORNER_RADIUS = "--fnb-card-corner-radius"
    const val CONTENT_PADDING = "--fnb-content-padding"
    const val GRID_SPACING = "--fnb-grid-spacing"
    const val TILE_CORNER_RADIUS = "--fnb-tile-corner-radius"
    const val TILE_IMAGE_HEIGHT = "--fnb-tile-image-height"
    const val TAB_CORNER_RADIUS = "--fnb-tab-corner-radius"
    const val BUTTON_CORNER_RADIUS = "--fnb-button-corner-radius"
    const val BUTTON_HEIGHT = "--fnb-button-height"
    const val STEPPER_SIZE = "--fnb-stepper-size"

    // Number in [0.3, 0.9]: menu card height as a fraction of screen height
    const val CARD_HEIGHT_FRACTION = "--fnb-card-height-fraction"
}

@Immutable
data class FnbColors(
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val disabled: Color,
    val tabBackground: Color,
    val tabText: Color,
    val tileBackground: Color,
    val tileImagePlaceholder: Color,
    val tagBackground: Color,
    val tagText: Color,
    val error: Color,
    val statusOnline: Color,
    val statusOffline: Color,
    val sheetBackground: Color
)

@Immutable
data class FnbDimens(
    val cardCornerRadius: Dp,
    val contentPadding: Dp,
    val gridSpacing: Dp,
    val tileCornerRadius: Dp,
    val tileImageHeight: Dp,
    val tabCornerRadius: Dp,
    val buttonCornerRadius: Dp,
    val buttonHeight: Dp,
    val stepperSize: Dp,
    val cardHeightFraction: Float
)

@Immutable
data class FnbThemeValues(val colors: FnbColors, val dimens: FnbDimens)

/**
 * Resolves F&B theme values. Every default is a PLACEHOLDER (derived from the host Concierge
 * palette) until the Figma tokens are supplied; replacing them is a values-only change here and in
 * theme.json.
 */
object FnbTheme {

    val current: FnbThemeValues
        @Composable get() {
            val variables = ConciergeTheme.tokens?.cssVariables.orEmpty()
            val base = ConciergeTheme.colors
            return remember(variables, base) { resolve(variables, base) }
        }

    fun resolve(variables: Map<String, String>, base: ConciergeColors): FnbThemeValues {
        fun color(key: String, default: Color) = variables[key]?.let(FnbThemeParsing::parseColor) ?: default
        fun dp(key: String, default: Dp) = variables[key]?.let(FnbThemeParsing::parseDp)?.dp ?: default
        val accent = base.buttonPrimaryBackground ?: base.primary
        return FnbThemeValues(
            colors = FnbColors(
                cardBackground = color(FnbThemeKeys.CARD_BACKGROUND, base.surface),
                cardBorder = color(FnbThemeKeys.CARD_BORDER, base.outline),
                textPrimary = color(FnbThemeKeys.TEXT_PRIMARY, base.onSurface),
                textSecondary = color(FnbThemeKeys.TEXT_SECONDARY, base.onSurfaceVariant),
                accent = color(FnbThemeKeys.ACCENT, accent),
                onAccent = color(FnbThemeKeys.ON_ACCENT, base.buttonPrimaryText ?: base.onPrimary),
                disabled = color(FnbThemeKeys.DISABLED, base.buttonDisabled ?: base.outline),
                tabBackground = color(FnbThemeKeys.TAB_BACKGROUND, base.container),
                tabText = color(FnbThemeKeys.TAB_TEXT, base.onSurface),
                tileBackground = color(FnbThemeKeys.TILE_BACKGROUND, base.surface),
                tileImagePlaceholder = color(FnbThemeKeys.TILE_IMAGE_PLACEHOLDER, base.container),
                tagBackground = color(FnbThemeKeys.TAG_BACKGROUND, base.onSurface),
                tagText = color(FnbThemeKeys.TAG_TEXT, base.surface),
                error = color(FnbThemeKeys.ERROR, base.error),
                statusOnline = color(FnbThemeKeys.STATUS_ONLINE, Color(0xFF2E7D32)),
                statusOffline = color(FnbThemeKeys.STATUS_OFFLINE, base.onSurfaceVariant),
                sheetBackground = color(FnbThemeKeys.SHEET_BACKGROUND, base.surface)
            ),
            dimens = FnbDimens(
                cardCornerRadius = dp(FnbThemeKeys.CARD_CORNER_RADIUS, 16.dp),
                contentPadding = dp(FnbThemeKeys.CONTENT_PADDING, 12.dp),
                gridSpacing = dp(FnbThemeKeys.GRID_SPACING, 12.dp),
                tileCornerRadius = dp(FnbThemeKeys.TILE_CORNER_RADIUS, 12.dp),
                tileImageHeight = dp(FnbThemeKeys.TILE_IMAGE_HEIGHT, 112.dp),
                tabCornerRadius = dp(FnbThemeKeys.TAB_CORNER_RADIUS, 20.dp),
                buttonCornerRadius = dp(FnbThemeKeys.BUTTON_CORNER_RADIUS, 24.dp),
                buttonHeight = dp(FnbThemeKeys.BUTTON_HEIGHT, 48.dp),
                stepperSize = dp(FnbThemeKeys.STEPPER_SIZE, 32.dp),
                cardHeightFraction = variables[FnbThemeKeys.CARD_HEIGHT_FRACTION]
                    ?.let(FnbThemeParsing::parseFraction) ?: 0.65f
            )
        )
    }
}

/** Pure parsers for theme values; malformed input returns null so the default applies. */
object FnbThemeParsing {

    private const val MAX_DP = 1000f

    fun parseColor(raw: String): Color? {
        val value = raw.trim()
        if (value.equals("transparent", ignoreCase = true)) return Color.Transparent
        if (!value.startsWith("#")) return null
        val hex = value.substring(1)
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

    fun parseDp(raw: String): Float? {
        val value = raw.trim().lowercase().removeSuffix("px").removeSuffix("dp").trim()
        return value.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..MAX_DP }
    }

    fun parseFraction(raw: String): Float? =
        raw.trim().toFloatOrNull()?.takeIf { it.isFinite() }?.coerceIn(0.3f, 0.9f)
}
