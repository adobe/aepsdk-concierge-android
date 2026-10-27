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

import androidx.compose.runtime.Immutable
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine

/**
 * F&B user intents. In the menu card, add/remove/customize stay local; ADD TO CART and the cart
 * card's actions act on the server-side tapin2 order. The app maps card actions to
 * `sendDataHandoff` calls (see `fnb/cards`).
 */
sealed interface FnbAction {
    /**
     * The whole local cart, emitted once by ADD TO CART as an `add-to-cart` handoff.
     * @property submitId client-generated UUID, one per ADD TO CART tap and reused when that same
     * cart is retried after a never-started rejection. Local only: not sent to Brand Concierge.
     */
    @Immutable
    data class SubmitCart(
        val submitId: String,
        val locationId: String,
        val locationName: String,
        val lines: List<CartLine>
    ) : FnbAction
}
