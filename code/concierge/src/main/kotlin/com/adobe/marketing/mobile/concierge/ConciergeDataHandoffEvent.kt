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
 * A generic payload the SDK forwards toward Brand Concierge on behalf of a host app, without the
 * user typing or saying anything in chat. The SDK does not interpret [xdmFields]'s contents.
 *
 * Internal wire-format plumbing for [Concierge.sendDataHandoff] — [toEventData] builds the
 * request [com.adobe.marketing.mobile.Event]'s data (type [ConciergeConstants.EventType.CONCIERGE],
 * source `com.adobe.marketing.mobile.EventSource.REQUEST_CONTENT`, name
 * [ConciergeConstants.DataHandoff.EventName.REQUEST]) and [fromEventData] decodes it back on the
 * extension side. The response reports the final validation or delivery outcome (see
 * [ConciergeConstants.DataHandoff.ResponseKey]). A successfully delivered handoff renders its
 * response through the active Concierge chat session.
 *
 * @property routingHint A keyword the user never sees, consumed by Brand Concierge's
 * phrase-based routing. It may be blank when the XDM fields alone determine routing.
 * @property xdmFields Arbitrary XDM data merged into the root of the outbound `xdm` object.
 * Required and must be non-empty. Every key must be a `String`; top-level keys colliding with
 * [ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS] are rejected, as is any value that isn't
 * JSON-safe (`String`, `Boolean`, finite `Int`/`Long`/`Double`/`Float`, or a `Map`/`List` of
 * further JSON-safe values, up to a bounded nesting depth).
 * @property localMessage Message to render in chat immediately before its queued handoff starts.
 */
internal data class ConciergeDataHandoffEvent(
    val routingHint: String,
    val xdmFields: Map<String, Any>,
    val localMessage: String? = null
) {

    /** Converts this to the `Map<String, Any>` shape expected by [com.adobe.marketing.mobile.Event.Builder.setEventData]. */
    internal fun toEventData(): Map<String, Any> {
        val keys = ConciergeConstants.DataHandoff.EventData.Key
        val data = mutableMapOf<String, Any>(
            keys.ROUTING_HINT to routingHint,
            keys.XDM_FIELDS to xdmFields
        )
        localMessage?.let { data[keys.LOCAL_MESSAGE] = it }
        return data
    }

    companion object {

        // Recursion depth cap for isJsonSafeValue.
        private const val MAX_XDM_FIELD_VALUE_DEPTH = 20

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
            val reasons = ConciergeConstants.DataHandoff.RejectReason

            // Absent or blank both mean "no hint" - xdmFields alone can carry enough routing
            // context on its own, so neither is rejected. A present, wrong-typed value still is -
            // including an explicit null, which Map.get() can't tell apart from an absent key
            // without checking containsKey first.
            val routingHint = when (val rawRoutingHint = data[keys.ROUTING_HINT]) {
                null -> if (keys.ROUTING_HINT in data) {
                    return DataHandoffDecodeResult.Rejected(reasons.INVALID_ROUTING_HINT_TYPE)
                } else {
                    ""
                }
                is String -> rawRoutingHint
                else -> return DataHandoffDecodeResult.Rejected(reasons.INVALID_ROUTING_HINT_TYPE)
            }
            if (keys.XDM_FIELDS !in data) {
                return DataHandoffDecodeResult.Rejected(reasons.MISSING_XDM_FIELDS)
            }
            val rawXdmFields = data[keys.XDM_FIELDS] as? Map<*, *>
                ?: return DataHandoffDecodeResult.Rejected(reasons.INVALID_XDM_FIELDS_TYPE)
            if (rawXdmFields.isEmpty()) {
                return DataHandoffDecodeResult.Rejected(reasons.EMPTY_XDM_FIELDS)
            }

            for ((key, value) in rawXdmFields) {
                if (key !is String) {
                    return DataHandoffDecodeResult.Rejected(reasons.INVALID_XDM_FIELD_KEY)
                }
                if (key in ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS) {
                    return DataHandoffDecodeResult.Rejected(reasons.RESERVED_KEY_COLLISION)
                }
                if (!isJsonSafeValue(value, depth = 0)) {
                    return DataHandoffDecodeResult.Rejected(reasons.INVALID_XDM_FIELD_VALUE)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val xdmFields = rawXdmFields as Map<String, Any>

            // Missing, wrong-typed, or blank localMessage decodes to null, not a rejection.
            val localMessage = (data[keys.LOCAL_MESSAGE] as? String)?.takeIf { it.isNotBlank() }

            return DataHandoffDecodeResult.Success(
                ConciergeDataHandoffEvent(
                    routingHint = routingHint.takeUnless { it.isBlank() }.orEmpty(),
                    xdmFields = xdmFields,
                    localMessage = localMessage
                )
            )
        }

        private fun isJsonSafeValue(value: Any?, depth: Int): Boolean {
            if (depth > MAX_XDM_FIELD_VALUE_DEPTH) return false
            return when (value) {
                is String, is Boolean, is Int, is Long -> true
                is Double -> value.isFinite()
                is Float -> value.isFinite()
                is Map<*, *> -> value.keys.all { it is String } && value.values.all { isJsonSafeValue(it, depth + 1) }
                is List<*> -> value.all { isJsonSafeValue(it, depth + 1) }
                else -> false // covers null and any other non-JSON-safe type
            }
        }
    }
}

/** Result of decoding untrusted app-supplied event data into a [ConciergeDataHandoffEvent]. */
internal sealed class DataHandoffDecodeResult {
    data class Success(val result: ConciergeDataHandoffEvent) : DataHandoffDecodeResult()
    data class Rejected(val reason: String) : DataHandoffDecodeResult()
}
