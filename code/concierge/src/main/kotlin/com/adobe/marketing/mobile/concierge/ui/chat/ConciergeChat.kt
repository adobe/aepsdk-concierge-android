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

package com.adobe.marketing.mobile.concierge.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adobe.marketing.mobile.concierge.ui.components.header.ChatHeader
import com.adobe.marketing.mobile.concierge.ui.components.input.UserInput
import com.adobe.marketing.mobile.concierge.ui.components.messages.MessageList
import com.adobe.marketing.mobile.concierge.ui.components.overlay.ErrorOverlay
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState

@Composable
fun ConciergeChat(
    viewModel: ConciergeChatViewModel,
    onClose: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inputState by viewModel.inputState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    // TODO: Need to expose this permission to the app level to handle permission requests
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()

    // Derive UI state from ChatScreenState
    val isProcessing = state is ChatScreenState.Processing
    val errorMessage = (state as? ChatScreenState.Error)?.error

    ConciergeChat(
        messages = messages,
        isProcessing = isProcessing,
        errorMessage = errorMessage,
        inputState = inputState,
        hasAudioPermission = hasAudioPermission,
        onTextStateChanged = viewModel::onTextStateChanged,
        onEvent = viewModel::processEvent,
        onPermissionResult = { granted ->
            viewModel.refreshPermissionStatus()
        },
        onClose = onClose
    )
}

@Composable
internal fun ConciergeChat(
    messages: List<ChatMessage>,
    isProcessing: Boolean,
    errorMessage: String?,
    inputState: UserInputState,
    hasAudioPermission: Boolean,
    onTextStateChanged: (Boolean) -> Unit,
    onEvent: (ChatEvent) -> Unit,
    onPermissionResult: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            ChatHeader(onClose = onClose)

            // Messages list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                MessageList(
                    messages = messages,
                    onFeedback = onEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                )
            }

            // User input
            UserInput(
                inputState = inputState,
                onContentAvailabilityChange = onTextStateChanged,
                isProcessing = isProcessing,
                hasAudioPermission = hasAudioPermission,
                onSend = { text ->
                    onEvent(ChatEvent.SendMessage(text))
                },
                onMicEvent = onEvent,
                onPermissionResult = onPermissionResult,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Error overlay if there's an error
        errorMessage?.let {
            ErrorOverlay(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                errorMessage = it,
                onDismiss = {
                    onEvent(ChatEvent.Reset)
                }
            )
        }
    }
}
