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

package com.adobe.marketing.mobile.conciergetestapp.fnb.cards

import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionSender
import com.adobe.marketing.mobile.conciergetestapp.cards.CardStateSaver
import com.adobe.marketing.mobile.conciergetestapp.cards.stringList
import com.adobe.marketing.mobile.conciergetestapp.cards.stringMap
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbHandoffFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * State and actions for one `kings.fnbcart` element. Remove and "show restaurants" become data handoffs;
 * checkout opens the allowlisted tapin2 page locally. One request runs at a time, and a retry of a
 * rejected request reuses its `actionId`. A request whose outcome is unknown is never sent again: it
 * may have reached the backend, so the fan is told to ask the concierge instead.
 *
 * Once checkout opened for this order ([isOrderClosed]), the cart takes no more changes; checkout
 * can still be reopened. [onCheckedOut] runs each time checkout opens.
 *
 * Pending removals, unconfirmed requests and retry ids are saved through [saveState] and restored
 * from [savedState].
 */
class CartCardRuntime(
    val model: CartSummaryUiModel,
    private val actions: CardActionSender,
    private val allowedCheckoutHosts: Set<String> = CheckoutUrlPolicy.DEFAULT_HOSTS,
    savedState: Map<String, Any?>? = null,
    saveState: (Map<String, Any?>) -> Unit = {},
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
    private val isOrderClosed: () -> Boolean = { false },
    private val onCheckedOut: () -> Unit = {}
) {
    companion object {
        const val MESSAGE_CHECKOUT_FAILED = "Couldn't open checkout. Try again."
        const val MESSAGE_FAILED = "Couldn't send your request. Try again."
        const val MESSAGE_UNAVAILABLE = "Chat isn't available right now."
        const val MESSAGE_BUSY = "Please wait for the current reply, then try again."
        const val MESSAGE_UNCONFIRMED = "Couldn't confirm your request went through. Ask the concierge to check."

        private const val PENDING_REMOVALS = "pendingRemovals"
        private const val UNCONFIRMED = "unconfirmed"
        private const val RETRY_IDS = "retryIds"

        fun removalKey(orderId: String, itemId: String) = "remove:$orderId:$itemId"
        fun showRestaurantsKey(orderId: String) = "showRestaurants:$orderId"
    }

    private val saver = CardStateSaver(savedState, saveState)
    private val pendingRemovalState = MutableStateFlow<Set<String>>(emptySet())
    private val unconfirmedState = MutableStateFlow<Set<String>>(emptySet())
    private val requestInFlightState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)
    private val retryActionIds = HashMap<String, String>()

    /** Item ids whose removal was handed off; the updated order arrives as a new cart card. */
    val pendingRemovals: StateFlow<Set<String>> = pendingRemovalState.asStateFlow()

    /** Keys ([removalKey], [showRestaurantsKey]) of requests whose outcome is unknown; they can't be sent again. */
    val unconfirmed: StateFlow<Set<String>> = unconfirmedState.asStateFlow()
    val requestInFlight: StateFlow<Boolean> = requestInFlightState.asStateFlow()
    val error: StateFlow<String?> = errorState.asStateFlow()

    init {
        restore(savedState)
    }

    /** Adopts [state] when another instance of this card saved it after this one. */
    fun adopt(state: Map<String, Any?>?) {
        if (saver.adopt(state)) restore(state)
    }

    fun remove(line: CartSummaryLine) {
        val order = model
        if (order.isPaid || isOrderClosed() || !line.removable || line.itemId in pendingRemovalState.value) return
        val retryKey = removalKey(order.orderId, line.itemId)
        request(
            retryKey = retryKey,
            action = { actionId ->
                mapOf(
                    "operation" to FnbOperations.REMOVE_CART_ITEM,
                    "actionId" to actionId,
                    "locationId" to line.locationId,
                    "productId" to line.productId,
                    "modifier" to line.modifierForRemoval,
                    "message" to FnbHandoffFormatter.removeMessage(line.title)
                )
            }
        ) { pendingRemovalState.value = pendingRemovalState.value + line.itemId }
    }

    /** Uses the last cart line's location, since the card has no order-level location. */
    fun showRestaurants() {
        if (isOrderClosed()) return
        val locationId = model.lines.lastOrNull()?.locationId?.takeIf(String::isNotBlank) ?: return
        request(retryKey = showRestaurantsKey(model.orderId), action = { actionId ->
            mapOf(
                "operation" to FnbOperations.SHOW_RESTAURANTS,
                "actionId" to actionId,
                "locationId" to locationId,
                "message" to FnbHandoffFormatter.SHOW_RESTAURANTS_MESSAGE
            )
        })
    }

    fun checkout() {
        val url = model.takeUnless { it.isPaid }?.checkoutUrl ?: return
        if (!CheckoutUrlPolicy.isAllowed(url, allowedCheckoutHosts)) {
            errorState.value = MESSAGE_CHECKOUT_FAILED
            return
        }
        request(
            retryKey = null,
            action = { actionId -> mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "actionId" to actionId, "url" to url) },
            failureMessage = MESSAGE_CHECKOUT_FAILED,
            onAccepted = onCheckedOut
        )
    }

    /** [retryKey] names the logical action whose `actionId` a retry reuses; null for local-only actions. */
    private fun request(
        retryKey: String?,
        action: (actionId: String) -> Map<String, Any?>,
        failureMessage: String = MESSAGE_FAILED,
        onAccepted: () -> Unit = {}
    ) {
        if (requestInFlightState.value) return
        if (retryKey != null && retryKey in unconfirmedState.value) {
            errorState.value = MESSAGE_UNCONFIRMED
            return
        }
        requestInFlightState.value = true
        errorState.value = null
        val actionId = retryKey?.let { retryActionIds.getOrPut(it, idFactory) } ?: idFactory()
        if (retryKey != null) save()
        val onResult: (CardActionOutcome) -> Unit = { outcome ->
            requestInFlightState.value = false
            when (outcome) {
                CardActionOutcome.ACCEPTED -> {
                    retryKey?.let(retryActionIds::remove)
                    onAccepted()
                }
                CardActionOutcome.BUSY -> errorState.value = MESSAGE_BUSY
                CardActionOutcome.UNAVAILABLE -> errorState.value = MESSAGE_UNAVAILABLE
                CardActionOutcome.INVALID -> errorState.value = failureMessage
                CardActionOutcome.UNCONFIRMED -> {
                    retryKey?.let {
                        retryActionIds.remove(it)
                        unconfirmedState.value = unconfirmedState.value + it
                    }
                    errorState.value = MESSAGE_UNCONFIRMED
                }
            }
            if (retryKey != null) save()
        }
        try {
            actions.send(action(actionId), onResult)
        } catch (e: Exception) {
            // A throwing sender may already have dispatched the request.
            if (requestInFlightState.value) onResult(CardActionOutcome.UNCONFIRMED)
        }
    }

    private fun save() {
        saver.save(
            mapOf(
                PENDING_REMOVALS to pendingRemovalState.value.toList(),
                UNCONFIRMED to unconfirmedState.value.toList(),
                RETRY_IDS to retryActionIds.toMap()
            )
        )
    }

    private fun restore(state: Map<String, Any?>?) {
        pendingRemovalState.value = stringList(state?.get(PENDING_REMOVALS)).toSet()
        unconfirmedState.value = stringList(state?.get(UNCONFIRMED)).toSet()
        retryActionIds.clear()
        retryActionIds.putAll(stringMap(state?.get(RETRY_IDS)))
    }
}
