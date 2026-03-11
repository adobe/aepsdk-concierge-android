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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.image.AsyncImage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Composable that displays a single product card containing a large image, badge,
 * product name, subtitle/description, and price.
 */
@Composable
internal fun ExtendedProductCard(
    element: MultimodalElement,
    modifier: Modifier = Modifier,
    onCardClick: (MultimodalElement) -> Unit = {}
) {
    val style = ConciergeStyles.extendedProductCardStyle
    val productName = element.content["productName"] as? String ?: element.title
    val productPrice = element.content["productPrice"] as? String
    val productWasPrice = element.content["productWasPrice"] as? String
    val productBadge = element.content["productBadge"] as? String
    val subtitle = element.content["productDescription"] as? String
        ?: element.content["description"] as? String
        ?: element.content["learningResource"] as? String
    val imageUrl = element.url ?: element.thumbnailUrl
    val imageWidth: Dp
    val imageHeight: Dp
    if (element.thumbnailWidth != null && element.thumbnailHeight != null) {
        // Use thumbnail dimensions as image size
        imageWidth = element.thumbnailWidth.dp
        imageHeight = element.thumbnailHeight.dp
    } else {
        imageWidth = style.imageWidth
        imageHeight = style.imageHeight
    }

    // Clickable card container (image + badge row + title/subtitle/price).
    Card(
        modifier = modifier
            .width(style.cardWidth)
            .height(style.cardHeight)
            .clip(style.cardShape)
            .then(
                if (style.cardOutlineColor != Color.Transparent) {
                    Modifier.border(1.dp, style.cardOutlineColor, style.cardShape)
                } else Modifier
            )
            .clickable { onCardClick(element) },
        shape = style.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = style.cardElevation),
        colors = CardDefaults.cardColors(containerColor = style.cardBackgroundColor)
    ) {
        // Main column: image, badge row, content.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(style.cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Image with badge overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(style.imageHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(style.imageHeight)
                        .clip(style.cardShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            url = imageUrl,
                            contentDescription = productName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(imageWidth)
                                .height(imageHeight)
                        )
                    }
                }

                if (!productBadge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-style.cardPadding))
                            .wrapContentWidth(unbounded = true)
                            .background(
                                color = style.badgeBackgroundColor,
                                shape = RectangleShape
                            )
                            .padding(
                                start = style.badgePaddingHorizontal,
                                end = style.badgePaddingHorizontal,
                                top = style.badgePaddingVertical,
                                bottom = style.badgePaddingVertical
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = productBadge.uppercase(),
                            color = style.badgeTextColor,
                            fontSize = style.badgeFontSize,
                            fontWeight = style.badgeFontWeight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Info block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .padding(
                        start = style.contentPadding,
                        end = style.contentPadding,
                        top = style.contentPaddingTop,
                        bottom = style.contentPaddingBottom
                    )
            ) {
                // Title, subtitle, and price column.
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Headline: title + subtitle
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(style.headlineGap)
                    ) {
                        if (!productName.isNullOrBlank()) {
                            Text(
                                text = productName,
                                color = style.titleColor,
                                fontSize = style.titleFontSize,
                                fontWeight = style.titleFontWeight,
                                lineHeight = style.titleLineHeight,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = style.subtitleColor,
                                fontSize = style.subtitleFontSize,
                                fontWeight = style.subtitleFontWeight,
                                lineHeight = style.subtitleLineHeight,
                                letterSpacing = style.subtitleLetterSpacing,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!productPrice.isNullOrBlank() || !productWasPrice.isNullOrBlank()) {
                        // Price block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(style.priceBlockHeight),
                            verticalArrangement = Arrangement.Top
                        ) {
                            if (!productPrice.isNullOrBlank()) {
                                Text(
                                    text = productPrice,
                                    color = style.priceColor,
                                    fontSize = style.priceFontSize,
                                    fontWeight = style.priceFontWeight,
                                    lineHeight = style.priceLineHeight,
                                    letterSpacing = style.priceLetterSpacing,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!productWasPrice.isNullOrBlank()) {
                                Text(
                                    text = style.wasPriceTextPrefix + productWasPrice,
                                    color = style.wasPriceColor,
                                    fontSize = style.wasPriceFontSize,
                                    fontWeight = style.wasPriceFontWeight,
                                    lineHeight = style.wasPriceLineHeight,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
