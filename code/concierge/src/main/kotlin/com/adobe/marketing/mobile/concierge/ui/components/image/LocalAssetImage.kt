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

package com.adobe.marketing.mobile.concierge.ui.components.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val ASSET_ICONS_FOLDER = "icons"
internal val SUPPORTED_EXTENSIONS = listOf(".png", ".webp", ".jpg", ".jpeg")

@VisibleForTesting
internal val assetBitmapCache: MutableMap<String, ImageBitmap?> = Collections.synchronizedMap(HashMap())

/**
 * Composable that loads and displays a company icon from either a remote URL or the app's
 * assets/icons folder.
 *
 * If [source] starts with "http://" or "https://", it is treated as a remote URL and loaded
 * via [AsyncImage]. Otherwise it is treated as a local asset name (without extension) and
 * resolved from the assets/icons folder, trying ".png", ".webp", ".jpg", ".jpeg" in order.
 *
 * Renders nothing if the remote request fails or no matching local file is found.
 */
@Composable
internal fun LocalAssetImage(
    source: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    if (source.startsWith("http://") || source.startsWith("https://")) {
        AsyncImage(
            url = source,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            error = if (fallback != null) {
                {
                    AsyncImage(
                        url = fallback,
                        contentDescription = contentDescription,
                        modifier = modifier,
                        contentScale = contentScale
                    )
                }
            } else null
        )
    } else {
        LocalFileImage(
            assetName = source,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

/**
 * Returns the loaded [ImageBitmap] for a local asset, or null if the asset does not exist or
 * has not finished loading yet. Results are cached so subsequent calls are synchronous.
 */
@Composable
internal fun rememberLocalAssetBitmap(assetName: String): ImageBitmap? {
    val context = LocalContext.current
    return produceState(initialValue = assetBitmapCache[assetName], key1 = assetName) {
        if (!assetBitmapCache.containsKey(assetName)) {
            val loaded = withContext(Dispatchers.IO) {
                loadAssetBitmap(context, assetName)?.asImageBitmap()
            }
            assetBitmapCache[assetName] = loaded
            value = loaded
        }
    }.value
}

@Composable
private fun LocalFileImage(
    assetName: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: String? = null,
    contentScale: ContentScale = ContentScale.Fit
) {
    val context = LocalContext.current
    val bitmap = produceState(initialValue = assetBitmapCache[assetName], key1 = assetName) {
        if (!assetBitmapCache.containsKey(assetName)) {
            val loaded = withContext(Dispatchers.IO) {
                loadAssetBitmap(context, assetName)?.asImageBitmap()
            }
            assetBitmapCache[assetName] = loaded
            value = loaded
        } else if (assetBitmapCache[assetName] == null && !fallback.isNullOrBlank()) {
            val fallbackImage = withContext(Dispatchers.IO) {
                loadAssetBitmap(context, fallback)?.asImageBitmap()
            }
            assetBitmapCache[fallback] = fallbackImage
            value = fallbackImage
        }
    }.value

    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

internal fun loadAssetBitmap(context: Context, name: String): Bitmap? {
    for (ext in SUPPORTED_EXTENSIONS) {
        try {
            val path = "$ASSET_ICONS_FOLDER/$name$ext"
            return context.assets.open(path).use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            // Try the next extension
        }
    }
    return null
}
