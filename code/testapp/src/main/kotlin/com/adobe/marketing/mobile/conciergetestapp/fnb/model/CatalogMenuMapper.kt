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
 * Maps BCOS menu elements into [MenuUiModel]: one `catalogItemCard` per product plus an optional
 * `cartBar`, with all data in `entity_info`. This is the only code coupled to that shape; every
 * field name lives in [Keys] so a contract change is a one-file edit.
 *
 * Input is the element list as plain maps (`{id, entityId, type, cardType, entity_info}`), so the
 * mapper stays independent of the renderer contract and JVM-testable. Reads are defensive: wrong
 * types or missing fields drop the affected item/group instead of throwing.
 */
object CatalogMenuMapper {

    object Keys {
        const val ID = "id"
        const val ENTITY_ID = "entityId"
        const val TYPE = "type"
        const val ENTITY_INFO = "entity_info"

        const val TYPE_CATALOG_ITEM = "catalogItemCard"
        const val TYPE_CART_BAR = "cartBar"

        // catalogItemCard.entity_info
        const val PRODUCT_NAME = "productName"
        const val PRODUCT_DESCRIPTION = "productDescription"
        const val PRODUCT_IMAGE_URL = "productImageURL"
        const val SUBTITLE = "subtitle"
        const val TAGS = "tags"
        const val LABEL = "label"
        const val UNIT_PRICE = "unitPrice"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val AVAILABLE = "available"
        const val CATEGORY_ID = "categoryId"
        const val CATEGORY_LABEL = "categoryLabel"
        const val VENUE_ID = "venueId"
        const val EVENT_ID = "eventId"
        const val LOCATION_ID = "locationId"
        const val LOCATION_LABEL = "locationLabel"
        const val HAS_OPTIONS = "hasOptions"
        const val ACTION = "action"
        const val ACTION_OPEN_SHEET = "OPEN_SHEET"
        const val SHEET = "sheet"

        // sheet
        const val OPTION_GROUPS = "optionGroups"
        const val TITLE = "title"
        const val MIN_SELECTIONS = "minSelections"
        const val MAX_SELECTIONS = "maxSelections"
        const val MAX_PER_OPTION = "maxPerOption"
        const val OPTIONS = "options"
        const val PRICE_DELTA = "priceDelta"
        const val SPECIAL_INSTRUCTIONS = "specialInstructions"
        const val ENABLED = "enabled"
        const val PLACEHOLDER = "placeholder"
        const val MAX_LENGTH = "maxLength"
        const val QUANTITY = "quantity"
        const val MIN = "min"
        const val MAX = "max"
        const val DEFAULT = "default"
        const val STEP = "step"
        const val ADD_TO_CART = "addToCart"

        // cartBar.entity_info
        const val SHOW_COUNT = "showCount"
        const val HIDE_WHEN_EMPTY = "hideWhenEmpty"

        // Not in the BCOS sample; honored if BCOS adds them.
        const val WAS_PRICE = "wasPrice"
        const val IS_ALCOHOL = "isAlcohol"
        const val IS_DEFAULT = "isDefault"
    }

    const val MAX_CATEGORIES = 50
    const val MAX_ITEMS = 500
    const val MAX_OPTION_GROUPS = 20
    const val MAX_OPTIONS_PER_GROUP = 50
    private const val MAX_TAG_LENGTH = 24
    private const val MAX_METADATA_ENTRIES = 4
    private const val MAX_INSTRUCTIONS_LENGTH = 500

    /** Returns null when there is no usable `catalogItemCard`. */
    fun map(elements: List<Map<String, Any?>>): MenuUiModel? {
        val cards = elements.filter { it[Keys.TYPE] == Keys.TYPE_CATALOG_ITEM }
        val cartBarInfo = elements.firstOrNull { it[Keys.TYPE] == Keys.TYPE_CART_BAR }?.get(Keys.ENTITY_INFO).asStringMap()

        val buckets = LinkedHashMap<String, Pair<String, MutableList<MenuItem>>>()
        val seenIds = HashSet<String>()
        var first: Map<String, Any?>? = null
        var currency: String? = null
        for (card in cards) {
            if (seenIds.size >= MAX_ITEMS) break
            val info = card[Keys.ENTITY_INFO].asStringMap() ?: continue
            val item = mapItem(card, info) ?: continue
            if (!seenIds.add(item.id)) continue
            val categoryId = info[Keys.CATEGORY_ID].asId() ?: continue
            val label = FnbText.sanitizeInline(info[Keys.CATEGORY_LABEL] as? String, FnbLimits.MAX_NAME_LENGTH)
            if (label.isEmpty()) continue
            if (first == null) first = info
            if (currency == null) currency = currencyOf(info[Keys.UNIT_PRICE])
            if (buckets.size >= MAX_CATEGORIES && categoryId !in buckets) continue
            buckets.getOrPut(categoryId) { label to mutableListOf() }.second += item
        }
        if (buckets.isEmpty()) return null

        return MenuUiModel(
            locationId = first?.get(Keys.LOCATION_ID).asId().orEmpty(),
            locationName = FnbText.sanitizeInline(first?.get(Keys.LOCATION_LABEL) as? String, FnbLimits.MAX_NAME_LENGTH),
            // The cartBar's `enabled` is the only location-level ordering signal in the contract.
            orderingAvailable = cartBarInfo?.get(Keys.ENABLED).asBoolean() != false,
            categories = buckets.map { (id, bucket) -> MenuCategory(id, bucket.first, bucket.second) },
            currencyCode = currency ?: MenuUiModel.DEFAULT_CURRENCY_CODE,
            venueId = (cartBarInfo?.get(Keys.VENUE_ID) ?: first?.get(Keys.VENUE_ID)).asId().orEmpty(),
            eventId = (cartBarInfo?.get(Keys.EVENT_ID) ?: first?.get(Keys.EVENT_ID)).asId().orEmpty(),
            cartBar = cartBarInfo?.let(::mapCartBar) ?: CartBarConfig()
        )
    }

    private fun mapItem(card: Map<String, Any?>, info: Map<String, Any?>): MenuItem? {
        val id = (card[Keys.ENTITY_ID] ?: card[Keys.ID]).asId() ?: return null
        val sheet = info[Keys.SHEET].asStringMap()
        val name = FnbText.sanitizeInline(info[Keys.PRODUCT_NAME] as? String, FnbLimits.MAX_NAME_LENGTH)
        if (name.isEmpty()) return null
        val price = moneyCents(info[Keys.UNIT_PRICE]) ?: moneyCents(sheet?.get(Keys.UNIT_PRICE)) ?: return null
        if (price < 0) return null
        val groups = mapOptionGroups(sheet?.get(Keys.OPTION_GROUPS) as? List<*>)
        val actionType = info[Keys.ACTION].asStringMap()?.get(Keys.TYPE) as? String
        return MenuItem(
            id = id,
            name = name,
            description = FnbText.stripHtml(
                (info[Keys.PRODUCT_DESCRIPTION] ?: sheet?.get(Keys.PRODUCT_DESCRIPTION)) as? String,
                FnbLimits.MAX_DESCRIPTION_LENGTH
            ),
            metadata = metadata(info[Keys.SUBTITLE] as? String),
            priceCents = price,
            wasPriceCents = moneyCents(info[Keys.WAS_PRICE])?.takeIf { it > price },
            imageUrl = FnbText.httpsUrlOrNull(info[Keys.PRODUCT_IMAGE_URL] as? String),
            tag = (info[Keys.TAGS] as? List<*>).orEmpty()
                .firstNotNullOfOrNull { tag -> (tag.asStringMap()?.get(Keys.LABEL) as? String)?.let { FnbText.sanitizeInline(it, MAX_TAG_LENGTH) } }
                ?.ifEmpty { null },
            isAlcohol = info[Keys.IS_ALCOHOL].asBoolean() == true,
            optionGroups = groups,
            available = info[Keys.AVAILABLE].asBoolean() != false,
            opensSheet = if (actionType != null) actionType == Keys.ACTION_OPEN_SHEET else (info[Keys.HAS_OPTIONS].asBoolean() == true || groups.isNotEmpty()),
            quantity = quantityRule(sheet?.get(Keys.QUANTITY).asStringMap()),
            instructions = instructions(sheet?.get(Keys.SPECIAL_INSTRUCTIONS).asStringMap()),
            addToCartLabel = FnbText.sanitizeInline(sheet?.get(Keys.ADD_TO_CART).asStringMap()?.get(Keys.LABEL) as? String, 40)
                .ifEmpty { "Add to order" }
        )
    }

    /** "Vegetarian · Shareable" → ["Vegetarian", "Shareable"]. */
    private fun metadata(subtitle: String?): List<String> =
        FnbText.sanitizeInline(subtitle, FnbLimits.MAX_NAME_LENGTH)
            .split('·').map { it.trim() }.filter { it.isNotEmpty() }.take(MAX_METADATA_ENTRIES)

    private fun mapOptionGroups(raw: List<*>?): List<OptionGroup> = raw.orEmpty()
        .asSequence()
        .mapNotNull { it.asStringMap() }
        .mapNotNull(::mapOptionGroup)
        .take(MAX_OPTION_GROUPS)
        .toList()

    /** Required when `minSelections >= 1`; `maxSelections <= 0` or absent means any number. */
    private fun mapOptionGroup(group: Map<String, Any?>): OptionGroup? {
        val id = group[Keys.ID].asId() ?: return null
        val title = FnbText.sanitizeInline(group[Keys.TITLE] as? String, FnbLimits.MAX_NAME_LENGTH)
        val options = (group[Keys.OPTIONS] as? List<*>).orEmpty()
            .asSequence()
            .mapNotNull { it.asStringMap() }
            .mapNotNull { option ->
                val optionId = option[Keys.ID].asId() ?: return@mapNotNull null
                val label = FnbText.sanitizeInline(option[Keys.LABEL] as? String, FnbLimits.MAX_NAME_LENGTH)
                if (label.isEmpty()) return@mapNotNull null
                OptionChoice(
                    id = optionId,
                    label = label,
                    priceDeltaCents = moneyCents(option[Keys.PRICE_DELTA])?.coerceAtLeast(0) ?: 0L,
                    isDefault = option[Keys.IS_DEFAULT].asBoolean() == true,
                    available = option[Keys.AVAILABLE].asBoolean() != false
                )
            }
            .distinctBy { it.id }
            .take(MAX_OPTIONS_PER_GROUP)
            .toList()
        if (title.isEmpty() || options.isEmpty()) return null

        val rawMin = group[Keys.MIN_SELECTIONS].asLong()?.toInt()?.coerceAtLeast(0) ?: 0
        val minSelect = rawMin.coerceAtMost(options.size)
        val rawMax = group[Keys.MAX_SELECTIONS].asLong()?.toInt()
        val maxSelect = (if (rawMax == null || rawMax <= 0) options.size else rawMax)
            .coerceAtMost(options.size)
            .coerceAtLeast(maxOf(minSelect, 1))
        return OptionGroup(
            id = id,
            title = title,
            required = minSelect >= 1,
            minSelect = minSelect,
            maxSelect = maxSelect,
            options = options,
            maxPerOption = group[Keys.MAX_PER_OPTION].asLong()?.toInt()?.coerceAtLeast(1) ?: 1
        )
    }

    private fun quantityRule(raw: Map<String, Any?>?): QuantityRule {
        if (raw == null) return QuantityRule()
        val min = raw[Keys.MIN].asLong()?.toInt()?.coerceAtLeast(1) ?: 1
        val max = (raw[Keys.MAX].asLong()?.toInt() ?: QuantityRule().max).coerceIn(min, CartReducer.MAX_LINE_QUANTITY)
        val step = (raw[Keys.STEP].asLong()?.toInt() ?: 1).coerceIn(1, max)
        val default = (raw[Keys.DEFAULT].asLong()?.toInt() ?: min).coerceIn(min, max)
        return QuantityRule(min = min, max = max, default = default, step = step)
    }

    private fun instructions(raw: Map<String, Any?>?): InstructionsConfig? {
        if (raw == null || raw[Keys.ENABLED].asBoolean() != true) return null
        return InstructionsConfig(
            label = FnbText.sanitizeInline(raw[Keys.LABEL] as? String, 60).ifEmpty { "Special instructions" },
            placeholder = FnbText.sanitizeInline(raw[Keys.PLACEHOLDER] as? String, 80),
            maxLength = (raw[Keys.MAX_LENGTH].asLong()?.toInt() ?: 140).coerceIn(1, MAX_INSTRUCTIONS_LENGTH)
        )
    }

    private fun mapCartBar(info: Map<String, Any?>): CartBarConfig = CartBarConfig(
        label = FnbText.sanitizeInline(info[Keys.LABEL] as? String, 40).ifEmpty { "Add to cart" },
        showCount = info[Keys.SHOW_COUNT].asBoolean() != false,
        hideWhenEmpty = info[Keys.HIDE_WHEN_EMPTY].asBoolean() == true,
        enabled = info[Keys.ENABLED].asBoolean() != false
    )

    /** Money objects are `{amount, currency, display}`; `amount` is authoritative for math. */
    internal fun moneyCents(raw: Any?): Long? = FnbText.toCents(raw.asStringMap()?.get(Keys.AMOUNT))

    private fun currencyOf(raw: Any?): String? = (raw.asStringMap()?.get(Keys.CURRENCY) as? String)
        ?.trim()?.uppercase()?.takeIf { it.length == 3 && it.all(Char::isLetter) }

    @Suppress("UNCHECKED_CAST")
    private fun Any?.asStringMap(): Map<String, Any?>? =
        (this as? Map<*, *>)?.takeIf { map -> map.keys.all { it is String } } as? Map<String, Any?>

    private fun Any?.asId(): String? = when (this) {
        is String -> trim().takeIf { it.isNotEmpty() && it.length <= 64 }
        else -> asLong()?.toString()
    }

    private fun Any?.asLong(): Long? = when (this) {
        is Int -> toLong()
        is Long -> this
        is Double -> if (isFinite() && this == Math.floor(this)) toLong() else null
        is BigDecimal -> runCatching { longValueExact() }.getOrNull()
        is String -> trim().toLongOrNull()
        else -> null
    }

    private fun Any?.asBoolean(): Boolean? = when (this) {
        is Boolean -> this
        is String -> when (trim().lowercase()) { "true" -> true; "false" -> false; else -> null }
        else -> null
    }
}

/** Shared text limits for mapped vendor strings. */
object FnbLimits {
    const val MAX_NAME_LENGTH = 80
    const val MAX_DESCRIPTION_LENGTH = 500
}
