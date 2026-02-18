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
 * UI tests for the MicButton composable.
 * Tests microphone button states and interactions.
 */
class MicButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun micButton_inEmptyState_displaysStartVoiceInput() {
        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Empty,
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input"))
            .assertIsDisplayed()
    }

    @Test
    fun micButton_inRecordingState_displaysStopRecording() {
        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Recording(transcription = ""),
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording"))
            .assertIsDisplayed()
    }

    @Test
    fun micButton_inEditingState_displaysStartVoiceInput() {
        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Editing(content = "test"),
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input"))
            .assertIsDisplayed()
    }

    @Test
    fun micButton_whenEnabled_triggersCallback() {
        var clickCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Empty,
                    isEnabled = true,
                    onClick = { clickCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input"))
            .performClick()

        assert(clickCalled)
    }

    @Test
    fun micButton_whenDisabled_doesNotTriggerCallback() {
        var clickCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Empty,
                    isEnabled = false,
                    onClick = { clickCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input"))
            .performClick()

        assert(!clickCalled)
    }

    @Test
    fun micButton_whileRecording_canBeClicked() {
        var clickCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Recording(transcription = "Hello..."),
                    isEnabled = true,
                    onClick = { clickCalled = true }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording"))
            .performClick()

        assert(clickCalled)
    }

    @Test
    fun micButton_stateTransition_fromEmptyToRecording() {
        var currentState: UserInputState = UserInputState.Empty

        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = currentState,
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Start voice input"))
            .assertIsDisplayed()

        currentState = UserInputState.Recording(transcription = "")
        composeTestRule.waitForIdle()
    }

    @Test
    fun micButton_multipleClicks_triggersMultipleTimes() {
        var clickCount = 0

        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Empty,
                    isEnabled = true,
                    onClick = { clickCount++ }
                )
            }
        }

        val button = composeTestRule.onNode(hasContentDescription("Start voice input"))
        button.performClick()
        button.performClick()

        assert(clickCount == 2)
    }

    @Test
    fun micButton_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Empty,
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun micButton_withTranscription_stillDisplaysStopRecording() {
        composeTestRule.setContent {
            ConciergeTheme {
                MicButton(
                    userInputState = UserInputState.Recording(transcription = "Hello world..."),
                    isEnabled = true,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Stop recording"))
            .assertIsDisplayed()
    }
}
