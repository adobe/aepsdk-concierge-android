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
import com.adobe.marketing.mobile.concierge.ui.config.SuggestedPrompt
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the WelcomeCard composable.
 * WelcomeCard displays welcomeHeader, subHeader, and suggested prompts (no brand name or placeholder replacement).
 */
class WelcomeCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeCard_displaysHeaderAndSubHeader() {
        val config = WelcomeConfig(
            welcomeHeader = "How can we help?",
            subHeader = "Try asking:"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How can we help?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Try asking:")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_displaysWithReturningUser() {
        val config = WelcomeConfig(
            welcomeHeader = "How can we help today?",
            subHeader = "Choose an option below."
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = true,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("How can we help today?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose an option below.")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_displaysSuggestedPrompts() {
        val config = WelcomeConfig(
            welcomeHeader = "Welcome",
            subHeader = "Try these:",
            suggestedPrompts = listOf(
                SuggestedPrompt(text = "Find a product"),
                SuggestedPrompt(text = "Track my order"),
                SuggestedPrompt(text = "Get help")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Find a product")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Track my order")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Get help")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_promptClick_triggersCallback() {
        var clickedPrompt: String? = null

        val config = WelcomeConfig(
            suggestedPrompts = listOf(
                SuggestedPrompt(text = "Show me products")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = { clickedPrompt = it }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Show me products")
            .performClick()

        assert(clickedPrompt == "Show me products")
    }

    @Test
    fun welcomeCard_withoutPrompts_displaysHeaderAndSubHeaderOnly() {
        val config = WelcomeConfig(
            welcomeHeader = "Welcome to support",
            subHeader = "How can we assist you?",
            suggestedPrompts = emptyList()
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Welcome to support")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("How can we assist you?")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_customHeader_isDisplayed() {
        val config = WelcomeConfig(
            welcomeHeader = "How can we assist?",
            subHeader = "Select a topic or type your question."
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("How can we assist?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Select a topic or type your question.")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_multiplePromptClicks_triggerMultipleCallbacks() {
        val clickedPrompts = mutableListOf<String>()

        val config = WelcomeConfig(
            welcomeHeader = "Welcome",
            subHeader = "Try:",
            suggestedPrompts = listOf(
                SuggestedPrompt(text = "Prompt A"),
                SuggestedPrompt(text = "Prompt B")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = { clickedPrompts.add(it) }
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Prompt A").performClick()
        composeTestRule.onNodeWithText("Prompt B").performClick()

        assert(clickedPrompts == listOf("Prompt A", "Prompt B"))
    }

    @Test
    fun welcomeCard_rendersWithoutCrashing() {
        val config = WelcomeConfig(
            welcomeHeader = "Welcome",
            subHeader = "Get started below."
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun welcomeCard_withNoThemeAndDarkMode_displaysWithDefaultDarkStyling() {
        val config = WelcomeConfig(
            welcomeHeader = "Welcome",
            subHeader = "Choose an option.",
            suggestedPrompts = listOf(
                SuggestedPrompt(text = "Option A"),
                SuggestedPrompt(text = "Option B")
            )
        )

        composeTestRule.setContent {
            ConciergeTheme(darkTheme = true, theme = null) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    WelcomeCard(
                        config = config,
                        isReturningUser = false,
                        onPromptClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Choose an option.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Option A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Option B").assertIsDisplayed()
    }
}
