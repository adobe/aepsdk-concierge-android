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

internal object ConciergeConstants {
    const val EXTENSION_NAME = "brandconcierge"
    const val EXTENSION_FRIENDLY_NAME = "BrandConcierge"
    const val VERSION = "3.5.0"
    const val LOG_TAG = "BrandConcierge"
    const val DATA_STORE_NAME = EXTENSION_NAME

    object SharedState {
        const val STATEOWNER = "stateowner"

        object EdgeIdentity {
            const val EXTENSION_NAME = "com.adobe.edge.identity"
            const val IDENTITY_MAP = "identityMap"
            const val ECID = "ECID"
            const val ID = "id"
        }
        object Configuration {
            const val EXTENSION_NAME = "com.adobe.module.configuration"
            const val CONCIERGE_SERVER = "concierge.server"
            const val CONCIERGE_CONFIG_ID = "concierge.configId"
        }
        object Consent {
            const val EXTENSION_NAME = "com.adobe.edge.consent"
            const val CONSENTS = "consents"
            const val COLLECT = "collect"
            const val VAL = "val"
        }
    }

    object ConsentValues {
        const val IN_VALUE = "in"
        const val OUT_VALUE = "out"
        const val UNKNOWN_VALUE = "unknown"
        const val DEFAULT_VALUE = IN_VALUE
    }

    object ChatInteraction {
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
    }

    object ChatHeader {
        const val TITLE = "Concierge"
        const val SUBTITLE = "Powered by Adobe"
    }

    object DataStoreKeys {
        const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
        const val KEY_SESSION_ID = "concierge_session_id"
        const val KEY_SESSION_TIMESTAMP = "concierge_session_timestamp"
    }

    object WelcomeCard {
        const val DEFAULT_HEADING = "I'm your personal guide to help you explore and find exactly what you need. Let's get started!"
        const val RETURNING_USER_WELCOME = "Hey, welcome back!"
        const val FIRST_TIME_WELCOME_TEMPLATE = "Welcome to %s concierge!"
        const val DEFAULT_SUBHEADING = "Not sure where to start? Explore the suggested ideas below."
    }

    object Disclaimer {
        const val DEFAULT_TEXT = "AI responses may be inaccurate. Check answers and sources. {Terms}"
        const val DEFAULT_TERMS_LABEL = "Terms"
        const val DEFAULT_TERMS_URL = "https://www.adobe.com/legal/licenses-terms/adobe-gen-ai-user-guidelines.html"
    }

    object EventType {
        const val CONCIERGE = "com.adobe.eventType.concierge"
    }

    object EventSource {
        // Not available as a named constant in the Android AEP Core SDK — defined here for parity with iOS.
        const val NOTIFICATION = "com.adobe.eventSource.notification"
    }

    object TrackingEvent {
        object Name {
            const val SESSION_INITIALIZED            = "Brand Concierge Session Initialized"
            const val CHAT_OPENED                    = "Brand Concierge Chat Opened"
            const val CHAT_CLOSED                    = "Brand Concierge Chat Closed"
            const val QUERY_SUBMITTED                = "Brand Concierge Query Submitted"
            const val PROMPT_SUGGESTION_CLICKED      = "Brand Concierge Prompt Suggestion Clicked"
            const val WELCOME_PROMPT_SUGGESTION_CLICKED = "Brand Concierge Welcome Prompt Suggestion Clicked"
            const val CARD_CLICKED                   = "Brand Concierge Card Clicked"
            const val MIC_BUTTON_CLICKED             = "Brand Concierge Mic Button Clicked"
            const val RESPONSE_STARTED               = "Brand Concierge Response Started"
            const val RESPONSE_COMPLETED             = "Brand Concierge Response Completed"
            const val CARDS_RENDERED                 = "Brand Concierge Cards Rendered"
            const val FEEDBACK_SUBMITTED             = "Brand Concierge Feedback Submitted"
            const val ERROR_OCCURRED                 = "Brand Concierge Error Occurred"
            const val DISCLAIMER_LINK_CLICKED        = "Brand Concierge Disclaimer Link Clicked"
        }

        object XDMType {
            const val SESSION_INITIALIZED            = "session:initialized"
            const val CHAT_OPENED                    = "chat:opened"
            const val CHAT_CLOSED                    = "chat:closed"
            const val QUERY_SUBMITTED                = "query:submitted"
            const val PROMPT_SUGGESTION_CLICKED      = "promptSuggestion:clicked"
            const val WELCOME_PROMPT_SUGGESTION_CLICKED = "welcomePromptSuggestion:clicked"
            const val CARD_CLICKED                   = "card:clicked"
            const val MIC_BUTTON_CLICKED             = "micButton:clicked"
            const val RESPONSE_STARTED               = "response:started"
            const val RESPONSE_COMPLETED             = "response:completed"
            const val CARDS_RENDERED                 = "cards:rendered"
            const val FEEDBACK_SUBMITTED             = "feedback:submitted"
            const val ERROR_OCCURRED                 = "error:occurred"
            const val DISCLAIMER_LINK_CLICKED        = "disclaimerLink:clicked"
        }

        object EventData {
            object Key {
                const val EPOCH_TIME        = "epochTime"
                const val DURATION_MILLIS   = "durationMillis"
                const val EVENT_TYPE        = "concierge.eventType"
                const val QUERY             = "query"
                const val SUGGESTION        = "suggestion"
                const val ELEMENT           = "element"
                const val ELEMENTS          = "elements"
                const val DISPLAY_MODE      = "displayMode"
                const val CONVERSATION_ID   = "conversationId"
                const val INTERACTION_ID    = "interactionId"
                const val FEEDBACK_TYPE     = "feedbackType"
                const val SELECTED_OPTIONS  = "selectedOptions"
                const val NOTES             = "notes"
                const val URL               = "url"
                const val ERROR_MESSAGE     = "errorMessage"
            }
        }
    }
}