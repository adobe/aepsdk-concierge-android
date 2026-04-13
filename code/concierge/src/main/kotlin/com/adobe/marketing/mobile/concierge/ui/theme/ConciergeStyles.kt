/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Central styling configuration for all Concierge UI composables.
 * Organized by composable-level styles for consistency and maintainability.
 * Fully supports light and dark modes through MaterialTheme and ConciergeTheme.
 */
internal object ConciergeStyles {

    /**
     * Helper function to apply theme typography (font family and line height) to a TextStyle
     */
    @Composable
    private fun TextStyle.withThemeTypography(): TextStyle {
        val tokens = ConciergeTheme.tokens
        val typography = tokens?.typography
        
        if (typography == null) {
            return this
        }
        
        return this.copy(
            // Note: Font family would require loading custom fonts, which is not implemented yet
            // fontFamily = typography.fontFamily?.let { FontFamily(...) },
            lineHeight = typography.lineHeight?.let { (this.fontSize.value * it).sp } ?: this.lineHeight
        )
    }

    /**
     * Styling for the chat header component
     */
    @Immutable
    data class HeaderStyle(
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val titleStyle: TextStyle,
        val titleFontWeight: FontWeight,
        val titleColor: Color,
        val subtitleStyle: TextStyle,
        val subtitleColor: Color,
        val iconSize: Dp,
        val iconColor: Color,
        val dividerColor: Color,
        val dividerThickness: Dp
    )

    val headerStyle: HeaderStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val textColor = themeColors.onSurface
            val cssLayout = ConciergeTheme.tokens?.cssLayout

            val titleStyle = cssLayout?.headerTitleFontSize?.let { size ->
                MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp)
            } ?: MaterialTheme.typography.headlineSmall

            return HeaderStyle(
                horizontalPadding = 12.dp,
                verticalPadding = 8.dp,
                titleStyle = titleStyle,
                titleFontWeight = FontWeight.Bold,
                titleColor = textColor,
                subtitleStyle = MaterialTheme.typography.bodySmall,
                subtitleColor = textColor.copy(alpha = 0.8f),
                iconSize = 24.dp,
                iconColor = textColor,
                dividerColor = textColor.copy(alpha = 0.12f),
                dividerThickness = 0.5.dp
            )
        }

    /**
     * Styling for chat input panel
     */
    @Immutable
    data class InputPanelStyle(
        val outerShape: Shape,
        val innerShape: Shape,
        val outerPadding: Dp,
        val innerPadding: Dp,
        val backgroundColor: Color,
        val borderColor: Color?,
        val borderWidth: Dp,
        val focusBorderColor: Color?,
        val focusBorderWidth: Dp,
        val recordingBorderColors: List<Color>,
        val recordingBorderAnimationDuration: Int,
        val buttonSpacing: Dp,
        val placeholderText: String,
        val listeningPlaceholderText: String
    )

    val inputPanelStyle: InputPanelStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val themeText = ConciergeTheme.text
            val tokens = ConciergeTheme.tokens

            // Get border configuration from theme tokens
            val borderWidth = tokens?.cssLayout?.inputOutlineWidth?.dp ?: 0.dp
            val focusBorderWidth = tokens?.cssLayout?.inputFocusOutlineWidth?.dp ?: 2.dp
            val innerRadius = tokens?.cssLayout?.inputBorderRadius?.dp ?: 10.dp
            val outerRadius = innerRadius + 2.dp

            return InputPanelStyle(
                outerShape = RoundedCornerShape(outerRadius),
                innerShape = RoundedCornerShape(innerRadius),
                outerPadding = 2.dp,
                innerPadding = 4.dp,
                backgroundColor = themeColors.inputBackground ?: themeColors.container,
                borderColor = themeColors.inputOutline ?: themeColors.outline,
                borderWidth = borderWidth,
                focusBorderColor = themeColors.inputOutlineFocus ?: themeColors.primary,
                focusBorderWidth = focusBorderWidth,
                recordingBorderColors = listOf(
                    themeColors.inputOutlineFocus ?: themeColors.primary,
                    themeColors.surface,
                    themeColors.surface,
                    themeColors.inputOutlineFocus ?: themeColors.primary
                ),
                recordingBorderAnimationDuration = 1500,
                buttonSpacing = 8.dp,
                placeholderText = themeText?.inputPlaceholder ?: "How can I help",
                listeningPlaceholderText = "Listening..."
            )
        }

    /**
     * Styling for voice recording panel
     */
    @Immutable
    data class VoiceRecordingPanelStyle(
        val shape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val padding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val cancelIconColor: Color,
        val contentSpacing: Dp,
        val pulseAnimationDuration: Int,
        val textStyle: TextStyle,
        val textColor: Color,
        val listeningText: String
    )

    val voiceRecordingPanelStyle: VoiceRecordingPanelStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return VoiceRecordingPanelStyle(
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                backgroundColor = themeColors.surface,
                padding = 16.dp,
                iconSize = 24.dp,
                iconColor = themeColors.primary,
                cancelIconColor = themeColors.onSurface,
                contentSpacing = 12.dp,
                pulseAnimationDuration = 1000,
                textStyle = MaterialTheme.typography.bodyLarge,
                textColor = themeColors.onSurface,
                listeningText = "Listening"
            )
        }

    /**
     * Styling for chat message bubbles
     */
    @Immutable
    data class MessageBubbleStyle(
        val padding: Dp,
        val innerPadding: Dp,
        val shape: Shape,
        val userMessageShape: Shape,
        val elevation: Dp,
        val userMessageBackgroundColor: Color,
        val botMessageBackgroundColor: Color,
        val userMessageTextColor: Color,
        val botMessageTextColor: Color,
        val textStyle: TextStyle,
        val contentSpacing: Dp,
        val segmentSpacing: Dp,
        val agentIconSize: Dp,
        val agentIconSpacing: Dp
    )

    val messageBubbleStyle: MessageBubbleStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val cssLayout = ConciergeTheme.tokens?.cssLayout
            val cornerRadius = cssLayout?.messageBorderRadius?.dp ?: 12.dp
            val defaultShape = RoundedCornerShape(cornerRadius)
            val userMessageShape = when (ConciergeTheme.behavior?.chat?.userMessageBubbleStyle) {
                UserMessageBubbleStyle.BALLOON -> RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius,
                    bottomStart = cornerRadius,
                    bottomEnd = 0.dp
                )
                else -> defaultShape
            }
            
            return MessageBubbleStyle(
                padding = 8.dp,
                innerPadding = 16.dp,
                shape = defaultShape,
                userMessageShape = userMessageShape,
                elevation = 0.dp,
                userMessageBackgroundColor = themeColors.userMessageBackground ?: themeColors.primary,
                botMessageBackgroundColor = themeColors.conciergeMessageBackground ?: themeColors.container,
                userMessageTextColor = themeColors.userMessageText ?: themeColors.onPrimary,
                botMessageTextColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                textStyle = MaterialTheme.typography.bodyLarge.withThemeTypography(),
                contentSpacing = 12.dp,
                segmentSpacing = 4.dp,
                agentIconSize = cssLayout?.agentIconSize?.dp ?: 39.dp,
                agentIconSpacing = cssLayout?.agentIconSpacing?.dp ?: 12.dp
            )
        }

    /**
     * Styling for circular citation badges
     */
    @Immutable
    data class CitationBadgeStyle(
        val backgroundColor: Color,
        val textColor: Color,
        val shape: Shape,
        val size: Dp
    )

    val citationBadgeStyle: CitationBadgeStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return CitationBadgeStyle(
                backgroundColor = themeColors.citationBackground ?: themeColors.outline,
                textColor = themeColors.citationText ?: themeColors.onSurface,
                shape = CircleShape,
                size = 18.dp
            )
        }

    /**
     * Styling for thinking animation
     */
    @Immutable
    data class ThinkingAnimationStyle(
        val dotSize: Dp,
        val dotSpacing: Dp,
        val textDotSpacing: Dp,
        val dotAnimationDuration: Int,
        val dotAnimationDelay: Int,
        val textStyle: TextStyle,
        val textColor: Color,
        val dotColor: Color,
        val thinkingText: String,
        val bubbleShape: Shape,
        val bubblePadding: PaddingValues,
        val dotVerticalAlignment: Alignment.Vertical
    )

    val thinkingAnimationStyle: ThinkingAnimationStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val themeText = ConciergeTheme.text
            val cssLayout = ConciergeTheme.tokens?.cssLayout
            val bubbleBorderRadius = cssLayout?.thinkingBubbleBorderRadius?.dp ?: 8.dp
            val bubblePaddingH = cssLayout?.thinkingBubblePaddingHorizontal?.dp ?: 16.dp
            val bubblePaddingV = cssLayout?.thinkingBubblePaddingVertical?.dp ?: 8.dp
            val dotVerticalAlignment = when (
                cssLayout?.thinkingDotVerticalAlignment?.let { ThinkingDotVerticalAlignment.fromString(it) }
            ) {
                ThinkingDotVerticalAlignment.TOP -> Alignment.Top
                ThinkingDotVerticalAlignment.BOTTOM -> Alignment.Bottom
                else -> Alignment.CenterVertically
            }
            return ThinkingAnimationStyle(
                dotSize = cssLayout?.thinkingDotSize?.dp ?: 8.dp,
                dotSpacing = cssLayout?.thinkingDotSpacing?.dp ?: 8.dp,
                textDotSpacing = 8.dp,
                dotAnimationDuration = 600,
                dotAnimationDelay = 200,
                textStyle = MaterialTheme.typography.bodyLarge,
                textColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                dotColor = themeColors.thinkingDotColor
                    ?: themeColors.primary.copy(alpha = 0.7f),
                thinkingText = themeText?.loadingMessage ?: "Thinking",
                bubbleShape = RoundedCornerShape(bubbleBorderRadius),
                bubblePadding = PaddingValues(horizontal = bubblePaddingH, vertical = bubblePaddingV),
                dotVerticalAlignment = dotVerticalAlignment
            )
        }

    /**
     * Styling for product cards
     */
    @Immutable
    data class ProductCardStyle(
        val shape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val imageHeight: Dp,
        val titleStyle: TextStyle,
        val titleFontWeight: FontWeight,
        val titleColor: Color,
        val titleMaxLines: Int,
        val captionStyle: TextStyle,
        val captionColor: Color,
        val captionTopPadding: Dp,
        val captionBottomPadding: Dp,
        val textTopPadding: Dp,
        val fallbackGradientColors: List<Color>
    )

    val productCardStyle: ProductCardStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductCardStyle(
                shape = RoundedCornerShape(0.dp),
                elevation = 1.dp,
                backgroundColor = themeColors.container,
                imageHeight = 250.dp,
                titleStyle = MaterialTheme.typography.bodyLarge,
                titleFontWeight = FontWeight.Bold,
                titleColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                titleMaxLines = 2,
                captionStyle = MaterialTheme.typography.bodyLarge,
                captionColor = themeColors.conciergeMessageText?.copy(alpha = 0.9f) ?: themeColors.onSurface.copy(alpha = 0.9f),
                captionTopPadding = 12.dp,
                captionBottomPadding = 16.dp,
                textTopPadding = 16.dp,
                fallbackGradientColors = listOf(
                    themeColors.primary.copy(alpha = 0.8f),
                    themeColors.primary.copy(alpha = 0.6f)
                )
            )
        }

    /**
     * Styling for product images (used in carousel and single cards)
     */
    @Immutable
    data class ProductImageStyle(
        val singleImageShape: Shape,
        val multiImageShape: Shape,
        val elevation: Dp,
        val backgroundColor: Color,
        val overlayBackgroundColor: Color,
        val overlayShape: Shape,
        val overlayPadding: Dp,
        val overlayInnerPadding: Dp,
        val overlayTextColor: Color,
        val overlayTextSize: Dp,
        val overlayTextFontWeight: FontWeight,
        val overlayTextStyle: TextStyle
    )

    val productImageStyle: ProductImageStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductImageStyle(
                singleImageShape = RoundedCornerShape(0.dp),
                multiImageShape = RoundedCornerShape(16.dp),
                elevation = 0.dp,
                backgroundColor = themeColors.surface,
                overlayBackgroundColor = themeColors.surface.copy(alpha = 0.95f),
                overlayShape = RoundedCornerShape(8.dp),
                overlayPadding = 8.dp,
                overlayInnerPadding = 8.dp,
                overlayTextColor = themeColors.onSurface,
                overlayTextSize = 16.dp,
                overlayTextFontWeight = FontWeight.Medium,
                overlayTextStyle = MaterialTheme.typography.bodyMedium
            )
        }

    /**
     * Styling for product carousel
     */
    @Immutable
    data class ProductCarouselStyle(
        val itemSpacing: Dp,
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val imageWidth: Dp,
        val imageHeight: Dp,
        val indicatorSize: Dp,
        val indicatorSpacing: Dp,
        val indicatorActiveColor: Color,
        val indicatorInactiveColor: Color,
        val indicatorInactiveAlpha: Float,
        val navigationIconActiveColor: Color,
        val navigationIconInactiveColor: Color,
        val navigationIconInactiveAlpha: Float,
        val navigationSpacing: Dp
    )

    val productCarouselStyle: ProductCarouselStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val layout = ConciergeTheme.tokens?.cssLayout
            val carouselHorizontalPadding = (
                layout?.productCardCarouselHorizontalPadding
                    ?: layout?.chatHistoryPadding
                    ?: 4.0
            ).toFloat().dp
            val carouselItemSpacing = (layout?.productCardCarouselSpacing ?: 12.0).toFloat().dp
            return ProductCarouselStyle(
                itemSpacing = carouselItemSpacing,
                horizontalPadding = carouselHorizontalPadding,
                verticalPadding = 8.dp,
                imageWidth = 200.dp,
                imageHeight = 150.dp,
                indicatorSize = 8.dp,
                indicatorSpacing = 8.dp,
                indicatorActiveColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                indicatorInactiveColor = themeColors.conciergeMessageText?.copy(alpha = 0.3f) ?: themeColors.onSurface.copy(alpha = 0.3f),
                indicatorInactiveAlpha = 0.3f,
                navigationIconActiveColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                navigationIconInactiveColor = themeColors.conciergeMessageText?.copy(alpha = 0.3f) ?: themeColors.onSurface.copy(alpha = 0.3f),
                navigationIconInactiveAlpha = 0.3f,
                navigationSpacing = 8.dp
            )
        }

    /**
     * Styling for extended product cards (carousel cards with image, badge, name, subtitle, price).
     */
    @Immutable
    data class ExtendedProductCardStyle(
        val cardShape: Shape,
        val cardBackgroundColor: Color,
        val cardOutlineColor: Color,
        val cardWidth: Dp,
        val cardHeight: Dp,
        val cardElevation: Dp,
        val imageWidth: Dp,
        val imageHeight: Dp,
        val imageTopPadding: Dp,
        val contentPaddingTop: Dp,
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
        val priceColor: Color,
        val priceFontSize: TextUnit,
        val priceFontWeight: FontWeight,
        val priceLineHeight: TextUnit,
        val priceLetterSpacing: TextUnit,
        val wasPriceFontSize: TextUnit,
        val wasPriceFontWeight: FontWeight,
        val wasPriceLineHeight: TextUnit,
        val wasPriceColor: Color,
        val wasPriceTextPrefix: String,
        val contentPadding: Dp,
        val contentPaddingBottom: Dp,
        val headlineGap: Dp
    )

    val extendedProductCardStyle: ExtendedProductCardStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val layout = ConciergeTheme.tokens?.cssLayout
            fun parseColor(hex: String?, default: Color): Color =
                if (!hex.isNullOrBlank()) try {
                    CSSValueConverter.parseColor(hex)
                } catch (_: Exception) {
                    default
                } else default
            // When no theme JSON colors are set, use device light/dark scheme
            val cardBg = parseColor(layout?.productCardBackgroundColor, themeColors.surface)
            val titleColor = parseColor(layout?.productCardTitleColor, themeColors.conciergeMessageText ?: themeColors.onSurface)
            val subtitleColor = parseColor(layout?.productCardSubtitleColor, Color(0xFF4F4F4F))
            val priceColor = parseColor(layout?.productCardPriceColor, themeColors.conciergeMessageText ?: themeColors.onSurface)
            val badgeBg = parseColor(layout?.productCardBadgeBackgroundColor, themeColors.primary)
            val badgeText = parseColor(layout?.productCardBadgeTextColor, themeColors.onPrimary)
            val titleSize = (layout?.productCardTitleFontSize ?: 14.0).toFloat().sp
            val subtitleSize = (layout?.productCardSubtitleFontSize ?: 12.0).toFloat().sp
            val priceSize = (layout?.productCardPriceFontSize ?: 14.0).toFloat().sp
            val badgeSize = (layout?.productCardBadgeFontSize ?: 12.0).toFloat().sp
            val badgeWeight = FontWeight(layout?.productCardBadgeFontWeight ?: 700)
            val titleWeight = FontWeight(layout?.productCardTitleFontWeight ?: 700)
            val subtitleWeight = FontWeight(layout?.productCardSubtitleFontWeight ?: 400)
            val priceWeight = FontWeight(layout?.productCardPriceFontWeight ?: 400)
            val cardBorderRadius = (ConciergeTheme.tokens?.cssLayout?.productCardBorderRadius ?: 8.0).toFloat().dp
            val outlineColor = parseColor(layout?.productCardOutlineColor, Color(0xFFE3E3E3))
            val cardWidthDp = (layout?.productCardWidth ?: 222.0).toFloat().dp
            val cardHeightDp = (maxOf(layout?.productCardHeight ?: 359.0, 359.0)).toFloat().dp
            val wasPriceColor = parseColor(layout?.productCardWasPriceColor, Color(0xFF4F4F4F))
            val wasPriceWeight = FontWeight(layout?.productCardWasPriceFontWeight ?: 400)
            val wasPriceTextPrefix = layout?.productCardWasPriceTextPrefix ?: "was "
            return ExtendedProductCardStyle(
                cardShape = RoundedCornerShape(cardBorderRadius),
                cardBackgroundColor = cardBg,
                cardOutlineColor = outlineColor,
                cardWidth = cardWidthDp,
                cardHeight = cardHeightDp,
                cardElevation = 1.dp,
                imageWidth = 190.dp,
                imageHeight = 190.dp,
                imageTopPadding = 16.dp,
                badgeBackgroundColor = badgeBg,
                badgeTextColor = badgeText,
                badgeFontSize = badgeSize,
                badgeFontWeight = badgeWeight,
                badgePaddingHorizontal = 12.dp,
                badgePaddingVertical = 4.dp,
                titleColor = titleColor,
                titleFontSize = titleSize,
                titleFontWeight = titleWeight,
                titleLineHeight = 17.sp,
                subtitleColor = subtitleColor,
                subtitleFontSize = subtitleSize,
                subtitleFontWeight = subtitleWeight,
                subtitleLineHeight = 14.sp,
                subtitleLetterSpacing = (-0.5).sp,
                priceColor = priceColor,
                priceFontSize = priceSize,
                priceFontWeight = priceWeight,
                priceLineHeight = 17.sp,
                priceLetterSpacing = (-0.5).sp,
                wasPriceFontSize = (layout?.productCardWasPriceFontSize ?: 12.0).toFloat().sp,
                wasPriceFontWeight = wasPriceWeight,
                wasPriceLineHeight = 14.sp,
                wasPriceColor = wasPriceColor,
                wasPriceTextPrefix = wasPriceTextPrefix,
                contentPadding = (layout?.productCardTextHorizontalPadding ?: 16.0).toFloat().dp,
                contentPaddingTop = (layout?.productCardTextTopPadding ?: 24.0).toFloat().dp,
                contentPaddingBottom = (layout?.productCardTextBottomPadding ?: 16.0).toFloat().dp,
                headlineGap = (layout?.productCardTextSpacing ?: 8.0).toFloat().dp
            )
        }

    /**
     * Styling for product action buttons
     */
    @Immutable
    data class ProductActionButtonsStyle(
        val height: Dp,
        val shape: Shape,
        val spacing: Dp,
        val primaryBackgroundColor: Color,
        val primaryContentColor: Color,
        val secondaryBackgroundColor: Color,
        val secondaryContentColor: Color,
        val secondaryBorderWidth: Dp,
        val secondaryBorderColor: Color,
        val secondaryBorderAlpha: Float,
        val textStyle: TextStyle,
        val textAlign: TextAlign,
        val fontSize: Dp,
        val fontWeight: FontWeight,
        val maxLines: Int,
        val overflow: TextOverflow
    )

    val productActionButtonsStyle: ProductActionButtonsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ProductActionButtonsStyle(
                height = 40.dp,
                shape = RoundedCornerShape(20.dp),
                spacing = 8.dp,
                primaryBackgroundColor = themeColors.buttonPrimaryBackground ?: themeColors.primary,
                primaryContentColor = themeColors.buttonPrimaryText ?: themeColors.onPrimary,
                secondaryBackgroundColor = themeColors.surface,
                secondaryContentColor = themeColors.buttonSecondaryText ?: themeColors.onSurface,
                secondaryBorderWidth = 1.dp,
                secondaryBorderColor = themeColors.buttonSecondaryBorder ?: themeColors.outline.copy(alpha = 0.5f),
                secondaryBorderAlpha = 0.5f,
                textStyle = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                fontSize = 12.dp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Visible
            )
        }

    /**
     * Styling for prompt suggestions
     */
    @Immutable
    data class PromptSuggestionsStyle(
        val containerTopPadding: Dp,
        val containerStartPadding: Dp,
        val containerEndPadding: Dp,
        val itemSpacing: Dp,
        val itemShape: Shape,
        val itemBackgroundColor: Color,
        val itemHorizontalPadding: Dp,
        val itemVerticalPadding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val iconSpacing: Dp,
        val textStyle: TextStyle,
        val textColor: Color,
        val textMaxLines: Int,
        val showHeader: Boolean,
        val headerText: String,
        val headerStyle: TextStyle,
        val headerColor: Color,
        val headerBottomPadding: Dp
    )

    val promptSuggestionsStyle: PromptSuggestionsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val contentColor = themeColors.conciergeMessageText ?: themeColors.onSurface
            val textColor = themeColors.suggestionText ?: contentColor
            val behavior = ConciergeTheme.behavior?.promptSuggestions
            return PromptSuggestionsStyle(
                containerTopPadding = 6.dp,
                containerStartPadding = 12.dp,
                containerEndPadding = 48.dp,
                itemSpacing = 8.dp,
                itemShape = RoundedCornerShape(ConciergeTheme.tokens?.cssLayout?.suggestionItemBorderRadius?.dp ?: 10.dp),
                itemBackgroundColor = themeColors.suggestionBackground ?: themeColors.container,
                itemHorizontalPadding = 16.dp,
                itemVerticalPadding = 12.dp,
                iconSize = 10.dp,
                iconColor = textColor,
                iconSpacing = 12.dp,
                textStyle = MaterialTheme.typography.bodyMedium,
                textColor = textColor,
                textMaxLines = behavior?.itemMaxLines ?: 1,
                showHeader = behavior?.showHeader ?: false,
                headerText = ConciergeTheme.text?.suggestionsHeader ?: "Suggestions",
                headerStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                headerColor = contentColor,
                headerBottomPadding = 4.dp
            )
        }

    /**
     * Styling for CTA button
     */
    @Immutable
    data class CtaButtonStyle(
        val containerTopPadding: Dp,
        val containerStartPadding: Dp,
        val shape: Shape,
        val backgroundColor: Color,
        val horizontalPadding: Dp,
        val verticalPadding: Dp,
        val iconSize: Dp,
        val iconColor: Color,
        val iconSpacing: Dp,
        val textStyle: TextStyle,
        val textColor: Color
    )

    val ctaButtonStyle: CtaButtonStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val ctaLayout = ConciergeTheme.tokens?.cssLayout
            val borderRadius = ctaLayout?.ctaButtonBorderRadius?.dp ?: 99.dp
            val fontWeight = ctaLayout?.ctaButtonFontWeight?.let { FontWeight(it) } ?: FontWeight.Normal
            val fontSize = ctaLayout?.ctaButtonFontSize?.sp ?: 14.sp
            return CtaButtonStyle(
                containerTopPadding = 6.dp,
                containerStartPadding = 12.dp,
                shape = RoundedCornerShape(borderRadius),
                backgroundColor = themeColors.ctaButtonBackground ?: Color(0xFFEDEDED),
                horizontalPadding = ctaLayout?.ctaButtonHorizontalPadding?.dp ?: 16.dp,
                verticalPadding = ctaLayout?.ctaButtonVerticalPadding?.dp ?: 12.dp,
                iconSize = ctaLayout?.ctaButtonIconSize?.dp ?: 16.dp,
                iconColor = themeColors.ctaButtonIcon ?: Color(0xFF161313),
                iconSpacing = 4.dp,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize,
                    fontWeight = fontWeight
                ),
                textColor = themeColors.ctaButtonText ?: Color(0xFF191F1C)
            )
        }

    /**
     * Styling for citation items
     */
    @Immutable
    data class CitationStyle(
        val containerPadding: Dp,
        val separatorHeight: Dp,
        val separatorColor: Color,
        val backgroundColor: Color? = null,
        val textStyle: TextStyle,
        val textColor: Color,
        val textLength: Int,
        val urlColor: Color,
        val expandAnimationDuration: Int,
        val collapseAnimationDuration: Int,
        val fontSize: TextUnit? = null
    )

    val citationStyle: CitationStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val themeTypography = ConciergeTheme.typography
            val fontSize = themeTypography?.citationsFontSize?.sp
            return CitationStyle(
                containerPadding = 8.dp,
                separatorHeight = 1.dp,
                separatorColor = themeColors.outline.copy(alpha = 0.3f),
                backgroundColor = themeColors.citationBackground,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = fontSize ?: MaterialTheme.typography.bodyMedium.fontSize
                ),
                textColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                textLength = 1,
                urlColor = themeColors.messageConciergeLink ?: themeColors.onSurface,
                expandAnimationDuration = 200,
                collapseAnimationDuration = 200,
                fontSize = fontSize
            )
        }

    /**
     * Styling for chat footer (contains citations and feedback)
     */
    @Immutable
    data class ChatFooterStyle(
        val sourcesButtonPadding: Dp,
        val textStyle: TextStyle,
        val textColor: Color,
        val iconColor: Color,
        val iconSpacing: Dp,
        val sourcesText: String
    )

    val chatFooterStyle: ChatFooterStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ChatFooterStyle(
                sourcesButtonPadding = 0.dp,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                textColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                iconColor = themeColors.conciergeMessageText ?: themeColors.onSurface,
                iconSpacing = 4.dp,
                sourcesText = ConciergeTheme.text?.sourcesLabel ?: "Sources"
            )
        }

    /**
     * Styling for the disclaimer text and link
     */
    @Immutable
    data class DisclaimerStyle(
        val textStyle: TextStyle,
        val textColor: Color,
        val linkTextDecoration: TextDecoration,
        val padding: Dp
    )

    val disclaimerStyle: DisclaimerStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val themeTypography = ConciergeTheme.typography
            val bodySmall = MaterialTheme.typography.bodySmall
            val fontSize = themeTypography?.disclaimerFontSize?.sp ?: bodySmall.fontSize
            val fontWeight = themeTypography?.disclaimerFontWeight?.let { w ->
                FontWeight(w.coerceIn(100, 900))
            } ?: bodySmall.fontWeight
            return DisclaimerStyle(
                textStyle = bodySmall.copy(
                    fontSize = fontSize,
                    fontWeight = fontWeight
                ),
                textColor = themeColors.disclaimerColor ?: themeColors.onSurfaceVariant,
                linkTextDecoration = TextDecoration.Underline,
                padding = 8.dp
            )
        }

    /**
     * Styling for feedback buttons (thumbs up/down)
     */
    @Immutable
    data class FeedbackButtonsStyle(
        val buttonSize: Dp,
        val iconSize: Dp,
        val spacing: Dp,
        val iconColor: Color,
        val backgroundColor: Color = Color.Transparent,
        val hoverBackgroundColor: Color? = null,
        val helpfulLabelText: String,
        val helpfulLabelStyle: TextStyle,
        val helpfulLabelColor: Color
    )

    val feedbackButtonsStyle: FeedbackButtonsStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val textColor = themeColors.conciergeMessageText ?: themeColors.onSurface
            return FeedbackButtonsStyle(
                buttonSize = 32.dp,
                iconSize = 16.dp,
                spacing = 4.dp,
                iconColor = textColor,
                backgroundColor = themeColors.feedbackIconButtonBackground ?: Color.Transparent,
                hoverBackgroundColor = themeColors.feedbackIconButtonHoverBackground,
                helpfulLabelText = ConciergeTheme.text?.feedbackHelpfulLabel ?: "Was this helpful?",
                helpfulLabelStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                helpfulLabelColor = textColor
            )
        }

    /**
     * Styling for error overlay
     */
    @Immutable
    data class ErrorOverlayStyle(
        val padding: Dp,
        val backgroundColor: Color,
        val contentPadding: Dp,
        val messageTextStyle: TextStyle,
        val messageTextColor: Color,
        val dismissTextStyle: TextStyle,
        val dismissTextColor: Color,
        val dismissStartPadding: Dp
    )

    /**
     * Styling for microphone button
     */
    @Immutable
    data class MicButtonStyle(
        val size: Dp,
        val iconColor: Color,
        val recordingIconColor: Color,
        val pulsingBackgroundColor: Color,
        val pulsingBackgroundAlpha: Float,
        val pulseAnimationDuration: Int,
        val pulseScaleRange: Pair<Float, Float>,
        val ringAlpha: Float
    )

    val micButtonStyle: MicButtonStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val micColor = themeColors.primary
            val micIconColor = themeColors.micIconColor ?: micColor
            return MicButtonStyle(
                size = 24.dp,
                iconColor = micIconColor,
                recordingIconColor = themeColors.micRecordingIconColor ?: themeColors.onPrimary,
                pulsingBackgroundColor = micColor,
                pulsingBackgroundAlpha = 0.25f,
                pulseAnimationDuration = 1000,
                pulseScaleRange = 1.5f to 2.0f,
                ringAlpha = 0.30f,
            )
        }

    /**
     * Styling for send button
     */
    @Immutable
    data class SendButtonStyle(
        val size: Dp,
        val enabledIconColor: Color,
        val arrowCircleColor: Color,
        val arrowIconColor: Color,
        val disabledIconAlpha: Float,
        val useArrowStyle: Boolean
    )

    val sendButtonStyle: SendButtonStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val sendButtonStyleName = ConciergeTheme.behavior?.sendButtonStyle ?: "default"
            return SendButtonStyle(
                size = 24.dp,
                enabledIconColor = themeColors.sendIconColor ?: themeColors.onSurface,
                arrowCircleColor = themeColors.sendArrowBackgroundColor ?: themeColors.sendIconColor ?: themeColors.primary,
                arrowIconColor = themeColors.sendArrowIconColor ?: themeColors.onPrimary,
                disabledIconAlpha = 0.3f,
                useArrowStyle = sendButtonStyleName == "arrow"
            )
        }

    /**
     * Styling for message list
     */
    @Immutable
    data class MessageListStyle(
        val verticalSpacing: Dp,
        val horizontalPadding: Dp
    )

    val messageListStyle: MessageListStyle
        @Composable get() = MessageListStyle(
            verticalSpacing = 2.dp,
            horizontalPadding = 16.dp
        )

    /**
     * Styling for chat screen container
     */
    @Immutable
    data class ChatScreenStyle(
        val backgroundColor: Color
    )

    val chatScreenStyle: ChatScreenStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            return ChatScreenStyle(
                backgroundColor = themeColors.background
            )
        }

    /**
     * Styling for the Concierge WebView overlay dialog.
     *
     * @param contentBackgroundColor Background color of the sheet
     * @param contentHeightFraction Fraction of screen height for the sheet
     * @param contentCornerRadius Corner radius for the top of the sheet
     * @param contentElevation Shadow elevation of the sheet
     * @param dismissDragThreshold Drag distance (dp) past which the sheet dismisses
     * @param slideAnimationDurationMs Duration (ms) of slide-in and slide-out animations
     * @param slideInInitialOffsetDp Initial offset (dp) below the sheet for slide-in (sheet starts off-screen)
     * @param handleWidth Width of the drag-handle touch area
     * @param handleHeight Height of the drag-handle touch area
     * @param handleTopPadding Top padding above the handle
     * @param handlePillWidth Width of the visible pill
     * @param handlePillHeight Height of the visible pill
     * @param handlePillCornerRadius Corner radius of the pill
     * @param handlePillColor Background color of the pill
     */
    @Immutable
    data class WebviewStyle(
        val contentBackgroundColor: Color,
        val contentHeightFraction: Float,
        val contentCornerRadius: Dp,
        val contentElevation: Dp,
        val dismissDragThreshold: Dp,
        val slideAnimationDurationMs: Int,
        val slideInInitialOffsetDp: Dp,
        val handleWidth: Dp,
        val handleHeight: Dp,
        val handleTopPadding: Dp,
        val handlePillWidth: Dp,
        val handlePillHeight: Dp,
        val handlePillCornerRadius: Dp,
        val handlePillColor: Color
    )

    val webviewStyle: WebviewStyle
        @Composable get() = WebviewStyle(
            contentBackgroundColor = Color.White,
            contentHeightFraction = 0.95f,
            contentCornerRadius = 12.dp,
            contentElevation = 8.dp,
            dismissDragThreshold = 80.dp,
            slideAnimationDurationMs = 300,
            slideInInitialOffsetDp = 1200.dp,
            handleWidth = 120.dp,
            handleHeight = 32.dp,
            handleTopPadding = 8.dp,
            handlePillWidth = 40.dp,
            handlePillHeight = 4.dp,
            handlePillCornerRadius = 2.dp,
            handlePillColor = Color.Gray.copy(alpha = 0.5f)
        )

    /**
     * Styling for chat text field
     */
    @Immutable
    data class ChatTextFieldStyle(
        val horizontalPadding: Dp,
        val maxLines: Int,
        val textStyle: TextStyle,
        val placeholderTextColor: Color,
        val fontSize: TextUnit? = null
    )

    val chatTextFieldStyle: ChatTextFieldStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            val themeTypography = ConciergeTheme.typography
            val fontSize = themeTypography?.inputFontSize?.sp
            return ChatTextFieldStyle(
                horizontalPadding = 8.dp,
                maxLines = 15,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = themeColors.inputText ?: themeColors.onSurface,
                    fontSize = fontSize ?: MaterialTheme.typography.bodyLarge.fontSize
                ),
                placeholderTextColor = themeColors.inputText?.copy(alpha = 0.7f) ?: themeColors.onSurface.copy(alpha = 0.7f),
                fontSize = fontSize
            )
        }

    /**
     * Styling for chat input field container
     */
    @Immutable
    data class ChatInputFieldStyle(
        val padding: Dp
    )

    val chatInputFieldStyle: ChatInputFieldStyle
        @Composable get() = ChatInputFieldStyle(
            padding = 16.dp
        )

    /**
     * Styling for feedback dialog
     */
    @Immutable
    data class FeedbackDialogStyle(
        val padding: Dp,
        val backgroundColor: Color,
        val elevation: Dp,
        val shape: Shape,
        val contentPadding: Dp,
        val titleStyle: TextStyle,
        val titleColor: Color,
        val titleSpacing: Dp,
        val questionStyle: TextStyle,
        val questionColor: Color,
        val questionSpacing: Dp,
        val categorySpacing: Dp,
        val checkboxCheckedColor: Color,
        val checkboxCheckmarkColor: Color,
        val checkboxUncheckedColor: Color,
        val checkboxSpacing: Dp,
        val categoryTextStyle: TextStyle,
        val categoryTextColor: Color,
        val categoriesNotesSpacing: Dp,
        val notesLabelStyle: TextStyle,
        val notesLabelColor: Color,
        val notesLabelSpacing: Dp,
        val notesPlaceholderStyle: TextStyle,
        val notesPlaceholderColor: Color,
        val notesButtonsSpacing: Dp,
        val textFieldBorderColor: Color,
        val textFieldTextColor: Color,
        val buttonSpacing: Dp,
        val cancelButtonColor: Color,
        val submitButtonColor: Color,
        val submitButtonTextColor: Color,
        val buttonTextStyle: TextStyle
    )

    val feedbackDialogStyle: FeedbackDialogStyle
        @Composable get() {
            val themeColors = ConciergeTheme.colors
            // Use the screen background for the dialog so it is always fully opaque.
            // conciergeMessageBackground can be semi-transparent (e.g. an overlay tint
            // for chat bubbles), which causes bleed-through when used on the dialog.
            val dialogBackground = themeColors.background
            val dialogTextColor = themeColors.conciergeMessageText ?: themeColors.onSurface
            // Checkbox: primary background when checked, white checkmark in both light and dark
            val checkboxCheckedColor = themeColors.feedbackDialogCheckboxCheckedColor ?: themeColors.primary
            val checkboxCheckmarkColor = Color.White

            return FeedbackDialogStyle(
                padding = 16.dp,
                backgroundColor = dialogBackground,
                elevation = 8.dp,
                shape = RoundedCornerShape(12.dp),
                contentPadding = 20.dp,
                titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                titleColor = dialogTextColor,
                titleSpacing = 12.dp,
                questionStyle = MaterialTheme.typography.bodyMedium,
                questionColor = dialogTextColor.copy(alpha = 0.7f),
                questionSpacing = 6.dp,
                categorySpacing = 0.dp,
                checkboxCheckedColor = checkboxCheckedColor,
                checkboxCheckmarkColor = checkboxCheckmarkColor,
                checkboxUncheckedColor = dialogTextColor.copy(alpha = 0.3f),
                checkboxSpacing = 8.dp,
                categoryTextStyle = MaterialTheme.typography.bodyMedium,
                categoryTextColor = dialogTextColor,
                categoriesNotesSpacing = 6.dp,
                notesLabelStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                notesLabelColor = dialogTextColor.copy(alpha = 0.7f),
                notesLabelSpacing = 8.dp,
                notesPlaceholderStyle = MaterialTheme.typography.bodyMedium,
                notesPlaceholderColor = dialogTextColor.copy(alpha = 0.5f),
                notesButtonsSpacing = 24.dp,
                textFieldBorderColor = dialogTextColor.copy(alpha = 0.2f),
                textFieldTextColor = dialogTextColor,
                buttonSpacing = 8.dp,
                cancelButtonColor = themeColors.feedbackDialogCancelButtonColor ?: themeColors.primary,
                submitButtonColor = themeColors.feedbackDialogSubmitButtonColor ?: themeColors.buttonSubmitFill ?: themeColors.primary,
                submitButtonTextColor = themeColors.feedbackDialogSubmitButtonTextColor ?: themeColors.buttonSubmitText ?: themeColors.onPrimary,
                buttonTextStyle = MaterialTheme.typography.labelMedium
            )
        }

    /**
     * Styling for the welcome card and its suggested prompt items.
     * When no theme is loaded and device is in dark mode, uses default dark palette; otherwise follows theme JSON.
     */
    @Immutable
    data class WelcomeCardStyle(
        val backgroundColor: Color,
        val shape: Shape,
        val elevation: Dp,
        val contentPadding: Dp,
        val titleTextStyle: TextStyle,
        val titleTextColor: Color,
        val titleBottomSpacing: Dp,
        val titleTextAlign: TextAlign,
        val descriptionTextStyle: TextStyle,
        val descriptionTextColor: Color,
        val descriptionTextAlign: TextAlign,
        val horizontalAlignment: Alignment.Horizontal,
        val promptsTopSpacing: Dp,
        val promptsHeaderTextStyle: TextStyle,
        val promptsHeaderTextColor: Color,
        val promptsHeaderBottomSpacing: Dp,
        val promptsSpacing: Dp,
        val promptBackgroundColor: Color,
        val promptShape: Shape,
        val promptPadding: Dp,
        val promptImageSize: Dp,
        val promptImageShape: Shape,
        val promptImagePlaceholderColor: Color,
        val promptImageSpacing: Dp,
        val promptTextStyle: TextStyle,
        val promptTextColor: Color,
        val promptFullWidth: Boolean,
        val promptMaxLines: Int
    )

    val welcomeCardStyle: WelcomeCardStyle
        @Composable get() {
            val isDark = isSystemInDarkTheme()
            val themeColors = ConciergeTheme.colors
            val useDefaultPalette = ConciergeTheme.useDefaultPalette
            val cssLayout = ConciergeTheme.tokens?.cssLayout

            // When no theme is loaded and device is in dark mode, use default dark palette; otherwise use theme colors
            val useDefaultDarkModeStyling = useDefaultPalette && isDark
            val cardBackground = if (useDefaultDarkModeStyling) DarkConciergeColors.background else themeColors.background
            val cardSurface = if (useDefaultDarkModeStyling) DarkConciergeColors.surface else themeColors.surface
            val textColor = if (useDefaultDarkModeStyling) DarkConciergeColors.onSurface else themeColors.onSurface

            // Resolve text alignment from theme (default: center to preserve existing behavior)
            val alignValue = cssLayout?.welcomeTextAlign
            val textAlign = when (alignValue) {
                "left", "start" -> TextAlign.Start
                "right", "end" -> TextAlign.End
                else -> TextAlign.Center
            }
            val horizontalAlignment = when (alignValue) {
                "left", "start" -> Alignment.Start
                "right", "end" -> Alignment.End
                else -> Alignment.CenterHorizontally
            }

            // Resolve title text style from theme font size
            val titleTextStyle = cssLayout?.welcomeTitleFontSize?.let { size ->
                MaterialTheme.typography.bodyLarge.copy(fontSize = size.sp)
            } ?: MaterialTheme.typography.headlineSmall

            return WelcomeCardStyle(
                backgroundColor = cardBackground,
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
                contentPadding = cssLayout?.welcomeContentPadding?.dp ?: 20.dp,
                titleTextStyle = titleTextStyle,
                titleTextColor = textColor,
                titleBottomSpacing = cssLayout?.welcomeTitleBottomSpacing?.dp ?: 8.dp,
                titleTextAlign = textAlign,
                descriptionTextStyle = MaterialTheme.typography.bodyMedium,
                descriptionTextColor = textColor.copy(alpha = 0.9f),
                descriptionTextAlign = textAlign,
                horizontalAlignment = horizontalAlignment,
                promptsTopSpacing = cssLayout?.welcomePromptsTopSpacing?.dp ?: 8.dp,
                promptsHeaderTextStyle = MaterialTheme.typography.bodySmall,
                promptsHeaderTextColor = textColor.copy(alpha = 0.8f),
                promptsHeaderBottomSpacing = 12.dp,
                promptsSpacing = cssLayout?.welcomePromptSpacing?.dp ?: 8.dp,
                promptBackgroundColor = themeColors.welcomePromptBackground ?: cardSurface,
                promptShape = RoundedCornerShape(cssLayout?.welcomePromptCornerRadius?.dp ?: 8.dp),
                promptPadding = cssLayout?.welcomePromptPadding?.dp ?: 0.dp,
                promptImageSize = cssLayout?.welcomePromptImageSize?.dp ?: 75.dp,
                promptImageShape = RoundedCornerShape(4.dp),
                promptImagePlaceholderColor = textColor.copy(alpha = 0.1f),
                promptImageSpacing = 12.dp,
                promptTextStyle = MaterialTheme.typography.bodyMedium,
                promptTextColor = themeColors.welcomePromptText ?: textColor,
                promptFullWidth = ConciergeTheme.behavior?.welcomeCard?.promptFullWidth ?: true,
                promptMaxLines = ConciergeTheme.behavior?.welcomeCard?.promptMaxLines ?: Int.MAX_VALUE
            )
        }
}