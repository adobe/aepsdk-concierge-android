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

/**
 * Represents the different states of receiving user input (text/voice input).
 */
sealed class UserInputState {
    // ready for input
    object Empty : UserInputState()

    // user is actively editing text input
    object Editing : UserInputState()
    
    // user is actively recording audio
    object Recording : UserInputState()
    
    // processing speech to text conversion
    object Transcribing : UserInputState()
    
    // error state
    data class Error(val message: String) : UserInputState()
}

/**
 * Represents events that can occur when capturing chat input.
 */
sealed class ChatInputEvent {
    // user started voice recording
    object StartMic : ChatInputEvent()
    
    // voice recording completed
    object RecordingComplete : ChatInputEvent()
    
    // voice processing completed with a result
    data class TranscriptionComplete(val transcribedText: String) : ChatInputEvent()
    
    // voice processing failed encountered an error
    data class TranscriptionError(val error: String) : ChatInputEvent()
    
    // user pressed the send button to send the message
    object SendMessage : ChatInputEvent()
    
    // input field was enabled/disabled
    data class InputEnabledChanged(val enabled: Boolean) : ChatInputEvent()
    
    // reset to initial state
    object Reset : ChatInputEvent()
}

/**
 * Represents the current data state of the chat input.
 */
data class ChatInputData(
    val text: String = "",
    val isEnabled: Boolean = true,
    val errorMessage: String? = null,
    val voiceInputText: String = "",
    val canSendMessage: Boolean = false
) {
    companion object {
        val EMPTY = ChatInputData()
    }
}
