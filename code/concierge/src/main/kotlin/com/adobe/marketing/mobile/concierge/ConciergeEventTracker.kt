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
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.services.Log

/**
 * Forwards Concierge notification events to the Adobe Experience Platform Edge Network as
 * Experience Events, without taking a compile-time dependency on the Edge extension.
 *
 * Each notification xdmType has an explicit branch that pulls the fields it expects and builds
 * the outbound `data` map. This keeps the mapping deterministic and grep-friendly: to see what
 * a given Concierge event sends to Edge, jump to the branch with that xdmType.
 *
 * Dispatch goes through `MobileCore.dispatchEvent` with type `com.adobe.eventType.edge` and
 * source `com.adobe.eventSource.requestContent`. The Edge extension — if present on the
 * classpath at runtime — picks the event up and forwards it. If Edge is missing, the event is
 * silently ignored. No compile-time coupling.
 *
 * Sanitization rules (per agreement with team):
 * - `query` (QuerySubmitted) and `notes` (FeedbackSubmitted) are dropped: free-form user text,
 *   PII risk.
 * - `element` (CardClicked) and entries in `elements` (CardsRendered) are filtered down to
 *   product-identifier fields (`productName`, `productPageURL`); display-only fields like
 *   `productDescription`, `productPrice`, `productBadge` are stripped.
 *
 * Mirrors the Optimize SDK's [`OptimizeExtension.handleTrackPropositions`] pattern for raw Edge
 * dispatch (`{xdm, data}` shape, EDGE / REQUEST_CONTENT type+source).
 */
internal object ConciergeEventTracker {

    private const val SELF_TAG = "ConciergeEventTracker"

    private const val EVENT_NAME = "Concierge Tracking Edge Request"
    private const val EDGE_EVENT_DATA_KEY_XDM = "xdm"
    private const val EDGE_EVENT_DATA_KEY_DATA = "data"
    private const val XDM_EVENT_TYPE = "eventType"

    // Fields kept when filtering card payloads.
    private const val CARD_KEY_PRODUCT_NAME = "productName"
    private const val CARD_KEY_PRODUCT_PAGE_URL = "productPageURL"

    internal var trackingEnabled = false
    internal fun enableTracking(enabled: Boolean) {
        trackingEnabled = enabled
        android.util.Log.d(ConciergeConstants.LOG_TAG, "Concierge tracking ${if(enabled) "enabled" else "disabled"}.")
    }

    /**
     * Builds and dispatches an Edge request event for the given Concierge notification.
     * No-ops if event data is missing, the routing key is absent, or the xdmType is unrecognized.
     */
    fun trackEvent(event: Event) {
        if(trackingEnabled.not()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                "Ignoring track event. Call Concierge.enableTracking() to enable tracking."
            )
            return
        }
        val data = event.eventData
        if (data == null) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                "Skipping Edge forwarding: notification event '${event.name}' has no event data."
            )
            return
        }
        val keys = ConciergeConstants.TrackingEvent.EventData.Key
        val xdmType = data[keys.EVENT_TYPE] as? String
        if (xdmType.isNullOrBlank()) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                "Skipping Edge forwarding: notification event missing routing key '${keys.EVENT_TYPE}'."
            )
            return
        }

        val types = ConciergeConstants.TrackingEvent.XDMType
        when (xdmType) {
            types.SESSION_INITIALIZED -> {
                dispatchEdge(xdmType, emptyMap())
            }
            types.CHAT_OPENED -> {
                val epoch = data[keys.EPOCH_TIME] as? Long
                val payload = mutableMapOf<String, Any>().apply {
                    epoch?.let { put(keys.EPOCH_TIME, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.CHAT_CLOSED -> {
                val epoch = data[keys.EPOCH_TIME] as? Long
                val duration = data[keys.DURATION_MILLIS] as? Long
                val payload = mutableMapOf<String, Any>().apply {
                    epoch?.let { put(keys.EPOCH_TIME, it) }
                    duration?.let { put(keys.DURATION_MILLIS, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.QUERY_SUBMITTED -> {
                // `query` is dropped (free-form user-typed text — PII risk).
                dispatchEdge(xdmType, emptyMap())
            }
            types.PROMPT_SUGGESTION_CLICKED -> {
                val suggestion = data[keys.SUGGESTION] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    suggestion?.let { put(keys.SUGGESTION, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.WELCOME_PROMPT_SUGGESTION_CLICKED -> {
                val suggestion = data[keys.SUGGESTION] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    suggestion?.let { put(keys.SUGGESTION, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.CARD_CLICKED -> {
                @Suppress("UNCHECKED_CAST")
                val element = data[keys.ELEMENT] as? Map<String, Any>
                val payload = mutableMapOf<String, Any>().apply {
                    element?.let {
                        val filtered = filterCardElement(it)
                        if (filtered.isNotEmpty()) put(keys.ELEMENT, filtered)
                    }
                }
                dispatchEdge(xdmType, payload)
            }
            types.MIC_BUTTON_CLICKED -> {
                dispatchEdge(xdmType, emptyMap())
            }
            types.RESPONSE_STARTED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    conversationId?.let { put(keys.CONVERSATION_ID, it) }
                    interactionId?.let { put(keys.INTERACTION_ID, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.RESPONSE_COMPLETED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    conversationId?.let { put(keys.CONVERSATION_ID, it) }
                    interactionId?.let { put(keys.INTERACTION_ID, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.CARDS_RENDERED -> {
                val displayMode = data[keys.DISPLAY_MODE] as? String
                @Suppress("UNCHECKED_CAST")
                val elements = data[keys.ELEMENTS] as? List<Map<String, Any>>
                val payload = mutableMapOf<String, Any>().apply {
                    displayMode?.let { put(keys.DISPLAY_MODE, it) }
                    elements?.let {
                        val filtered = it.map(::filterCardElement).filter { card -> card.isNotEmpty() }
                        if (filtered.isNotEmpty()) put(keys.ELEMENTS, filtered)
                    }
                }
                dispatchEdge(xdmType, payload)
            }
            types.FEEDBACK_SUBMITTED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                val feedbackType = data[keys.FEEDBACK_TYPE] as? String
                @Suppress("UNCHECKED_CAST")
                val selectedOptions = data[keys.SELECTED_OPTIONS] as? List<String>
                // `notes` is dropped (free-form user-typed text — PII risk).
                val payload = mutableMapOf<String, Any>().apply {
                    conversationId?.let { put(keys.CONVERSATION_ID, it) }
                    interactionId?.let { put(keys.INTERACTION_ID, it) }
                    feedbackType?.let { put(keys.FEEDBACK_TYPE, it) }
                    selectedOptions?.let { put(keys.SELECTED_OPTIONS, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.DISCLAIMER_LINK_CLICKED -> {
                val url = data[keys.URL] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    url?.let { put(keys.URL, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.ERROR_OCCURRED -> {
                val errorMessage = data[keys.ERROR_MESSAGE] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    errorMessage?.let { put(keys.ERROR_MESSAGE, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.CTA_BUTTON_CLICKED -> {
                val label = data[keys.LABEL] as? String
                val url = data[keys.URL] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    label?.let { put(keys.LABEL, it) }
                    url?.let { put(keys.URL, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            types.LINK_CLICKED -> {
                val url = data[keys.URL] as? String
                val origin = data[keys.ORIGIN] as? String
                val payload = mutableMapOf<String, Any>().apply {
                    url?.let { put(keys.URL, it) }
                    origin?.let { put(keys.ORIGIN, it) }
                }
                dispatchEdge(xdmType, payload)
            }
            else -> {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME, SELF_TAG,
                    "Skipping Edge forwarding: unrecognized xdmType '$xdmType'. " +
                        "Add a branch to ConciergeEventTracker.trackEvent to forward this event."
                )
            }
        }
    }

    /**
     * Returns a copy of a card content map keeping only fields that identify the product.
     * Strips display-only fields (`productDescription`, `productPrice`, `productBadge`).
     */
    private fun filterCardElement(card: Map<String, Any>): Map<String, Any> {
        val out = mutableMapOf<String, Any>()
        card[CARD_KEY_PRODUCT_NAME]?.let { out[CARD_KEY_PRODUCT_NAME] = it }
        card[CARD_KEY_PRODUCT_PAGE_URL]?.let { out[CARD_KEY_PRODUCT_PAGE_URL] = it }
        return out
    }

    /**
     * Builds the `(EventType.EDGE, EventSource.REQUEST_CONTENT)` event with the standard
     * `{xdm, data}` shape and dispatches it via [MobileCore].
     *
     * The xdmType is written into both:
     * - `xdm.eventType` — for tag-property Rule conditions targeting XDM
     * - `data.conciergeEventType` — for Rule conditions targeting the free-form data map
     *   (also useful when reading the dispatched event in Assurance / Logcat without inspecting xdm)
     */
    private fun dispatchEdge(xdmType: String, data: Map<String, Any>) {
        val xdm = mapOf<String, Any>(XDM_EVENT_TYPE to xdmType)
        val dataWithEventType = data.toMutableMap().apply {
            put(ConciergeConstants.TrackingEvent.EventData.Key.EVENT_TYPE, xdmType)
        }

        val edgeEvent = Event.Builder(
            EVENT_NAME,
            EventType.EDGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(
            mapOf(
                EDGE_EVENT_DATA_KEY_XDM to xdm,
                EDGE_EVENT_DATA_KEY_DATA to dataWithEventType
            )
        ).build()

        Log.trace(
            ConciergeConstants.EXTENSION_NAME, SELF_TAG,
            "Dispatching Edge request: xdm.eventType='$xdmType', data=$dataWithEventType."
        )
        MobileCore.dispatchEvent(edgeEvent)
    }
}
