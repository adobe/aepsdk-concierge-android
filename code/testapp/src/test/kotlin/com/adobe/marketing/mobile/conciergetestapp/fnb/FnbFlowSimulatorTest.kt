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
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CustomizeLogic
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Happy path through the same pipeline the debug E2E screen uses. */
class FnbFlowSimulatorTest {

    private val catalog: MenuUiModel = CatalogMenuMapper.map(elementsFixture("bcos_catalog_stage.json"))!!
    private val items = catalog.categories.flatMap { it.items }.associateBy { it.name }
    private val simulator = FnbFlowSimulator(catalog, guidFactory = { "ea29e49c-5347-4eb4-9190-6eab28a269eb" })
    private var submits = 0

    private fun submit(lines: List<CartLine>, submitId: String = "submit-${submits++}") = simulator.submit(
        FnbAction.SubmitCart(submitId, catalog.venueId, catalog.eventId, catalog.locationId, catalog.locationName, lines)
    )

    private fun cartOf(result: FnbFlowSimulator.AddResult) = CartSummaryMapper.map(listOf(result.cartView.asMap()))!!

    private fun sodaLine(flavor: String, quantity: Int = 1, note: String? = null): CartLine {
        val soda = items.getValue("Fountain Soda")
        val group = soda.optionGroups.single()
        val selections = mapOf(group.id to CustomizeLogic.toggle(group, emptySet(), group.options.first { it.label == flavor }.id))
        return CartReducer.add(Cart(), soda, CustomizeLogic.toSelectedOptions(soda, selections), quantity, note).lines.single()
    }

    @Test
    fun `first add creates the order and the cart view is priced by the server`() {
        val nachos = CartReducer.quickAdd(Cart(), items.getValue("Veggie Nachos"))!!.lines.single()
        val result = submit(listOf(nachos.copy(unitPriceCents = 1), sodaLine("Coke", note = "light ice")))

        assertNull("first cart/add has no orderId", result.cartAddRequest["orderId"])
        val cart = cartOf(result)
        assertEquals("679154", cart.cartId)
        assertEquals("ea29e49c-5347-4eb4-9190-6eab28a269eb", cart.guid)
        assertEquals("Golden 1 Center", cart.venueName)
        assertEquals(listOf("Veggie Nachos", "Fountain Soda"), cart.lines.map { it.title })
        assertEquals(listOf("1988604", "1988605"), cart.lines.map { it.lineId })
        assertEquals("price comes from the catalog, not the client", 1400L, cart.lines[0].subtotalCents)
        assertEquals("Coke", cart.lines[1].modifiersSummary)
        assertEquals("light ice", cart.lines[1].note)
        assertEquals("Market Cafe 122", cart.lines[1].locationName)
        assertEquals(1900L, cart.subtotalCents)
        assertEquals(162L, cart.taxCents)
        assertEquals(2062L, cart.totalCents)
        assertTrue(CheckoutUrlPolicy.isAllowed(cart.checkoutUrl!!))
        assertEquals(
            "https://mobile-stg.tapin2.co/Review/Index/1000010528?eventId=36747&orderId=ea29e49c-5347-4eb4-9190-6eab28a269eb",
            cart.checkoutUrl
        )
    }

    @Test
    fun `later adds reuse the order, merge identical lines, and keep different options separate`() {
        submit(listOf(sodaLine("Coke")))
        val second = submit(listOf(sodaLine("Coke", quantity = 2), sodaLine("Sprite")))

        assertEquals("679154", second.cartAddRequest["orderId"])
        val cart = cartOf(second)
        assertEquals(listOf("Coke" to 3, "Sprite" to 1), cart.lines.map { it.modifiersSummary to it.quantity })
        assertEquals(2000L, cart.subtotalCents)
    }

    @Test
    fun `a replayed submitId is not added twice`() {
        submit(listOf(sodaLine("Coke")), submitId = "same")
        val replay = submit(listOf(sodaLine("Coke")), submitId = "same")
        assertTrue(replay.duplicate)
        assertEquals(1, cartOf(replay).lines.single().quantity)
    }

    @Test
    fun `remove drops the line and the next cart view reflects it`() {
        val cart = cartOf(submit(listOf(sodaLine("Coke"), sodaLine("Sprite"))))
        assertEquals("Fountain Soda", simulator.remove(cart.lines[0].lineId))
        assertNull(simulator.remove("unknown"))
        val updated = CartSummaryMapper.map(listOf(simulator.cartView().asMap()))!!
        assertEquals(listOf("Sprite"), updated.lines.map { it.modifiersSummary })
        assertEquals(500L, updated.subtotalCents)
        assertFalse(updated.isEmpty)
    }

    @Test
    fun `cart_add request mirrors the add_to_cart body mapping`() {
        val request = submit(listOf(sodaLine("Coke", note = "no ice"))).cartAddRequest
        assertEquals(1000010528L, request["venueId"])
        assertEquals(36747L, request["eventId"])
        @Suppress("UNCHECKED_CAST")
        val product = (request["products"] as List<Map<String, Any?>>).single()
        assertEquals(19289L, product["locationId"])
        assertEquals("no ice", product["note"])
        @Suppress("UNCHECKED_CAST")
        val inner = product["product"] as Map<String, Any?>
        assertEquals(1364015L, inner["Id"])
    }
}
