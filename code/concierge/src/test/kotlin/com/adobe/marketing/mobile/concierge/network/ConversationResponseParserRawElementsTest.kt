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

package com.adobe.marketing.mobile.concierge.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationResponseParserRawElementsTest {

    private fun parse(elementsJson: String): ParsedConversationMessage = ConversationResponseParser.parseConversationData(
        """
        {"handle": [{"type": "brand-concierge:conversation", "payload": [{
          "state": "completed",
          "response": {"message": "", "multimodalElements": {"elements": $elementsJson}}
        }]}]}
        """.trimIndent()
    ).single()

    @Test
    fun `every element is kept whole, in order, with unknown fields and nulls`() {
        val message = parse(
            """
            [
              {"type": "acme.menu", "id": "menu-1", "entity_info": [{"product": {"id": 1364015, "eventPrice": 5.0, "note": null}}],
               "custom": {"nested": [true, null, 2]}},
              {"type": "productCard", "id": "p1", "entity_info": {"productName": "Tee"}},
              {"type": "ctaButton", "entity_info": {"primary": {"text": "Go", "url": "https://example.com"}}}
            ]
            """.trimIndent()
        )

        assertEquals(listOf(0, 1, 2), message.rawElements.map { it.position })
        assertEquals(listOf("acme.menu", "productCard", "ctaButton"), message.rawElements.map { it.type })

        val menu = message.rawElements[0].payload
        val product = ((menu["entity_info"] as List<*>)[0] as Map<*, *>)["product"] as Map<*, *>
        assertEquals(1364015, product["id"])
        assertEquals(5.0, product["eventPrice"])
        assertTrue(product.containsKey("note"))
        assertNull(product["note"])
        assertEquals(listOf(true, null, 2), (menu["custom"] as Map<*, *>)["nested"])

        assertTrue(message.rawElements[0].parsedBuiltIn is ParsedMultimodalItem.Card)
        assertEquals(message.rawElements[0].parsedBuiltIn, message.rawElements[0].builtInFallback)
        assertTrue(message.rawElements[2].parsedBuiltIn is ParsedMultimodalItem.Cta)
        assertEquals(message.rawElements.mapNotNull { it.parsedBuiltIn }, message.orderedElements)
        assertEquals(listOf("menu-1", "p1"), message.multimodalElements.map { it.id })
    }

    @Test
    fun `horizontal cards keep built-in content and response order`() {
        val message = parse(
            """
            [
              {"type": "horizontalCard", "id": "quote-1", "entity_info": {
                "productName": "Practice Facility Garage",
                "productDescription": "${'$'}0.00 · 2.3 min walk · Covered, Guaranteed spot",
                "productImageURL": "https://example.com/garage.jpeg",
                "primary": {"text": "Book on ParkWhiz", "url": "https://example.com/checkout"},
                "secondary": {"text": "Get directions", "url": "https://example.com/directions"}
              }},
              {"type": "acme.other", "id": "other"},
              {"type": "horizontalCard", "id": "quote-2", "entity_info": {"productName": "Second garage"}}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("quote-1", "other", "quote-2"), message.multimodalElements.map { it.id })
        assertEquals(message.rawElements.map { it.parsedBuiltIn }, message.orderedElements)
        val content = message.multimodalElements.first().content
        assertEquals("Practice Facility Garage", content["productName"])
        assertEquals("${'$'}0.00 · 2.3 min walk · Covered, Guaranteed spot", content["productDescription"])
        assertEquals("https://example.com/garage.jpeg", content["productImageURL"])
        assertEquals("Book on ParkWhiz", content["primaryText"])
        assertEquals("https://example.com/checkout", content["primaryUrl"])
        assertEquals("Get directions", content["secondaryText"])
        assertEquals("https://example.com/directions", content["secondaryUrl"])
        assertTrue(message.rawElements[1].builtInFallback is ParsedMultimodalItem.Card)
    }

    @Test
    fun `untyped and custom types keep legacy built-in rendering`() {
        val message = parse(
            """
            [
              {"id": "legacy", "entity_info": {"productName": "Legacy"}},
              {"type": "acme.other", "id": "other", "entity_info": {"productName": "Other"}}
            ]
            """.trimIndent()
        )

        assertEquals(listOf("legacy", "other"), message.multimodalElements.map { it.id })
        assertEquals(2, message.orderedElements.size)
        assertEquals(2, message.rawElements.size)
    }

    @Test
    fun `elements without a built-in representation are kept for native dispatch only`() {
        val message = parse(
            """
            [
              {"type": "acme.cart", "entity_info": {"id": 695685, "items": []}},
              {"type": "ctaButton", "id": "c1", "entity_info": {"primary": {"text": "No url"}}},
              {"entity_info": {"productName": "untyped, no id"}}
            ]
            """.trimIndent()
        )

        assertEquals(3, message.rawElements.size)
        assertTrue(message.rawElements.all { it.parsedBuiltIn == null })
        assertTrue(message.orderedElements.isEmpty())
        assertTrue(message.multimodalElements.isEmpty())
        assertEquals("acme.cart", message.rawElements[0].type)
        assertNull(message.rawElements[2].type)
        assertEquals(695685, (message.rawElements[0].payload["entity_info"] as Map<*, *>)["id"])
    }
}
