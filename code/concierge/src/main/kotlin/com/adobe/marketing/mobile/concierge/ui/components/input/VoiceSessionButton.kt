/*
 * Copyright 2026 Adobe. All rights reserved.
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

internal const val VOICE_SESSION_TOGGLE_TAG = "voiceSessionToggle"
internal const val START_VOICE_CONVERSATION_LABEL = "Start voice conversation"
internal const val STOP_VOICE_CONVERSATION_LABEL = "Stop voice conversation"

/**
 * What the composer shows for voice conversations.
 *
 * @property isAvailable A voice plugin is registered; without one the voice button is hidden.
 * @property isActive A voice conversation is running (connecting or connected).
 * @property isConnecting The running conversation is still connecting.
 * @property isEnabled The voice button can be tapped.
 */
@Immutable
internal data class VoiceControlState(
    val isAvailable: Boolean = false,
    val isActive: Boolean = false,
    val isConnecting: Boolean = false,
    val isEnabled: Boolean = false
)

/** The control shown at the trailing edge of the composer. */
internal enum class TrailingControl {
    /** Dictation in progress: the animated mic and the stop button. */
    RECORDING,

    /** The voice conversation button, preceded by the mic when the mic is shown. */
    VOICE,

    /** The clear and send buttons for typed text. */
    SEND,

    /** The dictation mic. */
    MIC,

    NONE
}

/**
 * Picks the composer's trailing control. Voice takes the place of send while the field is empty,
 * and stays put while a conversation is running or connecting so it can always be stopped.
 */
internal fun trailingControl(
    isRecording: Boolean,
    hasText: Boolean,
    showMic: Boolean,
    isVoiceAvailable: Boolean,
    isVoiceSessionActive: Boolean,
    isVoiceConnecting: Boolean
): TrailingControl = when {
    isRecording -> TrailingControl.RECORDING
    isVoiceAvailable && (!hasText || isVoiceSessionActive || isVoiceConnecting) -> TrailingControl.VOICE
    hasText -> TrailingControl.SEND
    showMic -> TrailingControl.MIC
    else -> TrailingControl.NONE
}

/**
 * Starts or stops a voice conversation. Shows a waveform, filled while a conversation runs, with
 * a spinner while it connects.
 */
@Composable
internal fun VoiceSessionButton(
    modifier: Modifier = Modifier,
    voiceControl: VoiceControlState,
    onClick: () -> Unit
) {
    val style = ConciergeStyles.voiceButtonStyle
    val isEnabled = voiceControl.isEnabled
    val label = if (voiceControl.isActive) STOP_VOICE_CONVERSATION_LABEL else START_VOICE_CONVERSATION_LABEL
    val baseIconColor = if (voiceControl.isActive) style.activeIconColor else style.iconColor
    val iconColor = if (isEnabled) baseIconColor else baseIconColor.copy(alpha = style.disabledAlpha)

    Box(
        modifier = modifier.size(style.size),
        contentAlignment = Alignment.Center
    ) {
        if (voiceControl.isActive) {
            val background = if (isEnabled) {
                style.activeBackgroundColor
            } else {
                style.activeBackgroundColor.copy(alpha = style.disabledAlpha)
            }
            Box(
                modifier = Modifier
                    .size(style.size)
                    .clip(CircleShape)
                    .background(background)
            )
        }
        IconButton(
            onClick = { if (isEnabled) onClick() },
            enabled = isEnabled,
            modifier = Modifier
                .size(style.size)
                .testTag(VOICE_SESSION_TOGGLE_TAG)
                .semantics { contentDescription = label }
        ) {
            if (voiceControl.isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(style.size * 0.6f),
                    color = if (voiceControl.isActive) style.activeIconColor else style.progressColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.concierge_ic_audiowave),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(if (voiceControl.isActive) style.size * 0.6f else style.size)
                )
            }
        }
    }
}
