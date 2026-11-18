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

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.services.Log

/** Where a Brand Concierge element carries its vendor payload. */
const val CARD_PAYLOAD_KEY = "entity_info"

/**
 * Builds a card renderer that maps an element's [CARD_PAYLOAD_KEY] value with [map] and shows it
 * as a [SimpleCardContent] carousel whose presses go to [actions]. A payload that doesn't map, or
 * whose mapper throws, shows a fallback line instead: renderers must not throw during composition.
 */
fun simpleCardRenderer(actions: CardActionSender, map: (Any?) -> SimpleCardList?): ConciergeCardRenderer =
    @Composable { element, context, saveState ->
        val model = remember(element) { mapElementOrNull(element) { map(element[CARD_PAYLOAD_KEY]) } }
        if (model == null) {
            CardUnavailable()
        } else {
            val state = savedCardState(context)
            val runtime = remember(context["cardId"], model) { SimpleCardRuntime(model, actions, state, saveState) }
            LaunchedEffect(runtime, state) { runtime.adopt(state) }
            SimpleCardContent(runtime, CardGate.from(context))
        }
    }

/**
 * Runs a renderer's [map] outside composition rules: a mapper that throws or returns null gives
 * null (logged), so the renderer shows [CardUnavailable] instead of crashing the chat.
 */
internal fun <T : Any> mapElementOrNull(element: Map<String, Any?>, map: () -> T?): T? =
    runCatching(map).getOrNull().also {
        if (it == null) Log.warning(LOG_TAG, SELF_TAG, "Unmappable ${element["type"]} element ${element["id"]}; showing fallback")
    }

/** Shown in place of a card whose element couldn't be mapped. */
@Composable
fun CardUnavailable() {
    Text("This content isn't available right now.")
}

private const val LOG_TAG = "ConciergeTestApp"
private const val SELF_TAG = "SimpleCardRenderer"
