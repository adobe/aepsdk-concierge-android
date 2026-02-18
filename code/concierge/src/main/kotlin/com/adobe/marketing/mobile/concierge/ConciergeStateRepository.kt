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
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Represents the state of the Concierge extension.
 *
 * @property experienceCloudId The Experience Cloud ID (ECID) from the EdgeIdentity extension.
 *                              Null indicates the ECID is not yet available or failed to load.
 * @property configurationReady Indicates whether the configuration is ready.
 * @property surfaces List of surface URLs set via the [ConciergeChat] surfaces parameter.
 * @property conciergeServer Server URL from concierge.server configuration.
 * @property conciergeConfigId Configuration ID from concierge.configId configuration.
 * @property consent Consent value from the Consent extension. Default is "in".
 */
internal data class ConciergeState(
    val experienceCloudId: String? = null,
    val configurationReady: Boolean = false,
    val surfaces: List<String> = emptyList(),
    val conciergeServer: String? = null,
    val conciergeConfigId: String? = null,
    val consent: String? = ConciergeConstants.ConsentValues.DEFAULT_VALUE
)

/**
 * Thread-safe singleton repository that holds shared state for the Concierge extension.
 *
 * This repository acts as an in-memory store that is populated by [ConciergeExtension]
 * and consumed by UI components like the ConciergeChatViewModel.
 *
 * Data is exposed as [StateFlow] instances for reactive observation.
 *
 * @param initialState Optional initial state for testing purposes. Defaults to empty state.
 */
internal class ConciergeStateRepository internal constructor(
    initialState: ConciergeState = ConciergeState()
) {

    companion object {
        const val LOG_TAG = "ConciergeStateRepository"

        internal val instance: ConciergeStateRepository by lazy {
            ConciergeStateRepository()
        }
    }

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ConciergeState> = _state.asStateFlow()

    /**
     * Sets the list of surface URLs for the chat experience. Updates [ConciergeState.surfaces].
     * Pass null or empty list to clear.
     *
     * @param surfaces List of surface URLs to use for the chat experience, or null to clear.
     */
    fun setSurfaces(surfaces: List<String>?) {
        val list = surfaces?.takeIf { it.isNotEmpty() } ?: emptyList()
        _state.update { it.copy(surfaces = list) }
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Surfaces set: $surfaces"
        )
    }

    /**
     * Returns the surfaces from state.
     */
    fun getSurfaces(): List<String> {
        return _state.value.surfaces
    }

    /**
     * Updates the Experience Cloud ID.
     * This should be called by the ConciergeExtension when ECID becomes available.
     *
     * @param api The ExtensionApi instance
     * @param event The event that triggered the update
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
        _state.update { it.copy(experienceCloudId = ecid) }
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Updated concierge state with ECID: $ecid"

        )
    }

    /**
     * Updates the configuration ready state.
     * This should be called by the ConciergeExtension when configuration becomes available.
     */
    fun updateConfiguration(configuration: SharedStateResult?) {
        if (configuration?.value.isNullOrEmpty()) {
            _state.update {
                it.copy(
                    configurationReady = false,
                    conciergeServer = "",
                    conciergeConfigId = ""
                )
            }

            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                LOG_TAG,
                "Configuration is null or empty. Concierge cannot be prepared"

            )
            return
        }

        val configMap = configuration?.value as? Map<String?, Any?>

        val server: String? = configMap?.let { map ->
            DataReader.optString(
                map,
                ConciergeConstants.SharedState.Configuration.CONCIERGE_SERVER,
                null
            )
                ?.takeIf { it.isNotEmpty() }
        }
        
        val configId: String? = configMap?.let { map ->
            DataReader.optString(
                map,
                ConciergeConstants.SharedState.Configuration.CONCIERGE_CONFIG_ID,
                null
            )
                ?.takeIf { it.isNotEmpty() }
        }

        _state.update {
            it.copy(
                configurationReady = true,
                conciergeServer = server,
                conciergeConfigId = configId
            )
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Updated ConciergeState with configId: $configId, server: $server"
        )
    }

    /**
     * Updates the consent value from the Consent extension.
     * This should be called by the ConciergeExtension when consent state changes.
     *
     * @param api The ExtensionApi instance
     * @param event The event that triggered the update
     */
    fun updateConsent(api: ExtensionApi, event: Event) {
        val consentSharedState = api.getXDMSharedState(
            ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
            event,
            false,
            SharedStateResolution.LAST_SET
        )

        val consentValue = extractConsentValue(consentSharedState?.value)

        _state.update { it.copy(consent = consentValue) }
        
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Updated consent value: $consentValue"
        )
    }

    /**
     * Extracts and maps the consent value from the consent shared state.
     * 
     * @param sharedState The consent shared state map
     * @return Mapped consent value (in/out/unknown) or default value
     */
    private fun extractConsentValue(sharedState: Map<String?, Any?>?): String {
        if (sharedState == null) return ConciergeConstants.ConsentValues.DEFAULT_VALUE

        val rawConsent = DataReader.optTypedMap(Any::class.java, sharedState, 
                ConciergeConstants.SharedState.Consent.CONSENTS, null)
            ?.let { consents ->
                DataReader.optTypedMap(Any::class.java, consents, 
                    ConciergeConstants.SharedState.Consent.COLLECT, null)
            }
            ?.let { collect ->
                DataReader.optString(collect, ConciergeConstants.SharedState.Consent.VAL, null)
            }

        return rawConsent.toConsentValue()
    }

    private fun String?.toConsentValue(): String = when(this) {
        "y" -> ConciergeConstants.ConsentValues.IN_VALUE
        "n" -> ConciergeConstants.ConsentValues.OUT_VALUE
        "u" -> ConciergeConstants.ConsentValues.UNKNOWN_VALUE
        else -> ConciergeConstants.ConsentValues.DEFAULT_VALUE
    }

    /**
     * Clears all stored state.
     * This can be called when the extension is unregistered or for testing purposes.
     */
    fun clear() {
        _state.value = ConciergeState()
    }

    private fun getXDMSharedState(
        api: ExtensionApi,
        extensionName: String,
        event: Event?
    ): MutableMap<String?, Any?>? =
        api.getXDMSharedState(
            extensionName, event, false, SharedStateResolution.LAST_SET
        )?.value
}

