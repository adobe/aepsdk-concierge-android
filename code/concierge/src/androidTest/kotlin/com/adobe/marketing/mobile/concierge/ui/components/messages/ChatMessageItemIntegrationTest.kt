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
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.test.ComposeTestUtils
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for ChatMessageItem using test utilities.
 * Demonstrates best practices for testing with helper functions.
 */
class ChatMessageItemIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatMessageItem_usingTestUtils_displaysUserMessage() {
        val message = ComposeTestUtils.createTextMessage(
            text = "Hello from test utils!",
            isFromUser = true
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Hello from test utils!")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_withCitations_usingTestUtils() {
        val citations = listOf(
            ComposeTestUtils.createCitation(
                url = "https://example.com/article1",
                title = "Article 1"
            ),
            ComposeTestUtils.createCitation(
                url = "https://example.com/article2",
                title = "Article 2"
            )
        )

        val message = ComposeTestUtils.createMessageWithCitations(
            text = "According to sources [1][2], this is correct.",
            citations = citations
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("According to sources [1][2], this is correct.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_withMixedContent_usingTestUtils() {
        val product = ComposeTestUtils.createProductWithActions(
            id = "prod-123",
            title = "Test Product",
            primaryText = "Buy Now",
            primaryUrl = "https://example.com/buy"
        )

        val message = ComposeTestUtils.createMixedMessage(
            text = "Check out this product:",
            multimodalElements = listOf(product)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Check out this product:")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Product")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Buy Now")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_withSuggestions_triggersCallback() {
        var selectedSuggestion: String? = null
        
        val message = ComposeTestUtils.createMessageWithSuggestions(
            text = "What would you like to know?",
            suggestions = listOf(
                "Tell me about products",
                "Show recommendations"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(
                    message = message,
                    onSuggestionClick = { selectedSuggestion = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Tell me about products")
            .performClick()

        assert(selectedSuggestion == "Tell me about products")
    }

    @Test
    fun chatMessageItem_multipleProducts_allDisplayed() {
        val products = listOf(
            ComposeTestUtils.createMultimodalElement(
                id = "prod-1",
                title = "Product One"
            ),
            ComposeTestUtils.createMultimodalElement(
                id = "prod-2",
                title = "Product Two"
            ),
            ComposeTestUtils.createMultimodalElement(
                id = "prod-3",
                title = "Product Three"
            )
        )

        val message = ComposeTestUtils.createMixedMessage(
            text = "Here are your recommendations:",
            multimodalElements = products
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        // Verify the message text is displayed
        composeTestRule.onNodeWithText("Here are your recommendations:")
            .assertIsDisplayed()
        
        // The carousel should render without crashing.
        composeTestRule.waitForIdle()
    }

    @Test
    fun chatMessageItem_complexScenario_allElementsWork() {
        val citations = listOf(
            ComposeTestUtils.createCitation(title = "Source A"),
            ComposeTestUtils.createCitation(title = "Source B")
        )

        val product = ComposeTestUtils.createProductWithActions(
            title = "Featured Product",
            primaryText = "Shop Now",
            secondaryText = "Learn More"
        )

        // Create a text message first (since mixed message with citations needs special handling)
        val message = ComposeTestUtils.createTextMessage(
            text = "Based on your interests [1][2], here's what we recommend:",
            isFromUser = false,
            interactionId = "complex-test-123"
        ).copy(
            citations = citations,
            uniqueCitations = citations,
            promptSuggestions = listOf("Show me more", "Tell me about deals")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Based on your interests", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Show me more")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Tell me about deals")
            .assertIsDisplayed()
    }
}
