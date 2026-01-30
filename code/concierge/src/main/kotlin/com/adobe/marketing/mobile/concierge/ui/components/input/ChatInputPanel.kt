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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

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
 * @param borderColors List of colors for the animated border gradient
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
    onSend: (String) -> Unit,
    onVoiceCancel: (() -> Unit)? = null,
    borderColors: List<Color> = emptyList(),
    isFocused: Boolean = false
) {
    val style = ConciergeStyles.inputPanelStyle
    
    // Determine border appearance based on focus state
    val borderModifier = when {
        isFocused && style.focusBorderWidth > 0.dp && style.focusBorderColor != null -> {
            Modifier.border(
                width = style.focusBorderWidth,
                color = style.focusBorderColor,
                shape = style.innerShape
            )
        }
        !isFocused && style.borderWidth > 0.dp && style.borderColor != null -> {
            Modifier.border(
                width = style.borderWidth,
                color = style.borderColor,
                shape = style.innerShape
            )
        }
        else -> Modifier
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier),
        shape = style.innerShape,
        color = style.backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(style.innerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatTextField(
                modifier = Modifier.weight(1f),
                value = text,
                onValueChange = onTextChange,
                isEnabled = enable,
                placeholder = if (inputState is UserInputState.Recording) style.listeningPlaceholderText else placeholder
            )

            // Input action buttons (mic and send) with state-aware animations
            InputActionButtons(
                inputState = inputState,
                text = text,
                isProcessing = isProcessing,
                onMicPressed = onMicPressed,
                onVoiceCancel = { onVoiceCancel?.invoke() },
                onSend = onSend
            )
        }
    }
}