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

/**
 * Delivers a data handoff into the conversation pipeline.
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
 * Forwards decoded data handoffs to the always-alive process conversation session.
 *
 * @param session resolves the session to forward into. Defaults to the process-wide
 * [ConciergeConversationSession.instance] and is resolved per call, not at construction, so
 * building a forwarder never forces the singleton into existence.
 */
internal class SessionDataHandoffForwarder(
    private val session: () -> ConciergeConversationSession = { ConciergeConversationSession.instance }
) : ConciergeDataHandoffForwarder {
    override fun forward(
        result: ConciergeDataHandoffEvent,
        completion: (DataHandoffDeliveryResult) -> Unit
    ) {
        session().enqueueDataHandoff(result, completion)
    }
}
