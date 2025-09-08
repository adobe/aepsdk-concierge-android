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

package com.adobe.marketing.mobile.concierge.ui.state

import com.adobe.marketing.mobile.concierge.ui.components.card.ProductCarouselData
import com.adobe.marketing.mobile.concierge.network.MultimodalElements

/**
 * Represents the overall state of the chat screen.
 */
internal sealed class ChatScreenState {
    /**
     * Chat is in idle state, waiting for user interaction.
     */
    object Idle : ChatScreenState()

    /**
     * Chat is actively processing a user message.
     */
    object Processing : ChatScreenState()

    /**
     * Chat is in an error state.
     */
    data class Error(val error: String) : ChatScreenState()
}

/**
 * Represents UI events that can be processed by the ViewModel.
 */
internal sealed class ChatEvent {
    /**
     * User wants to send a message.
     */
    data class SendMessage(val message: String) : ChatEvent()

    /**
     * An error occurred while processing the input.
     */
    data class Error(val message: String) : ChatEvent()

    /**
     * User dismissed the error, returning to idle state.
     */
    object Reset : ChatEvent()

}


internal sealed class MicEvent : ChatEvent() {
    object StartRecording : MicEvent()
    data class StopRecording(val isCancelled: Boolean, val isError: Boolean) : MicEvent()
}

/**
 * Represents feedback events that can be processed by the ViewModel.
 */
internal sealed class FeedbackEvent {
    /**
     * User provided positive feedback for a response.
     */
    data class ThumbsUp(val interactionId: String) : FeedbackEvent()

    /**
     * User provided negative feedback for a response.
     */
    data class ThumbsDown(val interactionId: String) : FeedbackEvent()
}

/**
 * Represents different types of content in a chat message
 */
internal sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class ProductCarousel(val carousel: ProductCarouselData) : MessageContent()
    data class ImageCarousel(val elements: MultimodalElements) : MessageContent()
    data class Mixed(
        val text: String, 
        val productCarousel: ProductCarouselData? = null,
        val imageCarousel: MultimodalElements? = null
    ) : MessageContent()
}

/**
 * A chat message data class that supports different content types.
 */
internal data class ChatMessage(
    val content: MessageContent,
    val isFromUser: Boolean,
    val timestamp: Long,
    val citations: List<Citation>? = null,
    val interactionId: String? = null
) {
    // Convenience constructor for text-only messages (backward compatibility)
    constructor(text: String, isFromUser: Boolean, timestamp: Long) : this(
        MessageContent.Text(text),
        isFromUser,
        timestamp
    )
    
    // Convenience property for text content
    val text: String
        get() = when (content) {
            is MessageContent.Text -> content.text
            is MessageContent.Mixed -> content.text
            is MessageContent.ProductCarousel -> ""
            is MessageContent.ImageCarousel -> ""
        }
}

/**
 * Represents a citation source for a chat message.
 */
internal data class Citation(
    val title: String,
    val url: String? = null
)
