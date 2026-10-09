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

package com.adobe.marketing.mobile.conciergetestapp.fnb

import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.activeCategoryIndex
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveCategoryIndexTest {

    // Categories: A (header 0, rows 1-3), B (header 4, rows 5-6), C (header 7, row 8)
    private val headers = listOf(0, 4, 7)

    @Test
    fun `follows the last header at or above the first visible item`() {
        assertEquals(0, activeCategoryIndex(headers, 0, (0..3).toList(), atEnd = false))
        assertEquals(0, activeCategoryIndex(headers, 3, (3..6).toList(), atEnd = false))
        assertEquals(1, activeCategoryIndex(headers, 4, (4..7).toList(), atEnd = false))
    }

    @Test
    fun `at the end of the list the last visible header wins so short trailing categories can be highlighted`() {
        // The list bottoms out with B's rows at the top; C's header can never reach the top.
        assertEquals(2, activeCategoryIndex(headers, 5, (5..8).toList(), atEnd = true))
    }

    @Test
    fun `at the end with no header visible it falls back to scroll position`() {
        assertEquals(1, activeCategoryIndex(listOf(0, 4), 6, (6..9).toList(), atEnd = true))
    }

    @Test
    fun `empty menu selects the first tab`() {
        assertEquals(0, activeCategoryIndex(emptyList(), 0, emptyList(), atEnd = false))
    }
}
