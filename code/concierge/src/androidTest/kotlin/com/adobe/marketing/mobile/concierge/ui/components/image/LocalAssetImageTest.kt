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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.graphics.asImageBitmap
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.After
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the LocalAssetImage composable.
 * Verifies routing between remote URL loading and local asset loading,
 * and that each path renders without crashing regardless of load outcome.
 */
class LocalAssetImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun clearBitmapCache() {
        assetBitmapCache.clear()
    }

    @Test
    fun localAssetImage_withHttpsUrl_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    LocalAssetImage(
                        source = "https://example.com/brand-icon.png",
                        contentDescription = "Brand icon"
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun localAssetImage_withHttpUrl_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    LocalAssetImage(
                        source = "http://example.com/brand-icon.png",
                        contentDescription = null
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun localAssetImage_withLocalAssetName_rendersWithoutCrashingWhenFileAbsent() {
        // Asset "nonexistent-icon" does not exist in the test APK's assets/icons/ folder.
        // LocalFileImage should silently render nothing rather than throwing.
        composeTestRule.setContent {
            ConciergeTheme {
                LocalAssetImage(
                    source = "nonexistent-icon",
                    contentDescription = null
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun localAssetImage_withLocalAssetName_cachesPreviouslyLoadedResult() {
        // Render the composable once. After the produceState block completes the
        // null result (asset is absent) must be stored in the cache so that
        // subsequent instances skip the IO load entirely.
        val source = "cache-test-icon"

        composeTestRule.setContent {
            ConciergeTheme {
                LocalAssetImage(
                    source = source,
                    contentDescription = null
                )
            }
        }
        composeTestRule.waitForIdle()
        // The cache is populated after a Dispatchers.IO load completes, which is not
        // tracked by Compose idle. Poll until the key is present to avoid a race.
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            assetBitmapCache.containsKey(source)
        }
    }

    @Test
    fun localAssetImage_withCachedBitmap_rendersImage() {
        // Pre-seed the cache with a valid bitmap to exercise the render branch
        // (bitmap?.let { Image(...) }) without requiring a real asset file on disk.
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
        assetBitmapCache["cached-icon"] = bitmap

        composeTestRule.setContent {
            ConciergeTheme {
                LocalAssetImage(
                    source = "cached-icon",
                    contentDescription = "Cached icon"
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Cached icon").assertIsDisplayed()
    }

    @Test
    fun localAssetImage_withEmptySource_rendersWithoutCrashing() {
        // An empty string is not a URL, so it routes to LocalFileImage which will
        // fail to find a matching asset and render nothing — no crash expected.
        composeTestRule.setContent {
            ConciergeTheme {
                LocalAssetImage(
                    source = "",
                    contentDescription = null
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
