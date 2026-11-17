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

package com.adobe.marketing.mobile.concierge.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageTypographyValidatorTest {
    @Test
    fun `invalid line heights are ignored and valid absolute and relative values are preserved`() {
        for (value in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 1e100, 1e-100)) {
            val typography = ConciergeTypography(lineHeight = value, lineHeightPx = value, fontSize = 18.0)
            assertEquals(
                typography.copy(lineHeight = null, lineHeightPx = null),
                MessageTypographyValidator.validate(typography)
            )
        }
        val typography = ConciergeTypography(lineHeight = 1.5, lineHeightPx = 24.0)
        assertEquals(typography, MessageTypographyValidator.validate(typography))
    }

    @Test
    fun `valid typography and unrelated settings are preserved`() {
        for (weight in listOf(null, 1, 400, 700, 1000)) {
            val typography = ConciergeTypography(
                fontFamily = "brand-font",
                lineHeight = 1.5,
                fontSize = 17.5,
                fontWeight = weight,
                linkFontWeight = weight,
                linkUnderline = false
            )
            assertEquals(typography, MessageTypographyValidator.validate(typography))
        }
        assertEquals(ConciergeTypography(), MessageTypographyValidator.validate(ConciergeTypography()))
    }

    @Test
    fun `invalid body weights do not remove valid link settings`() {
        for (weight in listOf(Int.MIN_VALUE, -1, 0, 1001, Int.MAX_VALUE)) {
            val typography = ConciergeTypography(fontSize = 18.0, fontWeight = weight, linkFontWeight = 800)
            assertEquals(
                typography.copy(fontWeight = null),
                MessageTypographyValidator.validate(typography)
            )
        }
    }

    @Test
    fun `invalid link weights do not remove valid body settings`() {
        for (weight in listOf(Int.MIN_VALUE, -1, 0, 1001, Int.MAX_VALUE)) {
            val typography = ConciergeTypography(fontSize = 18.0, fontWeight = 600, linkFontWeight = weight)
            assertEquals(
                typography.copy(linkFontWeight = null),
                MessageTypographyValidator.validate(typography)
            )
        }
    }

    @Test
    fun `invalid sizes are ignored without changing other settings`() {
        for (size in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1e100, 1e-100)) {
            val typography = ConciergeTypography(fontSize = size, fontWeight = 600, linkUnderline = false)
            val validated = MessageTypographyValidator.validate(typography)
            assertNull(validated.fontSize)
            assertEquals(typography.copy(fontSize = null), validated)
        }
    }
}
