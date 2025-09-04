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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState

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
    onSend: (String) -> Unit
) {
    // Static pulse value for mic button
    val waveformPulse = remember { 1.0f }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatTextField(
                modifier = Modifier.weight(1f),
                value = text,
                onValueChange = onTextChange,
                isEnabled = enable,
                placeholder = placeholder
            )

            MicButton(
                modifier = Modifier.size(24.dp),
                userInputState = inputState,
                isEnabled = enable,
                waveformPulse = waveformPulse,
                onClick = onMicPressed
            )

            Spacer(modifier = Modifier.width(8.dp))

            SendButton(
                modifier = Modifier.size(24.dp),
                isEnabled = (inputState is UserInputState.Editing) && !isProcessing,
                onSend = {
                    if (text.isNotBlank()) {
                        onSend(text)
                    }
                }
            )
        }
    }
}
