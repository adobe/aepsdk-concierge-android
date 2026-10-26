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
import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerConfig
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

/**
 * UI tests for ConciergeDisclaimer composable.
 */
class ConciergeDisclaimerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun disclaimer_themeResolvesSeparateBodyAndLinkColors_whiteLinks() {
        assertThemedColors("#ADADAD", "#FFFFFF")
    }

    @Test
    fun disclaimer_themeResolvesSeparateBodyAndLinkColors_darkLinks() {
        assertThemedColors("#757575", "#131313")
    }

    private fun assertThemedColors(disclaimerColor: String, linkColor: String) {
        val json = """
            {"theme": {"--disclaimer-color": "$disclaimerColor", "--color-text": "$linkColor"}}
        """.trimIndent()
        val theme = ConciergeThemeData(
            config = requireNotNull(ThemeParser.parseThemeJson(json)),
            tokens = requireNotNull(ThemeParser.parseThemeTokens(json))
        )
        lateinit var resolvedStyle: ConciergeStyles.DisclaimerStyle
        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                resolvedStyle = ConciergeStyles.disclaimerStyle
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(Color(android.graphics.Color.parseColor(disclaimerColor)), resolvedStyle.textColor)
            assertEquals(Color(android.graphics.Color.parseColor(linkColor)), resolvedStyle.linkTextColor)
        }
    }

    @Test
    fun disclaimer_withoutCustomThemeUsesResolvedPaletteForLinks() {
        lateinit var resolvedStyle: ConciergeStyles.DisclaimerStyle
        var expectedColor = Color.Unspecified
        composeTestRule.setContent {
            ConciergeTheme {
                resolvedStyle = ConciergeStyles.disclaimerStyle
                expectedColor = ConciergeTheme.colors.onSurface
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(expectedColor, resolvedStyle.linkTextColor)
        }
    }

    @Test
    fun disclaimer_displaysPlainTextWhenNoLinks() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(
                        text = "AI responses may be inaccurate.",
                        links = emptyList()
                    ),
                    onEvent = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("AI responses may be inaccurate.").assertIsDisplayed()
    }

    @Test
    fun disclaimer_unmatchedPlaceholderDisplaysFormattedPlainText() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(text = "Read {Privacy}.", links = emptyList()),
                    onEvent = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Read Privacy.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Read {Privacy}.").assertDoesNotExist()
    }

    @Test
    fun disclaimer_displaysTextWithLinkPlaceholder() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(
                        text = "Check our {Terms} for more.",
                        links = listOf(DisclaimerLink("Terms", "https://example.com/terms"))
                    ),
                    onEvent = {}
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
                ConciergeDisclaimer(disclaimerConfig = null, onEvent = {})
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun disclaimer_doesNotDisplayWhenTextBlank() {
        composeTestRule.setContent {
            ConciergeTheme {
                ConciergeDisclaimer(
                    disclaimerConfig = DisclaimerConfig(text = "  ", links = null),
                    onEvent = {}
                )
            }
        }

        composeTestRule.waitForIdle()
    }
}
