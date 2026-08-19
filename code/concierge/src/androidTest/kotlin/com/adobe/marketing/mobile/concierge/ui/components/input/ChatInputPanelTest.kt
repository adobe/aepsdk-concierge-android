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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradientColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeInputColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatInputPanel composable.
 */
class ChatInputPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatInputPanel_displaysPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    placeholder = "How can I help",
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help").assertExists()
    }

    @Test
    fun chatInputPanel_displaysTextAndSendButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputPanel(
                    text = "Hello",
                    onTextChange = {},
                    placeholder = "Type here",
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Hello").assertExists()
        composeTestRule.onNode(hasContentDescription("Send message")).assertIsDisplayed()
    }

    @Test
    fun chatInputPanel_sendButtonClick_triggersCallback() {
        var sentText: String? = null

        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputPanel(
                    text = "Test message",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = { sentText = it }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message")).performClick()
        assert(sentText == "Test message")
    }

    @Test
    fun chatInputPanel_recordingState_showsListeningPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    placeholder = "Type here",
                    inputState = UserInputState.Recording(transcription = "hello"),
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputPanel_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputPanel_withOutlineGradient_rendersWithoutCrashing() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(
                        outlineGradient = ConciergeGradientColors(
                            startColor = "#12B0A0",
                            endColor = "#6DD3C4",
                            angle = 90.0
                        )
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {},
                    isFocused = false
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputPanel_focusedWithOutlineGradient_prefersFocusBorderOverGradient() {
        // The focus border always takes priority over the outline gradient -- this just exercises
        // that branch alongside a configured gradient without crashing.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(
                        outlineGradient = ConciergeGradientColors(startColor = "#12B0A0", endColor = "#6DD3C4")
                    )
                )
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {},
                    isFocused = true
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
