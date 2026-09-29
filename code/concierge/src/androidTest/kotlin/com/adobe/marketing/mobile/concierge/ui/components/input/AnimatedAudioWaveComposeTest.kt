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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradient
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the AnimatedAudioWave composable, driving its frame-clock animation and
 * gradient/solid-color drawing paths through actual composition.
 */
class AnimatedAudioWaveComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun animatedAudioWave_withSolidColor_rendersWithoutCrashing() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier.size(24.dp),
                color = Color.Red
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun animatedAudioWave_withGradientColors_rendersWithoutCrashing() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier.size(24.dp),
                color = Color.Red,
                gradient = ConciergeGradient(startColor = Color.Cyan, endColor = Color.Black)
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun animatedAudioWave_withZeroAudioLevel_rendersWithoutCrashing() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier.size(24.dp),
                color = Color.Red,
                audioLevel = 0f
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun animatedAudioWave_withFullAudioLevel_rendersWithoutCrashing() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier.size(24.dp),
                color = Color.Red,
                audioLevel = 1f
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun animatedAudioWave_withCustomBarCount_rendersWithoutCrashing() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier.size(24.dp),
                color = Color.Red,
                barCount = 3
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun animatedAudioWave_withGradient_appliesFullGradientToEachBar() {
        composeTestRule.setContent {
            AnimatedAudioWave(
                modifier = Modifier
                    .size(width = 110.dp, height = 200.dp)
                    .background(Color.White)
                    .testTag("wave"),
                color = Color.Green,
                gradient = ConciergeGradient(startColor = Color.Red, endColor = Color.Blue, angle = 180f),
                audioLevel = 1f
            )
        }
        composeTestRule.waitForIdle()

        val pixels = composeTestRule.onNodeWithTag("wave").captureToImage().toPixelMap()
        // Sample the center column of the first (shortest) bar. With 6 bars the canvas is split
        // into 11 equal slots, so the first bar occupies the first slot.
        val column = pixels.width / 22
        fun isBarPixel(row: Int): Boolean {
            val c = pixels[column, row]
            return !(c.red > 0.95f && c.green > 0.95f && c.blue > 0.95f)
        }
        // Bars are vertically centered, so walk outward from the middle row to find the bar's
        // extent without picking up anything else in the captured image.
        val center = pixels.height / 2
        assertTrue("expected the first bar to be drawn", isBarPixel(center))
        var barTop = center
        while (barTop > 0 && isBarPixel(barTop - 1)) barTop--
        var barBottom = center
        while (barBottom < pixels.height - 1 && isBarPixel(barBottom + 1)) barBottom++

        val barHeight = barBottom - barTop
        val nearTop = pixels[column, barTop + barHeight / 10]
        val nearBottom = pixels[column, barTop + barHeight * 9 / 10]

        // A gradient spanning the whole canvas would leave this short, vertically centered bar
        // in mid-tones; a per-bar gradient runs from the start color to the end color.
        assertTrue("top of bar should be near the start color, was $nearTop", nearTop.red > 0.8f && nearTop.blue < 0.3f)
        assertTrue("bottom of bar should be near the end color, was $nearBottom", nearBottom.blue > 0.8f && nearBottom.red < 0.3f)
    }
}
