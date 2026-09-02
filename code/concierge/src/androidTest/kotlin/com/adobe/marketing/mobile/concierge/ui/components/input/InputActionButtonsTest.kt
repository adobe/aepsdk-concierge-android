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
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the InputActionButtons composable (mic and send buttons).
 */
class InputActionButtonsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputActionButtons_emptyState_displaysMic() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input")).assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Send message")).assertDoesNotExist()
    }

    @Test
    fun inputActionButtons_micClick_triggersCallback() {
        var micPressed = false

        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "",
                    isProcessing = false,
                    onMicPressed = { micPressed = true },
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input")).performClick()
        assert(micPressed)
    }

    @Test
    fun inputActionButtons_withText_sendTriggersCallback() {
        var sentText: String? = null

        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = { sentText = it }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Send message")).performClick()
        assert(sentText == "Hello")
    }

    @Test
    fun inputActionButtons_recordingState_voiceCancelTriggeredOnStopButtonClick() {
        var voiceCancelCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = { voiceCancelCalled = true },
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording")).performClick()
        assert(voiceCancelCalled)
    }

    @Test
    fun inputActionButtons_recordingState_displaysStopRecordingButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording")).assertIsDisplayed()
    }

    @Test
    fun inputActionButtons_emptyState_doesNotDisplayStopRecordingButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording")).assertDoesNotExist()
    }

    @Test
    fun inputActionButtons_recordingState_displaysBothMicAnimationAndStopButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Recording in progress")).assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Stop recording")).assertIsDisplayed()
    }

    @Test
    fun inputActionButtons_recordingWithPulsingBackgroundDisabled_micAndStopIconsEnlarge() {
        // Without the pulsing disc, the mic and stop icons enlarge to MIC_INNER_DISC_SCALE
        // (1.3x the base 24dp glyph size) so they stay visually consistent -- this must not be
        // clamped back down to the base size by their containers.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(behavior = ConciergeThemeBehavior(enableMicPulseBackground = false))
        )
        val enlargedSize = 24.dp * MIC_INNER_DISC_SCALE

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Recording in progress"))
            .assertWidthIsEqualTo(enlargedSize)
            .assertHeightIsEqualTo(enlargedSize)
        composeTestRule.onNode(hasContentDescription("Stop recording"))
            .assertWidthIsEqualTo(enlargedSize)
            .assertHeightIsEqualTo(enlargedSize)
    }

    @Test
    fun inputActionButtons_recordingWithPulsingBackgroundEnabled_micIconStaysBaseSize() {
        // With the pulsing disc providing visual weight, the mic icon should stay at the base
        // glyph size rather than enlarging.
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Recording in progress"))
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun inputActionButtons_recordingWithPulsingBackgroundEnabled_micContainerGrowsToFitPulseRing() {
        // The pulsing ring visually scales up to pulseScaleRange.second (2.0x by default), well
        // past the base 24dp glyph -- MicButton's own outer container (not the inner glyph, which
        // stays at base size per the test above) must reserve that much room, or this Row's
        // animateContentSize (which clips to its own bounds) truncates the ring. Regression test
        // for the mic-pulse-ring left-edge clipping bug.
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        val expectedSize = 24.dp * 2.0f // ConciergeStyles.micButtonStyle.pulseScaleRange.second default
        composeTestRule.onNodeWithTag("MicButtonContainer")
            .assertWidthIsEqualTo(expectedSize)
            .assertHeightIsEqualTo(expectedSize)
    }

    @Test
    fun inputActionButtons_recordingState_micClickDoesNotTriggerVoiceCancel() {
        var voiceCancelCalled = false
        var micPressedCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Recording(transcription = "test"),
                    text = "",
                    isProcessing = false,
                    onMicPressed = { micPressedCalled = true },
                    onVoiceCancel = { voiceCancelCalled = true },
                    onSend = {}
                )
            }
        }

        // Tapping the mic animation during recording is a no-op — only the stop button cancels.
        composeTestRule.onNode(hasContentDescription("Recording in progress")).performClick()

        assert(!voiceCancelCalled) { "onVoiceCancel must not fire when the mic animation is tapped during recording" }
        assert(!micPressedCalled) { "onMicPressed must not fire when the mic animation is tapped during recording" }
    }

    @Test
    fun inputActionButtons_micIconGlyph_resizesWithInputButtonHeight() {
        // Regression test: the mic glyph (GradientTintableIcon) previously had no explicit size
        // modifier, so it stayed at its drawable's intrinsic size even though the outer
        // IconButton container correctly resized -- the override had no visible effect.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(inputButtonHeight = 50.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // The glyph sits inside the button's merged semantics node, so it is only addressable
        // in the unmerged tree.
        composeTestRule.onNodeWithTag("MicIconGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun inputActionButtons_sendIconGlyph_resizesWithInputButtonHeight() {
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(inputButtonHeight = 50.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // The glyph sits inside the button's merged semantics node, so it is only addressable
        // in the unmerged tree.
        composeTestRule.onNodeWithTag("SendIconGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun inputActionButtons_clearIconGlyph_resizesWithInputButtonHeight() {
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(inputButtonHeight = 50.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // The glyph sits inside the button's merged semantics node, so it is only addressable
        // in the unmerged tree.
        composeTestRule.onNodeWithTag("ClearIconGlyph", useUnmergedTree = true)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun inputActionButtons_clearButtonContainer_resizesWithInputButtonHeight() {
        // Action buttons render at the shared glyph size with no padded layout container, so the
        // tap target tracks the icon-size knob exactly (touch expansion happens at the input
        // layer). The button must equal the configured glyph size, not glyph + padding.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(inputButtonHeight = 80.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasContentDescription("Clear input"))
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun inputActionButtons_clearButtonContainer_defaultsToGlyphSize24dp() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasContentDescription("Clear input"))
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun inputActionButtons_sendButtonContainer_resizesWithInputButtonHeight() {
        // Send tracks the same shared glyph size as its row neighbors, with no padded container.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(cssLayout = ConciergeLayout(inputButtonHeight = 80.0))
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertWidthIsEqualTo(80.dp)
            .assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun inputActionButtons_sendButtonContainer_defaultsToGlyphSize24dp() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "Hello",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNode(hasContentDescription("Send message"))
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun inputActionButtons_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                InputActionButtons(
                    inputState = UserInputState.Empty,
                    text = "",
                    isProcessing = false,
                    onMicPressed = {},
                    onVoiceCancel = {},
                    onSend = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
