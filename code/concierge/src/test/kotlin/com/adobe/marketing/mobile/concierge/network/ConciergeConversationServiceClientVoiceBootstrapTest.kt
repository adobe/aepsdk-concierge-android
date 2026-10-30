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

package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.concierge.ConciergeAuthTokenHolder
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
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
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.nio.charset.StandardCharsets

class ConciergeConversationServiceClientVoiceBootstrapTest {

    private lateinit var networkService: Networking
    private lateinit var stateRepository: ConciergeStateRepository
    private lateinit var sessionManager: ConciergeSessionManager
    private lateinit var stateFlow: MutableStateFlow<ConciergeState>
    private val requestSlot = slot<NetworkRequest>()
    private lateinit var connection: HttpConnecting

    private val testState = ConciergeState(
        experienceCloudId = "test-ecid",
        identityMap = mapOf("ECID" to listOf(mapOf("id" to "test-ecid"))),
        configurationReady = true,
        surfaces = listOf("surface1", "surface2"),
        conciergeServer = "edge.example.com",
        conciergeConfigId = "test-config-id",
        conciergeRegion = "va6"
    )

    @Before
    fun setup() {
        mockkStatic(ServiceProvider::class)
        val serviceProvider = mockk<ServiceProvider>(relaxed = true)
        networkService = mockk()
        every { ServiceProvider.getInstance() } returns serviceProvider
        every { serviceProvider.networkService } returns networkService

        stateFlow = MutableStateFlow(testState)
        stateRepository = mockk(relaxed = true)
        every { stateRepository.state } returns stateFlow
        every { stateRepository.withConversation(any(), any<() -> Any?>()) } answers {
            secondArg<() -> Any?>().invoke()
        }
        sessionManager = mockk(relaxed = true)
        every { sessionManager.getSessionId() } returns "test-session-id"

        connection = mockk(relaxed = true)
        every { connection.responseCode } returns 200
        every { connection.responseMessage } returns "OK"
        every { networkService.connectAsync(capture(requestSlot), any()) } answers {
            secondArg<NetworkCallback>().call(connection)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(ServiceProvider::class)
        ConciergeAuthTokenHolder.setProvider(null)
    }

    @Test
    fun `bootstrap posts to the region-less voice endpoint as an event stream`() = runTest {
        respondWith("")

        client().bootstrapVoiceSession().toList()

        val request = requestSlot.captured
        val uri = URI(request.url)
        assertEquals(HttpMethod.POST, request.method)
        assertEquals("edge.example.com", uri.host)
        assertEquals("/brand-concierge-voice/conversations", uri.path)
        assertTrue(uri.query.contains("configId=test-config-id"))
        assertTrue(uri.query.contains("sessionId=test-session-id"))
        assertTrue(uri.query.contains("requestId="))
        assertEquals("text/event-stream", request.headers["Accept"])
        assertEquals("application/json", request.headers["Content-Type"])
    }

    @Test
    fun `bootstrap uses an explicit session id when provided`() = runTest {
        respondWith("")

        client().bootstrapVoiceSession("explicit-session").toList()

        assertTrue(URI(requestSlot.captured.url).query.contains("sessionId=explicit-session"))
    }

    @Test
    fun `bootstrap body carries the livekit-bootstrap discriminator in the chat envelope`() = runTest {
        ConciergeAuthTokenHolder.setProvider(provider = { "should-not-be-sent" })
        respondWith("")

        client().bootstrapVoiceSession().toList()

        val body = JSONObject(String(requestSlot.captured.body, StandardCharsets.UTF_8))
        val event = body.getJSONArray("events").getJSONObject(0)
        val conversation = event.getJSONObject("query").getJSONObject("conversation")
        assertEquals("livekit-bootstrap", conversation.getJSONObject("data").getString("type"))
        assertEquals(1, conversation.getJSONObject("data").length())
        assertFalse(conversation.has("message"))
        assertEquals(listOf("surface1", "surface2"), conversation.getJSONArray("surfaces").let { a -> List(a.length()) { a.getString(it) } })
        assertEquals("in", event.getJSONObject("meta").getJSONObject("consent").getString("state"))
        assertEquals(
            "test-ecid",
            event.getJSONObject("xdm").getJSONObject("identityMap").getJSONArray("ECID").getJSONObject(0).getString("id")
        )
        assertFalse(body.toString().contains("should-not-be-sent"))
    }

    @Test
    fun `bootstrap emits credentials when the session payload arrives`() = runTest {
        respondWith(sse(session("wss://lk.example.com", "jwt")))

        val result = client().bootstrapVoiceSession().toList()

        assertEquals(listOf(VoiceSessionBootstrap("wss://lk.example.com", "jwt")), result)
        verify { connection.close() }
    }

    @Test
    fun `bootstrap emits once when the session payload repeats`() = runTest {
        respondWith(sse(session("wss://first", "t1")) + sse(session("wss://second", "t2")))

        val result = client().bootstrapVoiceSession().toList()

        assertEquals(listOf(VoiceSessionBootstrap("wss://first", "t1")), result)
    }

    @Test
    fun `bootstrap ignores incomplete and other voice payloads`() = runTest {
        respondWith(
            sse(voiceEvent("""{"type": "livekit_session", "token": "only-token"}""")) +
                sse(voiceEvent("""{"type": "done"}""")) +
                sse(session("wss://lk.example.com", "jwt"))
        )

        val result = client().bootstrapVoiceSession().toList()

        assertEquals(listOf(VoiceSessionBootstrap("wss://lk.example.com", "jwt")), result)
    }

    @Test
    fun `bootstrap completes without emission when no voice payload arrives`() = runTest {
        respondWith(
            sse("""{"handle": [{"type": "brand-concierge:conversation", "payload": [{"response": {"message": "Hi"}, "state": "completed"}]}]}""")
        )

        assertTrue(client().bootstrapVoiceSession().toList().isEmpty())
    }

    @Test
    fun `bootstrap throws on HTTP error`() = runTest {
        every { connection.responseCode } returns 500
        every { connection.responseMessage } returns "Server Error"

        try {
            client().bootstrapVoiceSession().toList()
            fail("Expected ConciergeHttpStatusException")
        } catch (e: ConciergeHttpStatusException) {
            assertEquals(500, e.statusCode)
        }
    }

    @Test
    fun `bootstrap throws when the connection cannot be established`() = runTest {
        every { networkService.connectAsync(any(), any()) } answers {
            secondArg<NetworkCallback>().call(null)
        }

        try {
            client().bootstrapVoiceSession().toList()
            fail("Expected IOException")
        } catch (e: IOException) {
            // expected
        }
    }

    @Test
    fun `bootstrap with no surfaces fails without sending a request`() = runTest {
        stateFlow.value = testState.copy(surfaces = emptyList())

        try {
            client().bootstrapVoiceSession().toList()
            fail("Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            // expected
        }
        verify(exactly = 0) { networkService.connectAsync(any(), any()) }
    }

    @Test
    fun `chat endpoint keeps the region segment`() = runTest {
        respondWith("")

        client().chat("hi").toList()

        assertEquals("/brand-concierge/va6/conversations", URI(requestSlot.captured.url).path)
    }

    @Test
    fun `default conversation service has no voice support`() = runTest {
        val service = object : ConversationService {
            override fun chat(message: String, xdmFields: Map<String, Any>, sessionId: String?) = throw UnsupportedOperationException()
            override fun sendDataHandoff(routingHint: String, xdmFields: Map<String, Any>, sessionId: String?) =
                throw UnsupportedOperationException()
            override suspend fun sendFeedback(feedback: com.adobe.marketing.mobile.concierge.ui.state.Feedback) = false
            override fun cleanup() = Unit
        }

        assertTrue(service.bootstrapVoiceSession().toList().isEmpty())
    }

    private fun client() = ConciergeConversationServiceClient(stateRepository, sessionManager)

    private fun respondWith(body: String) {
        every { connection.inputStream } answers { ByteArrayInputStream(body.toByteArray(StandardCharsets.UTF_8)) }
    }

    private fun sse(json: String): String = json.lines().joinToString("\n") { "data: $it" } + "\n\n"

    private fun session(url: String, token: String) =
        voiceEvent("""{"type": "livekit_session", "livekitUrl": "$url", "token": "$token"}""")

    private fun voiceEvent(voice: String) =
        """{"handle": [{"type": "brand-concierge:conversation", "payload": [{"response": {"voice": $voice}}]}]}"""
}
