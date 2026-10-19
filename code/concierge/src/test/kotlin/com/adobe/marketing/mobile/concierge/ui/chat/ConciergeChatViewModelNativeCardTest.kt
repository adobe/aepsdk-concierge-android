/*
 * Copyright 2026 Adobe. All rights reserved.
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

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.cards.ConciergeCardRenderers
import com.adobe.marketing.mobile.concierge.cards.NativeCardController.ContextKeys
import com.adobe.marketing.mobile.concierge.network.ConversationResponseParser
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConciergeChatViewModelNativeCardTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application

    private class FakeSpeech : SpeechCapturing {
        override fun isAvailable() = true
        override fun setListener(listener: SpeechCaptureListener?) = Unit
        override fun startCapture() = Unit
        override fun endCapture() = Unit
        override fun release() = Unit
    }

    private class ScriptedService(
        private val frame: String,
        private val afterFrame: suspend FlowCollector<ParsedConversationMessage>.() -> Unit
    ) : ConversationService {
        override fun chat(
            message: String,
            xdmFields: Map<String, Any>,
            sessionId: String?
        ): Flow<ParsedConversationMessage> = flow {
            ConversationResponseParser.parseConversationData(frame).forEach { emit(it) }
            afterFrame()
        }

        override fun sendDataHandoff(
            routingHint: String,
            xdmFields: Map<String, Any>,
            sessionId: String?
        ): Flow<ParsedConversationMessage> = chat(routingHint, xdmFields, sessionId)

        override suspend fun sendFeedback(feedback: Feedback) = true
        override fun cleanup() = Unit
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        mockkStatic(ServiceProvider::class)
        every { ServiceProvider.getInstance() } returns mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        ConciergeCardRenderers.clear()
        Dispatchers.resetMain()
    }

    private fun frame(message: String, vararg elements: Map<String, Any?>): String {
        val response = JSONObject().put("message", message)
        if (elements.isNotEmpty()) {
            response.put(
                "multimodalElements",
                JSONObject().put("elements", JSONArray().apply { elements.forEach { put(toJson(it)) } })
            )
        }
        return JSONObject().put(
            "handle",
            JSONArray().put(
                JSONObject()
                    .put("type", "brand-concierge:conversation")
                    .put(
                        "payload",
                        JSONArray().put(
                            JSONObject()
                                .put("state", "completed")
                                .put("conversationId", "conversation-1")
                                .put("interactionId", "interaction-1")
                                .put("response", response)
                        )
                    )
            )
        ).toString()
    }

    private fun toJson(value: Any?): Any = when (value) {
        null -> JSONObject.NULL
        is Map<*, *> -> JSONObject().apply { value.forEach { (key, item) -> put(key.toString(), toJson(item)) } }
        is List<*> -> JSONArray().apply { value.forEach { put(toJson(it)) } }
        else -> value
    }

    private fun viewModel(
        frame: String,
        stateRepository: ConciergeStateRepository = ConciergeStateRepository.instance,
        afterFrame: suspend FlowCollector<ParsedConversationMessage>.() -> Unit = {}
    ): ConciergeChatViewModel {
        val sessions = mockk<ConciergeSessionManager>(relaxed = true)
        every { sessions.currentSessionIdOrNull() } returns "session-1"
        every { sessions.getSessionId() } returns "session-1"
        val vm = ConciergeChatViewModel(
            app,
            FakeSpeech(),
            DefaultImageProvider(),
            ScriptedService(frame, afterFrame),
            stateRepository = stateRepository,
            sessionManager = sessions,
            sessionDispatcher = testDispatcher
        )
        vm.activateDataHandoffSession()
        return vm
    }

    private val menu = mapOf(
        "type" to "acme.menu",
        "id" to "menu-1",
        "entity_info" to listOf(mapOf("product" to mapOf("id" to 1364015, "extra" to null))),
        "custom" to mapOf("nested" to listOf(true, null))
    )

    private fun registerMenu() = Concierge.registerCardRenderer("acme.menu") { _, _, _ -> Unit }

    private fun ConciergeChatViewModel.nativeCardIds() =
        messages.value.mapNotNull { (it.content as? MessageContent.NativeCard)?.cardId }

    @Test
    fun `registered JSON elements are separate transcript rows and preserve complete input`() = runTest {
        registerMenu()
        val vm = viewModel(
            frame(
                "Here you go",
                mapOf("type" to "productCard", "id" to "p1", "entity_info" to mapOf("productName" to "A")),
                menu,
                mapOf("type" to "ctaButton", "id" to "c1", "entity_info" to mapOf("primary" to mapOf("text" to "Go")))
            )
        )

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()

        val messages = vm.messages.value
        assertTrue(messages.any { it.content is MessageContent.Mixed })
        val card = requireNotNull(vm.nativeCards.renderableCard(vm.nativeCardIds().single()))
        assertEquals(menu, card.element)
        assertEquals(3, messages.count { !it.isFromUser })
    }

    @Test
    fun `a typed chat error after a card rolls the turn back and disposes the card`() = runTest {
        registerMenu()
        lateinit var vm: ConciergeChatViewModel
        var cardId: String? = null
        vm = viewModel(frame("Here you go", menu)) {
            cardId = vm.nativeCardIds().single()
            emit(ParsedConversationMessage("boom", ConversationState.ERROR))
        }

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()

        assertNull(vm.nativeCards.renderableCard(requireNotNull(cardId)))
        assertTrue(vm.nativeCardIds().isEmpty())
    }

    @Test
    fun `card context reports the turn's ids and that the chat is idle`() = runTest {
        registerMenu()
        val vm = viewModel(frame("Here you go", menu))

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()

        val card = requireNotNull(vm.nativeCards.renderableCard(vm.nativeCardIds().single()))
        val context = card.rendererContext(vm.nativeCards.isBusy.value, card.isStale.value, card.state.value)
        assertEquals("session-1", context[ContextKeys.SESSION_ID])
        assertEquals("conversation-1", context[ContextKeys.CONVERSATION_ID])
        assertEquals("interaction-1", context[ContextKeys.INTERACTION_ID])
        assertEquals(false, context[ContextKeys.IS_BUSY])
        assertEquals(false, context[ContextKeys.IS_STALE])
    }

    @Test
    fun `elements render natively, else as built-in cards, else not at all`() = runTest {
        registerMenu()
        val vm = viewModel(
            frame(
                "Here you go",
                mapOf("type" to "productCard", "id" to "p1", "entity_info" to mapOf("productName" to "A")),
                mapOf("type" to "horizontalCard", "id" to "parking", "entity_info" to mapOf("productName" to "Garage")),
                mapOf("type" to "acme.other", "id" to "other", "entity_info" to mapOf("productName" to "X")),
                menu,
                mapOf("id" to "legacy", "entity_info" to mapOf("productName" to "B")),
                menu,
                mapOf("type" to "ctaButton", "id" to "c1", "entity_info" to mapOf("primary" to mapOf("text" to "Go", "url" to "https://example.com")))
            )
        )

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()

        val replies = vm.messages.value.filterNot { it.isFromUser }
        val cards = replies.mapNotNull { (it.content as? MessageContent.Mixed)?.multimodalElements }.single()
        assertEquals(listOf("p1", "parking", "legacy"), cards.map { it.id })
        assertEquals(1, vm.nativeCardIds().size)
        assertEquals(1, replies.count { it.content is MessageContent.CtaButton })
        assertEquals(4, replies.size)
    }

    @Test
    fun `an unknown type renders nothing even when no renderer is registered`() = runTest {
        val vm = viewModel(
            frame(
                "Here you go",
                mapOf("type" to "acme.other", "id" to "other", "entity_info" to mapOf("productName" to "X")),
                mapOf("type" to "productCard", "id" to "p1", "entity_info" to mapOf("productName" to "A")),
                mapOf("type" to "horizontalCard", "id" to "parking", "entity_info" to mapOf("productName" to "Garage"))
            )
        )

        vm.processEvent(ChatEvent.SendMessage("other"))
        advanceUntilIdle()

        val cards = vm.messages.value.mapNotNull { (it.content as? MessageContent.Mixed)?.multimodalElements }.flatten()
        assertEquals(listOf("p1", "parking"), cards.map { it.id })
        assertTrue(vm.messages.value.none { it.content is MessageContent.NativeCard })
    }

    @Test
    fun `a response with only unknown types shows no card`() = runTest {
        registerMenu()
        val vm = viewModel(frame("Text only", mapOf("type" to "acme.other", "id" to "other", "entity_info" to mapOf("x" to 1))))

        vm.processEvent(ChatEvent.SendMessage("other"))
        advanceUntilIdle()

        assertTrue(vm.messages.value.none { it.content is MessageContent.NativeCard })
        assertTrue(vm.messages.value.none { !(it.content as? MessageContent.Mixed)?.multimodalElements.isNullOrEmpty() })
        assertEquals(MessageContent.Text("Text only"), vm.messages.value.last().content)
    }

    @Test
    fun `earlier cards stay active and keep their saved state when a later response arrives`() = runTest {
        registerMenu()
        val vm = viewModel(frame("Here you go", menu))

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()
        val first = requireNotNull(vm.nativeCards.renderableCard(vm.nativeCardIds().single()))
        first.saveState(mapOf("status" to "submitted"))
        vm.processEvent(ChatEvent.SendMessage("menu again"))
        advanceUntilIdle()

        val cards = vm.nativeCardIds().map { requireNotNull(vm.nativeCards.renderableCard(it)) }
        assertEquals(2, cards.size)
        assertTrue(cards.none { it.isStale.value })
        assertEquals(mapOf("status" to "submitted"), cards.first().state.value)
    }

    private val resetRequest = Event.Builder("Reset", EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET).build()

    private fun readyRepository() = ConciergeStateRepository(
        ConciergeState(
            experienceCloudId = "old-ecid", configurationReady = true,
            conciergeServer = "https://example.com", conciergeConfigId = "config"
        )
    )

    private fun completeReset(repository: ConciergeStateRepository) {
        val api = mockk<ExtensionApi>()
        every { api.getXDMSharedState(any(), any(), any(), any()) } returns SharedStateResult(
            SharedStateStatus.SET, mapOf("identityMap" to mapOf("ECID" to listOf(mapOf("id" to "new-ecid"))))
        )
        repository.completeIdentityReset(
            api,
            Event.Builder("Complete", EventType.EDGE_IDENTITY, EventSource.RESET_COMPLETE)
                .inResponseToEvent(resetRequest).build()
        )
    }

    @Test
    fun `an identity reset makes live cards stale and busy, then drops them and their late saves`() = runTest {
        registerMenu()
        val repository = readyRepository()
        val vm = viewModel(frame("Here you go", menu), repository)
        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()
        val cardId = vm.nativeCardIds().single()
        val card = requireNotNull(vm.nativeCards.renderableCard(cardId))
        card.saveState(mapOf("status" to "draft"))
        runCurrent()

        repository.beginIdentityReset(resetRequest) { }
        runCurrent()
        assertTrue(card.isStale.value)
        assertTrue(vm.nativeCards.isBusy.value)
        assertEquals(true, card.rendererContext(vm.nativeCards.isBusy.value, card.isStale.value, card.state.value)[ContextKeys.IS_STALE])

        completeReset(repository)
        advanceUntilIdle()
        assertTrue(vm.nativeCardIds().isEmpty())
        assertNull(vm.nativeCards.renderableCard(cardId))
        assertFalse(vm.nativeCards.isBusy.value)

        card.saveState(mapOf("status" to "late"))
        advanceUntilIdle()
        assertEquals(mapOf("status" to "draft"), card.state.value)
        assertTrue(card.isStale.value)
    }

    @Test
    fun `cards after an identity reset start fresh`() = runTest {
        registerMenu()
        val repository = readyRepository()
        val vm = viewModel(frame("Here you go", menu), repository)
        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()
        repository.beginIdentityReset(resetRequest) { }
        runCurrent()
        completeReset(repository)
        advanceUntilIdle()

        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()

        val card = requireNotNull(vm.nativeCards.renderableCard(vm.nativeCardIds().single()))
        assertFalse(card.isStale.value)
        assertNull(card.state.value)
    }

    @Test
    fun `a configuration change re-renders the same live card with its saved state`() = runTest {
        registerMenu()
        val vm = viewModel(frame("Here you go", menu))
        vm.processEvent(ChatEvent.SendMessage("menu"))
        advanceUntilIdle()
        val cardId = vm.nativeCardIds().single()
        val before = requireNotNull(vm.nativeCards.renderableCard(cardId))
        before.saveState(mapOf("status" to "submitted", "rev" to 3))
        advanceUntilIdle()

        // The retained VM outlives the activity; recomposition looks the card up again by id.
        val after = requireNotNull(vm.nativeCards.renderableCard(vm.nativeCardIds().single()))

        assertSame(before, after)
        assertEquals(mapOf("status" to "submitted", "rev" to 3), after.state.value)
        assertFalse(after.isStale.value)
        assertEquals(menu, after.element)
    }
}
