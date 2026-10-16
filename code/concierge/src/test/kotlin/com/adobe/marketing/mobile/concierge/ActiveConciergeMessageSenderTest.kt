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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ActiveConciergeMessageSenderTest {

    private val registered = mutableListOf<ConciergeMessageSender>()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.warning(any(), any(), any()) } returns Unit
        every { Log.debug(any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        registered.forEach { ActiveConciergeMessageSender.unregister(it) }
        unmockkStatic(Log::class)
    }

    private fun register(sender: ConciergeMessageSender): ConciergeMessageSender {
        registered += sender
        ActiveConciergeMessageSender.register(sender)
        return sender
    }

    private fun send(message: String): Pair<Boolean, ConciergeSendMessageRejectReason?> {
        var result: Pair<Boolean, ConciergeSendMessageRejectReason?>? = null
        ActiveConciergeMessageSender.send(message) { accepted, reason -> result = accepted to reason }
        return result!!
    }

    @Test
    fun `send reports no active session when no chat host is registered`() {
        assertEquals(false to ConciergeSendMessageRejectReason.NO_ACTIVE_SESSION, send("hi"))
    }

    @Test
    fun `send rejects blank message without reaching the sender`() {
        var called = false
        register { called = true; null }
        assertEquals(false to ConciergeSendMessageRejectReason.EMPTY_MESSAGE, send("   "))
        assertFalse(called)
    }

    @Test
    fun `send rejects oversized message`() {
        register { null }
        val tooLong = "a".repeat(ConciergeConstants.SendMessage.MAX_MESSAGE_LENGTH + 1)
        assertEquals(false to ConciergeSendMessageRejectReason.MESSAGE_TOO_LONG, send(tooLong))
    }

    @Test
    fun `send accepts at max length and forwards the exact message`() {
        var forwarded: String? = null
        register { forwarded = it; null }
        val atMax = "a".repeat(ConciergeConstants.SendMessage.MAX_MESSAGE_LENGTH)
        val (accepted, reason) = send(atMax)
        assertTrue(accepted)
        assertNull(reason)
        assertEquals(atMax, forwarded)
    }

    @Test
    fun `send reports the sender's reject reason`() {
        register { ConciergeSendMessageRejectReason.CHAT_IN_PROGRESS }
        assertEquals(false to ConciergeSendMessageRejectReason.CHAT_IN_PROGRESS, send("hi"))
    }

    @Test
    fun `send maps a throwing sender to delivery failed`() {
        register { throw IllegalStateException("boom") }
        assertEquals(false to ConciergeSendMessageRejectReason.DELIVERY_FAILED, send("hi"))
    }

    @Test
    fun `most recently registered sender wins and unregistering a stale sender is a no-op`() {
        var firstCalls = 0
        var secondCalls = 0
        val first = register { firstCalls++; null }
        register { secondCalls++; null }
        ActiveConciergeMessageSender.unregister(first)

        assertTrue(send("hi").first)
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun `send with null completion does not throw`() {
        register { null }
        ActiveConciergeMessageSender.send("hi", null)
    }
}
