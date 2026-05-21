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

import androidx.compose.ui.text.font.FontFamily
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConciergeFontResolverTest {

    // ========== Supported font family values ==========

    @Test
    fun `resolve returns FontFamily Serif for "serif"`() {
        // Given
        val cssValue = "serif"

        // When
        val resolved = ConciergeFontResolver.resolve(cssValue)

        // Then
        assertEquals(FontFamily.Serif, resolved)
    }

    @Test
    fun `resolve returns FontFamily SansSerif for "sans-serif"`() {
        // Given
        val cssValue = "sans-serif"

        // When
        val resolved = ConciergeFontResolver.resolve(cssValue)

        // Then
        assertEquals(FontFamily.SansSerif, resolved)
    }

    @Test
    fun `resolve returns FontFamily Monospace for "monospace"`() {
        // Given
        val cssValue = "monospace"

        // When
        val resolved = ConciergeFontResolver.resolve(cssValue)

        // Then
        assertEquals(FontFamily.Monospace, resolved)
    }

    // ========== Case-insensitivity ==========

    @Test
    fun `resolve is case-insensitive for uppercase input`() {
        assertEquals(FontFamily.Serif, ConciergeFontResolver.resolve("SERIF"))
        assertEquals(FontFamily.SansSerif, ConciergeFontResolver.resolve("SANS-SERIF"))
        assertEquals(FontFamily.Monospace, ConciergeFontResolver.resolve("MONOSPACE"))
    }

    @Test
    fun `resolve is case-insensitive for mixed-case input`() {
        assertEquals(FontFamily.Serif, ConciergeFontResolver.resolve("Serif"))
        assertEquals(FontFamily.SansSerif, ConciergeFontResolver.resolve("Sans-Serif"))
        assertEquals(FontFamily.Monospace, ConciergeFontResolver.resolve("MonoSpace"))
    }

    // ========== Whitespace handling ==========

    @Test
    fun `resolve trims leading and trailing whitespace`() {
        assertEquals(FontFamily.Serif, ConciergeFontResolver.resolve("  serif  "))
        assertEquals(FontFamily.SansSerif, ConciergeFontResolver.resolve("\tsans-serif\n"))
        assertEquals(FontFamily.Monospace, ConciergeFontResolver.resolve(" monospace"))
    }

    // ========== Null / blank / unknown values ==========

    @Test
    fun `resolve returns null for null input`() {
        // When
        val resolved = ConciergeFontResolver.resolve(null)

        // Then
        assertNull(resolved)
    }

    @Test
    fun `resolve returns null for empty string`() {
        // When
        val resolved = ConciergeFontResolver.resolve("")

        // Then
        assertNull(resolved)
    }

    @Test
    fun `resolve returns null for blank string`() {
        // When
        val resolved = ConciergeFontResolver.resolve("   ")

        // Then
        assertNull(resolved)
    }

    @Test
    fun `resolve returns null for unknown font family value`() {
        assertNull(ConciergeFontResolver.resolve("Helvetica"))
        assertNull(ConciergeFontResolver.resolve("Arial"))
        assertNull(ConciergeFontResolver.resolve("cursive"))
        assertNull(ConciergeFontResolver.resolve("fantasy"))
    }

    @Test
    fun `resolve returns null for typo in supported value`() {
        // Common typos / wrong CSS keywords that should NOT resolve
        assertNull(ConciergeFontResolver.resolve("seriff"))
        assertNull(ConciergeFontResolver.resolve("sans serif"))  // missing hyphen
        assertNull(ConciergeFontResolver.resolve("mono-space"))  // unexpected hyphen
        assertNull(ConciergeFontResolver.resolve("mono"))
    }
}
