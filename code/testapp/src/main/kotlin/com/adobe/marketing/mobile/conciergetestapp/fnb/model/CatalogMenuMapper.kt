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
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.long
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.obj
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.objs
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.TapinReads.name

/**
 * Maps an `fnb.menu` element's payload, the tapin2
 * `GET /v2/venues/{venueId}/locations/{locationId}/products` response body, into [MenuUiModel].
 * The payload is tapin2's raw response (a root array of entries), read with tapin2's names:
 *
 * `[{ orderId, isVisible, hideInMobile, isActive, locationId, categoryId,
 *     location{id, title, orderingEnabled, isPaused, isActive, waitTime, ...},
 *     category{id, title, orderId, isActive},
 *     product{id, title, description, price, originalPrice, eventPrice, imageUrl, isAlcohol,
 *       isActive, isArchived, modifierGroups[{id, title, minQuantity, maxQuantity,
 *       maxOnePerSelection, showPublic, modifiers[{id, title, priceDiff, isActive, isArchived}]}]},
 *     venueId?, eventId? }, ...]`
 *
 * `venueId`/`eventId` are not in the tapin2 response today (BC takes them from the conversation
 * session); they are read if tapin2 adds them. Everything tapin2 does not send (copy, limits,
 * currency) comes from [MenuOptions]. Hidden or inactive entries are dropped; grouping and ordering
 * by `category.orderId` then entry `orderId` happen here. BC may trim fields, but the shape stays
 * tapin2's.
 */
object CatalogMenuMapper {

    object Keys {
        const val ID = "id"

        // tapin2 products entry
        const val VENUE_ID = "venueId"
        const val EVENT_ID = "eventId"
        const val ORDER_ID = "orderId"
        const val IS_VISIBLE = "isVisible"
        const val HIDE_IN_MOBILE = "hideInMobile"
        const val IS_ACTIVE = "isActive"
        const val IS_ARCHIVED = "isArchived"
        const val LOCATION_ID = "locationId"
        const val LOCATION = "location"
        const val CATEGORY = "category"
        const val PRODUCT = "product"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val PRICE = "price"
        const val ORIGINAL_PRICE = "originalPrice"
        const val EVENT_PRICE = "eventPrice"
        const val IMAGE_URL = "imageUrl"
        const val IS_ALCOHOL = "isAlcohol"
        const val MODIFIER_GROUPS = "modifierGroups"
        const val MIN_QUANTITY = "minQuantity"
        const val MAX_QUANTITY = "maxQuantity"
        const val MAX_ONE_PER_SELECTION = "maxOnePerSelection"
        const val SHOW_PUBLIC = "showPublic"
        const val MODIFIERS = "modifiers"
        const val PRICE_DIFF = "priceDiff"

        // tapin2 location
        const val ORDERING_ENABLED = "orderingEnabled"
        const val IS_PAUSED = "isPaused"
        const val WAIT_TIME = "waitTime"
    }

    const val MAX_CATEGORIES = 50
    const val MAX_ITEMS = 500
    const val MAX_OPTION_GROUPS = 20
    const val MAX_OPTIONS_PER_GROUP = 50

    private class Row(val categoryOrder: Long?, val itemOrder: Long?, val categoryId: String, val categoryTitle: String, val item: MenuItem)

    /** Returns null when the payload is not a products array or no orderable entry remains. */
    fun map(payload: Any?, options: MenuOptions = MenuOptions()): MenuUiModel? {
        val entries = objs(payload)

        val seen = HashSet<String>()
        val rows = mutableListOf<Row>()
        var firstKept: Map<String, Any?>? = null
        for (entry in entries) {
            if (seen.size >= MAX_ITEMS) break
            if (!isOrderable(entry)) continue
            val category = obj(entry[Keys.CATEGORY]) ?: continue
            val categoryId = id(category[Keys.ID]) ?: id(entry["categoryId"]) ?: continue
            val categoryTitle = name(category[Keys.TITLE])
            if (categoryTitle.isEmpty()) continue
            val item = obj(entry[Keys.PRODUCT])?.let { mapItem(it, options) } ?: continue
            // The same product can appear on several menus of one location; show it once.
            if (!seen.add(item.id)) continue
            if (firstKept == null) firstKept = entry
            rows += Row(long(category[Keys.ORDER_ID]), long(entry[Keys.ORDER_ID]), categoryId, categoryTitle, item)
        }
        if (rows.isEmpty()) return null

        // Stable sorts: equal orderIds keep payload order.
        val categories = rows
            .groupBy { it.categoryId }
            .values
            .sortedWith(compareBy(nullsLast()) { group -> group.first().categoryOrder })
            .take(MAX_CATEGORIES)
            .map { group ->
                MenuCategory(
                    id = group.first().categoryId,
                    label = group.first().categoryTitle,
                    items = group.sortedWith(compareBy(nullsLast()) { it.itemOrder }).map { it.item }
                )
            }

        val location = obj(firstKept?.get(Keys.LOCATION))
        return MenuUiModel(
            locationId = (id(location?.get(Keys.ID)) ?: id(firstKept?.get(Keys.LOCATION_ID))).orEmpty(),
            locationName = name(location?.get(Keys.TITLE)),
            orderingAvailable = bool(location?.get(Keys.ORDERING_ENABLED)) != false &&
                bool(location?.get(Keys.IS_PAUSED)) != true &&
                bool(location?.get(Keys.IS_ACTIVE)) != false,
            categories = categories,
            currencyCode = options.currencyCode,
            venueId = id(firstKept?.get(Keys.VENUE_ID)).orEmpty(),
            eventId = id(firstKept?.get(Keys.EVENT_ID)).orEmpty(),
            cartBar = options.cartBar,
            waitTime = FnbText.sanitizeInline(location?.get(Keys.WAIT_TIME) as? String, 40).ifEmpty { null }
        )
    }

    private fun isOrderable(entry: Map<String, Any?>): Boolean {
        val product = obj(entry[Keys.PRODUCT]) ?: return false
        val category = obj(entry[Keys.CATEGORY])
        return bool(entry[Keys.IS_VISIBLE]) != false &&
            bool(entry[Keys.HIDE_IN_MOBILE]) != true &&
            bool(entry[Keys.IS_ACTIVE]) != false &&
            bool(product[Keys.IS_ACTIVE]) != false &&
            bool(product[Keys.IS_ARCHIVED]) != true &&
            bool(category?.get(Keys.IS_ACTIVE)) != false
    }

    private fun mapItem(product: Map<String, Any?>, options: MenuOptions): MenuItem? {
        val itemId = id(product[Keys.ID]) ?: return null
        val title = name(product[Keys.TITLE])
        if (title.isEmpty()) return null
        val price = cents(product[Keys.EVENT_PRICE]) ?: cents(product[Keys.PRICE]) ?: return null
        if (price < 0) return null
        val groups = objs(product[Keys.MODIFIER_GROUPS])
            .asSequence()
            .filter { bool(it[Keys.SHOW_PUBLIC]) != false }
            .mapNotNull(::mapOptionGroup)
            .take(MAX_OPTION_GROUPS)
            .toList()
        return MenuItem(
            id = itemId,
            name = title,
            description = FnbText.stripHtml(product[Keys.DESCRIPTION] as? String, FnbLimits.MAX_DESCRIPTION_LENGTH),
            priceCents = price,
            wasPriceCents = cents(product[Keys.ORIGINAL_PRICE])?.takeIf { it > price },
            imageUrl = FnbText.httpsUrlOrNull(product[Keys.IMAGE_URL] as? String),
            isAlcohol = bool(product[Keys.IS_ALCOHOL]) == true,
            optionGroups = groups,
            opensSheet = groups.isNotEmpty(),
            quantity = options.quantity,
            instructions = options.instructions,
            addToCartLabel = options.addToCartLabel
        )
    }

    /** Required when `minQuantity >= 1`; `maxQuantity <= 0` or absent means any number. */
    private fun mapOptionGroup(group: Map<String, Any?>): OptionGroup? {
        val groupId = id(group[Keys.ID]) ?: return null
        val title = name(group[Keys.TITLE])
        val choices = objs(group[Keys.MODIFIERS])
            .asSequence()
            .filter { bool(it[Keys.IS_ACTIVE]) != false && bool(it[Keys.IS_ARCHIVED]) != true }
            .mapNotNull { modifier ->
                val modifierId = id(modifier[Keys.ID]) ?: return@mapNotNull null
                val label = name(modifier[Keys.TITLE])
                if (label.isEmpty()) null else OptionChoice(modifierId, label, cents(modifier[Keys.PRICE_DIFF])?.coerceAtLeast(0) ?: 0L)
            }
            .distinctBy { it.id }
            .take(MAX_OPTIONS_PER_GROUP)
            .toList()
        if (title.isEmpty() || choices.isEmpty()) return null

        val minSelect = (int(group[Keys.MIN_QUANTITY])?.coerceAtLeast(0) ?: 0).coerceAtMost(choices.size)
        val rawMax = int(group[Keys.MAX_QUANTITY])
        val maxSelect = (if (rawMax == null || rawMax <= 0) choices.size else rawMax)
            .coerceAtMost(choices.size)
            .coerceAtLeast(maxOf(minSelect, 1))
        return OptionGroup(
            id = groupId,
            title = title,
            required = minSelect >= 1,
            minSelect = minSelect,
            maxSelect = maxSelect,
            options = choices,
            maxPerOption = if (bool(group[Keys.MAX_ONE_PER_SELECTION]) == false) maxSelect else 1
        )
    }
}
