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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Chat input panel with text field, microphone button, and send button.
 * Used when not in voice recording mode.
 *
 * @param modifier Modifier for the composable
 * @param text The current text input value
 * @param onTextChange Callback when the text input changes
 * @param placeholder Placeholder text for the input field
 * @param enable Whether the input field and buttons are enabled
 * @param inputState The current state of user input (e.g. Empty, Editing)
 * @param onMicPressed Callback when the microphone button is pressed
 * @param onSend Callback when a send button is pressed with non-empty text
 * @param borderColors List of colors for the animated border gradient
 */
@Composable
internal fun ChatInputPanel(
    modifier: Modifier = Modifier,
    text: String,
    onTextChange: (String) -> Unit,
    placeholder: String = "How can I help",
    enable: Boolean = true,
    isProcessing: Boolean = false,
    inputState: UserInputState = UserInputState.Empty,
    onMicPressed: () -> Unit,
    onSend: (String) -> Unit,
    onVoiceCancel: (() -> Unit)? = null,
    borderColors: List<Color> = emptyList()
) {
    val style = ConciergeStyles.inputPanelStyle
    
    // Static pulse value for mic button
    val waveformPulse = remember { 1.0f }

    // Animated border rotation
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(style.recordingBorderAnimationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val brush = if (inputState is UserInputState.Recording) Brush.sweepGradient(
        style.recordingBorderColors
    ) else Brush.sweepGradient(listOf(Color.Transparent))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = style.outerShape,
        color = style.backgroundColor
    ) {
        Surface(
            modifier = Modifier
                .clipToBounds()
                .fillMaxWidth()
                .padding(style.outerPadding)
                .let { baseModifier ->
                    if (inputState is UserInputState.Recording) {
                        baseModifier.drawWithContent {
                            rotate(angle) {
                                drawCircle(
                                    brush = brush,
                                    radius = size.width,
                                    blendMode = BlendMode.SrcIn,
                                )
                            }
                            drawContent()
                        }
                    } else {
                        baseModifier
                    }
                },
            color = style.backgroundColor,
            shape = style.innerShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(style.innerPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChatTextField(
                    modifier = Modifier.weight(1f),
                    value = text,
                    onValueChange = onTextChange,
                    isEnabled = enable,
                    placeholder = if (inputState is UserInputState.Recording) style.listeningPlaceholderText else placeholder
                )

                // Show different buttons based on state
                when (inputState) {
                    is UserInputState.Recording -> {
                        // During recording, transform mic button to cancel button
                        IconButton(
                            onClick = { onVoiceCancel?.invoke() },
                            modifier = Modifier.size(ConciergeStyles.micButtonStyle.size)
                        ) {
                            Icon(
                                imageVector = Icons.Default.StopCircle,
                                contentDescription = "Stop recording",
                                tint = ConciergeStyles.micButtonStyle.iconColor
                            )
                        }

                        Spacer(modifier = Modifier.width(style.buttonSpacing))

                        // Send button remains but is disabled during recording
                        SendButton(
                            modifier = Modifier.size(ConciergeStyles.sendButtonStyle.size),
                            isEnabled = false, // Disabled during recording
                            onSend = { /* No-op during recording */ }
                        )
                    }

                    else -> {
                        // Normal state - show mic and send buttons
                        MicButton(
                            modifier = Modifier.size(ConciergeStyles.micButtonStyle.size),
                            userInputState = inputState,
                            isEnabled = enable,
                            waveformPulse = waveformPulse,
                            onClick = onMicPressed
                        )

                        Spacer(modifier = Modifier.width(style.buttonSpacing))

                        SendButton(
                            modifier = Modifier.size(ConciergeStyles.sendButtonStyle.size),
                            isEnabled = text.isNotBlank() && !isProcessing,
                            onSend = {
                                if (text.isNotBlank()) {
                                    onSend(text)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}