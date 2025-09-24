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

package com.adobe.marketing.mobile.concierge.ui.components.card

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement

/**
 * Composable that displays a single product recommendation in a card element containing
 * a large image with two action buttons.
 */
@Composable
internal fun ProductCard(
    element: MultimodalElement,
    modifier: Modifier = Modifier,
    onImageClick: (MultimodalElement) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onImageClick(element) },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        // Background image (if available)
        ProductImage(
            element = element,
            modifier = Modifier.fillMaxWidth(),
            onImageClick = { onImageClick(element) }
        )

        // Display area for text and buttons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.End)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .align(Alignment.TopStart)
            ) {
                // Title
                if (element.title != null) {
                    Text(
                        text = element.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Caption/Description
                if (element.caption != null && element.caption != element.title) {
                    Text(
                        text = element.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
                    )
                }

                // Action Buttons
                val actionButtons = extractActionButtons(element)
                ProductActionButtons(
                    actionButtons = actionButtons,
                    onActionClick = onActionClick
                )
            }
        }

    }
}

/**
 * Helper function to extract action buttons from MultimodalElement content map
 */
private fun extractActionButtons(element: MultimodalElement): List<ProductActionButton> {
    val actionButtons = mutableListOf<ProductActionButton>()

    // Extract primary action button
    val primaryText = element.content["primaryText"] as? String
    val primaryUrl = element.content["primaryUrl"] as? String
    if (!primaryText.isNullOrEmpty()) {
        actionButtons.add(
            ProductActionButton(
                id = "${element.id}_primary",
                text = primaryText,
                url = primaryUrl
            )
        )
    }

    // Extract secondary action button
    val secondaryText = element.content["secondaryText"] as? String
    val secondaryUrl = element.content["secondaryUrl"] as? String
    if (!secondaryText.isNullOrEmpty()) {
        actionButtons.add(
            ProductActionButton(
                id = "${element.id}_secondary",
                text = secondaryText,
                url = secondaryUrl
            )
        )
    }

    return actionButtons
}