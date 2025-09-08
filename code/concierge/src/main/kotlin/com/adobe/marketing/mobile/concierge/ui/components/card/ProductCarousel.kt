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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Composable that displays a carousel of product cards with different layout options.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProductCarousel(
    carousel: ProductCarouselData,
    modifier: Modifier = Modifier,
    onProductClick: (ProductCardData) -> Unit = {},
    onActionClick: (ProductActionButton) -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Carousel Title
        if (carousel.title != null) {
            Text(
                text = carousel.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Product Cards based on layout
        when (carousel.layout) {
            CarouselLayout.HORIZONTAL -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(carousel.products) { product ->
                        ProductCard(
                            product = product,
                            modifier = Modifier.width(280.dp),
                            onCardClick = onProductClick,
                            onActionClick = onActionClick
                        )
                    }
                }
            }
            
            CarouselLayout.VERTICAL -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(carousel.products) { product ->
                        ProductCard(
                            product = product,
                            onCardClick = onProductClick,
                            onActionClick = onActionClick
                        )
                    }
                }
            }
            
            CarouselLayout.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(carousel.products) { product ->
                        ProductCard(
                            product = product,
                            onCardClick = onProductClick,
                            onActionClick = onActionClick
                        )
                    }
                }
            }
        }
    }
}
