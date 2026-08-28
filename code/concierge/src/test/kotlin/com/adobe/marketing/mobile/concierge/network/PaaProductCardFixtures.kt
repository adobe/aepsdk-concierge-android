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

package com.adobe.marketing.mobile.concierge.network

/**
 * Mock conversation streams for a product-card response whose agent text is split into two parts.
 *
 * These reproduce the Edge SSE envelope the SDK consumes, not the A2A `parts` array the product
 * advisor agent produces — text parts are flattened into `response.message` before they reach the
 * SDK, so A2A JSON is not something the SDK can be driven with.
 *
 * Both streams follow the event sequence observed live: the agent text arrives complete on the
 * first event and is re-sent identically, elements arrive only on the final `completed` event.
 *
 * [interleavedStream] encodes the **proposed** contract, where positioned `text` elements carry
 * the ordering. No deployment sends this yet. [flattenedStream] is what the service sends today,
 * kept alongside so tests can pin both behaviors.
 */
internal object PaaProductCardFixtures {

    private const val USD = "$"

    /** Leading agent text: the product rundown that belongs above the cards. */
    const val INTRO_TEXT: String =
        "Here are some running shoes in size 9:\n\n" +
            "1. Aero Runner 5\n" +
            "   - Max-cushion daily trainer with a full-length foam midsole\n\n" +
            "2. Trail Glide 2\n" +
            "   - Breathable mesh upper and a foam midsole for everyday miles\n\n" +
            "3. Street Runner 8\n" +
            "   - Plush, smooth ride geared toward easy and long runs\n\n" +
            "4. Cloud Stride 12\n" +
            "   - Versatile road runner with an engineered mesh upper"

    /** Trailing agent text: the follow-up question that belongs below the cards. */
    const val FOLLOW_UP_TEXT: String =
        "Do you want a standard (medium) width, or do you need wide/extra wide?"

    val SUGGESTIONS: List<String> = listOf("Medium (standard)", "Wide", "Extra Wide")

    val CARD_IDS: List<String> = listOf(
        "AERO-RUNNER-5",
        "TRAIL-GLIDE-2",
        "STREET-RUNNER-8",
        "CLOUD-STRIDE-12"
    )

    /**
     * The whole response as one string, the way the service flattens multiple text parts today.
     */
    val FLATTENED_MESSAGE: String = "$INTRO_TEXT\n\n$FOLLOW_UP_TEXT"

    /**
     * Today's behavior: one text blob, then the cards. The follow-up question is stranded inside
     * the blob above the carousel, which is the defect this fixture pins.
     */
    fun flattenedStream(): String = flattenedPayloads().joinToString("") { sseEvent(it) }

    /** The same stream already run through the parser, for tests that start above the network. */
    fun flattenedParsedMessages(): List<ParsedConversationMessage> =
        flattenedPayloads().flatMap { ConversationResponseParser.parseConversationData(it) }

    private fun flattenedPayloads(): List<String> = listOf(
        payload("in-progress", FLATTENED_MESSAGE, elements = "", suggestions = emptyList()),
        payload("in-progress", FLATTENED_MESSAGE, elements = "", suggestions = emptyList()),
        payload("completed", FLATTENED_MESSAGE, elements = cardElements(), suggestions = SUGGESTIONS)
    )

    /**
     * The proposed contract: the same response with the agent text positioned as `text` elements
     * around the cards. `response.message` still carries the flattened copy for older clients.
     */
    fun interleavedStream(): String = interleavedPayloads().joinToString("") { sseEvent(it) }

    /** The same stream already run through the parser, for tests that start above the network. */
    fun interleavedParsedMessages(): List<ParsedConversationMessage> =
        interleavedPayloads().flatMap { ConversationResponseParser.parseConversationData(it) }

    private fun interleavedPayloads(): List<String> = listOf(
        payload("in-progress", FLATTENED_MESSAGE, elements = "", suggestions = emptyList()),
        payload("in-progress", FLATTENED_MESSAGE, elements = "", suggestions = emptyList()),
        payload(
            state = "completed",
            message = FLATTENED_MESSAGE,
            elements = listOf(
                textElement(INTRO_TEXT),
                cardElements(),
                textElement(FOLLOW_UP_TEXT)
            ).joinToString(","),
            suggestions = SUGGESTIONS
        )
    )

    private fun textElement(text: String): String =
        """{"type":"text","text":"${jsonEscape(text)}"}"""

    private fun cardElements(): String = listOf(
        card(CARD_IDS[0], "Aero Runner 5", "${USD}109.99-${USD}189.99", "New Color"),
        card(CARD_IDS[1], "Trail Glide 2", "${USD}53.97-${USD}79.99", "Extended Sizes"),
        card(CARD_IDS[2], "Street Runner 8", "${USD}89.07-${USD}164.99", "New Color"),
        card(CARD_IDS[3], "Cloud Stride 12", "${USD}97.48-${USD}144.99", "Extended Sizes")
    ).joinToString(",")

    private fun card(id: String, name: String, price: String, badge: String): String {
        val url = "https://example.com/p/${id.lowercase()}?concierge=true"
        val image = "https://images.example.com/is/image/$id?wid=190&hei=190"
        return """
            {
              "id": "$id",
              "width": 222,
              "height": 222,
              "thumbnail_width": 190,
              "thumbnail_height": 190,
              "entity_info": {
                "backgroundColor": "#FFFFFF",
                "description": null,
                "learningResource": null,
                "logo": null,
                "productDescription": null,
                "productName": "${jsonEscape(name)}",
                "productImageURL": "$image",
                "productPageURL": "$url",
                "productPrice": "$price",
                "productWasPrice": null,
                "productBadge": "$badge",
                "primary": { "text": "View Details", "url": "$url" }
              }
            }
        """.trimIndent()
    }

    private fun payload(
        state: String,
        message: String,
        elements: String,
        suggestions: List<String>
    ): String {
        val suggestionJson = suggestions.joinToString(",") { "\"${jsonEscape(it)}\"" }
        return """
            {
              "handle": [
                {
                  "type": "brand-concierge:conversation",
                  "payload": [
                    {
                      "conversationId": "conv-mock-1",
                      "interactionId": "interaction-mock-1",
                      "request": { "message": "running shoes size 9" },
                      "response": {
                        "message": "${jsonEscape(message)}",
                        "multimodalElements": { "elements": [$elements] },
                        "promptSuggestions": [$suggestionJson],
                        "sources": [],
                        "widgets": []
                      },
                      "state": "$state"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    private fun sseEvent(json: String): String = buildString {
        append("event: brand-concierge:conversation\n")
        append(json.lines().joinToString("\n") { "data: $it" })
        append("\n\n")
    }

    private fun jsonEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
}
