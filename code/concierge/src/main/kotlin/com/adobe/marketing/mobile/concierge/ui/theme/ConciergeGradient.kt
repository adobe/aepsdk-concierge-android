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

package com.adobe.marketing.mobile.concierge.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Two-color linear gradient for theme tokens that support either a solid color or a gradient
 * (input bar border, mic/send icon colors, mic waveform gradient) -- a start and end color, with
 * angle as an optional configuration knob on top rather than an arbitrary multi-stop system.
 */
@Immutable
data class ConciergeGradient(
    val startColor: Color,
    val endColor: Color,
    /** Degrees, CSS `linear-gradient` convention: 0 = "to top", increasing clockwise. Default 180 = "to bottom". */
    val angle: Float = 180f
) {
    /**
     * Whether both ends of the gradient are real (non-transparent) colors. [CSSKeyMapper] builds a
     * gradient incrementally from 3 independent CSS keys (start color, end color, angle) that may
     * arrive in any order, defaulting whichever side hasn't been set yet to transparent -- so a
     * gradient with only one side configured is not yet meant to render.
     */
    val isRenderable: Boolean
        get() = startColor != Color.Transparent && endColor != Color.Transparent
}

/**
 * Unit-space (0f..1f) start/end points for this gradient's [ConciergeGradient.angle], following CSS
 * `linear-gradient` convention (0 = to top, increasing clockwise). Exact for the 4 axis-aligned
 * angles (0/90/180/270); approximate otherwise -- does not correct for non-square aspect ratio or
 * CSS's corner-reaching line-length scaling. Split out from [toBrush] so the angle math is directly
 * unit-testable without needing an actual draw [Size].
 */
internal val ConciergeGradient.unitPoints: Pair<Offset, Offset>
    get() {
        val radians = Math.toRadians(angle.toDouble())
        val dx = (sin(radians) * 0.5).toFloat()
        val dy = (-cos(radians) * 0.5).toFloat()
        return Offset(0.5f - dx, 0.5f - dy) to Offset(0.5f + dx, 0.5f + dy)
    }

/** Resolves [unitPoints] against an actual draw [size] into a linear-gradient [Brush]. */
internal fun ConciergeGradient.toBrush(size: Size): Brush {
    val (startFraction, endFraction) = unitPoints
    return Brush.linearGradient(
        colors = listOf(startColor, endColor),
        start = Offset(startFraction.x * size.width, startFraction.y * size.height),
        end = Offset(endFraction.x * size.width, endFraction.y * size.height)
    )
}

/**
 * Draws a gradient border matching [androidx.compose.foundation.border]'s solid-color counterpart,
 * for tokens (e.g. the chat input bar outline) that support a themed gradient alternative.
 */
internal fun Modifier.conciergeGradientBorder(width: Dp, gradient: ConciergeGradient, shape: Shape): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val brush = gradient.toBrush(size)
        val strokeWidthPx = width.toPx()
        onDrawBehind {
            drawOutline(outline, brush = brush, style = Stroke(strokeWidthPx))
        }
    }

/**
 * Draws a gradient fill matching [androidx.compose.foundation.background]'s solid-color
 * counterpart, for tokens (e.g. the arrow-style send button's circle) that support a themed
 * gradient alternative.
 */
internal fun Modifier.conciergeGradientBackground(gradient: ConciergeGradient, shape: Shape): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val brush = gradient.toBrush(size)
        onDrawBehind {
            drawOutline(outline, brush = brush)
        }
    }
