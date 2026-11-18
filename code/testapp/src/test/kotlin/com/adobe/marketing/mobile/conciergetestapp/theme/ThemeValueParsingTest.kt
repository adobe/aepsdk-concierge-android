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

package com.adobe.marketing.mobile.conciergetestapp.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeValueParsingTest {

    @Test
    fun `parseColor supports css hex forms and transparent`() {
        assertEquals(Color(0xFFFF0000), ThemeValueParsing.parseColor("#F00"))
        assertEquals(Color(0xFF112233), ThemeValueParsing.parseColor(" #112233 "))
        assertEquals(Color(0x80112233), ThemeValueParsing.parseColor("#11223380"))
        assertEquals(Color.Transparent, ThemeValueParsing.parseColor("TRANSPARENT"))
        assertNull(ThemeValueParsing.parseColor("#12345"))
        assertNull(ThemeValueParsing.parseColor("#GGGGGG"))
        assertNull(ThemeValueParsing.parseColor("red"))
        assertNull(ThemeValueParsing.parseColor(null))
    }

    @Test
    fun `parseColor supports rgb and rgba like the SDK`() {
        assertEquals(Color(0xFF112233), ThemeValueParsing.parseColor("rgb(17, 34, 51)"))
        assertEquals(Color(17, 34, 51, 127), ThemeValueParsing.parseColor("rgba(17,34,51,0.5)"))
        assertEquals(Color(0x00112233), ThemeValueParsing.parseColor("RGBA(17, 34, 51, 0)"))
        assertEquals(Color(0xFF112233), ThemeValueParsing.parseColor("rgba(17, 34, 51, 1)"))
    }

    @Test
    fun `parseColor rejects malformed or out of range rgb`() {
        for (raw in listOf("rgb(256, 0, 0)", "rgba(0, 0, 0, 1.5)", "rgb(0, 0)", "rgb(0, 0, 0", "rgb(-1, 0, 0)", "rgb(a, b, c)")) {
            assertNull(raw, ThemeValueParsing.parseColor(raw))
        }
    }

    @Test
    fun `parseDp accepts px, dp and bare numbers within bounds`() {
        assertEquals(12f, ThemeValueParsing.parseDp("12px"))
        assertEquals(12f, ThemeValueParsing.parseDp(" 12PX "))
        assertEquals(8.5f, ThemeValueParsing.parseDp("8.5dp"))
        assertEquals(4f, ThemeValueParsing.parseDp("4"))
        assertEquals(140f, ThemeValueParsing.parseDp("140.0"))
        assertEquals(ThemeValueParsing.MAX_DP, ThemeValueParsing.parseDp("1000"))
        assertNull(ThemeValueParsing.parseDp(null))
        assertNull(ThemeValueParsing.parseDp("-1px"))
        assertNull(ThemeValueParsing.parseDp("1001"))
        assertNull(ThemeValueParsing.parseDp("1e9px"))
        assertNull(ThemeValueParsing.parseDp("wide"))
    }

    @Test
    fun `parseDp rejects values that aren't finite`() {
        for (raw in listOf("NaN", "Infinity", "-Infinity", "Infinitypx")) {
            assertNull(raw, ThemeValueParsing.parseDp(raw))
        }
    }
}
