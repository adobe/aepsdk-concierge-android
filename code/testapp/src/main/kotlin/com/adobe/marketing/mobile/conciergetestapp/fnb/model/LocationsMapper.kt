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

import com.adobe.marketing.mobile.conciergetestapp.common.PayloadText
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadLimits
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCard
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardCta
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardList
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbHandoffFormatter
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.bool
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.id
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.long
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.name
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.objectSequence

/**
 * Maps a `kings.fnblocation` element's payload into a [SimpleCardList]. The payload is tapin2's
 * raw section-locations body: BC emits one element per stand, so it is usually a single location
 * object, but an array of them is read the same way. Either way it is read with tapin2's names:
 *
 * `[{ id, title, description, section, imageUrl, imageUrlMobile, orderingEnabled, isActive,
 *     isPaused, orderId, isPickup, isDelivery, waitTime, ... }]`
 *
 * A stand tapin2 marks inactive is dropped outright; a stand that is only paused or has ordering
 * turned off is kept and rendered closed, so the fan can see the whole section.
 */
object LocationsMapper {

    object Keys {
        const val ID = "id"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val SECTION = "section"
        const val IMAGE_URL = "imageUrl"
        const val IMAGE_URL_MOBILE = "imageUrlMobile"
        const val ORDERING_ENABLED = "orderingEnabled"
        const val IS_ACTIVE = "isActive"
        const val IS_PAUSED = "isPaused"
        const val ORDER_ID = "orderId"
        const val IS_PICKUP = "isPickup"
        const val IS_DELIVERY = "isDelivery"
        const val WAIT_TIME = "waitTime"
    }

    /** Keys BC may use if it ever wraps the array; a bare object or array is the contract. */
    private val ROOT_ARRAY_KEYS = listOf("locations", "results", "data")

    const val MAX_STANDS = 50
    private const val MAX_RAW_ENTRIES = 500
    private const val MAX_SECTION_LENGTH = 16
    private const val MAX_WAIT_TIME_LENGTH = 40

    /** Intent BC resolves to the stand's menu lookup. */
    const val INTENT_SHOW_MENU = "view-menu"

    fun mapElement(element: Map<String, Any?>, options: LocationOptions = LocationOptions()): SimpleCardList? =
        map(element["entity_info"], options, long(element["eventId"])?.takeIf { it >= 0 })

    /** Returns null when the payload is not a tapin2 locations array with a usable stand. */
    fun map(payload: Any?, options: LocationOptions = LocationOptions(), eventId: Long? = null): SimpleCardList? {
        val seen = HashSet<String>()
        val rows = mutableListOf<Pair<Long?, SimpleCard>>()
        for (entry in objectSequence(rootArray(payload), MAX_RAW_ENTRIES)) {
            if (rows.size >= MAX_STANDS) break
            // tapin2 sets isActive=false for a stand that isn't part of this event at all.
            if (bool(entry[Keys.IS_ACTIVE]) == false) continue
            val locationId = id(entry[Keys.ID]) ?: continue
            val title = name(entry[Keys.TITLE])
            if (title.isEmpty()) continue
            if (!seen.add(locationId)) continue

            val isOrderable = bool(entry[Keys.ORDERING_ENABLED]) != false && bool(entry[Keys.IS_PAUSED]) != true
            val sectionLabel = id(entry[Keys.SECTION])
                ?.let { PayloadText.sanitizeInline(it, MAX_SECTION_LENGTH) }
                ?.takeIf { it.isNotEmpty() }
                ?.let { options.sectionLabelPrefix + it }
            val waitTime = PayloadText.sanitizeInline(entry[Keys.WAIT_TIME] as? String, MAX_WAIT_TIME_LENGTH).ifEmpty { null }

            rows += long(entry[Keys.ORDER_ID]) to SimpleCard(
                id = locationId,
                title = title,
                description = PayloadText.stripHtml(entry[Keys.DESCRIPTION] as? String, PayloadLimits.MAX_DESCRIPTION_LENGTH),
                imageUrl = PayloadText.httpsUrlOrNull(entry[Keys.IMAGE_URL_MOBILE] as? String)
                    ?: PayloadText.httpsUrlOrNull(entry[Keys.IMAGE_URL] as? String),
                // A closed stand says so where an open one shows its section.
                badge = if (isOrderable) sectionLabel else options.closedLabel,
                detail = detailLine(
                    isOrderable = isOrderable,
                    waitTime = waitTime,
                    isPickup = bool(entry[Keys.IS_PICKUP]) != false,
                    isDelivery = bool(entry[Keys.IS_DELIVERY]) == true,
                    closedLabel = options.closedLabel
                ),
                enabled = isOrderable,
                ctas = listOf(
                    SimpleCardCta(
                        label = options.ctaLabel,
                        intentName = INTENT_SHOW_MENU,
                        parameters = buildMap {
                            put("locationId", locationId)
                            eventId?.takeIf { it >= 0 }?.let { put("eventId", it) }
                        },
                        localMessage = FnbHandoffFormatter.selectLocationMessage(title),
                        pendingLabel = options.pendingCtaLabel
                    )
                )
            )
        }
        if (rows.isEmpty()) return null

        // Stable sort: equal orderIds keep payload order, and stands without one go last.
        val cards = rows.sortedWith(compareBy(nullsLast()) { it.first }).map { it.second }
        return SimpleCardList(
            cards = cards,
            // tapin2 ships most stands with no artwork at all; an image slot none of them can fill
            // is a card-sized hole, so the whole list drops it and the text leads instead.
            showImages = cards.any { it.imageUrl != null }
        )
    }

    private fun detailLine(
        isOrderable: Boolean,
        waitTime: String?,
        isPickup: Boolean,
        isDelivery: Boolean,
        closedLabel: String
    ): String? = when {
        !isOrderable -> "$closedLabel right now"
        waitTime != null && isDelivery -> "$waitTime · Delivery"
        waitTime != null -> waitTime
        isDelivery -> "Delivery to your seat"
        isPickup -> "Pickup"
        else -> null
    }

    private fun rootArray(payload: Any?): Any? = when (payload) {
        is List<*> -> payload
        // BC emits one element per stand, so the payload is usually a single location object.
        is Map<*, *> -> ROOT_ARRAY_KEYS.firstNotNullOfOrNull { key -> payload[key] as? List<*> }
            ?: payload.takeIf { it[Keys.ID] != null && it[Keys.TITLE] != null }?.let { listOf(it) }
        else -> null
    }
}
