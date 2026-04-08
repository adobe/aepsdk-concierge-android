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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.card.RecommendationCards
import com.adobe.marketing.mobile.concierge.ui.components.footer.ChatFooter
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.components.image.LocalAssetImage
import com.adobe.marketing.mobile.concierge.ui.components.serviceintent.CtaButton
import com.adobe.marketing.mobile.concierge.ui.components.suggestions.PromptSuggestions
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

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
    handleLink: (String) -> Unit = {},
    feedbackState: FeedbackState = FeedbackState.None,
    onCtaButtonClick: (String) -> Unit = {}
) {
    when (message.content) {
        is MessageContent.Text -> {
            RenderTextMessage(message, onFeedback, onSuggestionClick, handleLink, feedbackState, onCtaButtonClick)
        }

        is MessageContent.Mixed -> {
            RenderMixedMessage(
                message,
                onFeedback,
                onActionClick,
                onImageClick,
                onSuggestionClick,
                handleLink,
                feedbackState,
                onCtaButtonClick
            )
        }

        is MessageContent.CtaButton -> {
            RenderCtaButton(content = message.content, handleLink = handleLink)
        }
    }
}

@Composable
private fun RenderCtaButton(
    content: MessageContent.CtaButton,
    handleLink: (String) -> Unit
) {
    CtaButton(
        cta = content.button,
        onClick = handleLink,
        applyContainerPadding = false
    )
}

@Composable
private fun RenderTextMessage(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit,
    onSuggestionClick: (String) -> Unit,
    handleLink: (String) -> Unit,
    feedbackState: FeedbackState,
    onCtaButtonClick: (String) -> Unit
) {
    val style = ConciergeStyles.messageBubbleStyle
    val companyIconName = if (!message.isFromUser) ConciergeTheme.tokens?.assets?.icons?.company?.takeIf { it.isNotEmpty() } else null

    if (companyIconName != null) {
        RenderTextMessageWithIcon(
            message = message,
            companyIconName = companyIconName,
            style = style,
            onFeedback = onFeedback,
            onSuggestionClick = onSuggestionClick,
            handleLink = handleLink,
            feedbackState = feedbackState,
            onCtaButtonClick = onCtaButtonClick
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (message.isFromUser) {
                        Modifier.wrapContentWidth(Alignment.End)
                    } else {
                        Modifier
                    }
                )
                .padding(style.padding),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    style.userMessageBackgroundColor
                } else {
                    style.botMessageBackgroundColor
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
            shape = if (message.isFromUser) style.userMessageShape else style.shape
        ) {
            Box(
                modifier = Modifier.padding(style.innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    if (message.isFromUser) {
                        Text(
                            text = message.text,
                            style = style.textStyle,
                            color = style.userMessageTextColor
                        )
                    } else {
                        AgentResponseContent(
                            message = message,
                            handleLink = handleLink,
                            onFeedback = onFeedback,
                            feedbackState = feedbackState
                        )
                    }
                }
            }
        }

        if (!message.isFromUser) {
            BotMessageSuffix(
                message = message,
                onSuggestionClick = onSuggestionClick,
                onCtaButtonClick = onCtaButtonClick
            )
        }
    }
}

/**
 * Icon + message layout for agent text responses when a company icon is configured in the theme.
 * The icon sits to the left of the card; start padding is removed from the card and its inner
 * box so that text aligns flush with the 12dp gap between icon and content.
 */
@Composable
private fun RenderTextMessageWithIcon(
    message: ChatMessage,
    companyIconName: String,
    style: ConciergeStyles.MessageBubbleStyle,
    onFeedback: (FeedbackEvent) -> Unit,
    onSuggestionClick: (String) -> Unit,
    handleLink: (String) -> Unit,
    feedbackState: FeedbackState,
    onCtaButtonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = style.padding)
                    .size(style.agentIconSize)
            ) {
                LocalAssetImage(
                    source = companyIconName,
                    contentDescription = null,
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(style.agentIconSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = style.padding,
                            bottom = style.padding,
                            end = style.padding
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = style.botMessageBackgroundColor
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
                    shape = style.shape
                ) {
                    Box(
                        modifier = Modifier.padding(
                            top = style.innerPadding,
                            bottom = style.innerPadding,
                            end = style.innerPadding
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AgentResponseContent(
                                message = message,
                                handleLink = handleLink,
                                onFeedback = onFeedback,
                                feedbackState = feedbackState
                            )
                        }
                    }
                }
            }
        }

        BotMessageSuffix(
            message = message,
            onSuggestionClick = onSuggestionClick,
            onCtaButtonClick = onCtaButtonClick
        )
    }
}

@Composable
private fun RenderMixedMessage(
    message: ChatMessage,
    onFeedback: (FeedbackEvent) -> Unit,
    onActionClick: (ProductActionButton) -> Unit,
    onImageClick: (MultimodalElement) -> Unit,
    onSuggestionClick: (String) -> Unit,
    handleLink: (String) -> Unit,
    feedbackState: FeedbackState,
    onCtaButtonClick: (String) -> Unit
) {
    val style = ConciergeStyles.messageBubbleStyle
    val content = message.content as MessageContent.Mixed

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                    if (content.text.isNotEmpty()) {
                        ConciergeResponse(
                            text = content.text,
                            sources = message.citations ?: emptyList(),
                            handleLink = handleLink,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Add spacing between text and recommendation cards if both are present
                    if (content.text.isNotEmpty() && !content.multimodalElements.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(style.contentSpacing))
                    }

                    // Render multi-modal elements if present
                    content.multimodalElements?.let { multimodalElements ->
                        if (multimodalElements.isNotEmpty()) {
                            RecommendationCards(
                                elements = multimodalElements,
                                onImageClick = onImageClick,
                                onActionClick = onActionClick
                            )
                        }
                    }

                    // Show footer if we have citations or have an interaction id for providing feedback
                    if (!message.isFromUser && (message.citations != null || message.interactionId != null)) {
                        ChatFooter(
                            citations = message.citations,
                            uniqueCitations = message.uniqueCitations,
                            interactionId = message.interactionId,
                            sseComplete = message.sseComplete,
                            onFeedback = onFeedback,
                            handleLink = handleLink,
                            feedbackState = feedbackState
                        )
                    }
                }
            }
        }

        if (!message.isFromUser) {
            BotMessageSuffix(
                message = message,
                onSuggestionClick = onSuggestionClick,
                onCtaButtonClick = onCtaButtonClick
            )
        }
    }
}

/**
 * The markdown response text and citation footer for a bot message card body.
 * Shared between [RenderTextMessage] and [RenderTextMessageWithIcon].
 */
@Composable
private fun AgentResponseContent(
    message: ChatMessage,
    handleLink: (String) -> Unit,
    onFeedback: (FeedbackEvent) -> Unit,
    feedbackState: FeedbackState
) {
    ConciergeResponse(
        text = message.text,
        sources = message.citations ?: emptyList(),
        handleLink = handleLink,
        modifier = Modifier.fillMaxWidth()
    )

    if (message.citations != null || message.interactionId != null) {
        ChatFooter(
            citations = message.citations,
            uniqueCitations = message.uniqueCitations,
            interactionId = message.interactionId,
            sseComplete = message.sseComplete,
            onFeedback = onFeedback,
            handleLink = handleLink,
            feedbackState = feedbackState
        )
    }
}

/**
 * Prompt suggestions and CTA button shown below the message card for bot responses.
 * Shared across all bot message render functions.
 */
@Composable
private fun BotMessageSuffix(
    message: ChatMessage,
    onSuggestionClick: (String) -> Unit,
    onCtaButtonClick: (String) -> Unit
) {
    if (message.promptSuggestions.isNotEmpty()) {
        PromptSuggestions(
            suggestions = message.promptSuggestions,
            onSuggestionClick = onSuggestionClick
        )
    }

    message.ctaButton?.let { cta ->
        CtaButton(
            cta = cta,
            onClick = onCtaButtonClick
        )
    }
}
