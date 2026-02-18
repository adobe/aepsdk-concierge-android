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
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the CircularCitation composable.
 * CircularCitation displays a citation number in a circular badge and triggers onClick.
 */
class CircularCitationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun circularCitation_displaysNumber() {
        composeTestRule.setContent {
            ConciergeTheme {
                CircularCitation(
                    citationNumber = 1,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("1")
            .assertIsDisplayed()
    }

    @Test
    fun circularCitation_displaysMultipleDigits() {
        composeTestRule.setContent {
            ConciergeTheme {
                CircularCitation(
                    citationNumber = 12,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("12")
            .assertIsDisplayed()
    }

    @Test
    fun circularCitation_click_triggersCallback() {
        var clicked = false

        composeTestRule.setContent {
            ConciergeTheme {
                CircularCitation(
                    citationNumber = 1,
                    onClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithText("1")
            .performClick()

        assert(clicked)
    }

    @Test
    fun circularCitation_multipleClicks_triggersMultipleCallbacks() {
        var clickCount = 0

        composeTestRule.setContent {
            ConciergeTheme {
                CircularCitation(
                    citationNumber = 2,
                    onClick = { clickCount++ }
                )
            }
        }

        val node = composeTestRule.onNodeWithText("2")
        node.performClick()
        node.performClick()

        assert(clickCount == 2)
    }

    @Test
    fun circularCitation_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CircularCitation(
                    citationNumber = 1,
                    onClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
