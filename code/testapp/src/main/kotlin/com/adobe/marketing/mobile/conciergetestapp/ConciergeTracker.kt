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

package com.adobe.marketing.mobile.conciergetestapp

import android.util.Log
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.ConciergeConstants

/**
 * Sample tracker that listens to all Brand Concierge notification events and logs
 * their event-specific payloads. A consumer app would replace the `Log.d` calls
 * with calls to its analytics pipeline (Edge.sendEvent, Adobe Analytics, a custom
 * backend, etc.). See `Documentation/edge-tracking-guide.md` for an Edge mapping.
 */
object ConciergeTracker {

    private const val LOG_TAG = "ConciergeTracker"

    fun start() {
        MobileCore.registerEventListener(
            ConciergeConstants.EventType.CONCIERGE,
            ConciergeConstants.EventSource.NOTIFICATION
        ) { event -> handleEvent(event) }
    }

    private fun handleEvent(event: Event) {
        val data = event.eventData
        if (data == null) {
            Log.w(LOG_TAG, "Concierge notification event has no data (name=${event.name})")
            return
        }
        val keys = ConciergeConstants.TrackingEvent.EventData.Key
        val xdmType = data[keys.EVENT_TYPE] as? String
        if (xdmType == null) {
            Log.w(LOG_TAG, "Concierge notification event missing concierge.eventType: data=$data")
            return
        }

        val types = ConciergeConstants.TrackingEvent.XDMType
        when (xdmType) {
            types.SESSION_INITIALIZED -> {
                Log.d(LOG_TAG, "session:initialized")
            }
            types.CHAT_OPENED -> {
                val epoch = data[keys.EPOCH_TIME] as? Long
                Log.d(LOG_TAG, "chat:opened epochTime=$epoch")
            }
            types.CHAT_CLOSED -> {
                val epoch = data[keys.EPOCH_TIME] as? Long
                val duration = data[keys.DURATION_MILLIS] as? Long
                Log.d(LOG_TAG, "chat:closed epochTime=$epoch durationMillis=$duration")
            }
            types.QUERY_SUBMITTED -> {
                val query = data[keys.QUERY] as? String
                Log.d(LOG_TAG, "query:submitted query=\"$query\"")
            }
            types.PROMPT_SUGGESTION_CLICKED -> {
                val suggestion = data[keys.SUGGESTION] as? String
                Log.d(LOG_TAG, "promptSuggestion:clicked suggestion=\"$suggestion\"")
            }
            types.WELCOME_PROMPT_SUGGESTION_CLICKED -> {
                val suggestion = data[keys.SUGGESTION] as? String
                Log.d(LOG_TAG, "welcomePromptSuggestion:clicked suggestion=\"$suggestion\"")
            }
            types.CARD_CLICKED -> {
                val element = data[keys.ELEMENT] as? Map<*, *>
                Log.d(LOG_TAG, "card:clicked element=$element")
            }
            types.MIC_BUTTON_CLICKED -> {
                Log.d(LOG_TAG, "micButton:clicked")
            }
            types.RESPONSE_STARTED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                Log.d(LOG_TAG, "response:started conversationId=$conversationId interactionId=$interactionId")
            }
            types.RESPONSE_COMPLETED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                Log.d(LOG_TAG, "response:completed conversationId=$conversationId interactionId=$interactionId")
            }
            types.CARDS_RENDERED -> {
                val displayMode = data[keys.DISPLAY_MODE] as? String
                val elements = data[keys.ELEMENTS] as? List<*>
                Log.d(LOG_TAG, "cards:rendered displayMode=$displayMode elementCount=${elements?.size ?: 0} elements=$elements")
            }
            types.FEEDBACK_SUBMITTED -> {
                val conversationId = data[keys.CONVERSATION_ID] as? String
                val interactionId = data[keys.INTERACTION_ID] as? String
                val feedbackType = data[keys.FEEDBACK_TYPE] as? String
                val selectedOptions = data[keys.SELECTED_OPTIONS] as? List<*>
                val notes = data[keys.NOTES] as? String
                Log.d(
                    LOG_TAG,
                    "feedback:submitted conversationId=$conversationId interactionId=$interactionId " +
                        "feedbackType=$feedbackType selectedOptions=$selectedOptions notes=\"$notes\""
                )
            }
            types.DISCLAIMER_LINK_CLICKED -> {
                val url = data[keys.URL] as? String
                Log.d(LOG_TAG, "disclaimerLink:clicked url=$url")
            }
            types.ERROR_OCCURRED -> {
                val errorMessage = data[keys.ERROR_MESSAGE] as? String
                Log.w(LOG_TAG, "error:occurred errorMessage=\"$errorMessage\"")
            }
            else -> {
                Log.d(LOG_TAG, "$xdmType (unhandled) data=$data")
            }
        }
    }
}
