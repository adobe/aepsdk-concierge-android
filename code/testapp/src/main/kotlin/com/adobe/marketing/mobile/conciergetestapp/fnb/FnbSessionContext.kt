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

package com.adobe.marketing.mobile.conciergetestapp.fnb

import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.services.Log

/**
 * Publishes the current game to `_sacramentokings.conciergeContext.fnbOrders.eventId` in held XDM context, which the
 * SDK merges into every turn. Driven by `FnbOrderStore` when a `kings.fnblocation` response
 * with a top-level `eventId` is first displayed; the server infers that event, so the app never sets
 * it by hand.
 */
internal object FnbSessionContext {
    private val shared = FnbHostContext(updateContext = Concierge::updateXDMContext)

    /** Sets (or, with null, removes) `conciergeContext.fnbOrders.eventId` for every later turn. */
    fun setCurrentEvent(eventId: Long?) = shared.setCurrentEvent(eventId)

    /** Clears only the app's mirror; SDK session and identity lifecycle owns held-XDM cleanup. */
    fun clearLocalContext() = shared.clearLocalContext()
}

internal class FnbHostContext(private val updateContext: (Map<String, Any?>) -> Unit) {
    @Volatile
    var currentEventId: Long? = null
        private set

    fun setCurrentEvent(eventId: Long?) {
        require(eventId == null || eventId >= 0) { "eventId must be a non-negative tapin2 id" }
        if (currentEventId == eventId) return
        try {
            updateContext(FnbXdm.orders(mapOf(EVENT_ID to eventId)))
            currentEventId = eventId
        } catch (e: IllegalArgumentException) {
            Log.warning(LOG_TAG, SELF_TAG, "Could not set the current event in XDM context: ${e.message}")
        }
    }

    fun clearLocalContext() {
        currentEventId = null
    }

    private companion object {
        const val EVENT_ID = "eventId"
        const val LOG_TAG = "ConciergeTestApp"
        const val SELF_TAG = "FnbSessionContext"
    }
}
