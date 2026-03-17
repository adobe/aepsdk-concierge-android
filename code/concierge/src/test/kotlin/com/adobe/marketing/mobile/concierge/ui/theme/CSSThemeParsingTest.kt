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

import org.junit.Assert.*
import org.junit.Test

class CSSThemeParsingTest {
    
    @Test
    fun `test parseThemeJson handles theme with CSS block`() {
        val themeJson = """
            {
              "metadata": {
                "name": "Test Theme",
                "version": "1.0.0"
              },
              "theme": {
                "--color-primary": "#EB1000",
                "--color-text": "#2C2C2C",
                "--message-user-background": "#FF0000"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeJson(themeJson)
        assertNotNull("Theme should be parsed", theme)
        assertEquals("Test Theme", theme?.name)
        assertNotNull("Theme colors should exist", theme?.colors)
        assertEquals("#EB1000", theme?.colors?.primaryColors?.primary)
        assertEquals("#2C2C2C", theme?.colors?.primaryColors?.text)
        assertEquals("#FF0000", theme?.colors?.message?.userBackground)
    }
    
    @Test
    fun `test parseThemeTokens handles complete configuration`() {
        val themeJson = """
            {
              "metadata": {
                "name": "Complete Theme",
                "version": "1.0.0",
                "author": "Adobe"
              },
              "behavior": {
                "enableDarkMode": true,
                "enableAnimations": false
              },
              "theme": {
                "--color-primary": "#EB1000",
                "--input-height-mobile": "52px",
                "--message-border-radius": "10px"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(themeJson)
        assertNotNull("Theme should be parsed", theme)
        assertEquals("Complete Theme", theme?.metadata?.name)
        assertEquals("Adobe", theme?.metadata?.author)
        assertEquals(true, theme?.behavior?.enableDarkMode)
        assertEquals(false, theme?.behavior?.enableAnimations)
        assertEquals("#EB1000", theme?.colors?.primaryColors?.primary)
        assertEquals(52.0, theme?.cssLayout?.inputHeight)
        assertEquals(10.0, theme?.cssLayout?.messageBorderRadius)
    }
    
    @Test
    fun `test CSS theme with only theme block`() {
        val cssThemeJson = """
            {
              "theme": {
                "--color-primary": "#EB1000",
                "--color-text": "#2C2C2C"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("Theme colors should exist", theme?.colors)
        assertEquals("#EB1000", theme?.colors?.primaryColors?.primary)
        assertEquals("#2C2C2C", theme?.colors?.primaryColors?.text)
    }
    
    @Test
    fun `test CSS color parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--color-primary": "#EB1000",
                "--color-text": "#2C2C2C",
                "--message-user-background": "#FF0000",
                "--message-user-text": "#FFFFFF"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("Theme colors should exist", theme?.colors)
        
        // Check primary colors
        assertEquals("#EB1000", theme?.colors?.primaryColors?.primary)
        assertEquals("#2C2C2C", theme?.colors?.primaryColors?.text)
        
        // Check message colors
        assertEquals("#FF0000", theme?.colors?.message?.userBackground)
        assertEquals("#FFFFFF", theme?.colors?.message?.userText)
    }
    
    @Test
    fun `test CSS layout parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--input-height-mobile": "52px",
                "--input-border-radius-mobile": "12px",
                "--message-border-radius": "10px",
                "--button-height-s": "30px"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        
        // Check layout values
        assertEquals(52.0, theme?.cssLayout?.inputHeight)
        assertEquals(12.0, theme?.cssLayout?.inputBorderRadius)
        assertEquals(10.0, theme?.cssLayout?.messageBorderRadius)
        assertEquals(30.0, theme?.cssLayout?.buttonHeightSmall)
    }
    
    @Test
    fun `test CSS typography parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--font-family": "\"Adobe Clean\", sans-serif",
                "--line-height-body": "1.5"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("Typography should exist", theme?.typography)
        
        assertEquals("\"Adobe Clean\", sans-serif", theme?.typography?.fontFamily)
        assertEquals(1.5, theme?.typography?.lineHeight)
    }
    
    @Test
    fun `test CSS box shadow parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--input-box-shadow": "0 2px 8px rgba(0, 0, 0, 0.1)"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertNotNull("Box shadow should exist", theme?.cssLayout?.inputBoxShadow)
        
        val boxShadow = theme?.cssLayout?.inputBoxShadow
        assertEquals(0.0, boxShadow?.get("offsetX"))
        assertEquals(2.0, boxShadow?.get("offsetY"))
        assertEquals(8.0, boxShadow?.get("blurRadius"))
    }
    
    @Test
    fun `test CSS padding parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--message-padding": "12px 16px"
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertNotNull("Message padding should exist", theme?.cssLayout?.messagePadding)
        
        val padding = theme?.cssLayout?.messagePadding
        assertEquals(12.0, padding?.get(0)) // top
        assertEquals(16.0, padding?.get(1)) // right
        assertEquals(12.0, padding?.get(2)) // bottom
        assertEquals(16.0, padding?.get(3)) // left
    }
    
    @Test
    fun `test theme without theme block`() {
        val normalThemeJson = """
            {
              "metadata": {
                "name": "Normal Theme",
                "version": "1.0.0"
              },
              "behavior": {
                "enableDarkMode": true
              }
            }
        """.trimIndent()
        
        val theme = ThemeParser.parseThemeTokens(normalThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertEquals("Normal Theme", theme?.metadata?.name)
        assertEquals(true, theme?.behavior?.enableDarkMode)
        // CSS layout should be null since there's no theme block
        assertNull("CSS layout should be null without theme block", theme?.cssLayout)
    }
    
    @Test
    fun `test gradient outline color should be null`() {
        val cssThemeJson = """
            {
              "theme": {
                "--input-outline-color": "linear-gradient(90deg, #FF0000 0%, #FF8800 100%)"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("Input colors should exist", theme?.colors?.input)

        // Gradients should be set to null
        assertNull("Gradient outline should be null", theme?.colors?.input?.outline)
    }

    @Test
    fun `test product card border radius parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--product-card-border-radius": "8px"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertEquals(8.0, theme?.cssLayout?.productCardBorderRadius)
    }

    @Test
    fun `test product card text padding parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--product-card-text-horizontal-padding": "16px",
                "--product-card-text-top-padding": "24px",
                "--product-card-text-bottom-padding": "16px",
                "--product-card-text-spacing": "8px"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertEquals(16.0, theme?.cssLayout?.productCardTextHorizontalPadding)
        assertEquals(24.0, theme?.cssLayout?.productCardTextTopPadding)
        assertEquals(16.0, theme?.cssLayout?.productCardTextBottomPadding)
        assertEquals(8.0, theme?.cssLayout?.productCardTextSpacing)
    }

    @Test
    fun `test product card carousel spacing parsing`() {
        val cssThemeJson = """
            {
              "theme": {
                "--product-card-carousel-horizontal-padding": "16px",
                "--product-card-carousel-spacing": "12px"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertEquals(16.0, theme?.cssLayout?.productCardCarouselHorizontalPadding)
        assertEquals(12.0, theme?.cssLayout?.productCardCarouselSpacing)
    }

    @Test
    fun `test product card new variables are null when not specified`() {
        // Provide only an unrelated layout key so cssLayout is created but new variables are absent.
        val cssThemeJson = """
            {
              "theme": {
                "--input-height-mobile": "52px"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        assertNotNull("CSS layout should exist", theme?.cssLayout)
        assertNull("Border radius should be null when not specified", theme?.cssLayout?.productCardBorderRadius)
        assertNull("Text horizontal padding should be null when not specified", theme?.cssLayout?.productCardTextHorizontalPadding)
        assertNull("Text top padding should be null when not specified", theme?.cssLayout?.productCardTextTopPadding)
        assertNull("Text bottom padding should be null when not specified", theme?.cssLayout?.productCardTextBottomPadding)
        assertNull("Text spacing should be null when not specified", theme?.cssLayout?.productCardTextSpacing)
        assertNull("Carousel horizontal padding should be null when not specified", theme?.cssLayout?.productCardCarouselHorizontalPadding)
        assertNull("Carousel spacing should be null when not specified", theme?.cssLayout?.productCardCarouselSpacing)
    }

    @Test
    fun `test product card complete new variables block`() {
        val cssThemeJson = """
            {
              "theme": {
                "--product-card-border-radius": "12px",
                "--product-card-text-horizontal-padding": "20px",
                "--product-card-text-top-padding": "28px",
                "--product-card-text-bottom-padding": "12px",
                "--product-card-text-spacing": "10px",
                "--product-card-carousel-horizontal-padding": "8px",
                "--product-card-carousel-spacing": "16px"
              }
            }
        """.trimIndent()

        val theme = ThemeParser.parseThemeTokens(cssThemeJson)
        assertNotNull("Theme should be parsed", theme)
        val layout = theme?.cssLayout
        assertNotNull("CSS layout should exist", layout)
        assertEquals(12.0, layout?.productCardBorderRadius)
        assertEquals(20.0, layout?.productCardTextHorizontalPadding)
        assertEquals(28.0, layout?.productCardTextTopPadding)
        assertEquals(12.0, layout?.productCardTextBottomPadding)
        assertEquals(10.0, layout?.productCardTextSpacing)
        assertEquals(8.0, layout?.productCardCarouselHorizontalPadding)
        assertEquals(16.0, layout?.productCardCarouselSpacing)
    }
}

