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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.ChatFooter
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.components.card.RecommendationCards
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.services.Log

/**
 * Component that displays a single chat message.
 */
@Composable
internal fun ChatMessageItem(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {},
    onImageClick: (MultimodalElement) -> Unit = {}
) {
    when (message.content) {
        is MessageContent.Text -> {
            RenderTextMessage(message)
        }
        is MessageContent.Mixed -> {
            RenderMixedMessage(message, onActionClick, onImageClick)
        }
    }

    // If we have a response message and citations are available then show the footer
    if (!message.isFromUser && !message.citations.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(6.dp))
        ChatFooter(
            citations = message.citations,
            interactionId = message.interactionId,
            onFeedback = onFeedback
        )
    }
}

@Composable
private fun RenderTextMessage(message: ChatMessage) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(if (message.isFromUser) Alignment.End else Alignment.Start)
            .padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFromUser) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Use ConciergeResponse composable for response messages to support markdown formatting
                if (message.isFromUser) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    ConciergeResponse(
                        text = message.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderMixedMessage(
    message: ChatMessage,
    onActionClick: (ProductActionButton) -> Unit,
    onImageClick: (MultimodalElement) -> Unit
) {
    if (message.content is MessageContent.Mixed) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Render text content if present
            if (message.content.text.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.Start)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        ConciergeResponse(
                            text = message.content.text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            
            
            // Render multi-modal elements if present
            message.content.multimodalElements?.let { multimodalElements ->
                if (multimodalElements.isEmpty()) {
                    return@let
                }

                RecommendationCards(
                    elements = multimodalElements,
                    onImageClick = onImageClick,
                    onActionClick = onActionClick
                )
            }
        }
    }
}
