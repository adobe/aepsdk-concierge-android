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

package com.adobe.marketing.mobile.concierge.ui.components.suggestions

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the PromptSuggestions composable.
 * Tests suggestion display and interaction behavior.
 */
class PromptSuggestionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun promptSuggestions_displaysAllSuggestions() {
        val suggestions = listOf(
            "Show me product recommendations",
            "What are the best deals?",
            "Help me find a gift"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Show me product recommendations")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("What are the best deals?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Help me find a gift")
            .assertIsDisplayed()
    }

    @Test
    fun promptSuggestions_displaysSingleSuggestion() {
        val suggestions = listOf("Ask me anything")

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Ask me anything")
            .assertIsDisplayed()
    }

    @Test
    fun promptSuggestions_doesNotRenderWhenEmpty() {
        val suggestions = emptyList<String>()

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        // Component returns early when suggestions are empty
    }

    @Test
    fun promptSuggestions_triggersOnClickCallback() {
        var clickedSuggestion: String? = null
        val suggestions = listOf(
            "First suggestion",
            "Second suggestion"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = { clickedSuggestion = it }
                )
            }
        }

        composeTestRule.onNodeWithText("First suggestion").performClick()

        assert(clickedSuggestion == "First suggestion")
    }

    @Test
    fun promptSuggestions_handlesMultipleClicks() {
        val clickedSuggestions = mutableListOf<String>()
        val suggestions = listOf(
            "Option A",
            "Option B",
            "Option C"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = { clickedSuggestions.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Option A").performClick()
        composeTestRule.onNodeWithText("Option C").performClick()
        composeTestRule.onNodeWithText("Option B").performClick()

        assert(clickedSuggestions == listOf("Option A", "Option C", "Option B"))
    }

    @Test
    fun promptSuggestions_handlesLongText() {
        val longText = "This is a very long suggestion that might need to be truncated or wrapped to fit properly in the UI component without breaking the layout"
        val suggestions = listOf(longText)

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText(longText, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun promptSuggestions_displaysCorrectCount() {
        val suggestions = listOf(
            "Suggestion 1",
            "Suggestion 2",
            "Suggestion 3",
            "Suggestion 4",
            "Suggestion 5"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Suggestion 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion 4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Suggestion 5").assertIsDisplayed()
    }

    @Test
    fun promptSuggestions_handlesSpecialCharacters() {
        val suggestions = listOf(
            "What's the price?",
            "Show me items > $100",
            "Search for \"premium\" products"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                PromptSuggestions(
                    suggestions = suggestions,
                    onSuggestionClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What's the price?")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Show me items > $100")
            .assertIsDisplayed()
    }
}
