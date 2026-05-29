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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.theme.CarouselStyle
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/**
 * Composable that displays a carousel of product items with navigation controls.
 * When [useExtendedProductCards] is true, shows extended product cards
 * (image, badge, name, subtitle, price); otherwise shows image-only tiles.
 */
@Composable
internal fun ProductCarousel(
    elements: List<MultimodalElement>,
    onImageClick: (MultimodalElement) -> Unit,
    useExtendedProductCards: Boolean = false,
    leadingInset: Dp = 0.dp
) {
    val style = ConciergeStyles.productCarouselStyle
    val extendedProductCardStyle = ConciergeStyles.extendedProductCardStyle
    val itemWidth = if (useExtendedProductCards) extendedProductCardStyle.cardWidth else style.imageWidth
    val carouselMode = ConciergeTheme.behavior?.multimodalCarousel?.carouselStyle ?: CarouselStyle.PAGED
    val isPaged = carouselMode == CarouselStyle.PAGED

    if (useExtendedProductCards) {
        // Extended cards have dynamic heights; equalize every card to the tallest one so the
        // carousel has a single, consistent height with minimal blank space.
        EqualHeightCarousel(
            itemCount = elements.size,
            itemWidth = itemWidth,
            minHeight = extendedProductCardStyle.cardMinHeight,
            maxHeight = extendedProductCardStyle.cardMaxHeight,
            measureItem = { index ->
                ExtendedProductCard(element = elements[index], measureOnly = true)
            }
        ) { cardHeight ->
            CarouselContent(
                elements = elements,
                isPaged = isPaged,
                leadingInset = leadingInset,
                itemWidth = itemWidth,
                style = style
            ) { index ->
                ExtendedProductCard(
                    element = elements[index],
                    modifier = Modifier.width(itemWidth).height(cardHeight),
                    onCardClick = onImageClick
                )
            }
        }
    } else {
        CarouselContent(
            elements = elements,
            isPaged = isPaged,
            leadingInset = leadingInset,
            itemWidth = itemWidth,
            style = style
        ) { index ->
            ProductImage(
                element = elements[index],
                modifier = Modifier
                    .width(itemWidth)
                    .height(style.imageHeight),
                onImageClick = onImageClick,
                isMultiElement = true
            )
        }
    }
}

/**
 * Renders the scrollable row of carousel items plus, in paged mode, the navigation controls.
 * [item] supplies the composable for the element at a given index.
 */
@Composable
private fun CarouselContent(
    elements: List<MultimodalElement>,
    isPaged: Boolean,
    leadingInset: Dp,
    itemWidth: Dp,
    style: ConciergeStyles.ProductCarouselStyle,
    item: @Composable (index: Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val currentPage = listState.firstVisibleItemIndex

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(
                start = leadingInset,
                end = if (isPaged) itemWidth else style.trailingContentPadding,
                top = style.verticalPadding,
                bottom = style.verticalPadding
            ),
            horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(elements.size) { index ->
                item(index)
            }
        }

        if (isPaged) {
            CarouselSwitcher(
                currentPage = currentPage,
                totalPages = elements.size,
                onPreviousClick = {
                    val page = listState.firstVisibleItemIndex
                    if (page > 0) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(page - 1)
                        }
                    }
                },
                onNextClick = {
                    val page = listState.firstVisibleItemIndex
                    if (page < elements.size - 1) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(page + 1)
                        }
                    }
                },
                onPageClick = { page ->
                    coroutineScope.launch {
                        listState.animateScrollToItem(page)
                    }
                }
            )
        }
    }
}

/**
 * Layout that measures the natural (clamped) height of every carousel card, takes the tallest,
 * and renders [content] with that single height so all cards match. [measureItem] is composed
 * only to measure intrinsic heights — it should skip expensive work like network image loads.
 */
@Composable
private fun EqualHeightCarousel(
    itemCount: Int,
    itemWidth: Dp,
    minHeight: Dp,
    maxHeight: Dp,
    measureItem: @Composable (index: Int) -> Unit,
    content: @Composable (cardHeight: Dp) -> Unit
) {
    SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
        val itemWidthPx = itemWidth.roundToPx()
        val minPx = minHeight.roundToPx()
        val maxPx = maxHeight.roundToPx()

        // Pass 1: measure-only composition to find the tallest clamped card height.
        val resolvedPx = subcompose(CarouselSlot.Measure) {
            repeat(itemCount) { measureItem(it) }
        }.maxOfOrNull { it.maxIntrinsicHeight(itemWidthPx) }
            ?.coerceIn(minPx, maxPx)
            ?: minPx
        val cardHeight = resolvedPx.toDp()

        // Pass 2: render the real carousel with every card fixed to that height.
        val placeables = subcompose(CarouselSlot.Content) {
            content(cardHeight)
        }.map { it.measure(constraints) }

        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}

private enum class CarouselSlot { Measure, Content }

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
