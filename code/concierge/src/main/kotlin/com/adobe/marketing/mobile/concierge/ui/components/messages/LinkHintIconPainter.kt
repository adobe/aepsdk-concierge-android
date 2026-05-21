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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.components.image.assetBitmapCache
import com.adobe.marketing.mobile.concierge.ui.components.image.loadAssetBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Returns a [Painter] for the link-hint inline icon. Tries the themed bitmap under
 * `assets/icons/[assetName].{png,webp,jpg,jpeg}` first; falls back to the SDK's
 * [R.drawable.external_link] vector when the name is null/blank or the asset can't be
 * resolved. Always non-null so callers don't need branching.
 */
@Composable
internal fun rememberLinkHintIconPainter(assetName: String?): Painter {
    val themed: ImageBitmap? = if (assetName.isNullOrBlank()) {
        null
    } else {
        val context = LocalContext.current
        produceState<ImageBitmap?>(
            initialValue = assetBitmapCache[assetName],
            key1 = assetName
        ) {
            if (!assetBitmapCache.containsKey(assetName)) {
                val loaded = withContext(Dispatchers.IO) {
                    loadAssetBitmap(context, assetName)?.asImageBitmap()
                }
                assetBitmapCache[assetName] = loaded
                value = loaded
            }
        }.value
    }

    return themed?.let { BitmapPainter(it) }
        ?: painterResource(R.drawable.external_link)
}
