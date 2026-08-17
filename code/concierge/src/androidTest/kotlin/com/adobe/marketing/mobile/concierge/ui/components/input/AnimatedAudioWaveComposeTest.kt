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

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
                gradientStart = Color.Cyan,
                gradientEnd = Color.Black
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
}
