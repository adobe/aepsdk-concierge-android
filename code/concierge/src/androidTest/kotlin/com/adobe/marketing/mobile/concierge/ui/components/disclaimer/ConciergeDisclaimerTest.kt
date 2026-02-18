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

package com.adobe.marketing.mobile.concierge.ui.components.disclaimer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerConfig
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for ConciergeDisclaimer composable.
 */
class ConciergeDisclaimerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disclaimer_displaysPlainTextWhenNoLinks() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(
                        text = "AI responses may be inaccurate.",
                        links = emptyList()
                    )
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AI responses may be inaccurate.").assertIsDisplayed()
    }

    @Test
    fun disclaimer_displaysTextWithLinkPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(
                        text = "Check our {Terms} for more.",
                        links = listOf(DisclaimerLink("Terms", "https://example.com/terms"))
                    )
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Check our Terms for more.").assertIsDisplayed()
    }

    @Test
    fun disclaimer_doesNotDisplayWhenConfigNull_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(disclaimerConfig = null)
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun disclaimer_doesNotDisplayWhenTextBlank() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(text = "  ", links = null)
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
