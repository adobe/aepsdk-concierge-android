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

package com.adobe.marketing.mobile.conciergetestapp.cards

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.conciergetestapp.theme.ThemeValueParsing

/**
 * The out-of-the-box product card's look, resolved from the same public theme values the SDK uses
 * (`ConciergeThemeTokens.cssLayout`, i.e. the theme's `--product-card-*` / `--product-image-*`
 * keys) so a custom card sits next to the built-in carousel without a second set of knobs.
 *
 * A handful of things this card has and the built-in one does not — a rounded badge, the meta line
 * under the text, the placeholder behind a missing image — have no `--product-card-*` key, so they
 * read optional [SimpleCardKeys] overrides and otherwise derive from the brand palette. Nothing is
 * hard-coded to a colour the theme can't move.
 *
 * Theme values are read with [ThemeValueParsing], which accepts the same color notations as the
 * SDK; anything malformed falls back to the SDK palette.
 */
@Immutable
data class SimpleCardStyle(
    val cardShape: Shape,
    val cardBackgroundColor: Color,
    val cardOutlineColor: Color,
    val cardWidth: Dp,
    val cardMinHeight: Dp,
    val cardMaxHeight: Dp,
    val imageWidth: Dp,
    val imageHeight: Dp,
    val imageTopPadding: Dp,
    val imageShape: Shape,
    /** Flat fill behind an image that is missing or still loading. */
    val imagePlaceholderColor: Color,
    val badgeShape: Shape,
    /** Inset from the image's bottom-left corner, so a rounded badge doesn't collide with it. */
    val badgeInset: Dp,
    val badgeBackgroundColor: Color,
    val badgeTextColor: Color,
    val badgeFontSize: TextUnit,
    val badgeFontWeight: FontWeight,
    val badgePaddingHorizontal: Dp,
    val badgePaddingVertical: Dp,
    val titleColor: Color,
    val titleFontSize: TextUnit,
    val titleFontWeight: FontWeight,
    val titleLineHeight: TextUnit,
    val subtitleColor: Color,
    val subtitleFontSize: TextUnit,
    val subtitleFontWeight: FontWeight,
    val subtitleLineHeight: TextUnit,
    val subtitleLetterSpacing: TextUnit,
    val descriptionMaxLines: Int,
    /** The meta line above the buttons: wait time, pickup, tip-off. The price slot's styling. */
    val detailColor: Color,
    val detailFontSize: TextUnit,
    val detailFontWeight: FontWeight,
    val contentPadding: Dp,
    val contentPaddingTop: Dp,
    val contentPaddingBottom: Dp,
    val titleSubtitleSpacing: Dp,
    val sectionSpacing: Dp,
    val ctaShape: Shape,
    val ctaBackgroundColor: Color,
    val ctaTextColor: Color,
    val ctaHorizontalPadding: Dp,
    val ctaVerticalPadding: Dp,
    val ctaFontSize: TextUnit,
    val ctaFontWeight: FontWeight,
    val ctaTopSpacing: Dp,
    val ctaSpacing: Dp,
    /** The second button on a card: quieter than the first, so the primary action still leads. */
    val secondaryCtaBackgroundColor: Color,
    val secondaryCtaTextColor: Color,
    val secondaryCtaBorderColor: Color,
    val secondaryCtaBorderWidth: Dp,
    /** How far a card the fan can't act on is faded. */
    val disabledAlpha: Float,
    val carouselItemSpacing: Dp,
    val carouselTrailingPadding: Dp,
    val carouselVerticalPadding: Dp
)

/**
 * Theme keys for the few things the `--product-card-*` vocabulary has no word for. All optional:
 * each one falls back to a value derived from the brand palette.
 */
object SimpleCardKeys {
    /** Corner radius of the badge chip. `0px` restores the built-in card's square badge. */
    const val BADGE_BORDER_RADIUS = "--simple-card-badge-border-radius"

    /** How far the badge sits in from the image's bottom-left corner. */
    const val BADGE_INSET = "--simple-card-badge-inset"

    /** Corner radius of the image, so artwork can echo the card's own rounding. */
    const val IMAGE_BORDER_RADIUS = "--simple-card-image-border-radius"

    /** Fill behind an image that is missing or still loading. */
    const val IMAGE_PLACEHOLDER_COLOR = "--simple-card-image-placeholder-color"

    // The meta line above the buttons. Defaults to the product card's price styling.
    const val DETAIL_COLOR = "--simple-card-detail-color"
    const val DETAIL_FONT_SIZE = "--simple-card-detail-font-size"
    const val DETAIL_FONT_WEIGHT = "--simple-card-detail-font-weight"

    /** Opacity of a card the fan can't act on, `0`..`1`. */
    const val DISABLED_OPACITY = "--simple-card-disabled-opacity"
}

object SimpleCardTheme {

    val current: SimpleCardStyle
        @Composable get() {
            val tokens = ConciergeTheme.tokens
            val base = ConciergeTheme.colors
            return remember(tokens, base) { resolve(tokens?.cssLayout, base, tokens?.cssVariables.orEmpty()) }
        }

    /**
     * Every default here mirrors the SDK's own product card defaults, so an unthemed custom card
     * and an unthemed built-in card render identically.
     */
    fun resolve(
        layout: ConciergeLayout?,
        base: ConciergeColors,
        variables: Map<String, String> = emptyMap()
    ): SimpleCardStyle {
        fun color(value: String?, default: Color) = ThemeValueParsing.parseColor(value) ?: default
        fun custom(key: String, default: Color) = color(variables[key], default)
        fun customDp(key: String, default: Dp) = ThemeValueParsing.parseDp(variables[key])?.dp ?: default
        fun customFloat(key: String) = variables[key]?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() }
        // The spec's px line heights at the default font sizes (14/17 headline, 12/14 caption).
        val headlineLineHeightFactor = 17f / 14f
        val smallTextLineHeightFactor = 14f / 12f
        val titleSize = (layout?.productCardTitleFontSize ?: 14.0).toFloat().sp
        val subtitleSize = (layout?.productCardSubtitleFontSize ?: 12.0).toFloat().sp
        val badgeSize = (layout?.productCardBadgeFontSize ?: 12.0).toFloat().sp
        val ctaSize = (layout?.productCardCtaButtonFontSize ?: 12.0).toFloat().sp
        val detailSize = (ThemeValueParsing.parseDp(variables[SimpleCardKeys.DETAIL_FONT_SIZE]) ?: (layout?.productCardPriceFontSize ?: 14.0).toFloat()).sp
        val cardMinHeight = (layout?.productCardMinHeight ?: 240.0).toFloat().dp
        // A card is a brand surface before it is a product card, so the brand's own button colour
        // leads; the hard-coded orange only survives a theme that sets no button colour at all.
        val ctaBackground = base.productCardCtaButtonBackground
            ?: base.ctaButtonBackground
            ?: base.buttonPrimaryBackground
            ?: Color(0xFFBB5811)
        return SimpleCardStyle(
            cardShape = RoundedCornerShape((layout?.productCardBorderRadius ?: 8.0).toFloat().dp),
            cardBackgroundColor = color(layout?.productCardBackgroundColor, base.surface),
            cardOutlineColor = color(layout?.productCardOutlineColor, Color(0xFFE3E3E3)),
            cardWidth = (layout?.productCardWidth ?: 250.0).toFloat().dp,
            cardMinHeight = cardMinHeight,
            cardMaxHeight = (layout?.productCardMaxHeight ?: 360.0).toFloat().dp.coerceAtLeast(cardMinHeight),
            imageWidth = (layout?.productImageWidth ?: 190.0).toFloat().dp,
            imageHeight = (layout?.productImageHeight ?: 190.0).toFloat().dp,
            imageTopPadding = 16.dp,
            imageShape = RoundedCornerShape(customDp(SimpleCardKeys.IMAGE_BORDER_RADIUS, 0.dp)),
            // Not the card's own colour: a missing image should read as an empty slot, not as a
            // hole in the card.
            imagePlaceholderColor = custom(SimpleCardKeys.IMAGE_PLACEHOLDER_COLOR, base.secondary),
            badgeShape = RoundedCornerShape(customDp(SimpleCardKeys.BADGE_BORDER_RADIUS, 0.dp)),
            badgeInset = customDp(SimpleCardKeys.BADGE_INSET, 0.dp),
            badgeBackgroundColor = color(layout?.productCardBadgeBackgroundColor, base.primary),
            badgeTextColor = color(layout?.productCardBadgeTextColor, base.onPrimary),
            badgeFontSize = badgeSize,
            badgeFontWeight = cssFontWeight(layout?.productCardBadgeFontWeight ?: 700),
            badgePaddingHorizontal = 12.dp,
            badgePaddingVertical = 4.dp,
            titleColor = color(layout?.productCardTitleColor, base.conciergeMessageText ?: base.onSurface),
            titleFontSize = titleSize,
            titleFontWeight = cssFontWeight(layout?.productCardTitleFontWeight ?: 700),
            titleLineHeight = (titleSize.value * headlineLineHeightFactor).sp,
            subtitleColor = color(layout?.productCardSubtitleColor, Color(0xFF4F4F4F)),
            subtitleFontSize = subtitleSize,
            subtitleFontWeight = cssFontWeight(layout?.productCardSubtitleFontWeight ?: 400),
            subtitleLineHeight = (subtitleSize.value * smallTextLineHeightFactor).sp,
            subtitleLetterSpacing = (-0.5).sp,
            descriptionMaxLines = layout?.productCardDescriptionMaxLines?.takeIf { it > 0 } ?: 2,
            detailColor = custom(
                SimpleCardKeys.DETAIL_COLOR,
                color(layout?.productCardPriceColor, base.conciergeMessageText ?: base.onSurface)
            ),
            detailFontSize = detailSize,
            detailFontWeight = cssFontWeight(
                variables[SimpleCardKeys.DETAIL_FONT_WEIGHT]?.trim()?.toIntOrNull()
                    ?: layout?.productCardPriceFontWeight ?: 400
            ),
            contentPadding = (layout?.productCardTextHorizontalPadding ?: 16.0).toFloat().dp,
            contentPaddingTop = (layout?.productCardTextTopPadding ?: 24.0).toFloat().dp,
            contentPaddingBottom = (layout?.productCardTextBottomPadding ?: 16.0).toFloat().dp,
            titleSubtitleSpacing = (layout?.productCardTitleSubtitleSpacing ?: layout?.productCardTextSpacing ?: 8.0).toFloat().dp,
            sectionSpacing = (layout?.productCardSectionSpacing ?: layout?.productCardTextSpacing ?: 16.0).toFloat().dp,
            ctaShape = RoundedCornerShape((layout?.productCardCtaButtonBorderRadius ?: 40.0).toFloat().dp),
            ctaBackgroundColor = ctaBackground,
            ctaTextColor = base.productCardCtaButtonText ?: base.ctaButtonText ?: base.buttonPrimaryText ?: Color.White,
            ctaHorizontalPadding = (layout?.productCardCtaButtonHorizontalPadding ?: 16.0).toFloat().dp,
            ctaVerticalPadding = (layout?.productCardCtaButtonVerticalPadding ?: 8.0).toFloat().dp,
            ctaFontSize = ctaSize,
            ctaFontWeight = cssFontWeight(layout?.productCardCtaButtonFontWeight ?: 600),
            ctaTopSpacing = 16.dp,
            ctaSpacing = 8.dp,
            secondaryCtaBackgroundColor = base.productCardSecondaryCtaButtonBackground ?: Color.Transparent,
            secondaryCtaTextColor = base.productCardSecondaryCtaButtonText ?: base.onSurface,
            secondaryCtaBorderColor = base.productCardSecondaryCtaButtonBorder
                ?: color(layout?.productCardOutlineColor, base.outline),
            secondaryCtaBorderWidth = (layout?.productCardSecondaryCtaButtonBorderWidth ?: 1.0).toFloat().dp,
            disabledAlpha = customFloat(SimpleCardKeys.DISABLED_OPACITY)?.coerceIn(0f, 1f) ?: 0.45f,
            carouselItemSpacing = (layout?.productCardCarouselSpacing ?: 12.0).toFloat().dp,
            carouselTrailingPadding = (layout?.productCardCarouselHorizontalPadding ?: layout?.chatHistoryPadding ?: 4.0).toFloat().dp,
            carouselVerticalPadding = 8.dp
        )
    }
}

/** [FontWeight] throws outside 1..1000, so a theme value past either end is clamped to it. */
internal fun cssFontWeight(weight: Int): FontWeight = FontWeight(weight.coerceIn(1, 1000))
