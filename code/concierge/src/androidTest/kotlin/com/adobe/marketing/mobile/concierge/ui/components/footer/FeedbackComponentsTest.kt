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
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the FeedbackButtons composable (FeedbackComponents.kt).
 */
class FeedbackComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun feedbackButtons_displaysThumbsUpAndThumbsDown() {
        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackButtons(
                    interactionId = "test-id",
                    onFeedback = {},
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up")).assertIsDisplayed()
        composeTestRule.onNode(hasContentDescription("Thumbs down")).assertIsDisplayed()
    }

    @Test
    fun feedbackButtons_thumbsUp_triggersCallback() {
        var received: FeedbackEvent? = null

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackButtons(
                    interactionId = "id-1",
                    onFeedback = { received = it },
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up")).performClick()
        assert(received is FeedbackEvent.ThumbsUp)
        assert((received as FeedbackEvent.ThumbsUp).interactionId == "id-1")
    }

    @Test
    fun feedbackButtons_thumbsDown_triggersCallback() {
        var received: FeedbackEvent? = null

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackButtons(
                    interactionId = "id-2",
                    onFeedback = { received = it },
                    feedbackState = FeedbackState.None
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs down")).performClick()
        assert(received is FeedbackEvent.ThumbsDown)
        assert((received as FeedbackEvent.ThumbsDown).interactionId == "id-2")
    }

    @Test
    fun feedbackButtons_positiveState_displaysFilledThumbsUp() {
        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackButtons(
                    interactionId = "id",
                    onFeedback = {},
                    feedbackState = FeedbackState.Positive
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs up")).assertIsDisplayed()
    }

    @Test
    fun feedbackButtons_negativeState_displaysFilledThumbsDown() {
        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackButtons(
                    interactionId = "id",
                    onFeedback = {},
                    feedbackState = FeedbackState.Negative
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Thumbs down")).assertIsDisplayed()
    }
}
