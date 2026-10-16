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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoMultimodalCardsTest {

    @Test
    fun `location productCards parse through the production parser into card content`() {
        val json = """
            {"elements": [
              {"id": "location_19289", "entityId": "19289", "type": "productCard",
               "entity_info": {"productName": "Market Cafe 122", "productDescription": "Mexican cuisine and drinks.",
                               "productBadge": "Section 122",
                               "productPageURL": "conciergetestapp://fnb/location?locationId=19289&title=Market%20Cafe%20122",
                               "primary": {"text": "View menu", "url": "conciergetestapp://fnb/location?locationId=19289&title=Market%20Cafe%20122"}}},
              {"id": "location_19293", "entityId": "19293", "type": "productCard",
               "entity_info": {"productName": "Sweet Spot 126", "productDescription": "Churros and frozen treats.", "productBadge": "Paused until 8:15 PM"}}
            ]}
        """.trimIndent()

        val cards = parseDemoMultimodalCards(json)

        assertEquals(listOf("location_19289", "location_19293"), cards.map { it.id })
        val open = cards[0].content
        assertEquals("Market Cafe 122", open["productName"])
        assertEquals("Mexican cuisine and drinks.", open["productDescription"])
        assertEquals("Section 122", open["productBadge"])
        assertEquals("conciergetestapp://fnb/location?locationId=19289&title=Market%20Cafe%20122", open["productPageURL"])
        assertEquals("View menu", open["primaryText"])
        assertEquals(open["productPageURL"], open["primaryUrl"])
        assertNull("paused stands carry no link", cards[1].content["productPageURL"])
        assertNull("no image: the card shows its placeholder", cards[1].url)
    }

    @Test
    fun `malformed input yields no cards`() {
        assertTrue(parseDemoMultimodalCards("not json").isEmpty())
        assertTrue(parseDemoMultimodalCards("{}").isEmpty())
    }
}
