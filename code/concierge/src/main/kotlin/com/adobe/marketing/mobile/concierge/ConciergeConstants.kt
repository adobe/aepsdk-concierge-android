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
    const val EXTENSION_VERSION = "3.0.0"
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
        const val DEFAULT_VALUE = UNKNOWN_VALUE
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
}