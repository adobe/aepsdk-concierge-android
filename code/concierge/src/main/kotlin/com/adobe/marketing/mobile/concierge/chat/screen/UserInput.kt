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

package com.adobe.marketing.mobile.concierge.chat.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.chat.inputtext.ChatTextField
import com.adobe.marketing.mobile.concierge.chat.inputtext.MicButton
import com.adobe.marketing.mobile.concierge.chat.inputtext.SendButton

/**
 * User input section that handles text input, voice recording, and message sending.
 */
@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    inputText: String,
    isEnabled: Boolean,
    isRecording: Boolean,
    isTranscribing: Boolean,
    canSendMessage: Boolean,
    errorMessage: String?,
    userInputState: UserInputState,
    onSendMessage: () -> Unit,
    onMicButtonClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        // error message display
        errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // row with text field, mic button, and send button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // text input field
            ChatTextField(
                modifier = Modifier.weight(1f),
                value = inputText,
                enabled = isEnabled && !isRecording && !isTranscribing,
                placeholder = when (userInputState) {
                    is UserInputState.Empty -> "Type a message..."
                    is UserInputState.Editing -> "Type a message..."
                    is UserInputState.Recording -> "Recording... (click mic to stop)"
                    is UserInputState.Transcribing -> "Transcribing audio..."
                    is UserInputState.Error -> "Error: ${userInputState.message}"
                }
            )
            
            // mic button
            MicButton(
                isRecording = isRecording,
                isTranscribing = isTranscribing,
                onClick = onMicButtonClick
            )
            
            // send button
            SendButton(
                onClick = onSendMessage,
                enabled = canSendMessage && !isRecording && !isTranscribing
            )
        }
    }
}