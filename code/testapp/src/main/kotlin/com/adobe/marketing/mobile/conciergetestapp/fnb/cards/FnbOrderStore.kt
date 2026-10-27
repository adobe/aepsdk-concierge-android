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

import com.adobe.marketing.mobile.conciergetestapp.fnb.FnbXdm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * App-owned F&B coordination across card instances; the SDK holds no menu or order state.
 *
 * - UI state (kept here, never sent): which menu is the newest, and so the only actionable one.
 * - Current and past order IDs are mirrored into `_sacramentokings.conciergeContext.fnbOrders` through
 *   [publishContext]; order-status tracking stays app-local.
 *
 * Cards register on first display. Order of first display stands in for transcript order. The
 * store is memory-only and resets when a newly displayed card belongs to another Concierge session,
 * matching the SDK, which drops held XDM context for a new session. Checkout only archives the
 * order as `pending`: opening checkout proves neither payment nor fulfillment.
 */
class FnbOrderStore(
    private val publishEventContext: (Long) -> Unit = {},
    private val onSessionChanged: () -> Unit = {},
    private val publishContext: (Map<String, Any?>) -> Unit
) {

    data class State(
        val latestMenuCardId: String? = null,
        val currentOrderId: String? = null,
        val submittedOrderIds: List<String> = emptyList()
    ) {
        fun isOrderSubmitted(orderId: String): Boolean = orderId in submittedOrderIds
    }

    companion object {
        const val CURRENT_ORDER = "currentOrder"
        const val ORDER_ID = "orderId"
        const val PAST_ORDERS = "pastOrders"
    }

    private val lock = Any()
    private val stateFlow = MutableStateFlow(State())
    private val seenCardIds = HashSet<Pair<String, String?>>()
    private val retiredSessionIds = HashSet<String>()
    private var sessionId: String? = null

    val state: StateFlow<State> = stateFlow.asStateFlow()

    val currentOrderId: String? get() = stateFlow.value.currentOrderId

    /**
     * Whether the menu card [cardId] may take orders: it is the newest menu, or it is being displayed
     * for the first time (it becomes the newest once [menuShown] runs).
     */
    fun isActionableMenu(state: State, cardId: String, cardSessionId: String?): Boolean =
        synchronized(lock) {
            cardSessionId !in retiredSessionIds &&
                (state.latestMenuCardId == cardId || (cardId to cardSessionId) !in seenCardIds)
        }

    /** A menu card was displayed; the first display of a new card makes it the only actionable menu. */
    fun menuShown(cardId: String, sessionId: String?) {
        synchronized(lock) {
            if (!firstSeen(cardId, sessionId)) return
            stateFlow.update { it.copy(latestMenuCardId = cardId) }
        }
    }

    /** A locations response was first displayed; publish its game once, not from a card action. */
    fun locationsShown(cardId: String, sessionId: String?, eventId: Long?) {
        val event = synchronized(lock) {
            if (!firstSeen(cardId, sessionId)) return
            eventId?.takeIf { it >= 0 }
        }
        event?.let(publishEventContext)
    }

    /**
     * A cart card was displayed. On its first display, an unpaid order that wasn't checked out
     * becomes the current order, so later adds go to it.
     */
    fun cartShown(cardId: String, sessionId: String?, orderId: String, isPaid: Boolean) {
        val normalizedOrderId = orderId.trim()
        val numericOrderId = normalizedOrderId.toLongOrNull()?.takeIf { it >= 0 } ?: return
        val patch = synchronized(lock) {
            if (!firstSeen(cardId, sessionId)) return
            val current = stateFlow.value
            if (isPaid || normalizedOrderId.isEmpty() || current.isOrderSubmitted(normalizedOrderId) ||
                current.currentOrderId != null) return
            stateFlow.value = current.copy(currentOrderId = normalizedOrderId)
            FnbXdm.orders(mapOf(CURRENT_ORDER to mapOf(ORDER_ID to numericOrderId)))
        }
        publishContext(patch)
    }

    /**
     * Checkout opened for [orderId]: archive it in `pastOrders`, clear it as the current order,
     * and close every displayed menu. A later menu starts a new order. Reopening checkout for an
     * archived order changes nothing.
     */
    fun checkedOut(orderId: String) {
        if (orderId.isBlank()) return
        val patch = synchronized(lock) {
            val current = stateFlow.value
            if (current.isOrderSubmitted(orderId) || current.currentOrderId != orderId) return
            val submitted = current.submittedOrderIds + orderId
            stateFlow.value = current.copy(
                latestMenuCardId = null,
                currentOrderId = null,
                submittedOrderIds = submitted
            )
            val pastOrders = submitted.mapNotNull { it.toLongOrNull()?.takeIf { id -> id >= 0 } }
                .map { mapOf(ORDER_ID to it) }
            FnbXdm.orders(mapOf(CURRENT_ORDER to null, PAST_ORDERS to pastOrders))
        }
        publishContext(patch)
    }

    /** Drops process-local order state after an identity reset; the SDK clears its held XDM. */
    fun identityReset() {
        synchronized(lock) {
            sessionId?.let(retiredSessionIds::add)
            stateFlow.value = State()
            onSessionChanged()
        }
    }

    /** Records [cardId]; true on its first display. Cards from retired sessions cannot rebind it. */
    private fun firstSeen(cardId: String, cardSessionId: String?): Boolean {
        if (cardSessionId != null && cardSessionId in retiredSessionIds) return false
        if (!seenCardIds.add(cardId to cardSessionId)) return false
        if (cardSessionId != null && cardSessionId != sessionId) {
            sessionId?.let(retiredSessionIds::add)
            stateFlow.value = State()
            sessionId = cardSessionId
            onSessionChanged()
        }
        return true
    }
}
