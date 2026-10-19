/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.cards

import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

/**
 * Owns the native cards of one chat: their element, saved state, and the busy/stale flags their
 * `context` reports. Main-thread confined, except [RenderableCard.saveState].
 */
internal class NativeCardController(
    private val scope: CoroutineScope,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val cardIdGenerator: () -> String = { UUID.randomUUID().toString() },
    private val rendererLookup: (String?) -> ConciergeCardRenderer? = { ConciergeCardRenderers.rendererFor(it) },
    private val anyRendererRegistered: () -> Boolean = { ConciergeCardRenderers.hasAnyRenderer }
) {
    private companion object {
        const val TAG = "NativeCardController"
    }

    object ContextKeys {
        const val CARD_ID = "cardId"
        const val SESSION_ID = "sessionId"
        const val CONVERSATION_ID = "conversationId"
        const val INTERACTION_ID = "interactionId"
        const val IS_BUSY = "isBusy"
        const val IS_STALE = "isStale"
        const val STATE = "state"
    }

    class ActivationRequest(
        val element: Map<String, Any?>,
        val type: String,
        val turnId: String,
        val position: Int,
        val sessionId: String? = null,
        val conversationId: String? = null,
        val interactionId: String? = null
    )

    sealed class ActivationResult {
        data class Activated(val cardId: String) : ActivationResult()
        data class AlreadyActive(val cardId: String) : ActivationResult()
        object Rejected : ActivationResult()
    }

    /** What the transcript needs to draw one card. */
    class RenderableCard internal constructor(
        val cardId: String,
        val renderer: ConciergeCardRenderer,
        val element: Map<String, Any?>,
        val isStale: StateFlow<Boolean>,
        val state: StateFlow<Map<String, Any?>?>,
        val saveState: (Map<String, Any?>) -> Unit,
        private val identifiers: Map<String, String>
    ) {
        /** The renderer's `context`; absent identifiers and an unsaved state are omitted. */
        fun rendererContext(isBusy: Boolean, isStale: Boolean, state: Map<String, Any?>?): Map<String, Any?> {
            val context = LinkedHashMap<String, Any?>(identifiers.size + 4)
            context[ContextKeys.CARD_ID] = cardId
            context.putAll(identifiers)
            context[ContextKeys.IS_BUSY] = isBusy
            context[ContextKeys.IS_STALE] = isStale
            state?.let { context[ContextKeys.STATE] = it }
            return Collections.unmodifiableMap(context)
        }
    }

    private class CardEntry(
        val card: RenderableCard,
        val elementKey: String?,
        val position: Int,
        val stale: MutableStateFlow<Boolean>,
        val state: MutableStateFlow<Map<String, Any?>?>
    ) {
        @Volatile
        var isDisposed = false
    }

    private val busyState = MutableStateFlow(false)
    private var allCardsStale = false
    private val entries = LinkedHashMap<String, CardEntry>()
    private val cardIdsByElementKey = HashMap<String, String>()

    /** True while a chat or handoff turn is queued or running. */
    val isBusy: StateFlow<Boolean> = busyState.asStateFlow()

    fun hasRenderer(type: String?): Boolean = rendererLookup(type) != null

    val hasAnyRenderer: Boolean
        get() = anyRendererRegistered()

    fun setBusy(busy: Boolean) {
        busyState.value = busy
    }

    fun activate(request: ActivationRequest): ActivationResult {
        val renderer = rendererLookup(request.type) ?: return ActivationResult.Rejected
        val elementId = request.element["id"]?.toString()?.takeIf { it.isNotEmpty() }
        val key = "${request.turnId}/${elementId ?: "#${request.position}"}"

        cardIdsByElementKey[key]?.let { existingId ->
            if (entries[existingId]?.position == request.position) return ActivationResult.AlreadyActive(existingId)
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Element ${request.position} reuses element id '$elementId' within one response; not activating it."
            )
            return ActivationResult.Rejected
        }

        val element = try {
            CardJson.readOnlyCopy(request.element)
        } catch (e: IllegalArgumentException) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Element ${request.position} (type=${request.type}) isn't valid JSON data: ${e.message}"
            )
            return ActivationResult.Rejected
        }

        val cardId = cardIdGenerator()
        val stale = MutableStateFlow(allCardsStale)
        val state = MutableStateFlow<Map<String, Any?>?>(null)
        val identifiers = LinkedHashMap<String, String>(3).apply {
            request.sessionId?.let { put(ContextKeys.SESSION_ID, it) }
            request.conversationId?.let { put(ContextKeys.CONVERSATION_ID, it) }
            request.interactionId?.let { put(ContextKeys.INTERACTION_ID, it) }
        }
        lateinit var entry: CardEntry
        val card = RenderableCard(
            cardId = cardId,
            renderer = renderer,
            element = element,
            isStale = stale.asStateFlow(),
            state = state.asStateFlow(),
            saveState = { saveState(entry, cardId, it) },
            identifiers = identifiers
        )
        entry = CardEntry(card, key, request.position, stale, state)
        entries[cardId] = entry
        cardIdsByElementKey[key] = cardId
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Activated card $cardId (type=${request.type}).")
        return ActivationResult.Activated(cardId)
    }

    fun renderableCard(cardId: String): RenderableCard? = entries[cardId]?.takeUnless { it.isDisposed }?.card

    private fun saveState(entry: CardEntry, cardId: String, state: Map<String, Any?>) {
        if (entry.isDisposed) return
        val copy = try {
            CardJson.readOnlyCopy(state)
        } catch (e: IllegalArgumentException) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Ignoring saveState for card $cardId: not valid JSON data (${e.message})."
            )
            return
        }
        scope.launch(mainDispatcher) {
            if (!entry.isDisposed) entry.state.value = copy
        }
    }

    /** Marks every current and future card of this chat stale, e.g. when the identity resets. */
    fun markAllStale() {
        if (allCardsStale) return
        allCardsStale = true
        entries.values.forEach { it.stale.value = true }
        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Marked ${entries.size} card(s) stale.")
    }

    /** Drops every card, e.g. when the transcript is cleared; later saveState calls are ignored. */
    fun disposeAll() {
        entries.values.forEach(::dispose)
        entries.clear()
        cardIdsByElementKey.clear()
        allCardsStale = false
    }

    /** Drops cards whose transcript message was rolled back. */
    fun disposeAllExcept(cardIds: Set<String>) {
        val iterator = entries.entries.iterator()
        while (iterator.hasNext()) {
            val (cardId, entry) = iterator.next()
            if (cardId in cardIds) continue
            dispose(entry)
            iterator.remove()
            entry.elementKey?.let { key -> if (cardIdsByElementKey[key] == cardId) cardIdsByElementKey.remove(key) }
        }
    }

    private fun dispose(entry: CardEntry) {
        entry.isDisposed = true
        entry.stale.value = true
    }
}
