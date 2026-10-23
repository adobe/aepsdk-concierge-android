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

package com.adobe.marketing.mobile.concierge.ui.components.links

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkedTextFormatterTest {
    private val baseStyle = SpanStyle(color = Color.Black)

    @Test
    fun `empty text produces no content or annotations`() {
        val formatted = LinkedTextFormatter.format("", null, baseStyle, Color.Blue)

        assertEquals("", formatted.text)
        assertTrue(formatted.getStringAnnotations("URL", 0, formatted.length).isEmpty())
    }

    @Test
    fun `null links and blank URLs leave placeholders unlinked`() {
        listOf(null, listOf(DisclaimerLink("Terms", " \t"))).forEach { links ->
            val formatted = LinkedTextFormatter.format("{Terms}", links, baseStyle, Color.Blue)

            assertEquals("Terms", formatted.text)
            assertTrue(formatted.getStringAnnotations("URL", 0, formatted.length).isEmpty())
        }
    }

    @Test
    fun `repeated placeholders use the first exact matching link`() {
        val formatted = LinkedTextFormatter.format(
            text = "Read {Terms}; then revisit {Terms}.",
            links = listOf(
                DisclaimerLink("Terms", "https://first.example/terms"),
                DisclaimerLink("Terms", "https://second.example/terms")
            ),
            baseStyle = baseStyle,
            linkColor = Color.Blue
        )

        assertEquals("Read Terms; then revisit Terms.", formatted.text)
        val annotations = formatted.getStringAnnotations("URL", 0, formatted.length)
        assertEquals(2, annotations.size)
        assertEquals(listOf("https://first.example/terms", "https://first.example/terms"), annotations.map { it.item })
    }

    @Test
    fun `unmatched placeholders lose braces and unclosed placeholders remain verbatim`() {
        val formatted = LinkedTextFormatter.format(
            text = "See {Unknown}.\nKeep {unclosed",
            links = emptyList(),
            baseStyle = baseStyle,
            linkColor = Color.Blue
        )

        assertEquals("See Unknown.\nKeep {unclosed", formatted.text)
        assertTrue(formatted.getStringAnnotations("URL", 0, formatted.length).isEmpty())
    }

    @Test
    fun `malformed URL leaves label plain and unlinked`() {
        val formatted = LinkedTextFormatter.format(
            text = "Read {Terms}.",
            links = listOf(DisclaimerLink("Terms", "https://example.com/has space")),
            baseStyle = baseStyle,
            linkColor = Color.Blue,
            linkTextDecoration = TextDecoration.Underline
        )

        assertEquals("Read Terms.", formatted.text)
        assertTrue(formatted.getStringAnnotations("URL", 0, formatted.length).isEmpty())
    }

    @Test
    fun `invalid first exact match wins and copy is not interpreted as markup`() {
        val formatted = LinkedTextFormatter.format(
            text = "<b>{Terms}</b>\n{Terms}",
            links = listOf(
                DisclaimerLink("Terms", "https://example.com/bad path"),
                DisclaimerLink("Terms", "https://example.com/valid")
            ),
            baseStyle = baseStyle,
            linkColor = Color.Blue
        )

        assertEquals("<b>Terms</b>\nTerms", formatted.text)
        assertTrue(formatted.getStringAnnotations("URL", 0, formatted.length).isEmpty())
    }
}
