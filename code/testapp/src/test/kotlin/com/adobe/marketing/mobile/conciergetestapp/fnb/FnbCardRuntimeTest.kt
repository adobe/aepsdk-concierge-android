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

package com.adobe.marketing.mobile.conciergetestapp.fnb

import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionSender
import com.adobe.marketing.mobile.conciergetestapp.cards.RecordingCardActions
import com.adobe.marketing.mobile.conciergetestapp.cards.RecordingSaveState
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.CartCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.FnbCardTypes
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.MenuCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.SubmitState
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.registerFnbCardRenderers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuCardRuntimeTest {
    private val model = CatalogMenuMapper.map(
        stageProducts(),
        MenuOptions(instructions = MenuOptions.NOTES_ENABLED)
    )!!
    private val nachos = model.categories.flatMap { it.items }.first { it.name == "Veggie Nachos" }
    private val actions = RecordingCardActions()
    private val saved = RecordingSaveState()
    private var ids = 0
    private val runtime = newRuntime(null)

    private fun newRuntime(state: Map<String, Any?>?) = MenuCardRuntime(
        model, actions, state, saved,
        idFactory = { "action-${++ids}" }
    )

    private fun actionXdm(action: Map<String, Any?>): Map<*, *> = action["xdmFields"] as Map<*, *>

    private fun addNachos() = runtime.updateCart(CartReducer.quickAdd(runtime.cart.value, nachos)!!)

    @Test
    fun `submit hands off a local action id, the add-to-cart schema and a plain display line`() {
        addNachos()

        runtime.submit()

        val action = actions.actions.single()
        assertEquals("add-to-cart", action["operation"])
        assertEquals("action-1", action["actionId"])
        val namespace = actionXdm(action)["_sacramentokings"] as Map<*, *>
        val actions = namespace["conciergeAction"] as Map<*, *>
        val context = actions["addToCart"] as Map<*, *>
        assertEquals(setOf("products"), context.keys)
        val product = (context["products"] as List<*>).single() as Map<*, *>
        assertEquals(nachos.id, product["productId"].toString())
        assertEquals(model.locationId, product["locationId"].toString())
        assertEquals(1, product["quantity"])
        assertFalse(product.containsKey("note"))
        assertFalse(actionXdm(action).containsKey("sessionContext"))
        val message = action["message"].toString()
        assertTrue(message, message.startsWith("Adding to your cart") && message.contains("• 1 × Veggie Nachos"))
        assertFalse(message, message.contains('[') || message.contains('{'))
    }

    @Test
    fun `an accepted add clears the draft and closes the menu for good`() {
        addNachos()

        runtime.submit()

        assertEquals(SubmitState.Added, runtime.submitState.value)
        assertTrue(runtime.cart.value.isEmpty)
        assertTrue(runtime.isClosed)
        assertFalse(runtime.isEditable)
        assertEquals(MenuCardRuntime.STATUS_ADDED, saved.last!!["status"])
        val recreated = newRuntime(saved.last)
        assertEquals(SubmitState.Added, recreated.submitState.value)
        assertTrue(recreated.cart.value.isEmpty)
        assertFalse(recreated.isEditable)

        runtime.updateCart(CartReducer.quickAdd(Cart(), nachos)!!)
        runtime.submit()
        assertTrue(runtime.cart.value.isEmpty)
        assertEquals(SubmitState.Added, runtime.submitState.value)
        assertEquals(1, actions.actions.size)
    }

    @Test
    fun `an add leaves current-order context out of its action XDM`() {
        addNachos()

        runtime.submit()

        val namespace = actionXdm(actions.actions.single())["_sacramentokings"] as Map<*, *>
        assertFalse(namespace.containsKey("fnbOrders"))
    }

    @Test
    fun `the in-flight items are saved as submitting before the handoff is sent, and stay locked when recreated`() {
        addNachos()
        val draft = runtime.cart.value
        actions.deferred = true

        runtime.submit()

        assertEquals(MenuCardRuntime.STATUS_SUBMITTING, saved.last!!["status"])
        assertEquals("action-1", saved.last!!["submitId"])
        val recreated = newRuntime(saved.last)
        assertEquals(SubmitState.Submitting, recreated.submitState.value)
        assertEquals(draft, recreated.cart.value)
        assertFalse(recreated.isEditable)
        recreated.submit()
        recreated.updateCart(Cart())
        assertEquals(1, actions.actions.size)
        assertEquals(draft, recreated.cart.value)

        actions.complete(CardActionOutcome.ACCEPTED)
        recreated.adopt(saved.last)
        assertEquals(SubmitState.Added, recreated.submitState.value)
        assertFalse(recreated.isEditable)
    }

    @Test
    fun `a handoff that never started restores the editable draft for retry`() {
        addNachos()
        val draft = runtime.cart.value
        actions.result = CardActionOutcome.BUSY

        runtime.submit()

        assertEquals(draft, runtime.cart.value)
        assertEquals(SubmitState.Failed(MenuCardRuntime.MESSAGE_BUSY), runtime.submitState.value)
        assertTrue(runtime.isEditable)
        assertEquals(MenuCardRuntime.STATUS_DRAFT, saved.last!!["status"])
        assertEquals("action-1", saved.last!!["submitId"])
        assertEquals(draft, newRuntime(saved.last).cart.value)
    }

    @Test
    fun `a sender that throws marks the submission unconfirmed without resubmitting its items`() {
        addNachos()
        actions.throwOnSend = true

        runtime.submit()

        assertTrue(runtime.cart.value.isEmpty)
        assertEquals(SubmitState.Unconfirmed, runtime.submitState.value)
        assertFalse(runtime.isEditable)
        assertEquals(MenuCardRuntime.STATUS_UNCONFIRMED, saved.last!!["status"])
        actions.throwOnSend = false
        runtime.submit()
        newRuntime(saved.last).submit()
        assertEquals(1, actions.actions.size)
    }

    @Test
    fun `a handoff with an unknown outcome drops the draft so it is never resubmitted`() {
        addNachos()
        actions.result = CardActionOutcome.UNCONFIRMED

        runtime.submit()
        runtime.submit()

        assertEquals(1, actions.actions.size)
        assertEquals(SubmitState.Unconfirmed, runtime.submitState.value)
        assertTrue(runtime.cart.value.isEmpty)
        assertTrue("an unknown outcome closes the menu too", runtime.isClosed)
        val recreated = newRuntime(saved.last)
        assertEquals(SubmitState.Unconfirmed, recreated.submitState.value)
        assertTrue(recreated.cart.value.isEmpty)
        recreated.submit()
        assertEquals(1, actions.actions.size)
    }

    @Test
    fun `retrying a rejected submit reuses its action id, also after the card is recreated`() {
        addNachos()
        actions.result = CardActionOutcome.BUSY
        runtime.submit()
        actions.result = CardActionOutcome.ACCEPTED
        newRuntime(saved.last).submit()

        assertEquals(listOf("action-1", "action-1"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `changing the cart after a rejected submit gets a new action id`() {
        addNachos()
        actions.result = CardActionOutcome.INVALID
        runtime.submit()
        addNachos()
        runtime.submit()

        assertEquals(listOf("action-1", "action-2"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `a submit in flight locks the cart and ignores a second press`() {
        addNachos()
        val draft = runtime.cart.value
        actions.deferred = true

        runtime.submit()
        runtime.submit()
        runtime.updateCart(Cart())

        assertEquals(1, actions.actions.size)
        assertEquals(SubmitState.Submitting, runtime.submitState.value)
        assertFalse(runtime.isEditable)
        assertEquals(draft, runtime.cart.value)

        actions.complete(CardActionOutcome.UNAVAILABLE)
        assertEquals(SubmitState.Failed(MenuCardRuntime.MESSAGE_UNAVAILABLE), runtime.submitState.value)
        assertTrue(runtime.isEditable)
    }

    @Test
    fun `cart edits are saved and restored by a recreated card`() {
        addNachos()
        addNachos()

        val recreated = newRuntime(saved.last)

        assertEquals(runtime.cart.value, recreated.cart.value)
        assertEquals(2, recreated.cart.value.quantityFor(nachos.id))
        assertEquals(SubmitState.Idle, recreated.submitState.value)
    }

    @Test
    fun `a recreated card adopts the outcome saved by the instance that sent the order`() {
        addNachos()
        actions.deferred = true
        runtime.submit()
        val recreated = newRuntime(saved.last)

        actions.complete(CardActionOutcome.BUSY)
        recreated.adopt(saved.last)

        assertEquals(runtime.cart.value, recreated.cart.value)
        assertTrue(recreated.isEditable)
    }

    @Test
    fun `an older saved state is not adopted`() {
        addNachos()
        val older = saved.last
        addNachos()

        runtime.adopt(older)

        assertEquals(2, runtime.cart.value.quantityFor(nachos.id))
    }

    @Test
    fun `an empty cart is never submitted`() {
        runtime.submit()

        assertTrue(actions.actions.isEmpty())
        assertEquals(SubmitState.Idle, runtime.submitState.value)
    }
}

class CartCardRuntimeTest {
    private val model = CartSummaryMapper.map(stageOrder(), CartOptions.STAGE, "695685")!!
    private val actions = RecordingCardActions()
    private val saved = RecordingSaveState()
    private var ids = 0
    private val runtime = newRuntime(null)

    private fun newRuntime(state: Map<String, Any?>?) = CartCardRuntime(
        model,
        actions,
        CartOptions.STAGE.allowedCheckoutHosts,
        state,
        saved,
        idFactory = { "action-${++ids}" }
    )

    @Test
    fun `remove and show restaurants use the finalized action operations`() {
        val line = model.lines.first()
        runtime.remove(line)
        runtime.showRestaurants()

        assertEquals("remove-from-cart", actions.actions[0]["operation"])
        assertEquals("action-1", actions.actions[0]["actionId"])
        assertEquals(line.locationId, actions.actions[0]["locationId"])
        assertEquals(line.productId, actions.actions[0]["productId"])
        assertEquals(line.modifierForRemoval, actions.actions[0]["modifier"])
        assertEquals("showRestaurants", actions.actions[1]["operation"])
        assertEquals("action-2", actions.actions[1]["actionId"])
        assertEquals(model.lines.last().locationId, actions.actions[1]["locationId"])
    }

    @Test
    fun `retrying a rejected cart action reuses its action id`() {
        val line = model.lines.first()
        actions.result = CardActionOutcome.BUSY
        runtime.remove(line)
        actions.result = CardActionOutcome.INVALID
        runtime.remove(line)
        actions.result = CardActionOutcome.ACCEPTED
        runtime.remove(line)

        assertEquals(listOf("action-1", "action-1", "action-1"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `an unconfirmed cart action is never sent again, even after the card is recreated`() {
        val line = model.lines.first()
        actions.result = CardActionOutcome.UNCONFIRMED
        runtime.remove(line)
        runtime.showRestaurants()
        actions.result = CardActionOutcome.ACCEPTED
        runtime.remove(line)
        runtime.showRestaurants()

        assertEquals(2, actions.actions.size)
        assertEquals(CartCardRuntime.MESSAGE_UNCONFIRMED, runtime.error.value)
        val keys = setOf(
            CartCardRuntime.removalKey(model.orderId, line.itemId),
            CartCardRuntime.showRestaurantsKey(model.orderId)
        )
        assertEquals(keys, runtime.unconfirmed.value)
        assertTrue(line.itemId !in runtime.pendingRemovals.value)

        val recreated = newRuntime(saved.last)
        assertEquals(keys, recreated.unconfirmed.value)
        recreated.remove(line)
        recreated.showRestaurants()
        assertEquals(2, actions.actions.size)
        assertEquals(CartCardRuntime.MESSAGE_UNCONFIRMED, recreated.error.value)
    }

    @Test
    fun `an unconfirmed removal leaves other lines removable`() {
        @Suppress("UNCHECKED_CAST")
        val order = stageOrder().toMutableMap().apply {
            val item = (this["items"] as List<Map<String, Any?>>).first()
            this["items"] = listOf(item, item + ("id" to 999_001))
        }
        val twoLines = CartSummaryMapper.map(order, CartOptions.STAGE, "695685")!!
        val lines = twoLines.lines
        assertEquals(2, lines.size)
        val runtime = CartCardRuntime(twoLines, actions, CartOptions.STAGE.allowedCheckoutHosts, null, saved, idFactory = { "action-${++ids}" })
        actions.result = CardActionOutcome.UNCONFIRMED
        runtime.remove(lines[0])
        runtime.remove(lines[0])
        actions.result = CardActionOutcome.ACCEPTED
        runtime.remove(lines[1])

        assertEquals(listOf("remove-from-cart", "remove-from-cart"), actions.actions.map { it["operation"] })
        assertEquals(lines[1].productId, actions.actions[1]["productId"])
    }

    @Test
    fun `a recreated card restores pending removals and retry ids`() {
        val line = model.lines.first()
        runtime.remove(line)
        actions.result = CardActionOutcome.BUSY
        runtime.showRestaurants()

        val recreated = newRuntime(saved.last)
        assertEquals(setOf(line.itemId), recreated.pendingRemovals.value)
        recreated.remove(line)
        actions.result = CardActionOutcome.ACCEPTED
        recreated.showRestaurants()

        assertEquals(listOf("action-1", "action-2", "action-2"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `an accepted removal marks the line pending and can't be sent again`() {
        val line = model.lines.first()
        runtime.remove(line)
        runtime.remove(line)

        assertEquals(1, actions.actions.size)
        assertEquals(setOf(line.itemId), runtime.pendingRemovals.value)
    }

    @Test
    fun `a request in flight blocks other cart actions until it resolves`() {
        actions.deferred = true
        runtime.remove(model.lines.first())
        runtime.showRestaurants()
        runtime.checkout()

        assertEquals(1, actions.actions.size)
        assertTrue(runtime.requestInFlight.value)

        actions.complete(CardActionOutcome.BUSY)
        assertFalse(runtime.requestInFlight.value)
        assertEquals(CartCardRuntime.MESSAGE_BUSY, runtime.error.value)
        assertTrue(runtime.pendingRemovals.value.isEmpty())
    }

    @Test
    fun `a sender that throws clears the in-flight request without allowing a duplicate removal`() {
        val line = model.lines.first()
        actions.throwOnSend = true
        runtime.remove(line)

        assertFalse(runtime.requestInFlight.value)
        assertEquals(CartCardRuntime.MESSAGE_UNCONFIRMED, runtime.error.value)
        assertTrue(runtime.pendingRemovals.value.isEmpty())

        actions.throwOnSend = false
        runtime.remove(line)
        newRuntime(saved.last).remove(line)
        assertEquals(listOf("action-1"), actions.actions.map { it["actionId"] })
        assertTrue(runtime.pendingRemovals.value.isEmpty())
    }

    @Test
    fun `a checkout the sender couldn't open reports a checkout error`() {
        actions.result = CardActionOutcome.INVALID
        runtime.checkout()

        assertEquals(CartCardRuntime.MESSAGE_CHECKOUT_FAILED, runtime.error.value)
    }

    @Test
    fun `a paid cart sends no actions`() {
        val paid = CartCardRuntime(
            CartSummaryMapper.map(stageOrder() + ("isPaidInFull" to true), CartOptions.STAGE)!!,
            actions,
            CartOptions.STAGE.allowedCheckoutHosts
        )
        paid.remove(paid.model.lines.first())
        paid.checkout()

        assertTrue(actions.actions.isEmpty())
    }

    @Test
    fun `a checked-out order takes no more changes but checkout can be reopened`() {
        var closed = false
        var checkouts = 0
        val runtime = CartCardRuntime(
            model, actions, CartOptions.STAGE.allowedCheckoutHosts, null, saved,
            idFactory = { "action-${++ids}" },
            isOrderClosed = { closed },
            onCheckedOut = { checkouts++ }
        )

        runtime.checkout()
        assertEquals(1, checkouts)
        closed = true
        runtime.remove(model.lines.first())
        runtime.showRestaurants()
        runtime.checkout()

        assertEquals(listOf("openCheckout", "openCheckout"), actions.actions.map { it["operation"] })
        assertEquals(2, checkouts)
    }

    @Test
    fun `a checkout that couldn't open doesn't count as checked out`() {
        var checkouts = 0
        val runtime = CartCardRuntime(model, actions, CartOptions.STAGE.allowedCheckoutHosts, onCheckedOut = { checkouts++ })
        actions.result = CardActionOutcome.INVALID

        runtime.checkout()

        assertEquals(0, checkouts)
    }

    @Test
    fun `checkout URL is validated before it reaches the action sender`() {
        runtime.checkout()

        assertEquals("openCheckout", actions.actions.single()["operation"])
        assertEquals(model.checkoutUrl, actions.actions.single()["url"])
        assertNull(runtime.error.value)

        val strict = CartCardRuntime(model, actions, emptySet())
        val before = actions.actions.size
        strict.checkout()
        assertEquals(before, actions.actions.size)
        assertEquals(CartCardRuntime.MESSAGE_CHECKOUT_FAILED, strict.error.value)
    }
}

class FnbCardRenderersTest {
    @Test
    fun `registers F&B element types exactly`() {
        val registered = mutableMapOf<String, ConciergeCardRenderer>()

        registerFnbCardRenderers(
            LocationOptions(),
            MenuOptions(),
            CartOptions.STAGE,
            openCheckout = { true },
            register = { type, renderer -> registered[type] = renderer }
        )

        assertEquals(
            setOf(FnbCardTypes.LOCATIONS, FnbCardTypes.MENU, FnbCardTypes.CART),
            registered.keys
        )
        assertTrue(registered.keys.containsAll(setOf("kings.fnblocation", "kings.menu", "kings.fnbcart")))
    }
}
