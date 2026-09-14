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
 * A generic payload a host app hands to the Concierge SDK to forward toward the agent pipeline
 * (Brand Concierge → Product Advisor), without the user typing or saying anything in chat.
 * The SDK has no knowledge of what [xdmFields] represents — checkout is just the first use case.
 *
 * Build one of these, call [toEventData], and dispatch it via
 * `MobileCore.dispatchEventWithResponseCallback` using an [com.adobe.marketing.mobile.Event]
 * built with type [ConciergeConstants.EventType.CONCIERGE] and source
 * [ConciergeConstants.EventSource.DATA_HANDOFF]. The response reports accept/reject (see
 * [ConciergeConstants.DataHandoff.ResponseKey]); register a standing listener on
 * [ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY] for the eventual delivered/failed
 * outcome (see [ConciergeConstants.DataHandoff.DeliveryEventData]) — until a follow-up ticket
 * implements the real forward to Brand Concierge, every accepted event resolves as not-delivered
 * with [ConciergeConstants.DataHandoff.DeliveryErrorCode.NOT_IMPLEMENTED]. That delivery-result
 * event echoes this [routingHint]/[xdmFields] back verbatim, since the SDK has no correlation id
 * of its own — embed one inside [xdmFields] yourself if you need to match a result to a specific
 * submission.
 *
 * @property routingHint A keyword the user never sees, used only because Brand Concierge's
 * current routing is phrase-based. Required — not optional, since relaxing a required field to
 * optional later is non-breaking, while the reverse would not be.
 * @property xdmFields Arbitrary XDM data merged into the root of the outbound `xdm` object.
 * Required and must be non-empty. Every key must be a `String`; top-level keys colliding with
 * [ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS] are rejected, as is any value that isn't
 * JSON-safe (`String`, `Boolean`, a numeric type, or a `Map`/`List` of further JSON-safe values).
 */
data class ConciergeDataHandoffEvent(
    val routingHint: String,
    val xdmFields: Map<String, Any>
) {

    /** Converts this to the `Map<String, Any>` shape expected by [com.adobe.marketing.mobile.Event.Builder.setEventData]. */
    fun toEventData(): Map<String, Any> {
        val keys = ConciergeConstants.DataHandoff.EventData.Key
        return mapOf(
            keys.ROUTING_HINT to routingHint,
            keys.XDM_FIELDS to xdmFields
        )
    }

    companion object {

        /**
         * Decodes untrusted app-supplied event data into a [ConciergeDataHandoffEvent]. Never
         * throws — malformed, missing, or disallowed fields produce a
         * [DataHandoffDecodeResult.Rejected] with a reason code instead.
         */
        internal fun fromEventData(data: Map<String, Any?>?): DataHandoffDecodeResult {
            if (data == null) {
                return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.MISSING_EVENT_DATA)
            }
            val keys = ConciergeConstants.DataHandoff.EventData.Key

            val routingHint = (data[keys.ROUTING_HINT] as? String)?.takeIf { it.isNotBlank() }
                ?: return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.MISSING_ROUTING_HINT)

            val rawXdmFields = data[keys.XDM_FIELDS] as? Map<*, *>
                ?: return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.MISSING_XDM_FIELDS)
            if (rawXdmFields.isEmpty()) {
                return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.EMPTY_XDM_FIELDS)
            }

            if (rawXdmFields.keys.any { it !is String }) {
                return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_KEY)
            }

            if (rawXdmFields.keys.any { (it as String) in ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS }) {
                return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.RESERVED_KEY_COLLISION)
            }

            if (rawXdmFields.values.any { !isJsonSafeValue(it) }) {
                return DataHandoffDecodeResult.Rejected(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE)
            }

            @Suppress("UNCHECKED_CAST")
            val xdmFields = rawXdmFields as Map<String, Any>

            return DataHandoffDecodeResult.Success(
                ConciergeDataHandoffEvent(routingHint = routingHint, xdmFields = xdmFields)
            )
        }

        private fun isJsonSafeValue(value: Any?): Boolean = when (value) {
            is String, is Boolean, is Int, is Long, is Double, is Float -> true
            is Map<*, *> -> value.keys.all { it is String } && value.values.all { isJsonSafeValue(it) }
            is List<*> -> value.all { isJsonSafeValue(it) }
            else -> false // covers null and any other non-JSON-safe type
        }
    }
}

/** Result of decoding untrusted app-supplied event data into a [ConciergeDataHandoffEvent]. */
internal sealed class DataHandoffDecodeResult {
    data class Success(val result: ConciergeDataHandoffEvent) : DataHandoffDecodeResult()
    data class Rejected(val reason: String) : DataHandoffDecodeResult()
}
