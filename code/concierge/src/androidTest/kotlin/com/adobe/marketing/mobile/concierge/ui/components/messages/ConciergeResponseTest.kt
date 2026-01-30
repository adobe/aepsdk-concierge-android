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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ConciergeResponse composable.
 * Tests response rendering with markdown and citations.
 */
class ConciergeResponseTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun conciergeResponse_displaysSimpleText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "Hello, how can I help you today?"
                )
            }
        }

        composeTestRule.onNodeWithText("Hello, how can I help you today?")
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_emptyText_showsThinking() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = ""
                )
            }
        }

        composeTestRule.onNodeWithText("Thinking", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_displaysMarkdownBoldText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "This is **bold** text"
                )
            }
        }

        composeTestRule.onNodeWithText("This is bold text", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_displaysMarkdownItalicText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "This is *italic* text"
                )
            }
        }

        composeTestRule.onNodeWithText("This is italic text", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_withSources_rendersText() {
        val sources = listOf(
            Citation(
                title = "Source 1",
                url = "https://example.com/1",
                startIndex = 0,
                endIndex = 10
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "Based on research, this is correct information.",
                    sources = sources
                )
            }
        }

        composeTestRule.onNodeWithText("Based on research, this is correct information.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_multilineText_displaysAll() {
        val text = """
            Line 1: Introduction
            Line 2: Details
            Line 3: Conclusion
        """.trimIndent()

        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(text = text)
            }
        }

        composeTestRule.onNodeWithText("Line 1: Introduction", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_longText_displaysCorrectly() {
        val longText = "This is a very long response that contains multiple sentences. " +
                "It should display correctly without any truncation issues. " +
                "The text wrapping should work properly across multiple lines."

        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(text = longText)
            }
        }

        composeTestRule.onNodeWithText(longText, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_withSpecialCharacters_displaysCorrectly() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "Price: $99.99! (50% off) - Limited time offer?"
                )
            }
        }

        composeTestRule.onNodeWithText("Price: \$99.99! (50% off) - Limited time offer?")
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "Test response"
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun conciergeResponse_textWithNumbers_displaysCorrectly() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "The answer is 42 based on calculation of 21 * 2."
                )
            }
        }

        composeTestRule.onNodeWithText("The answer is 42 based on calculation of 21 * 2.")
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_textWithEmojis_displaysCorrectly() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "Great product! 👍 Highly recommended ⭐⭐⭐⭐⭐"
                )
            }
        }

        composeTestRule.onNodeWithText("Great product! 👍 Highly recommended ⭐⭐⭐⭐⭐")
            .assertIsDisplayed()
    }

    @Test
    fun conciergeResponse_complexMarkdown_rendersCorrectly() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponse(
                    text = "This has **bold**, *italic*, and **_both_** formatting."
                )
            }
        }

        composeTestRule.waitForIdle()
        // The text should be rendered with the markdown markers still present in the text content
        composeTestRule.onNodeWithText("This has", substring = true)
            .assertIsDisplayed()
    }
}
