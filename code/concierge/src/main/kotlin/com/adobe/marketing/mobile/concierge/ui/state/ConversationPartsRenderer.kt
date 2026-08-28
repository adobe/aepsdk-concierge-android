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

package com.adobe.marketing.mobile.concierge.ui.state

import com.adobe.marketing.mobile.concierge.network.ConversationPart
import com.adobe.marketing.mobile.concierge.network.MultimodalElement

/**
 * Turns the ordered parts of an agent response into the message contents that render them.
 *
 * Kept separate from the chat view model so the mapping can be exercised on its own: given a
 * parsed response, this is the whole of what the user will see and in what order.
 */
internal object ConversationPartsRenderer {

    /**
     * Collapses parts into renderable message contents, merging each run of adjacent cards into a
     * single carousel. Runs separated by text or a CTA stay separate, which is what allows a
     * response to place text between two groups of cards.
     *
     * Prompt suggestions are not returned here: they are not a message of their own, they attach
     * to the last message of the turn so they always render last.
     */
    fun buildRenderables(parts: List<ConversationPart>): List<MessageContent> {
        val renderables = mutableListOf<MessageContent>()
        val pendingCards = mutableListOf<MultimodalElement>()

        fun flushCards() {
            if (pendingCards.isNotEmpty()) {
                renderables.add(MessageContent.Mixed(text = "", multimodalElements = pendingCards.toList()))
                pendingCards.clear()
            }
        }

        parts.forEach { part ->
            when (part) {
                is ConversationPart.Card -> pendingCards.add(part.element)
                is ConversationPart.Text -> {
                    flushCards()
                    renderables.add(MessageContent.Text(part.text))
                }
                is ConversationPart.Cta -> {
                    flushCards()
                    renderables.add(MessageContent.CtaButton(part.button))
                }
                is ConversationPart.Suggestions -> Unit
            }
        }
        flushCards()
        return renderables
    }

    /**
     * The prompt suggestions a response carries, or an empty list when it has none.
     */
    fun suggestionsOf(parts: List<ConversationPart>): List<String> =
        parts.filterIsInstance<ConversationPart.Suggestions>().lastOrNull()?.prompts ?: emptyList()
}
