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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.card.RecommendationCards
import com.adobe.marketing.mobile.concierge.ui.components.footer.ChatFooter
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.components.suggestions.PromptSuggestions
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Component that displays a single chat message.
 */
@Composable
internal fun ChatMessageItem(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {},
    onImageClick: (MultimodalElement) -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    feedbackState: FeedbackState = FeedbackState.None
) {
    when (message.content) {
        is MessageContent.Text -> {
            RenderTextMessage(message, onFeedback, onSuggestionClick, feedbackState)
        }

        is MessageContent.Mixed -> {
            RenderMixedMessage(
                message,
                onFeedback,
                onActionClick,
                onImageClick,
                onSuggestionClick,
                feedbackState
            )
        }
    }
}

@Composable
private fun RenderTextMessage(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit,
    onSuggestionClick: (String) -> Unit,
    feedbackState: FeedbackState
) {
    val style = ConciergeStyles.messageBubbleStyle

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(if (message.isFromUser) Alignment.End else Alignment.Start)
                .padding(style.padding),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    style.userMessageBackgroundColor
                } else {
                    style.botMessageBackgroundColor
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
            shape = style.shape
        ) {
            Box(
                modifier = Modifier.padding(style.innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Use ConciergeResponse composable for response messages to support markdown formatting
                    if (message.isFromUser) {
                        Text(
                            text = message.text,
                            style = style.textStyle,
                            color = style.userMessageTextColor
                        )
                    } else {
                        ConciergeResponse(
                            text = message.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // If we have a response message and citations are available then show the footer
                    if (!message.isFromUser && !message.citations.isNullOrEmpty()) {
                        ChatFooter(
                            citations = message.citations,
                            interactionId = message.interactionId,
                            onFeedback = onFeedback,
                            feedbackState = feedbackState
                        )
                    }
                }
            }
        }

        // Show prompt suggestions for concierge responses
        if (!message.isFromUser && message.promptSuggestions.isNotEmpty()) {
            PromptSuggestions(
                suggestions = message.promptSuggestions,
                onSuggestionClick = onSuggestionClick
            )
        }
    }
}

@Composable
private fun RenderMixedMessage(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit,
    onActionClick: (ProductActionButton) -> Unit,
    onImageClick: (MultimodalElement) -> Unit,
    onSuggestionClick: (String) -> Unit,
    feedbackState: FeedbackState
) {
    val style = ConciergeStyles.messageBubbleStyle

    if (message.content is MessageContent.Mixed) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.Start)
                    .padding(style.padding),
                colors = CardDefaults.cardColors(
                    containerColor = style.botMessageBackgroundColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
                shape = style.shape
            ) {
                Box(
                    modifier = Modifier.padding(style.innerPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Render text content if present
                        if (message.content.text.isNotEmpty()) {
                            ConciergeResponse(
                                text = message.content.text,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Add spacing between text and recommendation cards if both are present
                        if (message.content.text.isNotEmpty() &&
                            !message.content.multimodalElements.isNullOrEmpty()
                        ) {
                            Spacer(modifier = Modifier.height(style.contentSpacing))
                        }

                        // Render multi-modal elements if present
                        message.content.multimodalElements?.let { multimodalElements ->
                            if (multimodalElements.isNotEmpty()) {
                                RecommendationCards(
                                    elements = multimodalElements,
                                    onImageClick = onImageClick,
                                    onActionClick = onActionClick
                                )
                            }
                        }

                        // If we have a response message and citations are available then show the footer
                        if (!message.isFromUser && !message.citations.isNullOrEmpty()) {
                            ChatFooter(
                                citations = message.citations,
                                interactionId = message.interactionId,
                                onFeedback = onFeedback,
                                feedbackState = feedbackState
                            )
                        }
                    }
                }
            }

            // Show prompt suggestions for concierge responses
            if (!message.isFromUser && message.promptSuggestions.isNotEmpty()) {
                PromptSuggestions(
                    suggestions = message.promptSuggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
    }
}
