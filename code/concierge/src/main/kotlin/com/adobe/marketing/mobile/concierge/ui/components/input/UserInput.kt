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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechPermissionHandler

/**
 * Component that handles user input including text input and voice recording.
 * Derives its behavior from UserInputState and processing state.
 */
@Composable
internal fun UserInput(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    onTextChange: (String) -> Unit,
    isProcessing: Boolean = false,
    onMicEvent: (MicEvent) -> Unit,
    onSend: (String) -> Unit,
    hasAudioPermission: Boolean,
    onPermissionResult: (Boolean) -> Unit
) {

    // Handle speech permission if needed
    SpeechPermissionHandler(
        hasPermission = hasAudioPermission,
        onPermissionResult = onPermissionResult
    )

    Column(
        modifier = modifier
    ) {
        // Show processing indicator above the input field when processing
        if (isProcessing) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Processing message...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input field
        ChatInputField(
            modifier = Modifier.fillMaxWidth(),
            enable = true,
            inputState = inputState,
            isProcessing = isProcessing,
            onTextChange = onTextChange,
            onMicPressed = {
                if (hasAudioPermission) {
                    onMicEvent(MicEvent.StartRecording)
                } else {
                    // Permission will be requested by SpeechPermissionHandler
                    // TODO: propagate permission error to user
                }
            },
            onVoiceCancel = {
                onMicEvent(MicEvent.StopRecording(isCancelled = false, isError = false))
            },
            onSend = onSend,
        )
    }
}
