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

import com.adobe.marketing.mobile.services.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ActiveConciergeDataHandoffForwarderTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.warning(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `forward reports no active session when no chat host is registered`() {
        var deliveryResult: DataHandoffDeliveryResult? = null

        ActiveConciergeDataHandoffForwarder.forward(
            ConciergeDataHandoffEvent(
                routingHint = "buy_now",
                xdmFields = mapOf("orderId" to "abc-123")
            )
        ) { deliveryResult = it }

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
            deliveryResult
        )
    }

    @Test
    fun `forward delegates to the registered chat host`() {
        var forwarded: ConciergeDataHandoffEvent? = null
        val host = object : ConciergeDataHandoffForwarder {
            override fun forward(
                result: ConciergeDataHandoffEvent,
                completion: (DataHandoffDeliveryResult) -> Unit
            ) {
                forwarded = result
                completion(DataHandoffDeliveryResult.Delivered)
            }
        }
        val handoff = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123")
        )

        ActiveConciergeDataHandoffForwarder.register(host)
        try {
            var deliveryResult: DataHandoffDeliveryResult? = null
            ActiveConciergeDataHandoffForwarder.forward(handoff) { deliveryResult = it }

            assertEquals(handoff, forwarded)
            assertEquals(DataHandoffDeliveryResult.Delivered, deliveryResult)
        } finally {
            ActiveConciergeDataHandoffForwarder.unregister(host)
        }
    }

    @Test
    fun `register logs a warning and wins over an already-active host`() {
        var firstForwarded: ConciergeDataHandoffEvent? = null
        val firstHost = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, completion: (DataHandoffDeliveryResult) -> Unit) {
                firstForwarded = result
            }
        }
        var secondForwarded: ConciergeDataHandoffEvent? = null
        val secondHost = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, completion: (DataHandoffDeliveryResult) -> Unit) {
                secondForwarded = result
            }
        }
        val handoff = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123")
        )

        ActiveConciergeDataHandoffForwarder.register(firstHost)
        try {
            ActiveConciergeDataHandoffForwarder.register(secondHost)

            ActiveConciergeDataHandoffForwarder.forward(handoff) { }

            assertEquals(handoff, secondForwarded)
            assertEquals(null, firstForwarded)
            verify(exactly = 1) { Log.warning(any(), any(), any()) }
        } finally {
            ActiveConciergeDataHandoffForwarder.unregister(secondHost)
        }
    }

    @Test
    fun `re-registering the same host does not warn`() {
        val host = object : ConciergeDataHandoffForwarder {
            override fun forward(result: ConciergeDataHandoffEvent, completion: (DataHandoffDeliveryResult) -> Unit) = Unit
        }

        ActiveConciergeDataHandoffForwarder.register(host)
        try {
            ActiveConciergeDataHandoffForwarder.register(host)

            verify(exactly = 0) { Log.warning(any(), any(), any()) }
        } finally {
            ActiveConciergeDataHandoffForwarder.unregister(host)
        }
    }
}
