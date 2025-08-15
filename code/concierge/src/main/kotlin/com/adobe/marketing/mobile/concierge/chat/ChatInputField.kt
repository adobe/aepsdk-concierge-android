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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import kotlinx.coroutines.delay

/**
 * A composable chat input field that supports both text and voice input.
 *
 * @param modifier Modifier for the composable
 * @param onMessageCreated Callback invoked when a message is sent or received
 * @param placeholder Placeholder text for the input field
 */
@Composable
fun ChatInputField(
    modifier: Modifier = Modifier,
    onMessageCreated: (String) -> Unit,
    placeholder: String = "Type a message...",
) {
    val stateMachine = remember { ChatInputStateMachine() }
    val context = LocalContext.current

    // Collect state from state machine
    val inputStreamState by stateMachine.inputStreamState.collectAsState()
    val data by stateMachine.data.collectAsState()

    // Track previous state for toast notifications
    var previousInputStreamState by remember { mutableStateOf<InputStreamState>(InputStreamState.Empty) }

    // For testing purposes, show toast for InputStreamState changes
    LaunchedEffect(inputStreamState) {
        if (inputStreamState != previousInputStreamState) {
            val stateName = when (inputStreamState) {
                is InputStreamState.Empty -> "Empty"
                is InputStreamState.Editing -> "Editing"
                is InputStreamState.Recording -> "Recording"
                is InputStreamState.Transcribing -> "Transcribing"
                is InputStreamState.Error -> "Error: ${(inputStreamState as InputStreamState.Error).message}"
            }
            Toast.makeText(context, "Input State: $stateName", Toast.LENGTH_SHORT).show()
            previousInputStreamState = inputStreamState
        }
    }

    // Smooth pulsing animation for waveform icon when recording
    var waveformPulseTarget by remember { mutableStateOf(1.0f) }
    val waveformPulse by animateFloatAsState(
        targetValue = waveformPulseTarget,
        animationSpec = tween(
            durationMillis = 600,
            easing = androidx.compose.animation.core.EaseInOutCubic
        ),
        label = "waveform_pulse"
    )
    
    LaunchedEffect(inputStreamState) {
        while (inputStreamState is InputStreamState.Recording) {
            waveformPulseTarget = 1.2f
            delay(600)
            waveformPulseTarget = 1.0f
            delay(600)
        }
    }

    // simulate voice input recording stopped and processing
    // to-do: replace with actual voice input handling logic
    LaunchedEffect(inputStreamState) {
        when (inputStreamState) {
            is InputStreamState.Transcribing -> {
                // Simulate voice recording delay
                delay(2000)
                stateMachine.processEvent(ChatInputEvent.RecordingComplete)
                // Simulate transcribed text
                stateMachine.processEvent(ChatInputEvent.TranscriptionComplete("Hello, this is a voice message!"))
            }
            else -> { /* No action needed */ }
        }
    }

    // Update text when speech to text input is available
    LaunchedEffect(data.voiceInputText) {
        if (data.voiceInputText.isNotEmpty()) {
            stateMachine.processEvent(ChatInputEvent.InputReceived(data.voiceInputText))
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
                    value = data.text,
                    onValueChange = { newText -> 
                        if (newText.isBlank() && data.text.isNotBlank()) {
                            stateMachine.processEvent(ChatInputEvent.DeleteContent)
                        } else if (newText.isNotBlank() && data.text.isBlank()) {
                            stateMachine.processEvent(ChatInputEvent.AddContent)
                        }
                        stateMachine.processEvent(ChatInputEvent.InputReceived(newText))
                    },
                    inputStreamState = inputStreamState,
                    isEnabled = data.isEnabled,
                    canSendMessage = data.canSendMessage,
                    placeholder = placeholder
                )

                // Mic button for voice input
                MicButton(
                    inputStreamState = inputStreamState,
                    isEnabled = data.isEnabled,
                    waveformPulse = waveformPulse,
                    onVoiceInputStart = {
                        stateMachine.processEvent(ChatInputEvent.StartMic)
                    },
                    onVoiceInputStop = {
                        stateMachine.processEvent(ChatInputEvent.RecordingComplete)
                    }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Send button
                SendButton(
                    canSendMessage = data.canSendMessage,
                    isEnabled = data.isEnabled,
                    isRecording = inputStreamState is InputStreamState.Recording,
                    onSend = {
                        val currentText = stateMachine.getCurrentText()
                        if (currentText.isNotBlank()) {
                            onMessageCreated(currentText)
                            stateMachine.processEvent(ChatInputEvent.SendMessage)
                        }
                        // reset input field after sending
                        stateMachine.processEvent(ChatInputEvent.Reset)
                    }
                )
            }
        }
    }
}
