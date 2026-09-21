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
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConciergeDataHandoffEventHandlerTest {

    private lateinit var handler: ConciergeDataHandoffEventHandler

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        mockkStatic(MobileCore::class)
        every { MobileCore.dispatchEvent(any()) } returns Unit
        handler = ConciergeDataHandoffEventHandler(forwarder = SuccessfulDataHandoffForwarder)
    }

    private object SuccessfulDataHandoffForwarder : ConciergeDataHandoffForwarder {
        override fun forward(
            result: ConciergeDataHandoffEvent,
            completion: (DataHandoffDeliveryResult) -> Unit
        ) {
            completion(DataHandoffDeliveryResult.Delivered)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(MobileCore::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `data handoff event flows through the handler and the conversation session to a correlated accepted response`() = runTest {
        // Exercises the production chain end-to-end rather than each link in isolation:
        // ConciergeDataHandoffEventHandler.handle -> SessionDataHandoffForwarder ->
        // ConciergeConversationSession.enqueueDataHandoff -> streamed delivery -> correlated
        // response. Only the session resolution is swapped; every link is the real one.
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoffXdm = mapOf("orderId" to "abc-123")
        every { chatClient.sendDataHandoff("buy_now", handoffXdm) } returns flow {
            emit(ParsedConversationMessage("Thanks for your order!", ConversationState.COMPLETED))
        }
        every { chatClient.cleanup() } just Runs
        val session = ConciergeConversationSession(chatService = chatClient, dispatch = null)
        val dispatchedResponses = mutableListOf<Event>()
        every { MobileCore.dispatchEvent(capture(dispatchedResponses)) } returns Unit

        val requestEvent = buildDataHandoffEvent(routingHint = "buy_now", xdmFields = handoffXdm)
        ConciergeDataHandoffEventHandler(
            forwarder = SessionDataHandoffForwarder { session }
        ).handle(requestEvent)
        advanceUntilIdle()

        verify(exactly = 1) { chatClient.sendDataHandoff("buy_now", handoffXdm) }
        val response = dispatchedResponses.single()
        assertEquals(true, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        // The response must correlate back to the request, or the caller's
        // dispatchEventWithResponseCallback never fires.
        assertEquals(requestEvent.uniqueIdentifier, response.responseID)

        session.shutdown()
    }

    private fun buildDataHandoffEvent(
        routingHint: String = "buy_now",
        xdmFields: Map<String, Any> = mapOf("orderId" to "abc-123")
    ): Event {
        return Event.Builder(
            ConciergeConstants.DataHandoff.EventName.REQUEST,
            ConciergeConstants.EventType.CONCIERGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(
            mapOf(
                ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to routingHint,
                ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to xdmFields
            )
        ).build()
    }

    @Test
    fun `handle dispatches rejected response for malformed payload`() {
        val event = Event.Builder(
            ConciergeConstants.DataHandoff.EventName.REQUEST,
            ConciergeConstants.EventType.CONCIERGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(emptyMap()).build()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify { MobileCore.dispatchEvent(capture(slots)) }

        val response = slots.single()
        assertEquals(false, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals("missing_routing_hint", response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON))
    }

    @Test
    fun `handle dispatches accepted response after successful delivery`() {
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }

        val response = slots.single()
        assertEquals(true, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals(EventSource.RESPONSE_CONTENT, response.source)
        assertEquals(ConciergeConstants.DataHandoff.EventName.RESPONSE, response.name)
    }

    @Test
    fun `handle correlates the response back to the triggering event`() {
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }

        // Without inResponseToEvent correlation the caller's dispatchEventWithResponseCallback
        // never fires, and the extension could re-ingest its own response.
        assertEquals(event.uniqueIdentifier, slots.single().responseID)
    }

    @Test
    fun `handle correlates a rejected response back to the triggering event`() {
        val event = Event.Builder(
            ConciergeConstants.DataHandoff.EventName.REQUEST,
            ConciergeConstants.EventType.CONCIERGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(emptyMap()).build()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }

        assertEquals(event.uniqueIdentifier, slots.single().responseID)
    }

    @Test
    fun `handle forwards the decoded payload to the forwarder on accept`() {
        var forwarded: ConciergeDataHandoffEvent? = null
        val recordingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                forwarded = result
                completion(DataHandoffDeliveryResult.Delivered)
            }
        }
        val recordingHandler = ConciergeDataHandoffEventHandler(forwarder = recordingForwarder)
        val xdmFields = mapOf("orderId" to "abc-123")
        val event = buildDataHandoffEvent(routingHint = "buy_now", xdmFields = xdmFields)

        recordingHandler.handle(event)

        assertEquals("buy_now", forwarded?.routingHint)
        assertEquals(xdmFields, forwarded?.xdmFields)
    }

    @Test
    fun `handle waits for delivery before dispatching a response`() {
        var deliveryCompletion: ((DataHandoffDeliveryResult) -> Unit)? = null
        val delayedForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                deliveryCompletion = completion
            }
        }
        val delayedHandler = ConciergeDataHandoffEventHandler(forwarder = delayedForwarder)

        delayedHandler.handle(buildDataHandoffEvent())

        verify(exactly = 0) { MobileCore.dispatchEvent(any()) }
        deliveryCompletion?.invoke(DataHandoffDeliveryResult.Delivered)

        val slots = mutableListOf<Event>()
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }
        assertEquals(true, slots.single().eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
    }

    @Test
    fun `handle dispatches the typed delivery failure returned by the forwarder`() {
        val failingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                completion(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.EMPTY_RESPONSE))
            }
        }
        val failingHandler = ConciergeDataHandoffEventHandler(forwarder = failingForwarder)

        failingHandler.handle(buildDataHandoffEvent())

        val slots = mutableListOf<Event>()
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }
        val response = slots.single()
        assertEquals(false, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals(
            ConciergeDataHandoffRejectReason.EMPTY_RESPONSE.rawValue,
            response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON)
        )
    }

    @Test
    fun `handle dispatches chat in progress returned by the forwarder`() {
        val busyForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                completion(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS))
            }
        }
        val busyHandler = ConciergeDataHandoffEventHandler(forwarder = busyForwarder)

        busyHandler.handle(buildDataHandoffEvent())

        val slots = mutableListOf<Event>()
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }
        val response = slots.single()
        assertEquals(false, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals(
            ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS.rawValue,
            response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON)
        )
    }

    @Test
    fun `handle does not forward a rejected payload`() {
        var forwardCalled = false
        val recordingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                forwardCalled = true
            }
        }
        val recordingHandler = ConciergeDataHandoffEventHandler(forwarder = recordingForwarder)
        val event = Event.Builder(
            ConciergeConstants.DataHandoff.EventName.REQUEST,
            ConciergeConstants.EventType.CONCIERGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(emptyMap()).build()

        recordingHandler.handle(event)

        assertEquals(false, forwardCalled)
    }

    @Test
    fun `handle reports a delivery failure when the forwarder throws`() {
        val throwingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                throw IllegalStateException("boom")
            }
        }
        val throwingHandler = ConciergeDataHandoffEventHandler(forwarder = throwingForwarder)
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        // Should not throw out of handle() — the forwarder's exception is caught and logged.
        throwingHandler.handle(event)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }

        val response = slots.single()
        assertEquals(false, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals(
            ConciergeDataHandoffRejectReason.DELIVERY_FAILED.rawValue,
            response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON)
        )
    }
}
