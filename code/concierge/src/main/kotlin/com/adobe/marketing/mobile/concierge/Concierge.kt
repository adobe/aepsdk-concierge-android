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
}
