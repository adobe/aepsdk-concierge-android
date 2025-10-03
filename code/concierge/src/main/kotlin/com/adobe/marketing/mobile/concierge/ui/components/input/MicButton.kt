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

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * A voice input button that supports recording, transcribing, and idle states.
 * Shows a subtle pulsing background during recording.
 *
 * @param modifier Modifier for the composable
 * @param userInputState The current state of the input stream
 * @param isEnabled Whether the button is enabled
 * @param waveformPulse The current pulse scale value for animation (deprecated, kept for compatibility)
 * @param onClick Callback when button is clicked
 */
@Composable
internal fun MicButton(
    modifier: Modifier = Modifier,
    userInputState: UserInputState,
    isEnabled: Boolean,
    waveformPulse: Float,
    onClick: () -> Unit = {},
) {
    val style = ConciergeStyles.micButtonStyle

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                if (isEnabled) {
                    onClick()
                }
            },
            modifier = Modifier.size(style.size)
        ) {
            Image(
                painter = painterResource(R.drawable.microphone),
                contentDescription = when (userInputState) {
                    is UserInputState.Recording -> "Stop recording"
                    else -> "Start voice input"
                },
                colorFilter = ColorFilter.tint(
                    if (isEnabled) style.iconColor 
                    else style.iconColor.copy(alpha = 0.38f)
                )
            )
        }
    }
}
