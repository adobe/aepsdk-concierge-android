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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatTextField composable.
 * ChatTextField is the text input for chat messages with placeholder and value binding.
 */
class ChatTextFieldTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatTextField_displaysPlaceholderWhenEmpty() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatTextField(
                    value = "",
                    onValueChange = {},
                    isEnabled = true,
                    placeholder = "Type a message..."
                )
            }
        }

        composeTestRule.onNodeWithText("Type a message...")
            .assertExists()
    }

    @Test
    fun chatTextField_displaysValue() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatTextField(
                    value = "Hello, world",
                    onValueChange = {},
                    isEnabled = true,
                    placeholder = "Placeholder"
                )
            }
        }

        composeTestRule.onNodeWithText("Hello, world")
            .assertExists()
    }

    @Test
    fun chatTextField_acceptsTextInput() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatTextField(
                    value = "",
                    onValueChange = {},
                    isEnabled = true,
                    placeholder = "Type here"
                )
            }
        }

        composeTestRule.onNodeWithTag("ChatTextField")
            .performTextInput("test input")

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatTextField_customPlaceholder_isDisplayed() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatTextField(
                    value = "",
                    onValueChange = {},
                    isEnabled = true,
                    placeholder = "How can I help"
                )
            }
        }

        composeTestRule.onNodeWithText("How can I help")
            .assertExists()
    }

    @Test
    fun chatTextField_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatTextField(
                    value = "",
                    onValueChange = {},
                    isEnabled = true
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
