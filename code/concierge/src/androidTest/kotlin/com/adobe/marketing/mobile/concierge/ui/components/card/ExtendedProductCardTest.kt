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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
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

class ExtendedProductCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Matches ConciergeStyles.extendedProductCardStyle's default cardMaxHeight.
    private val cardMaxHeight = 360.dp

    @Test
    fun extendedProductCard_wasPriceStaysWithinCardBounds_forWorstCaseContent() {
        val element = MultimodalElement(
            id = "worst-case",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name Goes Here Long Title Two Lines",
                "productDescription" to "Subtitle text goes here to describe the product or campaign",
                "productPrice" to "$399.99",
                "productWasPrice" to "$599.99"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val wasPriceBounds = composeTestRule.onNodeWithText("was $599.99").getBoundsInRoot()
        assertTrue(
            "was-price bottom (${wasPriceBounds.bottom}) exceeded the card height ($cardMaxHeight)",
            wasPriceBounds.bottom <= cardMaxHeight
        )
    }

    @Test
    fun extendedProductCard_displaysBadge_whenBadgeIsPresent() {
        val element = MultimodalElement(
            id = "with-badge",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "productBadge" to "Extended Sizes"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Extended Sizes").assertIsDisplayed()
    }

    @Test
    fun extendedProductCardDemoScreen_rendersLineCountAndContentVariantCards() {
        composeTestRule.setContent {
            ExtendedProductCardDemoScreen()
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Title & Description Variants").assertIsDisplayed()
        composeTestRule.onNodeWithText("Content Variations").assertIsDisplayed()
        // This title appears on two sample cards, so assert via the first match rather
        // than a single-node lookup.
        composeTestRule.onAllNodesWithText("Product Name Goes Here Long Title Two Lines")
            .onFirst()
            .assertIsDisplayed()
    }

    // -----------------------------------------------------------------------
    // Drop shadow rendering (--multimodal-card-box-shadow)
    // -----------------------------------------------------------------------

    private val shadowOuterPadding = 40.dp

    private fun renderCardOnWhiteBackground(themeData: ConciergeThemeData?) {
        val element = MultimodalElement(
            id = "shadow-test",
            url = null,
            content = mapOf("productName" to "Product")
        )

        composeTestRule.setContent {
            val content = @androidx.compose.runtime.Composable {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(shadowOuterPadding)
                    ) {
                        ExtendedProductCard(
                            element = element,
                            modifier = Modifier.height(cardMaxHeight)
                        )
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

    /** Red channel of the pixel just left of the card's edge, vertically centered on the card. */
    private fun samplePixelJustOutsideCardLeftEdge(): Int {
        composeTestRule.waitForIdle()
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val density = composeTestRule.density.density
        val outerPaddingPx = shadowOuterPadding.value * density
        val sampleX = (outerPaddingPx - (4.dp.value * density)).toInt().coerceAtLeast(0)
        val sampleY = (outerPaddingPx + (cardMaxHeight.value * density / 2)).toInt()
        val pixel = bitmap.getPixel(sampleX, sampleY)
        return android.graphics.Color.red(pixel)
    }

    @Test
    fun extendedProductCard_rendersShadowBeyondCardBounds_whenThemeConfiguresBoxShadow() {
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

        renderCardOnWhiteBackground(themeData)
        val red = samplePixelJustOutsideCardLeftEdge()

        assertTrue(
            "Expected a visible shadow (darker pixel) just outside the card's left edge, but " +
                "found red=$red (pure white background would be 255)",
            red < 250
        )
    }

    @Test
    fun extendedProductCard_noShadowBeyondCardBounds_whenThemeHasNoBoxShadow() {
        renderCardOnWhiteBackground(themeData = null)
        val red = samplePixelJustOutsideCardLeftEdge()

        assertTrue(
            "Expected no shadow (pure white background) just outside the card's left edge when " +
                "no box-shadow token is configured, but found red=$red",
            red >= 250
        )
    }
}
