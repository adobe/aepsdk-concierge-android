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

package com.adobe.marketing.mobile.concierge.ui.chat

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechToTextManager
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConciergeChatViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "ConciergeChatViewModel"
    }


    /**
     * Tracks the overall state of the chat flow
     */
    private val _state = MutableStateFlow<ChatScreenState>(
        ChatScreenState.Idle("")
    )
    internal val state: StateFlow<ChatScreenState> = _state.asStateFlow()

    /**
     * Tracks state of the user input area (text input, voice recording, etc.)
     */
    private val _inputState = MutableStateFlow<UserInputState>(
        UserInputState.Empty
    )
    internal val inputState: StateFlow<UserInputState> = _inputState.asStateFlow()

    /**
     * List of chat messages in the conversation
     */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    internal val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()


    private val networkScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val chatService : ConciergeConversationServiceClient = ConciergeConversationServiceClient()

    // Speech to text manager
    private val speechToTextManager = SpeechToTextManager(
        context = getApplication<Application>(),
        onSpeechStarted = {
            _inputState.update { UserInputState.Recording }
        },
        onSpeechEnded = {
            _inputState.update { UserInputState.Transcribing }
        },
        onTranscriptionResult = { transcription ->
            handleTranscriptionResult(transcription)
        },
        onSpeechError = { errorCode ->
            handleSpeechError(errorCode)
        }
    )

    /**
     * Indicates if speech recognition is available on the device
     * TODO: Permission handling should be wrapped and exposed to the app level to handle permission requests
     */
    private val _isSpeechRecognitionAvailable =
        MutableStateFlow(speechToTextManager.isAvailable.value)
    val isSpeechRecognitionAvailable: StateFlow<Boolean> =
        _isSpeechRecognitionAvailable.asStateFlow()
    private val _hasAudioPermission = MutableStateFlow(checkAudioPermission())
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()


    /**
     * Process incoming events from the UI
     * @param event The event to process
     */
    internal fun processEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.Error -> handleProcessingError(event.message)
            is ChatEvent.Reset -> handleResetChat()
            is ChatEvent.SendMessage -> handleSendMessage(event.message)

            is MicEvent.StartRecording -> {
                startSpeechRecognition()
            }

            is MicEvent.StopRecording -> {
                stopSpeechRecognition()
            }
        }
    }

    /**
     * Called when the text input state changes (e.g. user types or deletes text)
     * @param hasContent True if there is text content, false if empty
     */
    internal fun onTextStateChanged(hasContent: Boolean) {
        _inputState.value = if (hasContent) {
            UserInputState.Editing()  // Empty content for manual typing
        } else {
            UserInputState.Empty
        }
    }

    /**
     * Handles errors that occur during message processing
     * @param message The error message to display
     */
    private fun handleProcessingError(message: String) {
        _state.update { currentState ->
            ChatScreenState.Error(message)
        }
    }

    /**
     * Resets the chat to the initial idle state
     */
    private fun handleResetChat() {
        _state.update {
            ChatScreenState.Idle("")
        }
        _inputState.update { UserInputState.Empty }
    }

    /**
     * Handles sending a user message
     * @param messageText The text of the message to send
     */
    private fun handleSendMessage(messageText: String) {
        if (messageText.isBlank()) return

        // Add user message to the list
        val userMessage = ChatMessage(
            text = messageText,
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        _messages.update { currentMessages ->
            currentMessages + userMessage
        }
        // Reset input state after sending (text clearing is handled in ChatInputField)
        _inputState.update { UserInputState.Empty }

        // Transition to processing state
        _state.update {
            ChatScreenState.Processing(messageText)
        }

        // Start the conversation stream from the API
        initiateConversation(messageText)
    }
    
    /**
     * Handles the streaming conversation response from the API
     * @param messageText The original user message
     */
    private fun initiateConversation(messageText: String) {
        viewModelScope.launch {
            var assistantMessage: ChatMessage
            val contentBuilder = StringBuilder()

            try {
                // Create initial empty assistant message once the stream begins
                assistantMessage = ChatMessage(
                    text = "",
                    isFromUser = false,
                    timestamp = System.currentTimeMillis()
                )
                _messages.update { currentMessages -> currentMessages + assistantMessage }

                chatService.chat(messageText).collect { parsedMessage ->
                    onParsedMessage(parsedMessage, contentBuilder)
                }
            } catch (e: Exception) {
                Log.error(ConciergeConstants.EXTENSION_NAME, TAG, "Error processing conversation : ${e.message}")
                handleConversationError("Failed to process response: ${e.message}")
            }
        }
    }
    
    /**
     * Handles parsed event data by extracting conversation messages and updating the UI
     * @param jsonData The raw JSON data from the SSE event
     * @param contentBuilder StringBuilder tracking the full content
     */
    private fun onParsedMessage(parsedMessage: ParsedConversationMessage, contentBuilder: StringBuilder) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Parsed message: ${parsedMessage.messageContent}, state: ${parsedMessage.state}"
        )

        when (parsedMessage.state) {
            ConversationState.IN_PROGRESS -> {
                appendToAssistantMessage(parsedMessage.messageContent, contentBuilder)
            }
            ConversationState.COMPLETED -> {
                // Finalize the assistant message with the complete content
                // and change state to Idle
                contentBuilder.clear()
                _state.update { ChatScreenState.Idle("") }
            }
            ConversationState.ERROR -> {
                handleConversationError("Conversation error: ${parsedMessage.messageContent}")
            }
            else -> appendToAssistantMessage(parsedMessage.messageContent, contentBuilder)
        }
    }
    
    /**
     * Appends new content to the assistant message
     * @param newContent The new content to append
     * @param contentBuilder StringBuilder tracking the full content
     */
    private fun appendToAssistantMessage(newContent: String, contentBuilder: StringBuilder) {
        if (newContent.isNotBlank()) {
            contentBuilder.append(newContent)
            updateAssistantMessageText(contentBuilder.toString())
        }
    }

    /**
     * Updates the assistant message text in the UI
     * @param text The new text content for the assistant message
     */
    private fun updateAssistantMessageText(text: String) {
        _messages.update { currentMessages ->
            currentMessages.mapIndexed { index, message ->
                if (index == currentMessages.lastIndex && !message.isFromUser) {
                    message.copy(text = text)
                } else {
                    message
                }
            }
        }
    }
    
    /**
     * Handles errors during conversation
     * @param errorMessage The error message to display
     */
    private fun handleConversationError(errorMessage: String) {
        // Add error message to chat
        val errorChatMessage = ChatMessage(
            text = "Sorry, I encountered an error: $errorMessage",
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        
        _messages.update { currentMessages ->
            currentMessages + errorChatMessage
        }
        
        // Return to idle state
        _state.update {
            ChatScreenState.Idle("")
        }
    }

    /**
     * Starts speech recognition if permission is granted
     */
    private fun startSpeechRecognition() {
        if (_hasAudioPermission.value) {
            speechToTextManager.startListening()
        } else {
            _inputState.update {
                UserInputState.Error("Microphone permission required")
            }
        }
    }

    /**
     * Stops speech recognition
     */
    private fun stopSpeechRecognition() {
        speechToTextManager.stopListening()
    }

    /**
     * Handles the result of speech transcription
     * @param transcription The transcribed text
     */
    private fun handleTranscriptionResult(transcription: String) {
        if (transcription.isNotBlank()) {
            _inputState.update { UserInputState.Editing(transcription) }
        } else {
            _inputState.update { UserInputState.Empty }
        }
    }

    /**
     * Handles speech recognition errors
     * @param errorCode The error code from the speech recognizer
     */
    private fun handleSpeechError(errorCode: Int) {
        _inputState.update {
            UserInputState.Error("Speech recognition error: $errorCode")
        }
    }


    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun refreshPermissionStatus() {
        _hasAudioPermission.update { checkAudioPermission() }
    }

    override fun onCleared() {
        super.onCleared()
        speechToTextManager.release()
        chatService.cleanup()
    }
}
