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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleCardRuntimeTest {
    private val showMenu = SimpleCardCta("Menu", "showMenu", mapOf("standId" to "7"), localMessage = "Show me the menu")
    private val stand = SimpleCard(id = "7", title = "Burgers", ctas = listOf(showMenu))
    private val closed = SimpleCard(id = "8", title = "Closed", enabled = false, ctas = listOf(showMenu))
    private val actions = RecordingCardActions()
    private val saved = RecordingSaveState()
    private var ids = 0
    private val runtime = newRuntime(null)

    private fun newRuntime(state: Map<String, Any?>?, sender: CardActionSender = actions) = SimpleCardRuntime(
        SimpleCardList(listOf(stand, closed), showImages = false),
        sender,
        state,
        saved,
        idFactory = { "action-${++ids}" }
    )

    @Test
    fun `an accepted press sends the CTA as an action and selects the card`() {
        runtime.select(stand, showMenu)

        assertEquals(
            mapOf(
                "actionId" to "action-1",
                "intent" to "showMenu",
                "parameters" to mapOf("standId" to "7"),
                "message" to "Show me the menu"
            ),
            actions.actions.single()
        )
        assertEquals("7", runtime.selectedCardId.value)
        assertNull(runtime.pendingPress.value)
        assertNull(runtime.error.value)
    }

    @Test
    fun `presses are ignored on a disabled card and while another press is in flight`() {
        actions.deferred = true
        runtime.select(closed, showMenu)
        assertTrue(actions.actions.isEmpty())

        runtime.select(stand, showMenu)
        assertEquals(runtime.pressKey(stand, showMenu), runtime.pendingPress.value)
        runtime.select(stand, showMenu)
        assertEquals(1, actions.actions.size)

        actions.complete(CardActionOutcome.ACCEPTED)
        assertNull(runtime.pendingPress.value)
    }

    @Test
    fun `each rejection shows its own message and a retry reuses the action id`() {
        val expected = mapOf(
            CardActionOutcome.BUSY to SimpleCardRuntime.MESSAGE_BUSY,
            CardActionOutcome.UNAVAILABLE to SimpleCardRuntime.MESSAGE_UNAVAILABLE,
            CardActionOutcome.INVALID to SimpleCardRuntime.MESSAGE_FAILED
        )
        for ((outcome, message) in expected) {
            actions.result = outcome
            runtime.select(stand, showMenu)
            assertEquals(outcome.name, message, runtime.error.value)
            assertNull(runtime.selectedCardId.value)
        }
        actions.result = CardActionOutcome.ACCEPTED
        runtime.select(stand, showMenu)
        runtime.select(stand, showMenu)

        assertEquals(List(4) { "action-1" } + "action-2", actions.actions.map { it["actionId"] })
        assertNull(runtime.error.value)
    }

    @Test
    fun `an unconfirmed press is never sent again, even after the card is recreated`() {
        val key = runtime.pressKey(stand, showMenu)
        actions.result = CardActionOutcome.UNCONFIRMED
        runtime.select(stand, showMenu)
        assertEquals(SimpleCardRuntime.MESSAGE_UNCONFIRMED, runtime.error.value)
        assertEquals(setOf(key), runtime.unconfirmed.value)

        actions.result = CardActionOutcome.ACCEPTED
        runtime.select(stand, showMenu)
        val recreated = newRuntime(saved.last)
        recreated.select(stand, showMenu)

        assertEquals(1, actions.actions.size)
        assertEquals(setOf(key), recreated.unconfirmed.value)
        assertEquals(SimpleCardRuntime.MESSAGE_UNCONFIRMED, recreated.error.value)
        assertNull(runtime.selectedCardId.value)
    }

    @Test
    fun `an unconfirmed press leaves the card's other buttons usable`() {
        val lotA = SimpleCardCta("Lot A", "reserveParking", mapOf("lot" to "A"))
        val lotB = SimpleCardCta("Lot B", "reserveParking", mapOf("lot" to "B"))
        val card = SimpleCard(id = "event-1", title = "Game night", ctas = listOf(lotA, lotB))

        actions.result = CardActionOutcome.UNCONFIRMED
        runtime.select(card, lotA)
        actions.result = CardActionOutcome.ACCEPTED
        runtime.select(card, lotB)

        assertEquals(listOf("A", "B"), actions.actions.map { (it["parameters"] as Map<*, *>)["lot"] })
        assertEquals(listOf("action-1", "action-2"), actions.actions.map { it["actionId"] })
        assertEquals("event-1", runtime.selectedCardId.value)
        assertEquals(setOf(runtime.pressKey(card, lotA)), runtime.unconfirmed.value)
    }

    @Test
    fun `two buttons sharing an intent keep separate press keys and action ids`() {
        val lotA = SimpleCardCta("Lot A", "reserveParking", mapOf("lot" to "A"))
        val lotB = SimpleCardCta("Lot B", "reserveParking", mapOf("lot" to "B"))
        val card = SimpleCard(id = "event-1", title = "Game night", ctas = listOf(lotA, lotB))
        assertNotEquals(runtime.pressKey(card, lotA), runtime.pressKey(card, lotB))

        actions.result = CardActionOutcome.BUSY
        runtime.select(card, lotA)
        runtime.select(card, lotB)
        runtime.select(card, lotA)

        assertEquals(listOf("action-1", "action-2", "action-1"), actions.actions.map { it["actionId"] })
        assertEquals(listOf("A", "B", "A"), actions.actions.map { (it["parameters"] as Map<*, *>)["lot"] })
    }

    @Test
    fun `a sender that throws frees the buttons but prevents resending an unconfirmed action`() {
        var throwing = true
        val sender = CardActionSender { action, onResult ->
            if (throwing) throw IllegalStateException("boom")
            actions.send(action, onResult)
        }
        val runtime = newRuntime(null, sender)

        runtime.select(stand, showMenu)
        assertNull(runtime.pendingPress.value)
        assertEquals(SimpleCardRuntime.MESSAGE_UNCONFIRMED, runtime.error.value)

        throwing = false
        runtime.select(stand, showMenu)
        newRuntime(saved.last, sender).select(stand, showMenu)
        assertTrue(actions.actions.isEmpty())
        assertNull(runtime.selectedCardId.value)
    }

    @Test
    fun `a recreated card restores the pick and the retry ids`() {
        actions.result = CardActionOutcome.BUSY
        runtime.select(stand, showMenu)

        val recreated = newRuntime(saved.last)
        actions.result = CardActionOutcome.ACCEPTED
        recreated.select(stand, showMenu)
        assertEquals(listOf("action-1", "action-1"), actions.actions.map { it["actionId"] })

        val again = newRuntime(saved.last)
        assertEquals("7", again.selectedCardId.value)
    }

    @Test
    fun `adopt takes only state saved after this instance's own`() {
        val other = newRuntime(null)
        other.select(stand, showMenu)
        val newer = saved.last

        runtime.adopt(newer)
        assertEquals("7", runtime.selectedCardId.value)

        runtime.adopt(mapOf("rev" to 0L, "selectedCardId" to "8"))
        assertEquals("7", runtime.selectedCardId.value)
    }
}
