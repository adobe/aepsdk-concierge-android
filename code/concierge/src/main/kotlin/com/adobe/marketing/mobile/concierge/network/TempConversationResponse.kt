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
 * Response content containing the actual message and metadata
 */
internal data class ConversationResponse(
    val message: String,
    val multimodalElements: List<MultimodalElement> = emptyList(),
    val promptSuggestions: List<String> = emptyList()
)

/**
 * Multimodal elements for rich content (future use)
 */
internal data class MultimodalElement(
    val type: String? = null,
    val content: Map<String, Any> = emptyMap()
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
    val multimodalElements: List<MultimodalElement> = emptyList()
)
