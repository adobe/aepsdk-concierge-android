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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import com.adobe.marketing.mobile.concierge.ui.test.ComposeTestUtils
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the MessageList composable.
 */
class MessageListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun messageList_displaysMessages() {
        val messages = listOf(
            ComposeTestUtils.createTextMessage("Hello", isFromUser = true),
            ComposeTestUtils.createTextMessage("Hi there!", isFromUser = false)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun messageList_emptyList_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = emptyList())
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun messageList_singleMessage_displaysContent() {
        val messages = listOf(
            ComposeTestUtils.createTextMessage("Single message", isFromUser = false)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Single message").assertIsDisplayed()
    }
}
