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

import androidx.compose.runtime.Stable
import com.adobe.marketing.mobile.concierge.chat.userinput.UserInputState

/**
 * Represents the overall state of the chat screen.
 */
sealed class ChatScreenState {
    /**
     * Chat is in idle state, waiting for user interaction.
     */
    data class Idle(val inputState: UserInputState) : ChatScreenState()
    
    /**
     * Chat is actively processing a user message.
     */
    data class Processing(val inputState: UserInputState, val message: String) : ChatScreenState()
    
    /**
     * Chat is in an error state.
     */
    data class Error(val inputState: UserInputState, val error: String) : ChatScreenState()
}

/**
 * Represents UI events that can be processed by the ViewModel.
 */
sealed class UiEvent {
    /**
     * Text captured from keyboard input or voice transcription.
     */
    data class TextProcessingComplete(val text: String) : UiEvent()

    /**
     * User pressed the send button.
     */
    object SendMessage : UiEvent()

    /**
     * An error occurred while processing the input.
     */
    data class Error(val message: String) : UiEvent()

    /**
     * User dismissed the error, returning to idle state.
     */
    object Reset: UiEvent()
}

/**
 * Represents the current data state of the chat screen.
 */
@Stable
data class ChatScreenData(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isInputEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val canSendMessage: Boolean = false
) {
    companion object {
        val EMPTY = ChatScreenData()
    }
}
