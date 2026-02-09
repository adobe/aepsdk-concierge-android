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

package com.adobe.marketing.mobile.concierge.ui.components.welcome

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the SuggestedPromptItem composable.
 * SuggestedPromptItem displays a single suggested prompt with optional image/icon and triggers onClick.
 */
class SuggestedPromptItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun suggestedPromptItem_displaysText() {
        val prompt = SuggestedPrompt(text = "Find a product")

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    SuggestedPromptItem(
                        prompt = prompt,
                        onClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Find a product")
            .assertIsDisplayed()
    }

    @Test
    fun suggestedPromptItem_click_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    SuggestedPromptItem(
                        prompt = SuggestedPrompt(text = "Track my order"),
                        onClick = { clicked = true }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Track my order")
            .performClick()

        assert(clicked)
    }

    @Test
    fun suggestedPromptItem_multipleClicks_triggersMultipleCallbacks() {
        var clickCount = 0

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    SuggestedPromptItem(
                        prompt = SuggestedPrompt(text = "Get help"),
                        onClick = { clickCount++ }
                    )
                }
            }
        }

        val node = composeTestRule.onNodeWithText("Get help")
        node.performClick()
        node.performClick()
        node.performClick()

        assert(clickCount == 3)
    }

    @Test
    fun suggestedPromptItem_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    SuggestedPromptItem(
                        prompt = SuggestedPrompt(text = "Custom prompt text"),
                        onClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }
}
