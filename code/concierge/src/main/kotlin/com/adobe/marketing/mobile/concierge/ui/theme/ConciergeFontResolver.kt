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

import androidx.compose.ui.text.font.FontFamily

/**
 * Resolves the `--font-family` CSS string from the theme JSON into an Android system [FontFamily].
 *
 * Supported values (case-insensitive):
 * - `"serif"`       → [FontFamily.Serif]
 * - `"sans-serif"`  → [FontFamily.SansSerif]
 * - `"monospace"`   → [FontFamily.Monospace]
 *
 * Returns `null` for any unknown or blank value; callers fall back to the platform default font.
 */
internal object ConciergeFontResolver {

    private const val SERIF = "serif"
    private const val SANS_SERIF = "sans-serif"
    private const val MONOSPACE = "monospace"

    fun resolve(fontFamily: String?): FontFamily? {
        return when (fontFamily?.trim()?.lowercase()) {
            SERIF -> FontFamily.Serif
            SANS_SERIF -> FontFamily.SansSerif
            MONOSPACE -> FontFamily.Monospace
            else -> null
        }
    }
}
