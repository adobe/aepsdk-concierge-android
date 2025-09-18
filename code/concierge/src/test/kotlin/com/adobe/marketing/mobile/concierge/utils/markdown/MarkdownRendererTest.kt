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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

class MarkdownRendererTest {

    @Test
    fun `test render bold token`() {
        val markdown = "This is **bold** text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
        assertEquals("• List item", result.text)
        // Should have at least 1 span style for the regular text color
        assertTrue("Should have at least 1 span style", result.spanStyles.size >= 1)
    }

    @Test
    fun `test render list token with markdown`() {
        val markdown = "- This is **bold** text with *italic* and `code`"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, colorScheme)
        
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
        val result = MarkdownRenderer.render(markdown, tokens, darkColorScheme())
        
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
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
        assertEquals(markdown, result.text)
        // Should have 1 span style for the regular text color
        assertEquals(1, result.spanStyles.size)
        assertEquals(0, result.getStringAnnotations("URL", 0, result.length).size)
    }

    @Test
    fun `test render with overlapping tokens`() {
        val markdown = "**bold *italic* bold**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens, lightColorScheme())
        
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        
        // Should have some span styles applied
        assertTrue("Should have at least 1 span style", result.spanStyles.size >= 1)
    }
}
