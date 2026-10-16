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

package com.adobe.marketing.mobile.concierge.ui.components.card

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.concierge.network.ConversationResponseParser
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only bridge so a testapp demo that is not a full `ConciergeChat` can still show the SDK's
 * out-of-the-box product cards for a raw `multimodalElements` payload. It runs the production
 * [ConversationResponseParser] and [RecommendationCards] (single card or carousel, honoring the
 * theme's `productCard.cardStyle`), so what the demo shows is what a live response renders.
 *
 * Card taps (`productPageURL`) and CTA taps (`primary.url`) are reported to [onLink], the same
 * URLs `ConciergeChat` would pass to its `handleLink` callback.
 */
@Composable
fun DemoMultimodalCards(multimodalElementsJson: String, onLink: (String) -> Unit, modifier: Modifier = Modifier) {
    val elements = remember(multimodalElementsJson) { parseDemoMultimodalCards(multimodalElementsJson) }
    RecommendationCards(
        elements = elements,
        modifier = modifier,
        onImageClick = { element -> (element.content["productPageURL"] as? String)?.takeIf { it.isNotBlank() }?.let(onLink) },
        onActionClick = { button -> button.url?.takeIf { it.isNotBlank() }?.let(onLink) }
    )
}

/**
 * Wraps `{"elements": [...]}` in a completed conversation SSE frame and parses it with the
 * production parser, returning the card elements it would render (CTA-button elements excluded).
 */
internal fun parseDemoMultimodalCards(multimodalElementsJson: String): List<MultimodalElement> {
    val multimodal = runCatching { JSONObject(multimodalElementsJson) }.getOrNull() ?: return emptyList()
    val frame = JSONObject()
        .put(
            "handle",
            JSONArray().put(
                JSONObject()
                    .put("type", "brand-concierge:conversation")
                    .put(
                        "payload",
                        JSONArray().put(
                            JSONObject()
                                .put("conversationId", "demo-conversation")
                                .put("state", "completed")
                                .put("response", JSONObject().put("message", "").put("multimodalElements", multimodal))
                        )
                    )
            )
        )
    return ConversationResponseParser.parseConversationData(frame.toString()).flatMap { it.multimodalElements }
}
