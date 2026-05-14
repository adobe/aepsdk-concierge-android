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
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves a [ConciergeFontFamilySpec] (parsed from the theme JSON's `--font-family` object)
 * into a Compose [FontFamily] backed by font files in the host app's `assets/fonts/` folder.
 *
 * Each non-null slot in the spec is treated as a file basename (no extension). The resolver
 * tries `.ttf` first, then `.otf`. Missing files for a declared slot log a warning and are
 * dropped from the resulting family (Compose will synthesize the variant from neighbouring
 * weights/styles).
 *
 * Returns `null` if the spec is null, has no slots set, or none of the declared slots
 * resolved to an actual asset. Callers fall back to the platform default font.
 *
 * Results are cached by spec content; misses are remembered too.
 */
internal object ConciergeFontResolver {
    private const val TAG = "ConciergeFontResolver"
    private const val FONTS_DIR = "fonts"
    private val supportedExtensions = listOf("ttf", "otf")

    private val cache = ConcurrentHashMap<ConciergeFontFamilySpec, FontFamily>()
    private val missing = ConcurrentHashMap.newKeySet<ConciergeFontFamilySpec>()

    fun resolve(context: Context, spec: ConciergeFontFamilySpec?): FontFamily? {
        if (spec == null) return null
        cache[spec]?.let { return it }
        if (missing.contains(spec)) return null

        val assets = context.applicationContext.assets
        val availableFiles = runCatching { assets.list(FONTS_DIR) }.getOrNull().orEmpty().toSet()

        val slots = listOf(
            Slot(spec.thin, FontWeight.Thin, FontStyle.Normal, "thin"),
            Slot(spec.light, FontWeight.Light, FontStyle.Normal, "light"),
            Slot(spec.regular, FontWeight.Normal, FontStyle.Normal, "regular"),
            Slot(spec.italic, FontWeight.Normal, FontStyle.Italic, "italic"),
            Slot(spec.bold, FontWeight.Bold, FontStyle.Normal, "bold"),
            Slot(spec.black, FontWeight.Black, FontStyle.Normal, "black")
        )

        val fonts = slots.mapNotNull { slot ->
            val basename = slot.basename?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val matched = findFontFile(basename, availableFiles)
            if (matched == null) {
                Log.warning(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Font asset '$basename' for slot '${slot.name}' not found under assets/$FONTS_DIR/ (.ttf or .otf). Slot dropped."
                )
                return@mapNotNull null
            }
            Font(
                path = "$FONTS_DIR/$matched",
                assetManager = assets,
                weight = slot.weight,
                style = slot.style
            )
        }

        if (fonts.isEmpty()) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No font slots resolved for spec; falling back to default font."
            )
            missing.add(spec)
            return null
        }

        val family = FontFamily(fonts)
        cache[spec] = family
        return family
    }

    @VisibleForTesting
    internal fun clearCaches() {
        cache.clear()
        missing.clear()
    }

    private fun findFontFile(basename: String, files: Set<String>): String? {
        return supportedExtensions.firstNotNullOfOrNull { ext ->
            val candidate = "$basename.$ext"
            files.firstOrNull { it.equals(candidate, ignoreCase = true) }
        }
    }

    private data class Slot(
        val basename: String?,
        val weight: FontWeight,
        val style: FontStyle,
        val name: String
    )
}
