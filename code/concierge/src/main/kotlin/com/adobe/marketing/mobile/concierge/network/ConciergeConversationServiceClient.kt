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

package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ConciergeConversationServiceClient(
    private val stateRepository: ConciergeStateRepository = ConciergeStateRepository.instance,
    private val sessionManager: ConciergeSessionManager = ConciergeSessionManager.instance,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    companion object {
        private const val TAG = "ConciergeConversationServiceClient"

        private const val DEFAULT_CONNECT_TIMEOUT = 30
        private const val DEFAULT_READ_TIMEOUT = 60
    }
    
    // Shared StateFlow that continuously tracks state updates
    private val conciergeState: StateFlow<ConciergeState> = stateRepository.state
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = stateRepository.state.value
        )

    private val endpoint: String
        get() {
            val currentState = conciergeState.value
            val sessionId = sessionManager.getSessionId()
            return "https://${currentState.conciergeServer}/brand-concierge/conversations" +
                    "?configId=${currentState.conciergeConfigId}" +
                    "&sessionId=$sessionId" +
                    "&requestId=${UUID.randomUUID()}"
        }


    /**
     * Initiates a chat conversation and returns a cold Flow of parsed conversation messages.
     *
     * Behavior:
     * - Establishes an HTTP SSE connection and consumes events on Dispatchers.IO
     * - Uses [SSEParser] to parse the raw SSE stream into [StreamingEvent]s
     * - Converts DataReceived/EventReceived to [ParsedConversationMessage] and emits them downstream
     * - On HTTP errors or stream errors, throws an exception to the collector
     *
     * The lifecycle events (Started/Closed) are handled internally and are not emitted as messages.
     */
    fun chat(message: String): Flow<ParsedConversationMessage> = flow {
        val requestBody = createRequestBody(message, stateRepository.state.value)
        val request = createConversationServiceRequest(endpoint, requestBody)

        val connection = connect(request)
        var eventOrDataReceived = false

        processResponse(connection).collect { event ->
            when (event) {
                is StreamingEvent.EventReceived -> {
                    val parsed = TempConversationResponseParser.parseConversationData(event.data)
                    eventOrDataReceived = true
                    parsed.forEach { emit(it) }
                }

                is StreamingEvent.DataReceived -> {
                    val parsed = TempConversationResponseParser.parseConversationData(event.data)
                    eventOrDataReceived = true
                    parsed.forEach { emit(it) }
                }

                is StreamingEvent.Closed -> {
                    // We need to emit a final COMPLETED for the case where
                    // the stream closes without data/event indicating completion
                    // We don't have a message to pass here, so we can use an empty string
                    if (!eventOrDataReceived) {
                        emit(ParsedConversationMessage("", ConversationState.COMPLETED))
                    }
                }

                is StreamingEvent.Retry -> {
                    // TODO: Implement retry logic if needed
                }

                else -> {
                    // ignore Started/Closed here; Error is rethrown by processResponse
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Creates the JSON request body for the conversation request.
     */
    private fun createRequestBody(message: String, state: ConciergeState): String {
        val surfaces = state.surfaces
        return """
        {
            "events": [
                {
                    "meta": {
                         "consent": {
                            "state": {
                                "val": "${state.consent}"
                            }
                        }
                    },
                    "query": {
                        "conversation": {
                            "surfaces": ${surfaces.joinToString(",", "[\"", "\"]") { it }},
                            "message": "${message.replace("\"", "\\\"")}"
                        }
                    },
                    "xdm": {
                        "identityMap": {
                            "ECID": [
                                {
                                    "id": "${state.experienceCloudId}"
                                }
                            ]
                        }
                    }
                }
            ]
        }
    """.trimIndent()
    }

    /**
     * Establishes the network connection asynchronously and resumes with the connection.
     *
     * @param request NetworkRequest to use for establishing the SSE connection
     * @throws IOException when connection could not be established
     */
    private suspend fun connect(request: NetworkRequest): HttpConnecting =
        suspendCancellableCoroutine { continuation ->
            val callback = object : NetworkCallback {
                override fun call(connection: HttpConnecting?) {
                    when {
                        connection == null -> continuation.resumeWithException(
                            IOException("Failed to establish connection")
                        )

                        continuation.isActive -> continuation.resume(connection)
                    }
                }
            }

            ServiceProvider.getInstance().networkService.connectAsync(request, callback)

            continuation.invokeOnCancellation {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Connection cancelled")
            }
        }

    /**
     * Creates a POST [NetworkRequest] configured for Server-Sent Events (SSE).
     *
     * @param url endpoint URL
     * @param body JSON request body
     * @param additionalHeaders optional additional headers
     * @param connectTimeout connection timeout in seconds
     * @param readTimeout read timeout in seconds
     */
    private fun createConversationServiceRequest(
        url: String,
        body: String,
        additionalHeaders: Map<String, String> = emptyMap(),
        connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
        readTimeout: Int = DEFAULT_READ_TIMEOUT
    ): NetworkRequest {
        val headers = mapOf(
            "Accept" to "text/event-stream",
            "Cache-Control" to "no-cache",
            "Content-Type" to "application/json",
        ) + additionalHeaders


        return NetworkRequest(
            url,
            HttpMethod.POST,
            body.toByteArray(StandardCharsets.UTF_8),
            headers,
            connectTimeout,
            readTimeout
        )
    }


    /**
     * Consumes the HTTP response stream and returns a Flow of [StreamingEvent].
     *
     * Behavior:
     * - Validates HTTP status; on non-2xx throws an [IOException]
     * - Delegates parsing to [SSEParser.process], which emits Started, data events, and Closed
     * - Re-throws on [StreamingEvent.Error]
     * - Always closes the underlying [HttpConnecting]
     */
    private fun processResponse(
        connection: HttpConnecting
    ): Flow<StreamingEvent> = flow {
        try {
            validateResponseCode(connection)

            val input = connection.inputStream ?: throw IOException("Input stream is null")
            BufferedReader(
                InputStreamReader(input, StandardCharsets.UTF_8)
            ).use { reader ->
                SSEParser().process(reader).collect { event ->
                    when (event) {
                        is StreamingEvent.Started -> {
                            Log.trace(
                                ConciergeConstants.EXTENSION_NAME,
                                TAG,
                                "Streaming connection started"
                            )
                        }

                        is StreamingEvent.DataReceived -> emit(event)
                        is StreamingEvent.EventReceived -> emit(event)
                        is StreamingEvent.Error -> {
                            Log.error(
                                ConciergeConstants.EXTENSION_NAME,
                                TAG,
                                "Streaming error: ${event.exception.message}"
                            )
                            connection.close()
                            throw event.exception
                        }

                        is StreamingEvent.Retry -> {
                            Log.debug(
                                ConciergeConstants.EXTENSION_NAME,
                                TAG,
                                "Server requested retry interval: ${event.delayMillis} ms"
                            )
                            emit(StreamingEvent.Retry(event.delayMillis))
                        }

                        is StreamingEvent.Closed -> {
                            Log.debug(
                                ConciergeConstants.EXTENSION_NAME,
                                TAG,
                                "Streaming connection closed: ${event.reason}"
                            )
                            connection.close()
                            emit(event)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Streaming response processing error: ${e.message}"
            )
            throw e
        } finally {
            connection.close()
        }
    }

    /**
     * Validates the HTTP response code.
     * @throws IOException if the response code indicates an error (not in 200-299 range)
     */
    @Throws(IOException::class)
    private fun validateResponseCode(connection: HttpConnecting) {
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            throw IOException("HTTP error: $responseCode ${connection.responseMessage}")
        }
    }

    /**
     * Sends feedback for a conversation turn to the conversation service.
     *
     * @param feedback The feedback containing turnId, rating, categories, and notes
     * @return true if the feedback was successfully sent, false otherwise
     */
    suspend fun sendFeedback(feedback: Feedback): Boolean = withContext(Dispatchers.IO) {
        try {
            val state = stateRepository.state.value
            val requestBody = createFeedbackRequestBody(feedback, state)
            val request = createFeedbackRequest(endpoint, requestBody)

            val connection = connect(request)

            try {
                validateResponseCode(connection)
                true
            } finally {
                connection.close()
            }
        } catch (e: Exception) {
            Log.error(ConciergeConstants.EXTENSION_NAME, TAG, "Failed to send feedback: ${e.message}")
            false
        }
    }

    /**
     * Creates the feedback request body in XDM format
     */
    private fun createFeedbackRequestBody(feedback: Feedback, state: ConciergeState): String {
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val localTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())
        val timeZoneOffset =
            TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000 // offset in minutes

        val rawArray = if (feedback.notes.isNotBlank()) {
            """[{"text": "${feedback.notes.replace("\"", "\\\"")}","purpose": "user input"}]"""
        } else {
            "[]"
        }

        val conversationIdLine = feedback.conversationId?.let {
            """"conversationID": "$it","""
        } ?: ""

        val isPositive = feedback.feedbackType == FeedbackType.POSITIVE

        // TODO: this has to be formalized in a JSON structure once the data model is finalized.
        return """
{
    "events": [{
        "meta": {
            "consent": {
                "state": {
                    "val": "${state.consent}"
                }
            }
        },
        "xdm": {
            "identityMap": {
                "ECID": [{
                    "id": "${state.experienceCloudId}"
                }]
            },
            "conversation": {
                "feedback": {
                    "source": "end-user",
                    "raw": $rawArray,
                    "rating": {
                        "score": ${if (isPositive) 1 else 0},
                        "classification": "${if (isPositive) "Thumbs Up" else "Thumbs Down"}",
                        "reasons": [${feedback.selectedCategories.joinToString(",") { "\"$it\"" }}]
                    }
                },
                $conversationIdLine
                "turnID": "${feedback.interactionId}"
            },
            "eventType": "conversation.feedback",
            "timestamp": "$timestamp",
            "placeContext": {
                "localTimezoneOffset": $timeZoneOffset,
                "localTime": "$localTime"
            },
            "implementationDetails": {
            	"environment": "app",
            	"name": "https:\/\/ns.adobe.com\/experience\/mobilesdk\/android",
                "version": "3.5.0+${ConciergeConstants.EXTENSION_VERSION}"
            }
        }
    }]
}
""".trimIndent()
    }

    /**
     * Creates a POST [NetworkRequest] for sending feedback.
     */
    private fun createFeedbackRequest(
        url: String,
        body: String,
        connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
        readTimeout: Int = DEFAULT_READ_TIMEOUT
    ): NetworkRequest = NetworkRequest(
        url,
        HttpMethod.POST,
        body.toByteArray(StandardCharsets.UTF_8),
        mapOf("Content-Type" to "application/json"),
        connectTimeout,
        readTimeout
    )

    /**
     * Cleanup method to cancel any ongoing network operations.
     * Should be called when the client is no longer needed to prevent memory leaks.
     */
    fun cleanup() {
        scope.cancel()
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "ConciergeConversationServiceClient cleanup completed"
        )
    }
}