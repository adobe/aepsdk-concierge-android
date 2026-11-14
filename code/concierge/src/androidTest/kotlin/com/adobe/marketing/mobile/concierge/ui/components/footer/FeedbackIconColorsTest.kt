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
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.DarkConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.LightConciergeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedbackIconColorsTest {
    private companion object {
        const val MIN_FOREGROUND_SAMPLES = 8
        const val MIN_REFERENCE_COVERAGE = 0.5f
        const val TINT_CHANNEL_TOLERANCE = 0.05f
    }

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
                assertEquals(case.name, Color(0x668E8E93), style.dimmedIconColor)
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
                Column {
                    Box(Modifier.background(current.value.background)) {
                        FeedbackButtons(
                            interactionId = "color-test",
                            onFeedback = { callbacks++ },
                            feedbackState = state.value,
                            showHelpfulLabel = below.value
                        )
                    }
                    for (isThumbUp in listOf(true, false)) {
                        val resource = when {
                            isThumbUp && state.value == FeedbackState.Positive ->
                                R.drawable.concierge_ic_thumbs_up_filled
                            isThumbUp -> R.drawable.concierge_ic_thumbs_up
                            state.value == FeedbackState.Negative ->
                                R.drawable.concierge_ic_thumbs_down_filled
                            else -> R.drawable.concierge_ic_thumbs_down
                        }
                        Icon(
                            painter = painterResource(resource),
                            contentDescription = null,
                            modifier = Modifier
                                .size(ConciergeStyles.feedbackButtonsStyle.iconSize)
                                .background(Color.White)
                                .testTag("coverage-$isThumbUp"),
                            tint = Color.Black
                        )
                    }
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
                        val icon = composeTestRule.onNodeWithContentDescription(description, useUnmergedTree = true)
                        val expected = when {
                            vote == FeedbackState.None -> case.text
                            (vote == FeedbackState.Positive) == isThumbUp -> case.primary
                            else -> Color(0x668E8E93)
                        }
                        val pixels = icon.captureToImage().toPixelMap()
                        val reference = composeTestRule.onNodeWithTag("coverage-$isThumbUp")
                            .captureToImage().toPixelMap()
                        assertRenderedTint(
                            pixels, reference, expected, case.background,
                            "${case.name}, below=$showLabel, $vote, $description"
                        )
                        val button = composeTestRule.onNodeWithContentDescription(description)
                        if (vote == FeedbackState.None) {
                            button.assertIsEnabled()
                        } else {
                            button.assertIsNotEnabled().performClick()
                        }
                    }
                }
            }
        }
        composeTestRule.runOnIdle { assertEquals(0, callbacks) }
    }

    private fun assertRenderedTint(
        pixels: PixelMap,
        reference: PixelMap,
        tint: Color,
        background: Color,
        context: String
    ) {
        assertEquals("$context: reference width", reference.width, pixels.width)
        assertEquals("$context: reference height", reference.height, pixels.height)
        val samples = mutableListOf<Triple<Int, Int, Float>>()
        for (y in 0 until reference.height) {
            for (x in 0 until reference.width) {
                // A separate black-on-white rendering measures coverage, not the tint under test.
                val coverage = 1f - reference[x, y].red
                if (coverage >= MIN_REFERENCE_COVERAGE) samples.add(Triple(x, y, coverage))
            }
        }
        assertTrue(
            "$context: expected at least $MIN_FOREGROUND_SAMPLES reference samples, found ${samples.size}",
            samples.size >= MIN_FOREGROUND_SAMPLES
        )
        for ((x, y, coverage) in samples) {
            val expected = tint.copy(alpha = tint.alpha * coverage).compositeOver(background)
            val actual = pixels[x, y]
            assertTrue(
                "$context: pixel ($x, $y), coverage=$coverage: expected $expected, found $actual",
                abs(actual.red - expected.red) <= TINT_CHANNEL_TOLERANCE &&
                    abs(actual.green - expected.green) <= TINT_CHANNEL_TOLERANCE &&
                    abs(actual.blue - expected.blue) <= TINT_CHANNEL_TOLERANCE
            )
        }
    }

    @Test
    fun pixelCheck_acceptsCorrectAntialiasedTintAndRejectsDimmedGray() {
        val tint = Color(0x3300FF00)
        for (coverage in listOf(0.5f, 0.75f, 1f)) {
            val reference = solidPixels(Color.Black.copy(alpha = coverage).compositeOver(Color.White))
            val correct = solidPixels(tint.copy(alpha = tint.alpha * coverage).compositeOver(Color.White))
            assertRenderedTint(correct, reference, tint, Color.White, "correct transparent green")

            val wrong = solidPixels(Color(0x668E8E93).copy(alpha = 0.4f * coverage).compositeOver(Color.White))
            assertThrows(AssertionError::class.java) {
                assertRenderedTint(wrong, reference, tint, Color.White, "wrong dimmed gray")
            }
        }
    }

    @Test
    fun pixelCheck_rejectsMissingOrInsufficientReferencePixelsWithContext() {
        for (count in listOf(0, 1, MIN_FOREGROUND_SAMPLES - 1)) {
            val bitmap = Bitmap.createBitmap(MIN_FOREGROUND_SAMPLES, 1, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.White.toArgb())
            for (x in 0 until count) bitmap.setPixel(x, 0, Color.Black.toArgb())
            val reference = bitmap.asImageBitmap().toPixelMap()
            val error = assertThrows(AssertionError::class.java) {
                assertRenderedTint(solidPixels(Color.Black), reference, Color.Black, Color.White, "missing samples")
            }
            assertTrue(error.message.orEmpty().contains("missing samples: expected at least"))
        }
    }

    private fun solidPixels(color: Color): PixelMap {
        val bitmap = Bitmap.createBitmap(MIN_FOREGROUND_SAMPLES, 1, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color.toArgb())
        return bitmap.asImageBitmap().toPixelMap()
    }
}
