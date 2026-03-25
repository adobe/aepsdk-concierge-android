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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Component that displays a list of chat messages.
 */
@Composable
internal fun MessageList(
    modifier: Modifier = Modifier,
    messages: List<ChatMessage>,
    onFeedback: (FeedbackEvent) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {},
    onImageClick: (MultimodalElement) -> Unit = {},
    onSuggestionClick: (String) -> Unit = {},
    handleLink: (String) -> Unit = {},
    onCtaButtonClick: (String) -> Unit = {}
) {
    val style = ConciergeStyles.messageListStyle
    val listState = rememberLazyListState()

    // Keep track of the last user message index we've scrolled to
    var lastScrolledUserMessageIndex by remember { mutableStateOf<Int?>(null) }

    // When a new user message arrives, scroll so that it is anchored at the top
    LaunchedEffect(messages.size) {
        val currentLastUserIndex = messages.indexOfLast { it.isFromUser }
        if (currentLastUserIndex >= 0 && currentLastUserIndex != lastScrolledUserMessageIndex) {
            listState.animateScrollToItem(currentLastUserIndex)
            lastScrolledUserMessageIndex = currentLastUserIndex
        }
    }

    BoxWithConstraints(modifier = modifier) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(style.verticalSpacing),
        ) {
            // Show messages in chronological order (oldest first, newest last)
            itemsIndexed(
                items = messages,
                key = { _, message ->
                    message.interactionId ?: "${message.timestamp}:${message.isFromUser}:${message.text.hashCode()}"
                }
            ) { index, message ->
                // If the last item is an assistant message immediately following the latest user message,
                // set its minimum height to the parent height so the response "fills the screen",
                // but allow it to extend beyond if the content is larger.
                val lastUserIndex = messages.indexOfLast { it.isFromUser }
                val shouldFillRemaining = (index == messages.lastIndex &&
                        !message.isFromUser &&
                        lastUserIndex == index - 1)

                Box(
                    modifier = (
                            if (shouldFillRemaining) Modifier.then(
                                Modifier
                                    .heightIn(min = this@BoxWithConstraints.maxHeight)
                                    .animateContentSize()
                            ) else Modifier
                            )
                ) {
                    ChatMessageItem(
                        message = message,
                        onFeedback = onFeedback,
                        onActionClick = onActionClick,
                        onImageClick = onImageClick,
                        onSuggestionClick = onSuggestionClick,
                        handleLink = handleLink,
                        feedbackState = message.feedbackState,
                        onCtaButtonClick = onCtaButtonClick
                    )
                }
            }
        }
    }
}
