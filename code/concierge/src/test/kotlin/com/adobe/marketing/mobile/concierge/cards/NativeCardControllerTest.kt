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

package com.adobe.marketing.mobile.concierge.cards

import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.cards.NativeCardController.ActivationResult
import com.adobe.marketing.mobile.concierge.cards.NativeCardController.ContextKeys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NativeCardControllerTest {

    private val menuRenderer: ConciergeCardRenderer = { _, _, _ -> Unit }

    private inner class Harness(scope: TestScope) {
        private var ids = 0
        val controller = NativeCardController(
            scope = scope.backgroundScope,
            mainDispatcher = StandardTestDispatcher(scope.testScheduler),
            cardIdGenerator = { "card-${++ids}" },
            rendererLookup = { type -> menuRenderer.takeIf { type == "acme.menu" || type == "acme.cart" } },
            anyRendererRegistered = { true }
        )

        fun activate(
            element: Map<String, Any?> = mapOf("type" to "acme.menu", "id" to "menu-1", "entity_info" to listOf(1, 2)),
            turnId: String = "turn-1",
            position: Int = 0,
            sessionId: String? = "session-1",
            conversationId: String? = "conversation-1",
            interactionId: String? = "interaction-1"
        ) = controller.activate(
            NativeCardController.ActivationRequest(
                element = element,
                type = element["type"] as String,
                turnId = turnId,
                position = position,
                sessionId = sessionId,
                conversationId = conversationId,
                interactionId = interactionId
            )
        )

        fun card(activation: ActivationResult) =
            requireNotNull(controller.renderableCard((activation as ActivationResult.Activated).cardId))
    }

    @Test
    fun `registered element keeps its full immutable JSON and its renderer`() = runTest {
        val harness = Harness(this)
        val element = mapOf(
            "type" to "acme.menu",
            "id" to "menu-1",
            "entity_info" to listOf(mapOf("product" to mapOf("id" to 1364015, "extra" to null))),
            "custom" to mapOf("deep" to listOf(true, null))
        )

        val card = harness.card(harness.activate(element))

        assertEquals(element, card.element)
        assertSame(menuRenderer, card.renderer)
        assertEquals("card-1", card.cardId)
    }

    @Test
    fun `context carries the card's ids, busy and stale flags, and saved state`() = runTest {
        val harness = Harness(this)
        val card = harness.card(harness.activate())

        assertEquals(
            mapOf(
                ContextKeys.CARD_ID to card.cardId,
                ContextKeys.SESSION_ID to "session-1",
                ContextKeys.CONVERSATION_ID to "conversation-1",
                ContextKeys.INTERACTION_ID to "interaction-1",
                ContextKeys.IS_BUSY to true,
                ContextKeys.IS_STALE to false,
                ContextKeys.STATE to mapOf("status" to "draft")
            ),
            card.rendererContext(isBusy = true, isStale = false, state = mapOf("status" to "draft"))
        )
    }

    @Test
    fun `context omits absent ids and unsaved state`() = runTest {
        val harness = Harness(this)
        val card = harness.card(harness.activate(sessionId = null, conversationId = null, interactionId = null))

        assertEquals(
            setOf(ContextKeys.CARD_ID, ContextKeys.IS_BUSY, ContextKeys.IS_STALE),
            card.rendererContext(isBusy = false, isStale = false, state = null).keys
        )
    }

    @Test
    fun `saveState replaces the card's own state on the main dispatcher`() = runTest {
        val harness = Harness(this)
        val first = harness.card(harness.activate())
        val second = harness.card(harness.activate(mapOf("type" to "acme.cart", "id" to "cart-1"), position = 1))

        first.saveState(mapOf("quantities" to mapOf("1" to 2), "status" to "draft"))
        assertNull(first.state.value)
        runCurrent()
        first.saveState(mapOf("status" to "submitted"))
        runCurrent()

        assertEquals(mapOf("status" to "submitted"), first.state.value)
        assertNull(second.state.value)
    }

    @Test
    fun `invalid saved state is ignored`() = runTest {
        val harness = Harness(this)
        val card = harness.card(harness.activate())
        card.saveState(mapOf("status" to "draft"))
        runCurrent()

        card.saveState(mapOf("bad" to Any()))
        runCurrent()

        assertEquals(mapOf("status" to "draft"), card.state.value)
    }

    @Test
    fun `saveState after the transcript is cleared is ignored`() = runTest {
        val harness = Harness(this)
        val card = harness.card(harness.activate())
        card.saveState(mapOf("status" to "submitted"))

        harness.controller.disposeAll()
        runCurrent()
        card.saveState(mapOf("status" to "draft"))
        runCurrent()

        assertNull(card.state.value)
        assertTrue(card.isStale.value)
        assertNull(harness.controller.renderableCard(card.cardId))
    }

    @Test
    fun `an identity reset marks current and later cards stale until the transcript clears`() = runTest {
        val harness = Harness(this)
        val existing = harness.card(harness.activate())

        harness.controller.markAllStale()
        val later = harness.card(harness.activate(mapOf("type" to "acme.cart", "id" to "cart-1"), turnId = "turn-2"))

        assertTrue(existing.isStale.value)
        assertTrue(later.isStale.value)

        harness.controller.disposeAll()
        val fresh = harness.card(harness.activate(turnId = "turn-3"))
        assertFalse(fresh.isStale.value)
    }

    @Test
    fun `cards from earlier turns stay active`() = runTest {
        val harness = Harness(this)
        val prior = harness.card(harness.activate())

        val next = harness.card(harness.activate(turnId = "turn-2"))

        assertFalse(prior.isStale.value)
        assertFalse(next.isStale.value)
        assertTrue(prior.cardId != next.cardId)
    }

    @Test
    fun `busy is shared by every card of the chat`() = runTest {
        val harness = Harness(this)

        harness.controller.setBusy(true)
        assertTrue(harness.controller.isBusy.value)
        harness.controller.setBusy(false)
        assertFalse(harness.controller.isBusy.value)
    }

    @Test
    fun `an element occurrence activates only once and duplicate ids are rejected`() = runTest {
        val harness = Harness(this)
        val first = harness.activate() as ActivationResult.Activated

        assertEquals(ActivationResult.AlreadyActive(first.cardId), harness.activate())
        assertEquals(ActivationResult.Rejected, harness.activate(position = 1))
        assertEquals(ActivationResult.Rejected, harness.activate(position = 1))
    }

    @Test
    fun `invalid element JSON is rejected without recording a card`() = runTest {
        val harness = Harness(this)
        val bad = mapOf("type" to "acme.menu", "id" to "menu-1", "bad" to Any())

        assertEquals(ActivationResult.Rejected, harness.activate(bad))
        assertEquals(ActivationResult.Rejected, harness.activate(bad))
        assertTrue(harness.activate(position = 1) is ActivationResult.Activated)
    }

    @Test
    fun `unregistered types remain on built-in rendering`() = runTest {
        val harness = Harness(this)

        assertEquals(
            ActivationResult.Rejected,
            harness.activate(mapOf("type" to "productCard", "id" to "p1"))
        )
        assertFalse(harness.controller.hasRenderer("productCard"))
        assertFalse(harness.controller.hasRenderer(null))
    }

    @Test
    fun `rolled-back cards are dropped and their state is discarded`() = runTest {
        val harness = Harness(this)
        val kept = harness.card(harness.activate())
        val dropped = harness.card(harness.activate(mapOf("type" to "acme.cart", "id" to "cart-1"), position = 1))

        harness.controller.disposeAllExcept(setOf(kept.cardId))
        dropped.saveState(mapOf("status" to "draft"))
        runCurrent()

        assertSame(kept, harness.controller.renderableCard(kept.cardId))
        assertNull(harness.controller.renderableCard(dropped.cardId))
        assertNull(dropped.state.value)
    }
}
