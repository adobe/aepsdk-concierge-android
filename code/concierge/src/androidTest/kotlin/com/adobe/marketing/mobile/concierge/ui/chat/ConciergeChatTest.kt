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

package com.adobe.marketing.mobile.concierge.ui.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.test.ComposeTestUtils
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ConciergeChat composable (chat screen).
 * Tests the internal chat UI with direct state to cover ui.chat package without requiring ViewModel.
 */
class ConciergeChatTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun conciergeChat_displaysHeaderAndWelcomeCard() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ConciergeChat(
                        messages = emptyList(),
                        chatState = ChatScreenState.Idle(),
                        inputState = UserInputState.Empty,
                        hasAudioPermission = true,
                        showWelcomeCard = true,
                        welcomeConfig = WelcomeConfig(
                            welcomeHeader = "How can we help?",
                            subHeader = "Try asking:"
                        ),
                        isReturningUser = false,
                        onTextChanged = {},
                        onEvent = {},
                        onPermissionResult = {},
                        onClose = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Concierge").assertIsDisplayed()
        composeTestRule.onNodeWithText("How can we help?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Try asking:").assertIsDisplayed()
    }

    @Test
    fun conciergeChat_closeButton_triggersCallback() {
        var closeCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ConciergeChat(
                        messages = emptyList(),
                        chatState = ChatScreenState.Idle(),
                        inputState = UserInputState.Empty,
                        hasAudioPermission = true,
                        showWelcomeCard = true,
                        welcomeConfig = WelcomeConfig(),
                        isReturningUser = false,
                        onTextChanged = {},
                        onEvent = {},
                        onPermissionResult = {},
                        onClose = { closeCalled = true }
                    )
                }
            }
        }

        composeTestRule.onNode(hasContentDescription("Close chat")).performClick()
        assert(closeCalled)
    }

    @Test
    fun conciergeChat_withMessages_displaysMessageList() {
        val messages = listOf(
            ComposeTestUtils.createTextMessage("Hello", isFromUser = true),
            ComposeTestUtils.createTextMessage("Hi there!", isFromUser = false)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ConciergeChat(
                        messages = messages,
                        chatState = ChatScreenState.Idle(),
                        inputState = UserInputState.Empty,
                        hasAudioPermission = true,
                        showWelcomeCard = false,
                        welcomeConfig = WelcomeConfig(),
                        isReturningUser = true,
                        onTextChanged = {},
                        onEvent = {},
                        onPermissionResult = {},
                        onClose = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun conciergeChat_processingState_showsInputArea() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ConciergeChat(
                        messages = emptyList(),
                        chatState = ChatScreenState.Processing(),
                        inputState = UserInputState.Empty,
                        hasAudioPermission = true,
                        showWelcomeCard = false,
                        welcomeConfig = WelcomeConfig(),
                        isReturningUser = false,
                        onTextChanged = {},
                        onEvent = {},
                        onPermissionResult = {},
                        onClose = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Concierge").assertIsDisplayed()
    }

    @Test
    fun conciergeChat_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ConciergeChat(
                        messages = emptyList(),
                        chatState = ChatScreenState.Idle(),
                        inputState = UserInputState.Empty,
                        hasAudioPermission = true,
                        showWelcomeCard = true,
                        welcomeConfig = WelcomeConfig(),
                        isReturningUser = false,
                        onTextChanged = {},
                        onEvent = {},
                        onPermissionResult = {},
                        onClose = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
