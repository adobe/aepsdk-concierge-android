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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Composable that manages the mic and send button states and animations.
 * Handles smooth transitions between recording and editing states.
 *
 * @param modifier Modifier for the composable
 * @param inputState The current user input state (determines button visibility and behavior)
 * @param text Current text in the input field (used to enable/disable send button)
 * @param isProcessing Whether a message is currently being processed
 * @param isEnabled Whether the buttons are enabled
 * @param onMicPressed Callback when microphone button is pressed (to start recording)
 * @param onVoiceCancel Callback when recording should be stopped
 * @param onSend Callback when send button is pressed
 */
@Composable
internal fun InputActionButtons(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    text: String,
    isProcessing: Boolean,
    onMicPressed: () -> Unit,
    onVoiceCancel: () -> Unit,
    onSend: (String) -> Unit
) {
    val micButtonStyle = ConciergeStyles.micButtonStyle
    val sendButtonStyle = ConciergeStyles.sendButtonStyle
    val panelStyle = ConciergeStyles.inputPanelStyle
    val waveformPulse = remember { 1.0f }

    Row(
        modifier = modifier.animateContentSize(
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 380f
            )
        ).padding(end = if (inputState is UserInputState.Recording) 8.dp else 0.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mic button - always visible and enabled
        MicButton(
            modifier = Modifier.size(micButtonStyle.size),
            userInputState = inputState,
            isEnabled = true,
            waveformPulse = waveformPulse,
            onClick = {
                if (inputState is UserInputState.Recording) {
                    onVoiceCancel()
                } else {
                    onMicPressed()
                }
            }
        )

        // Send button - only visible when not recording.
        // Clickable if text is non-empty and not processing
        AnimatedVisibility(
            visible = inputState !is UserInputState.Recording,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        initialOffsetX = { it / 2 }
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 150),
                        targetOffsetX = { it / 2 }
                    )
        ) {
            Row {
                Spacer(modifier = Modifier.width(panelStyle.buttonSpacing))

                SendButton(
                    modifier = Modifier.size(sendButtonStyle.size),
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

