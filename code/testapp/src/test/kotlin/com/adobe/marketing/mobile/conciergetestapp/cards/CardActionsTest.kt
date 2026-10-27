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

package com.adobe.marketing.mobile.conciergetestapp.cards

import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardActionOutcomeTest {

    @Test
    fun `handoffs that never started can be retried`() {
        assertEquals(CardActionOutcome.ACCEPTED, CardActionOutcome.ofHandoff(true, null))
        assertEquals(CardActionOutcome.BUSY, CardActionOutcome.ofHandoff(false, ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS))
        assertEquals(CardActionOutcome.UNAVAILABLE, CardActionOutcome.ofHandoff(false, ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
        listOf(
            ConciergeDataHandoffRejectReason.MISSING_EVENT_DATA,
            ConciergeDataHandoffRejectReason.INVALID_ROUTING_HINT_TYPE,
            ConciergeDataHandoffRejectReason.MISSING_XDM_FIELDS,
            ConciergeDataHandoffRejectReason.INVALID_XDM_FIELDS_TYPE,
            ConciergeDataHandoffRejectReason.EMPTY_XDM_FIELDS,
            ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_KEY,
            ConciergeDataHandoffRejectReason.RESERVED_KEY_COLLISION,
            ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_VALUE
        ).forEach { reason ->
            val outcome = CardActionOutcome.ofHandoff(false, reason)
            assertEquals("$reason", CardActionOutcome.INVALID, outcome)
            assertTrue("$reason", outcome.neverStarted)
        }
        assertTrue(CardActionOutcome.BUSY.neverStarted)
        assertTrue(CardActionOutcome.UNAVAILABLE.neverStarted)
    }

    @Test
    fun `handoffs that started without a known outcome are unconfirmed`() {
        listOf(
            ConciergeDataHandoffRejectReason.DELIVERY_FAILED,
            ConciergeDataHandoffRejectReason.EMPTY_RESPONSE,
            ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT,
            ConciergeDataHandoffRejectReason.NO_RESPONSE,
            null
        ).forEach { reason ->
            val outcome = CardActionOutcome.ofHandoff(false, reason)
            assertEquals("$reason", CardActionOutcome.UNCONFIRMED, outcome)
            assertFalse("$reason", outcome.neverStarted)
        }
        assertFalse(CardActionOutcome.ACCEPTED.neverStarted)
    }

    @Test
    fun `every reject reason is classified`() {
        ConciergeDataHandoffRejectReason.values().forEach {
            assertTrue("$it", CardActionOutcome.ofHandoff(false, it) != CardActionOutcome.ACCEPTED)
        }
    }

    @Test
    fun `only known pre-send rejections are retryable, so a newly added reason is unconfirmed`() {
        val retryable = ConciergeDataHandoffRejectReason.values().filter { CardActionOutcome.ofHandoff(false, it).neverStarted }
        assertEquals(
            setOf(
                ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS,
                ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION,
                ConciergeDataHandoffRejectReason.MISSING_EVENT_DATA,
                ConciergeDataHandoffRejectReason.INVALID_ROUTING_HINT_TYPE,
                ConciergeDataHandoffRejectReason.MISSING_XDM_FIELDS,
                ConciergeDataHandoffRejectReason.INVALID_XDM_FIELDS_TYPE,
                ConciergeDataHandoffRejectReason.EMPTY_XDM_FIELDS,
                ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_KEY,
                ConciergeDataHandoffRejectReason.RESERVED_KEY_COLLISION,
                ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_VALUE
            ),
            retryable.toSet()
        )
    }
}

class CardGateTest {

    @Test
    fun `busy blocks sending but not editing, stale blocks both`() {
        assertEquals(CardGate.OPEN, CardGate.from(mapOf("cardId" to "c1")))
        assertTrue(CardGate.OPEN.canSend && CardGate.OPEN.canEdit)

        val busy = CardGate.from(mapOf("isBusy" to true, "isStale" to false))
        assertFalse(busy.canSend)
        assertTrue(busy.canEdit)

        val stale = CardGate.from(mapOf("isBusy" to false, "isStale" to true))
        assertFalse(stale.canSend)
        assertFalse(stale.canEdit)
    }

    @Test
    fun `only boolean true gates`() {
        assertEquals(CardGate.OPEN, CardGate.from(mapOf("isBusy" to "true", "isStale" to 1)))
    }
}

class CardStateTest {

    @Test
    fun `saved state is read from the context when present`() {
        assertNull(savedCardState(mapOf("cardId" to "c1")))
        assertNull(savedCardState(mapOf("state" to "not a map")))
        assertEquals(mapOf("a" to 1), savedCardState(mapOf("state" to mapOf("a" to 1))))
    }

    @Test
    fun `each save carries a higher revision`() {
        val saves = mutableListOf<Map<String, Any?>>()
        val saver = CardStateSaver(null, saves::add)

        saver.save(mapOf("a" to 1))
        saver.save(mapOf("a" to 2))

        assertEquals(listOf(1, 2), saves.map { it["a"] })
        assertEquals(listOf(1L, 2L), saves.map { it["rev"] })
    }

    @Test
    fun `only a newer state is adopted`() {
        val saver = CardStateSaver(mapOf("rev" to 3), {})

        assertFalse(saver.adopt(null))
        assertFalse(saver.adopt(mapOf("rev" to 3)))
        assertFalse(saver.adopt(mapOf("rev" to 2.0)))
        assertTrue(saver.adopt(mapOf("rev" to 4)))
        assertEquals(4L, saver.revision)
    }

    @Test
    fun `string helpers keep only well-typed entries`() {
        assertEquals(mapOf("k" to "v"), stringMap(mapOf("k" to "v", "n" to 1, 2 to "x")))
        assertEquals(emptyMap<String, String>(), stringMap("nope"))
        assertEquals(listOf("a"), stringList(listOf("a", 1, null)))
        assertEquals(emptyList<String>(), stringList(null))
    }
}
