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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Assert.assertEquals
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
    fun extendedProductCard_displaysCtaButton_withPayloadText() {
        val element = MultimodalElement(
            id = "buy-now",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
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
        composeTestRule.onNodeWithText("Buy now").assertIsDisplayed()
    }

    @Test
    fun extendedProductCard_ctaButtonLabel_reflectsPayloadText() {
        val element = MultimodalElement(
            id = "shop-now",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Shop Now",
                "primaryUrl" to "https://example.com/checkout"
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
        composeTestRule.onNodeWithText("Shop Now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy now").assertDoesNotExist()
    }

    @Test
    fun extendedProductCard_hidesCtaButton_whenNoPrimaryAction() {
        val element = MultimodalElement(
            id = "no-primary-action",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97"
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
        composeTestRule.onNodeWithText("Buy now").assertDoesNotExist()
    }

    @Test
    fun extendedProductCard_hidesCtaButton_whenNoPrimaryUrl() {
        val element = MultimodalElement(
            id = "no-primary-url",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now"
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
        composeTestRule.onNodeWithText("Buy now").assertDoesNotExist()
    }

    @Test
    fun extendedProductCard_hidesCtaButton_whenPrimaryUrlIsBlank() {
        val element = MultimodalElement(
            id = "blank-primary-url",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "   "
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
        composeTestRule.onNodeWithTag(CTA_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Buy now").assertDoesNotExist()
    }

    @Test
    fun extendedProductCard_ctaClick_firesActionClick_notCardClick() {
        var actionClicks = 0
        var cardClicks = 0
        val element = MultimodalElement(
            id = "cta-click-isolation",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight),
                        onCardClick = { cardClicks++ },
                        onActionClick = { actionClicks++ }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(CTA_BUTTON_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        // Tapping the CTA must route only through onActionClick; the card's own click must not
        // also fire now that both live on one card.
        assertEquals(1, actionClicks)
        assertEquals(0, cardClicks)
    }

    @Test
    fun extendedProductCard_showsCtaButton_whenSubtitlePresent() {
        // CTA visibility depends only on text+url, not subtitle presence.
        val element = MultimodalElement(
            id = "buy-now-with-subtitle",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productDescription" to "Subtitle text goes here",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
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
        composeTestRule.onNodeWithText("Buy now").assertExists()
    }

    @Test
    fun extendedProductCard_invokesOnActionClick_withTappedButton() {
        val element = MultimodalElement(
            id = "buy-now-click",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
            )
        )
        var clicked: ProductActionButton? = null

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight),
                        onActionClick = { clicked = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Buy now").performClick()
        assertEquals(
            ProductActionButton(
                id = "buy-now-click_primary",
                text = "Buy now",
                url = "https://example.com/checkout",
                productName = "Product Name"
            ),
            clicked
        )
    }


    // -----------------------------------------------------------------------
    // Secondary CTA (entity_info.secondary)
    // -----------------------------------------------------------------------

    @Test
    fun extendedProductCard_displaysBothCtaButtons_whenPrimaryAndSecondaryPresent() {
        val element = MultimodalElement(
            id = "buy-now-and-learn-more",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "myapp://checkout?productId=prod-123",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://shop.com/products/prod-123"
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
        composeTestRule.onNodeWithText("Buy now").assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn more").assertIsDisplayed()
    }

    @Test
    fun extendedProductCard_secondaryCtaRendersAfterPrimary() {
        val element = MultimodalElement(
            id = "order-check",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/learn-more"
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
        val primaryLeft = composeTestRule.onNodeWithTag(CTA_BUTTON_TEST_TAG).getBoundsInRoot().left
        val secondaryLeft = composeTestRule.onNodeWithTag(SECONDARY_CTA_BUTTON_TEST_TAG).getBoundsInRoot().left
        assertTrue(
            "Expected the secondary CTA (left=$secondaryLeft) to render to the right of the primary CTA (left=$primaryLeft)",
            secondaryLeft > primaryLeft
        )
    }

    @Test
    fun extendedProductCard_showsSecondaryCtaOnly_whenPrimaryHasNoUrl() {
        val element = MultimodalElement(
            id = "secondary-only",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/learn-more"
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
        composeTestRule.onNodeWithText("Buy now").assertDoesNotExist()
        composeTestRule.onNodeWithText("Learn more").assertIsDisplayed()
    }

    @Test
    fun extendedProductCard_showsBothCtaButtons_whenSubtitlePresent() {
        val element = MultimodalElement(
            id = "parking-with-subtitle",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Practice Facility Garage",
                "productDescription" to "\$0.00 · Covered, Guaranteed spot",
                "primaryText" to "Book on ParkWhiz",
                "primaryUrl" to "https://widget.example.com/checkout",
                "secondaryText" to "Get directions",
                "secondaryUrl" to "https://maps.example.com/directions"
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
        composeTestRule.onNodeWithText("Book on ParkWhiz").assertExists()
        composeTestRule.onNodeWithText("Get directions").assertExists()
    }

    @Test
    fun extendedProductCard_hidesSecondaryCtaButton_whenSecondaryUrlIsBlank() {
        val element = MultimodalElement(
            id = "blank-secondary-url",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "   "
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
        composeTestRule.onNodeWithText("Buy now").assertIsDisplayed()
        composeTestRule.onNodeWithTag(SECONDARY_CTA_BUTTON_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText("Learn more").assertDoesNotExist()
    }

    @Test
    fun extendedProductCard_trimsWhitespace_fromCtaTextAndUrl() {
        var clicked: ProductActionButton? = null
        val element = MultimodalElement(
            id = "padded-cta",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "  Buy now  ",
                "primaryUrl" to "  https://example.com/checkout  "
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight),
                        onActionClick = { clicked = it }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Buy now").performClick()
        assertEquals("https://example.com/checkout", clicked?.url)
        assertEquals("Buy now", clicked?.text)
    }

    @Test
    fun extendedProductCard_secondaryCtaClick_firesActionClick_notCardClick() {
        var actionClicks = 0
        var cardClicks = 0
        var clicked: ProductActionButton? = null
        val element = MultimodalElement(
            id = "secondary-click-isolation",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/learn-more"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ExtendedProductCard(
                        element = element,
                        modifier = Modifier.height(cardMaxHeight),
                        onCardClick = { cardClicks++ },
                        onActionClick = {
                            actionClicks++
                            clicked = it
                        }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SECONDARY_CTA_BUTTON_TEST_TAG).performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, actionClicks)
        assertEquals(0, cardClicks)
        assertEquals("secondary-click-isolation_secondary", clicked?.id)
        assertEquals("Learn more", clicked?.text)
        assertEquals("https://example.com/learn-more", clicked?.url)
    }

    @Test
    fun extendedProductCard_singleCtaButton_keepsIntrinsicWidth_doesNotStretchFullWidth() {
        val element = MultimodalElement(
            id = "single-cta-width",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
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
        val bounds = composeTestRule.onNodeWithTag(CTA_BUTTON_TEST_TAG).getBoundsInRoot()
        val ctaWidth = bounds.right - bounds.left
        // Content area is 218dp (250dp card - 2x16dp padding); intrinsic "Buy now" stays well under.
        assertTrue(
            "Expected the lone CTA to keep its intrinsic width, but it measured $ctaWidth wide",
            ctaWidth < 150.dp
        )
    }

    @Test
    fun extendedProductCard_bothCtaButtons_shareRowWidthEqually() {
        val element = MultimodalElement(
            id = "both-ctas-width",
            url = "https://example.com/image.jpg",
            content = mapOf(
                "productName" to "Product Name",
                "productPrice" to "$63.97",
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/learn-more"
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
        val primaryBounds = composeTestRule.onNodeWithTag(CTA_BUTTON_TEST_TAG).getBoundsInRoot()
        val secondaryBounds = composeTestRule.onNodeWithTag(SECONDARY_CTA_BUTTON_TEST_TAG).getBoundsInRoot()
        val primaryWidth = primaryBounds.right - primaryBounds.left
        val secondaryWidth = secondaryBounds.right - secondaryBounds.left
        assertTrue(
            "Expected both CTAs to share the row width equally (primary=$primaryWidth, secondary=$secondaryWidth)",
            kotlin.math.abs(primaryWidth.value - secondaryWidth.value) < 2f
        )
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
