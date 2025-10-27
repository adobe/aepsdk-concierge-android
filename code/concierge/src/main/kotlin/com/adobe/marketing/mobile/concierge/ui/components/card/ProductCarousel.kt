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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Finds the item closest to the center of the viewport.
 * @return The index of the centered item, or null if no items are visible.
 */
private fun LazyListLayoutInfo.findCenteredItemIndex(): Int? {
    val viewportCenter = viewportStartOffset + viewportSize.width / 2
    return visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        abs(itemCenter - viewportCenter)
    }?.index
}

/**
 * Composable that displays a carousel of product images with navigation controls.
 */
@Composable
internal fun ProductCarousel (
    elements: List<MultimodalElement>,
    onImageClick: (MultimodalElement) -> Unit
) {
    val style = ConciergeStyles.productCarouselStyle
    var currentPage by remember { mutableIntStateOf(0) }
    val totalPages = elements.size
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    // Use a Job to track programmatic scrolls and to avoid conflicts with user initiated scrolls
    var scrollJob by remember { mutableStateOf<Job?>(null) }

    // Helper function for scrolling to a page when page indicators or arrows are clicked
    val scrollToPage: (Int) -> Unit = remember {
        { page ->
            currentPage = page
            scrollJob?.cancel()
            scrollJob = coroutineScope.launch {
                listState.animateScrollToItem(page)
            }
        }
    }

    // Update the page indicator when user manually scrolls
    LaunchedEffect(listState) {
        snapshotFlow { 
            listState.isScrollInProgress to listState.layoutInfo
        }
            .distinctUntilChanged()
            .collect { (isScrolling, layoutInfo) ->
                if (!isScrolling) {
                    val newPage = layoutInfo.findCenteredItemIndex() ?: return@collect
                    val isProgrammaticScrollActive = scrollJob?.isActive == true
                    
                    // Only update if no programmatic scroll is active and page changed
                    if (newPage != currentPage && !isProgrammaticScrollActive) {
                        currentPage = newPage
                    }
                }
            }
    }
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Product carousel
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
            contentPadding = PaddingValues(horizontal = style.horizontalPadding, vertical = style.verticalPadding)
        ) {
            items(elements) { element ->
                ProductImage(
                    element = element,
                    modifier = Modifier
                        .width(style.imageWidth)
                        .height(style.imageHeight),
                    onImageClick = onImageClick,
                    isMultiElement = true
                )
            }
        }
        
        // Carousel switcher controls
        CarouselSwitcher(
            currentPage = currentPage,
            totalPages = totalPages,
            onPreviousClick = { 
                if (currentPage > 0) scrollToPage(currentPage - 1)
            },
            onNextClick = { 
                if (currentPage < totalPages - 1) scrollToPage(currentPage + 1)
            },
            onPageClick = { page -> 
                scrollToPage(page)
            }
        )
    }
}

/**
 * Composable that displays carousel navigation controls with arrows and page indicators.
 */
@Composable
internal fun CarouselSwitcher(
    currentPage: Int,
    totalPages: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPageClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.productCarouselStyle
    
    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        IconButton(
            onClick = onPreviousClick,
            enabled = currentPage > 0
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous",
                tint = if (currentPage > 0) {
                    style.navigationIconActiveColor
                } else {
                    style.navigationIconInactiveColor
                }
            )
        }
        Box(modifier = Modifier.width(style.navigationSpacing))
        
        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(style.indicatorSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalPages) { page ->
                Box(
                    modifier = Modifier
                        .size(style.indicatorSize)
                        .clip(CircleShape)
                        .background(
                            if (page == currentPage) {
                                style.indicatorActiveColor
                            } else {
                                style.indicatorInactiveColor
                            }
                        )
                        .clickable { onPageClick(page) }
                )
            }
        }

        // Next button
        Box(modifier = Modifier.width(style.navigationSpacing))
        IconButton(
            onClick = onNextClick,
            enabled = currentPage < totalPages - 1
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next",
                tint = if (currentPage < totalPages - 1) {
                    style.navigationIconActiveColor
                } else {
                    style.navigationIconInactiveColor
                }
            )
        }
    }
}
