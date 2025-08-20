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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Main chat screen composable that holds the conversation panel and user input text field.
 * This composable also observes the ViewModel state and emits UI events.
 *
 * @param modifier The modifier to be applied to the chat layout
 * @param viewModel The [ConciergeChatViewModel] instance that manages the chat state and events
 */
@Composable
fun ConciergeChat(
    modifier: Modifier = Modifier,
    viewModel: ConciergeChatViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // area to display outgoing and incoming messages
        ConversationPanel(
            modifier = Modifier.weight(1f),
            messages = uiState.messages
        )
        
        // user input at the bottom
        UserInput(
            modifier = Modifier.fillMaxWidth(),
            inputText = uiState.inputText,
            isEnabled = uiState.isInputEnabled,
            isRecording = uiState.isRecording,
            isTranscribing = uiState.isTranscribing,
            canSendMessage = uiState.canSendMessage,
            errorMessage = uiState.errorMessage,
            onTextChange = { text ->
                viewModel.handleTextInput(text)
            },
            onSendMessage = {
                viewModel.processChatUiEvent(ChatUiEvent.SendMessageClicked)
            },
            onMicButtonClick = {
                viewModel.processChatUiEvent(ChatUiEvent.RecordingStopped)
            }
        )
    }
}