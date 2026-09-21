/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.LinkHint
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.utils.buildCardElementDict
import com.adobe.marketing.mobile.concierge.utils.citation.CitationUtils
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Process-lifetime owner of the Concierge conversation — both user-typed chat sends and data
 * handoffs. It outlives the chat composable and the [com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel],
 * so a handoff fired while the chat is hidden or closed is still accepted, forwarded, buffered
 * into [messages], and painted as soon as a renderer observes the flow again.
 *
 * Owning the request queue here also keeps the `CHAT_IN_PROGRESS` contract intact: a chat send and
 * a data handoff contend for the same [pendingConversationRequests] gate regardless of which
 * surface (if any) is currently on screen.
 */
internal class ConciergeConversationSession internal constructor(
    private val chatService: ConversationService = ConciergeConversationServiceClient(),
    private val dispatch: ((Event) -> Unit)? = MobileCore::dispatchEvent,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    private val isProcessSession: Boolean = false
) {
    companion object {
        private const val TAG = "ConciergeConversationSession"
        private const val MAX_PENDING_CONVERSATION_REQUESTS = 64

        /**
         * User-facing fallback shown in the chat when a conversation cannot be completed
         * (for example, due to a network, server, or parsing error). Intentionally generic —
         * the underlying technical detail is sent to logs and telemetry, never to the user.
         */
        private const val DEFAULT_CONVERSATION_ERROR_MESSAGE =
            "Sorry, I encountered an error. Please try again."

        /** The process-wide conversation session shared by every renderer and the handoff seam. */
        internal val instance: ConciergeConversationSession by lazy {
            ConciergeConversationSession(isProcessSession = true)
        }
    }

    /**
     * List of chat messages in the conversation
     */
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    internal val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    /**
     * Tracks the overall state of the chat flow
     */
    private val _state = MutableStateFlow<ChatScreenState>(ChatScreenState.Idle)
    internal val state: StateFlow<ChatScreenState> = _state.asStateFlow()

    /**
     * Tracks the current conversation ID from the backend response
     */
    private var currentConversationId: String? = null

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

    init {
        startConversationProcessor()
    }

    /**
     * Queues a user-typed chat turn and moves the chat to Processing.
     *
     * @param messageText the message to send; callers are expected to have rejected blank text
     * @return false when the bounded request queue is full or closed, in which case no state changed
     */
    fun sendMessage(messageText: String): Boolean {
        pendingConversationRequests.incrementAndGet()
        if (conversationRequests.trySend(ConversationRequest.Chat(messageText)).isFailure) {
            pendingConversationRequests.decrementAndGet()
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Unable to queue chat message because the conversation queue is full or closed."
            )
            return false
        }
        _state.update { ChatScreenState.Processing }
        return true
    }

    /**
     * Returns the chat to the initial idle state. The transcript is intentionally left intact.
     */
    fun reset() {
        _state.update {
            ChatScreenState.Idle
        }
    }

    /**
     * Surfaces a generic error in the chat, keeping [logDetail] for diagnostics only.
     */
    fun reportProcessingError(logDetail: String) {
        Log.error(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Processing error: $logDetail"
        )
        _state.update {
            ChatScreenState.Error(DEFAULT_CONVERSATION_ERROR_MESSAGE)
        }
    }

    /**
     * Releases this session's request queue, processor coroutine, and conversation service.
     *
     * Only for a session someone privately owns. [instance] is meant to live as long as the
     * process and must never be shut down, or handoffs arriving while no chat is on screen would
     * be dropped - which is the capability this class exists to provide.
     */
    internal fun shutdown() {
        if (isProcessSession) {
            // Unrecoverable if allowed: `instance` is a lazy val that can never be rebuilt, so
            // every later send and handoff would fail for the life of the process.
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Ignoring shutdown() of the process conversation session; it must outlive every renderer."
            )
            return
        }

        conversationRequests.close()

        // Settle anything still QUEUED before cancelling the scope. The processor loop and every
        // request's timeout job both live on that scope, so cancelling first strands a queued
        // handoff: it is never delivered, never times out, and its completion callback never runs
        // - leaving the host's dispatchEventWithResponseCallback hanging.
        //
        // A handoff already being processed needs nothing here: scope.cancel() unwinds it into
        // processDataHandoffRequest's CancellationException branch, which completes it with this
        // same reason.
        while (true) {
            val queued = conversationRequests.tryReceive().getOrNull() ?: break
            if (queued is ConversationRequest.DataHandoff) {
                queued.completeAfterReleasingReservation(
                    DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION)
                )
            }
        }

        scope.cancel()
        chatService.cleanup()
    }

    /**
     * The conversation ID captured from the most recent backend response, or null before the
     * first response of a conversation arrives.
     */
    internal val conversationId: String?
        get() = currentConversationId

    /**
     * Stamps [feedbackState] onto the transcript message carrying [interactionId].
     */
    internal fun applyFeedbackState(interactionId: String, feedbackState: FeedbackState) {
        _messages.update { currentMessages ->
            currentMessages.map { message ->
                if (message.interactionId == interactionId) {
                    message.copy(feedbackState = feedbackState)
                } else {
                    message
                }
            }
        }
    }

    /**
     * Sends [feedback] to the conversation service, tagged with the current conversation ID.
     */
    internal fun sendFeedback(feedback: Feedback) {
        scope.launch {
            val feedbackWithConversationId = feedback.copy(conversationId = currentConversationId)

            val success = chatService.sendFeedback(feedbackWithConversationId)
            if (success) {
                Log.debug(
                    TAG,
                    "sendFeedback",
                    "Feedback sent successfully for turnId: ${feedback.interactionId}, conversationId: $currentConversationId"
                )
            } else {
                Log.warning(
                    TAG,
                    "sendFeedback",
                    "Failed to send feedback for turnId: ${feedback.interactionId}, conversationId: $currentConversationId"
                )
            }
        }
    }

    fun enqueueDataHandoff(
        result: ConciergeDataHandoffEvent,
        completion: (DataHandoffDeliveryResult) -> Unit
    ) {
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
        request.timeoutJob = scope.launch {
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

        responseStartedDispatched = false
        request.handoff.localMessage?.let(::appendUserMessage)
        _state.update {
            ChatScreenState.Processing
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

    private fun startConversationProcessor() {
        scope.launch {
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
            ChatScreenState.Processing
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
                is ChatScreenState.Processing -> ChatScreenState.Idle
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

        // Track the conversationId reported by the response. This session is process-lived and
        // outlives any single conversation - ConciergeSessionManager rolls its session ID after
        // 30 idle minutes, after which the backend answers on a new conversation - so follow
        // whatever it currently reports instead of pinning to the first ID ever seen. Pinning
        // would tag later turns' tracking events and feedback with a stale conversation.
        parsedMessage.conversationId?.let { conversationId ->
            if (currentConversationId != conversationId) {
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
                        is ChatScreenState.Processing -> ChatScreenState.Idle
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
                is ChatScreenState.Processing -> ChatScreenState.Idle
                else -> ChatScreenState.Idle
            }
        }
    }

    private fun dispatchTrackingEvent(trackingEvent: ConciergeTrackingEvent) {
        val event = trackingEvent.toEvent()
        Log.debug(ConciergeConstants.LOG_TAG, TAG, "Dispatching tracking event: $event")
        dispatch?.invoke(event)
    }
}
