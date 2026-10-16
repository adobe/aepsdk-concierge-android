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
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererIds
import org.json.JSONArray
import org.json.JSONObject

/**
 * What BC does with tapin2 responses in the demo, written as code so the demo and the tests agree:
 * - `get_section_locations` → one SDK out-of-the-box `productCard` per location (no custom
 *   renderer), linking to [FnbLinks.locationUrl];
 * - `GET …/locations/{id}/products` → one `fnb.menu` element: the thin envelope
 *   `{id, entityId: locationId, rendererId, entity_info: <tapin2 response body, untouched>}`.
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

    /** The `fnb.menu` element for one location: the tapin2 products response passed through as the payload. */
    fun menuElement(entries: List<JSONObject>, locationId: Long): FnbElement =
        FnbElement(
            id = "menu_$locationId",
            entityId = locationId.toString(),
            rendererId = FnbRendererIds.MENU,
            payload = entries.map { plain(it) }
        )

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

    /** Converts org.json values to Kotlin maps/lists (JSON null → null), as the SDK's parser would. */
    fun plain(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.keys().asSequence().associateWith { plain(value.opt(it)) }
        is JSONArray -> (0 until value.length()).map { plain(value.opt(it)) }
        else -> value
    }
}
