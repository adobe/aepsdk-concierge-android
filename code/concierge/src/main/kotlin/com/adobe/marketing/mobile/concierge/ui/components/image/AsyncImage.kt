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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

/**
 * AsyncImage composable to load and display an image from a URL asynchronously.
 * Uses [ImageProvider] for async image loading with in-memory caching.
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
    val imageResult = rememberRemoteImage(url, onError)
    val themeColors = ConciergeTheme.colors

    Box(modifier = modifier) {
        when (imageResult.state) {
            ImageLoadingState.Loading -> {
                // Show loading indicator until image is loaded
                placeholder?.invoke() ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(themeColors.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = themeColors.primary
                        )
                    }
                }
            }

            ImageLoadingState.Error -> {
                // Show error state
                error?.invoke() ?: run {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(themeColors.surface),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }

            ImageLoadingState.Success -> {
                Crossfade(
                    targetState = imageResult.image,
                    animationSpec = tween(durationMillis = 220),
                    label = "imageFadeIn"
                ) { bitmap ->
                    bitmap?.let {
                        Image(
                            painter = BitmapPainter(it),
                            contentDescription = contentDescription,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = contentScale
                        )
                    }
                }
            }
        }
    }
}

/**
 * Represents the different states of image loading.
 */
private enum class ImageLoadingState {
    Loading,
    Success,
    Error
}

/**
 * Data class to hold the result of image loading with state.
 */
private data class ImageResult(
    val image: ImageBitmap?,
    val state: ImageLoadingState
)

/**
 * Composable that remembers a remote image loaded from the provided URL.
 * Uses the [ImageProvider] to handle caching and downloading.
 * Returns both the image and loading state.
 */
@Composable
private fun rememberRemoteImage(url: String, onError: ((Throwable) -> Unit)? = null): ImageResult {
    val provider = LocalImageProvider.current
    return produceState(
        initialValue = provider.getCached(url)?.asImageBitmap()?.let { bitmap ->
            ImageResult(image = bitmap, state = ImageLoadingState.Success)
        } ?: ImageResult(image = null, state = ImageLoadingState.Loading),
        key1 = url
    ) {
        if (value.state == ImageLoadingState.Loading) {
            try {
                val bitmap = provider.get(url).asImageBitmap()
                value = ImageResult(image = bitmap, state = ImageLoadingState.Success)
            } catch (e: Exception) {
                // Image loading failed
                value = ImageResult(image = null, state = ImageLoadingState.Error)
                onError?.invoke(e)
            }
        }
    }.value
}

