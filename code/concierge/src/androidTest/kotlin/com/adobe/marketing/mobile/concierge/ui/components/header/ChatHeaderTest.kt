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

package com.adobe.marketing.mobile.concierge.ui.components.header

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatHeader composable.
 * ChatHeader displays a fixed title "Concierge", subtitle "Powered by Adobe", and a close button.
 */
class ChatHeaderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatHeader_displaysTitle() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatHeader(onClose = {})
            }
        }

        composeTestRule.onNodeWithText("Concierge")
            .assertIsDisplayed()
    }

    @Test
    fun chatHeader_displaysSubtitle() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatHeader(onClose = {})
            }
        }

        composeTestRule.onNodeWithText("Powered by Adobe")
            .assertIsDisplayed()
    }

    @Test
    fun chatHeader_displaysCloseButton() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatHeader(onClose = {})
            }
        }

        composeTestRule.onNode(hasContentDescription("Close chat"))
            .assertIsDisplayed()
    }

    @Test
    fun chatHeader_closeButtonTriggersCallback() {
        var closeCalled = false

        composeTestRule.setContent {
            ConciergeTheme {
                ChatHeader(onClose = { closeCalled = true })
            }
        }

        composeTestRule.onNode(hasContentDescription("Close chat"))
            .performClick()

        assert(closeCalled)
    }

    @Test
    fun chatHeader_multipleClicksOnCloseButton() {
        var clickCount = 0

        composeTestRule.setContent {
            ConciergeTheme {
                ChatHeader(onClose = { clickCount++ })
            }
        }

        val closeButton = composeTestRule.onNode(hasContentDescription("Close chat"))
        closeButton.performClick()
        closeButton.performClick()
        closeButton.performClick()

        assert(clickCount == 3)
    }
}
