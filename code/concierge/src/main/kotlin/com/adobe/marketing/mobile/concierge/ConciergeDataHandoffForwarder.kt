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
 * Seam for forwarding an accepted [ConciergeDataHandoffEvent] to Brand Concierge. Fire-and-forget
 * — there is no delivery-confirmation signal today (accept/reject only confirms the SDK validated
 * the payload's shape, not that Brand Concierge received it).
 */
internal interface ConciergeDataHandoffForwarder {
    fun forward(result: ConciergeDataHandoffEvent)
}

/**
 * Default forwarder while the real Brand Concierge forward is still pending —
 * `ConciergeChatService`'s XDM plumbing for this doesn't exist yet. A no-op (beyond logging) so a
 * follow-up implementation can swap in without changing any caller.
 */
internal object NotImplementedDataHandoffForwarder : ConciergeDataHandoffForwarder {
    private const val SELF_TAG = "NotImplementedDataHandoffForwarder"

    override fun forward(result: ConciergeDataHandoffEvent) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            SELF_TAG,
            "Data handoff forwarding not yet implemented; routingHint=${result.routingHint}"
        )
    }
}
