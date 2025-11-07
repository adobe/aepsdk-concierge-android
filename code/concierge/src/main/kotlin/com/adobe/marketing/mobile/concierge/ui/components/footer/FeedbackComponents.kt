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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Feedback buttons component with a thumbs up and thumbs down button.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param interactionId Interaction ID for the feedback buttons.
 * @param onFeedback Callback invoked when a feedback button is pressed.
 */
@Composable
internal fun FeedbackButtons(
    modifier: Modifier = Modifier,
    interactionId: String,
    onFeedback: (FeedbackEvent) -> Unit
) {
    val style = ConciergeStyles.feedbackButtonsStyle

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(style.spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbs up button
        IconButton(
            onClick = { onFeedback(FeedbackEvent.ThumbsUp(interactionId)) },
            modifier = Modifier.size(style.buttonSize)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.thumbs_up),
                contentDescription = "Thumbs up",
                modifier = Modifier.size(style.iconSize),
                tint = style.iconColor
            )
        }

        // Thumbs down button
        IconButton(
            onClick = { onFeedback(FeedbackEvent.ThumbsDown(interactionId)) },
            modifier = Modifier.size(style.buttonSize)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.thumbs_down),
                contentDescription = "Thumbs down",
                modifier = Modifier.size(style.iconSize),
                tint = style.iconColor
            )
        }
    }
}
