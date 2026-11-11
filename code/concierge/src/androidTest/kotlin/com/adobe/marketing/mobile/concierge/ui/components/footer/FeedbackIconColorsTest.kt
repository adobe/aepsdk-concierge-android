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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.DarkConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.LightConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedbackIconColorsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private data class ThemeCase(
        val name: String,
        val dark: Boolean,
        val theme: ConciergeThemeData?,
        val text: Color,
        val primary: Color
    ) {
        val background: Color get() = if (dark) Color.Black else Color.White
    }

    private val cases = listOf(
        ThemeCase(
            "default light", false, null,
            LightConciergeColors.conciergeMessageText ?: LightConciergeColors.onSurface,
            LightConciergeColors.primary
        ),
        ThemeCase(
            "default dark", true, null,
            DarkConciergeColors.conciergeMessageText ?: DarkConciergeColors.onSurface,
            DarkConciergeColors.primary
        ),
        customCase("custom light", false, "#006600", Color(0xFF006600)),
        customCase("custom dark", true, "#33FF00", Color(0xFF33FF00)),
        customCase("transparent text", false, "#3300FF00", Color(0x3300FF00))
    )

    private fun customCase(name: String, dark: Boolean, textHex: String, text: Color): ThemeCase {
        val json = """{"theme":{"--color-primary":"#B366CC","--message-concierge-text":"$textHex"}}"""
        return ThemeCase(
            name, dark,
            ConciergeThemeData(
                config = requireNotNull(ThemeParser.parseThemeJson(json)),
                tokens = requireNotNull(ThemeParser.parseThemeTokens(json))
            ),
            text, Color(0xFFB366CC)
        )
    }

    @Composable
    private fun TestTheme(case: ThemeCase, content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (case.dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        CompositionLocalProvider(LocalConfiguration provides configuration) {
            ConciergeTheme(darkTheme = case.dark, theme = case.theme, content = content)
        }
    }

    @Test
    fun style_usesPrimaryAndFixedGrayAcrossThemes() {
        val current = mutableStateOf(cases.first())
        lateinit var style: ConciergeStyles.FeedbackButtonsStyle
        composeTestRule.setContent {
            TestTheme(current.value) {
                style = ConciergeStyles.feedbackButtonsStyle
            }
        }

        for (case in cases) {
            composeTestRule.runOnIdle { current.value = case }
            composeTestRule.runOnIdle {
                assertEquals(case.name, case.text, style.iconColor)
                assertEquals(case.name, case.primary, style.selectedIconColor)
                assertEquals(case.name, Color(0x66808080), style.dimmedIconColor)
            }
        }
    }

    @Test
    fun renderedThumbs_useExpectedTintsAndDisableVotingAfterSelection() {
        val current = mutableStateOf(cases.first())
        val state = mutableStateOf(FeedbackState.None)
        val below = mutableStateOf(false)
        var callbacks = 0
        composeTestRule.setContent {
            TestTheme(current.value) {
                Box(Modifier.background(current.value.background)) {
                    FeedbackButtons(
                        interactionId = "color-test",
                        onFeedback = { callbacks++ },
                        feedbackState = state.value,
                        showHelpfulLabel = below.value
                    )
                }
            }
        }

        for (case in cases) {
            for (showLabel in listOf(false, true)) {
                for (vote in FeedbackState.values()) {
                    composeTestRule.runOnIdle {
                        current.value = case
                        below.value = showLabel
                        state.value = vote
                    }
                    for (isThumbUp in listOf(true, false)) {
                        val description = if (isThumbUp) "Thumbs up" else "Thumbs down"
                        val icon = composeTestRule.onNodeWithContentDescription(description)
                        val expected = when {
                            vote == FeedbackState.None -> case.text
                            (vote == FeedbackState.Positive) == isThumbUp -> case.primary
                            else -> Color(0x66808080)
                        }.compositeOver(case.background)
                        val pixels = icon.captureToImage().toPixelMap()
                        var matchingPixels = 0
                        for (y in 0 until pixels.height) {
                            for (x in 0 until pixels.width) {
                                val pixel = pixels[x, y]
                                if (abs(pixel.red - expected.red) < 0.01f &&
                                    abs(pixel.green - expected.green) < 0.01f &&
                                    abs(pixel.blue - expected.blue) < 0.01f
                                ) {
                                    matchingPixels++
                                }
                            }
                        }
                        assertTrue(
                            "${case.name}, below=$showLabel, $vote, $description: expected $expected",
                            matchingPixels >= 5
                        )
                        if (vote == FeedbackState.None) {
                            icon.assertIsEnabled()
                        } else {
                            icon.assertIsNotEnabled().performClick()
                        }
                    }
                }
            }
        }
        composeTestRule.runOnIdle { assertEquals(0, callbacks) }
    }
}
