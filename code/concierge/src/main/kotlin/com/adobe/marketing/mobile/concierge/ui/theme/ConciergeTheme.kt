/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.theme

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.adobe.marketing.mobile.concierge.R

/**
 * Data class holding the active theme configuration including resolved colors
 * @param useDefaultPalette True when no theme JSON is loaded (colors are Light/Dark defaults)
 */
data class ActiveConciergeTheme(
    val colors: ConciergeColors,
    val config: ConciergeThemeConfig? = null,
    val themeTokens: ConciergeThemeTokens? = null,
    val useDefaultPalette: Boolean = false
)

/**
 * CompositionLocal for providing the active Concierge theme
 */
private val LocalActiveConciergeTheme = staticCompositionLocalOf { 
    ActiveConciergeTheme(colors = LightConciergeColors)
}

private val bundledFonts: Map<String, FontFamily> = mapOf(
    "raleway" to FontFamily(
        Font(R.font.raleway_thin, FontWeight.Thin),
        Font(R.font.raleway_extralight, FontWeight.ExtraLight),
        Font(R.font.raleway_light, FontWeight.Light),
        Font(R.font.raleway_regular, FontWeight.Normal),
        Font(R.font.raleway_medium, FontWeight.Medium),
        Font(R.font.raleway_semibold, FontWeight.SemiBold),
        Font(R.font.raleway_bold, FontWeight.Bold),
        Font(R.font.raleway_extrabold, FontWeight.ExtraBold),
        Font(R.font.raleway_heavy, FontWeight.Black),
    )
)

private fun resolveFontFamily(name: String): FontFamily? {
    val key = name.trim().lowercase()
    return bundledFonts[key]
        ?: runCatching { FontFamily(Typeface.create(name, Typeface.NORMAL)) }.getOrNull()
}

/**
 * Theme provider for Concierge UI components.
 * Automatically switches between light and dark color schemes based on system theme.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param theme Complete theme data containing both config and tokens. When null (no theme JSON
 *        loaded), uses default colors for light or dark mode.
 * @param content The composable content to theme.
 */
@Composable
fun ConciergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: ConciergeThemeData? = null,
    content: @Composable () -> Unit
) {
    // When no theme is loaded, follow system light/dark so the theme recomposes on config change
    val systemDark = isSystemInDarkTheme()
    val effectiveDarkTheme = if (theme == null) systemDark else darkTheme
    val defaultColors = if (effectiveDarkTheme) DarkConciergeColors else LightConciergeColors

    // When a theme JSON is loaded, use only the colors defined in that theme. Use a fixed fallback for missing
    // keys so that text and other colors are not influenced by device light/dark mode.
    val colors = remember(theme?.tokens, theme?.config, effectiveDarkTheme) {
        val themeColors = theme?.tokens?.colors ?: theme?.config?.colors
        themeColors?.let {
            ThemeParser.createColorsFromJson(it, LightConciergeColors)
        } ?: defaultColors
    }
    
    val activeTheme = remember(colors, theme) {
        ActiveConciergeTheme(
            colors = colors,
            config = theme?.config,
            themeTokens = theme?.tokens,
            useDefaultPalette = theme == null
        )
    }

    val fontFamily = remember(theme?.tokens?.typography?.fontFamily) {
        theme?.tokens?.typography?.fontFamily
            ?.takeIf { it.isNotBlank() }
            ?.let { name -> resolveFontFamily(name) }
    }

    val typography = remember(fontFamily) {
        if (fontFamily != null) {
            val base = Typography()
            Typography(
                displayLarge = base.displayLarge.copy(fontFamily = fontFamily),
                displayMedium = base.displayMedium.copy(fontFamily = fontFamily),
                displaySmall = base.displaySmall.copy(fontFamily = fontFamily),
                headlineLarge = base.headlineLarge.copy(fontFamily = fontFamily),
                headlineMedium = base.headlineMedium.copy(fontFamily = fontFamily),
                headlineSmall = base.headlineSmall.copy(fontFamily = fontFamily),
                titleLarge = base.titleLarge.copy(fontFamily = fontFamily),
                titleMedium = base.titleMedium.copy(fontFamily = fontFamily),
                titleSmall = base.titleSmall.copy(fontFamily = fontFamily),
                bodyLarge = base.bodyLarge.copy(fontFamily = fontFamily),
                bodyMedium = base.bodyMedium.copy(fontFamily = fontFamily),
                bodySmall = base.bodySmall.copy(fontFamily = fontFamily),
                labelLarge = base.labelLarge.copy(fontFamily = fontFamily),
                labelMedium = base.labelMedium.copy(fontFamily = fontFamily),
                labelSmall = base.labelSmall.copy(fontFamily = fontFamily),
            )
        } else {
            Typography()
        }
    }

    MaterialTheme(typography = typography) {
        CompositionLocalProvider(
            LocalActiveConciergeTheme provides activeTheme,
            content = content
        )
    }
}

/**
 * Theme provider that loads configuration from a JSON file in assets.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param themeFileName Name of the JSON theme file in assets (e.g., "themeDemo.json").
 *                      If null or file doesn't exist, uses default theme.
 * @param content The composable content to theme.
 */
@Composable
fun ConciergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeFileName: String?,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    val themeData = remember(themeFileName) {
        themeFileName?.let { fileName ->
            ConciergeThemeLoader.load(context, fileName)
        }
    }
    
    ConciergeTheme(
        darkTheme = darkTheme,
        theme = themeData,
        content = content
    )
}

/**
 * Object to access current Concierge theme
 */
object ConciergeTheme {
    /**
     * Retrieves the current Concierge color scheme
     */
    val colors: ConciergeColors
        @Composable get() = LocalActiveConciergeTheme.current.colors

    /**
     * True when no theme JSON is loaded (using default light/dark palette).
     */
    val useDefaultPalette: Boolean
        @Composable get() = LocalActiveConciergeTheme.current.useDefaultPalette
    
    /**
     * Retrieves the current theme configuration (if any)
     */
    val config: ConciergeThemeConfig?
        @Composable get() = LocalActiveConciergeTheme.current.config
    
    /**
     * Retrieves the full theme tokens (if any)
     */
    val tokens: ConciergeThemeTokens?
        @Composable get() = LocalActiveConciergeTheme.current.themeTokens
    
    /**
     * Retrieves behavior configuration from the theme
     */
    val behavior: ConciergeThemeBehavior?
        @Composable get() = LocalActiveConciergeTheme.current.themeTokens?.behavior
    
    /**
     * Retrieves text strings from the theme configuration
     */
    val text: ConciergeTextStrings?
        @Composable get() = LocalActiveConciergeTheme.current.config?.text
    
    /**
     * Retrieves disclaimer configuration from the theme
     */
    val disclaimer: DisclaimerConfig?
        @Composable get() = LocalActiveConciergeTheme.current.config?.disclaimer
    
    /**
     * Retrieves typography configuration (font sizes) from the theme
     */
    val typography: ConciergeTypographyConfig?
        @Composable get() = LocalActiveConciergeTheme.current.config?.typography

    /**
     * Material Design ColorScheme derived from ConciergeColors.
     * Used for components that require Material ColorScheme (e.g., markdown rendering).
     */
    val colorScheme: ColorScheme
        @Composable get() {
            return if (isSystemInDarkTheme()) {
                darkColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    secondary = colors.secondary,
                    surface = colors.surface,
                    onSurface = colors.onSurface,
                    surfaceContainer = colors.container,
                    surfaceContainerHighest = colors.container,
                    background = colors.background,
                    error = colors.error,
                    onError = colors.onError
                )
            } else {
                lightColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    secondary = colors.secondary,
                    surface = colors.surface,
                    onSurface = colors.onSurface,
                    surfaceContainer = colors.container,
                    surfaceContainerHighest = colors.container,
                    background = colors.background,
                    error = colors.error,
                    onError = colors.onError
                )
            }
        }

}
