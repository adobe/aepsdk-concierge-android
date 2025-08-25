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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState


/**
 * A composable chat input coordinator that manages the display of different input panels.
 * Shows VoiceRecordingPanel during recording/transcribing states, otherwise shows ChatInputPanel.
 *
 * @param modifier Modifier for the composable
 * @param placeholder Placeholder text for the input field
 * @param enable Whether the input field is enabled
 * @param inputState The current state of user input (contains transcribed text if available)
 * @param onContentAvailabilityChanged Callback when content availability changes (non-empty/empty)
 * @param onMicPressed Callback when microphone button is pressed
 * @param onSend Callback when a message is created
 * @param onVoiceCancel Callback when voice recording is cancelled (X button)
 * @param onVoiceConfirm Callback when voice recording is confirmed (✓ button)
 */
@Composable
internal fun ChatInputField(
    modifier: Modifier = Modifier,
    placeholder: String = "How can I help",
    enable: Boolean = true,
    inputState: UserInputState = UserInputState.Empty,
    isProcessing: Boolean = false,
    onContentAvailabilityChanged: (available: Boolean) -> Unit,
    onMicPressed: () -> Unit,
    onSend: (String) -> Unit,
    onVoiceCancel: () -> Unit,
    onVoiceConfirm: () -> Unit
) {
    // Local text state to manage input field content
    var text by remember { mutableStateOf("") }

    // Update local text state when inputState changes to Editing with non-empty content
    LaunchedEffect(inputState) {
        if (inputState is UserInputState.Editing && inputState.content.isNotEmpty()) {
            val wasEmpty = text.isBlank()
            text += inputState.content
            
            // Notify parent about content availability change
            if (wasEmpty && inputState.content.isNotBlank()) {
                onContentAvailabilityChanged(true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (inputState) {
            is UserInputState.Empty, is UserInputState.Editing, is UserInputState.Error -> {
                ChatInputPanel(
                    text = text,
                    onTextChange = { newText ->
                        val wasEmpty = text.isBlank()
                        val willBeEmpty = newText.isBlank()
                        text = newText

                        // Only notify parent when empty/non-empty state changes
                        if (wasEmpty != willBeEmpty) {
                            onContentAvailabilityChanged(!willBeEmpty)
                        }
                    },
                    placeholder = placeholder,
                    enable = enable,
                    inputState = inputState,
                    isProcessing = isProcessing,
                    onMicPressed = onMicPressed,
                    onSend = { sentText ->
                        onSend(sentText)
                        text = ""
                        onContentAvailabilityChanged(false)
                    }
                )
            }
            is UserInputState.Recording, is UserInputState.Transcribing -> {
                VoiceRecordingPanel(
                    inputState = inputState,
                    onCancel = onVoiceCancel,
                    onConfirm = onVoiceConfirm
                )
            }
        }
    }
}
