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

package com.adobe.marketing.mobile.concierge.ui.components.feedback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import kotlinx.coroutines.delay

/**
 * Toast notification component for feedback confirmation.
 * Automatically hides after 3 seconds.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param isVisible Whether the toast is currently visible.
 * @param message The message to display in the toast.
 * @param onDismiss Callback to hide the toast (called automatically after 3s or when close button is clicked).
 */
@Composable
internal fun FeedbackToast(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    val style = ConciergeStyles.feedbackToastStyle

    // Auto-hide after 3 seconds
    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(3000L)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = style.backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
            shape = style.shape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(style.contentPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkmark icon
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Checkmark",
                    modifier = Modifier.size(style.iconSize),
                    tint = style.iconColor
                )
                
                Spacer(modifier = Modifier.width(style.iconSpacing))
                
                // Message text
                Text(
                    text = message,
                    style = style.messageTextStyle,
                    color = style.messageTextColor,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )
                
                Spacer(modifier = Modifier.width(style.messageCloseSpacing))
                
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(style.closeButtonSize)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(style.closeIconSize),
                        tint = style.closeIconColor
                    )
                }
            }
        }
    }
}
