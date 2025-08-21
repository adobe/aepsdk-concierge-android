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
    val state: StateFlow<ChatScreenState> = _state.asStateFlow()

    private val _data = MutableStateFlow(ChatScreenData.EMPTY)
    val data: StateFlow<ChatScreenData> = _data.asStateFlow()

    fun processEvent(event: UiEvent) {
        when (event) {
            is UiEvent.TextProcessingComplete -> handleTextProcessingComplete(event.text)
            is UiEvent.Error -> handleProcessingError(event.message)
            is UiEvent.Reset -> handleResetChat()
            is UiEvent.SendMessage -> handleSendMessage()
        }
    }

    private fun handleTextProcessingComplete(text: String) {
        _data.update { it.copy(inputText = text, canSendMessage = text.isNotBlank()) }
        
        val inputState = if (text.isBlank()) {
            UserInputState.Empty
        } else {
            UserInputState.Editing
        }
        
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Idle -> ChatScreenState.Idle(inputState)
                is ChatScreenState.Processing -> ChatScreenState.Processing(inputState, currentState.message)
                is ChatScreenState.Error -> ChatScreenState.Error(inputState, currentState.error)
            }
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
                isRecording = false,
                isInputEnabled = true,
                isProcessing = false
            )
        }
        _state.update { 
            ChatScreenState.Idle(UserInputState.Empty)
        }
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
     * Get the current messages list
     */
    fun getMessages(): List<ChatMessage> = _data.value.messages

    /**
     * Check if the chat is currently processing
     */
    fun isProcessing(): Boolean = _data.value.isProcessing

    /**
     * Check if the input is enabled
     */
    fun isInputEnabled(): Boolean = _data.value.isInputEnabled

    /**
     * Check if a message can be sent
     */
    fun canSendMessage(): Boolean = _data.value.canSendMessage

    /**
     * Update the input text as user types
     */
    fun updateInputText(text: String) {
        _data.update { it.copy(inputText = text, canSendMessage = text.isNotBlank()) }
        
        val inputState = if (text.isBlank()) {
            UserInputState.Empty
        } else {
            UserInputState.Editing
        }
        
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
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Idle -> ChatScreenState.Idle(voiceState)
                is ChatScreenState.Processing -> ChatScreenState.Processing(voiceState, currentState.message)
                is ChatScreenState.Error -> ChatScreenState.Error(voiceState, currentState.error)
            }
        }
    }
}
