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

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the VoiceRecordingPanel composable.
 */
class VoiceRecordingPanelTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun voiceRecordingPanel_displaysCancelButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                VoiceRecordingPanel(
                    inputState = UserInputState.Recording(),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Cancel recording")).assertExists()
    }

    @Test
    fun voiceRecordingPanel_recordingState_displaysConfirmButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                VoiceRecordingPanel(
                    inputState = UserInputState.Recording(transcription = "hello"),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Confirm recording")).assertExists()
    }

    @Test
    fun voiceRecordingPanel_cancelClick_triggersCallback() {
        var cancelCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                VoiceRecordingPanel(
                    inputState = UserInputState.Recording(),
                    onCancel = { cancelCalled = true },
                    onConfirm = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Cancel recording")).performClick()
        assert(cancelCalled)
    }

    @Test
    fun voiceRecordingPanel_confirmClick_triggersCallback() {
        var confirmCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                VoiceRecordingPanel(
                    inputState = UserInputState.Recording(),
                    onCancel = {},
                    onConfirm = { confirmCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Confirm recording")).performClick()
        assert(confirmCalled)
    }

    @Test
    fun voiceRecordingPanel_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                VoiceRecordingPanel(
                    inputState = UserInputState.Recording(),
                    onCancel = {},
                    onConfirm = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
