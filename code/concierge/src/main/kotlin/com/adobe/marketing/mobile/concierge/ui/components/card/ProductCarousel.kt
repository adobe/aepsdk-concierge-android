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

import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Composable that displays a carousel of product images with navigation controls.
 */
// HorizontalPager must be used with the experimental opt-in annotation but has been stabilized
// in the next available compose version.
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProductCarousel(
    elements: List<MultimodalElement>,
    onImageClick: (MultimodalElement) -> Unit
) {
    val style = ConciergeStyles.productCarouselStyle
    val pagerState = rememberPagerState(pageCount = { elements.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Use a HorizontalPager to show the elements in a carousel
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(style.imageWidth),
            // Use the image width for the end padding to allow the page indicator to scroll to
            // the last recommendation in the carousel
            contentPadding = PaddingValues(
                start = style.horizontalPadding,
                end = style.imageWidth,
                top = style.verticalPadding,
                bottom = style.verticalPadding
            ),
            pageSpacing = style.itemSpacing,
            beyondBoundsPageCount = 1,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            ProductImage(
                element = elements[page],
                modifier = Modifier
                    .width(style.imageWidth)
                    .height(style.imageHeight),
                onImageClick = onImageClick,
                isMultiElement = true
            )
        }

        // Carousel switcher controls
        CarouselSwitcher(
            currentPage = pagerState.settledPage,
            totalPages = elements.size,
            onPreviousClick = {
                if (pagerState.settledPage > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.settledPage - 1)
                    }
                }
            },
            onNextClick = {
                if (pagerState.settledPage < elements.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.settledPage + 1)
                    }
                }
            },
            onPageClick = { page ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(page)
                }
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
                contentDescription = "Previous page",
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

        Box(modifier = Modifier.width(style.navigationSpacing))

        // Next button
        IconButton(
            onClick = onNextClick,
            enabled = currentPage < totalPages - 1
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next page",
                tint = if (currentPage < totalPages - 1) {
                    style.navigationIconActiveColor
                } else {
                    style.navigationIconInactiveColor
                }
            )
        }
    }
}
