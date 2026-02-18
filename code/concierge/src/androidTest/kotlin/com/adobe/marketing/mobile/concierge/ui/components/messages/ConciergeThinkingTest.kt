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
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ConciergeThinking composable.
 * Tests the display and animation behavior of the thinking indicator.
 */
class ConciergeThinkingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun conciergeThinking_displaysThinkingText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeThinking()
            }
        }

        composeTestRule.onNodeWithText("Thinking", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun conciergeThinking_isRenderedWithoutCrashing() {
        var renderSuccessful = false
        
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeThinking()
            }
            renderSuccessful = true
        }

        assert(renderSuccessful)
    }

    @Test
    fun conciergeThinking_canBeRenderedMultipleTimes() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeThinking()
                ConciergeThinking()
                ConciergeThinking()
            }
        }

        // This test ensures the animation doesn't cause conflicts
        composeTestRule.waitForIdle()
    }
}
