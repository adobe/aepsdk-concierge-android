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

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.MobileCore
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConciergeDataHandoffSenderTest {

    @Before
    fun setup() {
        mockkStatic(MobileCore::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(MobileCore::class)
    }

    private fun captureCallback(): io.mockk.CapturingSlot<AdobeCallbackWithError<Event>> {
        val callbackSlot = slot<AdobeCallbackWithError<Event>>()
        every {
            MobileCore.dispatchEventWithResponseCallback(any(), any(), capture(callbackSlot))
        } returns Unit
        return callbackSlot
    }

    @Test
    fun `send builds a request event with routingHint, xdmFields, and localMessage`() {
        val eventSlot = slot<Event>()
        every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), "Thanks!") { _, _ -> }

        val event = eventSlot.captured
        assertEquals(ConciergeConstants.EventType.CONCIERGE, event.type)
        assertEquals(EventSource.REQUEST_CONTENT, event.source)
        assertEquals("buy_now", event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT))
        assertEquals(
            mapOf("orderId" to "abc-123"),
            event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS)
        )
        assertEquals("Thanks!", event.eventData?.get(ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE))
    }

    @Test
    fun `send omits localMessage from the request event when null`() {
        val eventSlot = slot<Event>()
        every { MobileCore.dispatchEventWithResponseCallback(capture(eventSlot), any(), any()) } returns Unit

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { _, _ -> }

        assertTrue(
            ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE !in
                (eventSlot.captured.eventData ?: emptyMap())
        )
    }

    @Test
    fun `send uses the configured response timeout`() {
        val timeoutSlot = slot<Long>()
        every {
            MobileCore.dispatchEventWithResponseCallback(any(), capture(timeoutSlot), any())
        } returns Unit

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { _, _ -> }

        assertEquals(ConciergeConstants.DataHandoff.RESPONSE_TIMEOUT_MS, timeoutSlot.captured)
    }

    @Test
    fun `send invokes completion with accepted true from the response event`() {
        val callbackSlot = captureCallback()
        var resultAccepted: Boolean? = null
        var resultReason: ConciergeDataHandoffRejectReason? = null

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { accepted, reason ->
            resultAccepted = accepted
            resultReason = reason
        }
        callbackSlot.captured.call(
            Event.Builder("resp", ConciergeConstants.EventType.CONCIERGE, EventSource.RESPONSE_CONTENT)
                .setEventData(mapOf(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to true))
                .build()
        )

        assertEquals(true, resultAccepted)
        assertNull(resultReason)
    }

    @Test
    fun `send invokes completion with chat in progress from the response event`() {
        val callbackSlot = captureCallback()
        var resultAccepted: Boolean? = null
        var resultReason: ConciergeDataHandoffRejectReason? = null

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { accepted, reason ->
            resultAccepted = accepted
            resultReason = reason
        }
        callbackSlot.captured.call(
            Event.Builder("resp", ConciergeConstants.EventType.CONCIERGE, EventSource.RESPONSE_CONTENT)
                .setEventData(
                    mapOf(
                        ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to false,
                        ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON to "chat_in_progress"
                    )
                )
                .build()
        )

        assertEquals(false, resultAccepted)
        assertEquals(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS, resultReason)
    }

    @Test
    fun `send invokes completion with accepted false and NO_RESPONSE reason on dispatch failure`() {
        val callbackSlot = captureCallback()
        var resultAccepted: Boolean? = null
        var resultReason: ConciergeDataHandoffRejectReason? = null

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { accepted, reason ->
            resultAccepted = accepted
            resultReason = reason
        }
        callbackSlot.captured.fail(AdobeError.CALLBACK_TIMEOUT)

        assertEquals(false, resultAccepted)
        assertEquals(ConciergeDataHandoffRejectReason.NO_RESPONSE, resultReason)
    }

    @Test
    fun `send falls back to NO_RESPONSE when the response carries an unrecognized reject reason`() {
        val callbackSlot = captureCallback()
        var resultReason: ConciergeDataHandoffRejectReason? = null

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null) { _, reason ->
            resultReason = reason
        }
        callbackSlot.captured.call(
            Event.Builder("resp", ConciergeConstants.EventType.CONCIERGE, EventSource.RESPONSE_CONTENT)
                .setEventData(
                    mapOf(
                        ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to false,
                        ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON to "some_future_reason"
                    )
                )
                .build()
        )

        assertEquals(ConciergeDataHandoffRejectReason.NO_RESPONSE, resultReason)
    }

    @Test
    fun `send with a null completion does not throw when the response arrives`() {
        val callbackSlot = captureCallback()

        ConciergeDataHandoffSender.send("buy_now", mapOf("orderId" to "abc-123"), null, null)
        callbackSlot.captured.call(
            Event.Builder("resp", ConciergeConstants.EventType.CONCIERGE, EventSource.RESPONSE_CONTENT)
                .setEventData(mapOf(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to true))
                .build()
        )
        callbackSlot.captured.fail(AdobeError.CALLBACK_TIMEOUT)
    }
}
