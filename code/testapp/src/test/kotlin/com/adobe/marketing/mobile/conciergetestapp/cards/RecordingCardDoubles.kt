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

/** Records actions and answers with [result]; with [deferred], answers only on [complete]. */
internal class RecordingCardActions(
    var result: CardActionOutcome = CardActionOutcome.ACCEPTED,
    var deferred: Boolean = false,
    var throwOnSend: Boolean = false
) : CardActionSender {
    val actions = mutableListOf<Map<String, Any?>>()
    private val pending = mutableListOf<(CardActionOutcome) -> Unit>()

    override fun send(action: Map<String, Any?>, onResult: (CardActionOutcome) -> Unit) {
        actions += action
        if (throwOnSend) throw IllegalStateException("sender failed")
        if (deferred) pending += onResult else onResult(result)
    }

    fun complete(result: CardActionOutcome) {
        pending.removeAt(0)(result)
    }
}

/** Stands in for the SDK's `saveState`: keeps every save, the last one being the card's state. */
internal class RecordingSaveState : (Map<String, Any?>) -> Unit {
    val saves = mutableListOf<Map<String, Any?>>()
    val last: Map<String, Any?>? get() = saves.lastOrNull()

    override fun invoke(state: Map<String, Any?>) {
        saves += state
    }
}
