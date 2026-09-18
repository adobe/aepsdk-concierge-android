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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.image.AsyncImage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/** Test tag on the [ExtendedProductCard] primary CTA button so UI tests can target it unambiguously. */
internal const val CTA_BUTTON_TEST_TAG = "ExtendedProductCardCtaButton"

/** Test tag on the [ExtendedProductCard] secondary CTA button so UI tests can target it unambiguously. */
internal const val SECONDARY_CTA_BUTTON_TEST_TAG = "ExtendedProductCardSecondaryCtaButton"

/** Kept separate from the buttons' own padding so a theme with 0 padding doesn't collapse the gap. */
private val PRODUCT_DETAIL_CTA_ROW_SPACING = 8.dp

internal enum class ProductCardCtaRole { PRIMARY, SECONDARY }

internal data class ProductDetailCta(
    val button: ProductActionButton,
    val role: ProductCardCtaRole
)

/** Trims text/url so a padded value can't pass validation here and then fail to parse at tap time. */
internal fun productDetailCtas(element: MultimodalElement): List<ProductDetailCta> =
    listOfNotNull(
        validatedProductDetailCta(primaryActionButton(element), ProductCardCtaRole.PRIMARY),
        validatedProductDetailCta(secondaryActionButton(element), ProductCardCtaRole.SECONDARY)
    )

private fun validatedProductDetailCta(button: ProductActionButton?, role: ProductCardCtaRole): ProductDetailCta? {
    if (button == null) return null
    val text = button.text.trim()
    val url = button.url?.trim().orEmpty()
    if (text.isEmpty() || url.isEmpty()) return null
    return ProductDetailCta(button.copy(text = text, url = url), role)
}

/**
 * Composable that displays a single product card containing a fixed-size image, badge,
 * product name, subtitle/description, and price.
 *
 * The image is always rendered at a fixed [ExtendedProductCardStyle.imageWidth] x
 * [ExtendedProductCardStyle.imageHeight]; every other element renders only when present.
 * The card height grows with its content, clamped between [ExtendedProductCardStyle.cardMinHeight]
 * and [ExtendedProductCardStyle.cardMaxHeight]; content that exceeds the available height
 * scrolls internally.
 *
 * When placed in a carousel, the caller passes a fixed height via [modifier] so every card
 * shares the tallest card's height. [measureOnly] lets the carousel's measurement pass skip
 * the network image load (image height is fixed, so the image is not needed to measure height).
 */
@Composable
internal fun ExtendedProductCard(
    element: MultimodalElement,
    modifier: Modifier = Modifier,
    measureOnly: Boolean = false,
    onCardClick: (MultimodalElement) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {}
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
    val imageWidth = style.imageWidth
    val imageHeight = style.imageHeight

    Card(
        modifier = modifier
            .width(style.cardWidth)
            .heightIn(min = style.cardMinHeight, max = style.cardMaxHeight)
            .then(
                if (style.shadowElevation > 0.dp) {
                    Modifier.shadow(
                        elevation = style.shadowElevation,
                        shape = style.cardShape,
                        ambientColor = style.shadowColor,
                        spotColor = style.shadowColor
                    )
                } else Modifier
            )
            .clip(style.cardShape)
            .then(
                if (style.cardOutlineColor != Color.Transparent) {
                    Modifier.border(1.dp, style.cardOutlineColor, style.cardShape)
                } else Modifier
            )
            .clickable { onCardClick(element) },
        shape = style.cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = style.cardBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Image section: always a fixed imageWidth x imageHeight slot. Missing or
            // failed images fall back to AsyncImage's surface-colored placeholder.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = style.imageTopPadding)
                    .height(imageHeight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .height(imageHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (!measureOnly && imageUrl != null) {
                        AsyncImage(
                            url = imageUrl,
                            contentDescription = productName,
                            contentScale = style.imageContentScale,
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
                            text = productBadge,
                            color = style.badgeTextColor,
                            fontSize = style.badgeFontSize,
                            fontWeight = style.badgeFontWeight,
                            lineHeight = style.badgeLineHeight,
                            letterSpacing = style.badgeLetterSpacing,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = style.contentPadding,
                        end = style.contentPadding,
                        top = style.contentPaddingTop,
                        bottom = style.contentPaddingBottom
                    ),
                verticalArrangement = Arrangement.Top
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
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = style.titleSubtitleSpacing)
                    )
                }

                if (!productPrice.isNullOrBlank() || !productWasPrice.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = style.sectionSpacing),
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
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = style.priceSpacing)
                            )
                        }
                    }
                }

                // Independent of subtitle presence -- overflow scrolls (see Column above), not clips.
                val ctas = remember(element) { productDetailCtas(element) }
                if (ctas.isNotEmpty()) {
                    // A lone CTA keeps its intrinsic width; only 2+ CTAs share the row equally.
                    val shareRowWidth = ctas.size > 1
                    Row(
                        modifier = Modifier
                            .padding(top = ConciergeStyles.productCardCtaButtonStyle.containerTopSpacing)
                            .then(if (shareRowWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()),
                        horizontalArrangement = Arrangement.spacedBy(PRODUCT_DETAIL_CTA_ROW_SPACING)
                    ) {
                        ctas.forEach { cta ->
                            key(cta.role) {
                                ProductDetailCtaButton(
                                    cta = cta,
                                    modifier = if (shareRowWidth) Modifier.weight(1f) else Modifier.wrapContentWidth(),
                                    onClick = { onActionClick(cta.button) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Renders one CTA: filled for primary, outlined for secondary. */
@Composable
private fun ProductDetailCtaButton(
    cta: ProductDetailCta,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor: Color
    val textColor: Color
    val borderColor: Color?
    val borderWidth: Dp?
    val shape: Shape
    val horizontalPadding: Dp
    val verticalPadding: Dp
    val textStyle: TextStyle
    val testTag: String

    when (cta.role) {
        ProductCardCtaRole.PRIMARY -> {
            val style = ConciergeStyles.productCardCtaButtonStyle
            backgroundColor = style.backgroundColor
            textColor = style.textColor
            borderColor = null
            borderWidth = null
            shape = style.shape
            horizontalPadding = style.horizontalPadding
            verticalPadding = style.verticalPadding
            textStyle = style.textStyle
            testTag = CTA_BUTTON_TEST_TAG
        }
        ProductCardCtaRole.SECONDARY -> {
            val style = ConciergeStyles.productCardSecondaryCtaButtonStyle
            backgroundColor = style.backgroundColor
            textColor = style.textColor
            borderColor = style.borderColor
            borderWidth = style.borderWidth
            shape = style.shape
            horizontalPadding = style.horizontalPadding
            verticalPadding = style.verticalPadding
            textStyle = style.textStyle
            testTag = SECONDARY_CTA_BUTTON_TEST_TAG
        }
    }

    Card(
        modifier = modifier
            .testTag(testTag)
            .then(
                if (borderColor != null && borderWidth != null) {
                    Modifier.border(borderWidth, borderColor, shape)
                } else Modifier
            )
            .clickable(onClickLabel = cta.button.text, role = Role.Button, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cta.button.text,
                style = textStyle,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
