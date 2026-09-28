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

import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.bool
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.cents
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.id
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.int
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.map
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.maps
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.name

/**
 * Maps the `cartView` element into [CartSummaryUiModel]. Its `entity_info` is a subset of the
 * tapin2 order returned by `POST /v2/cart/add` (and update/remove), with tapin2's names:
 *
 * `{ id, idLast3, guid, venueId, eventId, orderStatus, locationId, venue{id, title, taxRate,
 *    maxAlcoholPerOrder}, items[{id, locationId, product{id, title, imageUrl, isAlcohol}, quantity,
 *    pricePer, subtotal, taxAdded, total, modifier, modifiers, note}], distinctLocations[{id,
 *    title, section}], subtotalNet, taxAddedNet, feeAddedNet, discountNet, tipNet, totalNet,
 *    containsAlcohol, isPaidInFull }`
 *
 * tapin2 has no checkout link in the order (`receiptUrl` is not checkout), so the Review page URL
 * is built here from `venueId`, `eventId` and `guid` on [CartOptions.checkoutBaseUrl].
 */
object CartSummaryMapper {

    object Keys {
        const val TYPE = "type"
        const val ENTITY_ID = "entityId"
        const val ENTITY_INFO = "entity_info"
        const val TYPE_CART_VIEW = "cartView"

        const val ID = "id"
        const val ORDER_CODE = "idLast3"
        const val GUID = "guid"
        const val VENUE_ID = "venueId"
        const val EVENT_ID = "eventId"
        const val VENUE = "venue"
        const val TITLE = "title"
        const val ITEMS = "items"
        const val LOCATION_ID = "locationId"
        const val PRODUCT = "product"
        const val QUANTITY = "quantity"
        const val PRICE_PER = "pricePer"
        const val SUBTOTAL = "subtotal"
        const val MODIFIER = "modifier"
        const val MODIFIERS = "modifiers"
        const val NAME = "name"
        const val NOTE = "note"
        const val DISTINCT_LOCATIONS = "distinctLocations"
        const val LOCATION = "location"
        const val SUBTOTAL_NET = "subtotalNet"
        const val TAX_NET = "taxAddedNet"
        const val FEES_NET = "feeAddedNet"
        const val DISCOUNT_NET = "discountNet"
        const val TIP_NET = "tipNet"
        const val TOTAL_NET = "totalNet"
        const val IS_PAID = "isPaidInFull"
        const val CONTAINS_ALCOHOL = "containsAlcohol"
    }

    const val MAX_LINES = 50
    private const val MAX_MODIFIERS = 10
    private const val NO_MODIFIERS = "No modifiers"

    /** Maps the first `cartView` element; null when there is none or it has no order id. */
    fun map(elements: List<Map<String, Any?>>, options: CartOptions = CartOptions()): CartSummaryUiModel? {
        val element = elements.firstOrNull { it[Keys.TYPE] == Keys.TYPE_CART_VIEW } ?: return null
        val order = map(element[Keys.ENTITY_INFO]) ?: return null
        val orderId = id(order[Keys.ID]) ?: id(element[Keys.ENTITY_ID]) ?: return null
        val locations = (maps(order[Keys.DISTINCT_LOCATIONS]) + listOfNotNull(map(order[Keys.LOCATION])))
            .mapNotNull { location -> id(location[Keys.ID])?.let { it to name(location[Keys.TITLE]) } }
            .toMap()
        val lines = maps(order[Keys.ITEMS]).asSequence().mapNotNull { mapLine(it, locations) }.take(MAX_LINES).toList()
        val lineSum = lines.sumOf { it.subtotalCents }
        val guid = (order[Keys.GUID] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        val isPaid = bool(order[Keys.IS_PAID]) == true
        return CartSummaryUiModel(
            orderId = orderId,
            guid = guid,
            orderCode = FnbText.sanitizeInline(order[Keys.ORDER_CODE]?.toString(), 12).ifEmpty { null },
            venueName = name(map(order[Keys.VENUE])?.get(Keys.TITLE)),
            lines = lines.map { it.copy(removable = !isPaid) },
            subtotalCents = money(order[Keys.SUBTOTAL_NET]) ?: lineSum,
            taxCents = money(order[Keys.TAX_NET]) ?: 0L,
            feesCents = money(order[Keys.FEES_NET]) ?: 0L,
            discountCents = money(order[Keys.DISCOUNT_NET]) ?: 0L,
            tipCents = money(order[Keys.TIP_NET]) ?: 0L,
            totalCents = money(order[Keys.TOTAL_NET]) ?: lineSum,
            isPaid = isPaid,
            containsAlcohol = bool(order[Keys.CONTAINS_ALCOHOL]) == true,
            checkoutUrl = checkoutUrl(options, id(order[Keys.VENUE_ID]), id(order[Keys.EVENT_ID]), guid),
            currencyCode = options.currencyCode,
            checkoutLabel = options.checkoutLabel,
            showMoreLabel = options.showMoreLabel
        )
    }

    /** tapin2 Review page: `{base}/Review/Index/{venueId}?eventId={eventId}&orderId={guid}`. */
    fun checkoutUrl(options: CartOptions, venueId: String?, eventId: String?, guid: String?): String? {
        if (venueId == null || eventId == null || guid == null) return null
        if (!guid.all { it.isLetterOrDigit() || it == '-' }) return null
        val url = "${options.checkoutBaseUrl.trimEnd('/')}/Review/Index/$venueId?eventId=$eventId&orderId=$guid"
        return url.takeIf { CheckoutUrlPolicy.isAllowed(it, options.allowedCheckoutHosts) }
    }

    private fun mapLine(item: Map<String, Any?>, locations: Map<String, String>): CartSummaryLine? {
        val itemId = id(item[Keys.ID]) ?: return null
        val product = map(item[Keys.PRODUCT])
        val title = name(product?.get(Keys.TITLE))
        if (title.isEmpty()) return null
        val quantity = int(item[Keys.QUANTITY])?.takeIf { it > 0 } ?: return null
        val pricePer = money(item[Keys.PRICE_PER]) ?: 0L
        val locationId = id(item[Keys.LOCATION_ID]).orEmpty()
        return CartSummaryLine(
            itemId = itemId,
            productId = id(product?.get(Keys.ID)).orEmpty(),
            title = title,
            quantity = quantity,
            pricePerCents = pricePer,
            subtotalCents = money(item[Keys.SUBTOTAL]) ?: (pricePer * quantity),
            modifiersSummary = modifiersSummary(item),
            locationId = locationId,
            locationName = locations[locationId].orEmpty(),
            note = FnbText.sanitizeInline(item[Keys.NOTE] as? String, 140).ifEmpty { null }
        )
    }

    /**
     * tapin2 has `items[].modifier` (string) and `items[].modifiers` (list); both were empty in the
     * stage sample, which had no modifiers. Prefer the list, fall back to the string.
     */
    private fun modifiersSummary(item: Map<String, Any?>): String {
        val fromList = (item[Keys.MODIFIERS] as? List<*>).orEmpty().mapNotNull { modifier ->
            when (modifier) {
                is String -> modifier
                is Map<*, *> -> (modifier[Keys.TITLE] ?: modifier[Keys.NAME]) as? String
                else -> null
            }
        }
        val labels = fromList.ifEmpty { (item[Keys.MODIFIER] as? String).orEmpty().split(',') }
            .map { FnbText.sanitizeInline(it, FnbLimits.MAX_NAME_LENGTH) }
            .filter { it.isNotEmpty() }
            .take(MAX_MODIFIERS)
        return labels.joinToString(", ").ifEmpty { NO_MODIFIERS }
    }

    /** Magnitude in cents; a discount sent as negative is shown as a magnitude. */
    private fun money(value: Any?): Long? = cents(value)?.let { kotlin.math.abs(it) }
}

/** Widget-side values the tapin2 order does not carry. */
data class CartOptions(
    /** tapin2 mobile web host; production is `https://mobile.tapin2.co`. */
    val checkoutBaseUrl: String = "https://mobile.tapin2.co",
    val allowedCheckoutHosts: Set<String> = CheckoutUrlPolicy.DEFAULT_HOSTS,
    val currencyCode: String = MenuUiModel.DEFAULT_CURRENCY_CODE,
    val checkoutLabel: String = "Proceed to checkout",
    val showMoreLabel: String? = "Show more restaurants"
) {
    companion object {
        val STAGE = CartOptions(checkoutBaseUrl = "https://mobile-stg.tapin2.co")
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
