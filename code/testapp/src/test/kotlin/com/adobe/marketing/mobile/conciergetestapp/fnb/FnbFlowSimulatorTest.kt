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

import com.adobe.marketing.mobile.conciergetestapp.FnbFlowSimulator
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CustomizeLogic
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererIds
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Happy path through the same pipeline as the debug E2E screen: tapin2-shaped menu elements ->
 * widget cart -> user-turn text -> simulated BC/tapin2 -> fnb.cart element (tapin2 order) -> cart model.
 */
class FnbFlowSimulatorTest {

    private val catalog: MenuUiModel =
        CatalogMenuMapper.map(stageProducts(), MenuOptions(instructions = MenuOptions.NOTES_ENABLED))!!
    private val items = catalog.categories.flatMap { it.items }.associateBy { it.name }
    private val simulator = FnbFlowSimulator(catalog, guidFactory = { "68f4aac9-1b2b-49ed-ac78-92d7f26feb00" })
    private var submits = 0

    private fun submitTurn(lines: List<CartLine>, submitId: String = "submit-${submits++}") = FnbPromptFormatter.format(
        FnbAction.SubmitCart(submitId, catalog.venueId, catalog.eventId, catalog.locationId, catalog.locationName, lines)
    )

    private fun cartOf(result: FnbFlowSimulator.AddResult): CartSummaryUiModel {
        assertEquals(FnbRendererIds.CART, result.cartElement.rendererId)
        return CartSummaryMapper.map(result.cartElement.payload, CartOptions.STAGE, result.cartElement.entityId)!!
    }

    private fun sodaLine(flavor: String, quantity: Int = 1, note: String? = null): CartLine {
        val soda = items.getValue("Fountain Soda")
        val group = soda.optionGroups.single()
        val selections = mapOf(group.id to CustomizeLogic.toggle(group, emptySet(), group.options.first { it.label == flavor }.id))
        return CartReducer.add(Cart(), soda, CustomizeLogic.toSelectedOptions(soda, selections), quantity, note).lines.single()
    }

    private val nachos: CartLine get() = CartReducer.quickAdd(Cart(), items.getValue("Veggie Nachos"))!!.lines.single()

    @Test
    fun `first add forwards the cart_add body and returns a server-priced tapin2 order`() {
        val result = simulator.submit(submitTurn(listOf(nachos.copy(unitPriceCents = 1), sodaLine("Coke", note = "light ice"))))

        val request = result.cartAddRequest
        assertEquals("venue/event come from the session", 1000010528L, request.getLong("venueId"))
        assertEquals(36747L, request.getLong("eventId"))
        assertEquals(JSONObject.NULL, request.get("orderId"))
        assertEquals(1, request.getInt("deliveryMethod"))
        assertFalse("submitId is stripped before tapin2", request.has("submitId"))
        val soda = request.getJSONArray("products").getJSONObject(1)
        assertEquals("light ice", soda.getString("note"))
        assertEquals(1364015L, soda.getJSONObject("product").getLong("Id"))

        val cart = cartOf(result)
        assertEquals("695685", cart.orderId)
        assertEquals("685", cart.orderCode)
        assertEquals("Golden 1 Concierge", cart.venueName)
        assertEquals(listOf("Veggie Nachos", "Fountain Soda"), cart.lines.map { it.title })
        assertEquals(listOf("2035854", "2035855"), cart.lines.map { it.itemId })
        assertEquals("price comes from the menu, not the client", 1400L, cart.lines[0].subtotalCents)
        assertEquals("Coke", cart.lines[1].modifiersSummary)
        assertEquals("light ice", cart.lines[1].note)
        assertEquals("Market Cafe 122", cart.lines[1].locationName)
        assertEquals(1900L, cart.subtotalCents)
        assertEquals("8.5% per line: 1.19 + 0.43", 162L, cart.taxCents)
        assertEquals(2062L, cart.totalCents)
        assertEquals(
            "https://mobile-stg.tapin2.co/Review/Index/1000010528?eventId=36747&orderId=68f4aac9-1b2b-49ed-ac78-92d7f26feb00",
            cart.checkoutUrl
        )
    }

    @Test
    fun `stage nachos x3 reproduce the real cart_add totals`() {
        val cart = cartOf(simulator.submit(submitTurn(listOf(nachos.copy(quantity = 3)))))
        assertEquals(4200L, cart.subtotalCents)
        assertEquals(357L, cart.taxCents)
        assertEquals(4557L, cart.totalCents)
    }

    @Test
    fun `later adds reuse the order, merge identical lines, and keep different modifiers separate`() {
        simulator.submit(submitTurn(listOf(sodaLine("Coke"))))
        val second = simulator.submit(submitTurn(listOf(sodaLine("Coke", quantity = 2), sodaLine("Sprite"))))

        assertEquals(695685L, second.cartAddRequest.getLong("orderId"))
        assertEquals(listOf("Coke" to 3, "Sprite" to 1), cartOf(second).lines.map { it.modifiersSummary to it.quantity })
    }

    @Test
    fun `a replayed submitId is not added twice`() {
        simulator.submit(submitTurn(listOf(sodaLine("Coke")), submitId = "same"))
        val replay = simulator.submit(submitTurn(listOf(sodaLine("Coke")), submitId = "same"))
        assertTrue(replay.duplicate)
        assertEquals(1, cartOf(replay).lines.single().quantity)
    }

    @Test
    fun `remove turn drops the line and the next cart element reflects it`() {
        val cart = cartOf(simulator.submit(submitTurn(listOf(sodaLine("Coke"), sodaLine("Sprite")))))
        val removeTurn = FnbPromptFormatter.formatRemove(FnbAction.RemoveCartItem(cart.orderId, cart.lines[0].itemId, cart.lines[0].title))
        assertEquals("Fountain Soda", simulator.remove(removeTurn))
        assertNull("already removed", simulator.remove(removeTurn))
        assertNull("wrong order", simulator.remove(FnbPromptFormatter.formatRemove(FnbAction.RemoveCartItem("1", cart.lines[1].itemId, ""))))
        val updated = CartSummaryMapper.map(simulator.cartElement().payload, CartOptions.STAGE)!!
        assertEquals(listOf("Sprite"), updated.lines.map { it.modifiersSummary })
        assertEquals(500L, updated.subtotalCents)
    }
}
