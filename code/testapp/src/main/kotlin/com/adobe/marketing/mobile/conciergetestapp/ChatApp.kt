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
import com.adobe.marketing.mobile.Analytics
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Identity
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.edge.consent.Consent
import com.adobe.marketing.mobile.edge.identity.Identity as EdgeIdentity
import com.adobe.marketing.mobile.Lifecycle

class ChatApp : Application() {
    companion object {
        private const val LOG_TAG = "ChatApp"
        private const val APP_ID = "3149c49c3910/629a865c475d/launch-82c478370074"
    }

    override fun onCreate() {
        super.onCreate()
        MobileCore.setApplication(this)
        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        val extensions = listOf(
            Consent.EXTENSION,
            EdgeIdentity.EXTENSION,
            Identity.EXTENSION,
            Edge.EXTENSION,
            Analytics.EXTENSION,
            Concierge.EXTENSION,
            Lifecycle.EXTENSION
        )
        MobileCore.configureWithAppID(APP_ID)
        MobileCore.registerExtensions(extensions) {
            MobileCore.updateConfiguration(
                mapOf<String, String>(
                    "concierge.configId" to "4eb99a22-309c-471c-a1ab-f82560d87a0d",
                    "concierge.server" to "edge.adobedc.net"
                )
            )
            ConciergeTracker.start()
        }

//        MobileCore.initialize(this, APP_ID) {
//            MobileCore.updateConfiguration(
//                mapOf<String, String>(
//                    "concierge.configId" to "4eb99a22-309c-471c-a1ab-f82560d87a0d",
//                    "concierge.server" to "edge.adobedc.net"
//                )
//            )
//            ConciergeTracker.start()
//        }
    }
}
