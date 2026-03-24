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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.FeedbackThumbsPlacement

/**
 * Footer component for chat messages that includes a sources accordion and feedback buttons.
 * The footer component is only displayed if there are citations provided in the ChatMessage.
 *
 * Layout is determined by `behavior.feedback.thumbsPlacement`:
 * - `"inline"` (default): Sources accordion and feedback thumbs on the same row.
 * - `"below"`: Feedback thumbs with a feedback "helpful" label appear below the sources accordion.
 *
 * @param modifier Optional [Modifier] for this component.
 * @param citations List of [Citation] to display in the sources accordion.
 * @param uniqueCitations Pre-computed list of unique citations.
 * @param interactionId interaction ID for feedback buttons.
 * @param sseComplete True when the SSE stream for this message has completed.
 * @param onFeedback Callback invoked when a feedback button is pressed.
 * @param feedbackState Current feedback state for this interaction.
 */
@Composable
internal fun ChatFooter(
    modifier: Modifier = Modifier,
    citations: List<Citation>?,
    uniqueCitations: List<Citation>? = null,
    interactionId: String?,
    sseComplete: Boolean = false,
    onFeedback: (FeedbackEvent) -> Unit,
    handleLink: (String) -> Unit = {},
    feedbackState: FeedbackState = FeedbackState.None
) {
    val hasCitations = !citations.isNullOrEmpty()
    val showFeedbackButtons = !interactionId.isNullOrEmpty() && sseComplete
    var sourcesExpanded by remember { mutableStateOf(false) }
    val thumbsPlacement = ConciergeTheme.behavior?.feedback?.thumbsPlacement
        ?: FeedbackThumbsPlacement.INLINE

    Column(modifier = modifier) {
        when (thumbsPlacement) {
            FeedbackThumbsPlacement.INLINE -> {
                // Original layout: sources label and thumbs on the same row
                val arrangement = remember(hasCitations) {
                    if (hasCitations) Arrangement.SpaceBetween else Arrangement.End
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = arrangement,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasCitations) {
                        SourcesAccordionButton(
                            expanded = sourcesExpanded,
                            onExpandedChange = { sourcesExpanded = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showFeedbackButtons) {
                        FeedbackButtons(
                            interactionId = interactionId!!,
                            onFeedback = onFeedback,
                            feedbackState = feedbackState
                        )
                    }
                }
                if (hasCitations) {
                    ExpandedCitations(
                        citations = citations!!,
                        uniqueCitations = uniqueCitations,
                        expanded = sourcesExpanded,
                        handleLink = handleLink
                    )
                }
            }

            FeedbackThumbsPlacement.BELOW -> {
                // Design spec: feedback thumbs inside the Sources & Feedback accordion
                if (hasCitations) {
                    SourcesAccordionButton(
                        expanded = sourcesExpanded,
                        onExpandedChange = { sourcesExpanded = it }
                    )
                    ExpandedCitations(
                        modifier = Modifier.padding(start = 28.dp),
                        citations = citations!!,
                        uniqueCitations = uniqueCitations,
                        expanded = sourcesExpanded,
                        handleLink = handleLink,
                        footerContent = if (showFeedbackButtons) {
                            {
                                FeedbackButtons(
                                    interactionId = interactionId!!,
                                    onFeedback = onFeedback,
                                    feedbackState = feedbackState,
                                    showHelpfulLabel = true
                                )
                            }
                        } else null
                    )
                } else if (showFeedbackButtons) {
                    // No citations: show feedback standalone
                    FeedbackButtons(
                        interactionId = interactionId!!,
                        onFeedback = onFeedback,
                        feedbackState = feedbackState,
                        showHelpfulLabel = true
                    )
                }
            }
        }
    }
}
