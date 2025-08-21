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

package com.adobe.marketing.mobile.concierge.chat.userinput

import androidx.compose.runtime.Stable
import com.adobe.marketing.mobile.concierge.chat.messages.UserInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the different states of the input stream (text/voice input).
 */
sealed class UserInputState {
    // No input, ready for input
    object Empty : UserInputState()
    
    // User is actively recording audio
    object Recording : UserInputState()
    
    // Processing speech to text conversion
    object Transcribing : UserInputState()

    // Text content (typed or transcribed) received and ready for editing
    object Editing : UserInputState()
    
    // Error state
    data class Error(val message: String) : UserInputState()
}

/**
 * Represents events that can occur when capturing chat input.
 */
sealed class UserInputEvent {
    // Text or text to speech input received
    data class InputReceived(val newText: String) : UserInputEvent()
    
    // User started voice recording
    object RecordingStarted : UserInputEvent()
    
    // Voice recording completed
    object RecordingComplete : UserInputEvent()
    
    // Voice processing completed with a result
    data class TranscriptionComplete(val transcribedText: String) : UserInputEvent()
    
    // Voice processing failed encountered an error
    data class TranscriptionError(val error: String) : UserInputEvent()
    
    // User pressed the send button to send the received text
    object SendMessage : UserInputEvent()
    
    // Input field was enabled/disabled
    data class InputEnabledChanged(val enabled: Boolean) : UserInputEvent()
}

/**
 * Represents the current data state of the chat input.
 */
@Stable
data class UserInputData(
    val text: String = "",
    val isEnabled: Boolean = true,
    val errorMessage: String? = null,
    val voiceInputText: String = "",
    val canSendMessage: Boolean = false
) {
    companion object {
        val EMPTY = UserInputData()
    }
}

/**
 * State machine for managing chat input behavior and state transitions.
 */
class UserInputStateMachine {

    private val _userInputState = MutableStateFlow<UserInputState>(UserInputState.Empty)
    val userInputState: StateFlow<UserInputState> = _userInputState.asStateFlow()

    private val _data = MutableStateFlow(UserInputData.EMPTY)
    val data: StateFlow<UserInputData> = _data.asStateFlow()

    /**
     * Process an event then transition to the appropriate state.
     */
    fun processEvent(event: UserInputEvent): Boolean {
        val currentState = _userInputState.value
        val newState = when (event) {
            is UserInputEvent.InputReceived -> handleInputReceived(event, currentState)
            is UserInputEvent.RecordingStarted -> handleStartRecording(currentState)
            is UserInputEvent.RecordingComplete -> handleRecordingComplete(currentState)
            is UserInputEvent.TranscriptionComplete -> handleTranscriptionComplete(event, currentState)
            is UserInputEvent.TranscriptionError -> handleTranscriptionError(event, currentState)
            is UserInputEvent.SendMessage -> handleSendMessage(currentState)
            is UserInputEvent.InputEnabledChanged -> handleInputEnabledChanged(event, currentState)
        }

        val stateChanged = newState != currentState
        if (stateChanged) {
            _userInputState.value = newState
        }

        return stateChanged
    }

    // MARK: - State Transition Handlers
    private fun handleInputReceived(event: UserInputEvent.InputReceived, currentState: UserInputState): UserInputState {
        updateData {
            copy(
                text = event.newText,
                canSendMessage = event.newText.isNotBlank()
            )
        }

        return when (currentState) {
            is UserInputState.Empty,
            is UserInputState.Error -> UserInputState.Empty
            else -> currentState
        }
    }

    private fun handleStartRecording(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Empty -> UserInputState.Recording
            else -> currentState
        }

    private fun handleRecordingComplete(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Recording -> UserInputState.Transcribing
            else -> currentState
        }

    private fun handleTranscriptionComplete(event: UserInputEvent.TranscriptionComplete, currentState: UserInputState): UserInputState {
        updateData {
            copy(
                voiceInputText = event.transcribedText,
                text = event.transcribedText,
                canSendMessage = event.transcribedText.isNotBlank()
            )
        }

        return when (currentState) {
            is UserInputState.Transcribing -> UserInputState.Editing
            else -> currentState
        }
    }

    private fun handleTranscriptionError(event: UserInputEvent.TranscriptionError, currentState: UserInputState): UserInputState {
        updateData { copy(errorMessage = event.error) }

        return when (currentState) {
            is UserInputState.Transcribing -> UserInputState.Error(event.error)
            else -> currentState
        }
    }

    private fun handleSendMessage(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Editing -> {
                if (_data.value.canSendMessage) UserInputState.Empty else currentState
            }
            is UserInputState.Recording, UserInputState.Transcribing -> {
                // If currently recording or transcribing, we don't send the message
                currentState
            }

            else -> currentState
        }

    private fun handleInputEnabledChanged(event: UserInputEvent.InputEnabledChanged, currentState: UserInputState): UserInputState {
        updateData { copy(isEnabled = event.enabled) }

        return if (event.enabled) {
            when (currentState) {
                is UserInputState.Error -> UserInputState.Empty
                else -> currentState
            }
        } else {
            UserInputState.Error("Input disabled")
        }
    }

    // MARK: - Helper Methods

    /**
     * Updates the data state using the provided update function.
     */
    private inline fun updateData(update: UserInputData.() -> UserInputData) {
        _data.value = _data.value.update()
    }

    /**
     * Get the current text input from the state to be used for sending messages.
     */
    fun getCurrentText(): String = _data.value.text
}

