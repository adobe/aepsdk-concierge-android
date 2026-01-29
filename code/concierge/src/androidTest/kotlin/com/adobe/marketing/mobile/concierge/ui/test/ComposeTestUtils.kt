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

package com.adobe.marketing.mobile.concierge.ui.test

import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent

/**
 * Test utilities and factory functions for UI tests.
 * Provides common test data and helper functions.
 */
internal object ComposeTestUtils {

    /**
     * Creates a simple text message for testing.
     */
    internal fun createTextMessage(
        text: String,
        isFromUser: Boolean = false,
        timestamp: Long = System.currentTimeMillis(),
        interactionId: String? = null
    ): ChatMessage {
        return ChatMessage(
            content = MessageContent.Text(text),
            isFromUser = isFromUser,
            timestamp = timestamp,
            interactionId = interactionId
        )
    }

    /**
     * Creates a message with citations for testing.
     */
    internal fun createMessageWithCitations(
        text: String,
        citations: List<Citation>,
        isFromUser: Boolean = false,
        interactionId: String = "test-interaction"
    ): ChatMessage {
        return ChatMessage(
            content = MessageContent.Text(text),
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis(),
            citations = citations,
            uniqueCitations = citations,
            interactionId = interactionId
        )
    }

    /**
     * Creates a mixed content message with multimodal elements.
     */
    internal fun createMixedMessage(
        text: String,
        multimodalElements: List<MultimodalElement>,
        isFromUser: Boolean = false
    ): ChatMessage {
        return ChatMessage(
            content = MessageContent.Mixed(text, multimodalElements),
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Creates a message with prompt suggestions for testing.
     */
    internal fun createMessageWithSuggestions(
        text: String,
        suggestions: List<String>,
        isFromUser: Boolean = false
    ): ChatMessage {
        return ChatMessage(
            content = MessageContent.Text(text),
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis(),
            promptSuggestions = suggestions
        )
    }

    /**
     * Creates a sample Citation for testing.
     */
    internal fun createCitation(
        url: String = "https://example.com/source",
        title: String = "Test Source",
        startIndex: Int? = 0,
        endIndex: Int? = 10
    ): Citation {
        return Citation(
            url = url,
            title = title,
            startIndex = startIndex,
            endIndex = endIndex
        )
    }

    /**
     * Creates a sample MultimodalElement for testing.
     */
    internal fun createMultimodalElement(
        id: String = "element-1",
        title: String? = "Test Product",
        caption: String? = "Test Caption",
        url: String? = "https://example.com/image.jpg",
        alttext: String? = null,
        content: Map<String, Any> = emptyMap()
    ): MultimodalElement {
        return MultimodalElement(
            id = id,
            title = title,
            caption = caption,
            url = url,
            alttext = alttext,
            content = content
        )
    }

    /**
     * Creates a MultimodalElement with action buttons.
     */
    internal fun createProductWithActions(
        id: String = "product-1",
        title: String = "Product",
        primaryText: String? = null,
        primaryUrl: String? = null,
        secondaryText: String? = null,
        secondaryUrl: String? = null
    ): MultimodalElement {
        val content = mutableMapOf<String, Any>()
        primaryText?.let { content["primaryText"] = it }
        primaryUrl?.let { content["primaryUrl"] = it }
        secondaryText?.let { content["secondaryText"] = it }
        secondaryUrl?.let { content["secondaryUrl"] = it }

        return MultimodalElement(
            id = id,
            title = title,
            url = "https://example.com/product.jpg",
            content = content
        )
    }
}
