/*
  Copyright 2025 Adobe. All rights reserved.
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

/**
 * Defines the tracking events dispatched by the Brand Concierge extension to the AEP Event Hub.
 *
 * Each case produces an [Event] with:
 * - type: [ConciergeConstants.EventType.CONCIERGE]
 * - source: [EventSource.NOTIFICATION]
 * - data: contains [ConciergeConstants.TrackingEvent.EventData.Key.EVENT_TYPE] plus event-specific payload
 */
internal sealed class ConciergeTrackingEvent {

    object SessionInitialized : ConciergeTrackingEvent()

    data class ChatOpened(val epochTime: Long) : ConciergeTrackingEvent()

    data class ChatClosed(val epochTime: Long, val durationMillis: Long) : ConciergeTrackingEvent()

    data class QuerySubmitted(val query: String) : ConciergeTrackingEvent()

    data class PromptSuggestionClicked(val suggestion: String) : ConciergeTrackingEvent()

    data class WelcomePromptSuggestionClicked(val suggestion: String) : ConciergeTrackingEvent()

    data class CardClicked(val element: Map<String, Any>) : ConciergeTrackingEvent()

    object MicButtonClicked : ConciergeTrackingEvent()

    data class ResponseStarted(
        val conversationId: String,
        val interactionId: String
    ) : ConciergeTrackingEvent()

    data class ResponseCompleted(
        val conversationId: String,
        val interactionId: String
    ) : ConciergeTrackingEvent()

    data class CardsRendered(
        val displayMode: String,
        val elements: List<Map<String, Any>>
    ) : ConciergeTrackingEvent()

    data class FeedbackSubmitted(
        val conversationId: String,
        val interactionId: String,
        val feedbackType: String,
        val selectedOptions: List<String>,
        val notes: String
    ) : ConciergeTrackingEvent()

    data class DisclaimerLinkClicked(
        val linkUrl: String
    ) : ConciergeTrackingEvent()

    data class ErrorOccurred(val errorMessage: String) : ConciergeTrackingEvent()

    // MARK: - Event factory

    fun toEvent(): Event = Event.Builder(eventName, ConciergeConstants.EventType.CONCIERGE, ConciergeConstants.EventSource.NOTIFICATION)
        .setEventData(eventData)
        .build()

    private val eventName: String
        get() = when (this) {
            is SessionInitialized      -> ConciergeConstants.TrackingEvent.Name.SESSION_INITIALIZED
            is ChatOpened -> ConciergeConstants.TrackingEvent.Name.CHAT_OPENED
            is ChatClosed -> ConciergeConstants.TrackingEvent.Name.CHAT_CLOSED
            is QuerySubmitted          -> ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED
            is PromptSuggestionClicked -> ConciergeConstants.TrackingEvent.Name.PROMPT_SUGGESTION_CLICKED
            is CardClicked             -> ConciergeConstants.TrackingEvent.Name.CARD_CLICKED
            is MicButtonClicked        -> ConciergeConstants.TrackingEvent.Name.MIC_BUTTON_CLICKED
            is ResponseStarted         -> ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED
            is ResponseCompleted       -> ConciergeConstants.TrackingEvent.Name.RESPONSE_COMPLETED
            is CardsRendered           -> ConciergeConstants.TrackingEvent.Name.CARDS_RENDERED
            is FeedbackSubmitted       -> ConciergeConstants.TrackingEvent.Name.FEEDBACK_SUBMITTED
            is ErrorOccurred           -> ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED
            is WelcomePromptSuggestionClicked -> ConciergeConstants.TrackingEvent.Name.WELCOME_PROMPT_SUGGESTION_CLICKED
            is DisclaimerLinkClicked -> ConciergeConstants.TrackingEvent.Name.DISCLAIMER_LINK_CLICKED

        }

    private val xdmType: String
        get() = when (this) {
            is SessionInitialized      -> ConciergeConstants.TrackingEvent.XDMType.SESSION_INITIALIZED
            is ChatOpened -> ConciergeConstants.TrackingEvent.XDMType.CHAT_OPENED
            is ChatClosed -> ConciergeConstants.TrackingEvent.XDMType.CHAT_CLOSED
            is QuerySubmitted          -> ConciergeConstants.TrackingEvent.XDMType.QUERY_SUBMITTED
            is PromptSuggestionClicked -> ConciergeConstants.TrackingEvent.XDMType.PROMPT_SUGGESTION_CLICKED
            is CardClicked             -> ConciergeConstants.TrackingEvent.XDMType.CARD_CLICKED
            is MicButtonClicked        -> ConciergeConstants.TrackingEvent.XDMType.MIC_BUTTON_CLICKED
            is ResponseStarted         -> ConciergeConstants.TrackingEvent.XDMType.RESPONSE_STARTED
            is ResponseCompleted       -> ConciergeConstants.TrackingEvent.XDMType.RESPONSE_COMPLETED
            is CardsRendered           -> ConciergeConstants.TrackingEvent.XDMType.CARDS_RENDERED
            is FeedbackSubmitted       -> ConciergeConstants.TrackingEvent.XDMType.FEEDBACK_SUBMITTED
            is ErrorOccurred           -> ConciergeConstants.TrackingEvent.XDMType.ERROR_OCCURRED
            is WelcomePromptSuggestionClicked -> ConciergeConstants.TrackingEvent.XDMType.WELCOME_PROMPT_SUGGESTION_CLICKED
            is DisclaimerLinkClicked -> ConciergeConstants.TrackingEvent.XDMType.DISCLAIMER_LINK_CLICKED

        }

    private val eventData: Map<String, Any>
        get() {
            val keys = ConciergeConstants.TrackingEvent.EventData.Key
            val data = mutableMapOf<String, Any>(keys.EVENT_TYPE to xdmType)

            when (this) {
                is SessionInitialized -> Unit

                is QuerySubmitted ->
                    data[keys.QUERY] = query

                is PromptSuggestionClicked ->
                    data[keys.SUGGESTION] = suggestion

                is WelcomePromptSuggestionClicked ->
                    data[keys.SUGGESTION] = suggestion

                is CardClicked ->
                    data[keys.ELEMENT] = element

                is MicButtonClicked -> Unit

                is ResponseStarted -> {
                    data[keys.CONVERSATION_ID] = conversationId
                    data[keys.INTERACTION_ID]  = interactionId
                }

                is ResponseCompleted -> {
                    data[keys.CONVERSATION_ID] = conversationId
                    data[keys.INTERACTION_ID]  = interactionId
                }

                is CardsRendered -> {
                    data[keys.DISPLAY_MODE] = displayMode
                    data[keys.ELEMENTS]     = elements
                }

                is FeedbackSubmitted -> {
                    data[keys.CONVERSATION_ID]  = conversationId
                    data[keys.INTERACTION_ID]   = interactionId
                    data[keys.FEEDBACK_TYPE]    = feedbackType
                    data[keys.SELECTED_OPTIONS] = selectedOptions
                    data[keys.NOTES]            = notes
                }

                is ErrorOccurred ->
                    data[keys.ERROR_MESSAGE] = errorMessage

                is DisclaimerLinkClicked -> data[keys.URL] = linkUrl
                is ChatOpened -> data[keys.EPOCH_TIME] = epochTime
                is ChatClosed -> {
                    data[keys.EPOCH_TIME] = epochTime
                    data[keys.DURATION_MILLIS] = durationMillis
                }
            }

            return data
        }
}
