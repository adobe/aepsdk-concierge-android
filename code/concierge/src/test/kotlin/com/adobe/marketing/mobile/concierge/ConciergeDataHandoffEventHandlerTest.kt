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
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConciergeDataHandoffEventHandlerTest {

    private lateinit var handler: ConciergeDataHandoffEventHandler

    @Before
    fun setup() {
        mockkStatic(MobileCore::class)
        every { MobileCore.dispatchEvent(any()) } returns Unit
        handler = ConciergeDataHandoffEventHandler(forwarder = NotImplementedDataHandoffForwarder)
    }

    @After
    fun tearDown() {
        unmockkStatic(MobileCore::class)
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
    fun `handle dispatches accepted response for a valid payload`() {
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
    fun `handle forwards the decoded payload to the forwarder on accept`() {
        var forwarded: ConciergeDataHandoffEvent? = null
        val recordingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent) {
                forwarded = result
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
    fun `handle does not forward a rejected payload`() {
        var forwardCalled = false
        val recordingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent) {
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
    fun `handle still dispatches the accepted response when the forwarder throws`() {
        val throwingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent) {
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
        assertEquals(true, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
    }
}
