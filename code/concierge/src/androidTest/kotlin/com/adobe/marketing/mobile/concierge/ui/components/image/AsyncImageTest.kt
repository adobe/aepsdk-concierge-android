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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the AsyncImage composable.
 */
class AsyncImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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
