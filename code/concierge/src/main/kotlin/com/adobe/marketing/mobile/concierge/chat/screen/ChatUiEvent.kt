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

// UI events emitted by the ChatUi
sealed class ChatUiEvent {
    object SendMessageClicked : ChatUiEvent()
    object RecordingStopped : ChatUiEvent()
    object RecordingStarted : ChatUiEvent()
    data class TranscriptionComplete(val text: String) : ChatUiEvent()
    data class TranscriptionError(val error: String) : ChatUiEvent()
}

// UI state observed by the ChatUi
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val errorMessage: String? = null,
    val canSendMessage: Boolean = false
)

// Chat message data class
data class ChatMessage(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
