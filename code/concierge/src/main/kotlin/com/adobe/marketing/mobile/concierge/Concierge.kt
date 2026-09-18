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

import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChat
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatView

/** Public class containing APIs for the Brand Concierge extension. */
object Concierge {

    /** Reference to the Concierge Extension class used for registration via `MobileCore.registerExtensions`. */
    @JvmField
    val EXTENSION: Class<out Extension> = ConciergeExtension::class.java

    /** Returns the version of the Brand Concierge extension. */
    @JvmStatic
    fun extensionVersion(): String = ConciergeConstants.VERSION

    /**
     * Enables tracking of user interactions with the concierge chat interface. This allows the extension to collect data on user behavior and interactions, which can be used for analytics and improving the concierge experience.
     *
     */
    @JvmStatic
    fun setEdgeTrackingEnabled(enabled: Boolean) {
        ConciergeEventTracker.enableTracking(enabled)
    }

    /**
     * Registers the provider the SDK consults for an authentication token before building each
     * conversation turn, for both chat and feedback requests.
     *
     * Pass null to clear a previously registered provider. Setting a provider replaces any
     * previously registered one.
     *
     * @param provider the token provider, or null to clear.
     * @param timeoutMillis how long to wait for [provider] before sending the turn without a
     * token. Defaults to 3000ms (3 seconds); clamped range rather than rejected if out of bounds.
     */
    @JvmStatic
    @JvmOverloads
    fun setAuthTokenProvider(
        provider: ConciergeAuthTokenProvider?,
        timeoutMillis: Long = ConciergeAuthTokenHolder.DEFAULT_PROVIDE_TOKEN_TIMEOUT_MS
    ) {
        ConciergeAuthTokenHolder.setProvider(provider, timeoutMillis)
    }

    /**
     * Hands data to the SDK to forward toward the Brand Concierge agent pipeline,
     * outside of normal user-typed chat.
     *
     * Keep a configured [ConciergeChat] or [ConciergeChatView] rendered while calling this API.
     * The active chat session provides both routing surfaces and the transcript that renders the
     * handoff response.
     *
     * @param routingHint a keyword the end user never sees, consumed by Brand Concierge's
     * phrase-based router (e.g. "successful-checkout"), or an empty string when the XDM fields
     * determine routing.
     * @param xdmFields arbitrary XDM data merged into the root of the outbound XDM object; the
     * SDK does not interpret its contents. Must be non-empty, JSON-safe, and must not use
     * `identityMap` (or any other SDK-reserved top-level XDM key).
     * @param localMessage text to render in chat when this handoff starts, distinct from the data
     * forwarded to Brand Concierge. The handoff is rejected with [ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS]
     * when a chat turn or another handoff is active or waiting; callers can retry after it completes.
     * @param completion invoked exactly once with the outcome, on a background thread.
     * `accepted == true` confirms the service response completed and rendered successfully.
     */
    @JvmStatic
    @JvmOverloads
    fun sendDataHandoff(
        routingHint: String,
        xdmFields: Map<String, Any>,
        localMessage: String? = null,
        completion: ConciergeDataHandoffCallback? = null
    ) {
        ConciergeDataHandoffSender.send(routingHint, xdmFields, localMessage, completion)
    }

}
