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

package com.adobe.marketing.mobile.concierge.utils.markdown

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

class MarkdownRendererTest {
    @Test
    fun `streaming line height preserves text spans annotations and paragraph boundaries`() {
        val documents = listOf(
            "", "\n\n",
            "# Heading\nBody [Store](https://example.com)\n",
            "Body\n# Heading\nBody",
            "```\ncode\n```\nBody",
            "Body\n```\ncode\n```",
            "# A\n# B\nBody\n# C",
            "> > # Nested\n\nBody",
            "- > # Nested\n\nBody",
            "1. **[Store](https://example.com)**\n2. *[Other](https://example.com)*",
            "Body\r\n\r\n# Heading\r\n\r\nTail",
            "Body `inline` [^1]\n\n```\ncode\nmore\n```\n\nTail"
        )
        for (scheme in listOf(lightColorScheme(), darkColorScheme())) {
            for (document in documents) {
                for (length in 0..document.length) {
                    val markdown = document.take(length)
                    val tokens = MarkdownTokenizer.tokenize(markdown)
                    val baseline = MarkdownRenderer.render(markdown, tokens, scheme, testTextStyle)
                    val styled = MarkdownRenderer.render(
                        markdown, tokens, scheme, testTextStyle, bodyLineHeight = 24.sp
                    )
                    assertEquals(markdown, baseline.text, styled.text)
                    assertEquals(markdown, baseline.spanStyles, styled.spanStyles)
                    assertEquals(
                        markdown, baseline.getStringAnnotations(0, baseline.length),
                        styled.getStringAnnotations(0, styled.length)
                    )
                    var end = 0
                    for (range in styled.paragraphStyles) {
                        assertTrue(markdown, range.start >= end)
                        assertTrue(markdown, range.end > range.start && range.end <= styled.length)
                        assertTrue(markdown, range.start == 0 || styled.text[range.start - 1] == '\n')
                        assertTrue(markdown, range.end == styled.length || styled.text[range.end - 1] == '\n')
                        assertEquals(24.sp, range.item.lineHeight)
                        end = range.end
                    }
                }
            }
        }
    }

    @Test
    fun `body line height excludes specialized block paragraphs including nested blocks`() {
        for (markdown in listOf(
            "# Heading",
            "```\nCode\nMore code\n```",
            "> # Heading",
            "- # Heading",
            "Before\n\n# Heading\n\nAfter",
            "Before\n\n```\nCode\n```\n\nAfter",
            "# Heading\n\n```\nCode\n```"
        )) {
            val result = MarkdownRenderer.render(
                markdown, MarkdownTokenizer.tokenize(markdown), lightColorScheme(), testTextStyle,
                bodyLineHeight = 12.sp
            )
            for (special in listOf("Heading", "Code", "More code")) {
                val offset = result.text.indexOf(special)
                if (offset >= 0) {
                    assertTrue(markdown, result.paragraphStyles.none { offset >= it.start && offset < it.end })
                }
            }
            for (body in listOf("Before", "After")) {
                val offset = result.text.indexOf(body)
                if (offset >= 0) {
                    assertEquals(12.sp, result.paragraphStyles.single { offset >= it.start && offset < it.end }.item.lineHeight)
                }
            }
        }
    }

    @Test
    fun `body line height includes inline formatting and is absent by default`() {
        val markdown = "Body **bold** *italic* `code` [Link](https://example.com)\nNext"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val default = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        assertTrue(default.paragraphStyles.isEmpty())
        val styled = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle, bodyLineHeight = 24.sp)
        assertEquals(default.spanStyles, styled.spanStyles)
        assertEquals(default.text, styled.text)
        assertEquals(0, styled.paragraphStyles.single().start)
        assertEquals(styled.length, styled.paragraphStyles.single().end)
        assertEquals(24.sp, styled.paragraphStyles.single().item.lineHeight)
    }

    @Test
    fun `link overrides reach paragraphs lists blockquotes and nested blocks`() {
        val contents = listOf(
            "[Store](https://store.example.com)",
            "- [Store](https://store.example.com)",
            "1. [Store](https://store.example.com)",
            "> [Store](https://store.example.com)",
            "> - [Store](https://store.example.com)"
        )
        for (markdown in contents) {
            for (underline in listOf(true, false)) {
                val result = MarkdownRenderer.render(
                    markdown, MarkdownTokenizer.tokenize(markdown), lightColorScheme(),
                    testTextStyle.copy(fontWeight = FontWeight.Normal),
                    linkColor = Color.Magenta,
                    linkFontWeight = FontWeight.SemiBold,
                    linkUnderline = underline
                )
                val annotation = result.getStringAnnotations("URL", 0, result.length).single()
                assertEquals(markdown, "https://store.example.com", annotation.item)
                val span = result.spanStyles.single {
                    it.start == annotation.start && it.end == annotation.end
                }.item
                assertEquals(markdown, FontWeight.SemiBold, span.fontWeight)
                assertEquals(markdown, Color.Magenta, span.color)
                assertEquals(markdown, if (underline) TextDecoration.Underline else TextDecoration.None, span.textDecoration)
            }
        }
    }

    @Test
    fun `bold links remain at least bold and italic links keep italics`() {
        for (prefix in listOf("", "- ", "> ")) {
            for (weight in listOf(null, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Black)) {
                for (italic in listOf(false, true)) {
                    val link = if (italic) "*[Store](https://example.com)*" else "**[Store](https://example.com)**"
                    val markdown = prefix + link
                    val result = MarkdownRenderer.render(
                        markdown, MarkdownTokenizer.tokenize(markdown), lightColorScheme(),
                        testTextStyle.copy(fontWeight = FontWeight.Normal),
                        linkFontWeight = weight,
                        linkUnderline = false
                    )
                    val annotation = result.getStringAnnotations("URL", 0, result.length).single()
                    val span = result.spanStyles.single {
                        it.start == annotation.start && it.end == annotation.end
                    }.item
                    assertEquals(
                        markdown,
                        if (italic) weight ?: FontWeight.Normal else maxOf(weight ?: FontWeight.Bold, FontWeight.Bold),
                        span.fontWeight
                    )
                    if (italic) assertEquals(androidx.compose.ui.text.font.FontStyle.Italic, span.fontStyle)
                    assertEquals(TextDecoration.None, span.textDecoration)
                }
            }
        }
    }

    @Test
    fun `omitted link overrides preserve inherited weight underline and URL`() {
        val markdown = "[Store](https://example.com)"
        val result = MarkdownRenderer.render(
            markdown, MarkdownTokenizer.tokenize(markdown), lightColorScheme(),
            testTextStyle.copy(fontWeight = FontWeight.Medium)
        )
        assertEquals(FontWeight.Medium, result.spanStyles.single().item.fontWeight)
        assertEquals(TextDecoration.Underline, result.spanStyles.single().item.textDecoration)
        assertEquals("https://example.com", result.getStringAnnotations("URL", 0, result.length).single().item)
    }
    
    // Default text style for tests (matches bodyLarge)
    private val testTextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )

    @Test
    fun `test render bold token`() {
        val markdown = "This is **bold** text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertTrue(result.text.contains("bold"))
        // Should have styles for regular text + bold text
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the bold style among all span styles
        val boldStyle = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Should have a bold style", boldStyle)
        assertTrue(boldStyle!!.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render italic token`() {
        val markdown = "This is *italic* text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertTrue(result.text.contains("italic"))
        // Should have styles for regular text + italic text
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the italic style among all span styles
        val italicStyle = result.spanStyles.find { it.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic }
        assertNotNull("Should have an italic style", italicStyle)
        assertTrue(italicStyle!!.start >= 0)
        assertTrue(italicStyle.end > italicStyle.start)
    }

    @Test
    fun `test render code block token`() {
        val markdown = "```\nfun test() {}\n```"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("fun test() {}"))
        assertEquals(1, result.spanStyles.size)
        
        val codeBlockStyle = result.spanStyles[0]
        assertEquals(colorScheme.surfaceContainerHighest, codeBlockStyle.item.background)
        assertEquals(colorScheme.onSurface, codeBlockStyle.item.color)
        assertEquals(16.sp, codeBlockStyle.item.fontSize)
        assertTrue(codeBlockStyle.start >= 0)
        assertTrue(codeBlockStyle.end > codeBlockStyle.start)
    }

    @Test
    fun `test render link token`() {
        val markdown = "[Click here](https://example.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertEquals("Click here", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(1, result.getStringAnnotations("URL", 0, result.length).size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(TextDecoration.Underline, linkStyle.item.textDecoration)
        assertTrue(linkStyle.start >= 0)
        assertTrue(linkStyle.end > linkStyle.start)
        
        val urlAnnotation = result.getStringAnnotations("URL", 0, result.length)[0]
        assertEquals("https://example.com", urlAnnotation.item)
        assertTrue(urlAnnotation.start >= 0)
        assertTrue(urlAnnotation.end > urlAnnotation.start)
    }

    @Test
    fun `test render heading token level 1`() {
        val markdown = "# Main Heading"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Main Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(26.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.primary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render heading token level 2`() {
        val markdown = "## Sub Heading"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Sub Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(22.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.secondary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render heading token level 3`() {
        val markdown = "### Photoshop"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Photoshop", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(18.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.tertiary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render list token`() {
        val markdown = "- List item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertEquals("• List item", result.text)
        // Should have at least 1 span style for the regular text color
        assertTrue("Should have at least 1 span style", result.spanStyles.size >= 1)
    }

    @Test
    fun `test render list token with markdown`() {
        val markdown = "- This is **bold** text with *italic* and `code`"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertTrue(result.text.contains("• This is"))
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("code"))
        
        // Should have styles for bold, italic, and inline code
        assertTrue(result.spanStyles.size >= 3)
    }

    @Test
    fun `test render blockquote token`() {
        val markdown = "> This is a quote"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("This is a quote", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val blockquoteStyle = result.spanStyles[0]
        assertEquals(colorScheme.surfaceContainer, blockquoteStyle.item.background)
        assertTrue(blockquoteStyle.start >= 0)
        assertTrue(blockquoteStyle.end > blockquoteStyle.start)
    }

    @Test
    fun `test render inline code token`() {
        val markdown = "Use `println()` function"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("println()"))
        // Should have styles for regular text + inline code
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the inline code style among all span styles
        val inlineCodeStyle = result.spanStyles.find { it.item.background == colorScheme.surfaceContainer }
        assertNotNull("Should have an inline code style", inlineCodeStyle)
        assertEquals(colorScheme.surfaceContainer, inlineCodeStyle!!.item.background)
        assertEquals(colorScheme.onSurface, inlineCodeStyle.item.color)
        assertEquals(14.sp, inlineCodeStyle.item.fontSize)
        assertTrue(inlineCodeStyle.start >= 0)
        assertTrue(inlineCodeStyle.end > inlineCodeStyle.start)
    }

    @Test
    fun `test render heading level 1 in dark theme`() {
        val markdown = "# Main Heading"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Main Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(26.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.primary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render heading level 2 in dark theme`() {
        val markdown = "## Sub Heading"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Sub Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(22.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.secondary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render heading level 3 in dark theme`() {
        val markdown = "### Photoshop"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Photoshop", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(18.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(colorScheme.tertiary, headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render bold token in dark theme`() {
        val markdown = "This is **bold** text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("bold"))
        // Should have styles for regular text + bold text
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the bold style among all span styles
        val boldStyle = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Should have a bold style", boldStyle)
        assertEquals(FontWeight.Bold, boldStyle!!.item.fontWeight)
        assertEquals(colorScheme.onSurface, boldStyle.item.color)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render italic token in dark theme`() {
        val markdown = "This is *italic* text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("italic"))
        // Should have styles for regular text + italic text
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the italic style among all span styles
        val italicStyle = result.spanStyles.find { it.item.fontStyle == androidx.compose.ui.text.font.FontStyle.Italic }
        assertNotNull("Should have an italic style", italicStyle)
        assertEquals(colorScheme.onSurface, italicStyle!!.item.color)
        assertTrue(italicStyle.start >= 0)
        assertTrue(italicStyle.end > italicStyle.start)
    }

    @Test
    fun `test render code block token in dark theme`() {
        val markdown = "```\nfun test() {}\n```"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("fun test() {}"))
        assertEquals(1, result.spanStyles.size)
        
        val codeBlockStyle = result.spanStyles[0]
        assertEquals(colorScheme.surfaceContainerHighest, codeBlockStyle.item.background)
        assertEquals(colorScheme.onSurface, codeBlockStyle.item.color)
        assertEquals(16.sp, codeBlockStyle.item.fontSize)
        assertTrue(codeBlockStyle.start >= 0)
        assertTrue(codeBlockStyle.end > codeBlockStyle.start)
    }

    @Test
    fun `test render inline code token in dark theme`() {
        val markdown = "Use `println()` function"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertTrue(result.text.contains("println()"))
        // Should have styles for regular text + inline code
        assertTrue("Should have at least 2 span styles", result.spanStyles.size >= 2)
        
        // Find the inline code style among all span styles
        val inlineCodeStyle = result.spanStyles.find { it.item.background == colorScheme.surfaceContainer }
        assertNotNull("Should have an inline code style", inlineCodeStyle)
        assertEquals(colorScheme.surfaceContainer, inlineCodeStyle!!.item.background)
        assertEquals(colorScheme.onSurface, inlineCodeStyle.item.color)
        assertEquals(14.sp, inlineCodeStyle.item.fontSize)
        assertTrue(inlineCodeStyle.start >= 0)
        assertTrue(inlineCodeStyle.end > inlineCodeStyle.start)
    }

    @Test
    fun `test render blockquote token in dark theme`() {
        val markdown = "> This is a quote"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("This is a quote", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val blockquoteStyle = result.spanStyles[0]
        assertEquals(colorScheme.surfaceContainer, blockquoteStyle.item.background)
        assertEquals(colorScheme.onSurface, blockquoteStyle.item.color)
        assertTrue(blockquoteStyle.start >= 0)
        assertTrue(blockquoteStyle.end > blockquoteStyle.start)
    }

    @Test
    fun `test render complex nested markdown in dark theme`() {
        val markdown = """
            # Main Heading
            
            This is a paragraph with **bold** and *italic* text.
            
            - List item with `inline code`
            - Another item with [a link](https://example.com)
            - Item with **bold** and *italic* together
            
            > This is a blockquote with **bold** text
            
            ## Sub Heading
            
            ### Photoshop
            
            More content here.
        """.trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val result = MarkdownRenderer.render(markdown, tokens, darkColorScheme(), testTextStyle)
        
        // Check that all elements are properly processed
        assertTrue(result.text.contains("Main Heading"))
        assertTrue(result.text.contains("Sub Heading"))
        assertTrue(result.text.contains("Photoshop"))
        assertTrue(result.text.contains("• List item with"))
        assertTrue(result.text.contains("• Another item with"))
        assertTrue(result.text.contains("• Item with"))
        assertTrue(result.text.contains("This is a blockquote with"))
        
        // Check that nested markdown is processed
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("inline code"))
        assertTrue(result.text.contains("a link"))
        
        // Check that markdown syntax is removed
        assertFalse("Found **bold** in result: '${result.text}'", result.text.contains("**bold**"))
        assertFalse("Found *italic* in result: '${result.text}'", result.text.contains("*italic*"))
        assertFalse("Found `inline code` in result: '${result.text}'", result.text.contains("`inline code`"))
        assertFalse("Found [a link]( in result: '${result.text}'", result.text.contains("[a link]("))
        
        // Check that URL annotations are added
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse(annotations.isEmpty())
        assertEquals("https://example.com", annotations[0].item)
        
        // Check that span styles are applied
        assertTrue(result.spanStyles.size > 0)
    }

    @Test
    fun `test render empty tokens list`() {
        val markdown = "Plain text without markdown"
        val tokens = emptyList<MarkdownToken>()
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertEquals(markdown, result.text)
        // Should have 1 span style for the regular text color
        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.getStringAnnotations("URL", 0, result.length).size)
    }

    @Test
    fun `test render with overlapping tokens`() {
        val markdown = "**bold *italic* bold**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme(), testTextStyle)
        
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        
        // Should have some span styles applied
        assertTrue("Should have at least 1 span style", result.spanStyles.size >= 1)
    }
    
    @Test
    fun `test render bold link token`() {
        val markdown = "**[Adobe Premiere Pro](https://www.adobe.com/premiere)**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render link text only (no markdown syntax)
        assertEquals("Adobe Premiere Pro", result.text)
        
        // Should have 1 span style with bold + underline + link color
        assertEquals(1, result.spanStyles.size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, linkStyle.item.fontWeight)
        assertEquals(TextDecoration.Underline, linkStyle.item.textDecoration)
        assertEquals(colorScheme.onSurface, linkStyle.item.color)
        
        // Should have URL annotation
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://www.adobe.com/premiere", urlAnnotations[0].item)
    }
    
    @Test
    fun `test render italic link token`() {
        val markdown = "*[Adobe After Effects](https://www.adobe.com/aftereffects)*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render link text only (no markdown syntax)
        assertEquals("Adobe After Effects", result.text)
        
        // Should have 1 span style with italic + underline + link color
        assertEquals(1, result.spanStyles.size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(androidx.compose.ui.text.font.FontStyle.Italic, linkStyle.item.fontStyle)
        assertEquals(TextDecoration.Underline, linkStyle.item.textDecoration)
        assertEquals(colorScheme.onSurface, linkStyle.item.color)
        
        // Should have URL annotation
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://www.adobe.com/aftereffects", urlAnnotations[0].item)
    }

    @Test
    fun `test render applies custom link color to all link variants`() {
        val markdown = "[Plain](https://a.com) **[Bold](https://b.com)** *[Italic](https://c.com)*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        val customLinkColor = Color(0xFF00AA55)

        val result = MarkdownRenderer.render(
            markdown, tokens, colorScheme, testTextStyle, linkColor = customLinkColor
        )

        val linkStyles = result.spanStyles.filter { it.item.textDecoration == TextDecoration.Underline }
        assertEquals(3, linkStyles.size)
        linkStyles.forEach { assertEquals(customLinkColor, it.item.color) }
    }

    @Test
    fun `test render applies custom link color to links nested in lists`() {
        val markdown = "- Visit [the store](https://store.example.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val customLinkColor = Color(0xFF00AA55)

        val result = MarkdownRenderer.render(
            markdown, tokens, lightColorScheme(), testTextStyle, linkColor = customLinkColor
        )

        val linkStyle = result.spanStyles.single { it.item.textDecoration == TextDecoration.Underline }
        assertEquals(customLinkColor, linkStyle.item.color)
    }

    @Test
    fun `test render falls back to onSurface when link color is unspecified`() {
        val markdown = "[Store](https://store.example.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()

        val result = MarkdownRenderer.render(
            markdown, tokens, colorScheme, testTextStyle, linkColor = Color.Unspecified
        )

        assertEquals(colorScheme.onSurface, result.spanStyles.single().item.color)
    }
    
    @Test
    fun `test render bold link vs regular link`() {
        val markdown = "**[Bold Link](https://bold.com)** and [Regular Link](https://regular.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render both link texts
        assertTrue(result.text.contains("Bold Link"))
        assertTrue(result.text.contains("Regular Link"))
        
        // Should have at least 3 span styles (bold link, regular link, gap text)
        assertTrue("Should have at least 3 span styles", result.spanStyles.size >= 3)
        
        // Find bold link style
        val boldLinkStyle = result.spanStyles.find { 
            it.item.fontWeight == FontWeight.Bold && 
            it.item.textDecoration == TextDecoration.Underline 
        }
        assertNotNull("Should have a bold link style", boldLinkStyle)
        
        // Should have 2 URL annotations
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(2, urlAnnotations.size)
    }
    
    @Test
    fun `test render bold link vs regular bold`() {
        val markdown = "**[Bold Link](https://example.com)** and **regular bold**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render both texts
        assertTrue(result.text.contains("Bold Link"))
        assertTrue(result.text.contains("regular bold"))
        
        // Find bold link style (has underline)
        val boldLinkStyle = result.spanStyles.find { 
            it.item.fontWeight == FontWeight.Bold && 
            it.item.textDecoration == TextDecoration.Underline 
        }
        assertNotNull("Should have a bold link style", boldLinkStyle)
        
        // Find regular bold style (no underline)
        val regularBoldStyle = result.spanStyles.find { 
            it.item.fontWeight == FontWeight.Bold && 
            it.item.textDecoration != TextDecoration.Underline 
        }
        assertNotNull("Should have a regular bold style", regularBoldStyle)
        
        // Should have only 1 URL annotation (from bold link)
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, urlAnnotations.size)
        assertEquals("https://example.com", urlAnnotations[0].item)
    }
    
    @Test
    fun `test render multiple bold links in list`() {
        val markdown = """- **[Adobe Premiere Pro](https://adobe.com/premiere)**: Video editing
- **[Adobe After Effects](https://adobe.com/aftereffects)**: Motion graphics
- **[Adobe Audition](https://adobe.com/audition)**: Audio editing""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render all product names
        assertTrue(result.text.contains("Adobe Premiere Pro"))
        assertTrue(result.text.contains("Adobe After Effects"))
        assertTrue(result.text.contains("Adobe Audition"))
        
        // Should render list bullets
        assertTrue(result.text.contains("•"))
        
        // Should not contain markdown syntax
        assertFalse("Should not contain **", result.text.contains("**"))
        assertFalse("Should not contain [", result.text.contains("["))
        assertFalse("Should not contain ](", result.text.contains("]("))
        
        // Should have 3 URL annotations
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(3, urlAnnotations.size)
        assertEquals("https://adobe.com/premiere", urlAnnotations[0].item)
        assertEquals("https://adobe.com/aftereffects", urlAnnotations[1].item)
        assertEquals("https://adobe.com/audition", urlAnnotations[2].item)
    }
    
    @Test
    fun `test render bold link in dark theme`() {
        val markdown = "**[Adobe Premiere Pro](https://www.adobe.com/premiere)**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Adobe Premiere Pro", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, linkStyle.item.fontWeight)
        assertEquals(TextDecoration.Underline, linkStyle.item.textDecoration)
        assertEquals(colorScheme.onSurface, linkStyle.item.color)
        
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, urlAnnotations.size)
    }
    
    @Test
    fun `test render italic link in dark theme`() {
        val markdown = "*[Adobe After Effects](https://www.adobe.com/aftereffects)*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = darkColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        assertEquals("Adobe After Effects", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(androidx.compose.ui.text.font.FontStyle.Italic, linkStyle.item.fontStyle)
        assertEquals(TextDecoration.Underline, linkStyle.item.textDecoration)
        assertEquals(colorScheme.onSurface, linkStyle.item.color)
        
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(1, urlAnnotations.size)
    }
    
    @Test
    fun `test render real world example from user issue`() {
        val markdown = """To create polished, professional-quality videos from your clips, Adobe offers the following key products:

- **[Adobe Premiere Pro](https://www.adobe.com/products/premiere_pro.html)**: Industry-leading video editing software with advanced tools for editing, color correction, effects, and audio integration.
- **[Adobe After Effects](https://www.adobe.com/products/after_effects.html)**: For adding motion graphics, visual effects, and animated titles to enhance your videos.
- **[Adobe Audition](https://www.adobe.com/products/audition.html)**: Professional audio editing and mixing to improve your video's sound quality.

Would you like guidance on which product suits your skill level or specific video project type?""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        val colorScheme = lightColorScheme()
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme, testTextStyle)
        
        // Should render product names without ** markers
        assertTrue(result.text.contains("Adobe Premiere Pro"))
        assertTrue(result.text.contains("Adobe After Effects"))
        assertTrue(result.text.contains("Adobe Audition"))
        
        // Should NOT contain markdown syntax
        assertFalse("Should not show ** markers: ${result.text}", result.text.contains("**Adobe Premiere Pro**"))
        assertFalse("Should not show ** markers: ${result.text}", result.text.contains("**Adobe After Effects**"))
        assertFalse("Should not show ** markers: ${result.text}", result.text.contains("**Adobe Audition**"))
        assertFalse("Should not show link syntax: ${result.text}", result.text.contains("](https://"))
        
        // Should have 3 URL annotations for the products
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertEquals(3, urlAnnotations.size)
        assertTrue(urlAnnotations.any { it.item.contains("premiere_pro.html") })
        assertTrue(urlAnnotations.any { it.item.contains("after_effects.html") })
        assertTrue(urlAnnotations.any { it.item.contains("audition.html") })
        
        // Should have bold link styles applied (bold + underline + link color)
        val boldLinkStyles = result.spanStyles.filter { 
            it.item.fontWeight == FontWeight.Bold && 
            it.item.textDecoration == TextDecoration.Underline &&
            it.item.color == colorScheme.onSurface
        }
        assertEquals("Should have 3 bold link styles", 3, boldLinkStyles.size)
    }
}
