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
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeSendMessageRejectReason
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy

/**
 * Outward signals from F&B widgets. In the menu widget, add/remove/customize stay local; the
 * cart view's actions act on the server-side tapin2 order.
 */
sealed interface FnbAction {
    /**
     * The whole local cart, emitted once by ADD TO CART.
     * @property submitId client-generated UUID, one per ADD TO CART tap. Not a tapin2 id and never
     * sent to tapin2: BC compares it with the last one it handled to skip a replayed submit, because
     * `cart/add` with the same tapin2 `orderId` merges lines and would double the quantities. The
     * tapin2 `orderId` (the order being built) is resolved by BC from conversation state.
     */
    @Immutable
    data class SubmitCart(
        val submitId: String,
        /** tapin2 request ids (`venueId`, `eventId`) carried by the menu elements. */
        val venueId: String,
        val eventId: String,
        val locationId: String,
        val locationName: String,
        val lines: List<CartLine>
    ) : FnbAction

    /**
     * Cart view "Remove".
     * @property orderId the tapin2 order `id`.
     * @property itemId the tapin2 order line `items[].id`.
     */
    @Immutable
    data class RemoveCartItem(val orderId: String, val itemId: String, val title: String) : FnbAction

    /** Cart view "Show more restaurants": asks BC for other stands; the order is kept. */
    @Immutable
    data class ShowMoreRestaurants(val orderId: String) : FnbAction

    /**
     * A location picked from the out-of-the-box location product cards (via [FnbLinks]).
     * @property locationId tapin2 `location.id`.
     */
    @Immutable
    data class SelectLocation(val locationId: String, val title: String) : FnbAction

    /** Cart view "Proceed to checkout": the host opens the tapin2 Review page at [checkoutUrl]. */
    @Immutable
    data class Checkout(val orderId: String, val checkoutUrl: String) : FnbAction
}

sealed interface FnbActionResult {
    object Accepted : FnbActionResult

    /** @property retryable whether the same action may succeed if tried again later. */
    data class Rejected(val message: String, val retryable: Boolean) : FnbActionResult
}

/** Host-supplied sink for widget actions. [onResult] must be invoked exactly once. */
fun interface FnbActionHandler {
    fun onAction(action: FnbAction, onResult: (FnbActionResult) -> Unit)
}

/**
 * Production handler.
 * - [FnbAction.SubmitCart], [FnbAction.RemoveCartItem], [FnbAction.ShowMoreRestaurants],
 *   [FnbAction.SelectLocation] become a user turn through [Concierge.sendMessage].
 * - [FnbAction.Checkout] is opened by the host via [openUrl] (e.g. a Custom Tab rather than the
 *   SDK's WebView, which payment pages often break in). The URL is re-checked against
 *   [allowedCheckoutHosts] first.
 */
class ConciergeFnbActionHandler(
    private val openUrl: (String) -> Boolean,
    private val allowedCheckoutHosts: Set<String> = CheckoutUrlPolicy.DEFAULT_HOSTS
) : FnbActionHandler {

    override fun onAction(action: FnbAction, onResult: (FnbActionResult) -> Unit) {
        when (action) {
            is FnbAction.SubmitCart -> send(FnbPromptFormatter.format(action), onResult)
            is FnbAction.RemoveCartItem -> send(FnbPromptFormatter.formatRemove(action), onResult)
            is FnbAction.ShowMoreRestaurants -> send(FnbPromptFormatter.formatShowMore(action), onResult)
            is FnbAction.SelectLocation -> send(FnbPromptFormatter.formatSelectLocation(action), onResult)
            is FnbAction.Checkout -> onResult(
                if (CheckoutUrlPolicy.isAllowed(action.checkoutUrl, allowedCheckoutHosts) && openUrl(action.checkoutUrl)) {
                    FnbActionResult.Accepted
                } else {
                    FnbActionResult.Rejected("Couldn't open checkout. Try again.", retryable = true)
                }
            )
        }
    }

    private fun send(message: String, onResult: (FnbActionResult) -> Unit) {
        Concierge.sendMessage(message) { accepted, reason ->
            onResult(if (accepted) FnbActionResult.Accepted else rejection(reason))
        }
    }

    companion object {
        internal fun rejection(reason: ConciergeSendMessageRejectReason?): FnbActionResult.Rejected = when (reason) {
            ConciergeSendMessageRejectReason.CHAT_IN_PROGRESS ->
                FnbActionResult.Rejected("Please wait for the current reply, then try again.", retryable = true)
            ConciergeSendMessageRejectReason.MESSAGE_TOO_LONG ->
                FnbActionResult.Rejected("Your order is too large to send. Remove some items.", retryable = false)
            ConciergeSendMessageRejectReason.NO_ACTIVE_SESSION ->
                FnbActionResult.Rejected("Chat isn't available right now.", retryable = true)
            else -> FnbActionResult.Rejected("Couldn't send your request. Try again.", retryable = true)
        }
    }
}
