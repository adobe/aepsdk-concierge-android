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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeDataHandoffEventTest {

    @Test
    fun `toEventData includes routingHint and xdmFields`() {
        val event = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123", "quantity" to 2)
        )

        val data = event.toEventData()

        assertEquals("buy_now", data[ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT])
        assertEquals(
            mapOf("orderId" to "abc-123", "quantity" to 2),
            data[ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS]
        )
        assertTrue(ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE !in data)
    }

    @Test
    fun `toEventData includes localMessage when present`() {
        val event = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123"),
            localMessage = "Your order is confirmed!"
        )

        val data = event.toEventData()

        assertEquals("Your order is confirmed!", data[ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE])
    }

    @Test
    fun `fromEventData decodes a valid payload`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123")
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals("buy_now", result.result.routingHint)
        assertEquals(mapOf("orderId" to "abc-123"), result.result.xdmFields)
        assertEquals(null, result.result.localMessage)
    }

    @Test
    fun `fromEventData decodes localMessage when present`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123"),
            ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE to "Your order is confirmed!"
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals("Your order is confirmed!", result.result.localMessage)
    }

    @Test
    fun `fromEventData treats a blank or wrong-type localMessage as absent, not a decode failure`() {
        listOf<Any?>("   ", 42, null).forEach { badLocalMessage ->
            val data = mapOf(
                ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
                ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123"),
                ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE to badLocalMessage
            )

            val result = ConciergeDataHandoffEvent.fromEventData(data)

            require(result is DataHandoffDecodeResult.Success) { "expected success for localMessage=$badLocalMessage" }
            assertEquals(null, result.result.localMessage)
        }
    }

    @Test
    fun `fromEventData rejects null event data`() {
        val result = ConciergeDataHandoffEvent.fromEventData(null)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("missing_event_data", result.reason)
    }

    @Test
    fun `fromEventData rejects missing routingHint`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123")
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("missing_routing_hint", result.reason)
    }

    @Test
    fun `fromEventData accepts blank routingHint as an empty service query`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "   ",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123")
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals("", result.result.routingHint)
    }

    @Test
    fun `fromEventData rejects missing xdmFields`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now"
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("missing_xdm_fields", result.reason)
    }

    @Test
    fun `fromEventData rejects empty xdmFields`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to emptyMap<String, Any>()
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("empty_xdm_fields", result.reason)
    }

    @Test
    fun `fromEventData rejects a non-string xdmFields key`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf(42 to "abc-123")
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("invalid_xdm_field_key", result.reason)
    }

    @Test
    fun `fromEventData rejects xdmFields containing the reserved identityMap key`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf(
                "orderId" to "abc-123",
                "identityMap" to mapOf("ECID" to "should-not-be-allowed")
            )
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("reserved_key_collision", result.reason)
    }

    @Test
    fun `fromEventData rejects a non-JSON-safe top-level value`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("callback" to {})
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("invalid_xdm_field_value", result.reason)
    }

    @Test
    fun `fromEventData rejects a null xdmFields value`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to null)
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("invalid_xdm_field_value", result.reason)
    }

    @Test
    fun `fromEventData rejects a non-JSON-safe value nested inside a Map`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf(
                "nested" to mapOf("bad" to {})
            )
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("invalid_xdm_field_value", result.reason)
    }

    @Test
    fun `fromEventData rejects a non-JSON-safe value nested inside a List`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf(
                "items" to listOf("safe", {})
            )
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals("invalid_xdm_field_value", result.reason)
    }

    @Test
    fun `fromEventData accepts fully JSON-safe nested Map and List values`() {
        val xdmFields = mapOf(
            "orderId" to "abc-123",
            "quantity" to 2,
            "price" to 19.99,
            "confirmed" to true,
            "nested" to mapOf("a" to "b", "c" to listOf(1, 2, 3)),
            "items" to listOf("sku-1", "sku-2", mapOf("sku" to "sku-3"))
        )
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to xdmFields
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals(xdmFields, result.result.xdmFields)
    }

    @Test
    fun `fromEventData accepts a nested identityMap key since the reserved-key guard is top-level only`() {
        val xdmFields = mapOf(
            "orderId" to "abc-123",
            "nested" to mapOf("identityMap" to "not-actually-reserved-here")
        )
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to xdmFields
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals(xdmFields, result.result.xdmFields)
    }

    @Test
    fun `fromEventData rejects a non-string routingHint as invalid type, not missing`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to 42,
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("orderId" to "abc-123")
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals(ConciergeConstants.DataHandoff.RejectReason.INVALID_ROUTING_HINT_TYPE, result.reason)
    }

    @Test
    fun `fromEventData rejects a non-Map xdmFields as invalid type, not missing`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to "not-a-map"
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELDS_TYPE, result.reason)
    }

    @Test
    fun `fromEventData rejects NaN and Infinity values as not JSON-safe`() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { badValue ->
            val data = mapOf(
                ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
                ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("price" to badValue)
            )

            val result = ConciergeDataHandoffEvent.fromEventData(data)

            require(result is DataHandoffDecodeResult.Rejected) { "expected rejection for value $badValue" }
            assertEquals(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE, result.reason)
        }
    }

    @Test
    fun `fromEventData accepts a finite Double value`() {
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("price" to 19.99)
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
        assertEquals(19.99, result.result.xdmFields["price"])
    }

    @Test
    fun `fromEventData rejects a self-referential xdmFields value instead of stack overflowing`() {
        val cyclic = HashMap<String, Any>()
        cyclic["self"] = cyclic
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("cyclic" to cyclic)
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Rejected)
        assertEquals(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE, result.reason)
    }

    @Test
    fun `fromEventData accepts values nested well within the depth cap`() {
        var value: Any = "leaf"
        repeat(5) { value = mapOf("nested" to value) }
        val data = mapOf(
            ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
            ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to mapOf("deep" to value)
        )

        val result = ConciergeDataHandoffEvent.fromEventData(data)

        require(result is DataHandoffDecodeResult.Success)
    }
}
