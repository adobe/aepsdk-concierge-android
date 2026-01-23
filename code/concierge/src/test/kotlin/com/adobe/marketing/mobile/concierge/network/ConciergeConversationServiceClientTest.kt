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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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

    @Test
    fun `chat request includes default consent value in meta`() = runTest {
        // Given
        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // When
        client.chat("test message").toList()

        // Then
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain meta.consent", requestBody.contains("\"meta\""))
        assertTrue("Request should contain consent state", requestBody.contains("\"consent\""))
        assertTrue("Request should contain consent value", requestBody.contains("\"val\": \"in\""))
    }

    @Test
    fun `chat request includes custom consent value when set`() = runTest {
        // Given
        val customState = testState.copy(consent = "out")
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // When
        client.chat("test message").toList()

        // Then
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain consent value 'out'", requestBody.contains("\"val\": \"out\""))
    }

    @Test
    fun `chat request includes unknown consent value correctly`() = runTest {
        // Given
        val customState = testState.copy(consent= "unknown")
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val requestSlot = slot<NetworkRequest>()
        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { connection.inputStream } returns ByteArrayInputStream(ByteArray(0))
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // When
        client.chat("test message").toList()

        // Then
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain consent value 'unknown'", requestBody.contains("\"val\": \"unknown\""))
    }

    // ========== Feedback Request Tests ==========

    @Test
    fun `sendFeedback includes default consent value in meta`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "test-interaction-123",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful"),
            notes = "Great response!",
            conversationId = "conv-123"
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue("Feedback should be sent successfully", result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain meta.consent", requestBody.contains("\"meta\""))
        assertTrue("Request should contain consent state", requestBody.contains("\"consent\""))
        assertTrue("Request should contain default consent value 'in'", requestBody.contains("\"val\": \"in\""))
    }

    @Test
    fun `sendFeedback includes custom consent value when set to out`() = runTest {
        // Given
        val customState = testState.copy(consent = "out")
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val feedback = Feedback(
            interactionId = "test-interaction-456",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = listOf("Not helpful"),
            notes = "",
            conversationId = "conv-456"
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue("Feedback should be sent successfully", result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain consent value 'out'", requestBody.contains("\"val\": \"out\""))
    }

    @Test
    fun `sendFeedback includes unknown consent value correctly`() = runTest {
        // Given
        val customState = testState.copy(consent = "unknown")
        val stateFlow = MutableStateFlow(customState)
        every { mockStateRepository.state } returns stateFlow

        val feedback = Feedback(
            interactionId = "test-interaction-789",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList(),
            notes = "Test notes",
            conversationId = null
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue("Feedback should be sent successfully", result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        assertTrue("Request should contain consent value 'unknown'", requestBody.contains("\"val\": \"unknown\""))
    }

    @Test
    fun `sendFeedback includes proper structure with positive feedback`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "interaction-positive",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful", "Clear"),
            notes = "Excellent!",
            conversationId = "conv-positive"
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue(result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        // Verify consent is present
        assertTrue("Should contain consent", requestBody.contains("\"consent\""))
        assertTrue("Should contain consent value", requestBody.contains("\"val\": \"in\""))
        
        // Verify feedback structure
        assertTrue("Should contain interaction ID", requestBody.contains("\"turnID\": \"interaction-positive\""))
        assertTrue("Should contain conversation ID", requestBody.contains("\"conversationID\": \"conv-positive\""))
        assertTrue("Should contain score 1 for positive", requestBody.contains("\"score\": 1"))
        assertTrue("Should contain Thumbs Up", requestBody.contains("\"Thumbs Up\""))
        assertTrue("Should contain categories", requestBody.contains("\"Helpful\""))
        assertTrue("Should contain categories", requestBody.contains("\"Clear\""))
        assertTrue("Should contain notes", requestBody.contains("Excellent!"))
    }

    @Test
    fun `sendFeedback includes proper structure with negative feedback`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "interaction-negative",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = listOf("Confusing", "Inaccurate"),
            notes = "Not helpful",
            conversationId = "conv-negative"
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue(result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        // Verify consent is present
        assertTrue("Should contain consent", requestBody.contains("\"consent\""))
        
        // Verify feedback structure
        assertTrue("Should contain interaction ID", requestBody.contains("\"turnID\": \"interaction-negative\""))
        assertTrue("Should contain conversation ID", requestBody.contains("\"conversationID\": \"conv-negative\""))
        assertTrue("Should contain score 0 for negative", requestBody.contains("\"score\": 0"))
        assertTrue("Should contain Thumbs Down", requestBody.contains("\"Thumbs Down\""))
        assertTrue("Should contain categories", requestBody.contains("\"Confusing\""))
        assertTrue("Should contain categories", requestBody.contains("\"Inaccurate\""))
        assertTrue("Should contain notes", requestBody.contains("Not helpful"))
    }

    @Test
    fun `sendFeedback handles empty categories and notes`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "interaction-minimal",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList(),
            notes = "",
            conversationId = null
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue(result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        // Verify consent is still present
        assertTrue("Should contain consent", requestBody.contains("\"consent\""))
        assertTrue("Should contain consent value", requestBody.contains("\"val\": \"in\""))
        
        // Verify empty raw array for notes
        assertTrue("Should have empty raw array", requestBody.contains("\"raw\": []"))
        
        // Verify no conversation ID line
        assertFalse("Should not contain conversationID when null", 
            requestBody.contains("\"conversationID\":") && requestBody.contains("null"))
    }

    @Test
    fun `sendFeedback returns false on network error`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "test-interaction",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList(),
            notes = "",
            conversationId = null
        )

        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(null) // Simulate connection failure
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertFalse("Feedback should return false on network error", result)
    }

    @Test
    fun `sendFeedback returns false on HTTP error`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "test-interaction",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList(),
            notes = "",
            conversationId = null
        )

        val connection = mockk<HttpConnecting>(relaxed = true)
        every { connection.responseCode } returns 500
        every { connection.responseMessage } returns "Internal Server Error"
        every { networkService.connectAsync(any(), any()) } answers {
            val cb = secondArg<NetworkCallback>()
            cb.call(connection)
        }

        val client = ConciergeConversationServiceClient(mockStateRepository, mockSessionManager)

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertFalse("Feedback should return false on HTTP error", result)
    }

    @Test
    fun `sendFeedback escapes special characters in notes`() = runTest {
        // Given
        val feedback = Feedback(
            interactionId = "test-interaction",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList(),
            notes = "Response with \"quotes\" and special chars",
            conversationId = null
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

        // When
        val result = client.sendFeedback(feedback)

        // Then
        assertTrue(result)
        val capturedRequest = requestSlot.captured
        val requestBody = String(capturedRequest.body, StandardCharsets.UTF_8)
        
        // Verify consent is present
        assertTrue("Should contain consent", requestBody.contains("\"consent\""))
        
        // Verify escaped quotes
        assertTrue("Should escape quotes in notes", requestBody.contains("\\\"quotes\\\""))
    }

    @After
    fun tearDown() {
        unmockkStatic(ServiceProvider::class)
    }

    private fun toSse(json: String): String =
        json.lines().joinToString("\n") { "data: $it" } + "\n\n"
}
