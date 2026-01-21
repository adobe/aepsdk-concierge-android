/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.HttpMethod
import com.adobe.marketing.mobile.services.NetworkCallback
import com.adobe.marketing.mobile.services.NetworkRequest
import com.adobe.marketing.mobile.services.Networking
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ConciergeConversationServiceClientTest {

    private lateinit var serviceProvider: ServiceProvider
    private lateinit var networkService: Networking
    private lateinit var mockStateRepository: ConciergeStateRepository
    private lateinit var mockSessionManager: ConciergeSessionManager
    private val testState = ConciergeState(
        experienceCloudId = "test-ecid",
        configurationReady = true,
        conciergeSurfaces = listOf("surface1", "surface2"),
        conciergeServer = "https://test-server.com",
        conciergeConfigId = "test-config-id"
    )

    @Before
    fun setup() {
        mockkStatic(ServiceProvider::class)
        serviceProvider = mockk(relaxed = true)
        networkService = mockk()
        every { ServiceProvider.getInstance() } returns serviceProvider
        every { serviceProvider.networkService } returns networkService
        
        // Mock ConciergeStateRepository
        mockStateRepository = mockk(relaxed = true)
        val stateFlow = MutableStateFlow(testState)
        every { mockStateRepository.state } returns stateFlow
        
        // Mock ConciergeSessionManager
        mockSessionManager = mockk(relaxed = true)
        every { mockSessionManager.getSessionId() } returns "test-session-id"
    }

    @Test
    fun `chat emits parsed messages from SSE data events`() = runTest {
        // Prepare SSE stream with two data events containing JSON conversation payloads
        val json1 = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Hello!",
                        "promptSuggestions": ["a"]
                      },
                      "state": "in-progress",
                      "conversationId": "c1",
                      "interactionId": "i1"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val json2 = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {
                        "message": "Bye."
                      },
                      "state": "completed",
                      "conversationId": "c1",
                      "interactionId": "i2"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val sse = buildString {
            append(toSse(json1))
            append(toSse(json2))
        }

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))

        // connectAsync should invoke the callback with our connection
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        val emitted = mutableListOf<ParsedConversationMessage>()
        client.chat("hi").toList(emitted)

        assertEquals(2, emitted.size)
        assertEquals("Hello!", emitted[0].messageContent)
        assertEquals(ConversationState.IN_PROGRESS, emitted[0].state)
        assertEquals("Bye.", emitted[1].messageContent)
        assertEquals(ConversationState.COMPLETED, emitted[1].state)
    }

    @Test
    fun `chat throws when HTTP response is non-2xx`() = runTest {
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 500
        every { connection.responseMessage } returns "Server error"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        var threw = false
        try {
            // Collect at least one emission; should throw due to error mapping
            client.chat("hi").first()
        } catch (e: Exception) {
            threw = true
            assertTrue(e is IOException)
        }
        assertTrue(threw)
    }

    @Test
    fun `chat with null inputStream throws IOException`() = runTest {
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns null
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        var threw = false
        try {
            client.chat("hi").first()
        } catch (e: Exception) {
            threw = true
            assertTrue(e is IOException)
        }
        assertTrue(threw)
    }

    @Test
    fun `ignores non-conversation type emits nothing`() = runTest {
        val nonConversationJson = """
            {
              "handle": [
                {
                  "type": "some-other:type",
                  "payload": [
                    { "response": { "message": "Should be ignored" } }
                  ]
                }
              ]
            }
        """.trimIndent()

        val sse = toSse(nonConversationJson)

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(0, emitted.size)
    }

    @Test
    fun `malformed JSON yields no emissions`() = runTest {
        val sse = "data: not-json\n\n"

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(0, emitted.size)
    }

    @Test
    fun `connect callback null throws IOException`() = runTest {
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(null)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        var threw = false
        try {
            client.chat("hi").first()
        } catch (e: Exception) {
            threw = true
            assertTrue(e is IOException)
        }
        assertTrue(threw)
    }

    @Test
    fun `event field conversation handled correctly`() = runTest {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    { "response": { "message": "Hi" }, "state": "in-progress" }
                  ]
                }
              ]
            }
        """.trimIndent()

        val sse = buildString {
            append("event: brand-concierge:conversation\n")
            append(json.lines().joinToString("\n") { "data: $it" })
            append("\n\n")
        }

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(1, emitted.size)
        assertEquals("Hi", emitted[0].messageContent)
        assertEquals(ConversationState.IN_PROGRESS, emitted[0].state)
    }

    @Test
    fun `request is built with correct method headers url and body`() = runTest {
        val requestSlot = slot<NetworkRequest>()

        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            val connection = mockk<HttpConnecting>(relaxed = true)
            every { connection.responseCode } returns 200
            every { connection.responseMessage } returns "OK"
            every { connection.inputStream } returns null
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        var threw = false
        try {
            client.chat("hello \"world\"").first()
        } catch (_: Exception) {
            threw = true
        }
        assertTrue(threw)

        val req = requestSlot.captured
        assertEquals(HttpMethod.POST, req.method)
        assertEquals("text/event-stream", req.headers["Accept"])
        assertEquals("no-cache", req.headers["Cache-Control"])
        assertEquals("application/json", req.headers["Content-Type"])
        // default timeouts
        assertEquals(30, req.connectTimeout)
        assertEquals(60, req.readTimeout)
        // sanity checks on URL params
        assertTrue(req.url.contains("configId="))
        assertTrue(req.url.contains("sessionId="))
        assertTrue(req.url.contains("requestId="))
        val bodyStr = String(req.body ?: ByteArray(0), StandardCharsets.UTF_8)
        // TODO: Finalize and verify full body structure
    }

    @Test
    fun `blank data yields single COMPLETED empty emission`() = runTest {
        val sse = "data:\n\n"

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(1, emitted.size)
        assertEquals("", emitted[0].messageContent)
        assertEquals(ConversationState.COMPLETED, emitted[0].state)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `cancellation closes connection`() = runTest {
        val json1 = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": { "message": "one" },
                      "state": "in-progress"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val json2 = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": { "message": "two" },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val sse = toSse(json1) + toSse(json2)

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").take(1).toList(mutableListOf())
        assertEquals(1, emitted.size)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `mid-read error throws and closes connection`() = runTest {
        // InputStream that throws after a few bytes
        val payload = "data: {\"handle\":[]}\n\n".toByteArray()
        var index = 0
        val throwingStream = object : InputStream() {
            override fun read(): Int {
                if (index >= payload.size / 2) throw IOException("boom")
                return payload[index++].toInt()
            }
        }

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns throwingStream
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        var threw = false
        try {
            client.chat("hi").first()
        } catch (e: Exception) {
            threw = true
            assertTrue(e is IOException)
        }
        assertTrue(threw)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `empty stream yields single COMPLETED empty emission and closes`() = runTest {
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(1, emitted.size)
        assertEquals("", emitted[0].messageContent)
        assertEquals(ConversationState.COMPLETED, emitted[0].state)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `http 204 no content yields single COMPLETED empty emission and closes`() = runTest {
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 204
        every { connection.responseMessage } returns "No Content"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(1, emitted.size)
        assertEquals("", emitted[0].messageContent)
        assertEquals(ConversationState.COMPLETED, emitted[0].state)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `graceful end of stream completes and closes`() = runTest {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": { "message": "Done" },
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val sse = toSse(json)

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val emitted = client.chat("hi").toList(mutableListOf())
        assertEquals(1, emitted.size)
        assertEquals("Done", emitted[0].messageContent)
        assertEquals(ConversationState.COMPLETED, emitted[0].state)
        verify(atLeast = 1) { connection.close() }
    }

    // ========== Feedback Tests ==========

    @Test
    fun `sendFeedback returns true on successful submission`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful", "Accurate"),
            notes = "Great response!",
            conversationId = "conv-456"
        )

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val result = client.sendFeedback(feedback)

        assertTrue(result)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `sendFeedback returns false on HTTP error`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.NEGATIVE
        )

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 500
        every { connection.responseMessage } returns "Server Error"
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val result = client.sendFeedback(feedback)

        assertFalse(result)
        verify(atLeast = 1) { connection.close() }
    }

    @Test
    fun `sendFeedback returns false on connection failure`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE
        )

        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(null)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val result = client.sendFeedback(feedback)

        assertFalse(result)
    }

    @Test
    fun `sendFeedback returns false on network exception`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.NEGATIVE
        )

        every { networkService.connectAsync(any(), any()) } throws IOException("Network error")

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        val result = client.sendFeedback(feedback)

        assertFalse(result)
    }

    @Test
    fun `sendFeedback uses correct HTTP method and headers`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val request = requestSlot.captured
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals(30, request.connectTimeout)
        assertEquals(60, request.readTimeout)
    }

    @Test
    fun `sendFeedback creates request body with positive feedback`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful", "Accurate"),
            notes = "Great!",
            conversationId = "conv-456"
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"score\": 1"))
        assertTrue(requestBody.contains("\"classification\": \"Thumbs Up\""))
        assertTrue(requestBody.contains("\"turnID\": \"interaction-123\""))
        assertTrue(requestBody.contains("\"conversationID\": \"conv-456\""))
        assertTrue(requestBody.contains("\"Helpful\""))
        assertTrue(requestBody.contains("\"Accurate\""))
        assertTrue(requestBody.contains("\"text\": \"Great!\""))
    }

    @Test
    fun `sendFeedback creates request body with negative feedback`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-789",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = listOf("Inaccurate"),
            notes = "Not helpful"
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"score\": 0"))
        assertTrue(requestBody.contains("\"classification\": \"Thumbs Down\""))
        assertTrue(requestBody.contains("\"turnID\": \"interaction-789\""))
        assertTrue(requestBody.contains("\"Inaccurate\""))
        assertTrue(requestBody.contains("\"text\": \"Not helpful\""))
    }

    @Test
    fun `sendFeedback creates request body with empty notes`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            notes = ""
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"raw\": []"))
    }

    @Test
    fun `sendFeedback creates request body without conversationId when null`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            conversationId = null
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertFalse(requestBody.contains("\"conversationID\""))
    }

    @Test
    fun `sendFeedback escapes special characters in notes`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            notes = "Response with \"quotes\" is great"
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\\\"quotes\\\""))
    }

    @Test
    fun `sendFeedback includes ECID in request body`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"ECID\""))
        assertTrue(requestBody.contains("\"id\": \"test-ecid\""))
    }

    @Test
    fun `sendFeedback includes timestamp fields in request body`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.POSITIVE
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"timestamp\""))
        assertTrue(requestBody.contains("\"localTime\""))
        assertTrue(requestBody.contains("\"localTimezoneOffset\""))
        assertTrue(requestBody.contains("\"eventType\": \"conversation.feedback\""))
    }

    @Test
    fun `sendFeedback includes empty categories array when no categories selected`() = runTest {
        val feedback = Feedback(
            interactionId = "interaction-123",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = emptyList()
        )

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.sendFeedback(feedback)

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\"reasons\": []"))
    }

    // ========== Cleanup Tests ==========

    @Test
    fun `cleanup cancels the scope`() {
        val testJob = SupervisorJob()
        val testScope = CoroutineScope(testJob + Dispatchers.Default)
        val client = ConciergeConversationServiceClient(
            mockStateRepository,
            mockSessionManager,
            testScope
        )

        assertTrue(testJob.isActive)
        client.cleanup()
        assertFalse(testJob.isActive)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `chat request with empty surfaces list`() = runTest {
        val customState = testState.copy(conciergeSurfaces = emptyList())
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.chat("test").toList()

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        // When surfaces list is empty, joinToString produces [""]
        assertTrue(requestBody.contains("\"surfaces\": [\"\"]"))
    }

    @Test
    fun `chat request with null surfaces list`() = runTest {
        val customState = testState.copy(conciergeSurfaces = null)
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.chat("test").toList()

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        // When surfaces is null, it's converted to empty list and joinToString produces [""]
        assertTrue(requestBody.contains("\"surfaces\": [\"\"]"))
    }

    @Test
    fun `chat request escapes quotes in message`() = runTest {
        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.chat("Message with \"quotes\"").toList()

        val requestBody = String(requestSlot.captured.body, StandardCharsets.UTF_8)
        assertTrue(requestBody.contains("\\\"quotes\\\""))
    }

    @Test
    fun `endpoint includes correct query parameters`() = runTest {
        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)
        client.chat("test").toList()

        val url = requestSlot.captured.url
        assertTrue(url.startsWith("https://https://test-server.com/brand-concierge/conversations"))
        assertTrue(url.contains("configId=test-config-id"))
        assertTrue(url.contains("sessionId=test-session-id"))
        assertTrue(url.contains("requestId="))
    }

    @Test
    fun `multiple sequential chat calls work correctly`() = runTest {
        val json = """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "response": {"message": "Response"},
                      "state": "completed"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
        val sse = toSse(json)

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.inputStream } returns ByteArrayInputStream(sse.toByteArray(StandardCharsets.UTF_8))
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // First call
        val result1 = client.chat("message 1").toList()
        assertEquals(1, result1.size)

        // Second call
        val result2 = client.chat("message 2").toList()
        assertEquals(1, result2.size)

        verify(atLeast = 2) { networkService.connectAsync(any(), any()) }
    }

    @After
    fun tearDown() {
        unmockkStatic(ServiceProvider::class)
    }

    private fun toSse(json: String): String =
        json.lines().joinToString("\n") { "data: $it" } + "\n\n"
}
