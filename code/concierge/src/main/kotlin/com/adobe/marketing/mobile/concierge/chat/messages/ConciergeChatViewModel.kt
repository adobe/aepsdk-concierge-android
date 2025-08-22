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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.concierge.chat.simulation.SpeechSimulator
import com.adobe.marketing.mobile.concierge.chat.userinput.UserInputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConciergeChatViewModel : ViewModel() {
    private val _state = MutableStateFlow<ChatScreenState>(
        ChatScreenState.Idle(UserInputState.Empty)
    )
    private val _inputState = MutableStateFlow<UserInputState>(
        UserInputState.Empty
    )

    val state: StateFlow<ChatScreenState> = _state.asStateFlow()
    val inputState: StateFlow<UserInputState> = _inputState.asStateFlow()

    private val _data = MutableStateFlow(ChatScreenData.EMPTY)
    val data: StateFlow<ChatScreenData> = _data.asStateFlow()

    /* Process incoming UI events */
    fun processEvent(event: UiEvent) {
        when (event) {
            is UiEvent.TextProcessingComplete -> handleInputText(event.text)
            is UiEvent.Error -> handleProcessingError(event.message)
            is UiEvent.Reset -> handleResetChat()
            is UiEvent.SendMessage -> handleSendMessage()
        }
    }

    private fun handleProcessingError(message: String) {
        _data.update { it.copy(errorMessage = message) }
        _state.update { currentState ->
            val inputState = UserInputState.Error(message)
            when (currentState) {
                is ChatScreenState.Error -> ChatScreenState.Error(inputState, currentState.error)
                else -> ChatScreenState.Error(inputState, message)
            }
        }
    }

    private fun handleResetChat() {
        _data.update { 
            it.copy(
                inputText = "",
                canSendMessage = false,
                errorMessage = null,
                isInputEnabled = true,
                isProcessing = false
            )
        }
        _state.update { 
            ChatScreenState.Idle(UserInputState.Empty)
        }
        _inputState.update { UserInputState.Empty }
    }

    private fun handleSendMessage() {
        val currentText = _data.value.inputText
        if (currentText.isBlank()) return

        // Add user message to the list
        val userMessage = ChatMessage(
            text = currentText,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )
        
        _data.update { 
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                canSendMessage = false,
                isProcessing = true
            )
        }

        // Transition to processing state
        _state.update { 
            ChatScreenState.Processing(UserInputState.Empty, currentText)
        }

        // Simulate processing and response (replace with actual API call)
        viewModelScope.launch {
            try {
                // Simulate API delay
                kotlinx.coroutines.delay(SpeechSimulator().getSimulatedResponseDelay())
                
                val assistantMessage = ChatMessage(
                    text = SpeechSimulator().simulateResponse(currentText),
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                
                _data.update { 
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isProcessing = false
                    )
                }
                
                // Return to idle state after processing
                _state.update { 
                    ChatScreenState.Idle(UserInputState.Empty)
                }
            } catch (e: Exception) {
                // Handle any errors during processing
                handleProcessingError("Failed to process message: ${e.message}")
            }
        }
    }

    /**
     * Handle text input changes from keyboard or transcription
     */
    fun handleInputText(text: String) {
        _data.update { it.copy(inputText = text, canSendMessage = text.isNotBlank()) }

        _inputState.update {
            if (text.isBlank()) {
                UserInputState.Empty
            } else {
                UserInputState.Editing
            }
        }
        val inputState = _inputState.value

        
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Idle -> ChatScreenState.Idle(inputState)
                is ChatScreenState.Processing -> ChatScreenState.Processing(inputState, currentState.message)
                is ChatScreenState.Error -> ChatScreenState.Error(inputState, currentState.error)
            }
        }
    }

    /**
     * Handle voice input state changes
     */
    fun setVoiceInputState(voiceState: UserInputState) {
        // Update the dedicated input state
        _inputState.update { voiceState }
        
        // Update the UI state
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Idle -> ChatScreenState.Idle(voiceState)
                is ChatScreenState.Processing -> ChatScreenState.Processing(voiceState, currentState.message)
                is ChatScreenState.Error -> ChatScreenState.Error(voiceState, currentState.error)
            }
        }
    }

    /**
     * Start voice recording
     */
    fun startVoiceRecording() {
        setVoiceInputState(UserInputState.Recording)
    }

    /**
     * Stop voice recording and start transcription
     */
    fun stopVoiceRecording() {
        println("DEBUG: Starting voice transcription...")
        setVoiceInputState(UserInputState.Transcribing)
        
        // Simulate transcription delay and then complete
        viewModelScope.launch {
            try {
                // Simulate transcription delay using SpeechSimulator
                val speechSimulator = SpeechSimulator()
                val delay = speechSimulator.getSimulatedTranscriptionDelay()
                println("DEBUG: Transcription delay: ${delay}ms")
                kotlinx.coroutines.delay(delay)
                
                // Generate simulated transcribed text
                val transcribedText = speechSimulator.generateSimulatedTranscription()
                println("DEBUG: Transcribed text: $transcribedText")
                
                // Update the input text with transcribed content
                handleInputText(transcribedText)
                println("DEBUG: Updated input text in ViewModel")
                
                // Set state to editing mode
                setVoiceInputState(UserInputState.Editing)
                println("DEBUG: Transcribed message ready - user must press send button to send")
            } catch (e: Exception) {
                println("DEBUG: Transcription error: ${e.message}")
                // Handle transcription error
                setVoiceInputState(UserInputState.Error("Failed to transcribe voice message: ${e.message}"))
            }
        }
    }
}
