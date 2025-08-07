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
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_FRIENDLY_NAME
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_NAME
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_VERSION

class ConciergeExtension(extensionApi: ExtensionApi) : Extension(extensionApi) {
    override fun getName(): String {
        return EXTENSION_NAME
    }

    override fun getFriendlyName(): String {
        return EXTENSION_FRIENDLY_NAME
    }

    override fun getVersion(): String {
        return EXTENSION_VERSION
    }

    override fun onRegistered() {
        super.onRegistered()
        // Do something here
    }

    override fun readyForEvent(event: Event): Boolean {
        return true
    }
}