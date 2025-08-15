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

package com.adobe.marketing.mobile.concierge.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R

/**
 * A voice input button that supports recording, transcribing, and idle states.
 *
 * @param modifier Modifier for the composable
 * @param inputStreamState The current state of the input stream
 * @param isEnabled Whether the button is enabled
 * @param waveformPulse The current pulse scale value for animation
 * @param onVoiceInputStart Callback when voice recording should start
 * @param onVoiceInputStop Callback when voice recording should stop
 */
@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    inputStreamState: InputStreamState,
    isEnabled: Boolean,
    waveformPulse: Float,
    onVoiceInputStart: () -> Unit,
    onVoiceInputStop: () -> Unit
) {
    IconButton(
        onClick = {
            when (inputStreamState) {
                is InputStreamState.Recording -> onVoiceInputStop()
                else -> onVoiceInputStart()
            }
        },
        enabled = isEnabled,
        modifier = modifier
            .size(48.dp)
            .scale(if (inputStreamState is InputStreamState.Recording) waveformPulse else 1.0f)
            .background(
                color = when (inputStreamState) {
                    is InputStreamState.Recording -> MaterialTheme.colorScheme.primaryContainer
                    is InputStreamState.Transcribing -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                shape = CircleShape
            )
    ) {
        Image(
            painter = when (inputStreamState) {
                is InputStreamState.Recording -> painterResource(R.drawable.audiowave)
                is InputStreamState.Transcribing -> painterResource(R.drawable.microphone)
                else -> painterResource(R.drawable.microphone)
            },
            contentDescription = when (inputStreamState) {
                is InputStreamState.Recording -> "Stop recording"
                is InputStreamState.Transcribing -> "Processing voice input"
                else -> "Start voice input"
            },
            modifier = Modifier.size(24.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                when (inputStreamState) {
                    is InputStreamState.Recording -> MaterialTheme.colorScheme.onPrimaryContainer
                    is InputStreamState.Transcribing -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        )
    }
}
