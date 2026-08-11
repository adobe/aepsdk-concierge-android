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

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
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
}
