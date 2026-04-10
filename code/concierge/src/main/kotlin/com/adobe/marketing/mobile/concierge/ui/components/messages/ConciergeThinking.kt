/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Component that displays a thinking animation with three pulsing dots.
 * Used to indicate when the concierge is processing a response.
 */
@Composable
internal fun ConciergeThinking(
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.thinkingAnimationStyle

    Row(
        modifier = modifier,
        verticalAlignment = style.dotVerticalAlignment
    ) {
        if (style.thinkingText.isNotEmpty()) {
            Text(
                modifier = Modifier.weight(1f),
                text = style.thinkingText,
                style = style.textStyle,
                color = style.textColor
            )
            Spacer(modifier = Modifier.width(style.textDotSpacing))
        }

        // Create three pulsing dots with staggered animation
        PulsingDot(
            color = style.dotColor,
            size = style.dotSize,
            delay = 0,
            animationDuration = style.dotAnimationDuration
        )
        Spacer(modifier = Modifier.width(style.dotSpacing))
        PulsingDot(
            color = style.dotColor,
            size = style.dotSize,
            delay = style.dotAnimationDelay,
            animationDuration = style.dotAnimationDuration
        )
        Spacer(modifier = Modifier.width(style.dotSpacing))
        PulsingDot(
            color = style.dotColor,
            size = style.dotSize,
            delay = style.dotAnimationDelay * 2,
            animationDuration = style.dotAnimationDuration
        )
    }
}

/**
 * A single pulsing dot with animated scale and alpha.
 *
 * @param color The color of the dot
 * @param size The base size of the dot
 * @param delay Animation delay in milliseconds for staggered effect
 * @param animationDuration Duration of the animation in milliseconds
 */
@Composable
private fun PulsingDot(
    color: Color,
    size: Dp,
    delay: Int,
    animationDuration: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_pulse")

    val animationSpec = infiniteRepeatable<Float>(
        animation = tween(durationMillis = animationDuration, delayMillis = delay),
        repeatMode = RepeatMode.Reverse
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = animationSpec,
        label = "dot_scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = animationSpec,
        label = "dot_alpha"
    )

    Canvas(modifier = Modifier.size(size)) {
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = (size.toPx() / 2) * scale
        )
    }
}

