/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.conciergetestapp

import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuCategory
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.OptionChoice
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.OptionGroup

/** In-memory UI models that drive the pure composables without any payload mapping. */
internal object FnbSampleData {

    val burger = MenuItem(
        id = "item-burger",
        name = "Build Your Burger",
        description = "Half-pound patty on a brioche bun.",
        metadata = listOf("Beef", "Contains gluten"),
        priceCents = 1550,
        tag = "Popular",
        optionGroups = listOf(
            OptionGroup(
                id = "g-temp",
                title = "Temperature",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    OptionChoice("m-rare", "Medium rare"),
                    OptionChoice("m-med", "Medium"),
                    OptionChoice("m-well", "Well done")
                )
            ),
            OptionGroup(
                id = "g-cheese",
                title = "Cheese",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    OptionChoice("m-cheddar", "Cheddar", isDefault = true),
                    OptionChoice("m-swiss", "Swiss", priceDeltaCents = 50)
                )
            ),
            OptionGroup(
                id = "g-toppings",
                title = "Toppings",
                required = false,
                minSelect = 0,
                maxSelect = 2,
                options = listOf(
                    OptionChoice("m-bacon", "Bacon", priceDeltaCents = 200),
                    OptionChoice("m-avocado", "Avocado", priceDeltaCents = 150),
                    OptionChoice("m-onion", "Grilled onion")
                )
            )
        )
    )

    /** Mirrors the Figma "Customize — resting state (Required + Optional)" frame. */
    val nachos = MenuItem(
        id = "item-nachos",
        name = "Loaded Nachos",
        description = "Crispy tortilla chips smothered in warm cheese sauce, black beans, pico de gallo, and sour cream.",
        metadata = listOf("Vegetarian", "Serves 1–2"),
        priceCents = 1400,
        wasPriceCents = 1600,
        imageUrl = "https://storage.tapin2.co/images/t1/veggie-nachos.jpeg",
        tag = "Popular",
        optionGroups = listOf(
            OptionGroup(
                id = "g-nacho-size",
                title = "Size",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    OptionChoice("m-nacho-regular", "Regular", isDefault = true),
                    OptionChoice("m-nacho-large", "Large", priceDeltaCents = 150),
                    OptionChoice("m-nacho-sharing", "Sharing", priceDeltaCents = 300)
                )
            ),
            OptionGroup(
                id = "g-nacho-toppings",
                title = "Toppings",
                required = false,
                minSelect = 0,
                maxSelect = 3,
                options = listOf(
                    OptionChoice("m-jalapenos", "Extra jalapeños", isDefault = true),
                    OptionChoice("m-queso", "Extra queso"),
                    OptionChoice("m-guac", "Guacamole", priceDeltaCents = 200)
                )
            )
        )
    )

    private val fries = MenuItem(
        id = "item-fries",
        name = "Garlic Fries",
        metadata = listOf("Vegan"),
        priceCents = 700,
        optionGroups = listOf(
            OptionGroup(
                id = "g-size",
                title = "Size",
                required = true,
                minSelect = 1,
                maxSelect = 1,
                options = listOf(
                    OptionChoice("m-regular", "Regular", isDefault = true),
                    OptionChoice("m-large", "Large", priceDeltaCents = 200)
                )
            )
        )
    )

    val menu = MenuUiModel(
        locationId = "19289",
        locationName = "Market Cafe 122",
        orderingAvailable = true,
        categories = listOf(
            MenuCategory(
                id = "c-mains",
                label = "Mains",
                items = listOf(
                    burger,
                    fries,
                    MenuItem(id = "item-dog", name = "Classic Hot Dog", priceCents = 900, metadata = listOf("Beef")),
                    MenuItem(id = "item-pretzel", name = "Soft Pretzel", priceCents = 650, wasPriceCents = 800, tag = "Deal"),
                    nachos
                )
            ),
            MenuCategory(
                id = "c-drinks",
                label = "Drinks",
                items = listOf(
                    MenuItem(id = "item-soda", name = "Fountain Soda", priceCents = 500),
                    MenuItem(id = "item-lager", name = "Draft Lager", priceCents = 1425, isAlcohol = true, metadata = listOf("16 oz"))
                )
            ),
            MenuCategory(
                id = "c-dessert",
                label = "Dessert",
                items = listOf(
                    MenuItem(id = "item-churro", name = "Churro Bites", priceCents = 750, metadata = listOf("Cinnamon", "Sugar")),
                    MenuItem(id = "item-cookie", name = "Chocolate Chip Cookie", priceCents = 800)
                )
            )
        )
    )
}
