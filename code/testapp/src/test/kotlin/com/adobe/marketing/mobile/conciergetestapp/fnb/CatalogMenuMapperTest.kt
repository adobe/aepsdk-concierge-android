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

import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.CartFnbRenderer
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererIds
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererRegistry
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuFnbRenderer
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

internal fun resourceText(name: String): String =
    CatalogMenuMapperTest::class.java.classLoader!!.getResourceAsStream("fnb/$name")!!.bufferedReader().use { it.readText() }

/** The tapin2 stage products response, parsed as the SDK would hand it over (a list of maps). */
internal fun stageProducts(): List<Any?> = JSONUtils.toList(JSONArray(resourceText("tapin2_stage_raw.json")))!!

/** The tapin2 stage `cart/add` response as a map. */
internal fun stageOrder(): Map<String, Any?> = JSONUtils.toMap(JSONObject(resourceText("tapin2_stage_cart_add.json")))!!

class RendererEnvelopeTest {

    @Test
    fun `envelope parses id, entityId, rendererId and passes the payload through untouched`() {
        val payload = stageProducts()
        val element = FnbElement.fromMap(mapOf("id" to "menu_19289", "entityId" to 19289, "rendererId" to "fnb.menu", "entity_info" to payload))!!
        assertEquals("menu_19289", element.id)
        assertEquals("19289", element.entityId)
        assertEquals(FnbRendererIds.MENU, element.rendererId)
        assertTrue("payload is the same object", element.payload === payload)
        assertNull("only entity_info carries the payload", FnbElement.fromMap(mapOf("id" to "x", "rendererId" to "fnb.menu", "payload" to payload))!!.payload)
    }

    @Test
    fun `elements without id or rendererId are not custom-rendered`() {
        assertNull(FnbElement.fromMap(mapOf("rendererId" to "fnb.menu")))
        assertNull(FnbElement.fromMap(mapOf("id" to "x", "type" to "productCard")))
        assertNull(FnbElement.fromMap(mapOf("id" to "x", "rendererId" to "  ")))
    }

    @Test
    fun `registry resolves by rendererId and ignores unknown ids`() {
        val registry = FnbRendererRegistry.DEFAULT
        assertTrue(registry.resolve("fnb.menu") is MenuFnbRenderer)
        assertTrue(registry.resolve(FnbElement("c", "1", "fnb.cart", null)) is CartFnbRenderer)
        assertNull(registry.resolve("vendor.unknown"))
    }
}

class CatalogMenuMapperTest {

    private val menu = CatalogMenuMapper.map(stageProducts())!!
    private val items = menu.categories.flatMap { it.items }.associateBy { it.name }

    private fun entry(product: Map<String, Any?>, extra: Map<String, Any?> = emptyMap(), category: Map<String, Any?> = mapOf("id" to 1, "title" to "C", "orderId" to 1)) =
        mapOf("product" to product, "category" to category) + extra

    @Test
    fun `raw tapin2 products response maps location, categories by category orderId, and all items`() {
        assertEquals("19289", menu.locationId)
        assertEquals("Market Cafe 122", menu.locationName)
        assertTrue(menu.orderingAvailable)
        assertNull("stage waitTime is empty", menu.waitTime)
        assertEquals("", menu.venueId)
        assertEquals("not in the tapin2 response; BC uses the session", "", menu.eventId)
        assertEquals(
            listOf("Beer", "Beverages", "Bowls (Burrito/Rice)", "Dessert", "Entrées", "Hot Dogs", "Nachos", "Sides"),
            menu.categories.map { it.label }
        )
        assertEquals(33, menu.categories.sumOf { it.items.size })
        assertEquals("equal orderIds keep payload order", listOf("\"Light The Beam\" Churro", "Salt & Straw - Ancho Taco"), menu.categories[3].items.map { it.name })
    }

    @Test
    fun `product fields map from tapin2 names`() {
        val churro = items.getValue("\"Light The Beam\" Churro")
        assertEquals("1363910", churro.id)
        assertEquals(800L, churro.priceCents)
        assertEquals("HTML stripped", "Churro, UBE Sugar, Spiced Chocolate Sauce", churro.description)
        assertTrue("no tapin2 source for tile metadata", churro.metadata.isEmpty())
        assertNull(churro.tag)
        assertNull("empty imageUrl renders a placeholder", churro.imageUrl)
        assertFalse(churro.opensSheet)
        assertTrue(CartReducer.canQuickAdd(churro))
        assertEquals(13, menu.categories.flatMap { it.items }.count { it.imageUrl != null })
        assertTrue(menu.categories.first().items.all { it.isAlcohol })
    }

    @Test
    fun `modifierGroups map minQuantity, maxQuantity, maxOnePerSelection, and priceDiff`() {
        val soda = items.getValue("Fountain Soda")
        assertTrue(soda.opensSheet)
        assertFalse(CartReducer.canQuickAdd(soda))
        val group = soda.optionGroups.single()
        assertEquals("157322", group.id)
        assertEquals("Fountain Soda Flavor", group.title)
        assertTrue(group.required)
        assertTrue(group.isSingleSelect)
        assertEquals(1, group.maxPerOption)
        assertEquals(listOf("Coke", "Diet Coke", "Sprite", "Coke Zero", "Lemonade", "Fanta Orange", "Root Beer"), group.options.map { it.label })
        assertTrue(group.options.all { it.priceDeltaCents == 0L && !it.isDefault })
    }

    @Test
    fun `venueId and eventId are used when tapin2 adds them to the entries`() {
        val withIds = stageProducts().map { (it as Map<*, *>).entries.associate { (k, v) -> k.toString() to v } + mapOf("venueId" to 1000010528, "eventId" to 36747) }
        val model = CatalogMenuMapper.map(withIds)!!
        assertEquals("1000010528", model.venueId)
        assertEquals("36747", model.eventId)
    }

    @Test
    fun `widget defaults come from MenuOptions`() {
        assertNull("notes off until tapin2 confirms products[].note", items.getValue("Fountain Soda").instructions)
        val withNotes = CatalogMenuMapper.map(stageProducts(), MenuOptions(instructions = MenuOptions.NOTES_ENABLED, currencyCode = "CAD"))!!
        assertEquals(140, withNotes.categories.flatMap { it.items }.first().instructions!!.maxLength)
        assertEquals("CAD", withNotes.currencyCode)
    }

    @Test
    fun `hidden, inactive, archived, and duplicate entries are dropped and orderIds sort`() {
        val model = CatalogMenuMapper.map(
            listOf(
                entry(mapOf("id" to 2, "title" to "Second", "eventPrice" to 1.0), mapOf("orderId" to 2)),
                entry(mapOf("id" to 1, "title" to "First", "eventPrice" to 1.0), mapOf("orderId" to 1)),
                entry(mapOf("id" to 3, "title" to "Hidden", "eventPrice" to 1.0), mapOf("isVisible" to false)),
                entry(mapOf("id" to 4, "title" to "Web only", "eventPrice" to 1.0), mapOf("hideInMobile" to true)),
                entry(mapOf("id" to 5, "title" to "Inactive", "eventPrice" to 1.0, "isActive" to false)),
                entry(mapOf("id" to 6, "title" to "Archived", "eventPrice" to 1.0, "isArchived" to true)),
                entry(mapOf("id" to 7, "title" to "Inactive category", "eventPrice" to 1.0), category = mapOf("id" to 2, "title" to "X", "isActive" to false)),
                entry(mapOf("id" to 1, "title" to "First again (other menu)", "eventPrice" to 1.0), category = mapOf("id" to 3, "title" to "Y"))
            )
        )!!
        assertEquals(listOf("First", "Second"), model.categories.single().items.map { it.name })
    }

    @Test
    fun `price falls back to price, originalPrice becomes a was-price, and paused locations are unavailable`() {
        val model = CatalogMenuMapper.map(
            listOf(
                entry(
                    mapOf("id" to 1, "title" to "Promo", "price" to 7.5, "originalPrice" to 9.0, "eventPrice" to null),
                    mapOf("location" to mapOf("id" to 9, "title" to "Stand", "isPaused" to true, "waitTime" to "10 min"))
                )
            )
        )!!
        val promo = model.categories.single().items.single()
        assertEquals(750L, promo.priceCents)
        assertEquals(900L, promo.wasPriceCents)
        assertFalse(model.orderingAvailable)
        assertEquals("10 min", model.waitTime)
        assertEquals("9", model.locationId)
    }

    @Test
    fun `non-array or malformed payloads degrade instead of throwing`() {
        assertNull(CatalogMenuMapper.map(null))
        assertNull(CatalogMenuMapper.map(emptyList<Any>()))
        assertNull(CatalogMenuMapper.map(mapOf("products" to stageProducts())))
        assertNull(CatalogMenuMapper.map(listOf("junk", 42, null)))
        val model = CatalogMenuMapper.map(
            listOf(
                entry(mapOf("id" to 1, "title" to "No price")),
                entry(mapOf("title" to "No id", "eventPrice" to 1)),
                entry(mapOf("id" to 2, "title" to "Negative", "eventPrice" to -1)),
                entry(mapOf("id" to 3, "title" to "Ok", "eventPrice" to "2.50", "modifierGroups" to "junk"))
            )
        )!!
        assertEquals(listOf("3"), model.categories.single().items.map { it.id })
        assertEquals(250L, model.categories.single().items.single().priceCents)
    }
}
