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

/**
 * Data class representing a product card with all necessary information for display
 */
data class ProductCardData(
    val id: String,
    val title: String,
    val description: String? = null,
    val price: String? = null,
    val originalPrice: String? = null,
    val imageUrl: String? = null,
    val brand: String? = null,
    val rating: Float? = null,
    val reviewCount: Int? = null,
    val availability: String? = null,
    val actionButtons: List<ProductActionButton> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data class representing an action button on a product card
 */
data class ProductActionButton(
    val id: String,
    val text: String,
    val type: ActionButtonType,
    val url: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing different types of action buttons
 */
enum class ActionButtonType {
    ADD_TO_CART,
    VIEW_DETAILS,
    ADD_TO_WISHLIST,
    SHARE,
    CUSTOM
}

/**
 * Data class representing a collection of product cards (carousel)
 */
internal data class ProductCarouselData(
    val id: String,
    val title: String? = null,
    val products: List<ProductCardData>,
    val layout: CarouselLayout = CarouselLayout.HORIZONTAL,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing different carousel layouts
 */
internal enum class CarouselLayout {
    HORIZONTAL,
    VERTICAL,
    GRID
}
