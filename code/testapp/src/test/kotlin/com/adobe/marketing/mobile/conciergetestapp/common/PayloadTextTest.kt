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

package com.adobe.marketing.mobile.conciergetestapp.common

import androidx.compose.ui.graphics.Color
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbThemeKeys
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbThemeParsing
import com.adobe.marketing.mobile.conciergetestapp.theme.ThemeValueParsing
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadTextTest {

    @Test
    fun `stripHtml removes tags, script and style content, and decodes entities`() {
        assertEquals(
            "Hot & fresh <3 café",
            PayloadText.stripHtml("<p>Hot &amp; fresh</p><script>alert('x')</script><style>p{}</style> &lt;3 caf&#233;")
        )
        assertEquals("Line one Line two", PayloadText.stripHtml("Line one<br/>Line two"))
        assertEquals("a b", PayloadText.stripHtml("a&nbsp;&#x20;b"))
    }

    @Test
    fun `stripHtml leaves unknown or malformed entities and drops unterminated tags`() {
        assertEquals("&bogus; & x", PayloadText.stripHtml("&bogus; & x"))
        assertEquals("keep", PayloadText.stripHtml("keep<unterminated"))
        assertEquals("&#1114112;", PayloadText.stripHtml("&#1114112;"))
    }

    @Test
    fun `stripHtml keeps a less-than sign that does not open a tag`() {
        assertEquals("a < b", PayloadText.stripHtml("a < b"))
        assertEquals("x<3 and 1 <= 2", PayloadText.stripHtml("x<3 and 1 <= 2"))
        assertEquals("a < b c", PayloadText.stripHtml("a < <b>b</b> c"))
        assertEquals("trailing <", PayloadText.stripHtml("trailing <"))
        assertEquals("ok", PayloadText.stripHtml("ok<!-- note -->"))
    }

    @Test
    fun `stripHtml is linear on pathological input and clamps length`() {
        val pathological = "&".repeat(200_000) + "<" + "a".repeat(10)
        val result = PayloadText.stripHtml(pathological, maxLength = 50)
        assertEquals(51, result.length)
        assertEquals("", PayloadText.stripHtml(null))
    }

    @Test
    fun `sanitizeInline collapses whitespace and control characters`() {
        assertEquals("a b c", PayloadText.sanitizeInline("  a\n\tb\u0000\u00A0 c  "))
        assertEquals("abc…", PayloadText.sanitizeInline("abcdef", maxLength = 3))
    }

    @Test
    fun `sanitizeInline never splits a surrogate pair when clamping`() {
        assertEquals("ab…", PayloadText.sanitizeInline("ab\uD83D\uDE00cd", maxLength = 3))
        assertEquals("ab\uD83D\uDE00…", PayloadText.sanitizeInline("ab\uD83D\uDE00cd", maxLength = 4))
        assertEquals("…", PayloadText.sanitizeInline("\uD83D\uDE00x", maxLength = 1))
    }

    @Test
    fun `httpsUrlOrNull accepts only absolute https urls`() {
        assertEquals("https://x.test/a.png", PayloadText.httpsUrlOrNull(" https://x.test/a.png "))
        assertNull(PayloadText.httpsUrlOrNull("http://x.test/a.png"))
        assertNull(PayloadText.httpsUrlOrNull("javascript:alert(1)"))
        assertNull(PayloadText.httpsUrlOrNull("https://"))
        assertNull(PayloadText.httpsUrlOrNull(""))
        assertNull(PayloadText.httpsUrlOrNull(null))
    }

    @Test
    fun `httpsUrlOrNull rejects malformed urls, user info and missing hosts`() {
        assertEquals("https://cdn.x.test/a.png?w=200", PayloadText.httpsUrlOrNull("HTTPS://cdn.x.test/a.png?w=200")?.lowercase())
        assertNull(PayloadText.httpsUrlOrNull("https://good.test@evil.test/a.png"))
        assertNull(PayloadText.httpsUrlOrNull("https://x.test/a b.png"))
        assertNull(PayloadText.httpsUrlOrNull("https:///a.png"))
        assertNull(PayloadText.httpsUrlOrNull("https:x.test/a.png"))
        assertNull(PayloadText.httpsUrlOrNull("https://" + "a".repeat(2050)))
    }

    @Test
    fun `toCents handles every org_json number shape without float drift`() {
        assertEquals(800L, PayloadText.toCents(8))
        assertEquals(800L, PayloadText.toCents(8L))
        assertEquals(850L, PayloadText.toCents(8.5))
        assertEquals(30L, PayloadText.toCents(0.1 + 0.2))
        assertEquals(1999L, PayloadText.toCents("19.99"))
        assertEquals(1000L, PayloadText.toCents(BigDecimal("9.995")))
        assertNull(PayloadText.toCents(Double.NaN))
        assertNull(PayloadText.toCents("abc"))
        assertNull(PayloadText.toCents(null))
        assertNull(PayloadText.toCents(true))
    }

    @Test(timeout = 2_000)
    fun `toCents bounds exponents instead of rescaling huge or tiny values`() {
        assertNull(PayloadText.toCents("1e100000000"))
        assertNull(PayloadText.toCents(BigDecimal("1e100000000")))
        assertNull(PayloadText.toCents("1e17"))
        assertEquals(0L, PayloadText.toCents("1.5e-100000000"))
        assertEquals(0L, PayloadText.toCents(BigDecimal("-1e-100000000")))
        assertEquals(1L, PayloadText.toCents("0.005"))
        assertEquals(0L, PayloadText.toCents("0.0049"))
        assertEquals(0L, PayloadText.toCents("0.0009"))
        assertEquals(123_456_789_012_345_600L, PayloadText.toCents("1234567890123456"))
        assertNull(PayloadText.toCents("1" + "0".repeat(70)))
    }
}

class FnbThemeParsingTest {

    @Test
    fun `parseTileWidth accepts dimensions and clamps to a usable tile width`() {
        assertEquals(132f, FnbThemeParsing.DEFAULT_TILE_WIDTH)
        assertEquals(160f, FnbThemeParsing.parseTileWidth("160px"))
        assertEquals(96f, FnbThemeParsing.parseTileWidth("20dp"))
        assertEquals(320f, FnbThemeParsing.parseTileWidth("900"))
        assertNull(FnbThemeParsing.parseTileWidth("wide"))
        assertNull(FnbThemeParsing.parseTileWidth("-10px"))
    }
}

/**
 * The default theme is the quickstart reference for styling the F&B renderers from the same
 * theme.json the SDK already reads, so every key the widgets look up must be present and parseable.
 */
class FnbDefaultThemeTest {

    private val themeJson: String by lazy {
        val file = java.io.File("src/main/assets/themeDefault.json")
        assertTrue("themeDefault.json not found at ${file.absolutePath}", file.exists())
        file.readText()
    }

    private val declaredKeys: List<String> =
        FnbThemeKeys::class.java.declaredFields
            .filter { it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }

    private fun valueOf(key: String): String? =
        Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"([^\"]*)\"").find(themeJson)?.groupValues?.get(1)

    @Test
    fun `every theme key the F&B widgets read is defined in the default theme and parses`() {
        assertEquals(29, declaredKeys.size)
        declaredKeys.forEach { key ->
            val raw = valueOf(key)
            assertNotNull("themeDefault.json is missing $key", raw)
            val parsed: Any? =
                if (key.endsWith("-color")) ThemeValueParsing.parseColor(raw)
                else ThemeValueParsing.parseDp(raw)
            assertNotNull("themeDefault.json has an unparseable value for $key: '$raw'", parsed)
        }
    }
}
