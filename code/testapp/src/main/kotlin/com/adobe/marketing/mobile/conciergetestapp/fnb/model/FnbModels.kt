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

/** UI model for the F&B menu widget; independent of the BCOS/tapin2 wire shape. */
@Immutable
data class MenuUiModel(
    val locationId: String,
    val locationName: String,
    val orderingAvailable: Boolean,
    val categories: List<MenuCategory>,
    val currencyCode: String = DEFAULT_CURRENCY_CODE,
    /** tapin2 arena id, echoed on submit (`entity_info.venueId`). */
    val venueId: String = "",
    /** tapin2 event id, echoed on submit (`entity_info.eventId`). */
    val eventId: String = "",
    val cartBar: CartBarConfig = CartBarConfig()
) {
    companion object {
        const val DEFAULT_CURRENCY_CODE = "USD"
    }
}

@Immutable
data class MenuCategory(
    val id: String,
    val label: String,
    val items: List<MenuItem>
)

@Immutable
data class MenuItem(
    val id: String,
    val name: String,
    val description: String = "",
    val metadata: List<String> = emptyList(),
    val priceCents: Long,
    val wasPriceCents: Long? = null,
    val imageUrl: String? = null,
    val tag: String? = null,
    val isAlcohol: Boolean = false,
    val optionGroups: List<OptionGroup> = emptyList(),
    /** False renders the tile as sold out and blocks adding. */
    val available: Boolean = true,
    /** Server-declared tile action: true for `OPEN_SHEET`, false for `INCREMENT`. */
    val opensSheet: Boolean = optionGroups.isNotEmpty(),
    val quantity: QuantityRule = QuantityRule(),
    /** Null when special instructions are not offered for this item. */
    val instructions: InstructionsConfig? = null,
    /** Customize CTA label (`sheet.addToCart.label`). */
    val addToCartLabel: String = "Add to order"
) {
    val hasOptions: Boolean get() = optionGroups.isNotEmpty()
}

/** Customize quantity stepper bounds (`sheet.quantity`). */
@Immutable
data class QuantityRule(val min: Int = 1, val max: Int = 20, val default: Int = 1, val step: Int = 1)

/** Free-text line note (`sheet.specialInstructions`). */
@Immutable
data class InstructionsConfig(val label: String, val placeholder: String, val maxLength: Int)

/** Menu footer button (`cartBar` element). */
@Immutable
data class CartBarConfig(
    val label: String = "Add to cart",
    val showCount: Boolean = true,
    val hideWhenEmpty: Boolean = false,
    val enabled: Boolean = true
)

/**
 * A modifier group. [minSelect]/[maxSelect] are already normalized by the mapper:
 * `required` implies `minSelect >= 1`, and `maxSelect >= max(minSelect, 1)`.
 */
@Immutable
data class OptionGroup(
    val id: String,
    val title: String,
    val required: Boolean,
    val minSelect: Int,
    val maxSelect: Int,
    val options: List<OptionChoice>,
    /** Per-option quantity cap (`maxPerOption`); only 1 is supported by the UI today. */
    val maxPerOption: Int = 1
) {
    val isSingleSelect: Boolean get() = maxSelect == 1
}

@Immutable
data class OptionChoice(
    val id: String,
    val label: String,
    val priceDeltaCents: Long = 0L,
    val isDefault: Boolean = false,
    val available: Boolean = true
)
