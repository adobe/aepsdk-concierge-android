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
 * A simple chat message data class.
 */
internal data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long,
    val citations: List<Citation>? = null,
    val interactionId: String? = null
)

/**
 * Represents a citation source for a chat message.
 */
internal data class Citation(
    val title: String,
    val url: String? = null
)
