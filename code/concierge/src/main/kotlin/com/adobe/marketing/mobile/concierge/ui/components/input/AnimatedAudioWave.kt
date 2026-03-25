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

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * Animated audio waveform with 5 bars that oscillate at staggered intervals.
 * Each bar pulses between a minimum and maximum height fraction.
 *
 * @param modifier Modifier for the composable
 * @param color The color of the waveform bars
 */
@Composable
internal fun AnimatedAudioWave(
    modifier: Modifier = Modifier,
    color: Color
) {
    // Base height fractions for each bar (min, max) — gives a natural waveform look
    val barConfigs = listOf(
        0.25f to 0.55f,   // bar 1 (short)
        0.35f to 0.80f,   // bar 2
        0.30f to 0.65f,   // bar 3
        0.40f to 1.00f,   // bar 4 (tallest)
        0.20f to 0.50f    // bar 5 (shortest)
    )
    val delays = listOf(0, 150, 80, 200, 120)

    val transition = rememberInfiniteTransition(label = "audiowave")
    val fractions = barConfigs.mapIndexed { index, (min, max) ->
        transition.animateFloat(
            initialValue = min,
            targetValue = max,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 400, delayMillis = delays[index]),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar_$index"
        )
    }

    Canvas(modifier = modifier) {
        val barCount = fractions.size
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 2f - 1f) // bars + gaps
        val gap = barWidth
        val cornerRadius = barWidth / 2f

        fractions.forEachIndexed { index, anim ->
            val fraction by anim
            val barHeight = totalHeight * fraction
            val x = index * (barWidth + gap)
            val y = (totalHeight - barHeight) / 2f

            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
