/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CardElementDictTest {

    @Test
    fun `keeps only known product keys and drops unknown ones`() {
        val content = mapOf(
            "productName" to "Widget",
            "productDescription" to "A widget",
            "productPageURL" to "https://example.com/w",
            "productPrice" to "\$9.99",
            "productBadge" to "New",
            "ignored" to "nope"
        )

        val dict = buildCardElementDict(content)

        assertEquals(
            mapOf(
                "productName" to "Widget",
                "productDescription" to "A widget",
                "productPageURL" to "https://example.com/w",
                "productPrice" to "\$9.99",
                "productBadge" to "New"
            ),
            dict
        )
    }

    @Test
    fun `omits absent keys`() {
        assertEquals(
            mapOf("productName" to "Widget"),
            buildCardElementDict(mapOf("productName" to "Widget"))
        )
    }
}
