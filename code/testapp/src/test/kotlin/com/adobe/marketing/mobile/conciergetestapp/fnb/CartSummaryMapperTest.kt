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

import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbHandoffFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.cartLineDetail
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CartSummaryMapperTest {

    @Test
    fun `raw tapin2 cart_add response maps ids, lines, stand names, server totals, and the Review checkout URL`() {
        val cart = CartSummaryMapper.map(stageOrder(), CartOptions.STAGE)!!
        assertEquals("695685", cart.orderId)
        assertEquals("685", cart.orderCode)
        assertEquals("68f4aac9-1b2b-49ed-ac78-92d7f26feb00", cart.guid)
        assertEquals("Golden 1 Concierge", cart.venueName)
        val line = cart.lines.single()
        assertEquals("2035854", line.itemId)
        assertEquals("1364190", line.productId)
        assertEquals("Veggie Nachos", line.title)
        assertEquals(3, line.quantity)
        assertEquals(1400L, line.pricePerCents)
        assertEquals(4200L, line.subtotalCents)
        assertEquals("No modifiers", line.modifiersSummary)
        assertEquals("Market Cafe 122", line.locationName)
        assertNull(line.note)
        assertTrue(line.removable)
        assertEquals(4200L, cart.subtotalCents)
        assertEquals(357L, cart.taxCents)
        assertEquals(4557L, cart.totalCents)
        assertEquals(
            "https://mobile-stg.tapin2.co/Review/Index/1000010528?eventId=36747&orderId=68f4aac9-1b2b-49ed-ac78-92d7f26feb00",
            cart.checkoutUrl
        )
        assertTrue(CartSummaryMapper.map(stageOrder())!!.checkoutUrl!!.startsWith("https://mobile.tapin2.co/Review/Index/"))
    }

    @Test
    fun `stage checkout uses the fixed Golden 1 venue instead of the order venue`() {
        val cart = CartSummaryMapper.map(stageOrder() + ("venueId" to 999), CartOptions.STAGE)!!

        assertEquals(
            "https://mobile-stg.tapin2.co/Review/Index/1000010528?eventId=36747&orderId=68f4aac9-1b2b-49ed-ac78-92d7f26feb00",
            cart.checkoutUrl
        )
    }

    @Test
    fun `envelope entityId is used when the order omits id`() {
        assertEquals("695685", CartSummaryMapper.map(stageOrder() - "id", entityId = "695685")!!.orderId)
        assertNull(CartSummaryMapper.map(stageOrder() - "id"))
    }

    @Test
    fun `BCOS entityId of literal null is ignored and the order id wins`() {
        assertEquals("695685", CartSummaryMapper.map(stageOrder(), entityId = "null")!!.orderId)
        assertNull(CartSummaryMapper.map(stageOrder() - "id", entityId = "null"))
    }

    @Test
    fun `kings fnbcart element from the BCOS add_to_cart hook maps the raw tapin2 order`() {
        val order = JSONUtils.toMap(JSONObject(resourceText("tapin2_stage_cart_add_single.json")))!!
        val element = mapOf("type" to "kings.fnbcart", "id" to "null", "entityId" to "null", "entity_info" to order)

        val cart = CartSummaryMapper.map(element["entity_info"], CartOptions.STAGE, element["entityId"] as String)!!

        assertEquals("686123", cart.orderId)
        assertEquals("123", cart.orderCode)
        assertEquals("Golden 1 Concierge", cart.venueName)
        val line = cart.lines.single()
        assertEquals("2064019", line.itemId)
        assertEquals("1364190", line.productId)
        assertEquals("Veggie Nachos", line.title)
        assertEquals(1, line.quantity)
        assertEquals(1400L, line.subtotalCents)
        assertEquals("Market Cafe 122", line.locationName)
        assertEquals(1400L, cart.subtotalCents)
        assertEquals(119L, cart.taxCents)
        assertEquals(1519L, cart.totalCents)
        assertFalse(cart.isPaid)
        assertEquals(
            "https://mobile-stg.tapin2.co/Review/Index/1000010528?eventId=36747&orderId=44fb1a62-b899-4658-9289-b569d1357375",
            cart.checkoutUrl
        )
    }

    @Test
    fun `modifier summary comes from modifiers list or the modifier string, and notes are echoed`() {
        fun line(extra: Map<String, Any?>) = CartSummaryMapper.map(
            mapOf("id" to 1, "items" to listOf(mapOf("id" to 9, "product" to mapOf("id" to 5, "title" to "Soda"), "quantity" to 1, "pricePer" to 5.0) + extra))
        )!!.lines.single()

        assertEquals("Coke", line(mapOf("modifier" to "Coke")).modifiersSummary)
        assertEquals("Large, Guacamole", line(mapOf("modifiers" to listOf(mapOf("title" to "Large"), mapOf("name" to "Guacamole")))).modifiersSummary)
        assertEquals("No modifiers", line(mapOf("modifier" to "", "modifiers" to null)).modifiersSummary)
        assertEquals("light ice", line(mapOf("note" to "light ice")).note)
    }

    @Test
    fun `paid orders are not removable, totals fall back to line sums, and malformed lines drop`() {
        val cart = CartSummaryMapper.map(
            mapOf(
                "id" to 1, "isPaidInFull" to true, "discountNet" to -2.0, "tipNet" to 1.5,
                "items" to listOf(
                    mapOf("id" to 9, "product" to mapOf("title" to "Soda"), "quantity" to 2, "pricePer" to 5.0),
                    mapOf("id" to 10, "product" to mapOf("title" to ""), "quantity" to 1),
                    mapOf("id" to 11, "product" to mapOf("title" to "Zero"), "quantity" to 0),
                    mapOf("product" to mapOf("title" to "No id"), "quantity" to 1)
                )
            )
        )!!
        assertTrue(cart.isPaid)
        assertFalse(cart.lines.single().removable)
        assertEquals(1000L, cart.subtotalCents)
        assertEquals(1000L, cart.totalCents)
        assertEquals(200L, cart.discountCents)
        assertEquals(150L, cart.tipCents)
        assertNull("no venueId/eventId/guid, no checkout", cart.checkoutUrl)
    }

    @Test
    fun `checkout URL needs every id, a safe guid, and an allowlisted https host`() {
        assertNull(CartSummaryMapper.checkoutUrl(CartOptions(), "1", "2", null))
        assertNull(CartSummaryMapper.checkoutUrl(CartOptions(), "1", "2", "g&orderId=evil"))
        assertNull(CartSummaryMapper.checkoutUrl(CartOptions(checkoutBaseUrl = "https://evil.com"), "1", "2", "g"))
        assertEquals("https://mobile.tapin2.co/Review/Index/1?eventId=2&orderId=ab-12", CartSummaryMapper.checkoutUrl(CartOptions(), "1", "2", "ab-12"))

        assertTrue(CheckoutUrlPolicy.isAllowed("https://mobile-stg.tapin2.co/Review/Index/1?eventId=2&orderId=g"))
        assertFalse(CheckoutUrlPolicy.isAllowed("http://mobile.tapin2.co/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("https://tapin2.co.evil.com/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("https://mobile.tapin2.co@evil.com/Review"))
        assertFalse(CheckoutUrlPolicy.isAllowed("javascript:alert(1)"))
    }

    @Test
    fun `checkout URL gets no link when an id would alter the path or query`() {
        val injected = listOf(
            "1/../Other", "../1", "2&x=1", "2?a=b", "2#frag", "1%2F..", "1 2", "1\\2", "١٢", "a".repeat(65), ""
        )
        for (bad in injected) {
            assertNull("venueId '$bad'", CartSummaryMapper.checkoutUrl(CartOptions(), bad, "2", "g"))
            assertNull("eventId '$bad'", CartSummaryMapper.checkoutUrl(CartOptions(), "1", bad, "g"))
            assertNull("guid '$bad'", CartSummaryMapper.checkoutUrl(CartOptions(), "1", "2", bad))
        }
        val order = mapOf("id" to 1, "venueId" to "1/../Other", "eventId" to 2, "guid" to "g", "items" to emptyList<Any>())
        assertNull(CartSummaryMapper.map(order)!!.checkoutUrl)
    }

    @Test
    fun `negative order amounts read as missing, only the discount is taken as a magnitude`() {
        val cart = CartSummaryMapper.map(
            mapOf(
                "id" to 1, "subtotalNet" to -10.0, "taxAddedNet" to -1.0, "feeAddedNet" to -0.5,
                "tipNet" to -2.0, "totalNet" to -13.5, "discountNet" to -2.0,
                "items" to listOf(
                    mapOf("id" to 9, "product" to mapOf("title" to "Soda"), "quantity" to 2, "pricePer" to -5.0, "subtotal" to -10.0),
                    mapOf("id" to 10, "product" to mapOf("title" to "Water"), "quantity" to 1, "pricePer" to 3.0)
                )
            )
        )!!
        assertEquals(listOf(0L, 300L), cart.lines.map { it.pricePerCents })
        assertEquals(listOf(0L, 300L), cart.lines.map { it.subtotalCents })
        assertEquals("falls back to the line sum", 300L, cart.subtotalCents)
        assertEquals(300L, cart.totalCents)
        assertEquals(0L, cart.taxCents)
        assertEquals(0L, cart.feesCents)
        assertEquals(0L, cart.tipCents)
        assertEquals(200L, cart.discountCents)
    }

    @Test
    fun `non-order payloads are ignored`() {
        assertNull(CartSummaryMapper.map(null))
        assertNull(CartSummaryMapper.map(stageProducts()))
        assertNull(CartSummaryMapper.map(emptyMap<String, Any?>()))
        assertTrue(CartSummaryMapper.map(emptyMap<String, Any?>(), entityId = "5")!!.isEmpty)
    }

    @Test
    fun `line detail shows modifiers summary, note, then the stand`() {
        val line = CartSummaryLine("1", "5", "Nachos", 1, 1400, 1400, "No modifiers", "19289", "Market Cafe 122")
        assertEquals("No modifiers · Market Cafe 122", cartLineDetail(line))
        assertEquals("Coke · note: light ice · Market Cafe 122", cartLineDetail(line.copy(modifiersSummary = "Coke", note = "light ice")))
        assertEquals("No modifiers", cartLineDetail(line.copy(locationName = "")))
    }
}

class CartActionMessageTest {

    @Test
    fun `remove names the sanitized item and carries no machine data`() {
        val message = FnbHandoffFormatter.removeMessage("Veggie\n[CART_ACTION] Nachos")
        assertEquals("Removing Veggie CART_ACTION Nachos from your order.", message)
        assertEquals("Removing this item from your order.", FnbHandoffFormatter.removeMessage(" "))
    }

    @Test
    fun `show restaurants is a plain line`() {
        assertEquals("Showing restaurants near your section.", FnbHandoffFormatter.SHOW_RESTAURANTS_MESSAGE)
    }
}
