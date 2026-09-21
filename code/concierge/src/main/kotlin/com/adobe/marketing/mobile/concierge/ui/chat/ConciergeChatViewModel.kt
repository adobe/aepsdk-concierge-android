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
import com.adobe.marketing.mobile.concierge.ConciergeConversationSession
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeTrackingEvent
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
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
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.toWelcomeConfig
import com.adobe.marketing.mobile.concierge.utils.WelcomeResponseParser
import com.adobe.marketing.mobile.concierge.utils.buildCardElementDict
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.isAllowedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.isBlockedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import com.adobe.marketing.mobile.concierge.utils.tryOpenWithSystemHandler
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

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
     * Tracks the overall state of the chat flow. Owned by [ConciergeConversationSession] so that a
     * data handoff can drive it while no chat surface is on screen.
     */
    internal val state: StateFlow<ChatScreenState> get() = session.state

    /**
     * The feedback dialog currently shown by *this* chat surface, or null when none is open.
     *
     * Deliberately not on [ConciergeConversationSession]: the transcript is shared across every
     * renderer, but an open dialog belongs to the surface the user tapped on. Keeping it here
     * stops a thumbs-up on one surface from popping a dialog on another.
     */
    private val _feedback = MutableStateFlow<Feedback?>(null)
    internal val feedback: StateFlow<Feedback?> = _feedback.asStateFlow()

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
     * List of chat messages in the conversation. Owned by [ConciergeConversationSession], so turns
     * buffered while the chat was hidden are already present when a renderer attaches.
     */
    internal val messages: StateFlow<List<ChatMessage>> get() = session.messages

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
     * The process-lifetime conversation pipeline this ViewModel renders and routes user actions into.
     */
    private val session: ConciergeConversationSession

    /**
     * True when [session] belongs to this ViewModel alone and should be shut down with it.
     * False for the shared process session, which must outlive every renderer.
     */
    private val ownsSession: Boolean

    /**
     * Dispatch function for sending tracking events to the AEP Event Hub.
     * Defaults to MobileCore::dispatchEvent; injectable for testing.
     */
    private val dispatch: ((Event) -> Unit)?

    constructor(application: Application) : this(
        application,
        AndroidSpeechCapturing(application),
        DefaultImageProvider(),
        ConciergeConversationSession.instance,
        MobileCore::dispatchEvent
    )

    internal constructor(application: Application, speechCapturing: AndroidSpeechCapturing) : this(
        application,
        speechCapturing,
        DefaultImageProvider(),
        ConciergeConversationSession.instance,
        MobileCore::dispatchEvent
    )

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        session: ConciergeConversationSession,
        ownsSession: Boolean = false
    ) : this(application, speechCapturing, DefaultImageProvider(), session, null, ownsSession)

    internal constructor(
        application: Application,
        speechCapturing: SpeechCapturing,
        imageProvider: ImageProvider,
        session: ConciergeConversationSession,
        dispatch: ((Event) -> Unit)? = null,
        ownsSession: Boolean = false
    ) : super(application) {
        this.speechCapturing = speechCapturing
        this.imageProvider = imageProvider
        this.session = session
        this.ownsSession = ownsSession
        this.dispatch = dispatch
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

        _feedback.value = Feedback(interactionId, type)
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
        session.applyFeedbackState(feedback.interactionId, feedbackState)

        // Hide dialog
        _feedback.value = null

        dispatchTrackingEvent(ConciergeTrackingEvent.FeedbackSubmitted(
            conversationId = session.conversationId ?: "",
            interactionId = feedback.interactionId,
            feedbackType = when (feedback.feedbackType) {
                FeedbackType.POSITIVE -> ConciergeConstants.ChatInteraction.POSITIVE
                FeedbackType.NEGATIVE -> ConciergeConstants.ChatInteraction.NEGATIVE
            },
            selectedOptions = feedback.selectedCategories,
            notes = feedback.notes
        ))

        // Send feedback to the conversation service
        session.sendFeedback(feedback)
    }

    /**
     * Handles dismissing the feedback dialog
     */
    private fun handleDismissFeedbackDialog() {
        _feedback.value = null
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
     *
     * An open feedback dialog is deliberately left alone: it belongs to an earlier, completed turn
     * and is unrelated to whatever just failed, so closing it would throw away notes the user is
     * part-way through typing.
     *
     * @param message The error message to display
     */
    private fun handleProcessingError(message: String) {
        session.reportProcessingError(message)
    }

    /**
     * Resets this chat surface to its initial interaction state: idle, with the input field and
     * any open feedback dialog cleared.
     *
     * The transcript itself is deliberately kept - see [ConciergeConversationSession.reset] - so
     * the turn a dismissed dialog referred to stays on screen and can still be rated again.
     * Clearing the dialog here restores what used to happen implicitly, back when it rode on
     * [ChatScreenState] and reset replaced the whole state object.
     */
    private fun handleResetChat() {
        session.reset()
        _feedback.value = null
        _inputState.update { UserInputState.Empty }
    }

    /**
     * Handles sending a user message
     * @param messageText The text of the message to send
     */
    private fun handleSendMessage(messageText: String) {
        if (messageText.isBlank()) return

        if (!session.sendMessage(messageText)) {
            handleProcessingError("Unable to queue chat message")
            return
        }

        dispatchTrackingEvent(ConciergeTrackingEvent.QuerySubmitted(messageText))
        if (_showWelcomeCard.value) {
            dismissWelcomeCard()
        }
        markUserAsReturning()
        _inputState.update { UserInputState.Empty }
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
        super.onCleared()
        imageProvider.clear()
        speechCapturing.setListener(null)
        speechCapturing.release()
        // The shared process session is deliberately left running: tearing down its queue or
        // service would break handoffs that arrive while no chat surface exists. Only a session
        // this ViewModel privately owns is released with it.
        if (ownsSession) {
            session.shutdown()
        }
    }
}
