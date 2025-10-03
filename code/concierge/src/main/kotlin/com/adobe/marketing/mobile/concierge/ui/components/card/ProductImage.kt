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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.components.image.AsyncImage
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.services.Log

/**
 * Composable that displays an image for a product returned in a [MultimodalElement].
 */
@Composable
internal fun ProductImage(
    element: MultimodalElement,
    modifier: Modifier = Modifier,
    onImageClick: (MultimodalElement) -> Unit = {},
    isMultiElement: Boolean = false
) {
    val imageStyle = ConciergeStyles.productImageStyle
    val cardStyle = ConciergeStyles.productCardStyle
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onImageClick(element) },
        shape = if (isMultiElement) imageStyle.multiImageShape else imageStyle.singleImageShape,
        elevation = CardDefaults.cardElevation(defaultElevation = imageStyle.elevation),
        colors = CardDefaults.cardColors(
            containerColor = imageStyle.backgroundColor
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageUrl = element.url ?: element.thumbnailUrl
            if (imageUrl != null) {
                AsyncImage(
                    url = imageUrl,
                    contentDescription = element.alttext ?: element.title,
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        Log.warning(
                            ConciergeConstants.EXTENSION_NAME,
                            "ProductImage",
                            "Failed to load image: $imageUrl, error: $error"
                        )
                    },
                    modifier = Modifier.height(cardStyle.imageHeight)
                )
            } else {
                // Fallback to a gradient background if no image URL is available
                Box(
                    modifier = Modifier
                        .height(cardStyle.imageHeight)
                        .background(
                            Brush.horizontalGradient(
                                colors = cardStyle.fallbackGradientColors
                            )
                        )
                )
            }

            // Product ID overlay in bottom-left corner if there is more than 1 element
            if (isMultiElement) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(imageStyle.overlayPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = imageStyle.overlayBackgroundColor,
                                shape = imageStyle.overlayShape
                            )
                            .padding(imageStyle.overlayInnerPadding)
                    ) {
                        Text(
                            text = element.content["productName"] as? String ?: element.id,
                            color = imageStyle.overlayTextColor,
                            fontSize = imageStyle.overlayTextSize.value.sp,
                            fontWeight = imageStyle.overlayTextFontWeight,
                            style = imageStyle.overlayTextStyle
                        )
                    }
                }
            }
        }
    }
}