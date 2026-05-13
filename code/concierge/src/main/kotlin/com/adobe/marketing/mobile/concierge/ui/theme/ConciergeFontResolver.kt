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

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves the font family declared in a theme JSON into a Compose [FontFamily] backed by
 * a font asset shipped with the host app under `assets/fonts/`.
 *
 * Source of the family name: the `--font-family` CSS variable in the theme JSON's `theme`
 * block (parsed by `CSSKeyMapper` into `tokens.typography.fontFamily`). That is the only
 * JSON path that feeds this resolver; there is no top-level `typography.fontFamily` slot.
 *
 * Convention: a value of `"--font-family": "abc"` matches either `assets/fonts/abc.ttf`
 * or `assets/fonts/abc.otf` (case-insensitive). When no matching asset is found, [resolve]
 * returns `null`, callers fall back to the platform default font, and a warning is logged.
 *
 * The returned family registers a single Normal/Normal slot. Bold and italic markdown
 * spans render with Android's synthetic styling until additional variants are wired up.
 *
 * Results are cached by family name for the life of the process; misses are remembered
 * too so repeated lookups don't re-scan the assets folder.
 */
internal object ConciergeFontResolver {
    private const val TAG = "ConciergeFontResolver"
    private const val FONTS_DIR = "fonts"
    private val supportedExtensions = listOf("ttf", "otf")

    private val cache = ConcurrentHashMap<String, FontFamily>()
    private val missing = ConcurrentHashMap.newKeySet<String>()

    fun resolve(context: Context, familyName: String?): FontFamily? {
        val name = familyName?.trim().orEmpty()
        if (name.isEmpty()) return null

        cache[name]?.let { return it }
        if (missing.contains(name)) return null

        val assets = context.applicationContext.assets
        val files = runCatching { assets.list(FONTS_DIR) }.getOrNull().orEmpty()
        val match = files.firstOrNull { file ->
            val dot = file.lastIndexOf('.')
            if (dot <= 0 || dot == file.lastIndex) return@firstOrNull false
            val base = file.substring(0, dot)
            val ext = file.substring(dot + 1).lowercase()
            base.equals(name, ignoreCase = true) && ext in supportedExtensions
        }

        if (match == null) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No font asset found for '$name' under assets/$FONTS_DIR/ (.ttf or .otf). Falling back to default font."
            )
            missing.add(name)
            return null
        }

        val family = FontFamily(
            Font(
                path = "$FONTS_DIR/$match",
                assetManager = assets,
                weight = FontWeight.Normal,
                style = FontStyle.Normal
            )
        )
        cache[name] = family
        return family
    }
}
