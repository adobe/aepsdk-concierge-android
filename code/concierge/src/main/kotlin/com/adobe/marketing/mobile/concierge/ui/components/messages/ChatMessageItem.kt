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
import com.adobe.marketing.mobile.concierge.ui.components.footer.ChatFooter
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.components.card.ImageCarousel
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductCardData
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductCarousel
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent

/**
 * Component that displays a single chat message.
 */
@Composable
internal fun ChatMessageItem(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit = {},
    onProductClick: (ProductCardData) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {},
    onImageClick: (com.adobe.marketing.mobile.concierge.network.MultimodalElement) -> Unit = {}
) {
    when (message.content) {
        is MessageContent.Text -> {
            renderTextMessage(message)
        }
        is MessageContent.ProductCarousel -> {
            renderProductCarouselMessage(message, onProductClick, onActionClick)
        }
        is MessageContent.ImageCarousel -> {
            renderImageCarouselMessage(message, onImageClick)
        }
        is MessageContent.Mixed -> {
            renderMixedMessage(message, onProductClick, onActionClick, onImageClick)
        }
    }
}

@Composable
private fun renderTextMessage(message: ChatMessage,
                              onFeedback: (FeedbackEvent) -> Unit = {},) {
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

                    // If the message has citations show the footer
                    if (!message.citations.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ChatFooter(
                            citations = message.citations,
                            interactionId = message.interactionId,
                            onFeedback = onFeedback
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun renderProductCarouselMessage(
    message: ChatMessage,
    onProductClick: (ProductCardData) -> Unit,
    onActionClick: (ProductActionButton) -> Unit
) {
    if (message.content is MessageContent.ProductCarousel) {
        ProductCarousel(
            carousel = message.content.carousel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onProductClick = onProductClick,
            onActionClick = onActionClick
        )
    }
}

@Composable
private fun renderImageCarouselMessage(
    message: ChatMessage,
    onImageClick: (com.adobe.marketing.mobile.concierge.network.MultimodalElement) -> Unit
) {
    if (message.content is MessageContent.ImageCarousel) {
        ImageCarousel(
            elements = message.content.elements.elements,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            onImageClick = onImageClick
        )
    }
}

@Composable
private fun renderMixedMessage(
    message: ChatMessage,
    onProductClick: (ProductCardData) -> Unit,
    onActionClick: (ProductActionButton) -> Unit,
    onImageClick: (com.adobe.marketing.mobile.concierge.network.MultimodalElement) -> Unit
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
            
            // Render product carousel if present
            message.content.productCarousel?.let { carousel ->
                ProductCarousel(
                    carousel = carousel,
                    onProductClick = onProductClick,
                    onActionClick = onActionClick
                )
            }
            
            // Render image carousel if present
            message.content.imageCarousel?.let { imageCarousel ->
                ImageCarousel(
                    elements = imageCarousel.elements,
                    onImageClick = onImageClick
                )
            }
        }
    }
}
