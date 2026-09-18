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

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log

/**
 * Handles inbound data handoff request events (see [ConciergeExtension.isDataHandoffEvent]):
 * decodes and answers accept/reject immediately. `accepted == true` only confirms the SDK
 * validated the payload's shape — there is no delivery-confirmation signal today, so an accepted
 * event is forwarded to [forwarder] fire-and-forget with no outcome reported back to the host
 * app.
 */
internal class ConciergeDataHandoffEventHandler internal constructor(
    private val forwarder: ConciergeDataHandoffForwarder = BrandConciergeDataHandoffForwarder.instance
) {

    companion object {
        private const val SELF_TAG = "ConciergeDataHandoffEventHandler"

        internal val instance: ConciergeDataHandoffEventHandler by lazy {
            ConciergeDataHandoffEventHandler()
        }
    }

    fun handle(event: Event): Unit = when (val decoded = ConciergeDataHandoffEvent.fromEventData(event.eventData)) {
        is DataHandoffDecodeResult.Rejected -> respondRejected(event, decoded.reason)
        is DataHandoffDecodeResult.Success -> handleDecoded(event, decoded.result)
    }

    private fun handleDecoded(triggerEvent: Event, result: ConciergeDataHandoffEvent) {
        respondAccepted(triggerEvent)
        try {
            forwarder.forward(result)
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                "Forwarder threw for a data handoff event (routingHint=${result.routingHint}): ${e.message}"
            )
        }
    }

    private fun respondRejected(triggerEvent: Event, reason: String) {
        dispatch(
            triggerEvent,
            mapOf(
                ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to false,
                ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON to reason
            )
        )
    }

    private fun respondAccepted(triggerEvent: Event) {
        dispatch(triggerEvent, mapOf(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to true))
    }

    /**
     * Builds and dispatches the response [Event], logging (rather than throwing) if
     * [MobileCore.dispatchEvent] fails.
     */
    private fun dispatch(triggerEvent: Event, data: Map<String, Any>) {
        try {
            val response = Event.Builder(
                ConciergeConstants.DataHandoff.EventName.RESPONSE,
                ConciergeConstants.EventType.CONCIERGE,
                EventSource.RESPONSE_CONTENT
            ).inResponseToEvent(triggerEvent).setEventData(data).build()
            MobileCore.dispatchEvent(response)
        } catch (e: Exception) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Failed to dispatch response: ${e.message}")
        }
    }
}
