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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

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
        private const val RESPONSE_EVENT_NAME = "Concierge Data Handoff Event Response"
        private const val DELIVERY_EVENT_NAME = "Concierge Data Handoff Event Delivery"

        internal val instance: ConciergeDataHandoffEventHandler by lazy {
            ConciergeDataHandoffEventHandler()
        }
    }

    fun handle(event: Event) {
        when (val decoded = ConciergeDataHandoffEvent.fromEventData(event.eventData)) {
            is DataHandoffDecodeResult.Rejected -> respondRejected(event, decoded.reason)
            is DataHandoffDecodeResult.Success -> handleDecoded(event, decoded.result)
        }
    }

    private fun handleDecoded(triggerEvent: Event, result: ConciergeDataHandoffEvent) {
        respondAccepted(triggerEvent)

        coroutineScope.launch {
            val (delivered, errorCode) = try {
                withTimeout(forwardTimeoutMs) {
                    suspendCancellableCoroutine<Pair<Boolean, String?>> { continuation ->
                        val completed = AtomicBoolean(false)
                        try {
                            forwarder.forward(result) { forwardDelivered, forwardErrorCode ->
                                if (completed.compareAndSet(false, true)) {
                                    continuation.resume(forwardDelivered to forwardErrorCode)
                                }
                            }
                        } catch (e: Exception) {
                            if (completed.compareAndSet(false, true)) {
                                continuation.resumeWith(Result.failure(e))
                            }
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                false to ConciergeConstants.DataHandoff.DeliveryErrorCode.TIMEOUT
            } catch (e: Exception) {
                false to ConciergeConstants.DataHandoff.DeliveryErrorCode.UNKNOWN
            }
            dispatchDeliveryResult(result, delivered, errorCode)
        }
    }

    private fun respondRejected(triggerEvent: Event, reason: String) {
        val response = Event.Builder(
            RESPONSE_EVENT_NAME,
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.DATA_HANDOFF
        ).inResponseToEvent(triggerEvent).setEventData(
            mapOf(
                ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to false,
                ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON to reason
            )
        ).build()
        MobileCore.dispatchEvent(response)
    }

    private fun respondAccepted(triggerEvent: Event) {
        val response = Event.Builder(
            RESPONSE_EVENT_NAME,
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.DATA_HANDOFF
        ).inResponseToEvent(triggerEvent).setEventData(
            mapOf(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED to true)
        ).build()
        MobileCore.dispatchEvent(response)
    }

    private fun dispatchDeliveryResult(result: ConciergeDataHandoffEvent, delivered: Boolean, errorCode: String?) {
        val keys = ConciergeConstants.DataHandoff.DeliveryEventData.Key
        val data = mutableMapOf<String, Any>(
            keys.DELIVERED to delivered,
            keys.ROUTING_HINT to result.routingHint,
            keys.XDM_FIELDS to result.xdmFields
        )
        errorCode?.let { data[keys.DELIVERY_ERROR_CODE] = it }

        val deliveryEvent = Event.Builder(
            DELIVERY_EVENT_NAME,
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY
        ).setEventData(data).build()
        MobileCore.dispatchEvent(deliveryEvent)
    }
}
