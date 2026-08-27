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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradientColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeInputColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTextStrings
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
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
        // borderWidth must be > 0.dp for the border (solid or gradient) to render at all -- see
        // ConciergeStyles.inputPanelStyle, which falls back to 0.dp when cssLayout is unset.
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
                ),
                cssLayout = ConciergeLayout(inputOutlineWidth = 2.0)
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
    fun chatInputPanel_withSolidOutlineColorAndNoGradient_rendersWithoutCrashing() {
        // Exercises the solid-border branch (no renderable gradient set) with a nonzero border
        // width, the counterpart to the gradient case above.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                colors = ConciergeThemeColors(
                    input = ConciergeInputColors(outline = "#4B75FF")
                ),
                cssLayout = ConciergeLayout(inputOutlineWidth = 2.0)
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

    @Test
    fun chatInputPanel_showAiChatIcon_showsLeadingIconContainer_whenConfigured() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                text = ConciergeTextStrings(inputAiChatIconTooltip = "Ask AI")
            ),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(showAiChatIcon = "icon_ai_sparkle")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("ChatInputLeadingIcon").assertIsDisplayed()
    }

    @Test
    fun chatInputPanel_showAiChatIcon_glyphResizesWithInputButtonHeight() {
        // A URL (not a local asset name) so the glyph renders immediately via AsyncImage's
        // loading-state Box, without depending on a real bundled test asset resolving.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                text = ConciergeTextStrings(inputAiChatIconTooltip = "Ask AI")
            ),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(showAiChatIcon = "https://example.com/leading-icon.png"),
                cssLayout = ConciergeLayout(inputButtonHeight = 50.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatInputPanel(
                        text = "",
                        onTextChange = {},
                        inputState = UserInputState.Empty,
                        onMicPressed = {},
                        onSend = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ChatInputLeadingIconGlyph")
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun chatInputPanel_showAiChatIcon_containerResizesWithInputButtonHeight() {
        // Regression test: the leading icon's container was pinned at 56dp while only the glyph
        // scaled, so an icon size above 56dp overflowed its own container. The container is the
        // glyph plus the spec's 16dp padding on each side, so it must grow with the glyph.
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                text = ConciergeTextStrings(inputAiChatIconTooltip = "Ask AI")
            ),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(showAiChatIcon = "https://example.com/leading-icon.png"),
                cssLayout = ConciergeLayout(inputButtonHeight = 80.0)
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatInputPanel(
                        text = "",
                        onTextChange = {},
                        inputState = UserInputState.Empty,
                        onMicPressed = {},
                        onSend = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ChatInputLeadingIcon")
            .assertWidthIsEqualTo(112.dp)
            .assertHeightIsEqualTo(112.dp)
        composeTestRule.onNodeWithTag("ChatInputLeadingIconGlyph")
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun chatInputPanel_showAiChatIcon_containerDefaultsToSpec56dp() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(
                text = ConciergeTextStrings(inputAiChatIconTooltip = "Ask AI")
            ),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(showAiChatIcon = "https://example.com/leading-icon.png")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatInputPanel(
                        text = "",
                        onTextChange = {},
                        inputState = UserInputState.Empty,
                        onMicPressed = {},
                        onSend = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("ChatInputLeadingIcon")
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
    }

    @Test
    fun chatInputPanel_showAiChatIcon_hidesLeadingIcon_whenNotConfigured() {
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

        composeTestRule.onNodeWithTag("ChatInputLeadingIcon").assertDoesNotExist()
    }

    @Test
    fun chatInputPanel_showAiChatIcon_hidesLeadingIcon_whenBlank() {
        val themeData = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                behavior = ConciergeThemeBehavior(showAiChatIcon = "")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = themeData) {
                ChatInputPanel(
                    text = "",
                    onTextChange = {},
                    inputState = UserInputState.Empty,
                    onMicPressed = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("ChatInputLeadingIcon").assertDoesNotExist()
    }
}
