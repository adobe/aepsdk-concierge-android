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

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Voice recording panel that shows listening/transcribing states with action buttons.
 * Features cancel and confirm buttons at the corners with centered animation and status text.
 */
@Composable
internal fun VoiceRecordingPanel(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val style = ConciergeStyles.voiceRecordingPanelStyle

    // Animation for listening state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(style.pulseAnimationDuration)
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = style.shape,
        elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
        colors = CardDefaults.cardColors(
            containerColor = style.backgroundColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.padding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button (X)
            IconButton(
                onClick = onCancel,
                modifier = Modifier.size(style.iconSize)
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = "Cancel recording",
                    tint = style.cancelIconColor
                )
            }

            // Center content (icon + text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(style.contentSpacing)
            ) {
                // Animated microphone/waveform icon
                Icon(
                    painter = when (inputState) {
                        is UserInputState.Recording -> painterResource(R.drawable.audiowave)
                        else -> painterResource(R.drawable.microphone)
                    },
                    contentDescription = null,
                    tint = style.iconColor,
                    modifier = Modifier
                        .size(style.iconSize)
                        .scale(if (inputState is UserInputState.Recording) pulseScale else 1.0f)
                )

                // Status text
                Text(
                    text = when (inputState) {
                        is UserInputState.Recording -> style.listeningText
                        else -> ""
                    },
                    style = style.textStyle,
                    color = style.textColor
                )
            }

            // Confirm button (✓) at right corner - only show during listening
            if (inputState is UserInputState.Recording) {
                IconButton(
                    onClick = onConfirm,
                    modifier = Modifier.size(style.iconSize)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.checkmark),
                        contentDescription = "Confirm recording",
                        tint = style.iconColor
                    )
                }
            } else {
                // Spacer to maintain layout balance when confirm button is not shown
                Spacer(modifier = Modifier.size(style.iconSize))
            }
        }
    }
}
