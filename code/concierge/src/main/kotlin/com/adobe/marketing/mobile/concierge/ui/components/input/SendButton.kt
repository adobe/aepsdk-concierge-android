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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeGradient
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.conciergeGradientBackground

/**
 * The "arrow" style's up-arrow glyph is drawn smaller than its enclosing circle rather than
 * filling it edge-to-edge. Applies only to the circle style -- the "default" (bare icon, no
 * circle) style has nothing to leave a margin against, so its glyph fills its icon slot exactly
 * like every other input-row icon (mic, clear, leading icon).
 */
private const val SEND_ARROW_ICON_SCALE = 2f / 3f

/**
 * A send button for submitting chat messages.
 * Supports two styles controlled via theme behavior:
 * - "default": paper airplane icon with color tint (original look)
 * - "arrow": filled circle with upward arrow icon
 *
 * @param modifier Modifier for the composable
 * @param isEnabled Whether the button is enabled
 * @param onSend Callback when the send button is pressed
 */
@Composable
internal fun SendButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    onSend: () -> Unit
) {
    val style = ConciergeStyles.sendButtonStyle

    if (style.useArrowStyle) {
        SendButtonArrow(
            modifier = modifier,
            isEnabled = isEnabled,
            iconSize = style.size,
            circleColor = style.arrowCircleColor,
            circleGradient = style.arrowCircleGradient,
            arrowColor = style.arrowIconColor,
            disabledAlpha = style.disabledIconAlpha,
            onSend = onSend
        )
    } else {
        SendButtonDefault(
            modifier = modifier,
            isEnabled = isEnabled,
            iconSize = style.size,
            iconColor = style.enabledIconColor,
            disabledAlpha = style.disabledIconAlpha,
            onSend = onSend
        )
    }
}

/**
 * Default send button — paper airplane icon with color tint.
 */
@Composable
private fun SendButtonDefault(
    modifier: Modifier,
    isEnabled: Boolean,
    iconSize: Dp,
    iconColor: Color,
    disabledAlpha: Float,
    onSend: () -> Unit
) {
    IconButton(
        onClick = { if (isEnabled) onSend() },
        enabled = isEnabled,
        modifier = modifier
    ) {
        Image(
            painter = painterResource(R.drawable.send),
            contentDescription = "Send message",
            modifier = Modifier
                .size(iconSize)
                .testTag("SendIconGlyph"),
            colorFilter = ColorFilter.tint(
                if (isEnabled) iconColor
                else iconColor.copy(alpha = disabledAlpha)
            )
        )
    }
}

/**
 * Arrow send button — filled circle with upward arrow icon.
 *
 * [modifier] sizes the outer tap area to the shared row glyph size (no padded container); the
 * visible circle fills it at [iconSize], with the arrow glyph drawn smaller than the circle
 * (see [SEND_ARROW_ICON_SCALE]) so it doesn't touch the circle's edge.
 */
@Composable
private fun SendButtonArrow(
    modifier: Modifier,
    isEnabled: Boolean,
    iconSize: Dp,
    circleColor: Color,
    circleGradient: ConciergeGradient?,
    arrowColor: Color,
    disabledAlpha: Float,
    onSend: () -> Unit
) {
    val bgColor = if (isEnabled) circleColor else circleColor.copy(alpha = disabledAlpha)
    // Gradient only applies while enabled -- disabled state always renders the plain dimmed fill.
    val renderableGradient = if (isEnabled) circleGradient?.takeIf { it.isRenderable } else null

    Box(
        modifier = modifier
            .then(
                if (isEnabled) Modifier.clickable { onSend() }
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .then(
                    if (renderableGradient != null) Modifier.conciergeGradientBackground(renderableGradient, CircleShape)
                    else Modifier.background(bgColor)
                )
                .testTag("SendIconGlyph"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.send_arrow),
                contentDescription = "Send message",
                modifier = Modifier
                    .size(iconSize * SEND_ARROW_ICON_SCALE)
                    .testTag("SendArrowGlyph"),
                colorFilter = ColorFilter.tint(arrowColor)
            )
        }
    }
}
