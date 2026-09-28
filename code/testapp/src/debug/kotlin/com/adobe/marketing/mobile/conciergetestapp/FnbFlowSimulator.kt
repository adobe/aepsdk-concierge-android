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

import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import java.util.Locale
import java.util.UUID

/**
 * Happy-path stand-in for BC + BCOS + tapin2, so the widgets can be exercised end to end without
 * a backend. It plays the server's role faithfully where it matters:
 * - one tapin2 order per conversation: the first `cart/add` creates `orderId`/`guid`, later adds
 *   reuse them, and a repeated product (same options and note) merges into its line;
 * - prices come from the catalog, not from the widget's display prices (tapin2 re-prices);
 * - a replayed `submitId` is ignored (tapin2 would double the quantities);
 * - responses are shaped exactly like the BCOS `cartView` sample.
 *
 * Seed values mirror the stage data: order ids around 679154, line ids around 1988604, venue
 * "Golden 1 Center", 8.5% tax (tapin2 `venue.taxRate`).
 */
internal class FnbFlowSimulator(
    private val catalog: MenuUiModel,
    private val venueName: String = "Golden 1 Center",
    private val taxBasisPoints: Long = 850,
    private val checkoutBase: String = "https://mobile-stg.tapin2.co",
    private var nextOrderId: Long = 679154,
    private var nextLineId: Long = 1988604,
    private val guidFactory: () -> String = { UUID.randomUUID().toString() }
) {

    data class OrderLine(
        val lineId: String,
        val productId: String,
        val name: String,
        val quantity: Int,
        val unitCents: Long,
        val optionIds: List<String>,
        val modifiersSummary: String,
        val note: String?,
        val locationId: String,
        val locationLabel: String
    ) {
        val key: String get() = "$productId|${optionIds.sorted().joinToString(",")}|${note.orEmpty()}"
    }

    /** One `add_to_cart` round trip: what BCOS sent to tapin2, and the element it returned. */
    data class AddResult(val cartAddRequest: Map<String, Any?>, val cartView: FnbElement, val duplicate: Boolean)

    var orderId: String? = null
        private set
    var guid: String? = null
        private set
    private val lines = mutableListOf<OrderLine>()
    private val handledSubmitIds = mutableSetOf<String>()
    private val itemsById: Map<String, MenuItem> = catalog.categories.flatMap { it.items }.associateBy { it.id }

    val orderLines: List<OrderLine> get() = lines.toList()

    fun submit(action: FnbAction.SubmitCart): AddResult {
        val request = cartAddRequest(action)
        if (!handledSubmitIds.add(action.submitId)) return AddResult(request, cartView(), duplicate = true)
        if (orderId == null) {
            orderId = (nextOrderId++).toString()
            guid = guidFactory()
        }
        for (line in action.lines) {
            val item = itemsById[line.itemId] ?: continue
            val optionIds = line.selectedOptions.map { it.optionId }
            val selected = item.optionGroups.flatMap { it.options }.filter { it.id in optionIds }
            val candidate = OrderLine(
                lineId = "",
                productId = item.id,
                name = item.name,
                quantity = line.quantity,
                unitCents = item.priceCents + selected.sumOf { it.priceDeltaCents },
                optionIds = optionIds,
                modifiersSummary = selected.joinToString(", ") { it.label }.ifEmpty { "No modifiers" },
                note = line.note,
                locationId = action.locationId,
                locationLabel = action.locationName
            )
            val index = lines.indexOfFirst { it.key == candidate.key }
            if (index >= 0) {
                lines[index] = lines[index].copy(quantity = lines[index].quantity + candidate.quantity)
            } else {
                lines += candidate.copy(lineId = (nextLineId++).toString())
            }
        }
        return AddResult(request, cartView(), duplicate = false)
    }

    /** Returns the removed line's name, or null when it isn't on the order. */
    fun remove(lineId: String): String? {
        val index = lines.indexOfFirst { it.lineId == lineId }
        if (index < 0) return null
        return lines.removeAt(index).name
    }

    val subtotalCents: Long get() = lines.sumOf { it.unitCents * it.quantity }
    val taxCents: Long get() = (subtotalCents * taxBasisPoints + 5_000) / 10_000
    val totalCents: Long get() = subtotalCents + taxCents

    /** The tapin2 `POST /v2/cart/add` body BCOS would build from the submit's items. */
    fun cartAddRequest(action: FnbAction.SubmitCart): Map<String, Any?> = linkedMapOf(
        "venueId" to action.venueId.toLongOrNull(),
        "eventId" to action.eventId.toLongOrNull(),
        "orderId" to orderId,
        "deliveryMethod" to 1,
        "products" to action.lines.map { line ->
            linkedMapOf(
                "locationId" to action.locationId.toLongOrNull(),
                "quantity" to line.quantity,
                "note" to line.note,
                "product" to linkedMapOf(
                    "Id" to line.itemId.toLongOrNull(),
                    "modifierGroups" to line.selectedOptions.groupBy { it.groupId }.map { (_, options) ->
                        linkedMapOf(
                            "isMultiSelect" to options.first().groupMultiSelect,
                            "modifiers" to options.map { linkedMapOf("id" to it.optionId.toLongOrNull(), "isSelected" to true) }
                        )
                    }
                )
            )
        }
    )

    /** The current order as a BCOS `cartView` element (same shape as the BCOS sample). */
    fun cartView(): FnbElement {
        val id = orderId.orEmpty()
        val info = linkedMapOf<String, Any?>(
            "cartId" to id,
            "guid" to guid,
            "venueId" to catalog.venueId,
            "eventId" to catalog.eventId,
            "venue" to mapOf("name" to venueName),
            "lines" to lines.map { line ->
                linkedMapOf(
                    "lineId" to line.lineId,
                    "entityId" to line.productId,
                    "name" to line.name,
                    "quantity" to line.quantity,
                    "modifiersSummary" to line.modifiersSummary,
                    "note" to line.note,
                    "location" to mapOf("id" to line.locationId, "label" to line.locationLabel, "section" to line.locationLabel.substringAfterLast(' ')),
                    "unitPrice" to money(line.unitCents),
                    "lineTotal" to money(line.unitCents * line.quantity),
                    "actions" to mapOf(
                        "remove" to mapOf("action" to "REMOVE_LINE", "lineId" to line.lineId),
                        "edit" to mapOf("action" to "EDIT_LINE", "lineId" to line.lineId, "entityId" to line.productId, "locationId" to line.locationId)
                    )
                )
            },
            "totals" to mapOf("subtotal" to money(subtotalCents), "tax" to money(taxCents), "total" to money(totalCents)),
            "checkout" to mapOf(
                "label" to "Proceed to checkout",
                "action" to "PROCEED_TO_CHECKOUT",
                // The Review page (what tapin2 says checkout should be), not receiptUrl.
                "url" to "$checkoutBase/Review/Index/${catalog.venueId}?eventId=${catalog.eventId}&orderId=${guid.orEmpty()}"
            )
        )
        return FnbElement(id = "cart_$id", entityId = id, type = "cartView", cardType = "cartSummary", entityInfo = info)
    }

    private fun money(cents: Long): Map<String, Any?> = mapOf(
        "amount" to cents / 100.0,
        "currency" to catalog.currencyCode,
        "display" to String.format(Locale.US, "$%d.%02d", cents / 100, cents % 100)
    )
}
