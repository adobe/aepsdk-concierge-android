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
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Citation
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechToTextManager
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConciergeChatViewModel : AndroidViewModel {
    companion object {
        private const val TAG = "ConciergeChatViewModel"
    }

    /**
     * Tracks the overall state of the chat flow
     */
    private val _state = MutableStateFlow<ChatScreenState>(
        ChatScreenState.Idle
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

    private val chatService: ConciergeConversationServiceClient =
        ConciergeConversationServiceClient()

    // Image provider for handling image loading and caching
    val imageProvider: ImageProvider = DefaultImageProvider(maxEntries = 64)

    // Speech to text manager
    private val speechToTextManager = SpeechToTextManager(
        context = getApplication<Application>(),
        onSpeechStarted = {
            _inputState.update { UserInputState.Recording("") }
        },
        onSpeechEnded = {
            //_inputState.update { UserInputState.Transcribing() }
        },
        onPartialTranscription = { partialText ->
            handlePartialTranscription(partialText)
        },
        onTranscriptionResult = { transcription ->
            handleTranscriptionResult(transcription)
        },
        onSpeechError = { errorCode ->
            handleSpeechError(errorCode)
        }
    )

    /**
     * Tracks whether the app has audio recording permission
     */
    private val _hasAudioPermission = MutableStateFlow(checkAudioPermission())
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    /**
     * Speech capturing implementation that will be used for this session
     */
    private val speechCapturing: SpeechCapturing

    /**
     * Chat service client for handling conversation API calls
     */
    private val chatService: ConciergeConversationServiceClient

    constructor(application: Application) : this(application, AndroidSpeechCapturing(application))

    internal constructor(application: Application, speechCapturing: AndroidSpeechCapturing): this(application, speechCapturing, ConciergeConversationServiceClient())

    internal constructor(application: Application, speechCapturing: SpeechCapturing, chatService : ConciergeConversationServiceClient = ConciergeConversationServiceClient()) : super(application) {
        this.speechCapturing = speechCapturing
        this.chatService = chatService
        speechCapturing.setListener(captureListener)
    }

    private val captureListener = object : SpeechCaptureListener {
        override fun onSpeechStarted() {
            _inputState.update { UserInputState.Recording("") }
        }

        override fun onSpeechEnded() {
            // no-op for now
        }

        override fun onPartialTranscription(text: String) {
            handlePartialTranscription(text)
        }

        override fun onTranscriptionResult(text: String) {
            handleTranscriptionResult(text)
        }

        override fun onError(error: SpeechCaptureError) {
            handleSpeechError(error)
        }
    }

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
                // Immediately transition UI state based on current partial text
                val currentState = _inputState.value
                if (currentState is UserInputState.Recording) {
                    if (currentState.transcription.isNotBlank()) {
                        // If we have partial text, transition to Editing state
                        _inputState.update { UserInputState.Editing(currentState.transcription) }
                    } else {
                        // If no partial text, go back to Empty
                        _inputState.update { UserInputState.Empty }
                    }
                }
            }

            is FeedbackEvent.ThumbsUp -> handleFeedback(event.interactionId, ConciergeConstants.ChatInteraction.POSITIVE)
            is FeedbackEvent.ThumbsDown -> handleFeedback(event.interactionId, ConciergeConstants.ChatInteraction.NEGATIVE)

            is MessageInteractionEvent.ProductActionClick -> handleProductActionClick(event.button)
            is MessageInteractionEvent.ProductImageClick -> handleProductImageClick(event.element)
        }
    }

    /**
     * Handle product action button clicks
     * @param button The [ProductActionButton] that was pressed
     */
    private fun handleProductActionClick(button: ProductActionButton) {
        if (button.url.isNullOrEmpty()) {
            Log.debug(TAG, "handleProductActionClick", "Invalid url found, cannot open.")
            return
        }

        Log.debug(TAG, "handleProductActionClick", "Button pressed: ${button.text}, opening URL: ${button.url}")
        ServiceProvider.getInstance().uriService.openUri(button.url.toString())
    }

    /**
     * Handle product image clicks
     * @param element The [MultimodalElement] image that was clicked
     */
    private fun handleProductImageClick(element: MultimodalElement) {
        var url = element.content["productPageURL"] as? String
        if (url.isNullOrEmpty()) {
            Log.debug(TAG, "handleProductImageClick", "Invalid url found, cannot open.")
            return
        }

        Log.debug(TAG, "handleProductImageClick", "Multimodal element image clicked: ${element.id}, opening URL: ${element.content["productPageURL"]}")
        ServiceProvider.getInstance().uriService.openUri(url)
    }

    /**
     * Generates random citations for testing purposes
     */
    private fun generateRandomCitations(): List<Citation> {
        val sampleCitations = listOf(
            Citation(
                title = "Adobe Experience Platform Documentation",
                url = "https://experienceleague.adobe.com/docs/experience-platform.html"
            ),
            Citation(
                title = "Mobile SDK Implementation Guide",
                url = "https://developer.adobe.com/client-sdks/"
            ),
            Citation(
                title = "Adobe Firefly Service documentation",
                url = "https://developer.adobe.com/firefly-services/docs/guides/"
            )
        )
        
        // Randomly select 0-3 citations for variety
        val randomCount = (0..3).random()
        return sampleCitations.shuffled().take(randomCount)
    }

    /**
     * Handles user feedback for responses
     * @param interactionId The interaction ID to associate with the feedback
     * @param feedbackType The type of feedback ("positive" or "negative")
     */
    private fun handleFeedback(interactionId: String, feedbackType: String) {
        // TODO: Implement Edge send event with interaction ID in XDM
        // Edge.sendEvent(...)
        // For now, just log the feedback
        Log.debug(TAG, "handleFeedback", "Received feedback: $feedbackType for interactionId: $interactionId")
    }

    /**
     * Called when the text input state changes (e.g. user types or deletes text)
     * @param currentText The current text content being edited
     */
    internal fun onTextStateChanged(currentText: String) {
        _inputState.value = if (currentText.isNotEmpty()) {
            UserInputState.Editing(currentText)
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
            ChatScreenState.Idle
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
            content = MessageContent.Text(messageText),
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
            ChatScreenState.Processing
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
                    content = MessageContent.Text(""),
                    isFromUser = false,
                    timestamp = System.currentTimeMillis(),
                    citations = generateRandomCitations(),
                    interactionId = "sample-interaction-${System.currentTimeMillis()}"
                )
                _messages.update { currentMessages -> currentMessages + assistantMessage }

                chatService.chat(messageText).collect { parsedMessage ->
                    onParsedMessage(parsedMessage, contentBuilder)
                }
            } catch (e: Exception) {
                Log.error(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Error processing conversation : ${e.message}"
                )
                handleConversationError("Failed to process response: ${e.message}")
            }
        }
    }

    /**
     * Handles parsed event data by extracting conversation messages and updating the UI
     *
     * @param parsedMessage The parsed conversation message
     * @param contentBuilder StringBuilder tracking the full content
     */
    private fun onParsedMessage(
        parsedMessage: ParsedConversationMessage,
        contentBuilder: StringBuilder
    ) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Parsed message: ${parsedMessage.messageContent}, state: ${parsedMessage.state}"
        )

        when (parsedMessage.state) {
            ConversationState.IN_PROGRESS -> {
                appendToAssistantMessage(parsedMessage, contentBuilder)
            }

            ConversationState.COMPLETED -> {
                // For COMPLETED state, replace the content with the final message
                replaceAssistantMessageContent(parsedMessage)
                _state.update { ChatScreenState.Idle }
            }

            ConversationState.ERROR -> {
                handleConversationError("Conversation error: ${parsedMessage.messageContent}")
            }

            else -> appendToAssistantMessage(parsedMessage, contentBuilder)
        }
    }

    /**
     * Appends new content to the assistant message
     * @param parsedMessage The parsed message containing content
     * @param contentBuilder StringBuilder tracking the full content
     */
    private fun appendToAssistantMessage(parsedMessage: ParsedConversationMessage, contentBuilder: StringBuilder) {
        if (parsedMessage.messageContent.isNotBlank()) {
            contentBuilder.append(parsedMessage.messageContent)
        }
        
        // Create text-only message content for streaming updates
        val messageContent = MessageContent.Text(contentBuilder.toString())

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            "ConciergeChatViewModel",
            "Appending text content with length (${contentBuilder.length} chars)"
        )
        
        updateAssistantMessageContent(messageContent)
    }

    /**
     * Replaces the assistant message content with the final complete message
     *
     * @param parsedMessage The parsed message containing the final complete content
     */
    private fun replaceAssistantMessageContent(parsedMessage: ParsedConversationMessage) {
        // Create message content - conditionally include multimodal content
        val messageContent = if (parsedMessage.multimodalElements.isEmpty()) {
            MessageContent.Text(parsedMessage.messageContent)
        } else {
            MessageContent.Mixed(
                text = parsedMessage.messageContent,
                multimodalElements = parsedMessage.multimodalElements
            )
        }

        val logMessage = if (parsedMessage.multimodalElements.isEmpty()) {
            "Replacing with final Text message with length (${parsedMessage.messageContent.length} chars)"
        } else {
            "Replacing with final Mixed message with text (${parsedMessage.messageContent.length} chars) and ${parsedMessage.multimodalElements.size} multimodal elements."
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            "ConciergeChatViewModel",
            logMessage
        )
        
        updateAssistantMessageContent(messageContent)
    }

    /**
     * Updates the assistant message content in the UI
     * @param content The new content for the assistant message
     */
    private fun updateAssistantMessageContent(content: MessageContent) {
        _messages.update { currentMessages ->
            currentMessages.mapIndexed { index, message ->
                if (index == currentMessages.lastIndex && !message.isFromUser) {
                    message.copy(content = content)
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
            content = MessageContent.Text("Sorry, I encountered an error: $errorMessage"),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        _messages.update { currentMessages ->
            currentMessages + errorChatMessage
        }

        // Return to idle state
        _state.update {
            ChatScreenState.Idle
        }
    }

    /**
     * Starts speech recognition if permission is granted
     */
    private fun startSpeechRecognition() {
        if (_hasAudioPermission.value) {
            speechCapturing.startCapture()
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
        speechCapturing.endCapture()
    }

    /**
     * Handles partial transcription results during recording
     * @param partialText The partial transcribed text
     */
    private fun handlePartialTranscription(partialText: String) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "handlePartialTranscription: partialText='$partialText'"
        )
        _inputState.update { UserInputState.Recording(partialText) }
    }

    /**
     * Handles the result of speech transcription
     * @param transcription The transcribed text
     */
    private fun handleTranscriptionResult(transcription: String) {
        val currentState = _inputState.value
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "handleTranscriptionResult: transcription='$transcription', currentState=$currentState"
        )

        if (transcription.isNotBlank()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Transitioning to Editing state with transcription: '$transcription'"
            )
            _inputState.update { UserInputState.Editing(transcription) }
        } else {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Transitioning to Empty state (blank transcription)"
            )
            _inputState.update { UserInputState.Empty }
        }
    }

    /**
     * Handles speech recognition errors
     * @param errorCode The error code from the speech recognizer
     */
    private fun handleSpeechError(error: SpeechCaptureError) {
        val message = when (error) {
            is SpeechCaptureError.NoMatch -> "No speech recognized"
            is SpeechCaptureError.Client -> "Speech client error"
            is SpeechCaptureError.Permission -> "Microphone permission required"
            is SpeechCaptureError.Network -> "Network error during speech recognition"
            is SpeechCaptureError.Unknown -> "Speech recognition error: ${error.code}"
        }
        _inputState.update { UserInputState.Error(message) }
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
        imageProvider.clear()
        speechToTextManager.release()
        chatService.cleanup()
    }
}
