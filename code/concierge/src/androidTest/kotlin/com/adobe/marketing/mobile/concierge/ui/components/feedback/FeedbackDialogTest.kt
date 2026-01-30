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

package com.adobe.marketing.mobile.concierge.ui.components.feedback

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the FeedbackDialog composable.
 * Tests feedback dialog display and submission.
 */
class FeedbackDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun feedbackDialog_positive_displaysTitle() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Your feedback is appreciated")
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_positive_displaysPositiveQuestion() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What went well? Select all that apply.")
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_negative_displaysNegativeQuestion() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.NEGATIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("What went wrong? Select all that apply.")
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_displaysPositiveCategories() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Helpful and relevant recommendations")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear and easy to understand")
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_displaysNegativeCategories() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.NEGATIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Didn't understand my request")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Unhelpful or irrelevant information")
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_displaysNotesField() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Notes")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Add any additional comments...", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun feedbackDialog_submitButton_initiallyDisabled() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Submit")
            .assertIsNotEnabled()
    }

    @Test
    fun feedbackDialog_categoryClick_enablesSubmit() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Helpful and relevant recommendations")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun feedbackDialog_cancelButton_triggersCallback() {
        var dismissed = false
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = { dismissed = true },
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel")
            .performClick()

        assert(dismissed)
    }

    @Test
    fun feedbackDialog_submitWithCategory_triggersCallback() {
        var submittedFeedback: Feedback? = null
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = { submittedFeedback = it }
                )
            }
        }

        composeTestRule.onNodeWithText("Clear and easy to understand")
            .performClick()

        composeTestRule.onNodeWithText("Submit")
            .performClick()

        assert(submittedFeedback != null)
        assert(submittedFeedback?.selectedCategories?.contains("Clear and easy to understand") == true)
    }

    @Test
    fun feedbackDialog_notesInput_enablesSubmit() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Add any additional comments...", substring = true)
            .performTextInput("Great response!")

        composeTestRule.waitForIdle()
    }

    @Test
    fun feedbackDialog_multipleCategories_canBeSelected() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.NEGATIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Didn't understand my request")
            .performClick()
        composeTestRule.onNodeWithText("Unhelpful or irrelevant information")
            .performClick()

        composeTestRule.waitForIdle()
    }

    @Test
    fun feedbackDialog_rendersWithoutCrashing() {
        val feedback = Feedback(
            interactionId = "test-id",
            feedbackType = FeedbackType.POSITIVE
        )

        composeTestRule.setContent {
            ConciergeTheme {
                FeedbackDialog(
                    feedback = feedback,
                    onDismiss = {},
                    onSubmit = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
