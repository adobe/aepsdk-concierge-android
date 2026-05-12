/*
 * Copyright 2025 Adobe. All rights reserved.
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

import android.app.Application
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore

class ChatApp : Application() {
    companion object {
        private const val LOG_TAG = "ChatApp"

        // Regular environment
        // private const val APP_ID = "staging/1b50a869c4a2/570831bce333/launch-bcc070a55cca-development"

        // DSG environment
        private const val APP_ID = "staging/1b50a869c4a2/2993416661b1/launch-03ab810b3d2d-development"
    }

    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)

        MobileCore.initialize(this, APP_ID) {
            MobileCore.updateConfiguration(
                mapOf(
                    "concierge.server" to "edge.adobedc.net",
                    "concierge.configId" to "4eb99a22-309c-471c-a1ab-f82560d87a0d"
                )
            )
        }
    }
}
