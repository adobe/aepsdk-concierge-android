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

package com.adobe.marketing.mobile.concierge.chat.userinput

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.animation.core.EaseInOutCubic
import com.adobe.marketing.mobile.concierge.chat.simulation.SpeechSimulator
import kotlinx.coroutines.delay

/**
 * A composable chat input field that supports both text and voice input.
 *
 * @param modifier Modifier for the composable
 * @param text Current text in the input field
 * @param onTextChange Callback for when text changes
 * @param onMessageCreated Callback invoked when a message is sent
 * @param placeholder Placeholder text for the input field
 * @param isEnabled Whether the input field is enabled
 * @param isRecording Whether voice recording is currently active
 * @param canSendMessage Whether a message can be sent
 * @param onVoiceRecordingStarted Callback when voice recording starts
 * @param onVoiceRecordingStopped Callback when voice recording stops
 */
@Composable
fun ChatInputField(
    modifier: Modifier = Modifier,
    text: String = "",
    onTextChange: (String) -> Unit = {},
    onMessageCreated: (String) -> Unit,
    placeholder: String = "Type a message...",
    isEnabled: Boolean = true,
    isRecording: Boolean = false,
    canSendMessage: Boolean = false,
    onVoiceRecordingStarted: () -> Unit = {},
    onVoiceRecordingStopped: () -> Unit = {}
) {
    val context = LocalContext.current

    // Track previous state for toast notifications
    var previousRecordingState by remember { mutableStateOf(false) }

    // For testing purposes, show toast for recording state changes
    LaunchedEffect(isRecording) {
        if (isRecording != previousRecordingState) {
            val stateName = if (isRecording) "Recording Started" else "Recording Stopped"
            Toast.makeText(context, "Voice Input: $stateName", Toast.LENGTH_SHORT).show()
            previousRecordingState = isRecording
        }
    }

    // Smooth pulsing animation for waveform icon when recording
    var waveformPulseTarget by remember { mutableStateOf(1.0f) }
    val waveformPulse by animateFloatAsState(
        targetValue = waveformPulseTarget,
        animationSpec = tween(
            durationMillis = 600,
            easing = EaseInOutCubic
        ),
        label = "waveform_pulse"
    )
    
    LaunchedEffect(isRecording) {
        while (isRecording) {
            waveformPulseTarget = 1.2f
            delay(600)
            waveformPulseTarget = 1.0f
            delay(600)
        }
    }

    // Begin composable layout
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Frame the chat input field in a card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Text input field
                ChatTextField(
                    modifier = Modifier.weight(1f),
                    value = text,
                    onValueChange = onTextChange,
                    userInputState = if (isRecording) UserInputState.Recording else if (text.isNotBlank()) UserInputState.Editing else UserInputState.Empty,
                    isEnabled = isEnabled,
                    canSendMessage = canSendMessage,
                    placeholder = placeholder
                )

                // Mic button for voice input
                MicButton(
                    userInputState = if (isRecording) UserInputState.Recording else UserInputState.Empty,
                    isEnabled = isEnabled,
                    waveformPulse = waveformPulse,
                    onVoiceInputStart = onVoiceRecordingStarted,
                    onVoiceInputStop = onVoiceRecordingStopped
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                SendButton(
                    canSendMessage = canSendMessage,
                    isEnabled = isEnabled,
                    isRecording = isRecording,
                    onSend = {
                        if (text.isNotBlank()) {
                            onMessageCreated(text)
                        }
                    }
                )
            }
        }
    }
}
