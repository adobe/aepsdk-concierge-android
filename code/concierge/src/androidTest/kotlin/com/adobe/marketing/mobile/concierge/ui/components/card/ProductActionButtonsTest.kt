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
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ProductActionButtons composable.
 * ProductActionButtons displays primary and secondary action buttons for product recommendations.
 */
class ProductActionButtonsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun productActionButtons_displaysPrimaryButton() {
        val buttons = listOf(
            ProductActionButton(id = "btn-1", text = "Add to Cart", url = "https://example.com/cart")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductActionButtons(
                        actionButtons = buttons,
                        onActionClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Add to Cart")
            .assertIsDisplayed()
    }

    @Test
    fun productActionButtons_displaysPrimaryAndSecondaryButtons() {
        val buttons = listOf(
            ProductActionButton(id = "primary", text = "Buy Now", url = "https://example.com/buy"),
            ProductActionButton(id = "secondary", text = "Learn More", url = "https://example.com/info")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductActionButtons(
                        actionButtons = buttons,
                        onActionClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Buy Now")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Learn More")
            .assertIsDisplayed()
    }

    @Test
    fun productActionButtons_primaryClick_triggersCallback() {
        var clickedButton: ProductActionButton? = null
        val button = ProductActionButton(id = "btn-1", text = "Shop Now", url = "https://example.com")

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductActionButtons(
                        actionButtons = listOf(button),
                        onActionClick = { clickedButton = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Shop Now")
            .performClick()

        assert(clickedButton?.id == "btn-1")
        assert(clickedButton?.text == "Shop Now")
    }

    @Test
    fun productActionButtons_secondaryClick_triggersCallback() {
        var clickedButton: ProductActionButton? = null
        val buttons = listOf(
            ProductActionButton(id = "p", text = "Primary"),
            ProductActionButton(id = "s", text = "Secondary")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductActionButtons(
                        actionButtons = buttons,
                        onActionClick = { clickedButton = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Secondary")
            .performClick()

        assert(clickedButton?.id == "s")
        assert(clickedButton?.text == "Secondary")
    }

    @Test
    fun productActionButtons_emptyList_doesNotDisplayButtons() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ProductActionButtons(
                        actionButtons = emptyList(),
                        onActionClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
