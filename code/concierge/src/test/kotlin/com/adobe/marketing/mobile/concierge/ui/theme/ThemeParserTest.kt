/*
 * Copyright 2025 Adobe. All rights reserved.
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

import androidx.compose.ui.graphics.Color
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ThemeParserTest {

    @Test
    fun `parseThemeJson should parse valid complete theme`() {
        val json = """
            {
                "name": "Test Theme",
                "colors": {
                    "primary": "#3949AB",
                    "onPrimary": "#FFFFFF"
                },
                "styles": {
                    "header": {
                        "padding": 16,
                        "titleFontWeight": "bold"
                    }
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertEquals("Test Theme", config?.name)
        assertEquals("#3949AB", config?.colors?.primary)
        assertEquals("#FFFFFF", config?.colors?.onPrimary)
        assertEquals(16.0, config?.styles?.header?.padding)
        assertEquals("bold", config?.styles?.header?.titleFontWeight)
    }

    @Test
    fun `parseThemeJson should handle minimal theme`() {
        val json = """
            {
                "colors": {
                    "primary": "#FF0000"
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config)
        assertEquals("#FF0000", config?.colors?.primary)
        assertNull(config?.colors?.secondary)
        assertNull(config?.styles)
    }

    @Test
    fun `parseThemeJson should return null for invalid JSON`() {
        val json = "{ invalid json }"
        val config = ThemeParser.parseThemeJson(json)
        assertNull(config)
    }

    @Test
    fun `toComposeColor should parse 6-digit hex colors`() {
        val color = "#3949AB".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should parse 8-digit hex colors with alpha`() {
        val color = "#803949AB".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should parse 3-digit hex colors`() {
        val color = "#FFF".toComposeColor()
        assertNotNull(color)
        assertTrue(color is Color)
    }

    @Test
    fun `toComposeColor should return null for invalid hex`() {
        val color = "not-a-color".toComposeColor()
        assertNull(color)
    }

    @Test
    fun `toFontWeight should parse valid font weights`() {
        assertEquals(androidx.compose.ui.text.font.FontWeight.Thin, "thin".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Light, "light".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Normal, "normal".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Medium, "medium".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "bold".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Black, "black".toFontWeight())
    }

    @Test
    fun `toFontWeight should handle case insensitivity`() {
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "BOLD".toFontWeight())
        assertEquals(androidx.compose.ui.text.font.FontWeight.Bold, "Bold".toFontWeight())
    }

    @Test
    fun `toFontWeight should return null for invalid weight`() {
        assertNull("invalid".toFontWeight())
    }

    @Test
    fun `createColorsFromJson should merge with defaults`() {
        val jsonColors = ConciergeThemeColors(
            primary = "#FF0000",
            onPrimary = "#FFFFFF"
            // Other colors null
        )

        val colors = ThemeParser.createColorsFromJson(jsonColors, LightConciergeColors)

        // Should use JSON values where provided
        assertEquals(Color(0xFFFF0000), colors.primary)
        assertEquals(Color.White, colors.onPrimary)

        // Should fall back to defaults for missing values
        assertEquals(LightConciergeColors.secondary, colors.secondary)
        assertEquals(LightConciergeColors.surface, colors.surface)
        assertEquals(LightConciergeColors.background, colors.background)
    }

    @Test
    fun `createColorsFromJson should return defaults when json is null`() {
        val colors = ThemeParser.createColorsFromJson(null, LightConciergeColors)
        assertEquals(LightConciergeColors, colors)
    }

    @Test
    fun `parseThemeJson should handle all style objects`() {
        val json = """
            {
                "styles": {
                    "header": { "padding": 16 },
                    "inputPanel": { "outerCornerRadius": 12 },
                    "messageBubble": { "padding": 8 },
                    "productCard": { "elevation": 1 },
                    "welcomeCard": { "cornerRadius": 12 }
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertNotNull(config?.styles)
        assertNotNull(config?.styles?.header)
        assertNotNull(config?.styles?.inputPanel)
        assertNotNull(config?.styles?.messageBubble)
        assertNotNull(config?.styles?.productCard)
        assertNotNull(config?.styles?.welcomeCard)
    }

    @Test
    fun `toDp should convert Double to Dp`() {
        val dp = 16.0.toDp()
        assertEquals(16.0f, dp.value)
    }

    @Test
    fun `toAlpha should convert Double to Float and clamp`() {
        assertEquals(0.5f, 0.5.toAlpha())
        assertEquals(0.0f, (-0.5).toAlpha()) // Should clamp to 0
        assertEquals(1.0f, 1.5.toAlpha()) // Should clamp to 1
    }

    @Test
    fun `parseThemeJson should handle custom text values`() {
        val json = """
            {
                "styles": {
                    "inputPanel": {
                        "placeholderText": "Custom placeholder",
                        "listeningPlaceholderText": "Custom listening"
                    },
                    "thinkingAnimation": {
                        "thinkingText": "Processing"
                    }
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertEquals("Custom placeholder", config?.styles?.inputPanel?.placeholderText)
        assertEquals("Custom listening", config?.styles?.inputPanel?.listeningPlaceholderText)
        assertEquals("Processing", config?.styles?.thinkingAnimation?.thinkingText)
    }

    @Test
    fun `parseThemeJson should handle animation durations`() {
        val json = """
            {
                "styles": {
                    "inputPanel": {
                        "recordingBorderAnimationDuration": 1500
                    },
                    "thinkingAnimation": {
                        "dotAnimationDuration": 600,
                        "dotAnimationDelay": 200
                    }
                }
            }
        """.trimIndent()

        val config = ThemeParser.parseThemeJson(json)

        assertEquals(1500, config?.styles?.inputPanel?.recordingBorderAnimationDuration)
        assertEquals(600, config?.styles?.thinkingAnimation?.dotAnimationDuration)
        assertEquals(200, config?.styles?.thinkingAnimation?.dotAnimationDelay)
    }
}

