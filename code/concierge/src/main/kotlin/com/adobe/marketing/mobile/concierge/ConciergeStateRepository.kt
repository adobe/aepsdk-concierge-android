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
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateStatus
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.util.DataReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val consent: String? = ConciergeConstants.ConsentValues.DEFAULT_VALUE,
    val resetGeneration: Long = 0,
    val resetInProgress: Boolean = false
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
    initialState: ConciergeState = ConciergeState(),
    private val sessionManager: ConciergeSessionManager = ConciergeSessionManager.instance,
    private val resetWarningScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    companion object {
        const val LOG_TAG = "ConciergeStateRepository"
        internal const val RESET_WARNING_DELAY_MS = 5_000L

        internal val instance: ConciergeStateRepository by lazy {
            ConciergeStateRepository()
        }
    }

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<ConciergeState> = _state.asStateFlow()
    private val xdmContextLock = Any()
    private var heldXdmContext: Map<String, Any> = emptyMap()
    private var xdmContextSession = XdmContextSession(null)
    private val resetParticipants = mutableSetOf<Any>()
    private val pendingResets = linkedMapOf<Long, PendingReset>()
    private var resetRequest: Event? = null
    private var resetCompleteEvent: Event? = null
    private var resetIdentityEvent: Event? = null
    private var resetWarningJob: Job? = null

    private class PendingReset(
        val generation: Long,
        val sessionId: String?,
        var hadConversation: Boolean,
        val awaiting: MutableSet<Any>,
        val dispatch: (Event) -> Unit,
        var conversationId: String? = null,
        var hadActiveTurn: Boolean = false
    )

    fun registerResetParticipant(participant: Any) = synchronized(xdmContextLock) {
        resetParticipants.add(participant)
    }

    fun unregisterResetParticipant(participant: Any) {
        synchronized(xdmContextLock) {
            resetParticipants.remove(participant)
            pendingResets.values.forEach { it.awaiting.remove(participant) }
            finishTeardownIfReady()
        }
    }

    fun beginIdentityReset(request: Event, dispatch: (Event) -> Unit = MobileCore::dispatchEvent) {
        synchronized(xdmContextLock) {
            val generation = _state.value.resetGeneration + 1
            val sessionId = sessionManager.storedSessionIdOrNull()
            pendingResets[generation] = PendingReset(
                generation, sessionId, sessionId != null || heldXdmContext.isNotEmpty(),
                resetParticipants.toMutableSet(), dispatch
            )
            resetRequest = request
            resetCompleteEvent = null
            resetIdentityEvent = null
            xdmContextSession.valid = false
            heldXdmContext = emptyMap()
            xdmContextSession = XdmContextSession(null)
            sessionManager.clearSession()
            _state.update {
                it.copy(resetGeneration = generation, resetInProgress = true,
                    experienceCloudId = null, identityMap = null)
            }
            scheduleIdentityResetWarning(generation)
            finishTeardownIfReady()
        }
    }

    private fun scheduleIdentityResetWarning(generation: Long) {
        cancelIdentityResetWarning()
        resetWarningJob = resetWarningScope.launch {
            delay(RESET_WARNING_DELAY_MS)
            synchronized(xdmContextLock) {
                val current = _state.value
                if (!isActive || !current.resetInProgress || current.resetGeneration != generation) return@synchronized
                val reason = if (resetCompleteEvent == null) {
                    "missing Edge Identity RESET_COMPLETE; verify Edge Identity 3.0.0 or later is registered to provide request-correlated reset completion"
                } else {
                    mutableListOf<String>().apply {
                        if (current.experienceCloudId.isNullOrEmpty()) add("resolved Edge Identity state")
                        if (!current.configurationReady || current.conciergeServer.isNullOrEmpty() ||
                            current.conciergeConfigId.isNullOrEmpty()) add("valid Concierge configuration")
                        if (pendingResets.isNotEmpty()) add("local conversation teardown")
                    }.joinToString(", ")
                }
                resetWarningJob = null
                Log.error(
                    ConciergeConstants.EXTENSION_NAME, LOG_TAG,
                    "Identity reset remains pending after $RESET_WARNING_DELAY_MS ms: $reason. Requests remain blocked until readiness is verified."
                )
            }
        }
    }

    internal fun cancelIdentityResetWarning() = synchronized(xdmContextLock) {
        resetWarningJob?.cancel()
        resetWarningJob = null
    }

    fun acknowledgeIdentityReset(
        participant: Any, generation: Long, hadConversation: Boolean,
        conversationId: String?, hadActiveTurn: Boolean
    ) = synchronized(xdmContextLock) {
        // A teardown of the newest generation also settles superseded boundaries whose
        // StateFlow emissions may have been conflated. Keep their diagnostics until then.
        val superseded = pendingResets.values.filter { it.generation <= generation && participant in it.awaiting }
        superseded.forEachIndexed { index, reset ->
            if (reset.awaiting.remove(participant)) {
                // Work still retained by this participant belongs to its earliest unsettled
                // boundary, not to every reset received while that teardown was pending.
                if (index == 0) {
                    reset.hadConversation = reset.hadConversation || hadConversation
                    reset.conversationId = reset.conversationId ?: conversationId
                    reset.hadActiveTurn = reset.hadActiveTurn || hadActiveTurn
                }
            }
        }
        finishTeardownIfReady()
    }

    private fun finishTeardownIfReady() {
        val settled = pendingResets.values.filter { it.awaiting.isEmpty() }
        settled.forEach { reset ->
            pendingResets.remove(reset.generation)
            if (reset.hadConversation) {
                reset.dispatch(ConciergeTrackingEvent.ConversationEnded(
                    System.currentTimeMillis(), reset.sessionId, reset.conversationId, reset.hadActiveTurn
                ).toEvent())
            }
        }
        finishIdentityReadiness()
    }

    fun completeIdentityReset(api: ExtensionApi, event: Event) {
        synchronized(xdmContextLock) {
            val request = resetRequest ?: return
            if (!_state.value.resetInProgress ||
                event.responseID != request.uniqueIdentifier ||
                resetCompleteEvent != null) return
            resetCompleteEvent = event
            resetIdentityEvent = event
            refreshResetIdentity(api)
        }
    }

    private fun refreshResetIdentity(api: ExtensionApi) {
        val event = resetIdentityEvent ?: return
        val result = api.getXDMSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event, false, SharedStateResolution.ANY
        )
        if (result?.status != SharedStateStatus.SET) {
            _state.update { it.copy(experienceCloudId = null, identityMap = null) }
            Log.warning(ConciergeConstants.EXTENSION_NAME, LOG_TAG, "Identity reset is waiting for resolved Edge Identity state.")
            return
        }
        val identityMap = DataReader.optTypedMap(Any::class.java, result.value,
            ConciergeConstants.SharedState.EdgeIdentity.IDENTITY_MAP, null)
        _state.update { it.copy(experienceCloudId = extractEcid(identityMap), identityMap = identityMap) }
        finishIdentityReadiness()
    }

    private fun finishIdentityReadiness() {
        val current = _state.value
        if (current.resetInProgress && resetCompleteEvent != null && pendingResets.isEmpty() && current.configurationReady &&
            !current.experienceCloudId.isNullOrEmpty() && !current.conciergeServer.isNullOrEmpty() &&
            !current.conciergeConfigId.isNullOrEmpty()) {
            _state.update { it.copy(resetInProgress = false) }
            cancelIdentityResetWarning()
        }
    }

    fun <T> withConversation(generation: Long, operation: () -> T): T = synchronized(xdmContextLock) {
        if (_state.value.resetInProgress || generation != _state.value.resetGeneration) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, LOG_TAG, "Discarding conversation work across an identity reset.")
            throw CancellationException("Conversation ended by identity reset")
        }
        operation()
    }

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
            ConciergeXdmValue.copyAndValidate(value, allowNull = true)
        }

        synchronized(xdmContextLock) {
            val sessionId = sessionManager.currentSessionIdOrNull()
            val reuseContext = xdmContextSession.id == null || xdmContextSession.id == sessionId
            val base = if (reuseContext) heldXdmContext else emptyMap()
            val session = if (reuseContext) xdmContextSession else XdmContextSession(sessionId)
            heldXdmContext = copyXdmObject(mergeXdmPatch(base, copiedPatch))
            session.id = sessionId
            xdmContextSession = session
        }
    }

    /**
     * Returns an isolated snapshot of the context for [sessionId].
     */
    fun snapshotXDMContext(sessionId: String): Map<String, Any> =
        resolveXDMContext(captureXDMContext(), sessionId)

    // Pending snapshots share a binding so they can be adopted once, but not into later sessions.
    internal class XdmContextSession(var id: String?, var valid: Boolean = true)

    internal data class XdmContextSnapshot(
        val fields: Map<String, Any>,
        internal val session: XdmContextSession,
        val generation: Long = 0
    )

    fun captureXDMContext(): XdmContextSnapshot = synchronized(xdmContextLock) {
        XdmContextSnapshot(copyXdmObject(heldXdmContext), xdmContextSession, _state.value.resetGeneration)
    }

    fun resolveXDMContext(snapshot: XdmContextSnapshot, sessionId: String): Map<String, Any> =
        synchronized(xdmContextLock) {
            if (!snapshot.session.valid) return@synchronized emptyMap()
            if (snapshot.session.id == null) {
                snapshot.session.id = sessionId
            }
            // An older queued snapshot must not clear context re-established for a newer session.
            if (xdmContextSession === snapshot.session && snapshot.session.id != sessionId) {
                heldXdmContext = emptyMap()
                xdmContextSession = XdmContextSession(sessionId)
            }
            if (snapshot.session.id == sessionId) snapshot.fields else emptyMap()
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
    fun updateIdentity(api: ExtensionApi, event: Event) = synchronized(xdmContextLock) {
        if (_state.value.resetInProgress) {
            val completion = resetCompleteEvent ?: return
            if (event.timestamp >= completion.timestamp &&
                event.timestamp >= (resetIdentityEvent?.timestamp ?: completion.timestamp)) {
                resetIdentityEvent = event
            }
            refreshResetIdentity(api)
            return
        }
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
    fun updateConfiguration(configuration: SharedStateResult?) = synchronized(xdmContextLock) {
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
            return@synchronized
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
        finishIdentityReadiness()
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
        synchronized(xdmContextLock) {
            cancelIdentityResetWarning()
            _state.value = ConciergeState()
            pendingResets.clear()
            resetRequest = null
            resetCompleteEvent = null
            resetIdentityEvent = null
            resetParticipants.clear()
            xdmContextSession.valid = false
            heldXdmContext = emptyMap()
            xdmContextSession = XdmContextSession(null)
        }
    }

    /**
     * Deep-merges [overlay] on top of [base] using the same RFC 7396 semantics as
     * [updateXDMContext], so callers that combine held context with per-request fields get the
     * same merge behavior as the held context itself.
     */
    fun mergeXdmFields(base: Map<String, Any>, overlay: Map<String, Any>): Map<String, Any> =
        copyXdmObject(mergeXdmPatch(copyXdmObject(base), copyXdmObject(overlay)))

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
        value.mapValues { (_, nestedValue) -> ConciergeXdmValue.copyAndValidate(nestedValue, allowNull = false)!! }

    private fun getXDMSharedState(
        api: ExtensionApi,
        extensionName: String,
        event: Event?
    ): MutableMap<String?, Any?>? =
        api.getXDMSharedState(
            extensionName, event, false, SharedStateResolution.LAST_SET
        )?.value
}
