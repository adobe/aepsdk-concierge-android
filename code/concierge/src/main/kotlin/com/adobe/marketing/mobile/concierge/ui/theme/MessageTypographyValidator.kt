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

package com.adobe.marketing.mobile.concierge.ui.theme

import android.util.Log

internal object MessageTypographyValidator {
    private const val LOG_TAG = "ConciergeTheme"

    fun isValidFontSize(value: Double): Boolean =
        value.isFinite() && value > 0 && value.toFloat().isFinite() && value.toFloat() > 0

    fun isValidFontWeight(value: Int): Boolean = value in 1..1000

    fun validate(typography: ConciergeTypography): ConciergeTypography {
        val fontSize = typography.fontSize?.takeIf(::isValidFontSize)
        val fontWeight = typography.fontWeight?.takeIf(::isValidFontWeight)
        val linkFontWeight = typography.linkFontWeight?.takeIf(::isValidFontWeight)
        val lineHeight = typography.lineHeight?.takeIf(::isValidFontSize)
        val lineHeightPx = typography.lineHeightPx?.takeIf(::isValidFontSize)
        if (fontSize != typography.fontSize) {
            Log.w(LOG_TAG, "Ignoring invalid message fontSize; expected a finite positive Compose size.")
        }
        if (fontWeight != typography.fontWeight) {
            Log.w(LOG_TAG, "Ignoring invalid message fontWeight; expected a weight from 1 to 1000.")
        }
        if (linkFontWeight != typography.linkFontWeight) {
            Log.w(LOG_TAG, "Ignoring invalid message linkFontWeight; expected a weight from 1 to 1000.")
        }
        if (lineHeight != typography.lineHeight || lineHeightPx != typography.lineHeightPx) {
            Log.w(LOG_TAG, "Ignoring invalid message line height; expected a finite positive value.")
        }
        return typography.copy(
            fontSize = fontSize, fontWeight = fontWeight, linkFontWeight = linkFontWeight,
            lineHeight = lineHeight, lineHeightPx = lineHeightPx
        )
    }
}
