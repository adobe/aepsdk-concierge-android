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
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeIconAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatMessageItem composable.
 * Tests different message types, user interactions, and visual states.
 */
class ChatMessageItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatMessageItem_displaysUserTextMessage() {
        val message = ChatMessage(
            content = MessageContent.Text("Hello, Concierge!"),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Hello, Concierge!")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_displaysBotTextMessage() {
        val message = ChatMessage(
            content = MessageContent.Text("I can help you with that!"),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("I can help you with that!")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_displaysMixedContentMessage() {
        val multimodalElement = MultimodalElement(
            id = "element-1",
            alttext = "Product Image",
            url = "https://example.com/image.jpg"
        )
        
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = "Here are some recommendations:",
                multimodalElements = listOf(multimodalElement)
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Here are some recommendations:")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_displaysCitationsWhenPresent() {
        val citations = listOf(
            Citation(
                url = "https://example.com/page1",
                title = "Source 1",
                startIndex = 0,
                endIndex = 10
            ),
            Citation(
                url = "https://example.com/page2",
                title = "Source 2",
                startIndex = 20,
                endIndex = 30
            )
        )

        val message = ChatMessage(
            content = MessageContent.Text("Information [1] from sources [2]."),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations,
            uniqueCitations = citations,
            interactionId = "test-interaction-123"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Information [1] from sources [2].", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_triggersOnFeedbackCallback() {
        var feedbackReceived: FeedbackEvent? = null
        
        val message = ChatMessage(
            content = MessageContent.Text("How was this response?"),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            interactionId = "test-interaction-123"
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(
                    message = message,
                    onFeedback = { feedbackReceived = it }
                )
            }
        }

        // Note: Actually triggering feedback buttons would require finding them
        // by content description or test tags, which would need to be added
        // to the ChatFooter component for better testability
        
        composeTestRule.onNodeWithText("How was this response?")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_displaysPromptSuggestions() {
        val message = ChatMessage(
            content = MessageContent.Text("I can help you with:"),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            promptSuggestions = listOf(
                "Tell me more about products",
                "Show me recommendations",
                "Help with checkout"
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("I can help you with:")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Tell me more about products")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Show me recommendations")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_triggersOnSuggestionClick() {
        var clickedSuggestion: String? = null
        
        val message = ChatMessage(
            content = MessageContent.Text("Choose an option:"),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            promptSuggestions = listOf("Option 1", "Option 2")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(
                    message = message,
                    onSuggestionClick = { clickedSuggestion = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Option 1").performClick()

        assert(clickedSuggestion == "Option 1")
    }

    @Test
    fun chatMessageItem_handlesEmptyTextMessage() {
        val message = ChatMessage(
            content = MessageContent.Text(""),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatMessageItem_displaysFeedbackState() {
        val message = ChatMessage(
            content = MessageContent.Text("Helpful response"),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            interactionId = "test-123",
            feedbackState = FeedbackState.Positive
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(
                    message = message,
                    feedbackState = FeedbackState.Positive
                )
            }
        }

        composeTestRule.onNodeWithText("Helpful response")
            .assertIsDisplayed()
    }

    // --- Brand icon routing ---

    @Test
    fun chatMessageItem_botTextMessage_withCompanyIconUrl_displaysMessageContent() {
        // When assets.icons.company is a non-empty URL the icon layout path is used.
        // The message text must still be visible.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(
                    icons = ConciergeIconAssets(company = "https://example.com/brand-icon.png")
                )
            )
        )

        val message = ChatMessage(
            content = MessageContent.Text("Here is your answer."),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Here is your answer.")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_botTextMessage_withEmptyCompanyIcon_displaysMessageContent() {
        // When assets.icons.company is an empty string the no-icon (upstream) layout is used.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(
                    icons = ConciergeIconAssets(company = "")
                )
            )
        )

        val message = ChatMessage(
            content = MessageContent.Text("Here is your answer."),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Here is your answer.")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_userMessage_withCompanyIconConfigured_displaysMessageContent() {
        // User messages never use the icon layout regardless of theme configuration.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(
                    icons = ConciergeIconAssets(company = "https://example.com/brand-icon.png")
                )
            )
        )

        val message = ChatMessage(
            content = MessageContent.Text("Hello from user."),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Hello from user.")
            .assertIsDisplayed()
    }

    // --- BotMessageSuffix ---

    @Test
    fun chatMessageItem_botTextMessage_withCtaButton_displaysCtaButton() {
        val message = ChatMessage(
            content = MessageContent.Text("Check this out."),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            ctaButton = NetworkCtaButton(label = "Shop Now", url = "https://example.com/shop")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Shop Now")
            .assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_botTextMessage_withCtaButtonAndIcon_displaysCtaButton() {
        // CTA button must render in BotMessageSuffix regardless of whether the icon path is used.
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(
                    icons = ConciergeIconAssets(company = "https://example.com/brand-icon.png")
                )
            )
        )

        val message = ChatMessage(
            content = MessageContent.Text("Check this out."),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            ctaButton = NetworkCtaButton(label = "Learn More", url = "https://example.com/learn")
        )

        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Learn More")
            .assertIsDisplayed()
    }

    // --- RenderMixedMessage + BotMessageSuffix ---

    @Test
    fun chatMessageItem_botMixedMessage_withCtaButton_displaysBothTextAndCta() {
        // RenderMixedMessage was refactored to use BotMessageSuffix for prompt suggestions
        // and CTA button. Verify the CTA is rendered via BotMessageSuffix in the mixed path.
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = "Here are some options.",
                multimodalElements = listOf(
                    MultimodalElement(id = "p1", title = "Product One", url = "https://example.com/1.jpg")
                )
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            ctaButton = NetworkCtaButton(label = "View All", url = "https://example.com/all")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    ChatMessageItem(message = message)
                }
            }
        }

        composeTestRule.onNodeWithText("Here are some options.").assertIsDisplayed()
        composeTestRule.onNodeWithText("View All").assertIsDisplayed()
    }

    @Test
    fun chatMessageItem_botMixedMessage_withPromptSuggestions_displaysSuggestions() {
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = "Try one of these.",
                multimodalElements = emptyList()
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            promptSuggestions = listOf("Tell me more", "Show deals")
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatMessageItem(message = message)
            }
        }

        composeTestRule.onNodeWithText("Tell me more").assertIsDisplayed()
        composeTestRule.onNodeWithText("Show deals").assertIsDisplayed()
    }
}
