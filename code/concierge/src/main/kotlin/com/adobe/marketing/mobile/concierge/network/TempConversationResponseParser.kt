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
import com.adobe.marketing.mobile.concierge.ui.components.card.ActionButtonType
import com.adobe.marketing.mobile.concierge.ui.components.card.CarouselLayout
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductCardData
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductCarouselData
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
    private const val FIELD_ELEMENTS = "elements"
    private const val FIELD_URL = "url"
    private const val FIELD_WIDTH = "width"
    private const val FIELD_HEIGHT = "height"
    private const val FIELD_THUMBNAIL_URL = "thumbnailUrl"
    private const val FIELD_THUMBNAIL_WIDTH = "thumbnailWidth"
    private const val FIELD_THUMBNAIL_HEIGHT = "thumbnailHeight"
    private const val FIELD_ALT_TEXT = "alttext"
    private const val FIELD_CAPTION = "caption"
    private const val FIELD_TRANSCRIPT = "transcript"
    
    // Product card field names
    private const val FIELD_PRODUCT_CAROUSELS = "productCarousels"
    private const val FIELD_PRODUCTS = "products"
    private const val FIELD_ACTION_BUTTONS = "actionButtons"
    private const val FIELD_DESCRIPTION = "description"
    private const val FIELD_PRICE = "price"
    private const val FIELD_ORIGINAL_PRICE = "originalPrice"
    private const val FIELD_IMAGE_URL = "imageUrl"
    private const val FIELD_BRAND = "brand"
    private const val FIELD_RATING = "rating"
    private const val FIELD_REVIEW_COUNT = "reviewCount"
    private const val FIELD_AVAILABILITY = "availability"
    private const val FIELD_LAYOUT = "layout"
    private const val FIELD_METADATA = "metadata"
    
    // Additional field names for product cards (these were removed to avoid conflicts but are still needed)
    private const val FIELD_ID = "id"
    private const val FIELD_TITLE = "title"


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

        // Extract product carousels
        val productCarousels = extractProductCarousels(response)

        return ParsedConversationMessage(
            messageContent = message,
            state = state,
            conversationId = conversationId,
            interactionId = interactionId,
            promptSuggestions = promptSuggestions,
            multimodalElements = multimodalElements,
            productCarousels = productCarousels
        )
    }

    /**
     * Extracts multimodal elements from the response object
     */
    private fun extractMultimodalElements(response: JSONObject): MultimodalElements? {
        val multimodalObj = response.optJSONObject(FIELD_MULTIMODAL_ELEMENTS) ?: return null
        val elementsArray = multimodalObj.optJSONArray(FIELD_ELEMENTS) ?: return null
        
        val elements = mutableListOf<MultimodalElement>()
        for (i in 0 until elementsArray.length()) {
            val elementObj = elementsArray.optJSONObject(i) ?: continue
            val element = parseMultimodalElement(elementObj)
            if (element != null) {
                elements.add(element)
            }
        }
        
        return MultimodalElements(elements)
    }

    /**
     * Parses a single multimodal element from JSON
     */
    private fun parseMultimodalElement(elementObj: JSONObject): MultimodalElement? {
        val id = elementObj.optString(FIELD_ID)
        val type = elementObj.optString(FIELD_TYPE)
        
        if (id.isEmpty() || type.isEmpty()) return null

        val url = elementObj.optString(FIELD_URL).takeIf { it.isNotEmpty() }
        val width = elementObj.optInt(FIELD_WIDTH, -1).takeIf { it > 0 }
        val height = elementObj.optInt(FIELD_HEIGHT, -1).takeIf { it > 0 }
        val thumbnailUrl = elementObj.optString(FIELD_THUMBNAIL_URL).takeIf { it.isNotEmpty() }
        val thumbnailWidth = elementObj.optInt(FIELD_THUMBNAIL_WIDTH, -1).takeIf { it > 0 }
        val thumbnailHeight = elementObj.optInt(FIELD_THUMBNAIL_HEIGHT, -1).takeIf { it > 0 }
        val alttext = elementObj.optString(FIELD_ALT_TEXT).takeIf { it.isNotEmpty() }
        val title = elementObj.optString(FIELD_TITLE).takeIf { it.isNotEmpty() }
        val caption = elementObj.optString(FIELD_CAPTION).takeIf { it.isNotEmpty() }
        val transcript = elementObj.optString(FIELD_TRANSCRIPT).takeIf { it.isNotEmpty() }

        val content = mutableMapOf<String, Any>()
        elementObj.keys().forEach { key ->
            if (key !in setOf(
                FIELD_ID, FIELD_TYPE, FIELD_URL, FIELD_WIDTH, FIELD_HEIGHT,
                FIELD_THUMBNAIL_URL, FIELD_THUMBNAIL_WIDTH, FIELD_THUMBNAIL_HEIGHT,
                FIELD_ALT_TEXT, FIELD_TITLE, FIELD_CAPTION, FIELD_TRANSCRIPT
            )) {
                content[key] = elementObj.opt(key) ?: ""
            }
        }

        return MultimodalElement(
            id = id,
            type = type,
            url = url,
            width = width,
            height = height,
            thumbnailUrl = thumbnailUrl,
            thumbnailWidth = thumbnailWidth,
            thumbnailHeight = thumbnailHeight,
            alttext = alttext,
            title = title,
            caption = caption,
            transcript = transcript,
            content = content
        )
    }

    /**
     * Extracts product carousels from the response object
     */
    private fun extractProductCarousels(response: JSONObject): List<ProductCarouselData> {
        val carousels = mutableListOf<ProductCarouselData>()
        val carouselsArray = response.optJSONArray(FIELD_PRODUCT_CAROUSELS)
        
        if (carouselsArray != null) {
            for (i in 0 until carouselsArray.length()) {
                val carouselObj = carouselsArray.optJSONObject(i) ?: continue
                val carousel = parseProductCarousel(carouselObj)
                if (carousel != null) {
                    carousels.add(carousel)
                }
            }
        }
        
        return carousels
    }

    /**
     * Parses a single product carousel from JSON
     */
    private fun parseProductCarousel(carouselObj: JSONObject): ProductCarouselData? {
        val id = carouselObj.optString(FIELD_ID)
        if (id.isEmpty()) return null

        val title = carouselObj.optString(FIELD_TITLE).takeIf { it.isNotEmpty() }
        val layout = when (carouselObj.optString(FIELD_LAYOUT, "horizontal").lowercase()) {
            "vertical" -> CarouselLayout.VERTICAL
            "grid" -> CarouselLayout.GRID
            else -> CarouselLayout.HORIZONTAL
        }

        val products = mutableListOf<ProductCardData>()
        val productsArray = carouselObj.optJSONArray(FIELD_PRODUCTS)
        if (productsArray != null) {
            for (i in 0 until productsArray.length()) {
                val productObj = productsArray.optJSONObject(i) ?: continue
                val product = parseProductCard(productObj)
                if (product != null) {
                    products.add(product)
                }
            }
        }

        val metadata = mutableMapOf<String, Any>()
        val metadataObj = carouselObj.optJSONObject(FIELD_METADATA)
        if (metadataObj != null) {
            metadataObj.keys().forEach { key ->
                metadata[key] = metadataObj.opt(key) ?: ""
            }
        }

        return ProductCarouselData(
            id = id,
            title = title,
            products = products,
            layout = layout,
            metadata = metadata
        )
    }

    /**
     * Parses a single product card from JSON
     */
    private fun parseProductCard(productObj: JSONObject): ProductCardData? {
        val id = productObj.optString(FIELD_ID)
        val title = productObj.optString(FIELD_TITLE)
        
        if (id.isEmpty() || title.isEmpty()) return null

        val description = productObj.optString(FIELD_DESCRIPTION).takeIf { it.isNotEmpty() }
        val price = productObj.optString(FIELD_PRICE).takeIf { it.isNotEmpty() }
        val originalPrice = productObj.optString(FIELD_ORIGINAL_PRICE).takeIf { it.isNotEmpty() }
        val imageUrl = productObj.optString(FIELD_IMAGE_URL).takeIf { it.isNotEmpty() }
        val brand = productObj.optString(FIELD_BRAND).takeIf { it.isNotEmpty() }
        val rating = productObj.optDouble(FIELD_RATING, -1.0).takeIf { it >= 0 }?.toFloat()
        val reviewCount = productObj.optInt(FIELD_REVIEW_COUNT, -1).takeIf { it >= 0 }
        val availability = productObj.optString(FIELD_AVAILABILITY).takeIf { it.isNotEmpty() }

        val actionButtons = mutableListOf<ProductActionButton>()
        val buttonsArray = productObj.optJSONArray(FIELD_ACTION_BUTTONS)
        if (buttonsArray != null) {
            for (i in 0 until buttonsArray.length()) {
                val buttonObj = buttonsArray.optJSONObject(i) ?: continue
                val button = parseActionButton(buttonObj)
                if (button != null) {
                    actionButtons.add(button)
                }
            }
        }

        val metadata = mutableMapOf<String, Any>()
        val metadataObj = productObj.optJSONObject(FIELD_METADATA)
        if (metadataObj != null) {
            metadataObj.keys().forEach { key ->
                metadata[key] = metadataObj.opt(key) ?: ""
            }
        }

        return ProductCardData(
            id = id,
            title = title,
            description = description,
            price = price,
            originalPrice = originalPrice,
            imageUrl = imageUrl,
            brand = brand,
            rating = rating,
            reviewCount = reviewCount,
            availability = availability,
            actionButtons = actionButtons,
            metadata = metadata
        )
    }

    /**
     * Parses an action button from JSON
     */
    private fun parseActionButton(buttonObj: JSONObject): ProductActionButton? {
        val id = buttonObj.optString(FIELD_ID)
        val text = buttonObj.optString("text")
        val typeString = buttonObj.optString("type", "custom")
        
        if (id.isEmpty() || text.isEmpty()) return null

        val type = when (typeString.lowercase()) {
            "add_to_cart" -> ActionButtonType.ADD_TO_CART
            "view_details" -> ActionButtonType.VIEW_DETAILS
            "add_to_wishlist" -> ActionButtonType.ADD_TO_WISHLIST
            "share" -> ActionButtonType.SHARE
            else -> ActionButtonType.CUSTOM
        }

        val url = buttonObj.optString(FIELD_URL).takeIf { it.isNotEmpty() }

        val metadata = mutableMapOf<String, Any>()
        val metadataObj = buttonObj.optJSONObject(FIELD_METADATA)
        if (metadataObj != null) {
            metadataObj.keys().forEach { key ->
                metadata[key] = metadataObj.opt(key) ?: ""
            }
        }

        return ProductActionButton(
            id = id,
            text = text,
            type = type,
            url = url,
            metadata = metadata
        )
    }
}
