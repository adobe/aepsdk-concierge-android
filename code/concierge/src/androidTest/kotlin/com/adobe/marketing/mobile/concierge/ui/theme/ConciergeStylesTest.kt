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

package com.adobe.marketing.mobile.concierge.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for ConciergeStyles that require a Composable context (e.g. disclaimerStyle).
 */
class ConciergeStylesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disclaimerStyle_returnsStyleWithExpectedDefaults() {
        var style: ConciergeStyles.DisclaimerStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.disclaimerStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(8.dp, style!!.padding)
        assertEquals(TextDecoration.Underline, style!!.linkTextDecoration)
    }

    @Test
    fun disclaimerStyle_withThemeTypography_appliesFontSizeAndWeight() {
        var style: ConciergeStyles.DisclaimerStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                typography = ConciergeTypographyConfig(
                    disclaimerFontSize = 14.0,
                    disclaimerFontWeight = 700
                )
            ),
            tokens = null
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.disclaimerStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(14.0, style!!.textStyle.fontSize.value.toDouble(), 0.1)
        assertEquals(700, style!!.textStyle.fontWeight?.weight ?: 0)
    }

    // -----------------------------------------------------------------------
    // messageBubbleStyle
    // -----------------------------------------------------------------------

    @Test
    fun messageBubbleStyle_defaultStyle_allCornersRounded() {
        var style: ConciergeStyles.MessageBubbleStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.messageBubbleStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(style!!.shape, style!!.userMessageShape)
        assertEquals(RoundedCornerShape(12.dp), style!!.userMessageShape)
    }

    @Test
    fun messageBubbleStyle_balloonStyle_squaresBottomRightCorner() {
        var style: ConciergeStyles.MessageBubbleStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(
                    chat = ConciergeChatBehavior(userMessageBubbleStyle = UserMessageBubbleStyle.BALLOON)
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.messageBubbleStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        val expected = RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = 12.dp,
            bottomEnd = 0.dp
        )
        assertEquals(expected, style!!.userMessageShape)
        // Agent message shape is always fully rounded regardless of userMessageBubbleStyle
        assertEquals(RoundedCornerShape(12.dp), style!!.shape)
    }

    @Test
    fun messageBubbleStyle_customBorderRadius_appliedToBothShapes() {
        var style: ConciergeStyles.MessageBubbleStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(
                    chat = ConciergeChatBehavior(userMessageBubbleStyle = UserMessageBubbleStyle.BALLOON)
                ),
                cssLayout = ConciergeLayout(messageBorderRadius = 20.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.messageBubbleStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(RoundedCornerShape(20.dp), style!!.shape)
        val expectedUserShape = RoundedCornerShape(
            topStart = 20.dp,
            topEnd = 20.dp,
            bottomStart = 20.dp,
            bottomEnd = 0.dp
        )
        assertEquals(expectedUserShape, style!!.userMessageShape)
    }

    @Test
    fun messageBubbleStyle_defaultAgentIconDimensions_usedWhenTokensAbsent() {
        var style: ConciergeStyles.MessageBubbleStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.messageBubbleStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(39.dp, style!!.agentIconSize)
        assertEquals(12.dp, style!!.agentIconSpacing)
    }

    @Test
    fun messageBubbleStyle_agentIconDimensions_readFromCssLayoutTokens() {
        var style: ConciergeStyles.MessageBubbleStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(agentIconSize = 48.0, agentIconSpacing = 16.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.messageBubbleStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(48.dp, style!!.agentIconSize)
        assertEquals(16.dp, style!!.agentIconSpacing)
    }

    // -----------------------------------------------------------------------
    // thinkingAnimationStyle
    // -----------------------------------------------------------------------

    @Test
    fun thinkingAnimationStyle_returnsDefaultDotSizeAndSpacing() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(8.dp, style!!.dotSize)
        assertEquals(8.dp, style!!.dotSpacing)
    }

    @Test
    fun thinkingAnimationStyle_defaultVerticalAlignment_isCenterVertically() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Alignment.CenterVertically, style!!.dotVerticalAlignment)
    }

    @Test
    fun thinkingAnimationStyle_withCssLayout_appliesDotSize() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotSize = 12.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(12.dp, style!!.dotSize)
    }

    @Test
    fun thinkingAnimationStyle_withCssLayout_appliesDotSpacing() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotSpacing = 10.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(10.dp, style!!.dotSpacing)
    }

    @Test
    fun thinkingAnimationStyle_verticalAlignment_topString_mapsToAlignmentTop() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotVerticalAlignment = "top"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Alignment.Top, style!!.dotVerticalAlignment)
    }

    @Test
    fun thinkingAnimationStyle_verticalAlignment_bottomString_mapsToAlignmentBottom() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotVerticalAlignment = "bottom"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Alignment.Bottom, style!!.dotVerticalAlignment)
    }

    @Test
    fun thinkingAnimationStyle_verticalAlignment_unknownString_fallsBackToCenterVertically() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotVerticalAlignment = "invalid"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Alignment.CenterVertically, style!!.dotVerticalAlignment)
    }

    @Test
    fun thinkingAnimationStyle_verticalAlignment_upperCaseString_isCaseInsensitive() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingDotVerticalAlignment = "TOP"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Alignment.Top, style!!.dotVerticalAlignment)
    }

    @Test
    fun thinkingAnimationStyle_defaultBubbleShape_is8dpRoundedCorner() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(RoundedCornerShape(8.dp), style!!.bubbleShape)
    }

    @Test
    fun thinkingAnimationStyle_withCssLayout_appliesBubbleBorderRadius() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(thinkingBubbleBorderRadius = 16.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(RoundedCornerShape(16.dp), style!!.bubbleShape)
    }

    @Test
    fun thinkingAnimationStyle_defaultBubblePadding_is16dpHorizontal8dpVertical() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(PaddingValues(horizontal = 16.dp, vertical = 8.dp), style!!.bubblePadding)
    }

    @Test
    fun thinkingAnimationStyle_withCssLayout_appliesBubblePadding() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(
                    thinkingBubblePaddingHorizontal = 20.0,
                    thinkingBubblePaddingVertical = 12.0
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(PaddingValues(horizontal = 20.dp, vertical = 12.dp), style!!.bubblePadding)
    }

    @Test
    fun thinkingAnimationStyle_defaultDotColor_fallsBackToPrimaryWithAlpha() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        val expected = LightConciergeColors.primary.copy(alpha = 0.7f)
        assertEquals(expected, style!!.dotColor)
    }

    @Test
    fun thinkingAnimationStyle_customDotColor_takesOverDefault() {
        var style: ConciergeStyles.ThinkingAnimationStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                colors = ConciergeThemeColors(
                    thinking = ConciergeThinkingColors(dotColor = "#FF0000")
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.thinkingAnimationStyle
            }
        }

        composeTestRule.waitForIdle()
        assertEquals(Color(0xFFFF0000), style!!.dotColor)
    }

    // -----------------------------------------------------------------------
    // productCarouselStyle
    // -----------------------------------------------------------------------

    @Test
    fun productCarouselStyle_noTokens_trailingContentPaddingFallsBackTo4dp() {
        var style: ConciergeStyles.ProductCarouselStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.productCarouselStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(4.dp, style!!.trailingContentPadding)
    }

    @Test
    fun productCarouselStyle_trailingContentPadding_usesChatHistoryPaddingWhenCarouselPaddingUnset() {
        var style: ConciergeStyles.ProductCarouselStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(chatHistoryPadding = 20.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.productCarouselStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(20.dp, style!!.trailingContentPadding)
    }

    @Test
    fun productCarouselStyle_trailingContentPadding_usesCarouselHorizontalPaddingOverChatHistoryPadding() {
        var style: ConciergeStyles.ProductCarouselStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(
                    productCardCarouselHorizontalPadding = 8.0,
                    chatHistoryPadding = 20.0
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.productCarouselStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(8.dp, style!!.trailingContentPadding)
    }

    @Test
    fun productCarouselStyle_itemSpacing_readsFromProductCardCarouselSpacing() {
        var style: ConciergeStyles.ProductCarouselStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(productCardCarouselSpacing = 16.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.productCarouselStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(16.dp, style!!.itemSpacing)
    }

    @Test
    fun productCarouselStyle_noTokens_itemSpacingDefaultsTo12dp() {
        var style: ConciergeStyles.ProductCarouselStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.productCarouselStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(12.dp, style!!.itemSpacing)
    }

    // -----------------------------------------------------------------------
    // extendedProductCardStyle
    // -----------------------------------------------------------------------

    @Test
    fun extendedProductCardStyle_noTokens_cardWidthDefaultsTo250dp() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // 250dp was chosen after testing across devices
        assertEquals(250.dp, style!!.cardWidth)
    }

    @Test
    fun extendedProductCardStyle_noTokens_sectionAndPriceSpacingMatchSpecDefaults() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // The card spec's gap between the headline/subhead block and the price block is 16px.
        assertEquals(16.dp, style!!.sectionSpacing)
        // The card spec's price/was-price stack has no gap between the two lines.
        assertEquals(0.dp, style!!.priceSpacing)
    }

    @Test
    fun extendedProductCardStyle_noTokens_headlineLineHeightIs17sp_atDefault14spFontSize() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // Title and price are both 14px/700-or-400 "headline" role text; spec line-height is 17px.
        assertEquals(17f, style!!.titleLineHeight.value, 0.01f)
        assertEquals(17f, style!!.priceLineHeight.value, 0.01f)
    }

    @Test
    fun extendedProductCardStyle_noTokens_captionLineHeightIs14sp_atDefault12spFontSize() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // Subtitle, was-price, and badge are all 12px "caption" role text; spec line-height is 14px.
        assertEquals(14f, style!!.subtitleLineHeight.value, 0.01f)
        assertEquals(14f, style!!.wasPriceLineHeight.value, 0.01f)
        assertEquals(14f, style!!.badgeLineHeight.value, 0.01f)
    }

    @Test
    fun extendedProductCardStyle_lineHeightRatio_scalesWithCustomFontSize() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(productCardTitleFontSize = 28.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // Ratio (17/14) is preserved rather than an absolute px value, so a themed font size
        // still gets proportional, non-cramped line spacing.
        assertEquals(28f * (17f / 14f), style!!.titleLineHeight.value, 0.01f)
    }

    @Test
    fun extendedProductCardStyle_noTokens_subtitleAndPriceLetterSpacingMatchSpec() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(-0.5f, style!!.subtitleLetterSpacing.value, 0.01f)
        assertEquals(-0.5f, style!!.priceLetterSpacing.value, 0.01f)
    }

    @Test
    fun extendedProductCardStyle_noTokens_badgeLetterSpacingIsZero() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // Pinned explicitly so the badge can't inherit a host app's ambient letter spacing.
        assertEquals(0f, style!!.badgeLetterSpacing.value, 0.01f)
    }

    @Test
    fun extendedProductCardStyle_withThemeOverrides_sectionAndPriceSpacingAreConfigurable() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(
                    productCardSectionSpacing = 24.0,
                    productCardPriceSpacing = 4.0
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(24.dp, style!!.sectionSpacing)
        assertEquals(4.dp, style!!.priceSpacing)
    }

    @Test
    fun extendedProductCardStyle_sectionSpacing_fallsBackToGenericTextSpacingWhenUnset() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                // productCardSectionSpacing is intentionally left unset so sectionSpacing
                // falls through to the generic productCardTextSpacing value.
                cssLayout = ConciergeLayout(productCardTextSpacing = 12.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(12.dp, style!!.sectionSpacing)
    }

    @Test
    fun extendedProductCardStyle_withBoxShadowToken_shadowElevationAndColorReflectBlurAndColor() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                cssLayout = ConciergeLayout(
                    multimodalCardBoxShadow = mapOf(
                        "offsetX" to 0.0,
                        "offsetY" to 1.0,
                        "blurRadius" to 3.0,
                        "spreadRadius" to 0.0,
                        "color" to Color(0x14000000)
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        assertEquals(3.dp, style!!.shadowElevation)
        assertEquals(Color(0x14000000), style!!.shadowColor)
    }

    @Test
    fun extendedProductCardStyle_noBoxShadowToken_shadowElevationIsZeroAndColorIsTransparent() {
        var style: ConciergeStyles.ExtendedProductCardStyle? = null

        composeTestRule.setContent {
            ConciergeTheme {
                style = ConciergeStyles.extendedProductCardStyle
            }
        }

        composeTestRule.waitForIdle()
        assertNotNull(style)
        // Covers both an unset theme and an explicit "none" box-shadow, since parseBoxShadow
        // collapses "none" to the same null the token has when it's never set.
        assertEquals(0.dp, style!!.shadowElevation)
        assertEquals(Color.Transparent, style!!.shadowColor)
    }
}
