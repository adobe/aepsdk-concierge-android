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

package com.adobe.marketing.mobile.concierge.ui.components.footer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Feedback buttons component with a thumbs up and thumbs down button.
 *
 * When [showHelpfulLabel] is true, the feedback helpful label is shown
 * above the thumb icons (matching the design spec).
 *
 * @param modifier Optional [Modifier] for this component.
 * @param interactionId Interaction ID for the feedback buttons.
 * @param onFeedback Callback invoked when a feedback button is pressed.
 * @param feedbackState Current state of feedback for this interaction.
 * @param showHelpfulLabel Whether to show the feedback helpful label above the thumbs.
 */
@Composable
internal fun FeedbackButtons(
    modifier: Modifier = Modifier,
    interactionId: String,
    onFeedback: (FeedbackEvent) -> Unit,
    feedbackState: FeedbackState = FeedbackState.None,
    showHelpfulLabel: Boolean = false
) {
    val style = ConciergeStyles.feedbackButtonsStyle

    if (showHelpfulLabel) {
        // BELOW mode: label on top, thumbs row underneath
        Column(modifier = modifier) {
            if (style.helpfulLabelText.isNotEmpty()) {
                Text(
                    text = style.helpfulLabelText,
                    style = style.helpfulLabelStyle,
                    color = style.helpfulLabelColor
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            ThumbsRow(
                interactionId = interactionId,
                onFeedback = onFeedback,
                feedbackState = feedbackState,
                style = style
            )
        }
    } else {
        // INLINE mode: just the thumbs row
        ThumbsRow(
            modifier = modifier,
            interactionId = interactionId,
            onFeedback = onFeedback,
            feedbackState = feedbackState,
            style = style
        )
    }
}

@Composable
private fun ThumbsRow(
    modifier: Modifier = Modifier,
    interactionId: String,
    onFeedback: (FeedbackEvent) -> Unit,
    feedbackState: FeedbackState,
    style: ConciergeStyles.FeedbackButtonsStyle
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(style.spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbs up button
        IconButton(
            onClick = {
                if (feedbackState == FeedbackState.None) {
                    onFeedback(FeedbackEvent.ThumbsUp(interactionId))
                }
            },
            modifier = Modifier.size(style.buttonSize),
            enabled = feedbackState == FeedbackState.None
        ) {
            Icon(
                painter = painterResource(
                    if (feedbackState == FeedbackState.Positive) {
                        R.drawable.thumbs_up_filled
                    } else {
                        R.drawable.thumbs_up
                    }
                ),
                contentDescription = "Thumbs up",
                modifier = Modifier.size(style.iconSize),
                tint = style.iconColor
            )
        }

        // Thumbs down button
        IconButton(
            onClick = {
                if (feedbackState == FeedbackState.None) {
                    onFeedback(FeedbackEvent.ThumbsDown(interactionId))
                }
            },
            modifier = Modifier.size(style.buttonSize),
            enabled = feedbackState == FeedbackState.None
        ) {
            Icon(
                painter = painterResource(
                    id = if (feedbackState == FeedbackState.Negative) {
                        R.drawable.thumbs_down_filled
                    } else {
                        R.drawable.thumbs_down
                    }
                ),
                contentDescription = "Thumbs down",
                modifier = Modifier.size(style.iconSize),
                tint = style.iconColor
            )
        }
    }
}

/**
 * Enum representing the current feedback state for an interaction
 */
enum class FeedbackState {
    None,
    Positive,
    Negative
}
