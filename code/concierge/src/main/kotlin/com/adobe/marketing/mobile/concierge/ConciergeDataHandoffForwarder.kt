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
 * Seam for forwarding an accepted [ConciergeDataHandoffEvent] to Brand Concierge. [onComplete]
 * must eventually be called with the true delivered/failed outcome — it has no deadline of its
 * own (the caller is responsible for timeout handling).
 */
internal interface ConciergeDataHandoffForwarder {
    fun forward(result: ConciergeDataHandoffEvent, onComplete: (delivered: Boolean, errorCode: String?) -> Unit)
}

/**
 * Default forwarder while the real Brand Concierge forward is still pending —
 * `ConciergeChatService`'s XDM plumbing for this doesn't exist yet. Always reports
 * not-delivered with an explicit, deterministic error code — never a silent no-op — so a
 * follow-up implementation can swap in without changing any caller.
 */
internal object NotImplementedDataHandoffForwarder : ConciergeDataHandoffForwarder {
    private const val SELF_TAG = "NotImplementedDataHandoffForwarder"

    override fun forward(result: ConciergeDataHandoffEvent, onComplete: (Boolean, String?) -> Unit) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            SELF_TAG,
            "Data handoff forwarding not yet implemented; routingHint=${result.routingHint}"
        )
        onComplete(false, ConciergeConstants.DataHandoff.DeliveryErrorCode.NOT_IMPLEMENTED)
    }
}
