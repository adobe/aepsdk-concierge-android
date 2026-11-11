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

import android.graphics.Bitmap
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the AsyncImage composable.
 */
class AsyncImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun asyncImage_urlChangesDuringDownload_rendersNewImageWithoutReportingCancellation() {
        assertImageReloadsAfterUrlChange(PreviousImageState.LOADING)
    }

    @Test
    fun asyncImage_urlChangesAfterSuccess_rendersNewImage() {
        assertImageReloadsAfterUrlChange(PreviousImageState.SUCCESS)
    }

    @Test
    fun asyncImage_urlChangesAfterError_rendersNewImage() {
        assertImageReloadsAfterUrlChange(PreviousImageState.ERROR)
    }

    @Test
    fun asyncImage_urlChangesAfterCacheHit_rendersNewImage() {
        assertImageReloadsAfterUrlChange(PreviousImageState.CACHED)
    }

    private fun assertImageReloadsAfterUrlChange(previousState: PreviousImageState) {
        val oldUrl = "https://example.com/old.png"
        val newUrl = "https://example.com/new.png"
        val provider = ControlledImageProvider()
        val oldImage = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.RED)
        }
        val newImage = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
        val failure = IOException("Old image failed")
        val errors = mutableListOf<Throwable>()
        val url = mutableStateOf(oldUrl)
        if (previousState == PreviousImageState.CACHED) {
            provider.cached[oldUrl] = oldImage
        }

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides provider) {
                    AsyncImage(
                        url = url.value,
                        contentDescription = "Loaded image",
                        modifier = Modifier.size(64.dp),
                        onError = { errors.add(it) }
                    )
                }
            }
        }
        composeTestRule.runOnIdle {
            when (previousState) {
                PreviousImageState.SUCCESS -> provider.complete(oldUrl, oldImage)
                PreviousImageState.ERROR -> provider.fail(oldUrl, failure)
                else -> Unit
            }
        }
        composeTestRule.waitForIdle()
        if (previousState == PreviousImageState.SUCCESS || previousState == PreviousImageState.CACHED) {
            assertRenderedImageColor(Color.Red)
        }
        composeTestRule.runOnIdle {
            assertEquals(
                if (previousState == PreviousImageState.CACHED) emptyList<String>() else listOf(oldUrl),
                provider.requested
            )
            assertEquals(
                if (previousState == PreviousImageState.ERROR) listOf(failure) else emptyList<Throwable>(),
                errors
            )
            url.value = newUrl
        }
        composeTestRule.runOnIdle {
            assertTrue("The replacement URL must be requested", newUrl in provider.requested)
            provider.complete(newUrl, newImage)
        }
        composeTestRule.waitForIdle()
        assertRenderedImageColor(Color.Blue)
        composeTestRule.runOnIdle {
            assertEquals(
                if (previousState == PreviousImageState.ERROR) listOf(failure) else emptyList<Throwable>(),
                errors
            )
            assertEquals(
                if (previousState == PreviousImageState.LOADING) listOf(oldUrl) else emptyList<String>(),
                provider.cancelled
            )
        }
    }

    private fun assertRenderedImageColor(expected: Color) {
        val pixels = composeTestRule.onNodeWithContentDescription("Loaded image")
            .assertIsDisplayed()
            .captureToImage()
            .toPixelMap()
        assertEquals(expected, pixels[pixels.width / 2, pixels.height / 2])
    }

    private enum class PreviousImageState { LOADING, SUCCESS, ERROR, CACHED }

    private class ControlledImageProvider : ImageProvider {
        val cached = mutableMapOf<String, Bitmap>()
        val requested = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        private val pending = mutableMapOf<String, CompletableDeferred<Bitmap>>()

        override fun getCached(url: String): Bitmap? = cached[url]

        override suspend fun get(url: String): Bitmap {
            requested.add(url)
            val result = CompletableDeferred<Bitmap>()
            pending[url] = result
            return try {
                result.await()
            } catch (e: CancellationException) {
                cancelled.add(url)
                throw e
            }
        }

        fun complete(url: String, bitmap: Bitmap) {
            checkNotNull(pending[url]).complete(bitmap)
        }

        fun fail(url: String, error: Throwable) {
            checkNotNull(pending[url]).completeExceptionally(error)
        }

        override fun clear() {
            cached.clear()
        }
    }

    @Test
    fun asyncImage_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    AsyncImage(
                        url = "https://example.com/image.jpg",
                        contentDescription = "Test image"
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun asyncImage_withContentDescription_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    AsyncImage(
                        url = "https://example.com/photo.jpg",
                        contentDescription = "Product photo"
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        // Content description is set on Image which appears only after load; just verify no crash
    }

    @Test
    fun asyncImage_withCustomPlaceholder_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    AsyncImage(
                        url = "https://example.com/slow.jpg",
                        contentDescription = null,
                        placeholder = { }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
