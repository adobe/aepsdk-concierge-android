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

import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbLinks
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * What BCOS does with tapin2 responses in the demo (context_write hooks), written as code so the
 * demo, the tests, and the proposed JMESPath agree:
 * - `get_section_locations` → one SDK out-of-the-box `productCard` per location (no custom
 *   renderer), linking to [FnbLinks.locationUrl];
 * - `GET …/locations/{id}/products` → `catalogItemCard` per entry (a verbatim tapin2 subset) plus
 *   one `cartBar` carrying the location.
 */
internal object FnbBcosProjection {

    /** Location cards, active stands only, ordered by tapin2 `orderId`. Closed/paused stands have no link. */
    fun locationCards(locations: List<JSONObject>, scheme: String = FnbLinks.DEFAULT_SCHEME): JSONObject {
        val elements = JSONArray()
        locations
            .filter { it.optBoolean("isActive", true) }
            .sortedBy { it.optInt("orderId", Int.MAX_VALUE) }
            .forEach { location ->
                val id = location.getLong("id").toString()
                val title = location.optString("title")
                val orderable = location.optBoolean("orderingEnabled", true) && !location.optBoolean("isPaused", false)
                val waitTime = location.optString("waitTime").trim()
                val info = JSONObject()
                    .put("productName", title)
                    .put("productDescription", listOf(location.optString("description"), if (waitTime.isEmpty()) "" else "$waitTime wait").filter { it.isNotBlank() }.joinToString(" · "))
                    .put("productBadge", badge(location, orderable))
                location.optString("imageUrl").takeIf { it.isNotBlank() }?.let { info.put("productImageURL", it) }
                if (orderable) {
                    val link = FnbLinks.locationUrl(id, title, scheme)
                    info.put("productPageURL", link).put("primary", JSONObject().put("text", "View menu").put("url", link))
                }
                elements.put(JSONObject().put("id", "location_$id").put("entityId", id).put("type", "productCard").put("entity_info", info))
            }
        return JSONObject().put("elements", elements)
    }

    private fun badge(location: JSONObject, orderable: Boolean): String = when {
        location.optBoolean("isPaused", false) -> location.optString("pauseExpiration").takeIf { it.isNotBlank() && it != "null" }
            ?.let { "Paused until ${it.substringAfter(' ')}" } ?: "Paused"
        !orderable -> "Ordering closed"
        else -> "Section ${location.optString("section")}"
    }

    /** Menu elements for one location's tapin2 products response. */
    fun catalogElements(entries: List<JSONObject>, venueId: Long, eventId: Long): List<FnbElement> {
        val kept = entries
            .filter { e ->
                val p = e.getJSONObject("product")
                e.optBoolean("isVisible", true) && !e.optBoolean("hideInMobile", false) && e.optBoolean("isActive", true) &&
                    p.optBoolean("isActive", true) && !p.optBoolean("isArchived", false) &&
                    e.optJSONObject("category")?.optBoolean("isActive", true) != false
            }
            .sortedWith(compareBy({ it.getJSONObject("category").optInt("orderId") }, { it.optInt("orderId") }))
        val cards = kept.map { e ->
            val p = e.getJSONObject("product")
            val info = linkedMapOf<String, Any?>(
                "venueId" to venueId,
                "eventId" to eventId,
                "orderId" to e.opt("orderId"),
                "isVisible" to e.opt("isVisible"),
                "hideInMobile" to e.opt("hideInMobile"),
                "isActive" to e.opt("isActive"),
                "locationId" to e.opt("locationId"),
                "categoryId" to e.opt("categoryId"),
                "category" to pick(e.getJSONObject("category"), "id", "title", "orderId", "isActive"),
                "product" to pick(p, "id", "title", "description", "price", "originalPrice", "eventPrice", "imageUrl", "isAlcohol", "isActive", "isArchived") +
                    ("modifierGroups" to jsonList(p.optJSONArray("modifierGroups"))
                        .filter { it.optBoolean("showPublic", true) }
                        .map { g ->
                            pick(g, "id", "title", "minQuantity", "maxQuantity", "maxOnePerSelection", "showPublic") +
                                ("modifiers" to jsonList(g.optJSONArray("modifiers")).map { pick(it, "id", "title", "priceDiff", "isActive", "isArchived") })
                        })
            )
            val id = p.get("id").toString()
            FnbElement(id = id, entityId = id, type = "catalogItemCard", cardType = "catalogItem", entityInfo = info)
        }
        val location = kept.firstOrNull()?.getJSONObject("location") ?: return cards
        val locationId = location.getLong("id")
        val bar = FnbElement(
            id = "cartbar_${venueId}_${eventId}_$locationId",
            entityId = venueId.toString(),
            type = "cartBar",
            cardType = "stickyFooter",
            entityInfo = linkedMapOf(
                "venueId" to venueId,
                "eventId" to eventId,
                "locationId" to locationId,
                "location" to pick(location, "id", "title", "section", "orderingEnabled", "isPaused", "pauseExpiration", "isActive", "isPickup", "isDelivery", "waitTime")
            )
        )
        return cards + bar
    }

    /**
     * Demo-only stand-in for `GET …/locations/{id}/products` at the dummy stands: the real stage
     * entries (tapin2 location 19289) whose categories that stand sells, rehomed to [location].
     * The real stand gets its real menu.
     */
    fun productsFor(location: JSONObject, stageEntries: List<JSONObject>): List<JSONObject> {
        val categories = DUMMY_STAND_CATEGORIES[location.getLong("id")]
        return stageEntries
            .filter { categories == null || it.getJSONObject("category").optString("title") in categories }
            .map { entry ->
                JSONObject(entry.toString())
                    .put("locationId", location.getLong("id"))
                    .put("location", JSONObject(location.toString()))
            }
    }

    private val DUMMY_STAND_CATEGORIES: Map<Long, Set<String>> = mapOf(
        19290L to setOf("Hot Dogs", "Nachos", "Sides", "Beverages"),
        19291L to setOf("Bowls (Burrito/Rice)", "Entrées", "Beverages"),
        19292L to setOf("Beer", "Beverages"),
        19293L to setOf("Dessert", "Beverages"),
        19294L to setOf("Sides", "Beverages")
    )

    fun jsonList(array: JSONArray?): List<JSONObject> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull { array.optJSONObject(it) }

    /** Selects keys, converting nested JSON to Kotlin maps/lists and JSON null to null. */
    private fun pick(obj: JSONObject, vararg keys: String): Map<String, Any?> = keys.associateWith { key -> plain(obj.opt(key)) }

    private fun plain(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.opt(it)) }
        is JSONArray -> (0 until value.length()).map { plain(value.opt(it)) }
        else -> value
    }
}
