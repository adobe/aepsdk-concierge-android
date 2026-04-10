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

import androidx.compose.foundation.shape.RoundedCornerShape
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
}
