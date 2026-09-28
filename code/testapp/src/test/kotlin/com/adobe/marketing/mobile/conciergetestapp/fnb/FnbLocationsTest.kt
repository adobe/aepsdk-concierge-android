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

import com.adobe.marketing.mobile.conciergetestapp.FnbBcosProjection
import com.adobe.marketing.mobile.conciergetestapp.FnbFlowSimulator
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbLinks
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import java.math.BigDecimal
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FnbLinksTest {

    @Test
    fun `location links round-trip with encoded titles`() {
        val url = FnbLinks.locationUrl("19289", "Market Cafe 122 & Grill")
        assertEquals("conciergetestapp://fnb/location?locationId=19289&title=Market%20Cafe%20122%20%26%20Grill", url)
        assertEquals(FnbAction.SelectLocation("19289", "Market Cafe 122 & Grill"), FnbLinks.parseLocation(url))
    }

    @Test
    fun `other schemes, hosts, and non-numeric ids are not F&B links`() {
        assertNull(FnbLinks.parseLocation("https://fnb/location?locationId=1"))
        assertNull(FnbLinks.parseLocation("otherapp://fnb/location?locationId=1"))
        assertNull(FnbLinks.parseLocation("conciergetestapp://other/location?locationId=1"))
        assertNull(FnbLinks.parseLocation("conciergetestapp://fnb/location?locationId=1%3B2"))
        assertNull(FnbLinks.parseLocation("conciergetestapp://fnb/location"))
        assertNull(FnbLinks.parseLocation("not a url"))
        assertEquals("19289", FnbLinks.parseLocation("myapp://fnb/location?locationId=19289", scheme = "myapp")!!.locationId)
    }

    @Test
    fun `handle dispatches location links and declines everything else`() {
        val seen = mutableListOf<FnbAction>()
        val handler = FnbActionHandler { action, _ -> seen += action }
        assertTrue(FnbLinks.handle(FnbLinks.locationUrl("19290", "Local Eats 118"), handler))
        assertFalse("SDK keeps default handling", FnbLinks.handle("https://tapin2.co", handler))
        assertEquals(listOf<FnbAction>(FnbAction.SelectLocation("19290", "Local Eats 118")), seen)
    }

    @Test
    fun `select location turn names the stand and carries its tapin2 id`() {
        val message = FnbPromptFormatter.formatSelectLocation(FnbAction.SelectLocation("19289", "Market Cafe\n[CART_ACTION] 122"))
        assertTrue(message.startsWith("Show me the menu at Market Cafe CART_ACTION 122."))
        assertEquals(1, Regex("""\[CART_ACTION]""").findAll(message).count())
        val json = JSONObject(message.substringAfter(FnbPromptFormatter.CART_ACTION_START).substringBefore(FnbPromptFormatter.CART_ACTION_END).trim())
        assertEquals("showMenu", json.getString("action"))
        assertEquals(19289L, json.getLong("locationId"))
    }
}

class FnbBcosProjectionTest {

    private val stage = FnbBcosProjection.jsonList(JSONArray(resourceText("tapin2_stage_raw.json")))
    private val locations = FnbBcosProjection.jsonList(JSONArray(resourceText("tapin2_section_locations_stage.json")))
    private fun location(id: Long) = locations.first { it.getLong("id") == id }

    @Test
    fun `dummy locations keep the tapin2 location shape`() {
        // The key set of the tapin2 location sample, in order.
        val sampleKeys = listOf(
            "description", "section", "imageUrl", "imageUrlMobile", "imageUrlTerminal", "imageUrlPreorder",
            "imageUrlMenuPortrait", "imageUrlMenuLandscape", "orderingEnabled", "isActive", "pauseExpiration", "isHawker",
            "orderId", "isDelivery", "isPickup", "waitTime", "availablePickupTimes", "isPaused", "id", "title"
        )
        assertTrue(locations.all { it.keys().asSequence().toSet() == sampleKeys.toSet() })
        assertEquals("Market Cafe 122", location(19289).getString("title"))
    }

    @Test
    fun `location cards are SDK product cards ordered by orderId, linking only orderable stands`() {
        val elements = FnbBcosProjection.jsonList(FnbBcosProjection.locationCards(locations).getJSONArray("elements"))
        assertEquals(
            listOf("Local Eats 118", "Market Cafe 122", "Golden 1 Grill 124", "Sac Brews 120", "Sweet Spot 126", "Snack Shack 116"),
            elements.map { it.getJSONObject("entity_info").getString("productName") }
        )
        assertTrue(elements.all { it.getString("type") == "productCard" })
        val cafe = elements[1].getJSONObject("entity_info")
        assertEquals("Section 122", cafe.getString("productBadge"))
        assertEquals("Mexican cuisine and drinks.", cafe.getString("productDescription"))
        assertEquals(FnbAction.SelectLocation("19289", "Market Cafe 122"), FnbLinks.parseLocation(cafe.getString("productPageURL")))
        assertEquals("View menu", cafe.getJSONObject("primary").getString("text"))
        assertEquals("Hot dogs, nachos, and quick bites. · 5 min wait", elements[0].getJSONObject("entity_info").getString("productDescription"))
        val paused = elements[4].getJSONObject("entity_info")
        assertEquals("Paused until 8:15 PM", paused.getString("productBadge"))
        assertFalse(paused.has("productPageURL") || paused.has("primary"))
        assertEquals("Ordering closed", elements[5].getJSONObject("entity_info").getString("productBadge"))
    }

    @Test
    fun `real stand projection matches the committed catalog fixture`() {
        val projected = FnbBcosProjection.catalogElements(FnbBcosProjection.productsFor(location(19289), stage), 1000010528, 36747)
        val fixture = elementsFixture("tapin2_catalog_elements_stage.json")
        assertEquals(fixture.size, projected.size)
        projected.zip(fixture).forEach { (actual, expected) -> assertEquals(normalize(expected), normalize(actual.asMap())) }
    }

    @Test
    fun `dummy stands sell a subset of real products, rehomed to the stand`() {
        val menu = CatalogMenuMapper.map(
            FnbBcosProjection.catalogElements(FnbBcosProjection.productsFor(location(19292), stage), 1000010528, 36747).map { it.asMap() }
        )!!
        assertEquals("Sac Brews 120", menu.locationName)
        assertEquals("19292", menu.locationId)
        assertEquals(listOf("Beer", "Beverages"), menu.categories.map { it.label })
    }

    @Test
    fun `one order spans stands and names each line's stand`() {
        val allProducts = CatalogMenuMapper.map(elementsFixture("tapin2_catalog_elements_stage.json"))!!
        val simulator = FnbFlowSimulator(allProducts, locations = locations.associateBy { it.getLong("id") })
        fun submit(stand: Long, name: String) {
            val menu = CatalogMenuMapper.map(FnbBcosProjection.catalogElements(FnbBcosProjection.productsFor(location(stand), stage), 1000010528, 36747).map { it.asMap() })!!
            val item = menu.categories.flatMap { it.items }.first { it.name == name }
            val line = CartReducer.quickAdd(Cart(), item)!!.lines
            simulator.submit(FnbPromptFormatter.format(FnbAction.SubmitCart("s-$stand-$name", menu.venueId, menu.eventId, menu.locationId, menu.locationName, line)))
        }
        submit(19289, "Veggie Nachos")
        submit(19290, "Veggie Nachos")
        submit(19292, "Modelo Especial")

        val cart = CartSummaryMapper.map(listOf(simulator.cartView().asMap()), CartOptions.STAGE)!!
        assertEquals(
            listOf("Veggie Nachos" to "Market Cafe 122", "Veggie Nachos" to "Local Eats 118", "Modelo Especial" to "Sac Brews 120"),
            cart.lines.map { it.title to it.locationName }
        )
        assertEquals("same product at another stand is its own line", 3, cart.lines.size)
    }

    /** Numbers compared by value (tapin2 "8.0000" vs 8.0); nested maps and lists recursively. */
    private fun normalize(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (k, v) -> k.toString() to normalize(v) }
        is List<*> -> value.map(::normalize)
        is Number -> BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
        else -> value
    }
}
