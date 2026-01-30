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
 * Tests welcome screen display for new and returning users.
 */
class WelcomeCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun welcomeCard_displaysFirstTimeWelcome() {
        val config = WelcomeConfig(
            brandName = "TestBrand",
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
        composeTestRule.onNodeWithText("Welcome to [TestBrand] concierge!", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("How can we help?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Try asking:")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_displaysReturningUserWelcome() {
        val config = WelcomeConfig(
            brandName = "TestBrand",
            returningUserWelcomeMessage = "Welcome back!",
            welcomeHeader = "How can we help today?"
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

        composeTestRule.onNodeWithText("Welcome back!")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_displaysSuggestedPrompts() {
        val config = WelcomeConfig(
            brandName = "TestBrand",
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
            brandName = "TestBrand",
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
    fun welcomeCard_withoutPrompts_displaysWelcomeOnly() {
        val config = WelcomeConfig(
            brandName = "TestBrand",
            welcomeHeader = "Welcome to support",
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
    }

    @Test
    fun welcomeCard_customFirstTimeMessage_isDisplayed() {
        val config = WelcomeConfig(
            brandName = "TestBrand",
            firstTimeWelcomeMessage = "Hello! Welcome to our service!",
            welcomeHeader = "How can we assist?"
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

        composeTestRule.onNodeWithText("Hello! Welcome to our service!")
            .assertIsDisplayed()
    }

    @Test
    fun welcomeCard_multiplePromptClicks_triggerMultipleCallbacks() {
        val clickedPrompts = mutableListOf<String>()
        
        val config = WelcomeConfig(
            brandName = "TestBrand",
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
        val config = WelcomeConfig(brandName = "Test")

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
}
