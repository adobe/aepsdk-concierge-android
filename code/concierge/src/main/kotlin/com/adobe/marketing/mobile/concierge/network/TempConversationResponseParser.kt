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
import org.json.JSONException
import org.json.JSONObject

/**
 * TEMPORARY!! Parser for conversation API responses that extracts message content from JSON data events.
 * This parser is designed to handle the specific JSON structure returned by the conversation API.
 * TODO: Replace to use DataReader.opt* methods available in the platform SDK.
 */
internal object TempConversationResponseParser {
    private const val TAG = "ConversationResponseParser"
    private const val CONVERSATION_TYPE = "brand-concierge:conversation"

    // JSON field names
    private const val FIELD_HANDLE = "handle"
    private const val FIELD_TYPE = "type"
    private const val FIELD_PAYLOAD = "payload"
    private const val FIELD_CONVERSATION_ID = "conversationId"
    private const val FIELD_INTERACTION_ID = "interactionId"
    private const val FIELD_RESPONSE = "response"
    private const val FIELD_MESSAGE = "message"
    private const val FIELD_STATE = "state"
    private const val FIELD_PROMPT_SUGGESTIONS = "promptSuggestions"
    private const val FIELD_MULTIMODAL_ELEMENTS = "multimodalElements"


    /**
     * Parses a JSON string from an SSE data event and extracts conversation messages.
     *
     * @param jsonData The raw JSON string from the SSE event
     * @return List of parsed conversation messages, empty if parsing fails or no conversation data found
     */
    fun parseConversationData(jsonData: String): List<ParsedConversationMessage> {
        if (jsonData.isBlank()) {
            return emptyList()
        }

        return try {
            val jsonObject = JSONObject(jsonData)
            extractConversationMessages(jsonObject)
        } catch (e: JSONException) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Failed to parse JSON: ${e.message}"
            )
            emptyList()
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Unexpected error parsing conversation data: ${e.message}"
            )
            emptyList()
        }
    }

    /**
     * Extracts conversation messages from the parsed JSON object.
     */
    private fun extractConversationMessages(jsonObject: JSONObject): List<ParsedConversationMessage> {
        val messages = mutableListOf<ParsedConversationMessage>()

        val handleArray = jsonObject.optJSONArray(FIELD_HANDLE) ?: return emptyList()

        for (i in 0 until handleArray.length()) {
            val handle = handleArray.optJSONObject(i) ?: continue

            // Only process conversation-type handles
            if (handle.optString(FIELD_TYPE) == CONVERSATION_TYPE) {
                val payloadArray = handle.optJSONArray(FIELD_PAYLOAD) ?: continue

                for (j in 0 until payloadArray.length()) {
                    val payload = payloadArray.optJSONObject(j) ?: continue

                    val parsedMessage = extractMessageFromPayload(payload)
                    if (parsedMessage != null) {
                        messages.add(parsedMessage)
                    }
                }
            }
        }

        return messages
    }

    /**
     * Extracts a conversation message from a single payload object.
     */
    private fun extractMessageFromPayload(payload: JSONObject): ParsedConversationMessage? {
        val response = payload.optJSONObject(FIELD_RESPONSE) ?: return null

        val message = response.optString(FIELD_MESSAGE, "")
        if (message.isEmpty()) {
            return null
        }

        val state = ConversationState.fromString(payload.optString(FIELD_STATE))
        val conversationId = payload.optString(FIELD_CONVERSATION_ID).takeIf { it.isNotEmpty() }
        val interactionId = payload.optString(FIELD_INTERACTION_ID).takeIf { it.isNotEmpty() }

        // Extract prompt suggestions
        val promptSuggestions = mutableListOf<String>()
        val suggestionsArray = response.optJSONArray(FIELD_PROMPT_SUGGESTIONS)
        if (suggestionsArray != null) {
            for (k in 0 until suggestionsArray.length()) {
                val suggestion = suggestionsArray.optString(k)
                if (suggestion.isNotEmpty()) {
                    promptSuggestions.add(suggestion)
                }
            }
        }

        // Extract multimodal elements (basic extraction for now)
        val multimodalElements = mutableListOf<MultimodalElement>()
        val elementsArray = response.optJSONArray(FIELD_MULTIMODAL_ELEMENTS)
        if (elementsArray != null) {
            for (k in 0 until elementsArray.length()) {
                val element = elementsArray.optJSONObject(k)
                if (element != null) {
                    val elementType = element.optString("type")
                    val contentMap = mutableMapOf<String, Any>()
                    // Basic content extraction - can be enhanced later
                    element.keys().forEach { key ->
                        if (key != "type") {
                            contentMap[key] = element.opt(key) ?: ""
                        }
                    }
                    multimodalElements.add(MultimodalElement(elementType, contentMap))
                }
            }
        }

        return ParsedConversationMessage(
            messageContent = message,
            state = state,
            conversationId = conversationId,
            interactionId = interactionId,
            promptSuggestions = promptSuggestions,
            multimodalElements = multimodalElements
        )
    }
}
