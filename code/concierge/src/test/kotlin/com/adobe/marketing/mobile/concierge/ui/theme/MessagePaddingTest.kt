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

import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessagePaddingTest {

    @Test
    fun `CSS shorthand resolves all physical edges in either direction`() {
        val cases = listOf(
            "8px" to listOf(8.0, 8.0, 8.0, 8.0),
            "8px 16px" to listOf(8.0, 16.0, 8.0, 16.0),
            "8px 16px 4px" to listOf(8.0, 16.0, 4.0, 16.0),
            "8px 16px 4px 20px" to listOf(8.0, 16.0, 4.0, 20.0),
            "0px" to listOf(0.0, 0.0, 0.0, 0.0),
            "0" to listOf(0.0, 0.0, 0.0, 0.0),
            "  8.5px\t16.25px  " to listOf(8.5, 16.25, 8.5, 16.25)
        )
        cases.forEach { (css, expected) ->
            val tokens = CSSKeyMapper.apply("--message-padding", css, ConciergeThemeTokens())
            assertEquals(expected, tokens.cssLayout?.messagePadding)
            val padding = requireNotNull(resolveMessagePadding(tokens.cssLayout?.messagePadding))
            LayoutDirection.values().forEach { direction ->
                assertEquals(expected[0].dp, padding.calculateTopPadding())
                assertEquals(expected[1].dp, padding.calculateRightPadding(direction))
                assertEquals(expected[2].dp, padding.calculateBottomPadding())
                assertEquals(expected[3].dp, padding.calculateLeftPadding(direction))
            }
        }
    }

    @Test
    fun `programmatic shorthand and missing padding are supported`() {
        assertNull(resolveMessagePadding(null))
        (1..4).forEach { size ->
            val values = listOf(8.0, 16.0, 4.0, 20.0).take(size)
            val css = values.joinToString(" ") { "${it}px" }
            assertEquals(
                resolveMessagePadding(CSSValueConverter.parsePadding(css)),
                resolveMessagePadding(values)
            )
        }
    }

    @Test
    fun `invalid CSS preserves existing theme rather than applying partial padding`() {
        val theme = CSSKeyMapper.apply("--message-padding", "8px 16px", ConciergeThemeTokens())
        listOf(
            "", "invalid", "8px invalid", "8px 2em", "-1px", "8px -2px",
            "NaN", "Infinity", "1e100px", "1px 2px 3px 4px 5px"
        ).forEach { css ->
            assertNull(css, CSSValueConverter.parsePadding(css))
            assertEquals(css, theme, CSSKeyMapper.apply("--message-padding", css, theme))
            assertNull(CSSKeyMapper.apply("--message-padding", css, ConciergeThemeTokens()).cssLayout?.messagePadding)
        }
    }

    @Test
    fun `invalid programmatic padding falls back`() {
        listOf(
            emptyList(), listOf(-1.0), listOf(Double.NaN), listOf(Double.POSITIVE_INFINITY),
            listOf(Double.MAX_VALUE), List(5) { 8.0 }
        ).forEach { assertNull(resolveMessagePadding(it)) }
    }
}
