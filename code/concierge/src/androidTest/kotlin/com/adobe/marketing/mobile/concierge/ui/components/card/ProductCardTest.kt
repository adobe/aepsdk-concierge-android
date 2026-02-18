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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ProductCard composable.
 * Tests product display, image handling, and action button interactions.
 */
class ProductCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productCard_displaysTitleAndCaption() {
        val element = MultimodalElement(
            id = "product-1",
            title = "Amazing Product",
            caption = "High quality and affordable",
            url = "https://example.com/product.jpg",
            alttext = "Product image"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Amazing Product")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("High quality and affordable")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_displaysTitleOnly() {
        val element = MultimodalElement(
            id = "product-2",
            title = "Simple Product",
            caption = null,
            url = "https://example.com/product.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("Simple Product")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_doesNotDuplicateTitleAndCaption() {
        val element = MultimodalElement(
            id = "product-3",
            title = "Product Name",
            caption = "Product Name", // Same as title
            url = "https://example.com/product.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("Product Name")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_triggersOnImageClick() {
        var clickedElement: MultimodalElement? = null
        
        val element = MultimodalElement(
            id = "product-4",
            title = "Clickable Product",
            url = "https://example.com/product.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(
                        element = element,
                        onImageClick = { clickedElement = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Clickable Product").performClick()

        assert(clickedElement?.id == "product-4")
    }

    @Test
    fun productCard_displaysActionButtons() {
        val element = MultimodalElement(
            id = "product-5",
            title = "Product with Actions",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "primaryText" to "Learn More",
                "primaryUrl" to "https://example.com/infopage",
                "secondaryText" to "Product Details",
                "secondaryUrl" to "https://example.com/details"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("Learn More")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Product Details")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_triggersOnActionClick() {
        var clickedButton: ProductActionButton? = null
        
        val element = MultimodalElement(
            id = "product-6",
            title = "Interactive Product",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "primaryText" to "Add to Cart",
                "primaryUrl" to "https://example.com/cart"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(
                        element = element,
                        onActionClick = { clickedButton = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Add to Cart").performClick()

        assert(clickedButton?.text == "Add to Cart")
        assert(clickedButton?.url == "https://example.com/cart")
    }

    @Test
    fun productCard_handlesEmptyContent() {
        val element = MultimodalElement(
            id = "product-7",
            url = "https://example.com/product.jpg"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun productCard_handlesMissingImage() {
        val element = MultimodalElement(
            id = "product-8",
            title = "No Image Product",
            caption = "This product has no image",
            url = null
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("No Image Product")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("This product has no image")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_handlesOnlyPrimaryButton() {
        val element = MultimodalElement(
            id = "product-9",
            title = "Single Action Product",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "primaryText" to "Shop Now",
                "primaryUrl" to "https://example.com/shop"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("Shop Now")
            .assertIsDisplayed()
    }

    @Test
    fun productCard_handlesOnlySecondaryButton() {
        val element = MultimodalElement(
            id = "product-10",
            title = "Info Product",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "secondaryText" to "Read Reviews",
                "secondaryUrl" to "https://example.com/reviews"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("Read Reviews")
            .assertIsDisplayed()
    }
}
