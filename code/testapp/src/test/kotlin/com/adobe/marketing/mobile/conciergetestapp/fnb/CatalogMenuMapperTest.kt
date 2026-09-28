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
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderers
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuFnbRenderer
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Loads `{"multimodalElements": {"elements": [...]}}` fixtures as raw element maps. */
@Suppress("UNCHECKED_CAST")
internal fun elementsFixture(name: String): List<Map<String, Any?>> {
    val root = JSONUtils.toMap(JSONObject(resourceText(name)))!!
    return ((root["multimodalElements"] as Map<String, Any?>)["elements"] as List<*>).map { it as Map<String, Any?> }
}

/** tapin2 decimals parse as BigDecimal ("16.0000") or Double (16.0); compare numbers by value. */
internal fun assertSameValue(message: String, expected: Any?, actual: Any?) {
    if (expected is Number && actual is Number) {
        assertEquals(message, java.math.BigDecimal(expected.toString()).stripTrailingZeros(), java.math.BigDecimal(actual.toString()).stripTrailingZeros())
    } else {
        assertEquals(message, expected, actual)
    }
}

internal fun resourceText(name: String): String =
    CatalogMenuMapperTest::class.java.classLoader!!.getResourceAsStream("fnb/$name")!!.bufferedReader().use { it.readText() }

class CatalogMenuMapperTest {

    private val elements = elementsFixture("tapin2_catalog_elements_stage.json")
    private val menu = CatalogMenuMapper.map(elements)!!
    private val items = menu.categories.flatMap { it.items }.associateBy { it.name }

    private fun card(product: Map<String, Any?>, extra: Map<String, Any?> = emptyMap(), category: Map<String, Any?> = mapOf("id" to 1, "title" to "C", "orderId" to 1)) =
        mapOf("id" to product["id"].toString(), "type" to "catalogItemCard", "entity_info" to mapOf("product" to product, "category" to category) + extra)

    @Test
    fun `entity_info is a verbatim subset of the tapin2 products entries`() {
        val raw = JSONUtils.toList(JSONArray(resourceText("tapin2_stage_raw.json")))!!.map {
            @Suppress("UNCHECKED_CAST")
            it as Map<String, Any?>
        }.associateBy { (it["product"] as Map<*, *>)["id"].toString() }
        val cards = elements.filter { it["type"] == "catalogItemCard" }
        assertEquals(33, cards.size)
        for (card in cards) {
            val info = card["entity_info"] as Map<*, *>
            val entry = raw.getValue(card["entityId"] as String)
            for (key in listOf("orderId", "locationId", "categoryId", "isVisible", "hideInMobile")) assertSameValue(key, entry[key], info[key])
            val product = info["product"] as Map<*, *>
            val rawProduct = entry["product"] as Map<*, *>
            for ((key, value) in product) {
                if (key != "modifierGroups") assertSameValue("product.$key", rawProduct[key], value)
            }
            val category = info["category"] as Map<*, *>
            for ((key, value) in category) assertSameValue("category.$key", (entry["category"] as Map<*, *>)[key], value)
        }
    }

    @Test
    fun `stage elements map location from the cartBar and categories by category orderId`() {
        assertEquals("19289", menu.locationId)
        assertEquals("Market Cafe 122", menu.locationName)
        assertEquals("1000010528", menu.venueId)
        assertEquals("36747", menu.eventId)
        assertTrue(menu.orderingAvailable)
        assertNull("stage waitTime is empty", menu.waitTime)
        assertEquals(
            listOf("Beer", "Beverages", "Bowls (Burrito/Rice)", "Dessert", "Entrées", "Hot Dogs", "Nachos", "Sides"),
            menu.categories.map { it.label }
        )
        assertEquals(33, menu.categories.sumOf { it.items.size })
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
    fun `widget defaults come from MenuOptions`() {
        assertNull("notes off until tapin2 confirms products[].note", items.getValue("Fountain Soda").instructions)
        val withNotes = CatalogMenuMapper.map(elements, MenuOptions(instructions = MenuOptions.NOTES_ENABLED, currencyCode = "CAD"))!!
        assertEquals(140, withNotes.categories.flatMap { it.items }.first().instructions!!.maxLength)
        assertEquals("CAD", withNotes.currencyCode)
    }

    @Test
    fun `hidden, inactive, archived, and duplicate entries are dropped and orderIds sort`() {
        val model = CatalogMenuMapper.map(
            listOf(
                card(mapOf("id" to 2, "title" to "Second", "eventPrice" to 1.0), mapOf("orderId" to 2)),
                card(mapOf("id" to 1, "title" to "First", "eventPrice" to 1.0), mapOf("orderId" to 1)),
                card(mapOf("id" to 3, "title" to "Hidden", "eventPrice" to 1.0), mapOf("isVisible" to false)),
                card(mapOf("id" to 4, "title" to "Web only", "eventPrice" to 1.0), mapOf("hideInMobile" to true)),
                card(mapOf("id" to 5, "title" to "Inactive", "eventPrice" to 1.0, "isActive" to false)),
                card(mapOf("id" to 6, "title" to "Archived", "eventPrice" to 1.0, "isArchived" to true)),
                card(mapOf("id" to 7, "title" to "Inactive category", "eventPrice" to 1.0), category = mapOf("id" to 2, "title" to "X", "isActive" to false)),
                card(mapOf("id" to 1, "title" to "First again (other menu)", "eventPrice" to 1.0), category = mapOf("id" to 3, "title" to "Y"))
            )
        )!!
        assertEquals(listOf("First", "Second"), model.categories.single().items.map { it.name })
    }

    @Test
    fun `price falls back to price, originalPrice becomes a was-price, and paused locations are unavailable`() {
        val model = CatalogMenuMapper.map(
            listOf(
                card(mapOf("id" to 1, "title" to "Promo", "price" to 7.5, "originalPrice" to 9.0, "eventPrice" to null)),
                mapOf("type" to "cartBar", "entity_info" to mapOf("location" to mapOf("id" to 9, "title" to "Stand", "isPaused" to true, "waitTime" to "10 min")))
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
    fun `malformed elements degrade instead of throwing`() {
        assertNull(CatalogMenuMapper.map(emptyList()))
        assertNull(CatalogMenuMapper.map(listOf(mapOf("type" to "catalogItemCard", "entity_info" to "junk"))))
        val model = CatalogMenuMapper.map(
            listOf(
                card(mapOf("id" to 1, "title" to "No price")),
                card(mapOf("title" to "No id", "eventPrice" to 1)),
                card(mapOf("id" to 2, "title" to "Negative", "eventPrice" to -1)),
                card(mapOf("id" to 3, "title" to "Ok", "eventPrice" to "2.50", "modifierGroups" to "junk"))
            )
        )!!
        assertEquals(listOf("3"), model.categories.single().items.map { it.id })
        assertEquals(250L, model.categories.single().items.single().priceCents)
    }

    @Test
    fun `registry groups consecutive elements by claiming renderer`() {
        val cart = elementsFixture("tapin2_cart_view_stage.json")
        val all = (elements + cart).mapNotNull(FnbElement::fromMap) + FnbElement("x", "", "unknownType", "", emptyMap())
        val groups = FnbRenderers.group(all)
        assertEquals(2, groups.size)
        assertTrue(groups[0].first is MenuFnbRenderer)
        assertEquals(34, groups[0].second.size)
        assertTrue(groups[1].first is CartFnbRenderer)
        assertEquals(listOf("cartView"), groups[1].second.map { it.type })
    }
}
