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
import com.adobe.marketing.mobile.concierge.chat.simulation.SpeechSimulator

/**
 * ViewModel for managing the observable state of the Concierge Chat UI.
 * Coordinates between UI events, input state management, and message handling.
 */
class ConciergeChatViewModel : ViewModel() {
    
    // state flows
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    // input state manager
    private val inputStateManager = InputStateManager()
    
    // message manager
    private val messageManager = MessageManager()
    
    // speech simulator (to be replaced with actual speech recognition and brand concierge integration)
    private val speechSimulator = SpeechSimulator()
    
    /**
     * Processes incoming UI events and updates the chat state accordingly.
     */
    fun processChatUiEvent(event: ChatUiEvent) {
        when (event) {
            is ChatUiEvent.SendMessageClicked -> handleSendMessageClicked()
            is ChatUiEvent.RecordingStopped -> handleRecordingStopped()
            is ChatUiEvent.RecordingStarted -> handleRecordingStarted()
            is ChatUiEvent.TranscriptionComplete -> handleTranscriptionComplete(event.text)
            is ChatUiEvent.TranscriptionError -> handleTranscriptionError(event.error)
        }
        
        updateUiState()
    }
    
    // ui event handlers
    fun handleTextInput(text: String) {
        inputStateManager.handleTextInput(text)
        updateUiState()
    }

    private fun handleSendMessageClicked() {
        val currentText = inputStateManager.getCurrentText()
        if (currentText.isNotBlank()) {
            messageManager.addUserMessage(currentText)
            inputStateManager.handleSendMessage()
            updateUiState()
            
            // simulate bot response with delay
            viewModelScope.launch {
                delay(speechSimulator.getSimulatedResponseDelay()) // Simulate network delay
                val botMessage = speechSimulator.simulateResponse(currentText)
                messageManager.addBotMessage(botMessage)
                updateUiState()
            }
        }
    }
    
    private fun handleRecordingStopped() {
        if (inputStateManager.isCurrentlyRecording()) {
            inputStateManager.handleRecordingComplete()
            simulateTranscriptionProcessing()
        } else {
            inputStateManager.handleStartRecording()
        }
        updateUiState()
    }
    
    private fun handleRecordingStarted() {
        inputStateManager.handleRecordingComplete()
        updateUiState()
    }
    
    private fun handleTranscriptionComplete(text: String) {
        inputStateManager.handleTranscriptionComplete(text)
        updateUiState()
    }
    
    private fun handleTranscriptionError(error: String) {
        inputStateManager.handleTranscriptionError(error)
        updateUiState()
    }

    // simulates transcription processing with a delay and generates mock transcribed text
    private fun simulateTranscriptionProcessing() {
        viewModelScope.launch {
            delay(speechSimulator.getSimulatedTranscriptionDelay())
            val simulatedText = speechSimulator.generateSimulatedTranscription()
            inputStateManager.handleTranscriptionComplete(simulatedText)
            updateUiState()
        }
    }

    // updates the UI state based on the current input and message states
    private fun updateUiState() {
        val inputState = inputStateManager.getCurrentState()
        val messages = messageManager.getMessages()
        
        _uiState.update { currentState ->
            currentState.copy(
                messages = messages,
                inputText = inputState.text,
                isInputEnabled = inputState.isEnabled,
                isRecording = inputState.isRecording,
                isTranscribing = inputState.isTranscribing,
                errorMessage = inputState.errorMessage,
                canSendMessage = inputState.canSendMessage
            )
        }
    }
}

/**
 * Manages the input state machine and chat input data.
 */
private class InputStateManager {
    private val _inputState = MutableStateFlow<UserInputState>(UserInputState.Empty)
    private val _inputData = MutableStateFlow(ChatInputData.EMPTY)
    
    fun getCurrentState(): ChatInputData = _inputData.value
    fun getCurrentText(): String = _inputData.value.text
    fun isCurrentlyRecording(): Boolean = _inputState.value is UserInputState.Recording

    fun handleStartRecording() {
        processInputEvent(ChatInputEvent.StartMic)
    }
    
    fun handleRecordingComplete() {
        processInputEvent(ChatInputEvent.RecordingComplete)
    }
    
    fun handleTranscriptionComplete(text: String) {
        processInputEvent(ChatInputEvent.TranscriptionComplete(text))
    }
    
    fun handleTranscriptionError(error: String) {
        processInputEvent(ChatInputEvent.TranscriptionError(error))
    }
    
    fun handleSendMessage() {
        processInputEvent(ChatInputEvent.SendMessage)
    }
    
    fun handleTextInput(text: String) {
        updateInputData { 
            copy(
                text = text,
                canSendMessage = text.isNotBlank()
            )
        }

        if (_inputState.value !is UserInputState.Recording && 
            _inputState.value !is UserInputState.Transcribing && 
            _inputState.value !is UserInputState.Error) {
            _inputState.value = UserInputState.Editing
        }
    }

    private fun processInputEvent(event: ChatInputEvent) {
        val state = _inputState.value
        when (event) {
            is ChatInputEvent.StartMic -> handleStartMic(state)
            is ChatInputEvent.RecordingComplete -> handleRecordingComplete(state)
            is ChatInputEvent.TranscriptionComplete -> handleTranscriptionComplete(event, state)
            is ChatInputEvent.TranscriptionError -> handleTranscriptionError(event, state)
            is ChatInputEvent.SendMessage -> handleSendMessage(state)
            is ChatInputEvent.InputEnabledChanged -> handleInputEnabledChanged(event, state)
        }
    }

    // mark: chat input event handlers
    private fun handleStartMic(currentState: UserInputState) {
        updateInputData { copy(isRecording = true) }
        if (currentState is UserInputState.Empty || currentState is UserInputState.Editing) {
            _inputState.value = UserInputState.Recording
        }
    }
    
    private fun handleRecordingComplete(currentState: UserInputState) {
        updateInputData { copy(isRecording = false, isTranscribing = true) }
        if (currentState is UserInputState.Recording) {
            _inputState.value = UserInputState.Transcribing
        }
    }
    
    private fun handleTranscriptionComplete(event: ChatInputEvent.TranscriptionComplete, currentState: UserInputState) {
        updateInputData { 
            copy(
                voiceInputText = event.transcribedText,
                text = event.transcribedText,
                canSendMessage = event.transcribedText.isNotBlank(),
                isTranscribing = false
            )
        }
        
        if (currentState is UserInputState.Transcribing) {
            _inputState.value = UserInputState.Editing
        }
    }
    
    private fun handleTranscriptionError(event: ChatInputEvent.TranscriptionError, currentState: UserInputState) {
        updateInputData { copy(errorMessage = event.error) }
        
        if (currentState is UserInputState.Transcribing) {
            _inputState.value = UserInputState.Error(event.error)
        }
    }
    
    private fun handleSendMessage(currentState: UserInputState) {
        if (currentState is UserInputState.Editing && _inputData.value.canSendMessage) {
            updateInputData { 
                copy(
                    text = "",
                    voiceInputText = "",
                    canSendMessage = false,
                    isRecording = false,
                    isTranscribing = false
                ) 
            }
            _inputState.value = UserInputState.Empty
        }
    }
    
    private fun handleInputEnabledChanged(event: ChatInputEvent.InputEnabledChanged, currentState: UserInputState) {
        updateInputData { copy(isEnabled = event.enabled) }
        
        if (event.enabled) {
            if (currentState is UserInputState.Error) {
                _inputState.value = UserInputState.Empty
            }
        } else {
            _inputState.value = UserInputState.Error("Input disabled")
        }
    }
    
    private inline fun updateInputData(update: ChatInputData.() -> ChatInputData) {
        _inputData.value = _inputData.value.update()
    }
}

/**
 * Manages chat messages and their lifecycle.
 */
private class MessageManager {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    
    fun getMessages(): List<ChatMessage> = _messages.value
    
    fun addUserMessage(text: String) {
        val userMessage = createChatMessage(text, isFromUser = true)
        addMessage(userMessage)
    }
    
    fun addBotMessage(text: String) {
        val botMessage = createChatMessage(text, isFromUser = false)
        addMessage(botMessage)
    }
    
    private fun addMessage(message: ChatMessage) {
        _messages.update { currentMessages ->
            currentMessages + message
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
}