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

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.components.image.assetBitmapCache
import com.adobe.marketing.mobile.concierge.ui.components.image.loadAssetBitmap
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composable that manages the mic and send button states and animations.
 * Handles smooth transitions between recording and editing states.
 *
 * @param modifier Modifier for the composable
 * @param inputState The current user input state (determines button visibility and behavior)
 * @param text Current text in the input field (used to enable/disable send button)
 * @param isProcessing Whether a message is currently being processed
 * @param onMicPressed Callback when microphone button is pressed (to start recording)
 * @param onVoiceCancel Callback when recording should be stopped
 * @param onSend Callback when send button is pressed
 * @param onClear Callback when clear button is pressed to clear the input text
 */
@Composable
internal fun InputActionButtons(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    text: String,
    isProcessing: Boolean,
    onMicPressed: () -> Unit,
    onVoiceCancel: () -> Unit,
    onSend: (String) -> Unit,
    onClear: () -> Unit = {}
) {
    val micButtonStyle = ConciergeStyles.micButtonStyle
    val sendButtonStyle = ConciergeStyles.sendButtonStyle

    // Check if voice input is enabled from theme behavior
    val enableVoiceInput = ConciergeTheme.behavior?.enableVoiceInput ?: true

    Row(
        modifier = modifier
            .animateContentSize(animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f))
            .padding(end = 8.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tap-area size for mic / clear / stop buttons. Sized to match the OutlinedTextField's
        // minimum height so the row stays a uniform 56dp when the field is empty/single-line —
        // this eliminates the vertical gap that appeared with Alignment.Bottom.
        val micContainerSize = 56.dp
        val hasText = text.isNotBlank()

        if (enableVoiceInput) {
            when {
                // Clear button (X) — only shown when typing
                hasText && inputState !is UserInputState.Recording -> {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(micContainerSize)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = "Clear input",
                            tint = sendButtonStyle.enabledIconColor.copy(alpha = 0.5f)
                        )
                    }
                }
                // Recording state — animated mic + stop button, both visible together
                inputState is UserInputState.Recording -> {
                    MicButton(
                        modifier = Modifier.size(micContainerSize),
                        userInputState = inputState,
                        isEnabled = true,
                        onClick = {} // animation tap no longer stops recording — stop button does
                    )
                    StopRecordingButton(
                        modifier = Modifier.size(micContainerSize),
                        onClick = onVoiceCancel
                    )
                }
                // Idle — mic icon, tap to start recording
                else -> {
                    MicButton(
                        modifier = Modifier.size(micContainerSize),
                        userInputState = inputState,
                        isEnabled = true,
                        onClick = onMicPressed
                    )
                }
            }
        }

        // Send button - only visible when there is text or a response is processing.
        AnimatedVisibility(
            visible = inputState !is UserInputState.Recording && (hasText || isProcessing),
            enter = fadeIn(animationSpec = tween(durationMillis = 200)) +
                    slideInHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        initialOffsetX = { it / 2 }
                    ),
            exit = fadeOut(animationSpec = tween(durationMillis = 200)) +
                    slideOutHorizontally(
                        animationSpec = tween(durationMillis = 200),
                        targetOffsetX = { it / 2 }
                    )
        ) {
            Row {
                SendButton(
                    modifier = Modifier.size(sendButtonStyle.size),
                    isEnabled = text.isNotBlank() && !isProcessing,
                    onSend = {
                        if (text.isNotBlank()) {
                            onSend(text)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Stop-recording button shown alongside the animated mic during voice recording.
 * Renders the bitmap configured via `behavior.input.stopRecordingIcon` in its original
 * colors when available; otherwise falls back to a tinted Material `Icons.Filled.StopCircle`.
 */
@Composable
private fun StopRecordingButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.micButtonStyle
    val assetName = ConciergeTheme.behavior?.stopRecordingIcon
    val themedBitmap = rememberStopRecordingBitmap(assetName)

    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        // Center the glyph inside the 56dp tap area — keeps the stop visually close to the
        // adjacent mic-wave (also centered in its 56dp container) rather than pushing it to
        // the panel's right edge, which would leave a large gap between wave and stop.
        if (themedBitmap != null) {
            Image(
                bitmap = themedBitmap,
                contentDescription = "Stop recording",
                modifier = Modifier.size(style.size * MIC_INNER_DISC_SCALE)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.StopCircle,
                contentDescription = "Stop recording",
                modifier = Modifier.size(style.size * MIC_INNER_DISC_SCALE),
                tint = style.iconColor
            )
        }
    }
}

/**
 * Loads the stop-recording bitmap from `assets/icons/[assetName].{png,webp,jpg,jpeg}`.
 * Returns null while loading or when the asset name is blank/unresolved — callers should
 * fall back to a default vector icon in that case.
 */
@Composable
private fun rememberStopRecordingBitmap(assetName: String?): ImageBitmap? {
    if (assetName.isNullOrBlank()) return null
    val context = LocalContext.current
    return produceState<ImageBitmap?>(
        initialValue = assetBitmapCache[assetName],
        key1 = assetName
    ) {
        if (!assetBitmapCache.containsKey(assetName)) {
            val loaded = withContext(Dispatchers.IO) {
                loadAssetBitmap(context, assetName)?.asImageBitmap()
            }
            assetBitmapCache[assetName] = loaded
            value = loaded
        }
    }.value
}
