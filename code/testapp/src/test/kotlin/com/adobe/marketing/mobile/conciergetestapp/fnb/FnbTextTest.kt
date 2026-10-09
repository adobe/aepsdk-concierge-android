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

import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.FnbText
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbThemeParsing
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FnbTextTest {

    @Test
    fun `stripHtml removes tags, script and style content, and decodes entities`() {
        assertEquals(
            "Hot & fresh <3 café",
            FnbText.stripHtml("<p>Hot &amp; fresh</p><script>alert('x')</script><style>p{}</style> &lt;3 caf&#233;")
        )
        assertEquals("Line one Line two", FnbText.stripHtml("Line one<br/>Line two"))
        assertEquals("a b", FnbText.stripHtml("a&nbsp;&#x20;b"))
    }

    @Test
    fun `stripHtml leaves unknown or malformed entities and drops unterminated tags`() {
        assertEquals("&bogus; & x", FnbText.stripHtml("&bogus; & x"))
        assertEquals("keep", FnbText.stripHtml("keep<unterminated"))
        assertEquals("&#1114112;", FnbText.stripHtml("&#1114112;"))
    }

    @Test
    fun `stripHtml is linear on pathological input and clamps length`() {
        val pathological = "&".repeat(200_000) + "<" + "a".repeat(10)
        val result = FnbText.stripHtml(pathological, maxLength = 50)
        assertEquals(51, result.length)
        assertEquals("", FnbText.stripHtml(null))
    }

    @Test
    fun `sanitizeInline collapses whitespace and control characters`() {
        assertEquals("a b c", FnbText.sanitizeInline("  a\n\tb\u0000\u00A0 c  "))
        assertEquals("abc…", FnbText.sanitizeInline("abcdef", maxLength = 3))
    }

    @Test
    fun `httpsUrlOrNull accepts only absolute https urls`() {
        assertEquals("https://x.test/a.png", FnbText.httpsUrlOrNull(" https://x.test/a.png "))
        assertNull(FnbText.httpsUrlOrNull("http://x.test/a.png"))
        assertNull(FnbText.httpsUrlOrNull("javascript:alert(1)"))
        assertNull(FnbText.httpsUrlOrNull("https://"))
        assertNull(FnbText.httpsUrlOrNull(""))
        assertNull(FnbText.httpsUrlOrNull(null))
    }

    @Test
    fun `toCents handles every org_json number shape without float drift`() {
        assertEquals(800L, FnbText.toCents(8))
        assertEquals(800L, FnbText.toCents(8L))
        assertEquals(850L, FnbText.toCents(8.5))
        assertEquals(30L, FnbText.toCents(0.1 + 0.2))
        assertEquals(1999L, FnbText.toCents("19.99"))
        assertEquals(1000L, FnbText.toCents(BigDecimal("9.995")))
        assertNull(FnbText.toCents(Double.NaN))
        assertNull(FnbText.toCents("abc"))
        assertNull(FnbText.toCents(null))
        assertNull(FnbText.toCents(true))
    }
}

class FnbThemeParsingTest {

    @Test
    fun `parseColor supports css hex forms and transparent`() {
        assertEquals(Color(0xFFFF0000), FnbThemeParsing.parseColor("#F00"))
        assertEquals(Color(0xFF112233), FnbThemeParsing.parseColor(" #112233 "))
        assertEquals(Color(0x80112233), FnbThemeParsing.parseColor("#11223380"))
        assertEquals(Color.Transparent, FnbThemeParsing.parseColor("transparent"))
        assertNull(FnbThemeParsing.parseColor("#12345"))
        assertNull(FnbThemeParsing.parseColor("#GGGGGG"))
        assertNull(FnbThemeParsing.parseColor("red"))
    }

    @Test
    fun `parseDp accepts px, dp and bare numbers within bounds`() {
        assertEquals(12f, FnbThemeParsing.parseDp("12px"))
        assertEquals(8.5f, FnbThemeParsing.parseDp("8.5dp"))
        assertEquals(4f, FnbThemeParsing.parseDp("4"))
        assertNull(FnbThemeParsing.parseDp("-1px"))
        assertNull(FnbThemeParsing.parseDp("99999"))
        assertNull(FnbThemeParsing.parseDp("wide"))
    }

    @Test
    fun `parseFraction clamps to the supported card height range`() {
        assertEquals(0.5f, FnbThemeParsing.parseFraction("0.5"))
        assertEquals(0.9f, FnbThemeParsing.parseFraction("2"))
        assertEquals(0.3f, FnbThemeParsing.parseFraction("0"))
        assertNull(FnbThemeParsing.parseFraction("tall"))
    }
}
