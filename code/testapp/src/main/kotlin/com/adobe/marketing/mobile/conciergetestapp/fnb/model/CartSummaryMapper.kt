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

import com.adobe.marketing.mobile.conciergetestapp.common.PayloadLimits
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadText
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.bool
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.cents
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.id
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.int
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.obj
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.name
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.objectSequence

/**
 * Maps a `kings.fnbcart` element's payload, the tapin2 order returned by `POST /v2/cart/add` (and by
 * update/remove), into [CartSummaryUiModel]. The payload is tapin2's response body, read with
 * tapin2's names (other fields, including `userInfo`, are ignored; BC should drop PII):
 *
 * `{ id, idLast3, guid, venueId, eventId, orderStatus, locationId, venue{id, title, taxRate,
 *    maxAlcoholPerOrder}, items[{id, locationId, product{id, title, imageUrl, isAlcohol}, quantity,
 *    pricePer, subtotal, taxAdded, total, modifier, modifiers, note}], distinctLocations[{id,
 *    title, section}], subtotalNet, taxAddedNet, feeAddedNet, discountNet, tipNet, totalNet,
 *    containsAlcohol, isPaidInFull }`
 *
 * tapin2 has no checkout link in the order (`receiptUrl` is not checkout), so the Review page URL
 * is built here from the checkout venue, `eventId` and `guid` on [CartOptions.checkoutBaseUrl].
 */
object CartSummaryMapper {

    object Keys {
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
    private const val MAX_RAW_LINES = 500
    private const val MAX_DISTINCT_LOCATIONS = 100
    private const val MAX_RAW_MODIFIERS = 100
    private const val MAX_MODIFIERS = 10
    private const val NO_MODIFIERS = "No modifiers"

    /**
     * Returns null when the payload is not an order object or has no order id.
     * @param entityId the envelope's `entityId`, used when the order omits `id`. A literal `"null"`
     * is ignored: BCOS builds it with `to_string(result.orderId)`, a field tapin2 doesn't return.
     */
    fun map(payload: Any?, options: CartOptions = CartOptions(), entityId: String? = null): CartSummaryUiModel? {
        val order = obj(payload) ?: return null
        val orderId = id(order[Keys.ID]) ?: id(entityId)?.takeUnless { it.equals("null", ignoreCase = true) } ?: return null
        val locationSequence = sequence {
            yieldAll(objectSequence(order[Keys.DISTINCT_LOCATIONS], MAX_DISTINCT_LOCATIONS))
            obj(order[Keys.LOCATION])?.let { yield(it) }
        }
        val locations = locationSequence
            .mapNotNull { location -> id(location[Keys.ID])?.let { it to name(location[Keys.TITLE]) } }
            .toMap()
        val lines = objectSequence(order[Keys.ITEMS], MAX_RAW_LINES)
            .mapNotNull { mapLine(it, locations) }
            .take(MAX_LINES)
            .toList()
        val lineSum = lines.fold(0L) { total, line -> FnbMoneyMath.add(total, line.subtotalCents) }
        val guid = (order[Keys.GUID] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        val isPaid = bool(order[Keys.IS_PAID]) == true
        return CartSummaryUiModel(
            orderId = orderId,
            guid = guid,
            orderCode = PayloadText.sanitizeInline(order[Keys.ORDER_CODE]?.toString(), 12).ifEmpty { null },
            venueName = name(obj(order[Keys.VENUE])?.get(Keys.TITLE)),
            lines = lines.map { it.copy(removable = !isPaid) },
            subtotalCents = money(order[Keys.SUBTOTAL_NET]) ?: lineSum,
            taxCents = money(order[Keys.TAX_NET]) ?: 0L,
            feesCents = money(order[Keys.FEES_NET]) ?: 0L,
            discountCents = discount(order[Keys.DISCOUNT_NET]) ?: 0L,
            tipCents = money(order[Keys.TIP_NET]) ?: 0L,
            totalCents = money(order[Keys.TOTAL_NET]) ?: lineSum,
            isPaid = isPaid,
            containsAlcohol = bool(order[Keys.CONTAINS_ALCOHOL]) == true,
            checkoutUrl = checkoutUrl(
                options,
                options.checkoutVenueId ?: id(order[Keys.VENUE_ID]),
                id(order[Keys.EVENT_ID]),
                guid
            ),
            currencyCode = options.currencyCode,
            checkoutLabel = options.checkoutLabel,
            showRestaurantsLabel = options.showRestaurantsLabel
        )
    }

    /**
     * tapin2 Review page: `{base}/Review/Index/{venueId}?eventId={eventId}&orderId={guid}`. Every
     * value must be a plain id ([isUrlSafeId]); anything else gives no link rather than an altered one.
     */
    fun checkoutUrl(options: CartOptions, venueId: String?, eventId: String?, guid: String?): String? {
        if (venueId == null || eventId == null || guid == null) return null
        if (!isUrlSafeId(venueId) || !isUrlSafeId(eventId) || !isUrlSafeId(guid)) return null
        val url = "${options.checkoutBaseUrl.trimEnd('/')}/Review/Index/$venueId?eventId=$eventId&orderId=$guid"
        return url.takeIf { CheckoutUrlPolicy.isAllowed(it, options.allowedCheckoutHosts) }
    }

    private fun mapLine(item: Map<String, Any?>, locations: Map<String, String>): CartSummaryLine? {
        val itemId = id(item[Keys.ID]) ?: return null
        val product = obj(item[Keys.PRODUCT])
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
            subtotalCents = money(item[Keys.SUBTOTAL]) ?: FnbMoneyMath.multiply(pricePer, quantity.toLong()),
            modifiersSummary = modifiersSummary(item),
            locationId = locationId,
            locationName = locations[locationId].orEmpty(),
            note = PayloadText.sanitizeInline(item[Keys.NOTE] as? String, 140).ifEmpty { null }
        )
    }

    /**
     * tapin2 has `items[].modifier` (string) and `items[].modifiers` (list); both were empty in the
     * stage sample, which had no modifiers. Prefer the list, fall back to the string.
     */
    private fun modifiersSummary(item: Map<String, Any?>): String {
        val fromList = (item[Keys.MODIFIERS] as? List<*>).orEmpty().asSequence().take(MAX_RAW_MODIFIERS).mapNotNull { modifier ->
            when (modifier) {
                is String -> modifier
                is Map<*, *> -> (modifier[Keys.TITLE] ?: modifier[Keys.NAME]) as? String
                else -> null
            }
        }.toList()
        val labels = fromList.ifEmpty { (item[Keys.MODIFIER] as? String).orEmpty().split(',') }
            .map { PayloadText.sanitizeInline(it, PayloadLimits.MAX_NAME_LENGTH) }
            .filter { it.isNotEmpty() }
            .take(MAX_MODIFIERS)
        return labels.joinToString(", ").ifEmpty { NO_MODIFIERS }
    }

    /** ASCII letters, digits and `-`, at most 64 chars: safe in a URL path segment or query value. */
    fun isUrlSafeId(value: String): Boolean =
        value.length in 1..64 && value.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '-' }

    /** Amount in cents. Negative amounts aren't valid for an order and read as missing. */
    private fun money(value: Any?): Long? = cents(value)?.takeIf { it >= 0 }

    /** tapin2 may send a discount as negative; it's shown as a magnitude. */
    private fun discount(value: Any?): Long? = cents(value)?.takeUnless { it == Long.MIN_VALUE }?.let { kotlin.math.abs(it) }

}

/** Widget-side values the tapin2 order does not carry. */
data class CartOptions(
    /** tapin2 mobile web host; production is `https://mobile.tapin2.co`. */
    val checkoutBaseUrl: String = "https://mobile.tapin2.co",
    /** Fixed checkout venue; null uses the order's `venueId`. */
    val checkoutVenueId: String? = null,
    val allowedCheckoutHosts: Set<String> = CheckoutUrlPolicy.DEFAULT_HOSTS,
    val currencyCode: String = MenuUiModel.DEFAULT_CURRENCY_CODE,
    val checkoutLabel: String = "Proceed to checkout",
    val showRestaurantsLabel: String? = "Show restaurants"
) {
    companion object {
        val STAGE = CartOptions(checkoutBaseUrl = "https://mobile-stg.tapin2.co", checkoutVenueId = "1000010528")
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
