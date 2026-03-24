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

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.adobe.marketing.mobile.concierge.ui.webview.WebviewOverlayDialog
import com.adobe.marketing.mobile.concierge.ui.components.feedback.FeedbackDialog
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.ui.components.header.ChatHeader
import com.adobe.marketing.mobile.concierge.ui.components.disclaimer.ConciergeDisclaimer
import com.adobe.marketing.mobile.concierge.ui.components.input.UserInput
import com.adobe.marketing.mobile.concierge.ui.components.messages.MessageList
import com.adobe.marketing.mobile.concierge.ui.components.welcome.WelcomeCard
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent.ProductActionClick
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent.ProductImageClick
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent.PromptSuggestionClick
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

/**
 * Wrapper composable that manages the display of the Concierge Chat in a dialog.
 *
 * This composable provides a convenient way to integrate Concierge Chat by:
 * - Managing the show/hide state of the chat dialog internally
 * - Rendering your content composable
 * - Providing a callback to trigger the chat dialog from anywhere in your content
 * - Displaying the chat in a properly configured full-screen dialog
 *
 * @param viewModel The ConciergeChatViewModel to use for the chat session
 * @param surfaces List of surface URLs for the chat experience.
 * @param modifier Modifier to be applied to the chat content when displayed
 * @param content The content composable that will be displayed. This composable receives
 *                a `showChat` callback function that can be invoked to show the chat dialog.
 *
 * Example usage:
 * ```
 * @Composable
 * fun MyScreen() {
 *     val viewModel = viewModel<ConciergeChatViewModel>()
 *
 *     ConciergeChat(
 *         viewModel = viewModel,
 *         surfaces = listOf("web://example.com/surface.html")
 *     ) { showChat ->
 *         Button(onClick = { showChat() }) {
 *             Text("Start Chat")
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun ConciergeChat(
    modifier: Modifier = Modifier,
    viewModel: ConciergeChatViewModel,
    surfaces: List<String>? = null,
    handleLink: LinkHandler? = null,
    content: @Composable (showChat: () -> Unit) -> Unit
) {
    val showChatDialog by viewModel.isConciergeActive.collectAsStateWithLifecycle()
    val conciergeState by ConciergeStateRepository.instance.state.collectAsStateWithLifecycle()
    val repository = ConciergeStateRepository.instance

    // Set surfaces in state when provided
    if (surfaces != null) {
        repository.setSurfaces(surfaces)
    }

    // Use passed-in surfaces for ready check when present (state may not have emitted yet this frame)
    val surfacesForReady = surfaces?.takeIf { it.isNotEmpty() } ?: conciergeState.surfaces
    val ready = conciergeState.configurationReady &&
        conciergeState.experienceCloudId != null &&
        surfacesForReady.isNotEmpty()

    if (ready) {
        // Capture the current theme to update welcome card config
        val currentTheme = ConciergeTheme.config
        LaunchedEffect(ConciergeTheme.config) {
            viewModel.updateWelcomeConfigFromTheme(currentTheme)
        }

        // Render the content composable with the callback
        content { viewModel.openConcierge() }

        // Show the chat dialog when requested
        if (showChatDialog) {
            Dialog(
                onDismissRequest = { viewModel.closeConcierge() },
                properties = DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = false
                )
            ) {
                val parentView = LocalView.current.parent as View
                val window = (parentView as DialogWindowProvider).window
                window.setDimAmount(0f)
                window.setWindowAnimations(-1)

                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.attributes.fitInsetsTypes = 0
                    window.attributes.fitInsetsSides = 0
                }

                // Resize the dialog when the keyboard appears so the
                // header stays fixed and the weight(1f) messages area shrinks.
                @Suppress("DEPRECATION")
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                ConciergeChat(
                    viewModel = viewModel,
                    onClose = { viewModel.closeConcierge() },
                    modifier = modifier,
                    handleLink = handleLink
                )
            }
        }

    }
}

/**
 * Optional callback for intercepting link clicks (e.g., product cards, in-message links, citations).
 * Return `true` if the link was handled (e.g., opened as a deep link); return `false` to use
 * default behavior (in-app WebView overlay). When null, all links use the default WebView overlay.
 */
typealias LinkHandler = (url: String) -> Boolean

/**
 * Concierge chat composable (direct mode).
 *
 * @param viewModel The ConciergeChatViewModel for the chat session
 * @param onClose Callback when the close button is pressed
 * @param modifier Optional modifier for the chat content
 * @param handleLink Optional callback to intercept link clicks (product cards, in-message links, citations).
 *        Return true if the link was handled (e.g., opened as a deep link); return false to use
 *        default behavior (in-app WebView overlay). When null, all links use the WebView overlay.
 */
@Composable
fun ConciergeChat(
    viewModel: ConciergeChatViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    handleLink: LinkHandler? = null
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val inputState by viewModel.inputState.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val webviewOverlay by viewModel.webviewOverlay.collectAsStateWithLifecycle(initialValue = null)
    val hasAudioPermission by viewModel.hasAudioPermission.collectAsStateWithLifecycle()
    val showWelcomeCard by viewModel.showWelcomeCard.collectAsStateWithLifecycle()
    val welcomeConfig by viewModel.welcomeConfig.collectAsStateWithLifecycle()
    
    // Determine if user is returning
    val isReturningUser = viewModel.isReturningUser()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Capture the current theme to update welcome card config
    val currentTheme = ConciergeTheme.config
    LaunchedEffect(currentTheme) {
        viewModel.updateWelcomeConfigFromTheme(currentTheme)
    }

    // Refresh permission status when app resumes (e.g., returning from settings)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val resolvedLinkClick: (String) -> Unit = remember(handleLink) {
        { url -> viewModel.handleLinkClick(url, handleLink) }
    }

    val resolvedEvent: (ChatEvent) -> Unit = remember(handleLink) {
        { event ->
            when (event) {
                is ProductActionClick -> {
                    val url = event.button.url
                    if (url.isNullOrEmpty()) viewModel.processEvent(event)
                    else viewModel.handleLinkClick(url, handleLink)
                }
                is ProductImageClick -> {
                    val url = event.element.content["productPageURL"] as? String
                    if (url.isNullOrEmpty()) viewModel.processEvent(event)
                    else viewModel.handleLinkClick(url, handleLink)
                }
                else -> viewModel.processEvent(event)
            }
        }
    }

    CompositionLocalProvider(LocalImageProvider provides viewModel.imageProvider) {
        ConciergeChat(
            messages = messages,
            chatState = state,
            inputState = inputState,
            hasAudioPermission = hasAudioPermission,
            showWelcomeCard = showWelcomeCard,
            welcomeConfig = welcomeConfig,
            isReturningUser = isReturningUser,
            onTextChanged = viewModel::onTextStateChanged,
            onEvent = resolvedEvent,
            handleLink = resolvedLinkClick,
            onPermissionResult = { granted ->
                viewModel.refreshPermissionStatus()
            },
            onClose = onClose,
            modifier = modifier
        )
    }

    // WebView overlay dialog used for handling link clicks that require a browser.
    webviewOverlay?.let { url ->
        WebviewOverlayDialog(
            url = url,
            onDismiss = viewModel::dismissWebviewOverlay
        )
    }
}

@Composable
internal fun ConciergeChat(
    messages: List<ChatMessage>,
    chatState: ChatScreenState,
    inputState: UserInputState,
    hasAudioPermission: Boolean,
    showWelcomeCard: Boolean,
    welcomeConfig: WelcomeConfig,
    isReturningUser: Boolean,
    onTextChanged: (String) -> Unit,
    onEvent: (ChatEvent) -> Unit,
    handleLink: (String) -> Unit = {},
    onPermissionResult: (Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.chatScreenStyle
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    
    // Derive UI state from ChatScreenState
    val isProcessing = chatState is ChatScreenState.Processing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(style.backgroundColor)
            .navigationBarsPadding()
            .statusBarsPadding()
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ChatHeader(
                onClose = onClose
            )

            // Messages list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val messageListStyle = ConciergeStyles.messageListStyle
                MessageList(
                    messages = messages,
                    onFeedback = { feedbackEvent -> onEvent(feedbackEvent) },
                    onActionClick = { button -> onEvent(ProductActionClick(button)) },
                    onImageClick = { element -> onEvent(ProductImageClick(element)) },
                    onSuggestionClick = { suggestion -> onEvent(PromptSuggestionClick(suggestion)) },
                    handleLink = handleLink,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = messageListStyle.horizontalPadding)
                )
                
                // Welcome card, shown when no chat messages have been sent and text input is empty
                val isInputEmpty = inputState is UserInputState.Empty || inputState is UserInputState.Error
                val shouldShowWelcome = showWelcomeCard && messages.isEmpty() && isInputEmpty

                androidx.compose.animation.AnimatedVisibility(
                    visible = shouldShowWelcome,
                    modifier = Modifier.fillMaxSize(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(style.backgroundColor),
                        contentAlignment = Alignment.Center
                    ) {
                        WelcomeCard(
                            config = welcomeConfig,
                            isReturningUser = isReturningUser,
                            onPromptClick = { prompt -> onEvent(ChatEvent.SendMessage(prompt)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // User input
            UserInput(
                inputState = inputState,
                onTextChange = onTextChanged,
                isProcessing = isProcessing,
                hasAudioPermission = hasAudioPermission,
                placeholder = ConciergeTheme.text?.inputPlaceholder,
                onSend = { text ->
                    onEvent(ChatEvent.SendMessage(text))
                },
                onMicEvent = onEvent,
                onPermissionResult = onPermissionResult,
                modifier = Modifier.fillMaxWidth()
            )

            // Disclaimer
            ConciergeDisclaimer(
                disclaimerConfig = ConciergeTheme.disclaimer,
                handleLink = handleLink,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Feedback dialog overlay
        chatState.feedback?.let { feedback ->
            FeedbackDialog(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                feedback = feedback,
                onDismiss = {
                    onEvent(FeedbackEvent.DismissFeedbackDialog)
                },
                onSubmit = { submittedFeedback ->
                    onEvent(FeedbackEvent.SubmitFeedback(submittedFeedback))
                }
            )
        }
    }
}
