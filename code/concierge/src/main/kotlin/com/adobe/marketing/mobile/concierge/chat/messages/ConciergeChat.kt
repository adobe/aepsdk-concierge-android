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

package com.adobe.marketing.mobile.concierge.chat.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.chat.userinput.ChatInputField
import com.adobe.marketing.mobile.concierge.chat.userinput.UserInputState

@Composable
fun ConciergeChat(viewModel: ConciergeChatViewModel) {
    val data by viewModel.data.collectAsState()
    
    ConciergeChatContent(
        data = data,
        onMessageSent = { text ->
            viewModel.processEvent(UiEvent.TextProcessingComplete(text))
            viewModel.processEvent(UiEvent.SendMessage)
        },
        onInputTextChanged = { text ->
            viewModel.updateInputText(text)
        },
        onVoiceRecordingStarted = {
            viewModel.setVoiceInputState(UserInputState.Recording)
        },
        onVoiceRecordingStopped = {
            viewModel.setVoiceInputState(UserInputState.Transcribing)
        },
        onErrorDismissed = {
            viewModel.processEvent(UiEvent.Reset)
        }
    )
}

@Composable
fun ConciergeChatContent(
    data: ChatScreenData,
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = "BC Chat Frame",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
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
                    isRecording = data.isRecording,
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

@Composable
fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show messages in chronological order (oldest first, newest last)
        items(messages) { message ->
            ChatMessageItem(message = message)
        }
    }
}



@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    inputText: String,
    isInputEnabled: Boolean,
    isRecording: Boolean,
    canSendMessage: Boolean,
    isProcessing: Boolean = false,
    onMessageSent: (String) -> Unit,
    onInputTextChanged: (String) -> Unit,
    onVoiceRecordingStarted: () -> Unit,
    onVoiceRecordingStopped: () -> Unit
) {
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
            text = inputText,
            onTextChange = onInputTextChanged,
            onMessageCreated = onMessageSent,
            placeholder = "Type a message or use voice input...",
            isEnabled = isInputEnabled,
            isRecording = isRecording,
            canSendMessage = canSendMessage,
            onVoiceRecordingStarted = onVoiceRecordingStarted,
            onVoiceRecordingStopped = onVoiceRecordingStopped
        )
    }
}

@Composable
fun ErrorOverlay(
    modifier: Modifier = Modifier,
    errorMessage: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Text(
                text = "Dismiss",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { _, _ ->
                            onDismiss()
                        }
                    }
            )
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFromUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.isFromUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = if (message.isFromUser) "You" else "Assistant",
                style = MaterialTheme.typography.bodySmall,
                color = if (message.isFromUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
