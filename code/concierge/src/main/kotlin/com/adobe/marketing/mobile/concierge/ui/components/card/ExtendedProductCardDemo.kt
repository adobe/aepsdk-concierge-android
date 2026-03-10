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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeProductCardBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ProductCardStyle
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

private const val DEMO_IMAGE_URL = "https://picsum.photos/190/190"
private const val DEMO_IMAGE_SIZE_DP = 190

/** Demo elements: title+price, title+price+subtitle+badge, title+badge+price, full. */
internal fun extendedProductCardDemoElements(): List<MultimodalElement> = listOf(
    MultimodalElement(
        id = "demo-1",
        title = "Brand Name Basketball Shoes",
        url = DEMO_IMAGE_URL,
        thumbnailWidth = DEMO_IMAGE_SIZE_DP,
        thumbnailHeight = DEMO_IMAGE_SIZE_DP,
        content = mapOf(
            "productName" to "Brand Name Basketball Shoes",
            "productPrice" to "$91.99",
            "productPageURL" to "https://example.com/product"
        )
    ),
    MultimodalElement(
        id = "demo-2",
        title = "Brand Name Volleyball Shoes",
        url = DEMO_IMAGE_URL,
        thumbnailWidth = DEMO_IMAGE_SIZE_DP,
        thumbnailHeight = DEMO_IMAGE_SIZE_DP,
        content = mapOf(
            "productName" to "Brand Name Volleyball Shoes",
            "productPrice" to "$113.97",
            "productDescription" to "Balanced cushioning and stability for everyday play.",
            "productBadge" to "EXTENDED SIZES",
            "productPageURL" to "https://example.com/product"
        )
    ),
    MultimodalElement(
        id = "demo-3",
        title = "Brand Name Casual Shoes",
        url = DEMO_IMAGE_URL,
        thumbnailWidth = DEMO_IMAGE_SIZE_DP,
        thumbnailHeight = DEMO_IMAGE_SIZE_DP,
        content = mapOf(
            "productName" to "Brand Name Casual Shoes",
            "productPrice" to "$129.99",
            "productBadge" to "Women-Owned Brand",
            "productPageURL" to "https://example.com/product"
        )
    ),
    MultimodalElement(
        id = "demo-4",
        title = "Brand Name Premium Basketball Shoes",
        url = DEMO_IMAGE_URL,
        thumbnailWidth = DEMO_IMAGE_SIZE_DP,
        thumbnailHeight = DEMO_IMAGE_SIZE_DP,
        content = mapOf(
            "productName" to "Brand Name Premium Basketball Shoes",
            "productPrice" to "$113.97",
            "productWasPrice" to "$154.99",
            "productDescription" to "Offers the best responsiveness and consistent court grip.",
            "productBadge" to "NEW",
            "productPageURL" to "https://example.com/product"
        )
    )
)

@Composable
internal fun ExtendedProductCardDemoCarousel(
    onCardClick: (MultimodalElement) -> Unit = {},
    modifier: Modifier = Modifier
) {
    RecommendationCards(
        elements = extendedProductCardDemoElements(),
        modifier = modifier,
        onImageClick = onCardClick,
        onActionClick = {}
    )
}

/** Public demo carousel. Use with theme productCard.cardStyle = "productDetail". */
@Composable
fun ConciergeDemoProductCardCarousel(
    onCardClick: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ExtendedProductCardDemoCarousel(
        onCardClick = { element -> onCardClick(element.content["productPageURL"] as? String) },
        modifier = modifier
    )
}

@Composable
fun ConciergeDemoProductCardCarouselContent(
    theme: ConciergeThemeData,
    onCardClick: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    ConciergeTheme(theme = theme) {
        CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
            ConciergeDemoProductCardCarousel(onCardClick = onCardClick, modifier = modifier)
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ExtendedProductCardDemoCarouselPreview() {
    val theme = ConciergeThemeData(
        config = ConciergeThemeConfig(),
        tokens = ConciergeThemeTokens(
            behavior = ConciergeThemeBehavior(
                productCard = ConciergeProductCardBehavior(cardStyle = ProductCardStyle.PRODUCT_DETAIL)
            )
        )
    )
    ConciergeTheme(theme = theme) {
        CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
            ExtendedProductCardDemoCarousel(modifier = Modifier.fillMaxWidth())
        }
    }
}
