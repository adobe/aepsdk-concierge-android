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

package com.adobe.marketing.mobile.conciergetestapp.cards

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * State of one simple-card carousel. A button press becomes an action carrying the CTA's `intent`,
 * `parameters` and `message`; the concierge answers with the next card in the flow. The list stays
 * usable after a pick, so the fan can come back and choose differently.
 *
 * A press whose outcome is unknown is never sent again: the concierge may already be acting on it,
 * so its button stays disabled and the fan is pointed at the chat instead.
 *
 * The last pick, unconfirmed presses and the retry ids of rejected presses are saved through
 * [saveState] and restored from [savedState], so they survive the card being recreated.
 */
class SimpleCardRuntime(
    val model: SimpleCardList,
    private val actions: CardActionSender,
    savedState: Map<String, Any?>? = null,
    saveState: (Map<String, Any?>) -> Unit = {},
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    companion object {
        const val MESSAGE_BUSY = "Please wait for the current reply, then try again."
        const val MESSAGE_UNAVAILABLE = "Chat isn't available right now."
        const val MESSAGE_FAILED = "Couldn't do that. Try again."
        const val MESSAGE_UNCONFIRMED = "Couldn't confirm that went through. Ask the concierge to check."

        private const val SELECTED_CARD_ID = "selectedCardId"
        private const val RETRY_IDS = "retryIds"
        private const val UNCONFIRMED = "unconfirmed"
    }

    private val saver = CardStateSaver(savedState, saveState)
    private val pendingState = MutableStateFlow<String?>(null)
    private val selectedState = MutableStateFlow<String?>(null)
    private val errorState = MutableStateFlow<String?>(null)
    private val unconfirmedState = MutableStateFlow<Set<String>>(emptySet())
    private val retryActionIds = HashMap<String, String>()

    /** [pressKey] of the button whose action is in flight; null when none is. */
    val pendingPress: StateFlow<String?> = pendingState.asStateFlow()

    /** [SimpleCard.id] of the last accepted pick. */
    val selectedCardId: StateFlow<String?> = selectedState.asStateFlow()
    val error: StateFlow<String?> = errorState.asStateFlow()

    /** [pressKey]s whose outcome is unknown; they are never sent again. */
    val unconfirmed: StateFlow<Set<String>> = unconfirmedState.asStateFlow()

    init {
        restore(savedState)
    }

    /** Adopts [state] when another instance of this card saved it after this one. */
    fun adopt(state: Map<String, Any?>?) {
        if (saver.adopt(state)) restore(state)
    }

    /**
     * Identifies one button by its position on the card, so two buttons sharing an intent (with
     * different parameters) keep their own pending label and retry `actionId`.
     */
    fun pressKey(card: SimpleCard, cta: SimpleCardCta): String {
        val index = card.ctas.indexOf(cta)
        return if (index >= 0) "${card.id}#$index" else "${card.id}#${cta.intentName}"
    }

    /**
     * Sends [cta]'s action. Ignored for a disabled card or while another press is in flight; an
     * unconfirmed press only repeats its message.
     */
    fun select(card: SimpleCard, cta: SimpleCardCta) {
        if (!card.enabled || pendingState.value != null) return
        val key = pressKey(card, cta)
        if (key in unconfirmedState.value) {
            errorState.value = MESSAGE_UNCONFIRMED
            return
        }
        val actionId = retryActionIds.getOrPut(key, idFactory)
        pendingState.value = key
        errorState.value = null
        save()
        val action = mapOf(
            "actionId" to actionId,
            "intent" to cta.intentName,
            "parameters" to cta.parameters,
            "message" to cta.localMessage.orEmpty()
        )
        try {
            actions.send(action) { outcome -> finish(key, card, outcome) }
        } catch (e: Exception) {
            // A throwing sender may already have dispatched the action.
            if (pendingState.value == key) finish(key, card, CardActionOutcome.UNCONFIRMED)
        }
    }

    private fun finish(key: String, card: SimpleCard, outcome: CardActionOutcome) {
        pendingState.value = null
        when (outcome) {
            CardActionOutcome.ACCEPTED -> {
                retryActionIds.remove(key)
                selectedState.value = card.id
            }
            CardActionOutcome.BUSY -> errorState.value = MESSAGE_BUSY
            CardActionOutcome.UNAVAILABLE -> errorState.value = MESSAGE_UNAVAILABLE
            CardActionOutcome.INVALID -> errorState.value = MESSAGE_FAILED
            CardActionOutcome.UNCONFIRMED -> {
                retryActionIds.remove(key)
                unconfirmedState.value = unconfirmedState.value + key
                errorState.value = MESSAGE_UNCONFIRMED
            }
        }
        save()
    }

    private fun save() {
        saver.save(
            mapOf(
                SELECTED_CARD_ID to selectedState.value,
                RETRY_IDS to retryActionIds.toMap(),
                UNCONFIRMED to unconfirmedState.value.toList()
            )
        )
    }

    private fun restore(state: Map<String, Any?>?) {
        selectedState.value = state?.get(SELECTED_CARD_ID) as? String
        retryActionIds.clear()
        retryActionIds.putAll(stringMap(state?.get(RETRY_IDS)))
        unconfirmedState.value = stringList(state?.get(UNCONFIRMED)).toSet()
    }
}
