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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradientColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeInputColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the SendButton composable.
 * Tests send button states and interactions.
 */
class SendButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sendButton_whenEnabled_isDisplayed() {
        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = true,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_whenEnabled_isClickable() {
        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = true,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsEnabled()
    }

    @Test
    fun sendButton_whenDisabled_isDisplayed() {
        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = false,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_whenDisabled_isNotClickable() {
        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = false,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsNotEnabled()
    }

    @Test
    fun sendButton_whenEnabled_triggersCallback() {
        var sendCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = true,
                    onSend = { sendCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .performClick()

        assert(sendCalled)
    }

    @Test
    fun sendButton_whenDisabled_doesNotTriggerCallback() {
        var sendCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = false,
                    onSend = { sendCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .performClick()

        assert(!sendCalled)
    }

    @Test
    fun sendButton_multipleClicks_triggersMultipleTimes() {
        var clickCount = 0

        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = true,
                    onSend = { clickCount++ }
                )
            }
        }

        val button = composeTestRule.onNode(hasContentDescription("Send message"))
        button.performClick()
        button.performClick()
        button.performClick()

        assert(clickCount == 3)
    }

    @Test
    fun sendButton_stateChange_fromDisabledToEnabled() {
        var isEnabled = false

        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = isEnabled,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsNotEnabled()

        isEnabled = true
        composeTestRule.waitForIdle()
    }

    @Test
    fun sendButton_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                SendButton(
                    isEnabled = true,
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun sendButton_arrowStyleWithCircleGradient_isDisplayedAndClickable() {
        var sendCalled = false
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"),
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(
                        sendArrowBackgroundGradient = ConciergeGradientColors(startColor = "#12B0A0", endColor = "#6DD3C4")
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(
                    isEnabled = true,
                    onSend = { sendCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
            .performClick()
        assert(sendCalled)
    }

    @Test
    fun sendButton_arrowStyleDisabledWithCircleGradient_isDisplayedWithoutCrashing() {
        // Exercises the disabled path, which must fall back to the plain dimmed fill rather than
        // the gradient background.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"),
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(
                        sendArrowBackgroundGradient = ConciergeGradientColors(startColor = "#12B0A0", endColor = "#6DD3C4")
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(
                    isEnabled = false,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_arrowStyleWithNoGradientConfigured_isDisplayedAndClickable() {
        // Exercises the arrow style's plain solid-fill path (circleGradient is null).
        var sendCalled = false
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(
                    isEnabled = true,
                    onSend = { sendCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
            .performClick()
        assert(sendCalled)
    }

    @Test
    fun sendButton_arrowStyleWithNonRenderableGradient_fallsBackToSolidFill() {
        // Exercises the "gradient set but only one side configured" path, which must fall back to
        // the solid circleColor fill rather than the gradient.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"),
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(
                        sendArrowBackgroundGradient = ConciergeGradientColors(startColor = "#12B0A0")
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(
                    isEnabled = true,
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertIsDisplayed()
    }

    @Test
    fun sendButton_arrowStyle_glyphIsSmallerThanCircleAtDefaultSize() {
        // Regression test: the arrow glyph previously filled its circle edge-to-edge (both sized to
        // the same value), removing the margin around it. The circle ("SendIconGlyph") must track
        // the theme's icon-size knob directly; the arrow ("SendArrowGlyph") must stay smaller than it.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(isEnabled = true, onSend = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SendIconGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
        composeTestRule.onNodeWithTag("SendArrowGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(16.dp)
            .assertHeightIsEqualTo(16.dp)
    }

    @Test
    fun sendButton_arrowStyle_circleAndArrowResizeTogetherWithInputButtonHeight() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(sendButtonStyle = "arrow"),
                cssLayout = ConciergeLayout(inputButtonHeight = 60.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                SendButton(isEnabled = true, onSend = {})
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("SendIconGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(60.dp)
        composeTestRule.onNodeWithTag("SendArrowGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
    }

}
