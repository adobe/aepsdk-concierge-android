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

import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState

/**
 * Enum representing feedback sentiment type
 */
internal enum class FeedbackType {
    POSITIVE,
    NEGATIVE
}

/**
 * Data class for feedback - used both for showing the dialog and submitting feedback
 */
internal data class Feedback(
    val interactionId: String,
    val feedbackType: FeedbackType,
    val selectedCategories: List<String> = emptyList(),
    val notes: String = "",
    val conversationId: String? = null
)

/**
 * Represents the overall state of the chat screen.
 */
internal sealed class ChatScreenState {
    abstract val feedback: Feedback?

    /**
     * Chat is in idle state, waiting for user interaction.
     */
    data class Idle(
        override val feedback: Feedback? = null
    ) : ChatScreenState()

    /**
     * Chat is actively processing a user message.
     */
    data class Processing(
        override val feedback: Feedback? = null
    ) : ChatScreenState()

    /**
     * Chat is in an error state.
     */
    data class Error(
        val error: String,
        override val feedback: Feedback? = null
    ) : ChatScreenState()
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
internal sealed class FeedbackEvent : ChatEvent() {
    /**
     * User provided positive feedback for a response.
     */
    data class ThumbsUp(val interactionId: String) : FeedbackEvent()

    /**
     * User provided negative feedback for a response.
     */
    data class ThumbsDown(val interactionId: String) : FeedbackEvent()
    
    /**
     * User submitted feedback through the dialog.
     */
    data class SubmitFeedback(val feedback: Feedback) : FeedbackEvent()
    
    /**
     * User dismissed the feedback dialog.
     */
    object DismissFeedbackDialog : FeedbackEvent()
}

/**
 * Represents message interaction events that can be processed by the ViewModel.
 */
internal sealed class MessageInteractionEvent : ChatEvent() {
    /**
     * User clicked on a product action button.
     */
    data class ProductActionClick(val button: ProductActionButton) : MessageInteractionEvent()

    /**
     * User clicked on a product image.
     */
    data class ProductImageClick(val element: MultimodalElement) : MessageInteractionEvent()

    /**
     * User clicked on a prompt suggestion.
     */
    data class PromptSuggestionClick(val suggestion: String) : MessageInteractionEvent()
}

/**
 * Represents different types of content in a chat message
 */
// TODO: Find a better place for this, e.g. ChatMessage.MessageContent
internal sealed class MessageContent {
    data class Text(val text: String) : MessageContent()
    data class Mixed(
        val text: String,
        val multimodalElements: List<MultimodalElement>? = null
    ) : MessageContent()
    data class CtaButton(val button: NetworkCtaButton) : MessageContent()
}

/**
 * A chat message data class that supports different content types.
 */
internal data class ChatMessage(
    val content: MessageContent,
    val isFromUser: Boolean,
    val timestamp: Long,
    val citations: List<Citation>? = null,
    val uniqueCitations: List<Citation>? = null,
    val interactionId: String? = null,
    val sseComplete: Boolean = false,
    val promptSuggestions: List<String> = emptyList(),
    val feedbackState: FeedbackState = FeedbackState.None,
    val ctaButton: NetworkCtaButton? = null
) {
    val text: String
        get() = when (content) {
            is MessageContent.Text -> content.text
            is MessageContent.Mixed -> content.text
            is MessageContent.CtaButton -> content.button.label
        }
}
