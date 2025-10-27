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
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.util.DataReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


/**
 * Thread-safe singleton repository that holds shared state for the Concierge extension.
 *
 * This repository acts as an in-memory store that is populated by [ConciergeExtension]
 * and consumed by UI components like the ConciergeChatViewModel.
 *
 * Data is exposed as [StateFlow] instances for reactive observation.
 */
internal class ConciergeStateRepository private constructor() {

    /**
     * The Experience Cloud ID (ECID) from the EdgeIdentity extension.
     * Null indicates the ECID is not yet available or failed to load.
     */
    private val _experienceCloudId = MutableStateFlow<String?>(null)
    val experienceCloudId: StateFlow<String?> = _experienceCloudId.asStateFlow()

    private val _configurationReady = MutableStateFlow(false)
    val configurationReady: StateFlow<Boolean> = _configurationReady.asStateFlow()

    /**
     * Updates the Experience Cloud ID.
     * This should be called by the ConciergeExtension when ECID becomes available.
     *
     * @param ecid The Experience Cloud ID, or null if unavailable
     */
    fun updateExperienceCloudId(api: ExtensionApi, event: Event) {
        val edgeIdentitySharedState = getXDMSharedState(
            api,
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        val identityMap =
            DataReader.optTypedMap(
                Any::class.java,
                edgeIdentitySharedState,
                ConciergeConstants.SharedState.EdgeIdentity.IDENTITY_MAP,
                null
            )
        val ecids: MutableList<MutableMap<String?, Any?>?> =
            DataReader.optTypedListOfMap(
                Any::class.java,
                identityMap,
                ConciergeConstants.SharedState.EdgeIdentity.ECID,
                null
            )

        val ecidMap = ecids.firstOrNull()

        val ecid =
            DataReader.optString(ecidMap, ConciergeConstants.SharedState.EdgeIdentity.ID, null)
                ?.takeIf { it.isNotEmpty() }
        _experienceCloudId.update { ecid }
    }

    fun onConfigurationAvailable() {
        _configurationReady.update { true }
    }

    /**
     * Clears all stored state.
     * This can be called when the extension is unregistered or for testing purposes.
     */
    fun clear() {
        _experienceCloudId.value = null
        _configurationReady.value = false
    }

    private fun getXDMSharedState(
        api: ExtensionApi,
        extensionName: String,
        event: Event?
    ): MutableMap<String?, Any?>? =
        api.getXDMSharedState(
            extensionName, event, false, SharedStateResolution.LAST_SET
        )?.value

    companion object {
        /**
         * Thread-safe singleton instance using Kotlin's lazy delegate.
         * The lazy delegate uses double-checked locking by default (LazyThreadSafetyMode.SYNCHRONIZED).
         */
        internal val instance: ConciergeStateRepository by lazy {
            ConciergeStateRepository()
        }
    }
}

