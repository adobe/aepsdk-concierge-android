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

package com.adobe.marketing.mobile.conciergetestapp.cards

import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.EMPTY_XDM_FIELDS
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.INVALID_ROUTING_HINT_TYPE
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.INVALID_XDM_FIELDS_TYPE
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_KEY
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.INVALID_XDM_FIELD_VALUE
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.MISSING_EVENT_DATA
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.MISSING_XDM_FIELDS
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason.RESERVED_KEY_COLLISION

/** How a card action ended, from the card's point of view. */
enum class CardActionOutcome {
    ACCEPTED,

    /** Never started: another chat turn or handoff was running. */
    BUSY,

    /** Never started: no active chat session. */
    UNAVAILABLE,

    /** Never started: the action or its handoff was rejected as malformed, or couldn't run locally. */
    INVALID,

    /** Started, but its outcome is unknown; the backend may have acted on it. */
    UNCONFIRMED;

    /** True when nothing reached Brand Concierge, so the draft can be restored and retried. */
    val neverStarted: Boolean
        get() = this == BUSY || this == UNAVAILABLE || this == INVALID

    companion object {
        /** Classifies a [com.adobe.marketing.mobile.concierge.Concierge.sendDataHandoff] completion. */
        fun ofHandoff(accepted: Boolean, reason: ConciergeDataHandoffRejectReason?): CardActionOutcome = when {
            accepted -> ACCEPTED
            reason == CHAT_IN_PROGRESS -> BUSY
            reason == NO_ACTIVE_SESSION -> UNAVAILABLE
            reason in REJECTED_BEFORE_SENDING -> INVALID
            // Delivery failures, and any reason added later: the backend may have acted, so never retry.
            else -> UNCONFIRMED
        }

        /** Validation failures: the SDK rejected the handoff before anything was sent. */
        private val REJECTED_BEFORE_SENDING = setOf(
            MISSING_EVENT_DATA,
            INVALID_ROUTING_HINT_TYPE,
            MISSING_XDM_FIELDS,
            INVALID_XDM_FIELDS_TYPE,
            EMPTY_XDM_FIELDS,
            INVALID_XDM_FIELD_KEY,
            RESERVED_KEY_COLLISION,
            INVALID_XDM_FIELD_VALUE
        )
    }
}

/**
 * Sends a card's opaque action map (`operation`/`intent`, `actionId`, fields) and reports its
 * outcome on the main thread. Lets card runtimes be tested without the Concierge statics.
 */
fun interface CardActionSender {
    fun send(action: Map<String, Any?>, onResult: (CardActionOutcome) -> Unit)
}

/**
 * Which controls a card may enable, from its renderer `context`. Read it on every recomposition:
 * `isBusy` and `isStale` change while the card is on screen.
 */
data class CardGate(val isBusy: Boolean = false, val isStale: Boolean = false) {
    /** Controls that send a handoff. */
    val canSend: Boolean get() = !isBusy && !isStale

    /** Local drafts (e.g. menu quantities): still editable while a turn runs. */
    val canEdit: Boolean get() = !isStale

    companion object {
        val OPEN = CardGate()

        fun from(context: Map<String, Any?>): CardGate =
            CardGate(isBusy = context["isBusy"] == true, isStale = context["isStale"] == true)
    }
}

/** This card's last `saveState` map from its renderer `context`, or null when it never saved. */
fun savedCardState(context: Map<String, Any?>): Map<String, Any?>? =
    (context["state"] as? Map<*, *>)?.let { state ->
        state.entries.filter { it.key is String }.associate { it.key as String to it.value }
    }

/**
 * A runtime's durable fields, persisted through the card's `saveState`. Each save carries a
 * revision, so a newer save from another instance of the same card (a completion that landed after
 * the card scrolled off and was recreated) can be adopted, while an older one is ignored.
 */
class CardStateSaver(saved: Map<String, Any?>?, private val saveState: (Map<String, Any?>) -> Unit) {
    var revision: Long = revisionOf(saved)
        private set

    fun save(fields: Map<String, Any?>) {
        revision++
        saveState(fields + (REVISION to revision))
    }

    /** True when [state] is newer than anything this instance saved or restored; it is then adopted. */
    fun adopt(state: Map<String, Any?>?): Boolean {
        val incoming = revisionOf(state)
        if (incoming <= revision) return false
        revision = incoming
        return true
    }

    private companion object {
        const val REVISION = "rev"

        fun revisionOf(state: Map<String, Any?>?): Long = (state?.get(REVISION) as? Number)?.toLong() ?: 0L
    }
}

/** String-to-string entries of a saved JSON object; anything else is dropped. */
internal fun stringMap(value: Any?): Map<String, String> =
    (value as? Map<*, *>)?.entries
        ?.mapNotNull { (key, item) -> if (key is String && item is String) key to item else null }
        ?.toMap()
        .orEmpty()

/** String entries of a saved JSON array; anything else is dropped. */
internal fun stringList(value: Any?): List<String> = (value as? List<*>)?.filterIsInstance<String>().orEmpty()
