/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge

/**
 * Why the SDK rejected a [Concierge.sendDataHandoff] call, or ([NO_RESPONSE]) when no response
 * arrived at all.
 *
 * @property rawValue the wire-format string this case represents.
 */
enum class ConciergeDataHandoffRejectReason(val rawValue: String) {
    /** The SDK received no event data at all for this request. */
    MISSING_EVENT_DATA(ConciergeConstants.DataHandoff.RejectReason.MISSING_EVENT_DATA),

    /** `routingHint` was missing or blank. */
    MISSING_ROUTING_HINT(ConciergeConstants.DataHandoff.RejectReason.MISSING_ROUTING_HINT),

    /** `routingHint` was present but not a `String`. */
    INVALID_ROUTING_HINT_TYPE(ConciergeConstants.DataHandoff.RejectReason.INVALID_ROUTING_HINT_TYPE),

    /** `xdmFields` was missing. */
    MISSING_XDM_FIELDS(ConciergeConstants.DataHandoff.RejectReason.MISSING_XDM_FIELDS),

    /** `xdmFields` was present but not a `Map`. */
    INVALID_XDM_FIELDS_TYPE(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELDS_TYPE),

    /** `xdmFields` was empty. */
    EMPTY_XDM_FIELDS(ConciergeConstants.DataHandoff.RejectReason.EMPTY_XDM_FIELDS),

    /** `xdmFields` contained a non-`String` key. */
    INVALID_XDM_FIELD_KEY(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_KEY),

    /** `xdmFields` used a reserved top-level key (e.g. `identityMap`). */
    RESERVED_KEY_COLLISION(ConciergeConstants.DataHandoff.RejectReason.RESERVED_KEY_COLLISION),

    /** `xdmFields` contained a value that isn't JSON-safe. */
    INVALID_XDM_FIELD_VALUE(ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE),

    /** The extension never responded (e.g. the call timed out). */
    NO_RESPONSE(ConciergeConstants.DataHandoff.RejectReason.NO_RESPONSE);

    internal companion object {
        /** Returns the case matching [rawValue], or null when it doesn't match any known reason. */
        internal fun fromRawValue(rawValue: String?): ConciergeDataHandoffRejectReason? =
            values().firstOrNull { it.rawValue == rawValue }
    }
}
