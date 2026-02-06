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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the UserInput composable.
 */
class UserInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userInput_displaysPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                UserInput(
                    inputState = UserInputState.Empty,
                    onTextChange = {},
                    onMicEvent = {},
                    onSend = {},
                    hasAudioPermission = true,
                    onPermissionResult = {},
                    placeholder = "How can I help"
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help").assertExists()
    }

    @Test
    fun userInput_withPermission_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                UserInput(
                    inputState = UserInputState.Empty,
                    onTextChange = {},
                    onMicEvent = {},
                    onSend = {},
                    hasAudioPermission = true,
                    onPermissionResult = {},
                    placeholder = "Type here"
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun userInput_withoutPermission_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                UserInput(
                    inputState = UserInputState.Empty,
                    onTextChange = {},
                    onMicEvent = {},
                    onSend = {},
                    hasAudioPermission = false,
                    onPermissionResult = {},
                    placeholder = "Type here"
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
