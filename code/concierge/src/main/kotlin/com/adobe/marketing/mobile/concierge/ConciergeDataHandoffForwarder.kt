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

import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Seam for forwarding an accepted [ConciergeDataHandoffEvent] to Brand Concierge. Fire-and-forget
 * — there is no delivery-confirmation signal today (accept/reject only confirms the SDK validated
 * the payload's shape, not that Brand Concierge received it).
 */
internal interface ConciergeDataHandoffForwarder {
    fun forward(result: ConciergeDataHandoffEvent)
}

internal class BrandConciergeDataHandoffForwarder internal constructor(
    private val conversationService: ConversationService = ConciergeConversationServiceClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ConciergeDataHandoffForwarder {
    companion object {
        private const val SELF_TAG = "BrandConciergeDataHandoffForwarder"

        internal val instance: BrandConciergeDataHandoffForwarder by lazy {
            BrandConciergeDataHandoffForwarder()
        }
    }

    override fun forward(result: ConciergeDataHandoffEvent) {
        scope.launch {
            try {
                conversationService.sendDataHandoff(result.routingHint, result.xdmFields).collect()
            } catch (e: Exception) {
                Log.warning(
                    ConciergeConstants.EXTENSION_NAME,
                    SELF_TAG,
                    "Failed to forward data handoff event (routingHint=${result.routingHint}): ${e.message}"
                )
            }
        }
    }
}
