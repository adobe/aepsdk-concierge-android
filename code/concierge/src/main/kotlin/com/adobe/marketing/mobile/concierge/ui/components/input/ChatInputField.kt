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


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles


/**
 * A composable chat input coordinator that manages text input and voice recording.
 * Always shows ChatInputPanel which adapts its UI based on the current input state.
 * During recording, streams partial transcription results directly to the text field.
 *
 * @param modifier Modifier for the composable
 * @param placeholder Placeholder text for the input field
 * @param enable Whether the input field is enabled
 * @param inputState The current state of user input (contains transcribed text if available)
 * @param onTextChange Callback when text content changes
 * @param onMicPressed Callback when microphone button is pressed
 * @param onSend Callback when a message is created
 * @param onVoiceCancel Callback when voice recording is cancelled/stopped
 */
@Composable
internal fun ChatInputField(
    modifier: Modifier = Modifier,
    placeholder: String = "How can I help",
    enable: Boolean = true,
    inputState: UserInputState = UserInputState.Empty,
    isProcessing: Boolean = false,
    onTextChange: (String) -> Unit,
    onMicPressed: () -> Unit,
    onSend: (String) -> Unit,
    onVoiceCancel: () -> Unit
) {
    val style = ConciergeStyles.chatInputFieldStyle
    // Local text state to manage input field content
    val text = remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Update local text state when inputState changes (for voice transcription)
    LaunchedEffect(inputState) {
        when (inputState) {
            is UserInputState.Recording -> {
                // Stream partial transcription results directly to text field
                text.value = inputState.transcription
                // No need to notify parent - they already know about this content
            }

            is UserInputState.Editing -> {
                // Final transcription result
                if (inputState.content.isNotEmpty()) {
                    text.value = inputState.content
                    // No need to notify parent - they already know about this content
                }
            }

            else -> {
                // Do nothing for other states
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.padding)
    ) {
        // Always show ChatInputPanel - no more separate VoiceRecordingPanel
        ChatInputPanel(
            text = text.value,
            onTextChange = { newText ->
                // Only allow text changes during Empty/Editing states (not during Recording)
                if (inputState !is UserInputState.Recording) {
                    text.value = newText
                    // Always notify parent about text changes
                    onTextChange(newText)
                }
            },
            placeholder = placeholder,
            enable = enable && inputState !is UserInputState.Recording,
            inputState = inputState,
            isProcessing = isProcessing,
            onMicPressed = onMicPressed,
            onSend = { sentText ->
                onSend(sentText)
                text.value = ""
                onTextChange("")
                focusManager.clearFocus() // Dismiss keyboard after sending
            },
            onVoiceCancel = onVoiceCancel
        )
    }
}
