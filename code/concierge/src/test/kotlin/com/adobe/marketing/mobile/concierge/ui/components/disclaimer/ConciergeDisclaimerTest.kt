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

package com.adobe.marketing.mobile.concierge.ui.components.disclaimer

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerConfig
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeDisclaimerTest {

    private fun testDisclaimerStyle(): ConciergeStyles.DisclaimerStyle = ConciergeStyles.DisclaimerStyle(
        textStyle = TextStyle.Default,
        textColor = Color.Black,
        linkTextDecoration = TextDecoration.Underline,
        padding = 8.dp
    )

    @Test
    fun `buildDisclaimerAnnotatedString with no links returns plain text`() {
        val config = DisclaimerConfig(
            text = "AI responses may be inaccurate.",
            links = emptyList()
        )
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("AI responses may be inaccurate.", result.text)
        assertTrue(result.getStringAnnotations("URL", 0, result.length).isEmpty())
    }

    @Test
    fun `buildDisclaimerAnnotatedString with one link replaces placeholder and adds URL annotation`() {
        val config = DisclaimerConfig(
            text = "Check our {Terms} for more.",
            links = listOf(DisclaimerLink("Terms", "https://example.com/terms"))
        )
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("Check our Terms for more.", result.text)
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, annotations.size)
        assertEquals("https://example.com/terms", annotations[0].item)
    }

    @Test
    fun `buildDisclaimerAnnotatedString with multiple links replaces all placeholders`() {
        val config = DisclaimerConfig(
            text = "See {Terms} and {Privacy}.",
            links = listOf(
                DisclaimerLink("Terms", "https://example.com/terms"),
                DisclaimerLink("Privacy", "https://example.com/privacy")
            )
        )
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("See Terms and Privacy.", result.text)
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(2, annotations.size)
        assertEquals("https://example.com/terms", annotations[0].item)
        assertEquals("https://example.com/privacy", annotations[1].item)
    }

    @Test
    fun `buildDisclaimerAnnotatedString with placeholder not in text leaves text unchanged`() {
        val config = DisclaimerConfig(
            text = "No placeholders here.",
            links = listOf(DisclaimerLink("Terms", "https://example.com/terms"))
        )
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("No placeholders here.", result.text)
        assertTrue(result.getStringAnnotations("URL", 0, result.length).isEmpty())
    }

    @Test
    fun `buildDisclaimerAnnotatedString with null text uses empty string`() {
        val config = DisclaimerConfig(text = null, links = emptyList())
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("", result.text)
    }

    @Test
    fun `buildDisclaimerAnnotatedString with null links uses empty list`() {
        val config = DisclaimerConfig(
            text = "Plain text only.",
            links = null
        )
        val style = testDisclaimerStyle()
        val result = buildDisclaimerAnnotatedString(config, style)
        assertEquals("Plain text only.", result.text)
        assertTrue(result.getStringAnnotations("URL", 0, result.length).isEmpty())
    }
}
