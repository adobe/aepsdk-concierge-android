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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.R

/**
 * A voice input button that supports recording, transcribing, and idle states.
 *
 * @param modifier Modifier for the composable
 * @param userInputState The current state of the input stream
 * @param isEnabled Whether the button is enabled
 * @param waveformPulse The current pulse scale value for animation
 * @param onVoiceInputStart Callback when voice recording should start
 * @param onVoiceInputStop Callback when voice recording should stop
 */
@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    userInputState: UserInputState,
    isEnabled: Boolean,
    waveformPulse: Float,
    onVoiceInputStart: () -> Unit,
    onVoiceInputStop: () -> Unit
) {
    IconButton(
        onClick = {
            when (userInputState) {
                is UserInputState.Recording -> onVoiceInputStop()
                else -> onVoiceInputStart()
            }
        },
        enabled = isEnabled,
        modifier = modifier
            .size(48.dp)
            .scale(if (userInputState is UserInputState.Recording) waveformPulse else 1.0f)
            .background(
                color = when (userInputState) {
                    is UserInputState.Recording -> MaterialTheme.colorScheme.primaryContainer
                    is UserInputState.Transcribing -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = CircleShape
            )
    ) {
        Image(
            painter = when (userInputState) {
                is UserInputState.Recording -> painterResource(R.drawable.audiowave)
                is UserInputState.Transcribing -> painterResource(R.drawable.microphone)
                else -> painterResource(R.drawable.microphone)
            },
            contentDescription = when (userInputState) {
                is UserInputState.Recording -> "Stop recording"
                is UserInputState.Transcribing -> "Processing voice input"
                else -> "Start voice input"
            },
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(
                when (userInputState) {
                    is UserInputState.Recording -> MaterialTheme.colorScheme.onPrimaryContainer
                    is UserInputState.Transcribing -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        )
    }
}
