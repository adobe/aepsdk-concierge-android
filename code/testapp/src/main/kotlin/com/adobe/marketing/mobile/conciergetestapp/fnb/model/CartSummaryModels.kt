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

/**
 * UI model for the cart view (`fnb.cart`): the server's view of the fan's order after
 * `cart/add` (or an update/remove). All money is tapin2-computed and authoritative, unlike the
 * menu widget's display-only local cart.
 */
@Immutable
data class CartSummaryUiModel(
    /** tapin2 plain order id (BCOS `entity_info.cartId`), used by add/update/remove and status calls. */
    val cartId: String,
    /** tapin2 order GUID, used only for the checkout page. */
    val guid: String?,
    /** Short code shown to the fan (tapin2 `idLast3`), if present. */
    val orderCode: String?,
    val venueName: String,
    val lines: List<CartSummaryLine>,
    val subtotalCents: Long,
    val taxCents: Long,
    val feesCents: Long,
    val discountCents: Long,
    val totalCents: Long,
    val isPaid: Boolean,
    val containsAlcohol: Boolean,
    /** tapin2 checkout (Review) page; null hides the checkout button. */
    val checkoutUrl: String?,
    val currencyCode: String = MenuUiModel.DEFAULT_CURRENCY_CODE,
    val checkoutLabel: String = "Proceed to checkout",
    /** Null hides the secondary button. */
    val showMoreLabel: String? = "Show more restaurants"
) {
    val isEmpty: Boolean get() = lines.isEmpty()
}

@Immutable
data class CartSummaryLine(
    /** tapin2 order line id (`lines[].lineId`), the key for remove/edit. */
    val lineId: String,
    val productId: String,
    val title: String,
    val quantity: Int,
    val pricePerCents: Long,
    val subtotalCents: Long,
    /** BCOS `modifiersSummary`, e.g. "Coke" or "No modifiers". */
    val modifiersSummary: String,
    val locationId: String,
    val locationName: String,
    /** Fan's special instructions echoed back, if any. */
    val note: String? = null,
    /** True when BCOS declared `actions.remove` for the line. */
    val removable: Boolean = true
)
