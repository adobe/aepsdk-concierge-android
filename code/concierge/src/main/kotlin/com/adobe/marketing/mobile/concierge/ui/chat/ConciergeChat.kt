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

package com.adobe.marketing.mobile.concierge.ui.chat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenData
import com.adobe.marketing.mobile.concierge.ui.state.UiEvent
import com.adobe.marketing.mobile.concierge.ui.components.header.ChatHeader
import com.adobe.marketing.mobile.concierge.ui.components.messages.MessageList
import com.adobe.marketing.mobile.concierge.ui.components.input.UserInput
import com.adobe.marketing.mobile.concierge.ui.components.overlay.ErrorOverlay

@Composable
fun ConciergeChat(viewModel: ConciergeChatViewModel) {
    val data by viewModel.data.collectAsState()
    val inputState by viewModel.inputState.collectAsState()
    
    ConciergeChatContent(
        data = data,
        inputState = inputState,
        onMessageSent = { text ->
            viewModel.processEvent(UiEvent.TextProcessingComplete(text))
            viewModel.processEvent(UiEvent.SendMessage)
        },
        onInputTextChanged = { text ->
            viewModel.processEvent(UiEvent.TextProcessingComplete(text))
        },
        onVoiceRecordingStarted = {
            viewModel.startVoiceRecording()
        },
        onVoiceRecordingStopped = {
            viewModel.stopVoiceRecording()
        },
        onErrorDismissed = {
            viewModel.processEvent(UiEvent.Reset)
        }
    )
}

@Composable
fun ConciergeChatContent(
    data: ChatScreenData,
    inputState: UserInputState,
    onMessageSent: (String) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onVoiceRecordingStarted: () -> Unit,
    onVoiceRecordingStopped: () -> Unit,
    onErrorDismissed: () -> Unit
) {
    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Prevent swipe gestures from dismissing the composable
                        detectDragGestures { _, _ ->
                            // Consume all drag gestures to prevent dismissal
                        }
                    }
            ) {
                // Header at the top
                ChatHeader()
                
                // Messages list
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // Take up available space
                ) {
                    MessageList(
                        messages = data.messages,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter) // Anchor to bottom of available space
                            .padding(horizontal = 16.dp)
                    )
                }
                
                // User input at the bottom - directly below MessageList
                UserInput(
                    inputText = data.inputText,
                    isInputEnabled = data.isInputEnabled,
                    inputState = inputState,
                    canSendMessage = data.canSendMessage,
                    isProcessing = data.isProcessing,
                    onMessageSent = onMessageSent,
                    onInputTextChanged = onInputTextChanged,
                    onVoiceRecordingStarted = onVoiceRecordingStarted,
                    onVoiceRecordingStopped = onVoiceRecordingStopped,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Error overlay if there's an error (positioned as overlay on top)
            if (data.errorMessage != null) {
                ErrorOverlay(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    errorMessage = data.errorMessage,
                    onDismiss = onErrorDismissed
                )
            }
        }
    }
}


