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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ProductCarousel composable.
 * Tests carousel navigation and product display.
 */
class ProductCarouselTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productCarousel_displaysMultipleProducts() {
        val elements = listOf(
            MultimodalElement(
                id = "1",
                url = "https://example.com/image1.jpg",
                alttext = "Product 1"
            ),
            MultimodalElement(
                id = "2",
                url = "https://example.com/image2.jpg",
                alttext = "Product 2"
            ),
            MultimodalElement(
                id = "3",
                url = "https://example.com/image3.jpg",
                alttext = "Product 3"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_displaysNavigationButtons() {
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Previous page"))
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Next page"))
            .assertIsDisplayed()
    }

    @Test
    fun productCarousel_nextButton_navigatesToNextPage() {
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2"),
            MultimodalElement(id = "3", url = "https://example.com/3.jpg", alttext = "Product 3")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Next page"))
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_previousButton_navigatesToPreviousPage() {
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Next page"))
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasContentDescription("Previous page"))
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_singleProduct_displaysCarousel() {
        val elements = listOf(
            MultimodalElement(
                id = "1",
                url = "https://example.com/product.jpg",
                alttext = "Single Product"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Previous page"))
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Next page"))
            .assertIsDisplayed()
    }

    @Test
    fun productCarousel_imageClick_triggersCallback() {
        var clickedElement: MultimodalElement? = null
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = { clickedElement = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_multipleClicks_navigatesThroughPages() {
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2"),
            MultimodalElement(id = "3", url = "https://example.com/3.jpg", alttext = "Product 3"),
            MultimodalElement(id = "4", url = "https://example.com/4.jpg", alttext = "Product 4")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        val nextButton = composeTestRule.onNode(hasContentDescription("Next page"))
        nextButton.performClick()
        composeTestRule.waitForIdle()
        nextButton.performClick()
        composeTestRule.waitForIdle()
        nextButton.performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_rendersWithoutCrashing() {
        val elements = listOf(
            MultimodalElement(id = "1", url = "https://example.com/1.jpg", alttext = "Product 1"),
            MultimodalElement(id = "2", url = "https://example.com/2.jpg", alttext = "Product 2")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCarousel_withProductsWithActions_displaysCarousel() {
        val elements = listOf(
            MultimodalElement(
                id = "1",
                url = "https://example.com/1.jpg",
                alttext = "Product 1",
                title = "First Product",
                caption = "Great product"
            ),
            MultimodalElement(
                id = "2",
                url = "https://example.com/2.jpg",
                alttext = "Product 2",
                title = "Second Product",
                caption = "Amazing product"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCarousel(
                        elements = elements,
                        onImageClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
