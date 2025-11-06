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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for providing Concierge colors throughout the app
 */
val LocalConciergeColors = staticCompositionLocalOf { LightConciergeColors }

/**
 * Theme provider for Concierge UI components.
 * Automatically switches between light and dark color schemes based on system theme.
 *
 * @param darkTheme Whether to use dark theme. Defaults to system setting.
 * @param content The composable content to theme.
 */
@Composable
fun ConciergeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkConciergeColors else LightConciergeColors

    CompositionLocalProvider(
        LocalConciergeColors provides colors,
        content = content
    )
}

/**
 * Object to access current Concierge theme colors
 */
object ConciergeTheme {
    /**
     * Retrieves the current Concierge color scheme
     */
    val colors: ConciergeColors
        @Composable get() = LocalConciergeColors.current
}
