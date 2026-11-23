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

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

private fun turnKey(message: ChatMessage): String = message.turnId ?: message.localId

/** Start of the turn ending before [end]; rows without a turn ID fall back to the last user row. */
private fun turnStartBefore(messages: List<ChatMessage>, end: Int): Int {
    val turnId = messages.getOrNull(end - 1)?.turnId
        ?: return messages.subList(0, end.coerceIn(0, messages.size)).indexOfLast { it.isFromUser }
    for (index in end - 1 downTo 0) {
        if (messages[index].turnId != turnId) return index + 1
    }
    return 0
}

internal fun privacyNoticeIndex(
    messages: List<ChatMessage>,
    privacyNotice: PrivacyNoticeConfig?
): Int {
    if (privacyNotice?.hasContent != true) return -1
    return messages.indexOfFirst { it.isFromUser && it.content is MessageContent.Text }
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
    listState: LazyListState = rememberLazyListState()
) {
    val style = ConciergeStyles.messageListStyle

    val turnStartIndex = remember(messages) { turnStartBefore(messages, messages.size) }
    val turnStart = messages.getOrNull(turnStartIndex)
    val turnId = turnStart?.let(::turnKey)
    val previousTurnId = remember(messages, turnStartIndex) {
        if (turnStartIndex > 0) messages.getOrNull(turnStartBefore(messages, turnStartIndex))?.let(::turnKey) else null
    }
    val currentMessages = rememberUpdatedState(messages)
    val currentTurn = rememberUpdatedState(turnId to previousTurnId)
    // Bounded: turns are appended, and a rollback only exposes the turn before the handled one.
    var handledTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    var handledPreviousTurnId by rememberSaveable { mutableStateOf<String?>(null) }
    val rowHeights = remember(turnId) { mutableStateMapOf<String, Int>() }
    val privacyNoticeIndex = remember(messages, privacyNotice) { privacyNoticeIndex(messages, privacyNotice) }
    val density = LocalDensity.current

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            val (turn, previous) = currentTurn.value
            if (interaction is DragInteraction.Start && turn != null && turn != handledPreviousTurnId) {
                handledTurnId = turn
                handledPreviousTurnId = previous
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        val turnRows = if (turnStartIndex >= 0) messages.drop(turnStartIndex) else emptyList()
        val turnHeight = turnRows.sumOf { rowHeights[it.localId] ?: 0 } +
            with(density) { style.verticalSpacing.roundToPx() } * (turnRows.size - 1).coerceAtLeast(0)
        val trailingSpace = if (turnStart == null) 0 else (constraints.maxHeight - turnHeight).coerceAtLeast(0)

        LaunchedEffect(turnId) {
            if (messages.isEmpty()) {
                handledTurnId = null
                handledPreviousTurnId = null
            }
            // A rollback must not navigate back to the turn before the handled one.
            if (turnId == null || turnId == handledTurnId || turnId == handledPreviousTurnId) return@LaunchedEffect
            val anchorIndex = snapshotFlow {
                val latest = currentMessages.value
                latest.indexOfFirst { turnKey(it) == turnId }.takeIf {
                    it >= 0 && listState.layoutInfo.totalItemsCount == latest.size &&
                        listState.layoutInfo.viewportSize.height > 0
                }
            }.first { it != null } ?: return@LaunchedEffect
            // A drag that started while waiting for layout already claimed this turn.
            if (turnId == handledTurnId) return@LaunchedEffect
            try {
                listState.animateScrollToItem(anchorIndex)
            } catch (e: CancellationException) {
                // Disposal leaves the turn pending for restoration; an active user scroll claims it.
                if (!isActive) throw e
            }
            handledTurnId = turnId
            handledPreviousTurnId = previousTurnId
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(style.verticalSpacing),
            contentPadding = PaddingValues(bottom = with(density) { trailingSpace.toDp() })
        ) {
            // Show messages in chronological order (oldest first, newest last)
            itemsIndexed(
                items = messages,
                key = { _, message -> message.localId }
            ) { index, message ->
                Box(
                    modifier = if (index >= turnStartIndex && turnStart != null) {
                        Modifier.onSizeChanged { rowHeights[message.localId] = it.height }
                    } else Modifier
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
