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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing the observable state of the Concierge Chat UI.
 * Handles user input, manages chat messages, and simulates bot responses / handles responses
 * from brand concierge.
 */
class ConciergeChatViewModel : ViewModel() {
    // input state management
    private val _userInputState = MutableStateFlow<UserInputState>(UserInputState.Empty)
    val userInputState: StateFlow<UserInputState> = _userInputState.asStateFlow()
    private val _inputData = MutableStateFlow(ChatInputData.EMPTY)
    
    // uI state management
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // current recording job tracking
    private var recordingJob: kotlinx.coroutines.Job? = null

    // Mark: UI event processing
    /**
     * Processes incoming UI events and updates the chat state accordingly.
     * This method is called from the UI layer to handle user interactions.
     *
     * @param event The [ChatUiEvent] representing the user action
     */
    fun processChatUiEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessageClicked -> handleSendMessageClicked()
            is ChatUiEvent.RecordingStopped -> handleRecordingStopped()
            is ChatUiEvent.RecordingStarted -> handleRecordingStarted()
            is ChatUiEvent.TranscriptionComplete -> handleTranscriptionComplete(event.text)
            is ChatUiEvent.TranscriptionError -> handleTranscriptionError(event.error)
        }
        
        // update UI state to reflect input state machine changes
        updateUiStateFromInputMachine()
    }

    // ui event handlers
    private fun handleSendMessageClicked() {
        val currentText = _inputData.value.text
        if (currentText.isNotBlank()) {
            addUserMessage(currentText)
            simulateBotResponse(currentText)
            // reset input after sending
            processChatInputEvent(ChatInputEvent.SendMessage)
        }
    }
    
    private fun handleRecordingStopped() {
        val currentState = _userInputState.value
        
        if (currentState is UserInputState.Recording) {
            recordingJob?.cancel() // cancel the ongoing recording
            recordingJob = null
            
            processChatInputEvent(ChatInputEvent.RecordingComplete)

            // to-do: remove following code when actual speech-to-text implemented
            // simulate transcription processing time (1 second)
            viewModelScope.launch {
                delay(1000)
                
                // simulate successful transcription with sample text
                val simulatedTranscription = generateSimulatedTranscription()
                processChatInputEvent(ChatInputEvent.TranscriptionComplete(simulatedTranscription))
                
                // update UI state after transcription
                updateUiStateFromInputMachine()
            }
        } else {
            // start recording
            processChatInputEvent(ChatInputEvent.StartMic)
        }
    }
    
    private fun handleRecordingStarted() {
        processChatInputEvent(ChatInputEvent.RecordingComplete)
    }
    
    private fun handleTranscriptionComplete(text: String) {
        processChatInputEvent(ChatInputEvent.TranscriptionComplete(text))
        // update UI state after transcription
        updateUiStateFromInputMachine()
    }
    
    private fun handleTranscriptionError(error: String) {
        processChatInputEvent(ChatInputEvent.TranscriptionError(error))
    }
    
    // helper methods for managing chat messages
    private fun addUserMessage(text: String) {
        val userMessage = createChatMessage(text, isFromUser = true)
        _uiState.update { currentState ->
            currentState.copy(
                messages = currentState.messages + userMessage
            )
        }
    }

    /**
     * Generates a simulated transcription message for testing purposes.
     */
    private fun generateSimulatedTranscription(): String {
        val samplePhrases = listOf(
            "Hello, how can you help me today?",
            "I have a question about my account",
            "Can you please explain the pricing?",
            "I need assistance with the mobile app",
            "What are the available features?",
            "Thank you for your help"
        )
        return samplePhrases.random()
    }

    // to-do: replace with message from brand concierge
    private fun simulateBotResponse(userText: String) {
        viewModelScope.launch {
            // simulate network delay
            delay(1000)
            
            val botMessage = createChatMessage(
                text = "Thanks for your message: '$userText'",
                isFromUser = false
            )
            
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + botMessage
                )
            }
        }
    }
    
    private fun createChatMessage(text: String, isFromUser: Boolean): ChatMessage {
        return ChatMessage(
            id = generateMessageId(isFromUser),
            text = text,
            isFromUser = isFromUser
        )
    }
    
    private fun generateMessageId(isFromUser: Boolean): String {
        val prefix = if (isFromUser) "msg" else "bot"
        return "${prefix}_${System.currentTimeMillis()}"
    }

    // Mark: chat input event processing
    /**
     * Processes a [ChatInputEvent] to update the user input state machine.
     * This method handles the state transitions based on the event type and updates the
     * input data accordingly.
     */
    private fun processChatInputEvent(event: ChatInputEvent) {
        val currentState = _userInputState.value
        val newState = when (event) {
            is ChatInputEvent.StartMic -> handleStartMic(currentState)
            is ChatInputEvent.RecordingComplete -> handleRecordingComplete(currentState)
            is ChatInputEvent.TranscriptionComplete -> handleTranscriptionComplete(event, currentState)
            is ChatInputEvent.TranscriptionError -> handleTranscriptionError(event, currentState)
            is ChatInputEvent.SendMessage -> handleSendMessage(currentState)
            is ChatInputEvent.InputEnabledChanged -> handleInputEnabledChanged(event, currentState)
            is ChatInputEvent.Reset -> handleReset()
        }
        
        val stateChanged = newState != currentState
        if (stateChanged) {
            _userInputState.value = newState
        }
    }
    
    // chat input event handlers
    private fun handleStartMic(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Empty,
            is UserInputState.Editing -> UserInputState.Recording
            else -> currentState
        }
    
    private fun handleRecordingComplete(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Recording -> UserInputState.Transcribing
            else -> currentState
        }
    
    private fun handleTranscriptionComplete(event: ChatInputEvent.TranscriptionComplete, currentState: UserInputState): UserInputState {
        updateInputData { 
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
    
    private fun handleTranscriptionError(event: ChatInputEvent.TranscriptionError, currentState: UserInputState): UserInputState {
        updateInputData { copy(errorMessage = event.error) }
        
        return when (currentState) {
            is UserInputState.Transcribing -> UserInputState.Error(event.error)
            else -> currentState
        }
    }
    
    private fun handleSendMessage(currentState: UserInputState): UserInputState =
        when (currentState) {
            is UserInputState.Editing -> {
                if (_inputData.value.canSendMessage) {
                    // clear the input data when sending a message
                    updateInputData { 
                        copy(
                            text = "",
                            voiceInputText = "",
                            canSendMessage = false
                        ) 
                    }
                    UserInputState.Empty
                } else currentState
            }
            else -> currentState
        }
    
    private fun handleInputEnabledChanged(event: ChatInputEvent.InputEnabledChanged, currentState: UserInputState): UserInputState {
        updateInputData { copy(isEnabled = event.enabled) }
        
        return if (event.enabled) {
            when (currentState) {
                is UserInputState.Error -> UserInputState.Empty
                else -> currentState
            }
        } else {
            UserInputState.Error("Input disabled")
        }
    }
    
    private fun handleReset(): UserInputState {
        _inputData.value = ChatInputData.EMPTY
        return UserInputState.Empty
    }
    
    // chat input helpers
    /**
     * Updates the input data state using the provided update function.
     */
    private inline fun updateInputData(update: ChatInputData.() -> ChatInputData) {
        _inputData.value = _inputData.value.update()
    }
    
    /**
     * Updates the UI state to reflect the current state of the input state machine
     */
    private fun updateUiStateFromInputMachine() {
        val inputData = _inputData.value
        val inputState = _userInputState.value
        
        _uiState.update { currentState ->
            currentState.copy(
                inputText = inputData.text,
                isInputEnabled = inputData.isEnabled,
                isRecording = inputState is UserInputState.Recording,
                isTranscribing = inputState is UserInputState.Transcribing,
                errorMessage = inputData.errorMessage,
                canSendMessage = inputData.canSendMessage
            )
        }
    }
}