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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the ExpandedCitations composable.
 * ExpandedCitations displays a list of citations when expanded; each item shows index and title.
 */
class ExpandedCitationsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun expandedCitations_whenExpanded_displaysCitationTitles() {
        val citations = listOf(
            Citation(
                url = "https://example.com/1",
                title = "First Source",
                startIndex = 0,
                endIndex = 10
            ),
            Citation(
                url = "https://example.com/2",
                title = "Second Source",
                startIndex = 11,
                endIndex = 20
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ExpandedCitations(
                    citations = citations,
                    uniqueCitations = citations,
                    expanded = true
                )
            }
        }

        composeTestRule.onNodeWithText("First Source")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Source")
            .assertIsDisplayed()
    }

    @Test
    fun expandedCitations_displaysCitationNumbers() {
        val citations = listOf(
            Citation(
                url = "https://example.com/a",
                title = "Source A",
                startIndex = 0,
                endIndex = 5
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ExpandedCitations(
                    citations = citations,
                    uniqueCitations = citations,
                    expanded = true
                )
            }
        }

        composeTestRule.onNodeWithText("1. ")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Source A")
            .assertIsDisplayed()
    }

    @Test
    fun expandedCitations_whenCollapsed_doesNotDisplayContent() {
        val citations = listOf(
            Citation(
                url = "https://example.com",
                title = "Hidden Source",
                startIndex = 0,
                endIndex = 10
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ExpandedCitations(
                    citations = citations,
                    uniqueCitations = citations,
                    expanded = false
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hidden Source")
            .assertDoesNotExist()
    }

    @Test
    fun expandedCitations_singleCitation_displaysCorrectly() {
        val citations = listOf(
            Citation(
                url = "https://example.com/single",
                title = "Single Citation",
                startIndex = 0,
                endIndex = 15
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                ExpandedCitations(
                    citations = citations,
                    uniqueCitations = citations,
                    expanded = true
                )
            }
        }

        composeTestRule.onNodeWithText("Single Citation")
            .assertIsDisplayed()
    }

    @Test
    fun expandedCitations_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                ExpandedCitations(
                    citations = emptyList(),
                    expanded = true
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
