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
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderers
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuFnbRenderer
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Loads `{"multimodalElements": {"elements": [...]}}` fixtures as raw element maps. */
@Suppress("UNCHECKED_CAST")
internal fun elementsFixture(name: String): List<Map<String, Any?>> {
    val text = CatalogMenuMapperTest::class.java.classLoader!!.getResourceAsStream("fnb/$name")!!.bufferedReader().use { it.readText() }
    val root = JSONUtils.toMap(JSONObject(text))!!
    return ((root["multimodalElements"] as Map<String, Any?>)["elements"] as List<*>).map { it as Map<String, Any?> }
}

class CatalogMenuMapperTest {

    private val sample = CatalogMenuMapper.map(elementsFixture("bcos_catalog_sample.json"))!!

    @Test
    fun `BCOS sample maps location, ids, categories in card order, and the cart bar`() {
        assertEquals("19289", sample.locationId)
        assertEquals("Market Cafe 122", sample.locationName)
        assertEquals("1000010528", sample.venueId)
        assertEquals("36747", sample.eventId)
        assertEquals("USD", sample.currencyCode)
        assertTrue(sample.orderingAvailable)
        assertEquals(listOf("Snacks", "Drinks"), sample.categories.map { it.label })
        assertEquals("Add to cart", sample.cartBar.label)
        assertTrue(sample.cartBar.showCount)
        assertTrue(sample.cartBar.hideWhenEmpty)
    }

    @Test
    fun `INCREMENT card maps to a quick-add tile with subtitle metadata and tag`() {
        val nachos = sample.categories[0].items.single()
        assertEquals("1364190", nachos.id)
        assertEquals(1400L, nachos.priceCents)
        assertEquals(listOf("Vegetarian", "Shareable"), nachos.metadata)
        assertEquals("Popular", nachos.tag)
        assertFalse(nachos.opensSheet)
        assertTrue(nachos.available)
        assertTrue(CartReducer.canQuickAdd(nachos))
        assertNull("no sheet, so no instructions", nachos.instructions)
    }

    @Test
    fun `OPEN_SHEET card maps the sheet - option groups, instructions, quantity rule, CTA label`() {
        val soda = sample.categories[1].items.single()
        assertTrue(soda.opensSheet)
        assertFalse(CartReducer.canQuickAdd(soda))
        val group = soda.optionGroups.single()
        assertEquals("157322", group.id)
        assertTrue(group.required)
        assertTrue(group.isSingleSelect)
        assertEquals(1, group.maxPerOption)
        assertEquals(7, group.options.size)
        assertEquals("Coke", group.options.first().label)
        assertEquals("Optional instructions", soda.instructions!!.label)
        assertEquals("e.g. no ice, light ice…", soda.instructions!!.placeholder)
        assertEquals(140, soda.instructions!!.maxLength)
        assertEquals(25, soda.quantity.max)
        assertEquals(25, CartReducer.maxQuantity(soda))
        assertEquals("Add to order", soda.addToCartLabel)
    }

    @Test
    fun `stage catalog maps all 33 products in BCOS card order`() {
        val stage = CatalogMenuMapper.map(elementsFixture("bcos_catalog_stage.json"))!!
        assertEquals(33, stage.categories.sumOf { it.items.size })
        assertEquals(
            listOf("Beer", "Beverages", "Bowls (Burrito/Rice)", "Dessert", "Entrées", "Hot Dogs", "Nachos", "Sides"),
            stage.categories.map { it.label }
        )
        assertEquals("Churro, UBE Sugar, Spiced Chocolate Sauce", stage.categories[3].items[0].description)
    }

    @Test
    fun `availability, disabled cart bar, and optional extras are honored`() {
        fun card(id: String, info: Map<String, Any?>) = mapOf(
            "id" to id, "entityId" to id, "type" to "catalogItemCard",
            "entity_info" to mapOf(
                "productName" to "Item $id", "unitPrice" to mapOf("amount" to 3.5, "currency" to "USD"),
                "categoryId" to "c", "categoryLabel" to "Cat", "locationId" to "1", "locationLabel" to "Stand"
            ) + info
        )
        val model = CatalogMenuMapper.map(
            listOf(
                card("1", mapOf("available" to false, "isAlcohol" to true, "wasPrice" to mapOf("amount" to 5.0))),
                card("2", mapOf("productImageURL" to "http://insecure.example/x.png")),
                mapOf("id" to "bar", "type" to "cartBar", "entity_info" to mapOf("enabled" to false, "showCount" to false))
            )
        )!!
        val (soldOut, insecure) = model.categories.single().items
        assertFalse(soldOut.available)
        assertTrue(soldOut.isAlcohol)
        assertEquals(500L, soldOut.wasPriceCents)
        assertNull("http images are dropped", insecure.imageUrl)
        assertFalse("cartBar.enabled=false disables ordering", model.orderingAvailable)
        assertFalse(model.cartBar.showCount)
    }

    @Test
    fun `malformed elements degrade instead of throwing`() {
        assertNull(CatalogMenuMapper.map(emptyList()))
        assertNull(CatalogMenuMapper.map(listOf(mapOf("type" to "cartBar", "entity_info" to emptyMap<String, Any?>()))))
        val model = CatalogMenuMapper.map(
            listOf(
                mapOf("type" to "catalogItemCard", "entity_info" to mapOf("productName" to "No id")),
                mapOf("id" to "2", "type" to "catalogItemCard", "entity_info" to mapOf("productName" to "No price", "categoryId" to "c", "categoryLabel" to "C")),
                mapOf("id" to "3", "type" to "catalogItemCard", "entity_info" to mapOf("productName" to "Ok", "unitPrice" to mapOf("amount" to "2.50"), "categoryId" to "c", "categoryLabel" to "C")),
                mapOf("id" to "3", "type" to "catalogItemCard", "entity_info" to mapOf("productName" to "Duplicate", "unitPrice" to mapOf("amount" to 1), "categoryId" to "c", "categoryLabel" to "C")),
                mapOf("id" to "4", "type" to "somethingElse")
            )
        )!!
        assertEquals(listOf("3"), model.categories.single().items.map { it.id })
        assertEquals(250L, model.categories.single().items.single().priceCents)
    }

    @Test
    fun `registry groups consecutive elements by claiming renderer`() {
        val elements = (elementsFixture("bcos_catalog_sample.json") + elementsFixture("bcos_cart_sample.json"))
            .mapNotNull(FnbElement::fromMap) + FnbElement("x", "", "unknownType", "", emptyMap())
        val groups = FnbRenderers.group(elements)
        assertEquals(2, groups.size)
        assertTrue(groups[0].first === MenuFnbRenderer)
        assertEquals(listOf("catalogItemCard", "catalogItemCard", "cartBar"), groups[0].second.map { it.type })
        assertEquals(listOf("cartView"), groups[1].second.map { it.type })
    }
}
