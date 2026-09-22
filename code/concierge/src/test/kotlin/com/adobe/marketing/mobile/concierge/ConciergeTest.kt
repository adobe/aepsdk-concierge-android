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

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeTest {

    @After
    fun tearDown() {
        Concierge.setAuthTokenProvider(null)
    }

    @Test
    fun `sendDataHandoff dispatches a data handoff event with routingHint and xdmFields`() {
        mockkStatic(MobileCore::class)
        try {
            val eventSlot = slot<Event>()
            every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

            Concierge.sendDataHandoff("buy_now", mapOf("orderId" to "abc-123")) { _, _ -> }

            val event = eventSlot.captured
            assertEquals("buy_now", event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT))
            assertEquals(
                mapOf("orderId" to "abc-123"),
                event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS)
            )
            assertTrue(
                ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE !in (event.eventData ?: emptyMap())
            )
        } finally {
            unmockkStatic(MobileCore::class)
        }
    }

    @Test
    fun `sendDataHandoff with localMessage includes it in the dispatched event`() {
        mockkStatic(MobileCore::class)
        try {
            val eventSlot = slot<Event>()
            every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

            Concierge.sendDataHandoff("buy_now", mapOf("orderId" to "abc-123"), "Thanks!") { _, _ -> }

            assertEquals(
                "Thanks!",
                eventSlot.captured.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE)
            )
        } finally {
            unmockkStatic(MobileCore::class)
        }
    }

    @Test
    fun `sendDataHandoff without a completion still dispatches the event`() {
        mockkStatic(MobileCore::class)
        try {
            val eventSlot = slot<Event>()
            every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

            Concierge.sendDataHandoff("buy_now", mapOf("orderId" to "abc-123"))

            assertEquals("buy_now", eventSlot.captured.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT))
        } finally {
            unmockkStatic(MobileCore::class)
        }
    }

    @Test
    fun `sendDataHandoff omitting routingHint dispatches an empty routing hint`() {
        mockkStatic(MobileCore::class)
        try {
            val eventSlot = slot<Event>()
            every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

            // Callers whose XDM fields alone determine routing can skip the hint entirely.
            Concierge.sendDataHandoff(xdmFields = mapOf("orderId" to "abc-123"))

            val event = eventSlot.captured
            assertEquals("", event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT))
            assertEquals(
                mapOf("orderId" to "abc-123"),
                event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS)
            )
        } finally {
            unmockkStatic(MobileCore::class)
        }
    }

    @Test
    fun `setAuthTokenProvider makes the provider token available to the SDK`() {
        Concierge.setAuthTokenProvider(provider = { "athlete-token" })

        assertEquals("athlete-token", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `setAuthTokenProvider with null clears a previously set provider`() {
        Concierge.setAuthTokenProvider(provider = { "athlete-token" })
        Concierge.setAuthTokenProvider(null)

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `setAuthTokenProvider threads a custom timeoutMillis through to the holder`() {
        Concierge.setAuthTokenProvider(
            provider = {
                Thread.sleep(200)
                "too-late"
            },
            timeoutMillis = 50L
        )

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }
}
