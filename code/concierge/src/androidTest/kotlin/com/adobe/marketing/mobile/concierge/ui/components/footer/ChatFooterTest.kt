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

package com.adobe.marketing.mobile.concierge.ui.components.footer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ChatFooter composable.
 * Tests footer display with citations and feedback buttons.
 */
class ChatFooterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun chatFooter_withCitations_displaysSourcesButton() {
        val citations = listOf(
            Citation(
                title = "Source 1",
                url = "https://example.com/1",
                startIndex = 0,
                endIndex = 10
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = citations,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNodeWithText("Sources", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_withInteractionIdAndSseComplete_displaysFeedbackButtons() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Thumbs down"))
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_withInteractionIdButSseNotComplete_hidesFeedbackButtons() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = false,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .assertDoesNotExist()
        composeTestRule.onNode(hasContentDescription("Thumbs down"))
            .assertDoesNotExist()
    }

    @Test
    fun chatFooter_withCitationsAndFeedback_displaysBoth() {
        val citations = listOf(
            Citation(
                title = "Source",
                url = "https://example.com",
                startIndex = 0,
                endIndex = 5
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = citations,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNodeWithText("Sources", substring = true)
            .assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_thumbsUpClick_triggersCallback() {
        var feedbackEvent: FeedbackEvent? = null

        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = { feedbackEvent = it },
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .performClick()

        assert(feedbackEvent is FeedbackEvent.ThumbsUp)
    }

    @Test
    fun chatFooter_thumbsDownClick_triggersCallback() {
        var feedbackEvent: FeedbackEvent? = null

        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = { feedbackEvent = it },
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs down"))
            .performClick()

        assert(feedbackEvent is FeedbackEvent.ThumbsDown)
    }

    @Test
    fun chatFooter_withoutCitationsOrInteractionId_rendersEmpty() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = null,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun chatFooter_emptyCitationsList_doesNotDisplaySources() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = emptyList(),
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_multipleCitations_displaysSourcesButton() {
        val citations = listOf(
            Citation(title = "Source 1", url = "https://example.com/1", startIndex = 0, endIndex = 5),
            Citation(title = "Source 2", url = "https://example.com/2", startIndex = 10, endIndex = 15),
            Citation(title = "Source 3", url = "https://example.com/3", startIndex = 20, endIndex = 25)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = citations,
                    interactionId = null,
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNodeWithText("Sources", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_feedbackStatePositive_displaysFilledThumbsUp() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.Positive
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up"))
            .assertIsDisplayed()
    }

    @Test
    fun chatFooter_feedbackStateNegative_displaysFilledThumbsDown() {
        composeTestRule.setContent {
            ConciergeTheme {
                ChatFooter(
                    citations = null,
                    interactionId = "test-id",
                    sseComplete = true,
                    onFeedback = {},
                    feedbackState = FeedbackState.Negative
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs down"))
            .assertIsDisplayed()
    }
}
