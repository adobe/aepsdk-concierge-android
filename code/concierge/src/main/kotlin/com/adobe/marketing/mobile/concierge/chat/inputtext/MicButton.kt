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

package com.adobe.marketing.mobile.concierge.chat.inputtext

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A voice input button that supports recording and transcribing states.
 *
 * @param modifier The modifier to be applied to the button layout
 * @param isRecording Whether the microphone is currently recording audio
 * @param isTranscribing Whether audio is being processed/transcribed
 * @param onClick The callback function triggered when the button is pressed
 */
@Composable
fun MicButton(
    modifier: Modifier = Modifier,
    isRecording: Boolean,
    isTranscribing: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.2f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "mic_button_scale"
    )
    
    val backgroundColor = when {
        isRecording -> Color.Red
        isTranscribing -> Color(0xFFFF9800) // orange color
        else -> MaterialTheme.colorScheme.primary
    }
    
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .scale(scale),
        shape = CircleShape,
        containerColor = backgroundColor,
        contentColor = Color.White
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = if (isRecording) "Recording" else "Start recording",
            modifier = Modifier.size(24.dp)
        )
    }
}
