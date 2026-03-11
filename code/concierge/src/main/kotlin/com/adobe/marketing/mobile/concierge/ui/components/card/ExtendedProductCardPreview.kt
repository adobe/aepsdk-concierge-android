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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

// Variants by title/description line count, all with price + was price
private val lineCountVariants = listOf(
    // 2 lines of title + 2 lines of description
    MultimodalElement(
        id = "v1",
        url = "https://picsum.photos/id/10/190/190",
        content = mapOf(
            "productName" to "Product Name Goes Here Long Title Two Lines",
            "productDescription" to "Subtitle text goes here to describe the product or campaign",
            "productPrice" to "\$399.99",
            "productWasPrice" to "\$599.99"
        )
    ),
    // 2 lines of title + 1 line of description
    MultimodalElement(
        id = "v2",
        url = "https://picsum.photos/id/20/190/190",
        content = mapOf(
            "productName" to "Product Name Goes Here Long Title Two Lines",
            "productDescription" to "Short subtitle text",
            "productPrice" to "\$399.99",
            "productWasPrice" to "\$599.99"
        )
    ),
    // 1 line of title + 2 lines of description
    MultimodalElement(
        id = "v3",
        url = "https://picsum.photos/id/30/190/190",
        content = mapOf(
            "productName" to "Product Name",
            "productDescription" to "Subtitle text goes here to describe the product or campaign",
            "productPrice" to "\$399.99",
            "productWasPrice" to "\$599.99"
        )
    ),
    // 1 line of title + 1 line of description
    MultimodalElement(
        id = "v4",
        url = "https://picsum.photos/id/40/190/190",
        content = mapOf(
            "productName" to "Product Name",
            "productDescription" to "Short subtitle text",
            "productPrice" to "\$399.99",
            "productWasPrice" to "\$599.99"
        )
    )
)

// Sample data covering the required content variations
private val sampleCards = listOf(
    // Title + subtitle + price + was price + badge
    MultimodalElement(
        id = "1",
        url = "https://picsum.photos/id/10/190/190",
        title = "Product Name Goes Here Long Title Two Lines",
        content = mapOf(
            "productName" to "Product Name Goes Here Long Title Two Lines",
            "productDescription" to "Subtitle text goes here to describe the product or campaign",
            "productPrice" to "\$399.99",
            "productWasPrice" to "\$599.99",
            "productBadge" to "Sale"
        )
    ),
    // Title + price only (no subtitle, no was price, no badge)
    MultimodalElement(
        id = "2",
        url = "https://picsum.photos/id/20/190/190",
        title = "Product Name Goes Here",
        content = mapOf(
            "productName" to "Product Name Goes Here",
            "productPrice" to "\$63.97"
        )
    ),
    // Title + subtitle + price + badge (no was price)
    MultimodalElement(
        id = "3",
        url = "https://picsum.photos/id/30/190/190",
        title = "Product Name Goes Here Long Title Two Lines",
        content = mapOf(
            "productName" to "Product Name Goes Here Long Title Two Lines",
            "productDescription" to "Subtitle text goes here to describe the product or campaign",
            "productPrice" to "\$54.99",
            "productBadge" to "New Arrival"
        )
    ),
    // Title + price + was price (no subtitle, no badge)
    MultimodalElement(
        id = "4",
        url = "https://picsum.photos/id/40/190/190",
        title = "Product Name Goes Here",
        content = mapOf(
            "productName" to "Product Name Goes Here",
            "productPrice" to "\$139.95",
            "productWasPrice" to "\$179.95"
        )
    ),
    // Title + subtitle + price (no was price, no badge)
    MultimodalElement(
        id = "5",
        url = "https://picsum.photos/id/50/190/190",
        title = "Product Name Goes Here Long Title Two Lines",
        content = mapOf(
            "productName" to "Product Name Goes Here Long Title Two Lines",
            "productDescription" to "Subtitle text goes here to describe the product or campaign",
            "productPrice" to "\$190.00"
        )
    ),
    // Title + price + was price + badge (no subtitle)
    MultimodalElement(
        id = "6",
        url = "https://picsum.photos/id/60/190/190",
        title = "Product Name Goes Here",
        content = mapOf(
            "productName" to "Product Name Goes Here",
            "productPrice" to "\$159.99",
            "productWasPrice" to "\$199.99",
            "productBadge" to "Extended Sizes"
        )
    ),
    // Ranged price (e.g. multiple sizes/colors)
    MultimodalElement(
        id = "7",
        url = "https://picsum.photos/id/70/190/190",
        title = "Product Name Goes Here",
        content = mapOf(
            "productName" to "Product Name Goes Here",
            "productDescription" to "Available in multiple options",
            "productPrice" to "\$19.99 – \$49.99"
        )
    ),
    // "See price in cart" variant
    MultimodalElement(
        id = "8",
        url = "https://picsum.photos/id/80/190/190",
        title = "Product Name Goes Here",
        content = mapOf(
            "productName" to "Product Name Goes Here",
            "productDescription" to "Subtitle text goes here",
            "productPrice" to "See price in cart",
            "productWasPrice" to "\$199.99",
        )
    )
)

/**
 * Demo screen that renders sample extended product cards with varied content.
 * Intended for use in the test app to validate card layout and spacing.
 */
@Composable
fun ExtendedProductCardDemoScreen() {
    CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
        ConciergeTheme(theme = ConciergeThemeLoader.default()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
            ) {
                item {
                    Text(
                        text = "Title & Description Variants",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "2L title + 2L desc  •  2L title + 1L desc  •  1L title + 2L desc  •  1L title + 1L desc",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(lineCountVariants.size) { index ->
                            ExtendedProductCard(element = lineCountVariants[index])
                        }
                    }
                }
                item {
                    Text(
                        text = "Content Variations",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333),
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Badge, subtitle, price, was price, ranged price, see price in cart",
                        fontSize = 13.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sampleCards.size) { index ->
                            ExtendedProductCard(element = sampleCards[index])
                        }
                    }
                }
            }
        }
    }
}
