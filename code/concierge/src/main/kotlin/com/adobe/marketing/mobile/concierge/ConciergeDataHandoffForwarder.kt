/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.services.Log

/**
 * Delivers a data handoff through an active chat session.
 */
internal interface ConciergeDataHandoffForwarder {
    fun forward(
        result: ConciergeDataHandoffEvent,
        completion: (DataHandoffDeliveryResult) -> Unit
    )
}

/** The final delivery result for a decoded data handoff. */
internal sealed class DataHandoffDeliveryResult {
    object Delivered : DataHandoffDeliveryResult()
    data class Failed(val reason: ConciergeDataHandoffRejectReason) : DataHandoffDeliveryResult()
}

/**
 * Routes data handoffs to the chat host currently rendered by the app. Keeping this registry at
 * the UI boundary means the service response uses the same transcript and request queue as a user
 * message rather than starting an independent background conversation.
 *
 * Holds at most one active forwarder. If two chat hosts are rendered at once (e.g. two Activities,
 * or an Activity and a Fragment, each with their own [com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel]),
 * the most recently registered one wins and the other silently stops receiving handoffs — only one
 * configured chat surface should be kept active at a time.
 */
internal object ActiveConciergeDataHandoffForwarder : ConciergeDataHandoffForwarder {
    private const val SELF_TAG = "ActiveConciergeDataHandoffForwarder"

    private var activeForwarder: ConciergeDataHandoffForwarder? = null

    internal fun register(forwarder: ConciergeDataHandoffForwarder) {
        synchronized(this) {
            val previous = activeForwarder
            if (previous != null && previous !== forwarder) {
                Log.warning(
                    ConciergeConstants.EXTENSION_NAME,
                    SELF_TAG,
                    "Replacing an already-active data handoff session; only one configured chat " +
                        "surface should be active at a time."
                )
            }
            activeForwarder = forwarder
        }
    }

    internal fun unregister(forwarder: ConciergeDataHandoffForwarder) {
        synchronized(this) {
            if (activeForwarder === forwarder) {
                activeForwarder = null
            }
        }
    }

    override fun forward(
        result: ConciergeDataHandoffEvent,
        completion: (DataHandoffDeliveryResult) -> Unit
    ) {
        val forwarder = synchronized(this) { activeForwarder }
        if (forwarder == null) {
            completion(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION))
            return
        }
        forwarder.forward(result, completion)
    }
}
