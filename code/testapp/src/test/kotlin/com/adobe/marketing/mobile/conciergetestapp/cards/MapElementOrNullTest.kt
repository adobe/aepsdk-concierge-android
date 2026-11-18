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

package com.adobe.marketing.mobile.conciergetestapp.cards

import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapElementOrNullTest {
    private val element = mapOf("type" to "kings.fnbcart", "id" to "e1")

    @Test
    fun `a mapper that throws gives no model instead of crashing the renderer`() {
        assertNull(mapElementOrNull<Any>(element) { throw IllegalStateException("boom") })
        assertNull(mapElementOrNull<Any>(element) { throw ClassCastException("bad payload") })
        assertNull(mapElementOrNull<Any>(element) { error("nested") })
    }

    @Test
    fun `a mapper returning null gives no model and a value passes through`() {
        assertNull(mapElementOrNull<String>(element) { null })
        assertEquals("ok", mapElementOrNull(element) { "ok" })
    }

    @Test
    fun `garbage payloads reach the unavailable card, not an exception`() {
        listOf<Any?>(null, 42, "text", listOf(1), mapOf("result" to "nope")).forEach { payload ->
            assertNull(mapElementOrNull(element) { CartSummaryMapper.map(payload, CartOptions()) })
        }
    }
}
