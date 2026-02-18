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

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for ConciergeResponseText and related text composables (ConciergeTextComponents.kt).
 */
class ConciergeTextComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun conciergeResponseText_displaysPlainText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponseText(text = "Hello, world!")
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello, world!").assertIsDisplayed()
    }

    @Test
    fun conciergeResponseText_emptyText_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponseText(text = "")
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun conciergeResponseText_withMarkdown_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeResponseText(text = "**Bold** and *italic* text")
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun clickableText_withTextAlignCenter_rendersAndDisplaysText() {
        val annotatedText: AnnotatedString = buildAnnotatedString {
            append("Read our ")
            pushStringAnnotation("URL", "https://example.com/terms")
            append("Terms")
            pop()
            append(" for more.")
        }

        composeTestRule.setContent {
            ConciergeTheme {
                ClickableText(
                    text = annotatedText,
                    onLinkClick = {},
                    textAlign = TextAlign.Center
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Read our Terms for more.").assertIsDisplayed()
    }
}
