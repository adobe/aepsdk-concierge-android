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

package com.adobe.marketing.mobile.concierge.ui.components.card

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ProductImage composable.
 */
class ProductImageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productImage_withUrl_rendersWithoutCrashing() {
        val element = MultimodalElement(
            id = "img-1",
            title = "Product",
            url = "https://example.com/image.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductImage(element = element)
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productImage_click_triggersCallback() {
        var clickedElement: MultimodalElement? = null
        val element = MultimodalElement(
            id = "img-2",
            title = "Product",
            url = "https://example.com/image.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductImage(
                        element = element,
                        onImageClick = { clickedElement = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ProductImage").performClick()
        assert(clickedElement?.id == "img-2")
    }

    @Test
    fun productImage_noUrl_rendersWithoutCrashing() {
        val element = MultimodalElement(
            id = "img-3",
            title = "No URL",
            url = null
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductImage(element = element)
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productImage_multiElement_showsOverlay() {
        val element = MultimodalElement(
            id = "img-4",
            title = "Product",
            url = "https://example.com/image.jpg",
            content = mapOf("productName" to "Widget")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductImage(element = element, isMultiElement = true)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Widget").assertExists()
    }
}
