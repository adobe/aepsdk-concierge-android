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

package com.adobe.marketing.mobile.concierge.ui.components.overlay

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ErrorOverlay composable.
 * Tests error message display and dismiss functionality.
 */
class ErrorOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorOverlay_displaysErrorMessage() {
        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = "An error occurred",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("An error occurred")
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_displaysDismissText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = "Network error",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss")
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_displaysLongErrorMessage() {
        val longError = "This is a very long error message that describes in detail what went wrong with the operation and provides helpful context for debugging"

        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = longError,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(longError, substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_displaysShortErrorMessage() {
        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = "Error",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Error")
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_handlesEmptyErrorMessage() {
        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = "",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dismiss")
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_handlesSpecialCharacters() {
        val errorWithSpecialChars = "Error: Connection failed! (Code: 404) - Retry?"

        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = errorWithSpecialChars,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText(errorWithSpecialChars)
            .assertIsDisplayed()
    }

    @Test
    fun errorOverlay_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = "Test error",
                    onDismiss = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun errorOverlay_displaysMultilineErrorMessage() {
        val multilineError = """
            Error occurred during processing
            Please try again later
            Contact support if issue persists
        """.trimIndent()

        composeTestRule.setContent {
            ConciergeTheme {
                ErrorOverlay(
                    errorMessage = multilineError,
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Error occurred during processing", substring = true)
            .assertIsDisplayed()
    }
}
