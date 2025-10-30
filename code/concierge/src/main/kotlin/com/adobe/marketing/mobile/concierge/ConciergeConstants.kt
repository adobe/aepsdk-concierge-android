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

object ConciergeConstants {
    const val EXTENSION_NAME = "concierge"
    const val EXTENSION_FRIENDLY_NAME = "Concierge"
    const val EXTENSION_VERSION = "3.0.0"

    object ChatInteraction {
        const val POSITIVE = "positive"
        const val NEGATIVE = "negative"
    }

    object SharedPreferences {
        const val PREFS_NAME = "concierge_prefs"
        const val KEY_HAS_SEEN_WELCOME = "has_seen_welcome"
    }

    object WelcomeCard {
        const val DEFAULT_DESCRIPTION = "I'm your personal guide to help you explore and find exactly what you need. Let's get started!"
        const val RETURNING_USER_WELCOME = "Hey, welcome back!"
        const val FIRST_TIME_WELCOME_TEMPLATE = "Welcome to %s concierge!"
        const val PROMPTS_HEADER = "Not sure where to start? Explore the suggested ideas below."
    }
}