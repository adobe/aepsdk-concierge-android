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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.services.Log

/**
 * Composable that displays product recommendation cards containing one or more [MultimodalElement]s
 * in a single card or carousel style layout.
 */
@Composable
internal fun RecommendationCards(
    elements: List<MultimodalElement>,
    modifier: Modifier = Modifier,
    onImageClick: (MultimodalElement) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {}
) {
    if (elements.isEmpty()) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "RecommendationCards", "No elements to display, returning early")
        return
    }
    
    Log.debug(ConciergeConstants.EXTENSION_NAME, "RecommendationCards", "Rendering ImageCarousel with ${elements.size} elements")

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Special handling for single element - display as a product card
        if (elements.size == 1) {
            ProductCard(
                element = elements[0],
                onImageClick = onImageClick,
                onActionClick = onActionClick
            )
        } else {
            // Multiple elements - display in a product carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
            ) {
                items(elements) { element ->
                    ProductImage(
                        element = element,
                        modifier = modifier
                            .width(200.dp)
                            .height(150.dp),
                        onImageClick = onImageClick,
                        isMultiElement = true
                    )
                }
            }
        }
    }
}

/**
 * Data class representing an action button for product recommendations with multimodal elements
 */
internal data class ProductActionButton(
    val id: String,
    val text: String,
    val url: String? = null
)