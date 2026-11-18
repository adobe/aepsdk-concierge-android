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

/**
 * App-owned F&B coordination across card instances; the SDK holds no menu or order state.
 *
 * - UI state (kept here, never sent): which menus checkout closed.
 * - Current and past order IDs are mirrored into `_sacramentokings.conciergeContext.fnbOrders` through
 *   [publishContext]; order-status tracking stays app-local.
 *
 * Which card is newest comes from the SDK's `isLatest` context flag, never from display order:
 * Compose doesn't compose transcript rows in transcript order, so a card that displays later can
 * be older. Cards register on first display. The store is memory-only and resets when a newly
 * displayed card belongs to another Concierge session,
 * matching the SDK, which drops held XDM context for a new session. Checkout only archives the
 * order as `pending`: opening checkout proves neither payment nor fulfillment.
 */
class FnbOrderStore(
    private val publishEventContext: (Long?) -> Unit = {},
    private val onSessionChanged: () -> Unit = {},
    private val publishContext: (Map<String, Any?>) -> Unit
) {

    data class State(
        val closedMenuCardIds: Set<String> = emptySet(),
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
    private val currentLocationCardIds = HashSet<Pair<String, String?>>()
    private val shownMenuCardIds = HashSet<String>()
    private val retiredSessionIds = HashSet<String>()
    private var sessionId: String? = null
    private var latestLocationCard: Pair<String, String?>? = null

    val state: StateFlow<State> = stateFlow.asStateFlow()

    val currentOrderId: String? get() = stateFlow.value.currentOrderId

    /**
     * Whether the menu card [cardId] may take orders: it is the newest menu ([isLatest], from the
     * card's SDK context), its session is still current, and checkout hasn't closed it.
     */
    fun isActionableMenu(state: State, cardId: String, cardSessionId: String?, isLatest: Boolean): Boolean =
        synchronized(lock) {
            isLatest && cardSessionId !in retiredSessionIds && cardId !in state.closedMenuCardIds
        }

    /** A menu card was displayed; checkout closes every menu displayed before it. */
    fun menuShown(cardId: String, sessionId: String?) {
        synchronized(lock) {
            if (firstSeen(cardId, sessionId)) shownMenuCardIds += cardId
        }
    }

    /**
     * A locations response was displayed; publish its game only from the newest locations card.
     * Clear an unavailable game, and restore an older card's game when it displays as newest again.
     */
    fun locationsShown(cardId: String, sessionId: String?, eventId: Long?, isLatest: Boolean) {
        val event = synchronized(lock) {
            if (!isLatest || sessionId != null && sessionId in retiredSessionIds) return
            val card = cardId to sessionId
            if (card == latestLocationCard) return
            if (!firstSeen(cardId, sessionId) && card !in currentLocationCardIds) return
            currentLocationCardIds += card
            latestLocationCard = card
            eventId?.takeIf { it >= 0 }
        }
        publishEventContext(event)
    }

    /**
     * A cart card was displayed. On the first display of the newest cart, an unpaid order that
     * wasn't checked out becomes the current order, so later adds go to it.
     */
    fun cartShown(cardId: String, sessionId: String?, orderId: String, isPaid: Boolean, isLatest: Boolean) {
        val normalizedOrderId = orderId.trim()
        val numericOrderId = normalizedOrderId.toLongOrNull()?.takeIf { it >= 0 } ?: return
        val patch = synchronized(lock) {
            if (!isLatest || !firstSeen(cardId, sessionId)) return
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
                closedMenuCardIds = current.closedMenuCardIds + shownMenuCardIds,
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
            shownMenuCardIds.clear()
            currentLocationCardIds.clear()
            latestLocationCard = null
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
            shownMenuCardIds.clear()
            currentLocationCardIds.clear()
            latestLocationCard = null
            sessionId = cardSessionId
            onSessionChanged()
        }
        return true
    }
}
