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

package com.adobe.marketing.mobile.concierge.chat

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the different states of the input stream (text/voice input).
 */
sealed class InputStreamState {
    // No input, ready for input
    object Empty : InputStreamState()

    // User is actively editing text input
    object Editing : InputStreamState()
    
    // User is actively recording audio
    object Recording : InputStreamState()
    
    // Processing speech to text conversion
    object Transcribing : InputStreamState()
    
    // Error state
    data class Error(val message: String) : InputStreamState()
}

/**
 * Represents events that can occur when capturing chat input.
 */
sealed class ChatInputEvent {
    // User started typing text in the text field
    object AddContent : ChatInputEvent()
    
    // Text or text to speech input received
    data class InputReceived(val newText: String) : ChatInputEvent()
    
    // User cleared text field
    object DeleteContent : ChatInputEvent()
    
    // User started voice recording
    object StartMic : ChatInputEvent()
    
    // Voice recording completed
    object RecordingComplete : ChatInputEvent()
    
    // Voice processing completed with a result
    data class TranscriptionComplete(val transcribedText: String) : ChatInputEvent()
    
    // Voice processing failed encountered an error
    data class TranscriptionError(val error: String) : ChatInputEvent()
    
    // Permission error when attempting to use microphone
    data class PermissionError(val error: String) : ChatInputEvent()
    
    // User pressed the send button to send the message
    object SendMessage : ChatInputEvent()
    
    // Input field was enabled/disabled
    data class InputEnabledChanged(val enabled: Boolean) : ChatInputEvent()
    
    // Reset to initial state
    object Reset : ChatInputEvent()
}

/**
 * Represents the current data state of the chat input.
 */
@Stable
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

/**
 * State machine for managing chat input behavior and state transitions.
 */
class ChatInputStateMachine {
    
    private val _inputStreamState = MutableStateFlow<InputStreamState>(InputStreamState.Empty)
    val inputStreamState: StateFlow<InputStreamState> = _inputStreamState.asStateFlow()
    
    private val _data = MutableStateFlow(ChatInputData.EMPTY)
    val data: StateFlow<ChatInputData> = _data.asStateFlow()
    
    /**
     * Process an event then transition to the appropriate state.
     */
    fun processEvent(event: ChatInputEvent): Boolean {
        val currentState = _inputStreamState.value
        val newState = when (event) {
            is ChatInputEvent.AddContent -> handleAddContent(currentState)
            is ChatInputEvent.InputReceived -> handleInputReceived(event, currentState)
            is ChatInputEvent.DeleteContent -> handleDeleteContent(currentState)
            is ChatInputEvent.StartMic -> handleStartMic(currentState)
            is ChatInputEvent.RecordingComplete -> handleRecordingComplete(currentState)
            is ChatInputEvent.TranscriptionComplete -> handleTranscriptionComplete(event, currentState)
            is ChatInputEvent.TranscriptionError -> handleTranscriptionError(event, currentState)
            is ChatInputEvent.PermissionError -> handlePermissionError(event, currentState)
            is ChatInputEvent.SendMessage -> handleSendMessage(currentState)
            is ChatInputEvent.InputEnabledChanged -> handleInputEnabledChanged(event, currentState)
            is ChatInputEvent.Reset -> handleReset()
        }
        
        val stateChanged = newState != currentState
        if (stateChanged) {
            _inputStreamState.value = newState
        }
        
        return stateChanged
    }
    
    // MARK: - State Transition Handlers
    
    private fun handleAddContent(currentState: InputStreamState): InputStreamState = 
        when (currentState) {
            is InputStreamState.Empty -> InputStreamState.Editing
            else -> currentState
        }
    
    private fun handleInputReceived(event: ChatInputEvent.InputReceived, currentState: InputStreamState): InputStreamState {
        updateData { 
            copy(
                text = event.newText, 
                canSendMessage = event.newText.isNotBlank()
            ) 
        }
        
        return when (currentState) {
            is InputStreamState.Empty, 
            is InputStreamState.Editing, 
            is InputStreamState.Error -> InputStreamState.Editing
            else -> currentState
        }
    }
    
    private fun handleDeleteContent(currentState: InputStreamState): InputStreamState {
        updateData { 
            copy(
                text = "", 
                canSendMessage = false
            ) 
        }
        
        return when (currentState) {
            is InputStreamState.Editing -> InputStreamState.Empty
            else -> currentState
        }
    }
    
    private fun handleStartMic(currentState: InputStreamState): InputStreamState = 
        when (currentState) {
            is InputStreamState.Empty, 
            is InputStreamState.Editing -> InputStreamState.Recording
            else -> currentState
        }
    
    private fun handleRecordingComplete(currentState: InputStreamState): InputStreamState = 
        when (currentState) {
            is InputStreamState.Recording -> InputStreamState.Transcribing
            else -> currentState
        }
    
    private fun handleTranscriptionComplete(event: ChatInputEvent.TranscriptionComplete, currentState: InputStreamState): InputStreamState {
        updateData { 
            copy(
                voiceInputText = event.transcribedText,
                text = event.transcribedText,
                canSendMessage = event.transcribedText.isNotBlank()
            )
        }
        
        return when (currentState) {
            is InputStreamState.Transcribing -> InputStreamState.Editing
            else -> currentState
        }
    }
    
    private fun handleTranscriptionError(event: ChatInputEvent.TranscriptionError, currentState: InputStreamState): InputStreamState {
        updateData { copy(errorMessage = event.error) }
        
        return when (currentState) {
            is InputStreamState.Transcribing -> InputStreamState.Error(event.error)
            else -> currentState
        }
    }
    
    private fun handlePermissionError(event: ChatInputEvent.PermissionError, currentState: InputStreamState): InputStreamState {
        updateData { copy(errorMessage = event.error) }
        
        return when (currentState) {
            is InputStreamState.Recording -> InputStreamState.Error(event.error)
            else -> currentState
        }
    }
    
    private fun handleSendMessage(currentState: InputStreamState): InputStreamState = 
        when (currentState) {
            is InputStreamState.Editing -> {
                if (_data.value.canSendMessage) InputStreamState.Empty else currentState
            }
            else -> currentState
        }
    
    private fun handleInputEnabledChanged(event: ChatInputEvent.InputEnabledChanged, currentState: InputStreamState): InputStreamState {
        updateData { copy(isEnabled = event.enabled) }
        
        return if (event.enabled) {
            when (currentState) {
                is InputStreamState.Error -> InputStreamState.Empty
                else -> currentState
            }
        } else {
            InputStreamState.Error("Input disabled")
        }
    }
    
    private fun handleReset(): InputStreamState {
        _data.value = ChatInputData.EMPTY
        return InputStreamState.Empty
    }
    
    // MARK: - Helper Methods
    
    /**
     * Updates the data state using the provided update function.
     */
    private inline fun updateData(update: ChatInputData.() -> ChatInputData) {
        _data.value = _data.value.update()
    }

    /**
     * Get the current text input from the state to be used for sending messages.
     */
    fun getCurrentText(): String = _data.value.text
}

