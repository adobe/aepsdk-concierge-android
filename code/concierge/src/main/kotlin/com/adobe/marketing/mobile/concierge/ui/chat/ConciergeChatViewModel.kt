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
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.ActiveConciergeDataHandoffForwarder
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffEvent
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffForwarder
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeTrackingEvent
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.LinkHint
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.DisclaimerClickedEvent
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.toWelcomeConfig
import com.adobe.marketing.mobile.concierge.utils.WelcomeResponseParser
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.isAllowedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.isBlockedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import com.adobe.marketing.mobile.concierge.utils.tryOpenWithSystemHandler
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ConciergeChatViewModel : AndroidViewModel {
    companion object {
        private const val TAG = "ConciergeChatViewModel"
        private const val MAX_PENDING_CONVERSATION_REQUESTS = 64

        /**
         * User-facing fallback shown in the chat when a conversation cannot be completed
         * (for example, due to a network, server, or parsing error). Intentionally generic —
         * the underlying technical detail is sent to logs and telemetry, never to the user.
         */
        private const val DEFAULT_CONVERSATION_ERROR_MESSAGE =
            "Sorry, I encountered an error. Please try again."

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
     * Flips only when the input transitions between empty and non-empty.
     * Collected by the screen-level composable so it doesn't recompose on every character typed.
     */
    internal val isInputEmpty: StateFlow<Boolean> = _inputState
        .map { it is UserInputState.Empty || it is UserInputState.Error }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

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

    private var lastChatOpen: Long? = null

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
     * else WebView overlay. Dispatches a `LinkClicked` tracking event tagged with the [origin].
     *
     * @param url The URL to open
     * @param origin The surface that produced the click (see [ConciergeConstants.TrackingEvent.LinkClickOrigin])
     * @param handleLink Optional host callback; return true if handled
     */
    internal fun handleLinkClick(url: String, origin: String, handleLink: ((String) -> Boolean)?) {
        if (url.isBlank()) return
        dispatchTrackingEvent(ConciergeTrackingEvent.LinkClicked(linkUrl = url, origin = origin))
        when {
            handleLink?.invoke(url) == true -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: handled by host callback")
            }
            tryOpenAsAppLink(getApplication(), url) -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: opened as App Link")
            }
            isBlockedUrlScheme(url) -> {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: blocked scheme, ignoring")
            }
            !isAllowedUrlScheme(url) -> {
                // Non-http/https scheme (e.g. tel:, geo:, mailto:) — forward to system.
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "handleLinkClick: forwarding system scheme to device")
                tryOpenWithSystemHandler(getApplication(), url)
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
    private val chatService: ConversationService

    /**
     * Dispatch function for sending tracking events to the AEP Event Hub.
     * Defaults to MobileCore::dispatchEvent; injectable for testing.
     */
    private val dispatch: ((Event) -> Unit)?

    /**
     * Prevents duplicate responseStarted events within a single conversation turn.
     * Reset at the start of each new user message.
     */
    private var responseStartedDispatched = false

    private sealed class ConversationRequest {
        data class Chat(val message: String) : ConversationRequest()
        class DataHandoff(
            val handoff: ConciergeDataHandoffEvent,
            private val completion: (DataHandoffDeliveryResult) -> Unit,
            private val onCompleted: (DataHandoff) -> Unit,
            private val onReservationReleased: () -> Unit
        ) : ConversationRequest() {
            private val completed = AtomicBoolean(false)
            private val reservationReleased = AtomicBoolean(false)

            @Volatile
            var requestJob: Job? = null

            @Volatile
            var timeoutJob: Job? = null

            @Volatile
            private var terminalResult: DataHandoffDeliveryResult? = null

            val isCompleted: Boolean
                get() = completed.get()

            val timedOut: Boolean
                get() = terminalResult ==
                    DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT)

            fun complete(result: DataHandoffDeliveryResult) {
                if (markCompleted(result)) {
                    completion(result)
                }
            }

            fun completeAfterReleasingReservation(result: DataHandoffDeliveryResult) {
                if (markCompleted(result)) {
                    releaseReservation()
                    completion(result)
                }
            }

            fun timeout() {
                completeAfterReleasingReservation(
                    DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT)
                )
                requestJob?.cancel()
            }

            fun releaseReservation() {
                if (reservationReleased.compareAndSet(false, true)) {
                    onReservationReleased()
                }
            }

            private fun markCompleted(result: DataHandoffDeliveryResult): Boolean {
                if (!completed.compareAndSet(false, true)) {
                    return false
                }
                terminalResult = result
                timeoutJob?.cancel()
                onCompleted(this)
                return true
            }
        }
    }

    private enum class ConversationStreamResult {
        DELIVERED,
        EMPTY,
        FAILED
    }

    private val conversationRequests = Channel<ConversationRequest>(MAX_PENDING_CONVERSATION_REQUESTS)

    // reserveDataHandoffSlot() only ever admits one data handoff at a time, so this is never
    // more than a single in-flight request.
    @Volatile
    private var currentDataHandoff: ConversationRequest.DataHandoff? = null

    // Total conversation requests (chat or data handoff) currently queued or processing.
    // Chat messages increment/decrement this unconditionally as a queue-depth count; a data
    // handoff instead requires it to be exactly 0 to be admitted at all (reserveDataHandoffSlot),
    // making this field double as both a FIFO depth counter and a single-occupant exclusivity gate.
    private val pendingConversationRequests = AtomicInteger(0)

    @Volatile
    private var isDataHandoffSessionActive = false

    private val dataHandoffForwarder = object : ConciergeDataHandoffForwarder {
        override fun forward(
            result: ConciergeDataHandoffEvent,
            completion: (DataHandoffDeliveryResult) -> Unit
        ) {
            enqueueDataHandoff(result, completion)
        }
    }

    constructor(application: Application) : this(
        application,
        AndroidSpeechCapturing(application),
        DefaultImageProvider(),
        ConciergeConversationServiceClient(),
        MobileCore::dispatchEvent
    )

    internal constructor(application: Application, speechCapturing: AndroidSpeechCapturing) : this(
        application,
        speechCapturing,
        DefaultImageProvider(),
        ConciergeConversationServiceClient(),
        MobileCore::dispatchEvent
    )

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        chatClient: ConversationService
    ) : this(application, speechCapturing, DefaultImageProvider(), chatClient, null)

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        imageProvider: ImageProvider,
        chatService: ConversationService,
        dispatch: ((Event) -> Unit)? = null
    ) : super(application) {
        this.speechCapturing = speechCapturing
        this.imageProvider = imageProvider
        this.chatService = chatService
        this.dispatch = dispatch
        speechCapturing.setListener(captureListener)
        startConversationProcessor()

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
        dispatchTrackingEvent(ConciergeTrackingEvent.SessionInitialized)
    }

    private fun dispatchTrackingEvent(trackingEvent: ConciergeTrackingEvent) {
        val event = trackingEvent.toEvent()
        Log.debug(ConciergeConstants.LOG_TAG, TAG, "Dispatching tracking event: $event")
        dispatch?.invoke(event)
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

        override fun onAudioLevelChanged(level: Float) {
            val current = _inputState.value
            if (current is UserInputState.Recording) {
                _inputState.update { current.copy(audioLevel = level) }
            }
        }
    }

    /**
     * Process incoming events from the UI
     * @param event The event to process
     */
    internal fun processEvent(event: ChatEvent, handleLink: ((String) -> Boolean)? = null) {
        when (event) {
            is ChatEvent.Error -> handleProcessingError(event.message)
            is ChatEvent.Reset -> handleResetChat()
            is ChatEvent.SendMessage -> handleSendMessage(event.message)
            is MicEvent.StartRecording -> {
                dispatchTrackingEvent(ConciergeTrackingEvent.MicButtonClicked)
                startSpeechRecognition()
            }
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
            is MessageInteractionEvent.ProductActionClick -> handleProductActionClick(event.button, handleLink)
            is MessageInteractionEvent.ProductImageClick -> handleProductImageClick(event.element, handleLink)
            is MessageInteractionEvent.PromptSuggestionClick -> handlePromptSuggestionClick(event.suggestion)
            is MessageInteractionEvent.WelcomePromptSuggestionClick -> handleWelcomePromptSuggestionClick(event.suggestion)
            is DisclaimerClickedEvent -> handleDisclaimerLinkClickedEvent(event.url)
            is MessageInteractionEvent.CtaButtonClick -> handleCtaClicked(event.ctaButton)
        }
    }

    /**
     * Handle product action button clicks
     * @param button The [ProductActionButton] that was pressed
     */
    private fun handleProductActionClick(button: ProductActionButton, handleLink: ((String) -> Boolean)?) {
        val origin = ConciergeConstants.TrackingEvent.LinkClickOrigin.PRODUCT_CARD
        // Report the card's real product name; fall back to the button label only when the payload
        // carried no product name (e.g. a bare text action).
        val element = mutableMapOf<String, Any>("productName" to (button.productName ?: button.text))
        button.url?.let { element["productPageURL"] = it }
        dispatchTrackingEvent(ConciergeTrackingEvent.CardClicked(element))

        if (button.url.isNullOrEmpty()) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "No URL on action button, skipping navigation.")
            return
        }
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Button pressed: ${button.text}, opening URL: ${button.url}")
        handleLinkClick(button.url, origin, handleLink)
    }

    /**
     * Handle product image clicks
     * @param element The [MultimodalElement] image that was clicked
     */
    private fun handleProductImageClick(element: MultimodalElement, handleLink: ((String) -> Boolean)?) {
        dispatchTrackingEvent(ConciergeTrackingEvent.CardClicked(buildCardElementDict(element.content)))

        val url = element.content["productPageURL"] as? String
        if (url.isNullOrEmpty()) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "No URL on card image, skipping navigation.")
            return
        }
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Multimodal element image clicked: ${element.id}, opening URL: $url")
        handleLinkClick(url, ConciergeConstants.TrackingEvent.LinkClickOrigin.PRODUCT_CARD, handleLink)
    }

    private fun buildCardElementDict(content: Map<String, Any>): Map<String, Any> {
        val dict = mutableMapOf<String, Any>()
        content["productName"]?.let { dict["productName"] = it }
        content["productDescription"]?.let { dict["productDescription"] = it }
        content["productPageURL"]?.let { dict["productPageURL"] = it }
        content["productPrice"]?.let { dict["productPrice"] = it }
        content["productBadge"]?.let { dict["productBadge"] = it }
        return dict
    }

    /**
     * Handle welcome prompt suggestion clicks
     * @param suggestion The suggestion text that was clicked
     */
    private fun handleWelcomePromptSuggestionClick(suggestion: String) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Welcome Prompt suggestion clicked: $suggestion")
        dispatchTrackingEvent(ConciergeTrackingEvent.WelcomePromptSuggestionClicked(suggestion))
        // Auto-send the suggestion as a message
        handleSendMessage(suggestion)
    }

    /**
     * Handle disclaimer link clicks
     * @param suggestion The suggestion text that was clicked
     */
    private fun handleDisclaimerLinkClickedEvent(url: String) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Disclaimer Link clicked: $url")
        dispatchTrackingEvent(ConciergeTrackingEvent.DisclaimerLinkClicked(url))
    }

    /**
     * Handle cta click tracking
     * @param cta The suggestion text that was clicked
     */
    private fun handleCtaClicked(cta: CtaButton) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "CTA Button Clicked Link clicked {label: ${cta.label}, url: ${cta.url}}")
        dispatchTrackingEvent(ConciergeTrackingEvent.CtaButtonClicked(label = cta.label, linkUrl = cta.url))
    }

    /**
     * Handle prompt suggestion clicks
     * @param suggestion The suggestion text that was clicked
     */
    private fun handlePromptSuggestionClick(suggestion: String) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Prompt suggestion clicked: $suggestion")
        dispatchTrackingEvent(ConciergeTrackingEvent.PromptSuggestionClicked(suggestion))
        // Auto-send the suggestion as a message
        handleSendMessage(suggestion)
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

        dispatchTrackingEvent(ConciergeTrackingEvent.FeedbackSubmitted(
            conversationId = currentConversationId ?: "",
            interactionId = feedback.interactionId,
            feedbackType = when (feedback.feedbackType) {
                FeedbackType.POSITIVE -> ConciergeConstants.ChatInteraction.POSITIVE
                FeedbackType.NEGATIVE -> ConciergeConstants.ChatInteraction.NEGATIVE
            },
            selectedOptions = feedback.selectedCategories,
            notes = feedback.notes
        ))

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
        Log.error(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Processing error: $message"
        )
        _state.update {
            ChatScreenState.Error(DEFAULT_CONVERSATION_ERROR_MESSAGE)
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

        pendingConversationRequests.incrementAndGet()
        if (conversationRequests.trySend(ConversationRequest.Chat(messageText)).isFailure) {
            pendingConversationRequests.decrementAndGet()
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Unable to queue chat message because the conversation queue is full or closed."
            )
            handleProcessingError("Unable to queue chat message")
            return
        }

        dispatchTrackingEvent(ConciergeTrackingEvent.QuerySubmitted(messageText))
        if (_showWelcomeCard.value) {
            dismissWelcomeCard()
        }
        markUserAsReturning()
        _inputState.update { UserInputState.Empty }
        _state.update { ChatScreenState.Processing() }
    }

    private fun startConversationProcessor() {
        viewModelScope.launch {
            for (request in conversationRequests) {
                when (request) {
                    is ConversationRequest.Chat -> {
                        try {
                            processChatRequest(request.message)
                        } finally {
                            pendingConversationRequests.decrementAndGet()
                        }
                    }
                    is ConversationRequest.DataHandoff -> {
                        try {
                            processDataHandoffRequest(request)
                        } finally {
                            request.releaseReservation()
                        }
                    }
                }
            }
        }
    }

    /**
     * Runs [block], rethrowing [CancellationException] but reporting any other exception via
     * [handleConversationError] and returning [onError]'s value instead.
     */
    private suspend fun <T> reportingConversationErrors(
        errorPrefix: String,
        onError: () -> T,
        block: suspend () -> T
    ): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleConversationError("$errorPrefix: ${e.message}")
        onError()
    }

    private suspend fun processChatRequest(messageText: String) {
        responseStartedDispatched = false

        appendUserMessage(messageText)

        // Transition to processing state
        _state.update {
            ChatScreenState.Processing()
        }

        reportingConversationErrors("Failed to send message", onError = {}) {
            streamConversation(
                chatService.chat(messageText.trim()),
                removeEmptyPlaceholder = false,
                renderErrorsInChat = true
            )
        }
    }

    private fun appendUserMessage(messageText: String) {
        _messages.update { currentMessages ->
            currentMessages + ChatMessage(
                content = MessageContent.Text(messageText),
                isFromUser = true,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    internal fun enqueueDataHandoff(
        result: ConciergeDataHandoffEvent,
        completion: (DataHandoffDeliveryResult) -> Unit
    ) {
        if (!isDataHandoffSessionActive) {
            completion(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
            return
        }
        if (!reserveDataHandoffSlot()) {
            completion(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS))
            return
        }

        val request = ConversationRequest.DataHandoff(
            result,
            completion,
            { handoff -> if (currentDataHandoff === handoff) currentDataHandoff = null },
            { pendingConversationRequests.decrementAndGet() }
        )
        currentDataHandoff = request
        if (!isDataHandoffSessionActive) {
            request.releaseReservation()
            request.complete(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
            return
        }
        request.timeoutJob = viewModelScope.launch {
            delay(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            request.timeout()
        }
        if (conversationRequests.trySend(request).isFailure) {
            request.releaseReservation()
            request.complete(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED))
        }
    }

    private fun reserveDataHandoffSlot(): Boolean = pendingConversationRequests.compareAndSet(0, 1)

    private suspend fun processDataHandoffRequest(request: ConversationRequest.DataHandoff) {
        if (request.isCompleted) {
            return
        }
        if (!isDataHandoffSessionActive) {
            request.releaseReservation()
            request.complete(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
            return
        }

        responseStartedDispatched = false
        request.handoff.localMessage?.let(::appendUserMessage)
        _state.update {
            ChatScreenState.Processing()
        }

        val result = try {
            supervisorScope {
                val responseJob = async {
                    streamConversation(
                        chatService.sendDataHandoff(request.handoff.routingHint, request.handoff.xdmFields),
                        removeEmptyPlaceholder = true,
                        renderErrorsInChat = false
                    )
                }
                request.requestJob = responseJob
                if (request.isCompleted) {
                    responseJob.cancel()
                }
                when (responseJob.await()) {
                    ConversationStreamResult.DELIVERED -> DataHandoffDeliveryResult.Delivered
                    ConversationStreamResult.EMPTY ->
                        DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.EMPTY_RESPONSE)
                    ConversationStreamResult.FAILED ->
                        DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED)
                }
            }
        } catch (e: CancellationException) {
            if (request.isCompleted) {
                // A handoff timeout or a chat-close deactivation both cancel the stream. Neither is
                // a user-typed chat failure, so it's reported to the app via the completion callback,
                // not as an in-chat error. streamConversation already dropped its placeholder on
                // cancellation, so here we only return the UI to Idle.
                if (request.timedOut) {
                    Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Data handoff timed out")
                }
                resetProcessingStateToIdle()
                return
            }
            request.releaseReservation()
            request.complete(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
            throw e
        } catch (e: Exception) {
            // Reached only when the service call throws synchronously before streamConversation runs,
            // so there is no placeholder to remove - just reset state and report the failure.
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Failed to forward data handoff (routingHint=${request.handoff.routingHint}): ${e.message}"
            )
            resetProcessingStateToIdle()
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED)
        } finally {
            request.requestJob = null
        }
        request.completeAfterReleasingReservation(result)
    }

    /**
     * Consumes a conversation stream into the transcript.
     *
     * @param renderErrorsInChat when true (user-typed chat), a stream error is surfaced as a
     * generic error message in the transcript; when false (data handoff), failures are reported
     * only via the caller's completion callback, so the in-progress turn is discarded silently.
     */
    private suspend fun streamConversation(
        conversation: Flow<ParsedConversationMessage>,
        removeEmptyPlaceholder: Boolean,
        renderErrorsInChat: Boolean
    ): ConversationStreamResult {
        val contentBuilder = StringBuilder()
        var hasVisibleContent = false
        var hasError = false

        // Create an empty assistant message once the request reaches the front of the queue.
        val assistantMessage = ChatMessage(
            content = MessageContent.Text(""),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            citations = emptyList()
        )
        _messages.update { currentMessages -> currentMessages + assistantMessage }

        return try {
            conversation.collect { parsedMessage ->
                if (parsedMessage.state == ConversationState.ERROR) {
                    hasError = true
                    // onParsedMessage's ERROR branch renders the generic error message; skip it
                    // for a data handoff so the failure surfaces only via the completion callback.
                    if (renderErrorsInChat) {
                        onParsedMessage(parsedMessage, contentBuilder)
                    }
                } else {
                    hasVisibleContent = hasVisibleContent ||
                        parsedMessage.messageContent.isNotBlank() || parsedMessage.orderedElements.isNotEmpty()
                    onParsedMessage(parsedMessage, contentBuilder)
                }
            }
            when {
                hasError -> {
                    if (!renderErrorsInChat) {
                        discardAssistantTurn()
                    }
                    ConversationStreamResult.FAILED
                }
                hasVisibleContent -> {
                    finishConversation()
                    ConversationStreamResult.DELIVERED
                }
                else -> {
                    if (removeEmptyPlaceholder) {
                        removeLastAssistantPlaceholder()
                    }
                    finishConversation()
                    ConversationStreamResult.EMPTY
                }
            }
        } catch (e: CancellationException) {
            // This turn's placeholder is streamConversation's responsibility, so drop it here for a
            // handoff (e.g. timeout/deactivation cancellation) before propagating - the caller then
            // only has to reset state, and never removes a message it doesn't own. Chat keeps its
            // placeholder (its scope is usually shutting down).
            if (!renderErrorsInChat) {
                removeLastAssistantPlaceholder()
            }
            throw e
        } catch (e: Exception) {
            if (renderErrorsInChat) {
                handleConversationError("Failed to process response: ${e.message}")
            } else {
                Log.warning(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Data handoff stream failed: ${e.message}"
                )
                discardAssistantTurn()
            }
            ConversationStreamResult.FAILED
        }
    }

    /**
     * Silently removes the in-progress assistant placeholder and returns the UI to Idle. Only call
     * this where [streamConversation] has actually added a placeholder as the last message.
     */
    private fun discardAssistantTurn() {
        removeLastAssistantPlaceholder()
        resetProcessingStateToIdle()
    }

    private fun finishConversation() {
        _state.update { currentState ->
            when (currentState) {
                is ChatScreenState.Processing -> ChatScreenState.Idle(feedback = currentState.feedback)
                else -> currentState
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
                val hasVisibleContent = parsedMessage.messageContent.isNotBlank() ||
                    parsedMessage.orderedElements.isNotEmpty()
                if (!responseStartedDispatched && hasVisibleContent) {
                    responseStartedDispatched = true
                    dispatchTrackingEvent(ConciergeTrackingEvent.ResponseStarted(
                        conversationId = currentConversationId ?: "",
                        interactionId = parsedMessage.interactionId ?: ""
                    ))
                }
                appendToAssistantMessage(parsedMessage, contentBuilder)
            }

            ConversationState.COMPLETED -> {
                // For COMPLETED state, replace content if there is text or ordered elements.
                // If both are absent, keep existing streamed content and just transition to Idle.
                val hasVisibleContent = parsedMessage.messageContent.isNotBlank() ||
                    parsedMessage.orderedElements.isNotEmpty()
                if (hasVisibleContent) {
                    replaceAssistantMessageContent(parsedMessage)
                } else {
                    setLastAssistantMessageSseComplete(parsedMessage.feedbackEligible)
                }
                // Ensure ResponseStarted precedes ResponseCompleted even if the server jumped
                // straight to COMPLETED without an IN_PROGRESS chunk.
                if (!responseStartedDispatched && hasVisibleContent) {
                    responseStartedDispatched = true
                    dispatchTrackingEvent(ConciergeTrackingEvent.ResponseStarted(
                        conversationId = currentConversationId ?: "",
                        interactionId = parsedMessage.interactionId ?: ""
                    ))
                }
                dispatchTrackingEvent(ConciergeTrackingEvent.ResponseCompleted(
                    conversationId = currentConversationId ?: "",
                    interactionId = parsedMessage.interactionId ?: ""
                ))
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
                    emptyList(),
                    parsedMessage.sources,
                    interactionId = if (hasCtas) null else parsedMessage.interactionId,
                    sseComplete = true,
                    feedbackEligible = if (hasCtas) false else parsedMessage.feedbackEligible,
                    linkHints = parsedMessage.linkHints
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
            appendOrderedElementMessages(parsedMessage.orderedElements, parsedMessage.promptSuggestions)
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
                sseComplete = true,
                feedbackEligible = parsedMessage.feedbackEligible,
                linkHints = parsedMessage.linkHints
            )
        }
    }

    /**
     * Appends standalone messages for each ordered element.
     * All cards are batched into one Mixed message at the position of the first Card element.
     * Each CTA becomes its own CtaButton message.
     */
    private fun appendOrderedElementMessages(
        orderedElements: List<ParsedMultimodalItem>,
        promptSuggestions: List<String> = emptyList()
    ) {
        val cardElements = orderedElements
            .filterIsInstance<ParsedMultimodalItem.Card>()
            .map { it.element }
        var cardMessageAppended = false

        if (cardElements.isNotEmpty()) {
            val displayMode = if (cardElements.size == 1) "single" else "carousel"
            val elementDicts = cardElements.map { element -> buildCardElementDict(element.content) }
            dispatchTrackingEvent(ConciergeTrackingEvent.CardsRendered(displayMode, elementDicts))
        }

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
                            sseComplete = true,
                            promptSuggestions = promptSuggestions
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
        sseComplete: Boolean? = null,
        feedbackEligible: Boolean? = null,
        linkHints: List<LinkHint> = emptyList()
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
                    sseComplete = sseComplete ?: lastAssistantMessage.sseComplete,
                    feedbackEligible = feedbackEligible ?: lastAssistantMessage.feedbackEligible,
                    linkHints = linkHints
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

    private fun setLastAssistantMessageSseComplete(feedbackEligible: Boolean = false) {
        _messages.update { existing ->
            val lastIdx = existing.lastIndex
            if (lastIdx >= 0 && !existing[lastIdx].isFromUser) {
                existing.toMutableList().apply {
                    set(lastIdx, this[lastIdx].copy(sseComplete = true, feedbackEligible = feedbackEligible))
                }
            } else existing
        }
    }

    /**
     * Handles errors during conversation
     * @param errorMessage The error message to display
     */
    private fun handleConversationError(errorMessage: String) {
        // Keep the raw technical detail for diagnostics (logs + telemetry only)...
        Log.error(ConciergeConstants.EXTENSION_NAME, TAG, "Conversation error: $errorMessage")
        dispatchTrackingEvent(ConciergeTrackingEvent.ErrorOccurred(errorMessage))
        // ...but never surface the raw exception to the user. Show generic copy instead.
        replaceAssistantMessageContent(
            ParsedConversationMessage(
                messageContent = DEFAULT_CONVERSATION_ERROR_MESSAGE,
                state = ConversationState.COMPLETED,
            )
        )
        resetProcessingStateToIdle()
    }

    private fun resetProcessingStateToIdle() {
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
                // Normal streaming while recording (preserve the live audio level)
                _inputState.update { current.copy(transcription = partialText) }
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
     * Opens the Concierge chat interface (dialog mode).
     * ChatOpened tracking is handled by the [DisposableEffect] in the [ConciergeChat] composable,
     * which fires when the chat composable enters composition.
     */
    fun openConcierge() {
        _isConciergeActive.value = true
    }

    /**
     * Closes the Concierge chat interface (dialog mode).
     * ChatClosed tracking is handled by the [DisposableEffect] onDispose in the [ConciergeChat]
     * composable, which fires when the chat composable leaves composition.
     */
    fun closeConcierge() {
        _isConciergeActive.value = false
        deactivateDataHandoffSession()
    }

    internal fun activateDataHandoffSession() {
        isDataHandoffSessionActive = true
        ActiveConciergeDataHandoffForwarder.register(dataHandoffForwarder)
    }

    internal fun deactivateDataHandoffSession() {
        isDataHandoffSessionActive = false
        ActiveConciergeDataHandoffForwarder.unregister(dataHandoffForwarder)
        currentDataHandoff?.let { request ->
            request.completeAfterReleasingReservation(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION)
            )
            request.requestJob?.cancel()
        }
    }

    /**
     * Dispatches a ChatOpened tracking event.
     * Called from the [DisposableEffect] in the [ConciergeChat] composable when it enters
     * composition. This covers all integration modes: Compose direct, dialog, and XML.
     */
    internal fun trackChatOpened() {
        val now = System.currentTimeMillis()
        lastChatOpen = now
        dispatchTrackingEvent(ConciergeTrackingEvent.ChatOpened(now))
    }

    /**
     * Dispatches a ChatClosed tracking event.
     * Called from the [DisposableEffect] onDispose in the [ConciergeChat] composable when it
     * leaves composition. This covers the close button, back-press dismissal, and XML view
     * detachment — exactly once per open, with no double-tracking.
     */
    internal fun trackChatClosed() {
        val currentTime = System.currentTimeMillis()
        // If trackChatClosed somehow runs before trackChatOpened, report a 0 duration rather
        // than a ~50-year value derived from an uninitialized epoch.
        val duration = lastChatOpen?.let { currentTime - it } ?: 0L
        dispatchTrackingEvent(ConciergeTrackingEvent.ChatClosed(currentTime, duration))
        lastChatOpen = null
    }

    override fun onCleared() {
        deactivateDataHandoffSession()
        conversationRequests.close()
        super.onCleared()
        imageProvider.clear()
        speechCapturing.setListener(null)
        speechCapturing.release()
        chatService.cleanup()
    }
}
