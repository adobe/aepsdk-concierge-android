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

package com.adobe.marketing.mobile.concierge.ui.components.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.utils.image.ImageDownloader
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AsyncImage composable to load and display an image from a URL asynchronously.
 * Uses [ImageDownloader] for async image loading with in-memory caching.
 */
@Composable
internal fun AsyncImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onError: ((Throwable) -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    val imageDownloader = ImageDownloader.getInstance()

    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var hasError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        isLoading = true
        hasError = false

        try {
            var bitmap = imageDownloader.getCachedBitmap(url)

            // If not in cache, download
            if (bitmap == null) {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    "AsyncImage",
                    "Image not found in cache, downloading image from URL: $url"
                )
                bitmap = withContext(Dispatchers.IO) {
                    imageDownloader.downloadImage(url)
                }
            }

            bitmap?.let {
                imageBitmap = it.asImageBitmap()
                isLoading = false
            } ?: run {
                hasError = true
                isLoading = false
                onError?.invoke(Exception("Failed to load image: $url"))
            }
        } catch (e: Exception) {
            hasError = true
            isLoading = false
            onError?.invoke(e)
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                // Show loading indicator until image is loaded
                placeholder?.invoke() ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            hasError -> {
                // Show error state
                error?.invoke() ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            imageBitmap != null -> {
                // Show the loaded image
                Image(
                    painter = BitmapPainter(imageBitmap!!),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )
            }
        }
    }
}

