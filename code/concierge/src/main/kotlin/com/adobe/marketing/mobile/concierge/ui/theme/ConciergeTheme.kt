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

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * Data class holding the active theme configuration including resolved colors
 */
data class ActiveConciergeTheme(
    val colors: ConciergeColors,
    val config: ConciergeThemeConfig? = null,
    val themeTokens: ConciergeThemeTokens? = null
)

/**
 * CompositionLocal for providing the active Concierge theme
 */
private val LocalActiveConciergeTheme = staticCompositionLocalOf { 
    ActiveConciergeTheme(colors = LightConciergeColors)
}

/**
 * Theme provider for Concierge UI components.
 * Automatically switches between light and dark color schemes based on system theme.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param theme Optional theme configuration to override default theme.
 * @param content The composable content to theme.
 */
@Composable
fun ConciergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: ConciergeThemeConfig? = null,
    themeTokens: ConciergeThemeTokens? = null,
    content: @Composable () -> Unit
) {
    val defaultColors = if (darkTheme) DarkConciergeColors else LightConciergeColors
    
    // Apply theme colors if available, otherwise use defaults
    val colors = remember(theme, darkTheme) {
        if (theme?.colors != null) {
            ThemeParser.createColorsFromJson(theme.colors, defaultColors)
        } else {
            defaultColors
        }
    }
    
    val activeTheme = remember(colors, theme, themeTokens) {
        ActiveConciergeTheme(colors = colors, config = theme, themeTokens = themeTokens)
    }

    CompositionLocalProvider(
        LocalActiveConciergeTheme provides activeTheme,
        content = content
    )
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
            val themeConfig = ThemeParser.loadThemeFromAssets(context, fileName)
            val tokens = ThemeParser.parseThemeTokens(
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            )
            Pair(themeConfig, tokens)
        }
    }
    
    ConciergeTheme(
        darkTheme = darkTheme,
        theme = themeData?.first,
        themeTokens = themeData?.second,
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
     * Retrieves text strings from the theme configuration
     */
    val text: ConciergeTextStrings?
        @Composable get() = LocalActiveConciergeTheme.current.config?.text
    
    /**
     * Retrieves disclaimer configuration from the theme
     */
    val disclaimer: ConciergeDisclaimer?
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
