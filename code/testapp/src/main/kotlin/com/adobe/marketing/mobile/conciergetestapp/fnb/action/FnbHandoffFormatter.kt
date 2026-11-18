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

package com.adobe.marketing.mobile.conciergetestapp.fnb.action

import com.adobe.marketing.mobile.conciergetestapp.fnb.FnbXdm
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadText
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem

/**
 * What F&B card actions hand off: the `add-to-cart` XDM fields, and the short line each action
 * shows in the transcript as its `localMessage`. That line is display-only (never sent to Brand
 * Concierge), so it carries no machine data; vendor names in it are sanitized and clamped.
 */
object FnbHandoffFormatter {

    const val SHOW_RESTAURANTS_MESSAGE = "Showing restaurants near your section."

    private const val MAX_DISPLAY_NAME_LENGTH = 40
    private const val MAX_LISTED_LINES = 6

    /** "Adding to your cart from Market Cafe 122:" plus one bullet per line; a count when long. */
    fun addToCartMessage(action: FnbAction.SubmitCart): String {
        if (action.lines.size > MAX_LISTED_LINES) {
            return "Adding ${action.lines.sumOf { it.quantity }} items to your cart."
        }
        val location = displayName(action.locationName)
        val header = if (location.isEmpty()) "Adding to your cart:" else "Adding to your cart from $location:"
        return header + action.lines.joinToString("") { "\n• ${it.quantity} × ${lineLabel(it)}" }
    }

    fun removeMessage(itemTitle: String): String {
        val title = displayName(itemTitle)
        return if (title.isEmpty()) "Removing this item from your order." else "Removing $title from your order."
    }

    fun selectLocationMessage(locationTitle: String): String {
        val title = displayName(locationTitle)
        return if (title.isEmpty()) "Opening the menu." else "Opening the menu at $title."
    }

    /**
     * The per-turn `add-to-cart` XDM fields. Event and current order are held separately in
     * `_sacramentokings.conciergeContext.fnbOrders` and deep-merged by the SDK.
     * - A modifier group is sent only when the line selected something in it, listing only the
     *   selected modifiers (`isSelected: true`) and the group's selection mode from [menuItems].
     *
     * tapin2 ids are sent as JSON integers. Returns null when any id isn't a non-negative integer,
     * so nothing is sent. Prices and notes are not part of the finalized request schema.
     */
    fun addToCartContext(
        action: FnbAction.SubmitCart,
        menuItems: Map<String, MenuItem>
    ): Map<String, Any>? {
        val locationId = tapinId(action.locationId) ?: return null
        val products = action.lines.map { line ->
            product(line, locationId, menuItems[line.itemId]) ?: return null
        }
        if (products.isEmpty()) return null
        return FnbXdm.action("addToCart", mapOf("products" to products))
    }

    private fun product(line: CartLine, locationId: Long, item: MenuItem?): Map<String, Any>? {
        val product = LinkedHashMap<String, Any>()
        product["locationId"] = locationId
        product["productId"] = tapinId(line.itemId) ?: return null
        product["quantity"] = line.quantity
        val groups = modifierGroups(line, item) ?: return null
        if (groups.isNotEmpty()) product["modifierGroups"] = groups
        return product
    }

    private fun modifierGroups(line: CartLine, item: MenuItem?): List<Map<String, Any>>? {
        val menuGroups = item?.optionGroups.orEmpty().associateBy { it.id }
        return line.selectedOptions.groupBy { it.groupId }.map { (groupId, selected) ->
            // Falls back to the mode captured at selection when the group left the menu.
            val isMultiSelect = menuGroups[groupId]?.let { !it.isSingleSelect } ?: selected.first().groupMultiSelect
            mapOf(
                "isMultiSelect" to isMultiSelect,
                "modifiers" to selected.map {
                    mapOf("id" to (tapinId(it.optionId) ?: return null), "isSelected" to true)
                }
            )
        }
    }

    /** tapin2 ids are non-negative integers; anything else is rejected. */
    internal fun tapinId(raw: String): Long? =
        raw.trim().takeIf { it.isNotEmpty() && it.length <= 19 && it.all { ch -> ch in '0'..'9' } }?.toLongOrNull()

    private fun lineLabel(line: CartLine): String {
        val name = displayName(line.name)
        val parts = line.selectedOptions.map { displayName(it.label) } +
            listOfNotNull(line.note?.let(::displayName)?.takeIf { it.isNotEmpty() }?.let { "note: $it" })
        return if (parts.isEmpty()) name else "$name (" + parts.joinToString(", ") + ")"
    }

    /** Single-line, delimiter-free, clamped display text. */
    internal fun displayName(raw: String): String =
        PayloadText.sanitizeInline(raw.filterNot { it == '[' || it == ']' || it == '=' || it == ';' }, MAX_DISPLAY_NAME_LENGTH)
}
