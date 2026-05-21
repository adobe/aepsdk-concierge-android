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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

private data class ComparisonCase(
    val number: Int,
    val title: String,
    val description: String,
    val element: MultimodalElement
)

// Builds a MultimodalElement that populates BOTH ProductCard fields (title, caption,
// primary/secondary CTA entries) AND ExtendedProductCard fields (productName, productDescription,
// productPrice, productWasPrice, productBadge) so each card renders the same logical product.
private fun makeComparisonElement(
    id: String,
    imageUrl: String? = "https://picsum.photos/id/${(id.toIntOrNull() ?: 1) * 10}/190/190",
    title: String? = "Product Name",
    description: String? = "Subtitle text goes here",
    price: String? = "\$399.99",
    wasPrice: String? = "\$599.99",
    badge: String? = "Sale",
    primaryCta: String? = "Buy now",
    secondaryCta: String? = "Learn more"
): MultimodalElement = MultimodalElement(
    id = id,
    url = imageUrl,
    title = title,
    caption = description,
    content = mutableMapOf<String, Any>().apply {
        title?.let { put("productName", it) }
        description?.let { put("productDescription", it) }
        price?.let { put("productPrice", it) }
        wasPrice?.let { put("productWasPrice", it) }
        badge?.let { put("productBadge", it) }
        primaryCta?.let {
            put("primaryText", it)
            put("primaryUrl", "#")
        }
        secondaryCta?.let {
            put("secondaryText", it)
            put("secondaryUrl", "#")
        }
    }
)

private val comparisonCases: List<ComparisonCase> = listOf(
    ComparisonCase(
        number = 1,
        title = "Full content",
        description = "All fields present: title, description, price, was-price, badge, two CTAs.",
        element = makeComparisonElement(id = "1")
    ),
    ComparisonCase(
        number = 2,
        title = "Missing description",
        description = "caption = null and content[\"productDescription\"] = null. Subtitle row collapses.",
        element = makeComparisonElement(id = "2", description = null)
    ),
    ComparisonCase(
        number = 3,
        title = "Missing price",
        description = "No price / was-price / badge. ExtendedProductCard's price block disappears; ProductCard unaffected.",
        element = makeComparisonElement(id = "3", price = null, wasPrice = null, badge = null)
    ),
    ComparisonCase(
        number = 4,
        title = "Long title + description (truncation)",
        description = "Triggers maxLines = 2 + ellipsis on both cards' title and subtitle.",
        element = makeComparisonElement(
            id = "4",
            title = "Product Name Goes Here Long Title Two Lines",
            description = "Subtitle text goes here to describe the product or campaign across two lines"
        )
    ),
    ComparisonCase(
        number = 5,
        title = "Price-only (no was-price, no badge)",
        description = "Verifies ExtendedProductCard keeps price at a fixed Y when was-price is absent (alpha = 0 slot).",
        element = makeComparisonElement(id = "5", wasPrice = null, badge = null)
    ),
    ComparisonCase(
        number = 6,
        title = "No image",
        description = "url = null. ExtendedProductCard skips AsyncImage; ProductCard still invokes ProductImage.",
        element = makeComparisonElement(id = "6", imageUrl = null)
    ),
    ComparisonCase(
        number = 7,
        title = "Single CTA only",
        description = "Primary CTA only, no secondary. Affects ProductCard's action-button row.",
        element = makeComparisonElement(id = "7", secondaryCta = null)
    )
)

@Composable
internal fun ProductCardComparisonScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "ProductCard vs ExtendedProductCard",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Text(
                text = "Same MultimodalElement rendered both ways across content variants.",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                color = Color(0xFFE0E0E0)
            )
        }
        items(comparisonCases.size) { index ->
            ComparisonCaseRow(case = comparisonCases[index])
            if (index < comparisonCases.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}

@Composable
private fun ComparisonCaseRow(case: ComparisonCase) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Case ${case.number} — ${case.title}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        Text(
            text = case.description,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ProductCard (ACTION_BUTTON)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555)
                )
                ProductCard(
                    element = case.element,
                    onImageClick = {},
                    onActionClick = {}
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "ExtendedProductCard (PRODUCT_DETAIL)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF555555)
                )
                ExtendedProductCard(
                    element = case.element,
                    onCardClick = {}
                )
            }
        }
    }
}

@Preview(
    name = "Compact (480dp)",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    widthDp = 480,
    heightDp = 2400
)
@Composable
internal fun ProductCardComparisonPreviewCompact() {
    CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
        ConciergeTheme(theme = ConciergeThemeLoader.default()) {
            ProductCardComparisonScreen()
        }
    }
}

@Preview(
    name = "Wide (720dp)",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5,
    widthDp = 720,
    heightDp = 2400
)
@Composable
internal fun ProductCardComparisonPreviewWide() {
    CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
        ConciergeTheme(theme = ConciergeThemeLoader.default()) {
            ProductCardComparisonScreen()
        }
    }
}
