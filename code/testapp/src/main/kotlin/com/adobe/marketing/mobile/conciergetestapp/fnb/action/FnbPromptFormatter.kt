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

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.FnbText

/**
 * Formats [FnbAction.SubmitCart] as the natural-language user turn sent to Brand Concierge.
 *
 * Vendor-supplied names are untrusted LLM input, so they are only used in the human-readable
 * summary after sanitizing (single line, no delimiter characters, clamped). The machine-readable
 * [DETAILS_START]..[DETAILS_END] block is the tapin2 `POST /v2/cart/add` request body with tapin2's
 * names (`venueId`, `eventId`, `products[{locationId, quantity, note, product{Id,
 * modifierGroups[{isMultiSelect, modifiers[{id, isSelected}]}]}}]`), so BC forwards it as-is. BC
 * owns `orderId` (conversation state) and `deliveryMethod` (enum unconfirmed), and strips the one
 * non-tapin2 field, `submitId`, which it uses to skip replays. tapin2 re-prices.
 *
 * The exact template is an open contract question with BC; keep all wording in this file.
 */
object FnbPromptFormatter {

    const val DETAILS_START = "[ORDER_DETAILS]"
    const val DETAILS_END = "[/ORDER_DETAILS]"
    const val CART_ACTION_START = "[CART_ACTION]"
    const val CART_ACTION_END = "[/CART_ACTION]"
    private const val MAX_DISPLAY_NAME_LENGTH = 40
    private const val MAX_ID_LENGTH = 64
    private const val MAX_NOTE_LENGTH = 140
    private val MAX_LENGTH = ConciergeConstants.SendMessage.MAX_MESSAGE_LENGTH

    fun format(action: FnbAction.SubmitCart): String {
        val location = displayName(action.locationName)
        val header = if (location.isEmpty()) "Please add these items to my order:" else "Please add these items to my order from $location:"
        val summary = action.lines.joinToString("\n") { "- ${it.quantity} x ${lineLabel(it)}" }
        val details = detailsBlock(action)
        val full = "$header\n$summary\n\n$details"
        if (full.length <= MAX_LENGTH) return full
        // Drop the readable summary before dropping any machine-readable detail.
        val itemCount = action.lines.sumOf { it.quantity }
        return "Please add these $itemCount items to my order.\n\n$details"
    }

    /**
     * "Remove" from the cart view. BC maps the block to its remove tool; `orderId` should also be
     * checked against conversation state rather than trusted.
     */
    fun formatRemove(action: FnbAction.RemoveCartItem): String {
        val title = displayName(action.title)
        val lead = if (title.isEmpty()) "Please remove this item from my order." else "Please remove $title from my order."
        return "$lead\n\n$CART_ACTION_START\n" +
            """{"action":"remove","orderId":${idValue(action.orderId)},"itemId":${idValue(action.itemId)}}""" +
            "\n$CART_ACTION_END"
    }

    /** "Show more restaurants" from the cart view; the existing order continues. */
    fun formatShowMore(action: FnbAction.ShowMoreRestaurants): String =
        "Show me more restaurants near my section. Keep my current order.\n\n$CART_ACTION_START\n" +
            """{"action":"showMoreLocations","orderId":${idValue(action.orderId)}}""" +
            "\n$CART_ACTION_END"

    /**
     * Hand-built rather than org.json so key order is deterministic; every value is a number,
     * boolean, or [safeId] string, none of which need escaping.
     */
    private fun detailsBlock(action: FnbAction.SubmitCart): String {
        val locationId = idValue(action.locationId)
        val products = action.lines.joinToString(",") { line ->
            val groups = line.selectedOptions.groupBy { it.groupId }.values.joinToString(",") { options ->
                val modifiers = options.joinToString(",") { """{"id":${idValue(it.optionId)},"isSelected":true}""" }
                """{"isMultiSelect":${options.first().groupMultiSelect},"modifiers":[$modifiers]}"""
            }
            val note = line.note?.let(::safeNote)?.takeIf { it.isNotEmpty() }?.let { ""","note":${jsonString(it)}""" }.orEmpty()
            """{"locationId":$locationId,"quantity":${line.quantity}$note,"product":{"Id":${idValue(line.itemId)},"modifierGroups":[$groups]}}"""
        }
        return "$DETAILS_START\n{\"submitId\":\"${safeId(action.submitId)}\"," +
            "\"venueId\":${idValue(action.venueId)},\"eventId\":${idValue(action.eventId)},\"products\":[$products]}\n$DETAILS_END"
    }

    /** The fan's own note: single line, no block delimiters, clamped. */
    private fun safeNote(raw: String): String =
        FnbText.sanitizeInline(raw.filterNot { it == '[' || it == ']' }, MAX_NOTE_LENGTH)

    /** Minimal JSON string encoder for free text (quotes, backslashes, control characters). */
    internal fun jsonString(value: String): String = buildString {
        append('"')
        for (ch in value) {
            when {
                ch == '"' -> append("\\\"")
                ch == '\\' -> append("\\\\")
                ch < ' ' -> append(String.format("\\u%04x", ch.code))
                else -> append(ch)
            }
        }
        append('"')
    }

    /** tapin2 ids are integers; emit them as JSON numbers, anything else as a restricted string. */
    private fun idValue(raw: String): String {
        val id = safeId(raw)
        val isInteger = id == "0" || (id.length in 1..18 && id.first() in '1'..'9' && id.all { it in '0'..'9' })
        return if (isInteger) id else "\"$id\""
    }

    private fun lineLabel(line: CartLine): String {
        val name = displayName(line.name)
        val parts = line.selectedOptions.map { displayName(it.label) } +
            listOfNotNull(line.note?.let(::displayName)?.takeIf { it.isNotEmpty() }?.let { "note: $it" })
        return if (parts.isEmpty()) name else "$name (" + parts.joinToString(", ") + ")"
    }

    /** Single-line, delimiter-free, clamped display text. */
    internal fun displayName(raw: String): String =
        FnbText.sanitizeInline(raw.filterNot { it == '[' || it == ']' || it == '=' || it == ';' }, MAX_DISPLAY_NAME_LENGTH)

    /** Restricts ids to `[A-Za-z0-9_.-]` so they cannot break out of the details block. */
    internal fun safeId(raw: String): String =
        raw.filter { it.isLetterOrDigit() && it.code < 128 || it == '_' || it == '-' || it == '.' }.take(MAX_ID_LENGTH)
}
