package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.ServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.collections.mapOf
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Configuration for the conversation service request.
 */
internal data class ConversationConfig(
    val configId: String = "51ee226f-9327-4b97-99fb-d5f9877d8198",
    val sessionId: String = "083f7d55-df46-43f3-a70d-626cc324d1ef",
    val requestId: String = "f199b4ed-50db-44cd-9371-291778e81927",
    val baseUrl: String = "https://bc-conversation-service-dev.corp.ethos11-stage-va7.ethos.adobe.net",
    val surfaces: List<String> = listOf("web://bc-conversation-service-dev.corp.ethos11-stage-va7.ethos.adobe.net/brand-concierge/pages/745F37C35E4B776E0A49421B@AdobeOrg/index.html")
)

internal class ConciergeConversationServiceClient(
    private val config: ConversationConfig = ConversationConfig()
) {
    
    companion object {
        private const val LOG_TAG = ConciergeConstants.EXTENSION_FRIENDLY_NAME
        private const val TAG = "ConciergeConversationServiceClient"
        
        private const val DEFAULT_CONNECT_TIMEOUT = 30
        private const val DEFAULT_READ_TIMEOUT = 60
    }

    private val endpoint: String
        get() = "${config.baseUrl}/brand-concierge/conversations" +
                "?configId=${config.configId}&sessionId=${config.sessionId}&requestId=${config.requestId}"


    /**
     * Initiates a chat conversation and returns parsed conversation messages as they stream in.
     * All network operations are performed on the IO dispatcher. Parsing and streaming concerns
     * are encapsulated so consumers receive structured data.
     */
    fun chat(message: String): Flow<ParsedConversationMessage> = flow {
            val requestBody = createRequestBody(message)
            val request = createConversationServiceRequest(endpoint, requestBody)

            try {
                val connection = connect(request)

                processResponse(connection) { event ->
                    when (event) {
                        is StreamingEvent.Started -> {
                            Log.debug(LOG_TAG, TAG, "Streaming connection started")
                        }
                        is StreamingEvent.EventReceived -> {
                            val parsed = TempConversationResponseParser.parseConversationData(event.data)
                            parsed.forEach { emit(it) }
                        }
                        is StreamingEvent.DataReceived -> {
                            val parsed = TempConversationResponseParser.parseConversationData(event.data)
                            parsed.forEach { emit(it) }
                        }
                        is StreamingEvent.Error ->  {
                            throw event.exception
                            connection.close()
                        }
                        is StreamingEvent.Closed -> {
                            connection.close()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.error(LOG_TAG, TAG, "Chat error: ${e.message}")
                throw e
            }
        }.flowOn(Dispatchers.IO)
    
    /**
     * Creates the JSON request body for the conversation.
     */
    private fun createRequestBody(message: String): String = """
        {
            "events": [
                {
                    "query": {
                        "conversation": {
                            "fetchConversationalExperience": true,
                            "surfaces": ${config.surfaces.joinToString(",", "[\"", "\"]") { it }},
                            "message": "${message.replace("\"", "\\\"")}"
                        }
                    }
                }
            ]
        }
    """.trimIndent()
    
    /**
     * Suspending function to establish network connection.
     * @param request the NetworkRequest to connect with
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
                Log.debug(LOG_TAG, TAG, "Connection cancelled")
            }
        }

    /**
     * Creates a NetworkRequest configured for Server-Sent Events (SSE) with POST method.
     * 
     * @param url the endpoint URL for the SSE stream
     * @param body the request body as JSON
     * @param additionalHeaders optional additional headers to include
     * @param connectTimeout connection timeout in seconds
     * @param readTimeout read timeout in seconds
     * @return NetworkRequest configured for SSE streaming with POST
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
     * Processes the streaming response and emits events.
     * @param connection The established HttpConnecting
     * @param onEvent Callback to emit StreamingEvent
     */
    private suspend fun processResponse(
        connection: HttpConnecting,
        onEvent: suspend (StreamingEvent) -> Unit
    ) {
        BufferedReader(
            InputStreamReader(connection.inputStream ?: throw IOException("Input stream is null"), StandardCharsets.UTF_8)
        ).use { reader ->
            try {
                validateResponseCode(connection)
                SSEParser().processEvents(reader, onEvent)
            } catch (e: IOException) {
                Log.warning(LOG_TAG, TAG, "Streaming connection error: ${e.message}")
                onEvent(StreamingEvent.Error(e))
            } catch (e: Exception) {
                Log.warning(LOG_TAG, TAG, "Unexpected streaming error: ${e.message}")
                onEvent(StreamingEvent.Error(e))
            } finally {
                connection.close()
                onEvent(StreamingEvent.Closed("Connection closed"))
            }
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
     * Cleanup method to cancel any ongoing network operations.
     * Should be called when the client is no longer needed to prevent memory leaks.
     */
    fun cleanup() {
        Log.debug(LOG_TAG, TAG, "ConciergeConversationServiceClient cleanup completed")
    }
}