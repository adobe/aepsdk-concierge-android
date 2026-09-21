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

import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Drives [ConciergeConversationSession] directly — no ViewModel, no composable, and no
 * activate/deactivate handshake. Every case here used to run through the ViewModel; they now
 * exercise the process-lifetime owner of the conversation pipeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConciergeConversationSessionTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    private val sessions = mutableListOf<ConciergeConversationSession>()

    @After
    fun tearDown() {
        // Each session owns a processor coroutine, a Channel, and a service; release them rather
        // than leaking one per test.
        sessions.forEach { it.shutdown() }
        sessions.clear()
        Dispatchers.resetMain()
    }

    private fun session(chatClient: ConciergeConversationServiceClient): ConciergeConversationSession {
        every { chatClient.cleanup() } just Runs
        return ConciergeConversationSession(chatService = chatClient, dispatch = null)
            .also { sessions += it }
    }

    @Test
    fun `data handoff renders local message and streamed response before reporting delivery`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("", mapOf("orderId" to "abc-123"), "Order placed")
        val card = MultimodalElement(id = "product-1", content = mapOf("productName" to "Widget"))
        every { chatClient.sendDataHandoff("", handoff.xdmFields) } returns flow {
            emit(
                ParsedConversationMessage(
                    messageContent = "You may also like this.",
                    state = ConversationState.COMPLETED,
                    orderedElements = listOf(
                        ParsedMultimodalItem.Card(card),
                        ParsedMultimodalItem.Cta(NetworkCtaButton("Shop now", "https://example.com"))
                    )
                )
            )
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        val messages = session.messages.value
        assertEquals(4, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals("Order placed", messages[0].text)
        assertEquals("You may also like this.", messages[1].text)
        assertTrue(messages[2].content is MessageContent.Mixed)
        assertTrue(messages[3].content is MessageContent.CtaButton)
        assertEquals(DataHandoffDeliveryResult.Delivered, deliveryResult)
        assertTrue(session.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `data handoff completion can immediately retry after delivery`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        every { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Second response", ConversationState.COMPLETED))
        }
        val session = session(chatClient)
        var firstResult: DataHandoffDeliveryResult? = null
        var secondResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(firstHandoff) { result ->
            firstResult = result
            session.enqueueDataHandoff(secondHandoff) { secondResult = it }
        }
        advanceUntilIdle()

        assertEquals(DataHandoffDeliveryResult.Delivered, firstResult)
        assertEquals(DataHandoffDeliveryResult.Delivered, secondResult)
        verify(exactly = 1) { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) }
        verify(exactly = 1) { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) }
    }

    @Test
    fun `data handoff reports empty response after removing its assistant placeholder`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow { }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        assertEquals(listOf("Order placed"), session.messages.value.map { it.text })
        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.EMPTY_RESPONSE),
            deliveryResult
        )
    }

    @Test
    fun `data handoff failure does not render an error bubble in chat`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        // localMessage renders immediately; the forward then fails mid-stream.
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            throw IllegalStateException("service failure")
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        // Only the local message remains - no generic error bubble, no leftover placeholder.
        assertEquals(listOf("Order placed"), session.messages.value.map { it.text })
        assertEquals(ChatScreenState.Idle, session.state.value)
    }

    @Test
    fun `data handoff reports delivery timeout when its active stream does not finish`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()
        advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
        runCurrent()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
            deliveryResult
        )
        advanceUntilIdle()
        verify(exactly = 1) { chatClient.sendDataHandoff("checkout", handoff.xdmFields) }
        // No localMessage was set and the forward timed out, so the transcript stays empty.
        assertTrue(session.messages.value.isEmpty())
        assertEquals(ChatScreenState.Idle, session.state.value)
    }

    @Test
    fun `data handoff reports chat in progress while a chat turn is active`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstTurnCanFinish = CompletableDeferred<Unit>()
        val callOrder = mutableListOf<String>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.chat("First") } returns flow {
            callOrder += "chat"
            emit(ParsedConversationMessage("First response", ConversationState.IN_PROGRESS))
            firstTurnCanFinish.await()
            emit(ParsedConversationMessage("", ConversationState.COMPLETED))
        }
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            callOrder += "handoff"
            emit(ParsedConversationMessage("Handoff response", ConversationState.COMPLETED))
        }
        val session = session(chatClient)

        assertTrue(session.sendMessage("First"))
        var deliveryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()

        assertEquals(listOf("chat"), callOrder)
        assertEquals(listOf("First", "First response"), session.messages.value.map { it.text })
        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS),
            deliveryResult
        )
        verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }

        firstTurnCanFinish.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("chat"), callOrder)
    }

    @Test
    fun `send message adds user and streams assistant updates to completion`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Hel", ConversationState.IN_PROGRESS))
            emit(ParsedConversationMessage("lo", ConversationState.IN_PROGRESS))
            // the "completed" message contains the full text
            emit(ParsedConversationMessage("Hello", ConversationState.COMPLETED))
        }
        val session = session(chatClient)

        assertTrue(session.sendMessage("Hi"))
        // Processing is visible synchronously, before the queued turn is drained.
        assertTrue(session.state.value is ChatScreenState.Processing)
        advanceUntilIdle()

        val messages = session.messages.value
        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals("Hi", messages[0].text)
        assertTrue(!messages[1].isFromUser)
        assertEquals("Hello", messages[1].text)
        assertTrue(session.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `chat stream ERROR state shows generic copy and returns Idle`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("oops-raw-server-detail", ConversationState.ERROR))
        }
        val session = session(chatClient)

        assertTrue(session.sendMessage("Hi"))
        advanceUntilIdle()

        val last = session.messages.value.last()
        assertTrue(!last.isFromUser)
        // User sees the generic, themeable message.
        assertEquals("Sorry, I encountered an error. Please try again.", last.text)
        assertTrue(!last.text.contains("oops-raw-server-detail"))
        assertTrue(session.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `data handoff reports chat in progress when a chat message is waiting`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.chat("First") } returns flow {
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.sendMessage("First")
        session.enqueueDataHandoff(handoff) { deliveryResult = it }

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS),
            deliveryResult
        )
        verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }

        advanceUntilIdle()
        verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }
    }

    @Test
    fun `data handoff whose service call throws completes with DELIVERY_FAILED and does not break later requests`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } throws
            IllegalStateException("boom")
        val session = session(chatClient)

        var deliveryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        assertEquals(ChatScreenState.Idle, session.state.value)

        // The shared processor coroutine must still be alive for a later request.
        every { chatClient.chat("hello") } returns flow {
            emit(ParsedConversationMessage("hi", ConversationState.COMPLETED))
        }
        session.sendMessage("hello")
        advanceUntilIdle()

        assertEquals(listOf("hello", "hi"), session.messages.value.map { it.text })
    }

    @Test
    fun `data handoff reports chat in progress while another handoff is active`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoffCanFinish = CompletableDeferred<Unit>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            firstHandoffCanFinish.await()
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        val session = session(chatClient)
        val deliveryResults = mutableListOf<DataHandoffDeliveryResult>()

        session.enqueueDataHandoff(firstHandoff) { deliveryResults += it }
        runCurrent()
        session.enqueueDataHandoff(secondHandoff) { deliveryResults += it }

        assertEquals(
            listOf(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS)),
            deliveryResults
        )
        verify(exactly = 0) { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) }

        firstHandoffCanFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `data handoff slot is free again immediately after a delivery timeout`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        every { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Second response", ConversationState.COMPLETED))
        }
        val session = session(chatClient)

        var firstResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(firstHandoff) { firstResult = it }
        runCurrent()
        advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
        runCurrent()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
            firstResult
        )

        // A retry issued right after the timeout completion must not be spuriously
        // rejected with CHAT_IN_PROGRESS - the reservation must already be released.
        var secondResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(secondHandoff) { secondResult = it }
        advanceUntilIdle()

        assertEquals(DataHandoffDeliveryResult.Delivered, secondResult)
    }

    @Test
    fun `data handoff reports service and timeout failures`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val serviceFailure = ConciergeDataHandoffEvent("service-failure", mapOf("orderId" to "1"))
        val timeout = ConciergeDataHandoffEvent("timeout", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("service-failure", serviceFailure.xdmFields) } returns flow {
            throw IllegalStateException("service failure")
        }
        every { chatClient.sendDataHandoff("timeout", timeout.xdmFields) } returns flow {
            awaitCancellation()
        }
        val session = session(chatClient)
        val deliveryResults = mutableListOf<DataHandoffDeliveryResult>()

        session.enqueueDataHandoff(serviceFailure) { deliveryResults += it }
        advanceUntilIdle()
        session.enqueueDataHandoff(timeout) { deliveryResults += it }
        runCurrent()
        advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
        advanceUntilIdle()

        assertEquals(
            listOf(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT)
            ),
            deliveryResults
        )
    }

    @Test
    fun `data handoff timeout does not render an error bubble in chat`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()
        advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
            deliveryResult
        )
        // No localMessage was set and the forward timed out, so the transcript stays empty.
        assertTrue(session.messages.value.isEmpty())
        assertEquals(ChatScreenState.Idle, session.state.value)
    }

    @Test
    fun `data handoff that streams an error frame reports failure without an error bubble`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        // A mid-stream ERROR frame (not a thrown exception) - the distinct hasError branch.
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("raw-server-detail", ConversationState.ERROR))
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        // Only the local message remains: the ERROR frame renders no bubble and leaves no placeholder.
        assertEquals(listOf("Order placed"), session.messages.value.map { it.text })
        assertEquals(ChatScreenState.Idle, session.state.value)
    }

    @Test
    fun `data handoff failure does not remove a prior turn's assistant message`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Previous answer", ConversationState.COMPLETED))
        }
        // No localMessage; the service call throws synchronously, so streamConversation never runs
        // and no placeholder is created for this turn.
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } throws
            IllegalStateException("boom")
        val session = session(chatClient)

        session.sendMessage("Hi")
        advanceUntilIdle()
        assertEquals(listOf("Hi", "Previous answer"), session.messages.value.map { it.text })

        var deliveryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        // The failed handoff must not delete the previous turn's assistant answer.
        assertEquals(listOf("Hi", "Previous answer"), session.messages.value.map { it.text })
    }

    @Test
    fun `data handoff timeout removes only its own partial bubble, not a prior turn`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Previous answer", ConversationState.COMPLETED))
        }
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        // Streams partial content, then stalls until the delivery timeout cancels it.
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Working on it", ConversationState.IN_PROGRESS))
            awaitCancellation()
        }
        val session = session(chatClient)

        session.sendMessage("Hi")
        advanceUntilIdle()

        var deliveryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()
        // The partial handoff bubble is present mid-stream, on top of the prior turn.
        assertEquals(listOf("Hi", "Previous answer", "Working on it"), session.messages.value.map { it.text })

        advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
            deliveryResult
        )
        // Only the handoff's own partial bubble is removed; the prior turn survives.
        assertEquals(listOf("Hi", "Previous answer"), session.messages.value.map { it.text })
        assertEquals(ChatScreenState.Idle, session.state.value)
    }

    @Test
    fun `handoff is accepted and buffered with no renderer attached, then observable for a later renderer`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("buy_now", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("buy_now", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("You may also like this.", ConversationState.COMPLETED))
        }
        // Nothing observes session.messages yet - the chat is "hidden", and before this change the
        // handoff would have been rejected outright with NO_ACTIVE_SESSION.
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        advanceUntilIdle()

        // Delivered even though no ViewModel or composable ever attached...
        assertEquals(DataHandoffDeliveryResult.Delivered, deliveryResult)
        // ...and buffered in the transcript a later renderer will paint on first collect.
        val buffered = session.messages.value
        assertEquals(listOf("Order placed", "You may also like this."), buffered.map { it.text })
        assertTrue(session.state.value is ChatScreenState.Idle)

        // A renderer attaching afterwards sees the completed turn, not an empty transcript.
        assertEquals(buffered, session.messages.first())
    }

    @Test
    fun `handoff with no surfaces configured fails promptly instead of waiting out the delivery timeout`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("buy_now", mapOf("orderId" to "abc-123"))
        // Mirrors ConciergeConversationServiceClient's own no-surfaces guard, which fails the flow
        // before any network request is made (see its "no surfaces configured" tests).
        every { chatClient.sendDataHandoff("buy_now", handoff.xdmFields) } returns flow {
            throw IllegalStateException("No surfaces configured for the chat experience")
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()

        // Reported without advancing to the delivery timeout: a misconfigured app gets a real
        // reason back immediately rather than a DELIVERY_TIMEOUT minutes later.
        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        // The failed turn leaves nothing behind - no placeholder, no error bubble.
        assertTrue(session.messages.value.isEmpty())
        assertEquals(ChatScreenState.Idle, session.state.value)

        // And the reservation is free again, so a later correctly-configured handoff still works.
        every { chatClient.sendDataHandoff("retry", any()) } returns flow {
            emit(ParsedConversationMessage("Thanks!", ConversationState.COMPLETED))
        }
        var retryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(ConciergeDataHandoffEvent("retry", mapOf("orderId" to "2"))) {
            retryResult = it
        }
        advanceUntilIdle()

        assertEquals(DataHandoffDeliveryResult.Delivered, retryResult)
    }

    @Test
    fun `conversationId follows the backend when a later turn starts a new conversation`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("first") } returns flow {
            emit(
                ParsedConversationMessage(
                    messageContent = "one",
                    state = ConversationState.COMPLETED,
                    conversationId = "conv-A"
                )
            )
        }
        every { chatClient.chat("second") } returns flow {
            emit(
                ParsedConversationMessage(
                    messageContent = "two",
                    state = ConversationState.COMPLETED,
                    conversationId = "conv-B"
                )
            )
        }
        val session = session(chatClient)

        assertTrue(session.sendMessage("first"))
        advanceUntilIdle()
        assertEquals("conv-A", session.conversationId)

        // ConciergeSessionManager rolls its session ID after 30 idle minutes, so the backend can
        // answer the next turn on a brand new conversation. Because this session is process-lived
        // it must follow that, rather than pinning to the first conversationId it ever saw and
        // tagging later tracking events and feedback with a conversation that has already ended.
        assertTrue(session.sendMessage("second"))
        advanceUntilIdle()
        assertEquals("conv-B", session.conversationId)
    }

    @Test
    fun `shutdown releases the queue, the processor, and the conversation service`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val session = session(chatClient)

        session.shutdown()

        verify { chatClient.cleanup() }

        // The request queue is closed, so nothing further can be admitted from either entry point.
        assertTrue(!session.sendMessage("after shutdown"))
        var deliveryResult: DataHandoffDeliveryResult? = null
        session.enqueueDataHandoff(ConciergeDataHandoffEvent("late", mapOf("orderId" to "1"))) {
            deliveryResult = it
        }
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
            deliveryResult
        )
        verify(exactly = 0) { chatClient.chat(any()) }
        verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }
    }

    @Test
    fun `shutdown completes a queued handoff instead of abandoning its caller`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        // Queued, but the processor has not had a turn on Main yet.
        session.enqueueDataHandoff(ConciergeDataHandoffEvent("queued", mapOf("orderId" to "1"))) {
            deliveryResult = it
        }
        session.shutdown()
        advanceUntilIdle()

        // Without draining the queue before cancelling the scope this stays null forever: the
        // processor is dead and the request's timeout job died with it, so the handler never
        // responds and the host's dispatchEventWithResponseCallback hangs.
        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
            deliveryResult
        )
    }

    /**
     * Unlike the queued case above, this needs no dedicated handling in [shutdown]: cancelling the
     * scope unwinds the in-flight request into processDataHandoffRequest's CancellationException
     * branch. This pins that the two paths agree on one reason.
     */
    @Test
    fun `shutdown completes a handoff that is already streaming`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val handoff = ConciergeDataHandoffEvent("inflight", mapOf("orderId" to "1"))
        every { chatClient.sendDataHandoff("inflight", handoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        session.enqueueDataHandoff(handoff) { deliveryResult = it }
        runCurrent()
        verify(exactly = 1) { chatClient.sendDataHandoff("inflight", handoff.xdmFields) }

        session.shutdown()
        advanceUntilIdle()

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
            deliveryResult
        )
    }

    @Test
    fun `shutdown is refused for the process session`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        every { chatClient.chat("still works") } returns flow {
            emit(ParsedConversationMessage("yes", ConversationState.COMPLETED))
        }
        // Same flag the lazy `instance` is built with, without forcing the real singleton (and its
        // real network client) into existence just to assert the guard.
        val processSession = ConciergeConversationSession(
            chatService = chatClient,
            dispatch = null,
            isProcessSession = true
        )

        processSession.shutdown()

        // Refused, so the queue and service are untouched and the session still works. Honouring
        // it would be unrecoverable: `instance` is a lazy val that can never be rebuilt.
        verify(exactly = 0) { chatClient.cleanup() }
        assertTrue(processSession.sendMessage("still works"))
        advanceUntilIdle()
        assertEquals(listOf("still works", "yes"), processSession.messages.value.map { it.text })
    }
}
