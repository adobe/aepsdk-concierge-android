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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import kotlin.math.sin

private const val BAR_HEIGHT_FLOOR = 0.12
private const val MAX_AMPLITUDE = 0.88

/**
 * Computes the height fraction for a waveform bar at [index] given the elapsed [timeSeconds].
 * Each bar is offset in phase by [index] so the bars don't all pulse in lockstep. [audioLevel]
 * (0f..1f, defaults to full amplitude) scales the oscillation's amplitude: at 0 (no voice
 * detected) the bar is static at [BAR_HEIGHT_FLOOR]; at 1 it swings through the full
 * [BAR_HEIGHT_FLOOR]..1f range.
 */
internal fun audioWaveBarScale(index: Int, timeSeconds: Double, audioLevel: Float = 1f): Float {
    val phase = sin(timeSeconds * 4.0 + index * 1.2)
    val oscillation = (phase + 1.0) / 2.0
    val amplitude = MAX_AMPLITUDE * audioLevel.coerceIn(0f, 1f)
    val scale = BAR_HEIGHT_FLOOR + amplitude * oscillation
    return scale.toFloat()
}

/**
 * Returns a vertical gradient brush when both [gradientStart] and [gradientEnd] are provided,
 * otherwise falls back to a solid [color] fill.
 */
internal fun audioWaveBarBrush(color: Color, gradientStart: Color?, gradientEnd: Color?): Brush {
    return if (gradientStart != null && gradientEnd != null) {
        Brush.verticalGradient(listOf(gradientStart, gradientEnd))
    } else {
        SolidColor(color)
    }
}

/**
 * Animated audio waveform with 5 bars that pulse while recording, each bar staggered in phase
 * for a natural waveform look. Bars sit static and flat until voice is actually detected.
 *
 * @param modifier Modifier for the composable
 * @param color The color of the waveform bars, used when no gradient is configured
 * @param gradientStart Optional top color of a vertical gradient fill for the bars
 * @param gradientEnd Optional bottom color of a vertical gradient fill for the bars
 * @param barCount Number of bars to render
 * @param audioLevel Normalized 0f..1f input level. Defaults to full amplitude; pass the live
 * level from [com.adobe.marketing.mobile.concierge.ui.state.UserInputState.Recording] so the
 * bars stay static during silence and only animate once voice is detected.
 */
@Composable
internal fun AnimatedAudioWave(
    modifier: Modifier = Modifier,
    color: Color,
    gradientStart: Color? = null,
    gradientEnd: Color? = null,
    barCount: Int = 5,
    audioLevel: Float = 1f
) {
    var elapsedMillis by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { frameMillis ->
                elapsedMillis = (frameMillis - start).toFloat()
            }
        }
    }

    // Smooth out the discrete, somewhat noisy level updates from the speech engine so the
    // amplitude eases between readings instead of jumping.
    val smoothedAudioLevel by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 150),
        label = "audio_level"
    )

    val brush = audioWaveBarBrush(color, gradientStart, gradientEnd)

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 2f - 1f) // bars + gaps
        val gap = barWidth
        val cornerRadius = barWidth / 2f
        val timeSeconds = elapsedMillis / 1000.0

        for (index in 0 until barCount) {
            val scale = audioWaveBarScale(index, timeSeconds, smoothedAudioLevel)
            val barHeight = totalHeight * scale
            val x = index * (barWidth + gap)
            val y = (totalHeight - barHeight) / 2f

            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
        }
    }
}
