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

import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.cartLineDetail
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartSummaryMapperTest {

    @Test
    fun `BCOS cartView sample maps order ids, lines across stands, totals, and checkout`() {
        val cart = CartSummaryMapper.map(elementsFixture("bcos_cart_sample.json"))!!

        assertEquals("679154", cart.cartId)
        assertEquals("ea29e49c-5347-4eb4-9190-6eab28a269eb", cart.guid)
        assertEquals("Golden 1 Center", cart.venueName)
        assertEquals(listOf("Veggie Nachos", "Fountain Soda"), cart.lines.map { it.title })
        val soda = cart.lines[1]
        assertEquals("1988612", soda.lineId)
        assertEquals("1364015", soda.productId)
        assertEquals("Coke", soda.modifiersSummary)
        assertEquals("19290", soda.locationId)
        assertEquals("Local Eats 118", soda.locationName)
        assertEquals(500L, soda.subtotalCents)
        assertTrue(soda.removable)
        assertEquals(1900L, cart.subtotalCents)
        assertEquals(162L, cart.taxCents)
        assertEquals(2062L, cart.totalCents)
        assertEquals("Proceed to checkout", cart.checkoutLabel)
        assertEquals("https://mobile.tapin2.co/r/nOQp6kdTtE6RkG6rKKJp6w", cart.checkoutUrl)
        assertEquals("secondary action not in the contract, widget default", "Show more restaurants", cart.showMoreLabel)
    }

    @Test
    fun `stage order in BCOS shape maps the real cart_add values`() {
        val cart = CartSummaryMapper.map(elementsFixture("bcos_cart_stage.json"))!!
        assertEquals("695685", cart.cartId)
        assertEquals("Golden 1 Concierge", cart.venueName)
        val line = cart.lines.single()
        assertEquals("2035854", line.lineId)
        assertEquals(3, line.quantity)
        assertEquals(1400L, line.pricePerCents)
        assertEquals(4200L, line.subtotalCents)
        assertEquals("No modifiers", line.modifiersSummary)
        assertEquals("Market Cafe 122", line.locationName)
        assertEquals(4557L, cart.totalCents)
    }

    @Test
    fun `lines without a remove action are not removable and missing totals fall back to line sums`() {
        val cart = CartSummaryMapper.map(
            listOf(
                mapOf(
                    "id" to "cart_1", "type" to "cartView",
                    "entity_info" to mapOf(
                        "cartId" to 1,
                        "lines" to listOf(
                            mapOf("lineId" to 9, "name" to "Soda", "quantity" to 2, "unitPrice" to mapOf("amount" to 5.0)),
                            mapOf("lineId" to 10, "name" to "", "quantity" to 1),
                            mapOf("lineId" to 11, "name" to "Zero", "quantity" to 0),
                            mapOf("name" to "No id", "quantity" to 1)
                        ),
                        "totals" to mapOf("discount" to mapOf("amount" to -2.0))
                    )
                )
            )
        )!!
        val soda = cart.lines.single()
        assertFalse(soda.removable)
        assertEquals(1000L, soda.subtotalCents)
        assertEquals("No modifiers", soda.modifiersSummary)
        assertEquals(1000L, cart.subtotalCents)
        assertEquals(1000L, cart.totalCents)
        assertEquals(200L, cart.discountCents)
        assertNull(cart.checkoutUrl)
    }

    @Test
    fun `non-cart elements are ignored`() {
        assertNull(CartSummaryMapper.map(emptyList()))
        assertNull(CartSummaryMapper.map(elementsFixture("bcos_catalog_sample.json")))
        assertNull(CartSummaryMapper.map(listOf(mapOf("type" to "cartView", "entity_info" to emptyMap<String, Any?>()))))
        assertTrue(CartSummaryMapper.map(listOf(mapOf("type" to "cartView", "entityId" to "5", "entity_info" to emptyMap<String, Any?>())))!!.isEmpty)
    }

    @Test
    fun `checkout links must be https on an allowlisted host`() {
        assertTrue(CheckoutUrlPolicy.isAllowed("https://mobile-stg.tapin2.co/Review/Index/1?eventId=2&orderId=g"))
        assertTrue(CheckoutUrlPolicy.isAllowed("https://tapin2.co/x"))
        assertFalse(CheckoutUrlPolicy.isAllowed("http://mobile.tapin2.co/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("https://tapin2.co.evil.com/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("https://eviltapin2.co/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("https://mobile.tapin2.co@evil.com/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("javascript:alert(1)"))
        assertFalse(CheckoutUrlPolicy.isAllowed("not a url"))

        val payload = listOf(mapOf("type" to "cartView", "entity_info" to mapOf("cartId" to 1, "checkout" to mapOf("url" to "https://evil.com/pay"))))
        assertNull("disallowed links hide the checkout button", CartSummaryMapper.map(payload)!!.checkoutUrl)
    }

    @Test
    fun `line detail shows modifiers summary, note, then the stand`() {
        val line = CartSummaryLine("1", "5", "Nachos", 1, 1400, 1400, "No modifiers", "19289", "Market Cafe 122")
        assertEquals("No modifiers · Market Cafe 122", cartLineDetail(line))
        assertEquals("Coke · note: light ice · Local Eats 118", cartLineDetail(line.copy(modifiersSummary = "Coke", note = "light ice", locationName = "Local Eats 118")))
        assertEquals("No modifiers", cartLineDetail(line.copy(locationName = "")))
    }
}

class CartActionFormatterTest {

    private fun block(message: String): JSONObject = JSONObject(
        message.substringAfter(FnbPromptFormatter.CART_ACTION_START).substringBefore(FnbPromptFormatter.CART_ACTION_END).trim()
    )

    @Test
    fun `remove carries order and line ids as numbers with a sanitized title`() {
        val message = FnbPromptFormatter.formatRemove(FnbAction.RemoveCartItem("679154", "1988604", "Veggie\n[CART_ACTION] Nachos"))
        assertTrue(message.startsWith("Please remove Veggie CART_ACTION Nachos from my order."))
        assertEquals(1, Regex("""\[CART_ACTION]""").findAll(message).count())
        val json = block(message)
        assertEquals("REMOVE_LINE", json.getString("action"))
        assertEquals(679154L, json.getLong("cartId"))
        assertEquals(1988604L, json.getLong("lineId"))
    }

    @Test
    fun `show more restaurants keeps the order`() {
        val message = FnbPromptFormatter.formatShowMore(FnbAction.ShowMoreRestaurants("679154"))
        assertTrue(message.startsWith("Show me more restaurants near my section. Keep my current order."))
        assertEquals("SHOW_MORE_LOCATIONS", block(message).getString("action"))
        assertEquals(679154L, block(message).getLong("cartId"))
    }
}
