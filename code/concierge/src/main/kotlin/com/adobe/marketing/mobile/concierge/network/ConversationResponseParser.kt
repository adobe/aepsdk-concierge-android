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
import com.adobe.marketing.mobile.util.DataReader
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONException
import org.json.JSONObject

/**
 * Parser for conversation API responses that extracts message content from JSON data events.
 * This parser is designed to handle the specific JSON structure returned by the conversation API.
 */
internal object ConversationResponseParser {
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
    private const val FIELD_DETAILS = "details"
    private const val FIELD_PRIMARY = "primary"
    private const val FIELD_SECONDARY = "secondary"
    private const val FIELD_BUTTON_TEXT = "text"
    private const val FIELD_BUTTON_URL = "url"
    private const val FIELD_ENTITY_INFO = "entity_info"
    private const val FIELD_PRODUCT_PRICE = "productPrice"
    private const val FIELD_PRODUCT_WAS_PRICE = "productWasPrice"
    private const val FIELD_PRODUCT_BADGE = "productBadge"
    private const val ELEMENT_TYPE_CTA_BUTTON = "ctaButton"

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
            val conversationMap = JSONUtils.toMap(jsonObject) ?: return emptyList()
            extractConversationMessages(conversationMap)
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
     * Extracts conversation messages from the root map (from [JSONUtils.toMap]).
     */
    private fun extractConversationMessages(conversationMap: Map<String, Any?>): List<ParsedConversationMessage> {
        val messages = mutableListOf<ParsedConversationMessage>()
        val handleList = DataReader.optTypedListOfMap(Any::class.java, conversationMap, FIELD_HANDLE, null) ?: return emptyList()

        for (handle in handleList) {
            if (DataReader.optString(handle, FIELD_TYPE, null) != CONVERSATION_TYPE) continue

            val payloadList = DataReader.optTypedListOfMap(Any::class.java, handle, FIELD_PAYLOAD, null) ?: continue

            for (payload in payloadList) {
                val parsedMessage = extractMessageFromPayload(payload)
                if (parsedMessage != null) {
                    messages.add(parsedMessage)
                }
            }
        }

        return messages
    }

    /**
     * Extracts a conversation message from a single payload map.
     * Returns null if response is missing or message is blank (except when state is completed).
     */
    private fun extractMessageFromPayload(payload: Map<String, Any?>): ParsedConversationMessage? {
        val response = DataReader.optTypedMap(Any::class.java, payload, FIELD_RESPONSE, null) ?: return null

        val state = ConversationState.fromString(DataReader.optString(payload, FIELD_STATE, ""))
        val message = DataReader.optString(response, FIELD_MESSAGE, "")
        if (message.isEmpty() && state != ConversationState.COMPLETED) {
            return null
        }
        val conversationId = DataReader.optString(payload, FIELD_CONVERSATION_ID, null)?.takeIf { it.isNotEmpty() }
        val interactionId = DataReader.optString(payload, FIELD_INTERACTION_ID, null)?.takeIf { it.isNotEmpty() }

        val promptSuggestions = (DataReader.optTypedList(String::class.java, response, FIELD_PROMPT_SUGGESTIONS, null)
            ?: emptyList()).filter { it.isNotEmpty() }

        val orderedElements = extractOrderedElements(response)
        val multimodalElements = orderedElements.filterIsInstance<ParsedMultimodalItem.Card>().map { it.element }
        val sources = extractSources(response)

        return ParsedConversationMessage(
            messageContent = message,
            state = state,
            conversationId = conversationId,
            interactionId = interactionId,
            promptSuggestions = promptSuggestions,
            multimodalElements = multimodalElements,
            orderedElements = orderedElements,
            sources = sources
        )
    }

    /**
     * Extracts ordered elements from the response, preserving original array position.
     * Each element is parsed as either a [ParsedMultimodalItem.Cta] (for ctaButton elements)
     * or a [ParsedMultimodalItem.Card] (for all other element types).
     */
    private fun extractOrderedElements(response: Map<String, Any?>): List<ParsedMultimodalItem> {
        val multimodalRaw = response[FIELD_MULTIMODAL_ELEMENTS] ?: return emptyList()
        if (multimodalRaw is List<*>) return emptyList()
        val multimodalMap = multimodalRaw as? Map<*, *> ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val elementsList = DataReader.optTypedListOfMap(
            Any::class.java,
            multimodalMap as Map<String, Any?>,
            FIELD_ELEMENTS,
            null
        ) ?: return emptyList()

        val items = mutableListOf<ParsedMultimodalItem>()
        elementsList.forEachIndexed { i, elementMap ->
            val elementType = DataReader.optString(elementMap, FIELD_TYPE, "")
            if (elementType == ELEMENT_TYPE_CTA_BUTTON) {
                val ctaButton = parseCtaButton(elementMap)
                if (ctaButton != null) {
                    items.add(ParsedMultimodalItem.Cta(ctaButton))
                    Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Parsed CTA button ${i + 1}: label=${ctaButton.label}")
                } else {
                    Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Failed to parse CTA button element ${i + 1}.")
                }
            } else {
                val element = parseMultimodalElement(elementMap)
                if (element != null) {
                    items.add(ParsedMultimodalItem.Card(element))
                    Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Parsed card element ${i + 1}: id=${element.id}")
                } else {
                    Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Failed to parse card element ${i + 1}.")
                }
            }
        }
        return items
    }

    /**
     * Parses a CTA button from an element map.
     * Reads label and url from entity_info.primary.text and entity_info.primary.url.
     * Returns null if label or url is missing.
     */
    private fun parseCtaButton(elementMap: Map<String, Any?>): CtaButton? {
        val entityInfo = DataReader.optTypedMap(Any::class.java, elementMap, FIELD_ENTITY_INFO, null)
        val primaryAction = entityInfo?.let { DataReader.optTypedMap(Any::class.java, it, FIELD_PRIMARY, null) }
        val label = DataReader.optString(primaryAction, FIELD_BUTTON_TEXT, null)?.takeIf { it.isNotEmpty() }
        val url = DataReader.optString(primaryAction, FIELD_BUTTON_URL, null)?.takeIf { it.isNotEmpty() }
        if (label.isNullOrEmpty() || url.isNullOrEmpty()) return null
        return CtaButton(label = label, url = url)
    }

    /**
     * Parses a single multimodal element from an element map.
     * Prefers entity_info when present; falls back to root element for missing fields.
     * Returns null if id is empty.
     */
    private fun parseMultimodalElement(elementMap: Map<String, Any?>): MultimodalElement? {
        val id = DataReader.optString(elementMap, FIELD_ID, "")
        if (id.isEmpty()) return null

        val entityInfo = DataReader.optTypedMap(Any::class.java, elementMap, FIELD_ENTITY_INFO, null)

        val content = mutableMapOf<String, Any>()

        val width = DataReader.optInt(elementMap, FIELD_WIDTH, -1).takeIf { it > 0 }
        val height = DataReader.optInt(elementMap, FIELD_HEIGHT, -1).takeIf { it > 0 }
        val thumbnailWidth = DataReader.optInt(elementMap, FIELD_THUMBNAIL_WIDTH, -1).takeIf { it > 0 }
        val thumbnailHeight = DataReader.optInt(elementMap, FIELD_THUMBNAIL_HEIGHT, -1).takeIf { it > 0 }

        val productName = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_NAME)
        val productDescription = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_DESCRIPTION)
        val description = optStringFallback(entityInfo, elementMap, FIELD_DESCRIPTION)
        val productPageUrl = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_PAGE_URL)
        val productImageUrl = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_IMAGE_URL)
        val backgroundColor = optStringFallback(entityInfo, elementMap, FIELD_BACKGROUND_COLOR)
        val learningResource = optStringFallback(entityInfo, elementMap, FIELD_LEARNING_RESOURCE)
        val logo = optStringFallback(entityInfo, elementMap, FIELD_LOGO)
        val details = optStringFallback(entityInfo, elementMap, FIELD_DETAILS)

        val primaryAction = entityInfo?.let { DataReader.optTypedMap(Any::class.java, it, FIELD_PRIMARY, null) }
            ?: DataReader.optTypedMap(Any::class.java, elementMap, FIELD_PRIMARY, null)
        val secondaryAction = entityInfo?.let { DataReader.optTypedMap(Any::class.java, it, FIELD_SECONDARY, null) }
            ?: DataReader.optTypedMap(Any::class.java, elementMap, FIELD_SECONDARY, null)

        val productPrice = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_PRICE)
        val productWasPrice = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_WAS_PRICE)
        val productBadge = optStringFallback(entityInfo, elementMap, FIELD_PRODUCT_BADGE)

        productName?.let { content["productName"] = it }
        productDescription?.let { content["productDescription"] = it }
        description?.let { content["description"] = it }
        productPageUrl?.let { content["productPageURL"] = it }
        productImageUrl?.let { content["productImageURL"] = it }
        backgroundColor?.let { content["backgroundColor"] = it }
        learningResource?.let { content["learningResource"] = it }
        logo?.let { content["logo"] = it }
        details?.let { content["details"] = it }
        productPrice?.let { content["productPrice"] = it }
        productWasPrice?.let { content["productWasPrice"] = it }
        productBadge?.let { content["productBadge"] = it }

        primaryAction?.let { action ->
            val primaryText = DataReader.optString(action, FIELD_BUTTON_TEXT, null)?.takeIf { it.isNotEmpty() }
            val primaryUrl = DataReader.optString(action, FIELD_BUTTON_URL, null)?.takeIf { it.isNotEmpty() }
            primaryText?.let { content["primaryText"] = it }
            primaryUrl?.let { content["primaryUrl"] = it }
        }

        secondaryAction?.let { action ->
            val secondaryText = DataReader.optString(action, FIELD_BUTTON_TEXT, null)?.takeIf { it.isNotEmpty() }
            val secondaryUrl = DataReader.optString(action, FIELD_BUTTON_URL, null)?.takeIf { it.isNotEmpty() }
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
     * Returns the string value from primary map if non-empty, otherwise from fallback map.
     */
    private fun optStringFallback(
        primary: Map<String, Any?>?,
        fallback: Map<String, Any?>,
        field: String
    ): String? {
        val primaryVal = DataReader.optString(primary, field, null)?.takeIf { it.isNotEmpty() }
        if (primaryVal != null) return primaryVal
        return DataReader.optString(fallback, field, null)?.takeIf { it.isNotEmpty() }
    }

    /**
     * Extracts citation sources from the response map.
     */
    private fun extractSources(response: Map<String, Any?>): List<Citation> {
        val sourcesList = DataReader.optTypedListOfMap(Any::class.java, response, FIELD_SOURCES, null)
        if (sourcesList == null) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "No sources found in response.")
            return emptyList()
        }

        val sources = sourcesList.mapIndexedNotNull { i, sourceMap ->
            parseSource(sourceMap)?.also { source ->
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Parsed source ${i + 1}: citationNumber=${source.citationNumber}, title=${source.title}."
                )
            }
        }

        Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Parsed ${sources.size} sources from response.")
        return sources
    }

    /**
     * Parses a single source from a map and creates a [Citation].
     * Returns null if title or url is missing.
     */
    private fun parseSource(sourceMap: Map<String, Any?>): Citation? {
        val title = DataReader.optString(sourceMap, "title", null)
        if (title.isNullOrEmpty()) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Source missing required title field.")
            return null
        }

        val url = DataReader.optString(sourceMap, "url", null)
        if (url.isNullOrEmpty()) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Source missing required url field.")
            return null
        }

        return Citation(
            title = title,
            url = url,
            citationNumber = DataReader.optInt(sourceMap, FIELD_CITATION_NUMBER, -1).takeIf { it > 0 },
            startIndex = DataReader.optInt(sourceMap, FIELD_START_INDEX, -1).takeIf { it >= 0 },
            endIndex = DataReader.optInt(sourceMap, FIELD_END_INDEX, -1).takeIf { it >= 0 }
        )
    }
}
