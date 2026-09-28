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

package com.adobe.marketing.mobile.conciergetestapp.fnb.model

import androidx.compose.runtime.Immutable

@Immutable
data class SelectedOption(
    val groupId: String,
    val optionId: String,
    val label: String,
    val priceDeltaCents: Long,
    /** Echoed to tapin2 `cart/add` as `modifierGroups[].isMultiSelect`. */
    val groupMultiSelect: Boolean = false
)

/**
 * One cart line. Lines with the same item and the same option set merge (see [lineKey]).
 * [unitPriceCents] already includes option price deltas. Display-only: the backend must re-price.
 */
@Immutable
data class CartLine(
    val itemId: String,
    val name: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val selectedOptions: List<SelectedOption> = emptyList(),
    /** Fan's special instructions for this line, already sanitized; null when none. */
    val note: String? = null
) {
    val lineKey: String
        get() = itemId + "|" + selectedOptions.map { "${it.groupId}:${it.optionId}" }.sorted().joinToString(",") + "|" + note.orEmpty()

    val totalCents: Long get() = unitPriceCents * quantity
}

@Immutable
data class Cart(val lines: List<CartLine> = emptyList()) {
    val totalQuantity: Int get() = lines.sumOf { it.quantity }
    val totalCents: Long get() = lines.sumOf { it.totalCents }
    val isEmpty: Boolean get() = lines.isEmpty()

    fun quantityFor(itemId: String): Int = lines.filter { it.itemId == itemId }.sumOf { it.quantity }
}

/** Pure cart transitions. All functions return the input cart unchanged when a limit is hit. */
object CartReducer {

    const val MAX_LINES = 20
    /** Absolute per-line ceiling; each item's own `quantity.max` (default 20) applies below it. */
    const val MAX_LINE_QUANTITY = 99

    /** Line cap: the item's `quantity.max`, never above [MAX_LINE_QUANTITY]. */
    fun maxQuantity(item: MenuItem): Int = item.quantity.max.coerceIn(1, MAX_LINE_QUANTITY)

    /**
     * Adds [quantity] of [item] with [selected] options (and optional [note]), merging with an
     * identical line. Sold-out items are ignored.
     */
    fun add(cart: Cart, item: MenuItem, selected: List<SelectedOption>, quantity: Int = 1, note: String? = null): Cart {
        if (quantity <= 0 || !item.available) return cart
        val max = maxQuantity(item)
        val line = CartLine(
            itemId = item.id,
            name = item.name,
            unitPriceCents = item.priceCents + selected.sumOf { it.priceDeltaCents },
            quantity = quantity.coerceAtMost(max),
            selectedOptions = selected,
            note = note?.takeIf { it.isNotBlank() }
        )
        val index = cart.lines.indexOfFirst { it.lineKey == line.lineKey }
        if (index >= 0) {
            val existing = cart.lines[index]
            val merged = existing.copy(quantity = (existing.quantity + quantity).coerceAtMost(max))
            return Cart(cart.lines.toMutableList().also { it[index] = merged })
        }
        if (cart.lines.size >= MAX_LINES) return cart
        return Cart(cart.lines + line)
    }

    /**
     * Whether `+` can add [item] without opening Customize: it must be available, declared
     * `INCREMENT` (not `OPEN_SHEET`), and every required group must be met by defaults.
     */
    fun canQuickAdd(item: MenuItem): Boolean =
        item.available && !item.opensSheet &&
            item.optionGroups.all { group -> CustomizeLogic.isSatisfied(group, CustomizeLogic.defaultIds(group)) }

    /** Adds one unit with default options, or returns null when the item needs Customize first. */
    fun quickAdd(cart: Cart, item: MenuItem): Cart? {
        if (!canQuickAdd(item)) return null
        val defaults = CustomizeLogic.toSelectedOptions(item, CustomizeLogic.initialSelections(item))
        return add(cart, item, defaults, 1)
    }

    /**
     * Removes one unit of [itemId]: from the default-options line first, otherwise from the most
     * recently added line for that item.
     */
    fun decrement(cart: Cart, item: MenuItem): Cart {
        val defaultsKey = CartLine(
            itemId = item.id,
            name = item.name,
            unitPriceCents = 0,
            quantity = 0,
            selectedOptions = CustomizeLogic.toSelectedOptions(item, CustomizeLogic.initialSelections(item))
        ).lineKey
        val index = cart.lines.indexOfFirst { it.lineKey == defaultsKey }
            .takeIf { it >= 0 }
            ?: cart.lines.indexOfLast { it.itemId == item.id }
        if (index < 0) return cart
        val line = cart.lines[index]
        val lines = cart.lines.toMutableList()
        if (line.quantity <= 1) lines.removeAt(index) else lines[index] = line.copy(quantity = line.quantity - 1)
        return Cart(lines)
    }
}

/** Pure option-selection rules shared by the Customize UI and the cart. */
object CustomizeLogic {

    fun defaultIds(group: OptionGroup): Set<String> =
        group.options.filter { it.isDefault && it.available }.map { it.id }.take(group.maxSelect).toSet()

    fun initialSelections(item: MenuItem): Map<String, Set<String>> =
        item.optionGroups.associate { it.id to defaultIds(it) }

    fun isSatisfied(group: OptionGroup, selected: Set<String>): Boolean =
        selected.size >= group.minSelect && selected.size <= group.maxSelect

    fun isValid(item: MenuItem, selections: Map<String, Set<String>>): Boolean =
        item.optionGroups.all { isSatisfied(it, selections[it.id].orEmpty()) }

    fun unmetGroupIds(item: MenuItem, selections: Map<String, Set<String>>): Set<String> =
        item.optionGroups.filterNot { isSatisfied(it, selections[it.id].orEmpty()) }.map { it.id }.toSet()

    /**
     * Single-select groups behave as radios (an optional one can be cleared by re-tapping).
     * Multi-select groups ignore additions beyond [OptionGroup.maxSelect].
     */
    fun toggle(group: OptionGroup, current: Set<String>, optionId: String): Set<String> {
        if (group.options.none { it.id == optionId && it.available }) return current
        return if (group.isSingleSelect) {
            when {
                optionId !in current -> setOf(optionId)
                group.required -> current
                else -> emptySet()
            }
        } else {
            when {
                optionId in current -> current - optionId
                current.size >= group.maxSelect -> current
                else -> current + optionId
            }
        }
    }

    fun toSelectedOptions(item: MenuItem, selections: Map<String, Set<String>>): List<SelectedOption> =
        item.optionGroups.flatMap { group ->
            val ids = selections[group.id].orEmpty()
            group.options.filter { it.id in ids }
                .map { SelectedOption(group.id, it.id, it.label, it.priceDeltaCents, groupMultiSelect = !group.isSingleSelect) }
        }

    fun unitPriceCents(item: MenuItem, selections: Map<String, Set<String>>): Long =
        item.priceCents + toSelectedOptions(item, selections).sumOf { it.priceDeltaCents }
}
