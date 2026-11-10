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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Assert.assertTrue
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
    fun productCard_hidesActionButton_whenTextIsWhitespaceOnly() {
        val element = MultimodalElement(
            id = "product-whitespace",
            title = "Product with blank action",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "primaryText" to "   ",
                "primaryUrl" to "https://example.com/infopage"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductCard(element = element)
                }
            }
        }

        composeTestRule.onNodeWithText("   ").assertDoesNotExist()
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

    // -----------------------------------------------------------------------
    // Drop shadow rendering (--multimodal-card-box-shadow)
    // -----------------------------------------------------------------------

    private val shadowOuterPadding = 40.dp

    private fun renderProductCardOnWhiteBackground(themeData: ConciergeThemeData?) {
        val element = MultimodalElement(
            id = "shadow-test",
            title = "Product",
            url = null
        )

        composeTestRule.setContent {
            val content = @androidx.compose.runtime.Composable {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(shadowOuterPadding)
                    ) {
                        ProductCard(element = element)
                    }
                }
            }
            if (themeData != null) {
                ConciergeTheme(theme = themeData) { content() }
            } else {
                ConciergeTheme { content() }
            }
        }
    }

    /** Red channel of the pixel just left of the full-width card's edge, near its top. */
    private fun redJustOutsideCardLeftEdge(): Int {
        composeTestRule.waitForIdle()
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val density = composeTestRule.density.density
        val outerPaddingPx = shadowOuterPadding.value * density
        val sampleX = (outerPaddingPx - (4.dp.value * density)).toInt().coerceAtLeast(0)
        val sampleY = (outerPaddingPx + (80.dp.value * density)).toInt()
        return android.graphics.Color.red(bitmap.getPixel(sampleX, sampleY))
    }

    @Test
    fun productCard_rendersShadowBeyondCardBounds_whenThemeConfiguresBoxShadow() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(
                    multimodalCardBoxShadow = mapOf(
                        "offsetX" to 0.0,
                        "offsetY" to 0.0,
                        "blurRadius" to 16.0,
                        "spreadRadius" to 0.0,
                        "color" to Color.Black
                    )
                )
            )
        )

        renderProductCardOnWhiteBackground(themeData)
        val red = redJustOutsideCardLeftEdge()

        assertTrue(
            "Expected a visible shadow (darker pixel) just outside the card's left edge, but " +
                "found red=$red (pure white background would be 255)",
            red < 250
        )
    }

    @Test
    fun productCard_noShadowBeyondCardBounds_whenThemeHasNoBoxShadow() {
        renderProductCardOnWhiteBackground(themeData = null)
        val red = redJustOutsideCardLeftEdge()

        assertTrue(
            "Expected no custom box-shadow (near-white background) just outside the card's left " +
                "edge when no box-shadow token is configured, but found red=$red",
            red >= 250
        )
    }
}
