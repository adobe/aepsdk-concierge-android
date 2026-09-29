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
 * @property experienceCloudId The Experience Cloud ID (ECID) derived from [identityMap]. Used as
 *                              the identity readiness signal. Null if not yet available.
 * @property identityMap The full identityMap from the EdgeIdentity extension, carried verbatim so
 *                        every namespace (not just ECID) is forwarded. Null if not yet available.
 * @property configurationReady Indicates whether the configuration is ready.
 * @property surfaces List of surface URLs set via the [ConciergeChat] surfaces parameter.
 * @property conciergeServer Server URL from concierge.server configuration.
 * @property conciergeConfigId Configuration ID from concierge.configId configuration.
 * @property conciergeRegion Region from concierge.region configuration. Null when not configured.
 * @property consent Consent value from the Consent extension. Default is "in".
 */
internal data class ConciergeState(
    val experienceCloudId: String? = null,
    val identityMap: Map<String, Any?>? = null,
    val configurationReady: Boolean = false,
    val surfaces: List<String> = emptyList(),
    val conciergeServer: String? = null,
    val conciergeConfigId: String? = null,
    val conciergeRegion: String? = null,
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
    private val xdmContextLock = Any()
    private var heldXdmContext: Map<String, Any> = emptyMap()
    private var xdmContextSessionId: String? = null

    /**
     * Applies an RFC 7396 JSON Merge Patch to the held conversational XDM context.
     *
     * Null values remove keys. Nested maps merge recursively; arrays and scalar values replace
     * their previous value. `identityMap` is owned by the SDK.
     */
    fun updateXDMContext(fields: Map<String, Any?>) {
        require(ConciergeConstants.SharedState.EdgeIdentity.IDENTITY_MAP !in fields) {
            "XDM context must not use the reserved top-level identityMap key."
        }
        val copiedPatch = fields.mapValues { (_, value) ->
            copyAndValidateXdmValue(value, allowNull = true)
        }

        synchronized(xdmContextLock) {
            heldXdmContext = mergeXdmPatch(heldXdmContext, copiedPatch)
        }
    }

    /**
     * Returns an isolated snapshot of the context for [sessionId].
     *
     * A genuinely new session *replacing* a prior one starts without whatever context that prior
     * session accumulated - the app is responsible for re-establishing it. The very first session
     * observed isn't replacing anything, so it must not clear context an app already set before
     * its first request, which is the API's primary supported use case.
     */
    fun snapshotXDMContext(sessionId: String): Map<String, Any> = synchronized(xdmContextLock) {
        if (xdmContextSessionId != null && xdmContextSessionId != sessionId) {
            heldXdmContext = emptyMap()
        }
        xdmContextSessionId = sessionId
        copyXdmObject(heldXdmContext)
    }

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
     * Stores the full EdgeIdentity identityMap verbatim, along with the derived ECID.
     * This should be called by the ConciergeExtension when identity becomes available.
     *
     * @param api The ExtensionApi instance
     * @param event The event that triggered the update
     */
    fun updateIdentity(api: ExtensionApi, event: Event) {
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
            )?.takeIf { it.isNotEmpty() }

        _state.update {
            it.copy(experienceCloudId = extractEcid(identityMap), identityMap = identityMap)
        }
        // Log namespace names only, never id values (PII).
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Updated concierge state with identityMap namespaces: ${identityMap?.keys}"
        )
    }

    /**
     * Extracts the ECID from the [identityMap], or null when absent or empty.
     */
    private fun extractEcid(identityMap: Map<String, Any?>?): String? {
        val ecidEntry = DataReader.optTypedListOfMap(
            Any::class.java,
            identityMap ?: return null,
            ConciergeConstants.SharedState.EdgeIdentity.ECID,
            null
        )?.firstOrNull()

        return DataReader.optString(ecidEntry, ConciergeConstants.SharedState.EdgeIdentity.ID, null)
            ?.takeIf { it.isNotEmpty() }
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
                    conciergeConfigId = "",
                    conciergeRegion = null
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

        val region: String? = configMap?.let { map ->
            DataReader.optString(
                map,
                ConciergeConstants.SharedState.Configuration.CONCIERGE_REGION,
                null
            )
                ?.takeIf { it.isNotEmpty() }
        }

        _state.update {
            it.copy(
                configurationReady = true,
                conciergeServer = server,
                conciergeConfigId = configId,
                conciergeRegion = region
            )
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            "Updated ConciergeState with configId: $configId, server: $server, region: $region"
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
        synchronized(xdmContextLock) {
            heldXdmContext = emptyMap()
            xdmContextSessionId = null
        }
    }

    /**
     * Deep-merges [overlay] on top of [base] using the same RFC 7396 semantics as
     * [updateXDMContext], so callers that combine held context with per-request fields get the
     * same merge behavior as the held context itself.
     */
    fun mergeXdmFields(base: Map<String, Any>, overlay: Map<String, Any>): Map<String, Any> =
        mergeXdmPatch(base, overlay)

    private fun copyAndValidateXdmValue(value: Any?, allowNull: Boolean): Any? = when (value) {
        null -> {
            require(allowNull) { "XDM values must not be null." }
            null
        }
        is String, is Boolean -> value
        is Number -> {
            require(value is Byte || value is Short || value is Int || value is Long ||
                value is Float || value is Double) {
                "Unsupported number type in XDM context: ${value::class.java.name}."
            }
            if (value is Float) require(value.isFinite()) { "XDM numbers must be finite." }
            if (value is Double) require(value.isFinite()) { "XDM numbers must be finite." }
            value
        }
        is Map<*, *> -> {
            val copied = linkedMapOf<String, Any?>()
            value.forEach { (key, nestedValue) ->
                require(key is String) { "XDM object keys must be strings." }
                copied[key] = copyAndValidateXdmValue(nestedValue, allowNull)
            }
            copied
        }
        // Null is a literal array element in JSON Merge Patch. Keep allowing it throughout the
        // array subtree, including maps nested inside the array.
        is List<*> -> value.map { copyAndValidateXdmValue(it, allowNull = true) }
        else -> throw IllegalArgumentException("Unsupported value type in XDM context: ${value::class.java.name}.")
    }

    private fun mergeXdmPatch(target: Map<String, Any>, patch: Map<String, Any?>): Map<String, Any> {
        val result = target.toMutableMap()
        patch.forEach { (key, value) ->
            if (value == null) {
                result.remove(key)
            } else if (value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val nestedPatch = value as Map<String, Any?>
                val nestedTarget = result[key] as? Map<String, Any> ?: emptyMap()
                result[key] = mergeXdmPatch(nestedTarget, nestedPatch)
            } else {
                result[key] = value
            }
        }
        return result
    }

    private fun copyXdmObject(value: Map<String, Any>): Map<String, Any> =
        value.mapValues { (_, nestedValue) -> copyAndValidateXdmValue(nestedValue, allowNull = false)!! }

    private fun getXDMSharedState(
        api: ExtensionApi,
        extensionName: String,
        event: Event?
    ): MutableMap<String?, Any?>? =
        api.getXDMSharedState(
            extensionName, event, false, SharedStateResolution.LAST_SET
        )?.value
}
