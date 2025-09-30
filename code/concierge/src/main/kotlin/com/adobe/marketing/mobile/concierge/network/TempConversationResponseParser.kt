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
    private const val FIELD_ID = "id"
    private const val FIELD_TYPE = "type"
    private const val FIELD_PAYLOAD = "payload"
    private const val FIELD_CONVERSATION_ID = "conversationId"
    private const val FIELD_INTERACTION_ID = "interactionId"
    private const val FIELD_RESPONSE = "response"
    private const val FIELD_MESSAGE = "message"
    private const val FIELD_STATE = "state"
    private const val FIELD_PROMPT_SUGGESTIONS = "promptSuggestions"
    private const val FIELD_MULTIMODAL_ELEMENTS = "multimodalElements"
    private const val FIELD_SOURCES = "sources"
    private const val FIELD_START_INDEX = "start_index"
    private const val FIELD_END_INDEX = "end_index"
    private const val FIELD_CITATION_NUMBER = "citation_number"
    private const val FIELD_ELEMENTS = "elements"
    private const val FIELD_WIDTH = "width"
    private const val FIELD_HEIGHT = "height"
    private const val FIELD_THUMBNAIL_WIDTH = "thumbnail_width"
    private const val FIELD_THUMBNAIL_HEIGHT = "thumbnail_height"

    // product card field names
    private const val FIELD_PRODUCT_NAME = "productName"
    private const val FIELD_PRODUCT_DESCRIPTION = "productDescription"
    private const val FIELD_DESCRIPTION = "description"
    private const val FIELD_PRODUCT_PAGE_URL = "productPageURL"
    private const val FIELD_PRODUCT_IMAGE_URL = "productImageURL"
    private const val FIELD_BACKGROUND_COLOR = "backgroundColor"
    private const val FIELD_LEARNING_RESOURCE = "learningResource"
    private const val FIELD_LOGO = "logo"
    private const val FIELD_PRIMARY = "primary"
    private const val FIELD_SECONDARY = "secondary"
    private const val FIELD_BUTTON_TEXT = "text"
    private const val FIELD_BUTTON_URL = "url"

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

        // Extract multimodal elements
        val multimodalElements = extractMultimodalElements(response)

        // Extract sources
        val sources = extractSources(response)

        return ParsedConversationMessage(
            messageContent = message,
            state = state,
            conversationId = conversationId,
            interactionId = interactionId,
            promptSuggestions = promptSuggestions,
            multimodalElements = multimodalElements,
            sources = sources
        )
    }

    /**
     * Extracts multimodal elements from the response object
     */
    private fun extractMultimodalElements(response: JSONObject): List<MultimodalElement> {
        val multimodalObj = response.optJSONObject(FIELD_MULTIMODAL_ELEMENTS)
        if (multimodalObj == null) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No multimodalElements found in response."
            )
            return emptyList()
        }

        val elementsArray = multimodalObj.optJSONArray(FIELD_ELEMENTS)
        if (elementsArray == null) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No elements array found in multimodalElements."
            )
            return emptyList()
        }

        val elements = mutableListOf<MultimodalElement>()
        for (i in 0 until elementsArray.length()) {
            val elementObj = elementsArray.optJSONObject(i) ?: continue
            val element = parseMultimodalElement(elementObj)
            if (element != null) {
                elements.add(element)
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Parsed multimodal element ${i + 1}: id=${element.id}, title=${element.title ?: "N/A"}."
                )
            } else {
                Log.warning(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Failed to parse multimodal element ${i + 1} from JSON."
                )
            }
        }

        if (elements.isNotEmpty()) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Successfully parsed ${elements.size} multimodal elements from response."
            )
        } else {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No valid multimodal elements found in elements array."
            )
        }

        return elements
    }

    /**
     * Parses a single multimodal element from a [JSONObject].
     * Returns null if required fields are missing or parsing fails.
     */
    private fun parseMultimodalElement(elementObj: JSONObject): MultimodalElement? {
        val id = elementObj.optString(FIELD_ID)
        if (id.isEmpty()) return null

        val content = mutableMapOf<String, Any>()

        // get image fields from the base json object element
        val width = elementObj.optInt(FIELD_WIDTH, -1).takeIf { it > 0 }
        val height = elementObj.optInt(FIELD_HEIGHT, -1).takeIf { it > 0 }
        val thumbnailWidth = elementObj.optInt(FIELD_THUMBNAIL_WIDTH, -1).takeIf { it > 0 }
        val thumbnailHeight = elementObj.optInt(FIELD_THUMBNAIL_HEIGHT, -1).takeIf { it > 0 }

        // Extract product information
        val productName = elementObj.optString(FIELD_PRODUCT_NAME).takeIf { it.isNotEmpty() }
        val productDescription =
            elementObj.optString(FIELD_PRODUCT_DESCRIPTION).takeIf { it.isNotEmpty() }
        val description = elementObj.optString(FIELD_DESCRIPTION).takeIf { it.isNotEmpty() }
        val productPageUrl =
            elementObj.optString(FIELD_PRODUCT_PAGE_URL).takeIf { it.isNotEmpty() }
        val productImageUrl =
            elementObj.optString(FIELD_PRODUCT_IMAGE_URL).takeIf { it.isNotEmpty() }
        val backgroundColor =
            elementObj.optString(FIELD_BACKGROUND_COLOR).takeIf { it.isNotEmpty() }
        val learningResource =
            elementObj.optString(FIELD_LEARNING_RESOURCE).takeIf { it.isNotEmpty() }
        val logo = elementObj.optString(FIELD_LOGO).takeIf { it.isNotEmpty() }

        // Extract primary and secondary action buttons
        val primaryAction = elementObj.optJSONObject(FIELD_PRIMARY)
        val secondaryAction = elementObj.optJSONObject(FIELD_SECONDARY)

        // Add element fields to content
        productName?.let { content["productName"] = it }
        productDescription?.let { content["productDescription"] = it }
        description?.let { content["description"] = it }
        productPageUrl?.let { content["productPageURL"] = it }
        productImageUrl?.let { content["productImageURL"] = it }
        backgroundColor?.let { content["backgroundColor"] = it }
        learningResource?.let { content["learningResource"] = it }
        logo?.let { content["logo"] = it }

        // Add action buttons
        primaryAction?.let { action ->
            val primaryText = action.optString(FIELD_BUTTON_TEXT).takeIf { it.isNotEmpty() }
            val primaryUrl = action.optString(FIELD_BUTTON_URL).takeIf { it.isNotEmpty() }
            primaryText?.let { content["primaryText"] = it }
            primaryUrl?.let { content["primaryUrl"] = it }
        }

        secondaryAction?.let { action ->
            val secondaryText = action.optString(FIELD_BUTTON_TEXT).takeIf { it.isNotEmpty() }
            val secondaryUrl = action.optString(FIELD_BUTTON_URL).takeIf { it.isNotEmpty() }
            secondaryText?.let { content["secondaryText"] = it }
            secondaryUrl?.let { content["secondaryUrl"] = it }
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Building multimodal element - productName: $productName, productImageURL: $productImageUrl, primaryText: ${content["primaryText"]}"
        )

        return MultimodalElement(
            id = id,
            url = productImageUrl,
            width = width,
            height = height,
            thumbnailUrl = productImageUrl,
            thumbnailWidth = thumbnailWidth,
            thumbnailHeight = thumbnailHeight,
            alttext = productName,
            title = productName,
            caption = description,
            transcript = productDescription,
            content = content
        )
    }

    /**
     * Extracts sources from the response object
     */
    private fun extractSources(response: JSONObject): List<Citation> {
        val sourcesArray = response.optJSONArray(FIELD_SOURCES)
        if (sourcesArray == null) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "No sources found in response."
            )
            return emptyList()
        }

        val sources = mutableListOf<Citation>()
        for (i in 0 until sourcesArray.length()) {
            val sourceObj = sourcesArray.optJSONObject(i) ?: continue
            val source = parseSource(sourceObj)
            if (source != null) {
                sources.add(source)
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Parsed source ${i + 1}: citationNumber=${source.citationNumber}, title=${source.title}."
                )
            }
        }

        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Parsed ${sources.size} sources from response."
        )

        return sources
    }

    /**
     * Parses a single source from a [JSONObject] and creates a [Citation].
     * Returns the created citation or null if parsing fails.
     */
    private fun parseSource(sourceObj: JSONObject): Citation? {
        val title = sourceObj.optString("title")
        if (title.isEmpty()) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Source missing required title field."
            )
            return null
        }

        val url = sourceObj.optString("url").takeIf { it.isNotEmpty() }
        val citationNumber = sourceObj.optInt(FIELD_CITATION_NUMBER, -1).takeIf { it > 0 }
        val startIndex = sourceObj.optInt(FIELD_START_INDEX, -1).takeIf { it >= 0 }
        val endIndex = sourceObj.optInt(FIELD_END_INDEX, -1).takeIf { it >= 0 }

        return Citation(
            title = title,
            url = url,
            citationNumber = citationNumber,
            startIndex = startIndex,
            endIndex = endIndex
        )
    }
}
