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
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles inbound [ConciergeConstants.EventSource.DATA_HANDOFF] events: decodes, answers
 * accept/reject immediately (never waiting on the network forward — there is no idempotency
 * check to gate this, since there is no SDK-level correlation id), then asynchronously invokes
 * [forwarder] with timeout/exception safety and reports the outcome on
 * [ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY], echoing the original submission back
 * so a host app can self-correlate using whatever key it chose to embed in `xdmFields`.
 */
internal class ConciergeDataHandoffEventHandler internal constructor(
    private val forwarder: ConciergeDataHandoffForwarder = NotImplementedDataHandoffForwarder,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val forwardTimeoutMs: Long = 30_000L
) {

    companion object {
        private const val SELF_TAG = "ConciergeDataHandoffEventHandler"
        private const val RESPONSE_EVENT_NAME = "Concierge Data Handoff Event Response"
        private const val DELIVERY_EVENT_NAME = "Concierge Data Handoff Event Delivery"

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

        coroutineScope.launch {
            val deferred = CompletableDeferred<Pair<Boolean, String?>>()

            try {
                forwarder.forward(result) { delivered, errorCode ->
                    if (!deferred.complete(delivered to errorCode)) {
                        Log.debug(
                            ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                            "Ignoring late/duplicate onComplete for data handoff forward " +
                                "(routingHint=${result.routingHint}); outcome already reported."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!deferred.complete(false to ConciergeConstants.DataHandoff.DeliveryErrorCode.UNKNOWN)) {
                    Log.debug(
                        ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                        "Forwarder threw after already completing " +
                            "(routingHint=${result.routingHint}): ${e.message}"
                    )
                }
            }

            val outcome = withTimeoutOrNull(forwardTimeoutMs) { deferred.await() }
            val (delivered, errorCode) = outcome ?: run {
                // Mark the deferred completed ourselves so a forwarder that eventually calls
                // onComplete after this point hits the "already completed" branch above and
                // gets logged, instead of silently completing a deferred nobody awaits anymore.
                deferred.complete(false to ConciergeConstants.DataHandoff.DeliveryErrorCode.TIMEOUT)
                false to ConciergeConstants.DataHandoff.DeliveryErrorCode.TIMEOUT
            }
            dispatchDeliveryResult(result, delivered, errorCode)
        }
    }

    private fun respondRejected(triggerEvent: Event, reason: String) {
        dispatch(
            RESPONSE_EVENT_NAME,
            ConciergeConstants.EventSource.DATA_HANDOFF,
            mapOf(
                ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to false,
                ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON to reason
            ),
            triggerEvent = triggerEvent
        )
    }

    private fun respondAccepted(triggerEvent: Event) {
        dispatch(
            RESPONSE_EVENT_NAME,
            ConciergeConstants.EventSource.DATA_HANDOFF,
            mapOf(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to true),
            triggerEvent = triggerEvent
        )
    }

    private fun dispatchDeliveryResult(result: ConciergeDataHandoffEvent, delivered: Boolean, errorCode: String?) {
        val keys = ConciergeConstants.DataHandoff.DeliveryEventData.Key
        val data = mutableMapOf<String, Any>(
            keys.DELIVERED to delivered,
            keys.ROUTING_HINT to result.routingHint,
            keys.XDM_FIELDS to result.xdmFields
        )
        errorCode?.let { data[keys.DELIVERY_ERROR_CODE] = it }

        dispatch(DELIVERY_EVENT_NAME, ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY, data)
    }

    /**
     * Builds and dispatches an [Event], logging (rather than throwing) if [MobileCore.dispatchEvent]
     * fails — this runs both synchronously from [handle] and from inside [coroutineScope]'s
     * fire-and-forget coroutine, where an uncaught exception would otherwise be unrecoverable.
     */
    private fun dispatch(name: String, source: String, data: Map<String, Any>, triggerEvent: Event? = null) {
        try {
            val builder = Event.Builder(name, ConciergeConstants.EventType.CONCIERGE, source).setEventData(data)
            val event = if (triggerEvent != null) builder.inResponseToEvent(triggerEvent).build() else builder.build()
            MobileCore.dispatchEvent(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Failed to dispatch '$name': ${e.message}")
        }
    }
}
