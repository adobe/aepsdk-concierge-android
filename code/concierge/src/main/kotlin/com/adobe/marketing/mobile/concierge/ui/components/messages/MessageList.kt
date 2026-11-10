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
import androidx.compose.foundation.layout.Column
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
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig

internal fun privacyNoticeIndex(
    messages: List<ChatMessage>,
    privacyNotice: PrivacyNoticeConfig?
): Int {
    if (privacyNotice?.hasContent != true) return -1
    return messages.indexOfFirst { it.isFromUser && it.content is MessageContent.Text }
}

/**
 * Whether the message at [index] reserves the list's height below itself, so the scroll can bring
 * the turn's first message to the top and the reply shows below it. That's the last message when it
 * is the assistant's reply right after the latest user message, or, during a voice conversation,
 * when only assistant bubbles follow [voiceScrollAnchorIndex] (or it is the anchor).
 */
internal fun shouldFillRemainingHeight(
    messages: List<ChatMessage>,
    index: Int,
    voiceScrollAnchorIndex: Int?
): Boolean {
    if (index != messages.lastIndex || index < 0) return false
    if (voiceScrollAnchorIndex != null && voiceScrollAnchorIndex in 0..index &&
        (voiceScrollAnchorIndex + 1..index).none { messages[it].isFromUser }
    ) {
        return true
    }
    return !messages[index].isFromUser && messages.indexOfLast { it.isFromUser } == index - 1
}

/**
 * The message to scroll to the top, or null to stay: the voice anchor during a voice conversation,
 * otherwise the latest user message, once each. Only a later one than [lastScrolledIndex] scrolls,
 * so the end of a voice conversation doesn't jump back to an earlier user message; a cleared
 * list starts over.
 */
internal fun scrollTargetIndex(
    messages: List<ChatMessage>,
    voiceScrollAnchorIndex: Int?,
    lastScrolledIndex: Int?
): Int? {
    val target = voiceScrollAnchorIndex ?: messages.indexOfLast { it.isFromUser }
    if (target !in messages.indices) return null
    return if (lastScrolledIndex == null || target > lastScrolledIndex || lastScrolledIndex >= messages.size) {
        target
    } else {
        null
    }
}

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
    handleLink: (String, String) -> Unit = { _, _ -> },
    onCtaButtonClick: (CtaButton) -> Unit = {},
    privacyNotice: PrivacyNoticeConfig? = null,
    voiceScrollAnchorIndex: Int? = null
) {
    val style = ConciergeStyles.messageListStyle
    val listState = rememberLazyListState()

    // Keep track of the last message index we've scrolled to the top
    var lastScrolledIndex by remember { mutableStateOf<Int?>(null) }

    val privacyNoticeIndex = remember(messages, privacyNotice) { privacyNoticeIndex(messages, privacyNotice) }

    // When a new user message (or voice anchor) arrives, scroll so that it is anchored at the top
    LaunchedEffect(messages.size, voiceScrollAnchorIndex) {
        val target = scrollTargetIndex(messages, voiceScrollAnchorIndex, lastScrolledIndex)
        if (target != null) {
            listState.animateScrollToItem(target)
            lastScrolledIndex = target
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
                // Set the filling item's minimum height to the parent height so the response
                // "fills the screen", but allow it to extend beyond if the content is larger.
                val shouldFillRemaining = shouldFillRemainingHeight(messages, index, voiceScrollAnchorIndex)

                Box(
                    modifier = (
                            if (shouldFillRemaining) Modifier.then(
                                Modifier
                                    .heightIn(min = this@BoxWithConstraints.maxHeight)
                                    .animateContentSize()
                            ) else Modifier
                            )
                ) {
                    if (index == privacyNoticeIndex && privacyNotice != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(style.verticalSpacing)) {
                            PrivacyNoticeView(config = privacyNotice, handleLink = handleLink)
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
                    } else {
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
}
