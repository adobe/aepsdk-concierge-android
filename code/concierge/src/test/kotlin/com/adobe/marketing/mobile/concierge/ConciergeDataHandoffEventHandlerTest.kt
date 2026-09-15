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
import com.adobe.marketing.mobile.MobileCore
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
        handler = ConciergeDataHandoffEventHandler(
            forwarder = NotImplementedDataHandoffForwarder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )
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
            "Data Handoff Event",
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.DATA_HANDOFF
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
            "Data Handoff Event",
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.DATA_HANDOFF
        ).setEventData(emptyMap()).build()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify { MobileCore.dispatchEvent(capture(slots)) }

        val response = slots.single()
        assertEquals(false, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
        assertEquals("missing_routing_hint", response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON))
    }

    @Test
    fun `handle dispatches accepted response and a delivery-result event for a valid payload`() {
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify(exactly = 2) { MobileCore.dispatchEvent(capture(slots)) }

        val response = slots[0]
        assertEquals(true, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))

        val deliveryEvent = slots[1]
        assertEquals(ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY, deliveryEvent.source)
        assertEquals(false, deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERED))
        assertEquals(
            ConciergeConstants.DataHandoff.DeliveryErrorCode.NOT_IMPLEMENTED,
            deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERY_ERROR_CODE)
        )
    }

    @Test
    fun `handle echoes routingHint and xdmFields in the delivery-result event`() {
        val xdmFields = mapOf("orderId" to "abc-123", "quantity" to 2)
        val event = buildDataHandoffEvent(routingHint = "buy_now", xdmFields = xdmFields)

        val slots = mutableListOf<Event>()
        handler.handle(event)
        verify(exactly = 2) { MobileCore.dispatchEvent(capture(slots)) }

        val deliveryEvent = slots[1]
        assertEquals("buy_now", deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.ROUTING_HINT))
        assertEquals(xdmFields, deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.XDM_FIELDS))
    }

    @Test
    fun `handle does not block the calling thread on the network forward`() {
        val testScope = TestScope(StandardTestDispatcher())
        val asyncHandler = ConciergeDataHandoffEventHandler(
            forwarder = NotImplementedDataHandoffForwarder,
            coroutineScope = testScope
        )
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        asyncHandler.handle(event)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(slots)) }

        testScope.advanceUntilIdle()
        verify(exactly = 2) { MobileCore.dispatchEvent(capture(slots)) }
    }

    @Test
    fun `handle reports UNKNOWN error code when the forwarder throws synchronously`() {
        val throwingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, onComplete: (Boolean, String?) -> Unit) {
                throw IllegalStateException("boom")
            }
        }
        val throwingHandler = ConciergeDataHandoffEventHandler(
            forwarder = throwingForwarder,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher())
        )
        val event = buildDataHandoffEvent()

        val slots = mutableListOf<Event>()
        throwingHandler.handle(event)
        verify(exactly = 2) { MobileCore.dispatchEvent(capture(slots)) }

        val deliveryEvent = slots[1]
        assertEquals(false, deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERED))
        assertEquals(
            ConciergeConstants.DataHandoff.DeliveryErrorCode.UNKNOWN,
            deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERY_ERROR_CODE)
        )
    }

    @Test
    fun `handle reports TIMEOUT error code when the forwarder never calls onComplete`() {
        val hangingForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, onComplete: (Boolean, String?) -> Unit) {
                // never calls onComplete
            }
        }
        val testScope = TestScope(StandardTestDispatcher())
        val hangingHandler = ConciergeDataHandoffEventHandler(
            forwarder = hangingForwarder,
            coroutineScope = testScope,
            forwardTimeoutMs = 100L
        )
        val event = buildDataHandoffEvent()

        hangingHandler.handle(event)
        testScope.advanceUntilIdle()

        val slots = mutableListOf<Event>()
        verify(exactly = 2) { MobileCore.dispatchEvent(capture(slots)) }

        val deliveryEvent = slots[1]
        assertEquals(false, deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERED))
        assertEquals(
            ConciergeConstants.DataHandoff.DeliveryErrorCode.TIMEOUT,
            deliveryEvent.eventData?.get(ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERY_ERROR_CODE)
        )
    }

    @Test
    fun `handle ignores a forwarder callback that fires after the timeout already resolved`() {
        var lateCallback: ((Boolean, String?) -> Unit)? = null
        val lateForwarder = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, onComplete: (Boolean, String?) -> Unit) {
                lateCallback = onComplete // never invoked synchronously — simulates a real async forward
            }
        }
        val testScope = TestScope(StandardTestDispatcher())
        val lateHandler = ConciergeDataHandoffEventHandler(
            forwarder = lateForwarder,
            coroutineScope = testScope,
            forwardTimeoutMs = 100L
        )
        val event = buildDataHandoffEvent()

        lateHandler.handle(event)
        testScope.advanceUntilIdle() // timeout fires; TIMEOUT delivery event dispatched
        verify(exactly = 2) { MobileCore.dispatchEvent(any()) }

        // The forwarder eventually completes after the handler already moved on.
        lateCallback?.invoke(true, null)
        testScope.advanceUntilIdle()

        // The late completion must be discarded, not delivered a second time.
        verify(exactly = 2) { MobileCore.dispatchEvent(any()) }
    }
}
