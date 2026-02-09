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

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the SourcesAccordionButton composable (SourcesComponents.kt).
 */
class SourcesComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun sourcesAccordionButton_whenCollapsed_displaysExpandContentDescription() {
        composeTestRule.setContent {
            ConciergeTheme {
                SourcesAccordionButton(
                    expanded = false,
                    onExpandedChange = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Expand sources")).assertExists()
    }

    @Test
    fun sourcesAccordionButton_whenExpanded_displaysCollapseContentDescription() {
        composeTestRule.setContent {
            ConciergeTheme {
                SourcesAccordionButton(
                    expanded = true,
                    onExpandedChange = {}
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Collapse sources")).assertExists()
    }

    @Test
    fun sourcesAccordionButton_click_togglesExpanded() {
        var expanded = false

        composeTestRule.setContent {
            ConciergeTheme {
                SourcesAccordionButton(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                )
            }
        }

        composeTestRule.onNode(hasContentDescription("Expand sources")).performClick()
        composeTestRule.waitForIdle()
        assert(expanded)
    }

    @Test
    fun sourcesAccordionButton_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                SourcesAccordionButton(
                    expanded = false,
                    onExpandedChange = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
