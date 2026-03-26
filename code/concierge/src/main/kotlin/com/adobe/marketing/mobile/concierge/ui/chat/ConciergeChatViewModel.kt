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
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.toWelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.WelcomeResponseParser
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
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
        
        /**
         * Initializes the welcome config using the parser example
         * In the finalized implementation, the config contained in the mock response would
         * be fetched from a concierge configuration service.
         */
        private fun initializeWelcomeConfig(): WelcomeConfig {
            // Setup a mock welcome response
            val mockResponse = """
                {
                "welcome.heading": "Explore what you can do with Adobe apps.",
                "welcome.subheading": "Choose an option or tell us what interests you and we'll point you in the right direction.",
                "welcome.examples": [
                    {
                        "text": "I'd like to explore templates to see what I can create.",
                        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_142fd6e4e46332d8f41f5aef982448361c0c8c65e.png",
                        "backgroundColor": "#FFFFFF"
                    },
                    {
                        "text": "I want to touch up and enhance my photos.",
                        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1e188097a1bc580b26c8be07d894205c5c6ca5560.png",
                        "backgroundColor": "#FFFFFF"
                    },
                    {
                        "text": "I'd like to edit PDFs and make them interactive.",
                        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_1f6fed23045bbbd57fc17dadc3aa06bcc362f84cb.png",
                        "backgroundColor": "#FFFFFF"
                    },
                    {
                        "text": "I want to turn my clips into polished videos.",
                        "image": "https://main--milo--adobecom.aem.page/drafts/methomas/assets/media_16c2ca834ea8f2977296082ae6f55f305a96674ac.png",
                        "backgroundColor": "#FFFFFF"
                    }
                ]
            }
            """.trimIndent()

            val welcomeData = WelcomeResponseParser.parseWelcomeData(mockResponse)

            // Use default values if none are configured
            return WelcomeConfig(
                showWelcomeCard = true,
                welcomeHeader = welcomeData?.heading ?: ConciergeConstants.WelcomeCard.DEFAULT_HEADING,
                subHeader = welcomeData?.subheading ?: ConciergeConstants.WelcomeCard.DEFAULT_SUBHEADING,
                suggestedPrompts = welcomeData?.prompts ?: emptyList()
            )
        }
    }

    /**
     * Tracks the overall state of the chat flow
     */
    private val _state = MutableStateFlow<ChatScreenState>(
        ChatScreenState.Idle()
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

    /**
     * Tracks the current conversation ID from the backend response
     */
    private var currentConversationId: String? = null

    /**
     * Tracks whether the app has audio recording permission
     */
    private val _hasAudioPermission = MutableStateFlow(checkAudioPermission())
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    /**
     * Tracks whether the welcome card should be shown
     */
    private val _showWelcomeCard = MutableStateFlow(false)
    val showWelcomeCard: StateFlow<Boolean> = _showWelcomeCard.asStateFlow()

    /**
     * Configuration for the welcome card
     */
    private val _welcomeConfig = MutableStateFlow(initializeWelcomeConfig())
    internal val welcomeConfig: StateFlow<WelcomeConfig> = _welcomeConfig.asStateFlow()
    
    /**
     * Updates the welcome configuration from a theme config
     * @param themeConfig The theme configuration containing welcome data
     */
    fun updateWelcomeConfigFromTheme(themeConfig: ConciergeThemeConfig?) {
        if (themeConfig != null) {
            _welcomeConfig.value = themeConfig.toWelcomeConfig(showWelcomeCard = true)
        }
    }

    /**
     * Data store collection for persisting concierge
     */
    private val conciergeNamedCollection =
        ServiceProvider.getInstance().dataStoreService.getNamedCollection(ConciergeConstants.DATA_STORE_NAME)

    /**
     * Tracks whether the Concierge chat interface is active/open
     */
    private val _isConciergeActive = MutableStateFlow(false)
    val isConciergeActive: StateFlow<Boolean> = _isConciergeActive.asStateFlow()

    /**
     * URL to show in the in-app fullscreen WebView overlay, or null when overlay is dismissed.
     */
    private val _webviewOverlay = MutableStateFlow<String?>(null)
    internal val webviewOverlay: StateFlow<String?> = _webviewOverlay.asStateFlow()

    /**
     * Opens the given URL in the in-app fullscreen WebView overlay.
     */
    internal fun openWebviewOverlay(url: String) {
        _webviewOverlay.value = url
    }

    /**
     * Dismisses the in-app WebView overlay.
     */
    internal fun dismissWebviewOverlay() {
        _webviewOverlay.value = null
    }

    /**
     * Handles a link click: host callback first, then App Link if host app is verified handler,
     * else WebView overlay.
     *
     * @param url The URL to open
     * @param handleLink Optional host callback; return true if handled
     */
    internal fun handleLinkClick(url: String, handleLink: ((String) -> Boolean)?) {
        if (url.isBlank()) return
        when {
            handleLink?.invoke(url) == true -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: handled by host callback")
            }
            tryOpenAsAppLink(getApplication(), url) -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: opened as App Link")
            }
            else -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: opening in WebView overlay")
                openWebviewOverlay(url)
            }
        }
    }

    /**
     * Speech capturing implementation that will be used for this session
     */
    private val speechCapturing: SpeechCapturing

    /**
     * Image provider for handling image loading and caching
     */
    internal val imageProvider: ImageProvider

    /**
     * Chat service client for handling conversation API calls
     */
    private val chatService: ConciergeConversationServiceClient

    constructor(application: Application) : this(application, AndroidSpeechCapturing(application))

    internal constructor(application: Application, speechCapturing: AndroidSpeechCapturing) : this(
        application,
        speechCapturing,
        DefaultImageProvider(),
        ConciergeConversationServiceClient()
    )

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        chatClient: ConciergeConversationServiceClient
    ) : this(application, speechCapturing, DefaultImageProvider(), chatClient)

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        imageProvider: ImageProvider,
        chatService: ConciergeConversationServiceClient
    ) : super(application) {
        this.speechCapturing = speechCapturing
        this.imageProvider = imageProvider
        this.chatService = chatService
        speechCapturing.setListener(captureListener)

        // Initialize welcome card state based on config and user history
        checkAndShowWelcomeCard()
    }

    /**
     * Checks if the welcome card should be shown based on configuration
     */
    private fun checkAndShowWelcomeCard() {
        // Show welcome card every time chat is opened if config allows
        if (welcomeConfig.value.showWelcomeCard) {
            _showWelcomeCard.value = true
        }
    }

    /**
     * Returns whether the user is a returning user (has seen the welcome card before)
     */
    internal fun isReturningUser(): Boolean {
        return conciergeNamedCollection.getBoolean(ConciergeConstants.DataStoreKeys.KEY_HAS_SEEN_WELCOME, false)
    }

    /**
     * Marks the user as a returning user (has seen and interacted with the welcome card)
     */
    private fun markUserAsReturning() {
        conciergeNamedCollection.setBoolean(ConciergeConstants.DataStoreKeys.KEY_HAS_SEEN_WELCOME, true)
    }

    /**
     * Dismisses the welcome card
     */
    fun dismissWelcomeCard() {
        _showWelcomeCard.value = false
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

            is MicEvent.StartRecording -> { startSpeechRecognition() }

            is MicEvent.StopRecording -> { handleStopRecording() }

            is FeedbackEvent.ThumbsUp -> handleFeedback(
                event.interactionId,
                ConciergeConstants.ChatInteraction.POSITIVE
            )

            is FeedbackEvent.ThumbsDown -> handleFeedback(
                event.interactionId,
                ConciergeConstants.ChatInteraction.NEGATIVE
            )

            is FeedbackEvent.SubmitFeedback -> handleFeedbackSubmission(event.feedback)
            is FeedbackEvent.DismissFeedbackDialog -> handleDismissFeedbackDialog()

            is MessageInteractionEvent.ProductActionClick -> handleProductActionClick(event.button)
            is MessageInteractionEvent.ProductImageClick -> handleProductImageClick(event.element)
            is MessageInteractionEvent.PromptSuggestionClick -> handlePromptSuggestionClick(event.suggestion)
        }
    }

    /**
     * Handle product action button clicks
     * @param button The [ProductActionButton] that was pressed
     */
    private fun handleProductActionClick(button: ProductActionButton) {
        if (button.url.isNullOrEmpty()) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Invalid url found, cannot open.")
            return
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Button pressed: ${button.text}, opening URL: ${button.url}"
        )
        openWebviewOverlay(button.url.toString())
    }

    /**
     * Handle product image clicks
     * @param element The [MultimodalElement] image that was clicked
     */
    private fun handleProductImageClick(element: MultimodalElement) {
        val url = element.content["productPageURL"] as? String
        if (url.isNullOrEmpty()) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Invalid url found, cannot open.")
            return
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Multimodal element image clicked: ${element.id}, opening URL: ${element.content["productPageURL"]}"
        )
        openWebviewOverlay(url)
    }

    /**
     * Handle prompt suggestion clicks
     * @param suggestion The suggestion text that was clicked
     */
    private fun handlePromptSuggestionClick(suggestion: String) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Prompt suggestion clicked: $suggestion")
        // Set the suggestion text in the input field
        _inputState.update { UserInputState.Editing(suggestion) }
    }

    /**
     * Helper to update feedback dialog state
     * @param feedback The feedback data to set, or null to clear
     */
    private fun updateFeedback(feedback: Feedback?) {
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Idle -> currentState.copy(feedback = feedback)
                is ChatScreenState.Processing -> currentState.copy(feedback = feedback)
                is ChatScreenState.Error -> currentState.copy(feedback = feedback)
            }
        }
    }

    /**
     * Handles user feedback for responses
     * @param interactionId The interaction ID to associate with the feedback
     * @param feedbackType The type of feedback ("positive" or "negative")
     */
    private fun handleFeedback(interactionId: String, feedbackType: String) {
        // Show feedback dialog based on the type
        val type = when (feedbackType) {
            ConciergeConstants.ChatInteraction.POSITIVE -> FeedbackType.POSITIVE
            ConciergeConstants.ChatInteraction.NEGATIVE -> FeedbackType.NEGATIVE
            else -> return
        }

        updateFeedback(Feedback(interactionId, type))
    }

    /**
     * Handles feedback submission from the dialog
     * @param feedback The feedback data
     */
    private fun handleFeedbackSubmission(feedback: Feedback) {
        // Update feedback state
        val feedbackState = when (feedback.feedbackType) {
            FeedbackType.POSITIVE -> FeedbackState.Positive
            FeedbackType.NEGATIVE -> FeedbackState.Negative
        }

        // Find and update the message with the feedback state
        _messages.update { currentMessages ->
            currentMessages.map { message ->
                if (message.interactionId == feedback.interactionId) {
                    message.copy(feedbackState = feedbackState)
                } else {
                    message
                }
            }
        }

        // Hide dialog
        updateFeedback(null)

        // Send feedback to the conversation service
        viewModelScope.launch {
            val feedbackWithConversationId = feedback.copy(conversationId = currentConversationId)
            
            val success = chatService.sendFeedback(feedbackWithConversationId)
            if (success) {
                Log.debug(
                    TAG,
                    "handleFeedbackSubmission",
                    "Feedback sent successfully for turnId: ${feedback.interactionId}, conversationId: $currentConversationId"
                )
            } else {
                Log.warning(
                    TAG,
                    "handleFeedbackSubmission",
                    "Failed to send feedback for turnId: ${feedback.interactionId}, conversationId: $currentConversationId"
                )
            }
        }
    }

    /**
     * Handles dismissing the feedback dialog
     */
    private fun handleDismissFeedbackDialog() {
        updateFeedback(null)
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
            ChatScreenState.Idle()
        }
        _inputState.update { UserInputState.Empty }
    }

    /**
     * Handles sending a user message
     * @param messageText The text of the message to send
     */
    private fun handleSendMessage(messageText: String) {
        if (messageText.isBlank()) return

        // Dismiss welcome card when user sends their first message
        if (_showWelcomeCard.value) {
            dismissWelcomeCard()
        }
        
        // Mark user as returning (has seen and interacted with welcome)
        markUserAsReturning()

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
            ChatScreenState.Processing()
        }

        // Start the conversation stream from the API
        initiateConversation(messageText.trim())
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
                    citations = emptyList()
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

        // Capture conversationId if present in the response
        parsedMessage.conversationId?.let { conversationId ->
            if (currentConversationId == null) {
                currentConversationId = conversationId
                Log.debug(TAG, "onParsedMessage", "Captured conversationId: $conversationId")
            }
        }

        when (parsedMessage.state) {
            ConversationState.IN_PROGRESS -> {
                appendToAssistantMessage(parsedMessage, contentBuilder)
            }

            ConversationState.COMPLETED -> {
                // For COMPLETED state, replace content if there is text or ordered elements.
                // If both are absent, keep existing streamed content and just transition to Idle.
                if (parsedMessage.messageContent.isNotBlank() || parsedMessage.orderedElements.isNotEmpty()) {
                    replaceAssistantMessageContent(parsedMessage)
                } else {
                    setLastAssistantMessageSseComplete()
                }
                _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Processing -> ChatScreenState.Idle(
                    feedback = currentState.feedback
                )
                        else -> currentState
                    }
                }
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
    private fun appendToAssistantMessage(
        parsedMessage: ParsedConversationMessage,
        contentBuilder: StringBuilder
    ) {
        if (parsedMessage.messageContent.isNotBlank()) {
            contentBuilder.append(parsedMessage.messageContent)
        }

        // Create text-only message content for streaming updates
        val messageContent = MessageContent.Text(contentBuilder.toString())

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Appending text content with length (${contentBuilder.length} chars)"
        )

        // Use the interactionId as the turnId for feedback
        updateAssistantMessageContent(messageContent, interactionId = parsedMessage.interactionId)
    }

    /**
     * Replaces the assistant message content with the final complete message
     *
     * @param parsedMessage The parsed message containing the final complete content
     */
    private fun replaceAssistantMessageContent(parsedMessage: ParsedConversationMessage) {
        if (parsedMessage.orderedElements.isNotEmpty()) {
            if (parsedMessage.messageContent.isNotEmpty()) {
                // Text + ordered elements: keep the text message, then append elements.
                // Suppress interactionId (and thus feedback controls) when CTAs are present —
                // service-intent responses are deterministic and don't warrant thumbs up/down.
                val hasCtas = parsedMessage.orderedElements.any { it is ParsedMultimodalItem.Cta }
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Replacing with final Text message (${parsedMessage.messageContent.length} chars), then appending ${parsedMessage.orderedElements.size} ordered elements."
                )
                updateAssistantMessageContent(
                    MessageContent.Text(parsedMessage.messageContent),
                    parsedMessage.promptSuggestions,
                    parsedMessage.sources,
                    interactionId = if (hasCtas) null else parsedMessage.interactionId,
                    sseComplete = true
                )
            } else {
                // No text, ordered elements only: remove the streaming placeholder so feedback
                // controls don't appear on an empty bubble.
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "No text content, removing placeholder and appending ${parsedMessage.orderedElements.size} ordered elements."
                )
                removeLastAssistantPlaceholder()
            }
            appendOrderedElementMessages(parsedMessage.orderedElements)
        } else {
            // Legacy path: text-only or mixed message
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

            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, logMessage)

            updateAssistantMessageContent(
                messageContent,
                parsedMessage.promptSuggestions,
                parsedMessage.sources,
                parsedMessage.interactionId,
                sseComplete = true
            )
        }
    }

    /**
     * Appends standalone messages for each ordered element.
     * All cards are batched into one Mixed message at the position of the first Card element.
     * Each CTA becomes its own CtaButton message.
     */
    private fun appendOrderedElementMessages(orderedElements: List<ParsedMultimodalItem>) {
        val cardElements = orderedElements
            .filterIsInstance<ParsedMultimodalItem.Card>()
            .map { it.element }
        var cardMessageAppended = false

        for (element in orderedElements) {
            when (element) {
                is ParsedMultimodalItem.Cta -> {
                    val ctaMessage = ChatMessage(
                        content = MessageContent.CtaButton(element.button),
                        isFromUser = false,
                        timestamp = System.currentTimeMillis(),
                        sseComplete = true
                    )
                    _messages.update { it + ctaMessage }
                }
                is ParsedMultimodalItem.Card -> {
                    if (!cardMessageAppended) {
                        cardMessageAppended = true
                        val cardMessage = ChatMessage(
                            content = MessageContent.Mixed(text = "", multimodalElements = cardElements),
                            isFromUser = false,
                            timestamp = System.currentTimeMillis(),
                            sseComplete = true
                        )
                        _messages.update { it + cardMessage }
                    }
                }
            }
        }
    }

    /**
     * Updates the assistant message content in the UI
     * @param content The new content for the assistant message
     * @param promptSuggestions Optional prompt suggestions to include with the message
     * @param sources Optional sources to include with the message
     * @param interactionId Optional interaction ID from the backend to use as a turnId for feedback
     * @param sseComplete True when SSE stream has completed for this message
     */
    private fun updateAssistantMessageContent(
        content: MessageContent,
        promptSuggestions: List<String> = emptyList(),
        sources: List<Citation> = emptyList(),
        interactionId: String? = null,
        sseComplete: Boolean? = null
    ) {
        // Pre-compute unique citations once to avoid redundant processing
        val uniqueSources = if (sources.isNotEmpty()) {
            CitationUtils.createUniqueSources(sources)
        } else {
            null
        }

        _messages.update { existingMessages ->
            val lastIndex = existingMessages.lastIndex
            if (lastIndex >= 0 && !existingMessages[lastIndex].isFromUser) {
                val updatedMessages = existingMessages.toMutableList()
                val lastAssistantMessage = existingMessages[lastIndex]
                updatedMessages[lastIndex] = lastAssistantMessage.copy(
                    content = content,
                    promptSuggestions = promptSuggestions,
                    citations = sources,
                    uniqueCitations = uniqueSources,
                    interactionId = interactionId,
                    sseComplete = sseComplete ?: lastAssistantMessage.sseComplete
                )
                updatedMessages
            } else {
                existingMessages
            }
        }
    }

    private fun removeLastAssistantPlaceholder() {
        _messages.update { existingMessages ->
            val lastIndex = existingMessages.lastIndex
            if (lastIndex >= 0 && !existingMessages[lastIndex].isFromUser) {
                existingMessages.dropLast(1)
            } else {
                existingMessages
            }
        }
    }

    private fun setLastAssistantMessageSseComplete() {
        _messages.update { existing ->
            val lastIdx = existing.lastIndex
            if (lastIdx >= 0 && !existing[lastIdx].isFromUser) {
                existing.toMutableList().apply { set(lastIdx, this[lastIdx].copy(sseComplete = true)) }
            } else existing
        }
    }

    /**
     * Handles errors during conversation
     * @param errorMessage The error message to display
     */
    private fun handleConversationError(errorMessage: String) {
        replaceAssistantMessageContent(
            ParsedConversationMessage(
                messageContent = "Sorry, I encountered an error: $errorMessage",
                state = ConversationState.COMPLETED,
            )
        )

        // Return to idle state
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Processing -> ChatScreenState.Idle(
                    feedback = currentState.feedback
                )
                else -> ChatScreenState.Idle()
            }
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
     * Handles stopping speech recognition
     */
    private fun handleStopRecording() {
        speechCapturing.endCapture()

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Stopped speech recognition, current input state: ${_inputState.value}"
        )
        // Immediately transition UI state based on current partial text
        val currentState = _inputState.value
        if (currentState is UserInputState.Recording) {
            if (currentState.transcription.isNotBlank()) {
                // If we have partial text, transition to Editing state and keep accepting late partials
                _inputState.update { UserInputState.Editing(currentState.transcription, isPendingTranscription = true) }
            } else {
                // If no partial text, go back to Empty
                _inputState.update { UserInputState.Empty }
            }
        }
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Input state after stopping recording: ${_inputState.value}"
        )
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
        val current = _inputState.value
        when (current) {
            is UserInputState.Recording -> {
                // Normal streaming while recording
                _inputState.update { UserInputState.Recording(partialText) }
            }
            is UserInputState.Editing -> {
                if (current.isPendingTranscription) {
                    // After stop: continue showing latest partials while staying in Editing
                    _inputState.update { UserInputState.Editing(partialText, isPendingTranscription = true) }
                } else {
                    // Stay in current state
                    _inputState.update { current }
                }
            }
            else -> {
                Log.trace(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Ignoring partial transcription in state: $current"
                )
            }
        }
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
            _inputState.update { UserInputState.Editing(transcription, isPendingTranscription = false) }
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

    /**
     * Opens the Concierge chat interface
     */
    fun openConcierge() {
        _isConciergeActive.value = true
    }

    /**
     * Closes the Concierge chat interface
     */
    fun closeConcierge() {
        _isConciergeActive.value = false
    }

    override fun onCleared() {
        super.onCleared()
        imageProvider.clear()
        speechCapturing.setListener(null)
        speechCapturing.release()
        chatService.cleanup()
    }
}
