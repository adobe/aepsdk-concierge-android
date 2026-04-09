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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeProductCardBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.ui.theme.ProductCardStyle
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the RecommendationCards composable.
 */
class RecommendationCardsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun recommendationCards_singleElement_displaysProductCard() {
        val elements = listOf(
            MultimodalElement(
                id = "prod-1",
                title = "Single Product",
                caption = "Description",
                url = "https://example.com/img.jpg"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    RecommendationCards(elements = elements)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Single Product").assertIsDisplayed()
        composeTestRule.onNodeWithText("Description").assertIsDisplayed()
    }

    @Test
    fun recommendationCards_multipleElements_displaysCarousel() {
        val elements = listOf(
            MultimodalElement(id = "p1", title = "First", url = "https://example.com/1.jpg"),
            MultimodalElement(id = "p2", title = "Second", url = "https://example.com/2.jpg")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    RecommendationCards(elements = elements)
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun recommendationCards_emptyList_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    RecommendationCards(elements = emptyList())
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun recommendationCards_singleExtendedProductCard_isCenteredHorizontally() {
        // ExtendedProductCard uses a fixed cardWidth that is narrower than the screen.
        // The Column's horizontalAlignment = CenterHorizontally should produce equal left/right
        // margins, verified by comparing the card's x offset to the container's remaining space.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(
                    productCard = ConciergeProductCardBehavior(cardStyle = ProductCardStyle.PRODUCT_DETAIL)
                )
            )
        )
        val elements = listOf(
            MultimodalElement(
                id = "ext-1",
                title = "Extended Product",
                url = "https://example.com/img.jpg"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    RecommendationCards(elements = elements)
                }
            }
        }

        // Capture the card node bounds after layout settles
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Extended Product").assertIsDisplayed()

        val cardBounds = composeTestRule
            .onNodeWithText("Extended Product")
            .fetchSemanticsNode()
            .boundsInRoot

        // The card title is inside the fixed-width card. Left and right margins from the
        // screen edges should be approximately equal (centered), allowing 2px tolerance.
        val leftMargin = cardBounds.left
        val rightMargin = composeTestRule.onRoot().fetchSemanticsNode().boundsInRoot.right - cardBounds.right
        assertEquals(
            "Extended product card should be horizontally centered",
            leftMargin, rightMargin, 2f
        )
    }

    @Test
    fun recommendationCards_onActionClick_triggersCallback() {
        var clickedButton: ProductActionButton? = null
        val elements = listOf(
            MultimodalElement(
                id = "prod-2",
                title = "Product",
                url = "https://example.com/img.jpg",
                content = mapOf(
                    "primaryText" to "Add to Cart",
                    "primaryUrl" to "https://example.com/cart"
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    RecommendationCards(
                        elements = elements,
                        onActionClick = { clickedButton = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Add to Cart").performClick()
        assert(clickedButton?.text == "Add to Cart")
    }
}
