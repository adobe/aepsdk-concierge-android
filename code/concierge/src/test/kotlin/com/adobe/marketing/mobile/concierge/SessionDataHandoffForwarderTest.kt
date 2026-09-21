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
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Covers the seam between [ConciergeDataHandoffEventHandler] and the conversation session.
 * In production the forwarder resolves [ConciergeConversationSession.instance]; here it is
 * pointed at a session built over a mock client.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionDataHandoffForwarderTest {

    private val sessions = mutableListOf<ConciergeConversationSession>()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
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
    fun `forwards the handoff into the resolved session and reports its delivery result`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("buy_now", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("buy_now", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Thanks for your order!", ConversationState.COMPLETED))
        }
        val session = session(chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        SessionDataHandoffForwarder { session }.forward(handoff) { deliveryResult = it }
        advanceUntilIdle()

        verify(exactly = 1) { chatClient.sendDataHandoff("buy_now", handoff.xdmFields) }
        assertEquals(DataHandoffDeliveryResult.Delivered, deliveryResult)
        assertEquals(
            listOf("Order placed", "Thanks for your order!"),
            session.messages.value.map { it.text }
        )
    }

    @Test
    fun `propagates a rejection from the session back to the caller`() = runTest {
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("busy") } returns flow {
            emit(ParsedConversationMessage("working", ConversationState.COMPLETED))
        }
        val session = session(chatClient)
        // Occupy the shared gate with a chat turn so the handoff is refused.
        session.sendMessage("busy")
        var deliveryResult: DataHandoffDeliveryResult? = null

        SessionDataHandoffForwarder { session }
            .forward(ConciergeDataHandoffEvent("buy_now", mapOf("orderId" to "1"))) {
                deliveryResult = it
            }

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS),
            deliveryResult
        )
        advanceUntilIdle()
    }

    @Test
    fun `resolves the session per call, not at construction`() {
        var resolutions = 0
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val session = session(chatClient)
        val forwarder = SessionDataHandoffForwarder {
            resolutions++
            session
        }

        // Constructing a forwarder must not force a session into existence - ConciergeExtension
        // builds the handler (and therefore the default forwarder) at registration time, long
        // before any handoff arrives and before Dispatchers.Main work is wanted.
        assertEquals(0, resolutions)

        forwarder.forward(ConciergeDataHandoffEvent("buy_now", mapOf("orderId" to "1"))) {}

        assertEquals(1, resolutions)
    }
}
