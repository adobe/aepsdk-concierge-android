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

package com.adobe.marketing.mobile.conciergetestapp

import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

/**
 * Happy-path stand-in for BC + BCOS + tapin2. It consumes the widget's actual user-turn text,
 * as BC would, and answers with tapin2-shaped data:
 * - `[ORDER_DETAILS]` is read as the tapin2 `cart/add` body; `submitId` is stripped (a replay is
 *   skipped, since `cart/add` merges quantities), and `orderId` + `deliveryMethod` are added
 *   from "BC state";
 * - one tapin2 order per conversation, spanning stands: the first add creates `id`/`guid`, later
 *   adds reuse them, and a repeated product at the same stand with the same modifiers and note
 *   merges into its line;
 * - prices come from the menu, not the widget's display prices (tapin2 re-prices), with
 *   per-line `taxAdded` at `venue.taxRate`;
 * - `cartView.entity_info` is a subset of the tapin2 order with tapin2's names.
 *
 * Seeds match the stage `cart/add` sample: order id 695685, line id 2035854, venue
 * "Golden 1 Concierge", tax rate 8.5.
 */
internal class FnbFlowSimulator(
    private val catalog: MenuUiModel,
    /** tapin2 locations (`id` → `{id, title, section}`) for `distinctLocations`; defaults to the catalog's stand. */
    private val locations: Map<Long, JSONObject> = catalog.locationId.toLongOrNull()
        ?.let { mapOf(it to JSONObject().put("id", it).put("title", catalog.locationName).put("section", "")) }
        .orEmpty(),
    private val venueTitle: String = "Golden 1 Concierge",
    private val taxRate: BigDecimal = BigDecimal("8.5"),
    private var nextOrderId: Long = 695685,
    private var nextItemId: Long = 2035854,
    private val guidFactory: () -> String = { UUID.randomUUID().toString() }
) {

    /** One tapin2 order line (`items[]`). */
    data class Item(
        val id: Long,
        val locationId: Long,
        val productId: Long,
        val title: String,
        val isAlcohol: Boolean,
        val quantity: Int,
        val pricePer: BigDecimal,
        val modifierIds: List<Long>,
        val modifier: String,
        val note: String
    ) {
        val key: String get() = "$locationId|$productId|${modifierIds.sorted()}|$note"
    }

    /** One `add_to_cart` round trip: the body BCOS sent to tapin2, and the element it returned. */
    data class AddResult(val cartAddRequest: JSONObject, val cartView: FnbElement, val duplicate: Boolean)

    var orderId: Long? = null
        private set
    var guid: String? = null
        private set
    private val items = mutableListOf<Item>()
    private val handledSubmitIds = mutableSetOf<String>()
    private val menuItems: Map<String, MenuItem> = catalog.categories.flatMap { it.items }.associateBy { it.id }

    val orderItems: List<Item> get() = items.toList()

    /** Handles a SUBMIT turn: reads `[ORDER_DETAILS]` and calls "tapin2 cart/add". */
    fun submit(userTurn: String): AddResult {
        val details = JSONObject(block(userTurn, FnbPromptFormatter.DETAILS_START, FnbPromptFormatter.DETAILS_END))
        val submitId = details.optString("submitId")
        // What BC forwards to tapin2: the body minus submitId, plus BC-owned orderId/deliveryMethod.
        val request = JSONObject(details.toString()).apply {
            remove("submitId")
            put("orderId", orderId ?: JSONObject.NULL)
            put("deliveryMethod", 1)
        }
        if (!handledSubmitIds.add(submitId)) return AddResult(request, cartView(), duplicate = true)
        if (orderId == null) {
            orderId = nextOrderId++
            guid = guidFactory()
        }
        val products = details.optJSONArray("products") ?: JSONArray()
        for (i in 0 until products.length()) {
            val entry = products.getJSONObject(i)
            val product = entry.getJSONObject("product")
            val menuItem = menuItems[product.get("Id").toString()] ?: continue
            val modifierIds = selectedModifierIds(product.optJSONArray("modifierGroups"))
            val choices = menuItem.optionGroups.flatMap { it.options }.filter { it.id.toLong() in modifierIds }
            val candidate = Item(
                id = 0,
                locationId = entry.getLong("locationId"),
                productId = menuItem.id.toLong(),
                title = menuItem.name,
                isAlcohol = menuItem.isAlcohol,
                quantity = entry.getInt("quantity"),
                pricePer = BigDecimal.valueOf(menuItem.priceCents + choices.sumOf { it.priceDeltaCents }, 2),
                modifierIds = modifierIds,
                modifier = choices.joinToString(", ") { it.label },
                note = entry.optString("note", "")
            )
            val index = items.indexOfFirst { it.key == candidate.key }
            if (index >= 0) {
                items[index] = items[index].copy(quantity = items[index].quantity + candidate.quantity)
            } else {
                items += candidate.copy(id = nextItemId++)
            }
        }
        return AddResult(request, cartView(), duplicate = false)
    }

    /** Handles a `[CART_ACTION]` remove turn; returns the removed title, or null. */
    fun remove(userTurn: String): String? {
        val action = JSONObject(block(userTurn, FnbPromptFormatter.CART_ACTION_START, FnbPromptFormatter.CART_ACTION_END))
        if (action.optString("action") != "remove" || action.optLong("orderId") != orderId) return null
        val index = items.indexOfFirst { it.id == action.optLong("itemId") }
        return if (index < 0) null else items.removeAt(index).title
    }

    /** The current tapin2 order as a `cartView` element (`entity_info` = order subset). */
    fun cartView(): FnbElement {
        val lines = items.map { item ->
            val subtotal = item.pricePer.multiply(BigDecimal(item.quantity))
            val tax = subtotal.multiply(taxRate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP)
            linkedMapOf<String, Any?>(
                "id" to item.id,
                "locationId" to item.locationId,
                "product" to linkedMapOf("id" to item.productId, "title" to item.title, "imageUrl" to menuItems[item.productId.toString()]?.imageUrl.orEmpty(), "isAlcohol" to item.isAlcohol),
                "quantity" to item.quantity,
                "pricePer" to item.pricePer.toDouble(),
                "subtotal" to subtotal.toDouble(),
                "taxAdded" to tax.toDouble(),
                "total" to subtotal.add(tax).toDouble(),
                "modifier" to item.modifier,
                "modifiers" to null,
                "note" to item.note
            ) to (subtotal to tax)
        }
        val subtotal = lines.fold(BigDecimal.ZERO) { acc, (_, amounts) -> acc.add(amounts.first) }
        val tax = lines.fold(BigDecimal.ZERO) { acc, (_, amounts) -> acc.add(amounts.second) }
        val locationIds = items.map { it.locationId }.distinct()
        val order = linkedMapOf<String, Any?>(
            "id" to orderId,
            "idLast3" to orderId?.toString()?.takeLast(3),
            "guid" to guid,
            "venueId" to catalog.venueId.toLongOrNull(),
            "eventId" to catalog.eventId.toLongOrNull(),
            "orderStatus" to 1,
            "locationId" to (locationIds.firstOrNull() ?: catalog.locationId.toLongOrNull()),
            "venue" to linkedMapOf("id" to catalog.venueId.toLongOrNull(), "title" to venueTitle, "taxRate" to taxRate.toDouble(), "maxAlcoholPerOrder" to null),
            "items" to lines.map { it.first },
            "distinctLocations" to locationIds.map { id ->
                val location = locations[id]
                linkedMapOf("id" to id, "title" to location?.optString("title").orEmpty(), "section" to location?.optString("section").orEmpty())
            },
            "subtotalNet" to subtotal.toDouble(),
            "taxAddedNet" to tax.toDouble(),
            "feeAddedNet" to 0.0,
            "discountNet" to 0.0,
            "tipNet" to 0.0,
            "totalNet" to subtotal.add(tax).toDouble(),
            "containsAlcohol" to items.any { it.isAlcohol },
            "isPaidInFull" to false
        )
        val id = orderId?.toString().orEmpty()
        return FnbElement(id = "cart_$id", entityId = id, type = "cartView", cardType = "cartSummary", entityInfo = order)
    }

    private fun selectedModifierIds(groups: JSONArray?): List<Long> {
        if (groups == null) return emptyList()
        val ids = mutableListOf<Long>()
        for (g in 0 until groups.length()) {
            val modifiers = groups.getJSONObject(g).optJSONArray("modifiers") ?: continue
            for (m in 0 until modifiers.length()) {
                val modifier = modifiers.getJSONObject(m)
                if (modifier.optBoolean("isSelected")) ids += modifier.getLong("id")
            }
        }
        return ids
    }

    private fun block(text: String, start: String, end: String): String =
        text.substringAfter(start, "").substringBefore(end, "").trim().ifEmpty { "{}" }
}
