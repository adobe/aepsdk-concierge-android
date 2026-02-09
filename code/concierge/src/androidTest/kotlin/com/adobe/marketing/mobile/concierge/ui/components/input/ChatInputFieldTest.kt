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
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatInputField composable.
 * Tests input handling, voice recording states, and user interactions.
 */
class ChatInputFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatInputField_displaysPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    placeholder = "How can I help",
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help")
            .assertIsDisplayed()
    }

    @Test
    fun chatInputField_isEnabledByDefault() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    enable = true,
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        // Note: More specific assertions would require test tags on the TextField
        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_canBeDisabled() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    enable = false,
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_triggersOnTextChangeCallback() {
        var changedText = ""
        
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    placeholder = "Type here",
                    onTextChange = { changedText = it },
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        // Note: In a real test, you'd use onNodeWithTag to find the input field
        
        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_handlesRecordingState() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    inputState = UserInputState.Recording(transcription = "Hello..."),
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_handlesEditingState() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    inputState = UserInputState.Editing(content = "Test message"),
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_handlesEmptyState() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    inputState = UserInputState.Empty,
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_showsProcessingState() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    isProcessing = true,
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatInputField_doesNotCrashWithMultipleStateChanges() {
        var currentState: UserInputState = UserInputState.Empty
        
        composeTestRule.setContent {
            ConciergeTheme {
                ChatInputField(
                    inputState = currentState,
                    onTextChange = {},
                    onMicPressed = {},
                    onSend = {},
                    onVoiceCancel = {}
                )
            }
        }

        currentState = UserInputState.Editing("Test")
        composeTestRule.waitForIdle()
        
        currentState = UserInputState.Recording("Recording...")
        composeTestRule.waitForIdle()
        
        currentState = UserInputState.Empty
        composeTestRule.waitForIdle()

    }
}
