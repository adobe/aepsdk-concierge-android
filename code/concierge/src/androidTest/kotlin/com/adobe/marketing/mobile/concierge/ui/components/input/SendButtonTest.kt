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
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
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
}
