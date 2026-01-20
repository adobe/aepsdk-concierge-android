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
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.Extension
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_FRIENDLY_NAME
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_NAME
import com.adobe.marketing.mobile.concierge.ConciergeConstants.EXTENSION_VERSION
import com.adobe.marketing.mobile.services.Log

class ConciergeExtension(extensionApi: ExtensionApi) : Extension(extensionApi) {
    companion object {
        private const val SELF_TAG = "ConciergeExtension"
    }

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
        api.registerEventListener(
            EventType.HUB,
            EventSource.SHARED_STATE,
            this::processEvent)
        api.registerEventListener(
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT,
            this::processEvent);
    }

    override fun readyForEvent(event: Event): Boolean {
        return true
    }

    internal fun processEvent(event: Event) {
        Log.trace(
            EXTENSION_NAME,
            SELF_TAG,
            "Processing event of type: ${event.type} and source: ${event.source}"
        )

        if (event.isIdentitySharedStateEvent()) {
            Log.trace(
                EXTENSION_NAME,
                SELF_TAG,
                "Identity shared state event received."
            )
            ConciergeStateRepository.instance.updateExperienceCloudId(api, event)
        } else if (event.isConsentSharedStateEvent()) {
            Log.trace(
                EXTENSION_NAME,
                SELF_TAG,
                "Consent shared state event received."
            )
            ConciergeStateRepository.instance.updateConsent(api, event)
        } else if (event.isConfigurationResponse()) {
            Log.trace(
                EXTENSION_NAME,
                SELF_TAG,
                "Configuration response event received."
            )
            val configState = getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                event
            )
            ConciergeStateRepository.instance.updateConfiguration(configState)
        }
    }

    internal fun Event.isIdentitySharedStateEvent(): Boolean {
        return this.type == EventType.HUB &&
            this.source == EventSource.SHARED_STATE &&
                eventData?.get("stateowner") == ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME
    }

    internal fun Event.isConsentSharedStateEvent(): Boolean {
        return this.type == EventType.HUB &&
            this.source == EventSource.SHARED_STATE &&
                eventData?.get("stateowner") == ConciergeConstants.SharedState.Consent.EXTENSION_NAME
    }

    internal fun Event.isConfigurationResponse(): Boolean {
        return this.type == EventType.CONFIGURATION &&
            this.source == EventSource.RESPONSE_CONTENT
    }

    private fun hasValidXdmSharedState(extensionName: String, event: Event): Boolean {
        val sharedState: Map<String, Any?>? = api.getXDMSharedState(
            extensionName,
            event,
            false,
            SharedStateResolution.LAST_SET
        )?.value
        return sharedState != null && sharedState.isNotEmpty()
    }

    private fun getSharedState(extensionName: String, event: Event): SharedStateResult? {
        return api.getSharedState(
            extensionName,
            event,
            false,
            SharedStateResolution.LAST_SET
        )
    }

}