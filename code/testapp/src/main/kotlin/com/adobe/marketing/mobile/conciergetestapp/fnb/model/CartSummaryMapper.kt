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

import java.math.BigDecimal

/**
 * Maps the BCOS cart element (`type: "cartView"`, `cardType: "cartSummary"`) into
 * [CartSummaryUiModel]. BCOS builds it from the tapin2 order (`cart/add`, update, remove); all
 * money is tapin2's. Every field name lives in [Keys].
 *
 * `entity_info` (BCOS sample): `{ cartId, guid, venueId, eventId, venue{name},
 *   lines[{ lineId, entityId, name, quantity, modifiersSummary, location{id, label, section},
 *           unitPrice, lineTotal, actions{remove{action, lineId}, edit{...}} }],
 *   totals{subtotal, tax, total}, checkout{label, action, url | receiptUrl} }`
 * Money is `{amount, currency, display}`; `amount` is authoritative.
 */
object CartSummaryMapper {

    object Keys {
        const val TYPE = "type"
        const val ENTITY_ID = "entityId"
        const val ENTITY_INFO = "entity_info"
        const val TYPE_CART_VIEW = "cartView"

        const val CART_ID = "cartId"
        const val GUID = "guid"
        const val ORDER_CODE = "orderCode"
        const val VENUE = "venue"
        const val NAME = "name"
        const val LINES = "lines"
        const val LINE_ID = "lineId"
        const val QUANTITY = "quantity"
        const val MODIFIERS_SUMMARY = "modifiersSummary"
        const val NOTE = "note"
        const val LOCATION = "location"
        const val ID = "id"
        const val LABEL = "label"
        const val UNIT_PRICE = "unitPrice"
        const val LINE_TOTAL = "lineTotal"
        const val ACTIONS = "actions"
        const val REMOVE = "remove"
        const val TOTALS = "totals"
        const val SUBTOTAL = "subtotal"
        const val TAX = "tax"
        const val FEES = "fees"
        const val DISCOUNT = "discount"
        const val TOTAL = "total"
        const val CURRENCY = "currency"
        const val IS_PAID = "isPaid"
        const val CONTAINS_ALCOHOL = "containsAlcohol"
        const val CHECKOUT = "checkout"
        const val URL = "url"
        const val RECEIPT_URL = "receiptUrl"
        const val SECONDARY_ACTION = "secondaryAction"
    }

    const val MAX_LINES = 50
    private const val DEFAULT_MODIFIERS_SUMMARY = "No modifiers"

    /** Maps the first `cartView` element; null when there is none or it has no cart id. */
    fun map(elements: List<Map<String, Any?>>, allowedCheckoutHosts: Set<String> = CheckoutUrlPolicy.DEFAULT_HOSTS): CartSummaryUiModel? {
        val element = elements.firstOrNull { it[Keys.TYPE] == Keys.TYPE_CART_VIEW } ?: return null
        val info = element[Keys.ENTITY_INFO].asStringMap() ?: return null
        val cartId = (info[Keys.CART_ID] ?: element[Keys.ENTITY_ID]).asId() ?: return null
        val mapped = (info[Keys.LINES] as? List<*>).orEmpty()
            .asSequence()
            .mapNotNull { it.asStringMap() }
            .mapNotNull(::mapLine)
            .take(MAX_LINES)
            .toList()
        val lines = mapped.map { it.line }
        val totals = info[Keys.TOTALS].asStringMap()
        val checkout = info[Keys.CHECKOUT].asStringMap()
        val secondary = info[Keys.SECONDARY_ACTION].asStringMap()
        val lineSum = lines.sumOf { it.subtotalCents }
        // BCOS currently points checkout at tapin2's receiptUrl; prefer an explicit url when present.
        val checkoutUrl = ((checkout?.get(Keys.URL) ?: checkout?.get(Keys.RECEIPT_URL)) as? String)
            ?.takeIf { CheckoutUrlPolicy.isAllowed(it, allowedCheckoutHosts) }
        return CartSummaryUiModel(
            cartId = cartId,
            guid = (info[Keys.GUID] as? String)?.trim()?.takeIf { it.isNotEmpty() },
            orderCode = FnbText.sanitizeInline(info[Keys.ORDER_CODE]?.toString(), 12).ifEmpty { null },
            venueName = FnbText.sanitizeInline(info[Keys.VENUE].asStringMap()?.get(Keys.NAME) as? String, FnbLimits.MAX_NAME_LENGTH),
            lines = lines,
            subtotalCents = money(totals?.get(Keys.SUBTOTAL)) ?: lineSum,
            taxCents = money(totals?.get(Keys.TAX)) ?: 0L,
            feesCents = money(totals?.get(Keys.FEES)) ?: 0L,
            discountCents = money(totals?.get(Keys.DISCOUNT)) ?: 0L,
            totalCents = money(totals?.get(Keys.TOTAL)) ?: lineSum,
            isPaid = info[Keys.IS_PAID] == true,
            containsAlcohol = info[Keys.CONTAINS_ALCOHOL] == true,
            checkoutUrl = checkoutUrl,
            currencyCode = currencyOf(totals?.get(Keys.TOTAL)) ?: mapped.firstNotNullOfOrNull { it.currency } ?: MenuUiModel.DEFAULT_CURRENCY_CODE,
            checkoutLabel = FnbText.sanitizeInline(checkout?.get(Keys.LABEL) as? String, 40).ifEmpty { "Proceed to checkout" },
            // Not in the BCOS contract yet; the UX always shows it, so the widget supplies the default.
            showMoreLabel = if (secondary == null) "Show more restaurants" else FnbText.sanitizeInline(secondary[Keys.LABEL] as? String, 40).ifEmpty { null }
        )
    }

    private class MappedLine(val line: CartSummaryLine, val currency: String?)

    private fun mapLine(line: Map<String, Any?>): MappedLine? {
        val lineId = line[Keys.LINE_ID].asId() ?: return null
        val title = FnbText.sanitizeInline(line[Keys.NAME] as? String, FnbLimits.MAX_NAME_LENGTH)
        if (title.isEmpty()) return null
        val quantity = (line[Keys.QUANTITY] as? Number)?.toInt()?.takeIf { it > 0 } ?: return null
        val unit = money(line[Keys.UNIT_PRICE]) ?: 0L
        val location = line[Keys.LOCATION].asStringMap()
        return MappedLine(
            CartSummaryLine(
                lineId = lineId,
                productId = line[Keys.ENTITY_ID].asId().orEmpty(),
                title = title,
                quantity = quantity,
                pricePerCents = unit,
                subtotalCents = money(line[Keys.LINE_TOTAL]) ?: (unit * quantity),
                modifiersSummary = FnbText.sanitizeInline(line[Keys.MODIFIERS_SUMMARY] as? String, FnbLimits.MAX_NAME_LENGTH)
                    .ifEmpty { DEFAULT_MODIFIERS_SUMMARY },
                locationId = location?.get(Keys.ID).asId().orEmpty(),
                locationName = FnbText.sanitizeInline(location?.get(Keys.LABEL) as? String, FnbLimits.MAX_NAME_LENGTH),
                note = FnbText.sanitizeInline(line[Keys.NOTE] as? String, 140).ifEmpty { null },
                removable = line[Keys.ACTIONS].asStringMap()?.get(Keys.REMOVE) != null
            ),
            currencyOf(line[Keys.LINE_TOTAL]) ?: currencyOf(line[Keys.UNIT_PRICE])
        )
    }

    /** Money magnitude in cents; a discount sent as negative is shown as a magnitude. */
    private fun money(raw: Any?): Long? = CatalogMenuMapper.moneyCents(raw)?.let { kotlin.math.abs(it) }

    private fun currencyOf(raw: Any?): String? = (raw.asStringMap()?.get(Keys.CURRENCY) as? String)
        ?.trim()?.uppercase()?.takeIf { it.length == 3 && it.all(Char::isLetter) }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asStringMap(): Map<String, Any?>? =
        (this as? Map<*, *>)?.takeIf { map -> map.keys.all { it is String } } as? Map<String, Any?>

    private fun Any?.asId(): String? = when (this) {
        is String -> trim().takeIf { it.isNotEmpty() && it.length <= 64 }
        is Int, is Long -> toString()
        is Double -> if (isFinite() && this == Math.floor(this)) toLong().toString() else null
        is BigDecimal -> runCatching { longValueExact().toString() }.getOrNull()
        else -> null
    }
}

/** Only https checkout links on allowlisted hosts (or their subdomains) are opened. */
object CheckoutUrlPolicy {
    val DEFAULT_HOSTS: Set<String> = setOf("tapin2.co")

    fun isAllowed(url: String, allowedHosts: Set<String> = DEFAULT_HOSTS): Boolean {
        val uri = runCatching { java.net.URI(url.trim()) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.rawUserInfo != null) return false
        val host = uri.host?.lowercase() ?: return false
        return allowedHosts.any { allowed -> host == allowed || host.endsWith(".$allowed") }
    }
}
