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

package com.adobe.marketing.mobile.concierge.ui.components.card

import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Unit tests for [productDetailCtas]. */
class ExtendedProductCardLogicTest {

    @Test
    fun `productDetailCtas returns only primary when secondary is absent`() {
        val element = MultimodalElement(
            id = "prod-1",
            content = mapOf(
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout"
            )
        )

        val ctas = productDetailCtas(element)

        assertEquals(1, ctas.size)
        assertEquals(ProductCardCtaRole.PRIMARY, ctas[0].role)
        assertEquals("Buy now", ctas[0].button.text)
        assertEquals("https://example.com/checkout", ctas[0].button.url)
    }

    @Test
    fun `productDetailCtas returns primary then secondary when both are valid`() {
        val element = MultimodalElement(
            id = "prod-2",
            content = mapOf(
                "primaryText" to "Buy now",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/details"
            )
        )

        val ctas = productDetailCtas(element)

        assertEquals(2, ctas.size)
        assertEquals(ProductCardCtaRole.PRIMARY, ctas[0].role)
        assertEquals("Buy now", ctas[0].button.text)
        assertEquals(ProductCardCtaRole.SECONDARY, ctas[1].role)
        assertEquals("Learn more", ctas[1].button.text)
    }

    @Test
    fun `productDetailCtas returns secondary only when primary has no url`() {
        val element = MultimodalElement(
            id = "prod-3",
            content = mapOf(
                "primaryText" to "Buy now",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/details"
            )
        )

        val ctas = productDetailCtas(element)

        assertEquals(1, ctas.size)
        assertEquals(ProductCardCtaRole.SECONDARY, ctas[0].role)
        assertEquals("Learn more", ctas[0].button.text)
    }

    @Test
    fun `productDetailCtas drops an action when text is blank`() {
        val element = MultimodalElement(
            id = "prod-4",
            content = mapOf(
                "primaryText" to "   ",
                "primaryUrl" to "https://example.com/checkout",
                "secondaryText" to "Learn more",
                "secondaryUrl" to "https://example.com/details"
            )
        )

        val ctas = productDetailCtas(element)

        assertEquals(1, ctas.size)
        assertEquals(ProductCardCtaRole.SECONDARY, ctas[0].role)
    }

    @Test
    fun `productDetailCtas drops an action when url is null, blank, or whitespace`() {
        val noUrl = MultimodalElement(id = "prod-5a", content = mapOf("primaryText" to "Buy now"))
        val blankUrl = MultimodalElement(
            id = "prod-5b",
            content = mapOf("primaryText" to "Buy now", "primaryUrl" to "")
        )
        val whitespaceUrl = MultimodalElement(
            id = "prod-5c",
            content = mapOf("primaryText" to "Buy now", "primaryUrl" to "   ")
        )

        assertTrue(productDetailCtas(noUrl).isEmpty())
        assertTrue(productDetailCtas(blankUrl).isEmpty())
        assertTrue(productDetailCtas(whitespaceUrl).isEmpty())
    }

    @Test
    fun `productDetailCtas returns empty list when neither action is valid`() {
        val element = MultimodalElement(id = "prod-6", content = emptyMap())

        assertTrue(productDetailCtas(element).isEmpty())
    }

    @Test
    fun `productDetailCtas trims whitespace-padded text and url and stores the trimmed values`() {
        val element = MultimodalElement(
            id = "prod-7",
            content = mapOf(
                "primaryText" to "  Buy now  ",
                "primaryUrl" to "  https://example.com/checkout  "
            )
        )

        val ctas = productDetailCtas(element)

        assertEquals(1, ctas.size)
        assertEquals("Buy now", ctas[0].button.text)
        assertEquals("https://example.com/checkout", ctas[0].button.url)
    }
}
