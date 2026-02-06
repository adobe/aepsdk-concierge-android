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
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the InputActionButtons composable (mic and send buttons).
 */
class InputActionButtonsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun inputActionButtons_emptyState_displaysMicAndSend() {
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
        composeTestRule.onNode(hasContentDescription("Send message")).assertIsDisplayed()
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
    fun inputActionButtons_recordingState_voiceCancelTriggeredOnMicClick() {
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
