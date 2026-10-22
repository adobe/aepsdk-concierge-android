/*
 * Copyright 2025 Adobe. All rights reserved.
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

import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log
import java.net.URLDecoder
import java.net.URLEncoder

// TODO: Temporary data classes for parsing conversation temporary API responses. Need to be
//  replaced with actual API response structures once available

/**
 * Root response structure for conversation API responses
 */
internal data class ConversationApiResponse(
    val handle: List<Handle> = emptyList()
)

/**
 * Handle object containing different types of payloads
 */
internal data class Handle(
    val type: String,
    val payload: List<Map<String, Any>> = emptyList()
)

/**
 * Conversation-specific payload structure
 */
internal data class ConversationPayload(
    val conversationId: String? = null,
    val interactionId: String? = null,
    val request: ConversationRequest? = null,
    val response: ConversationResponse? = null,
    val state: String? = null
)

/**
 * Request information (typically empty in responses)
 */
internal data class ConversationRequest(
    val message: String? = null
)

/**
 * Represents a backend hint about a link in the message, used to attach an inline icon.
 */
internal data class LinkHint(
    val kind: String,
    val href: String
)

/**
 * Response content containing the actual message and metadata
 */
internal data class ConversationResponse(
    val message: String,
    val multimodalElements: List<MultimodalElement> = emptyList(),
    val promptSuggestions: List<String> = emptyList(),
    val ctaButton: CtaButton? = null,
    val feedback: ConversationFeedbackInfo? = null,
    val linkHints: List<LinkHint> = emptyList()
)

/**
 * Feedback metadata returned with an agent response.
 * @param eligible Whether this message is eligible for end-user feedback. Defaults to `false` when absent.
 */
internal data class ConversationFeedbackInfo(
    val eligible: Boolean = false
)

/**
 * Represents a CTA button that appears as a standalone action below a bot message.
 *
 * @param label The button text (`entity_info.primary.text`).
 * @param url The destination url (`entity_info.primary.url`).
 * @param summary An optional conversation summary (`entity_info.primary.summary`), appended to [url]
 * as a `summary` query parameter when the button is clicked; never displayed.
 */
internal data class CtaButton(
    val label: String,
    val url: String,
    val summary: String? = null
) {
    /**
     * The URL to open on click: for an http(s) [url], [summary] appended as a percent-encoded
     * `summary` query parameter before any fragment; otherwise [url] unchanged. If appending the
     * summary would make the URL longer than [MAX_CLICK_URL_LENGTH], or [url] already has a `summary`
     * query parameter (matched case-insensitively after percent-decoding), [url] is returned unchanged.
     */
    val clickUrl: String
        get() {
            val isHttp = url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)
            if (summary.isNullOrBlank() || !isHttp) return url
            val fragmentStart = url.indexOf('#').takeIf { it >= 0 } ?: url.length
            val base = url.substring(0, fragmentStart)
            val query = base.substringAfter('?', "")
            if (query.split('&').any { decodeOrNull(it.substringBefore('=')).equals(SUMMARY_PARAM, ignoreCase = true) }) {
                Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "CTA summary not added: url already has a summary parameter.")
                return url
            }
            val separator = when {
                '?' !in base -> "?"
                base.endsWith('?') || base.endsWith('&') -> ""
                else -> "&"
            }
            val encodedSummary = URLEncoder.encode(summary, "UTF-8").replace("+", "%20")
            val withSummary = "$base$separator$SUMMARY_PARAM=$encodedSummary${url.substring(fragmentStart)}"
            if (withSummary.length > MAX_CLICK_URL_LENGTH) {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "CTA summary not added: url would be ${withSummary.length} characters (max $MAX_CLICK_URL_LENGTH)."
                )
                return url
            }
            return withSummary
        }

    companion object {
        private const val TAG = "CtaButton"
        private const val SUMMARY_PARAM = "summary"

        /** Decodes a percent-encoded query parameter name, or null if it is malformed. */
        private fun decodeOrNull(name: String): String? =
            try {
                URLDecoder.decode(name, "UTF-8")
            } catch (e: IllegalArgumentException) {
                null
            }

        /** Longest URL the summary may produce, to stay within common server and CDN limits. */
        internal const val MAX_CLICK_URL_LENGTH = 2048
    }
}

/**
 * Represents an ordered element from a multimodal API response.
 * Preserves the original array position so CTAs and cards can be interleaved correctly.
 */
internal sealed class ParsedMultimodalItem {
    data class Card(val element: MultimodalElement) : ParsedMultimodalItem()
    data class Cta(val button: CtaButton) : ParsedMultimodalItem()
}

/**
 * Multimodal elements for rich content
 */
internal data class MultimodalElement(
    val id: String,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailUrl: String? = null,
    val thumbnailWidth: Int? = null,
    val thumbnailHeight: Int? = null,
    val alttext: String? = null,
    val title: String? = null,
    val caption: String? = null,
    val transcript: String? = null,
    val content: Map<String, Any> = emptyMap()
)

/**
 * Represents a citation source for a chat message.
 */
internal data class Citation(
    val title: String,
    val url: String,
    val citationNumber: Int? = null,
    val startIndex: Int? = null,
    val endIndex: Int? = null
)

/**
 * Represents the state of a conversation interaction
 */
internal enum class ConversationState {
    IN_PROGRESS,
    COMPLETED,
    ERROR,
    UNKNOWN;

    companion object {
        fun fromString(state: String?): ConversationState {
            return when (state?.lowercase()) {
                "in-progress" -> IN_PROGRESS
                "completed" -> COMPLETED
                "error" -> ERROR
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Parsed conversation message containing extracted content and metadata
 */
internal data class ParsedConversationMessage(
    val messageContent: String,
    val state: ConversationState,
    val conversationId: String? = null,
    val interactionId: String? = null,
    val promptSuggestions: List<String> = emptyList(),
    val multimodalElements: List<MultimodalElement> = emptyList(),
    val orderedElements: List<ParsedMultimodalItem> = emptyList(),
    val sources: List<Citation> = emptyList(),
    val feedbackEligible: Boolean = false,
    val linkHints: List<LinkHint> = emptyList()
)
