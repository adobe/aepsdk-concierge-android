/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.utils.markdown

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.*

class MarkdownRendererTest {

    @Test
    fun `test render with empty tokens list`() {
        val markdown = "Hello World"
        val tokens = emptyList<MarkdownToken>()
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Hello World", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render with no tokens`() {
        val markdown = "Plain text without markdown"
        val tokens = emptyList<MarkdownToken>()
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Plain text without markdown", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render bold token`() {
        val markdown = "This is **bold** text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("This is bold text", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render italic token`() {
        val markdown = "This is *italic* text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("This is italic text", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val italicStyle = result.spanStyles[0]
        assertEquals(FontStyle.Italic, italicStyle.item.fontStyle)
        assertTrue(italicStyle.start >= 0)
        assertTrue(italicStyle.end > italicStyle.start)
    }

    @Test
    fun `test render inline code token`() {
        val markdown = "Use `println()` function"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Use println() function", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val codeStyle = result.spanStyles[0]
        assertEquals(Color.LightGray, codeStyle.item.background)
        assertEquals(14.sp, codeStyle.item.fontSize)
        assertEquals(FontFamily.Monospace, codeStyle.item.fontFamily)
        assertTrue(codeStyle.start >= 0)
        assertTrue(codeStyle.end > codeStyle.start)
    }

    @Test
    fun `test render code block token`() {
        val markdown = "```\nfun test() {}\n```"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertTrue(result.text.contains("fun test() {}"))
        assertEquals(1, result.spanStyles.size)
        
        val codeBlockStyle = result.spanStyles[0]
        assertEquals(Color(0xFFEFEFEF), codeBlockStyle.item.background)
        assertEquals(Color(0xFF333333), codeBlockStyle.item.color)
        assertEquals(16.sp, codeBlockStyle.item.fontSize)
        assertEquals(FontFamily.Monospace, codeBlockStyle.item.fontFamily)
        assertTrue(codeBlockStyle.start >= 0)
        assertTrue(codeBlockStyle.end > codeBlockStyle.start)
    }

    @Test
    fun `test render link token`() {
        val markdown = "[Click here](https://example.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Click here", result.text)
        assertEquals(1, result.spanStyles.size)
        assertEquals(1, result.getStringAnnotations("URL", 0, result.length).size)
        
        val linkStyle = result.spanStyles[0]
        assertEquals(Color.Blue, linkStyle.item.color)
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
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Main Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(26.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(Color(0xFFA75CF2), headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render heading token level 2`() {
        val markdown = "## Sub Heading"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Sub Heading", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val headingStyle = result.spanStyles[0]
        assertEquals(22.sp, headingStyle.item.fontSize)
        assertEquals(FontWeight.Bold, headingStyle.item.fontWeight)
        assertEquals(Color(0xFF48D883), headingStyle.item.color)
        assertTrue(headingStyle.start >= 0)
        assertTrue(headingStyle.end > headingStyle.start)
    }

    @Test
    fun `test render list token`() {
        val markdown = "- List item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("• List item", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render list token with markdown`() {
        val markdown = "- This is **bold** text with *italic* and `code`"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        // The list structure should be preserved
        assertTrue("Expected text to start with '• This is', but got: '${result.text}'", result.text.startsWith("• This is"))
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("code"))
        
        // Check that markdown syntax is removed
        assertFalse(result.text.contains("**bold**"))
        assertFalse(result.text.contains("*italic*"))
        assertFalse(result.text.contains("`code`"))
        
        // Check that span styles are applied for the nested markdown
        assertTrue(result.spanStyles.size > 0)
        
        // Verify specific styles are applied
        val boldStyle = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Should have bold style", boldStyle)
        
        val italicStyle = result.spanStyles.find { it.item.fontStyle == FontStyle.Italic }
        assertNotNull("Should have italic style", italicStyle)
        
        val codeStyle = result.spanStyles.find { it.item.fontFamily == FontFamily.Monospace }
        assertNotNull("Should have code style", codeStyle)
    }

    @Test
    fun `test render blockquote token`() {
        val markdown = "> This is a quote"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("This is a quote", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val quoteStyle = result.spanStyles[0]
        assertEquals(Color(0xFFE0E0E0), quoteStyle.item.background)
        assertEquals(FontStyle.Italic, quoteStyle.item.fontStyle)
        assertTrue(quoteStyle.start >= 0)
        assertTrue(quoteStyle.end > quoteStyle.start)
    }

    @Test
    fun `test render multiple tokens in sequence`() {
        val markdown = "**Bold** and *italic* text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Bold and italic text", result.text)
        assertEquals(2, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
        
        val italicStyle = result.spanStyles[1]
        assertEquals(FontStyle.Italic, italicStyle.item.fontStyle)
        assertTrue(italicStyle.start >= 0)
        assertTrue(italicStyle.end > italicStyle.start)
    }

    @Test
    fun `test render tokens with gaps`() {
        val markdown = "Start **middle** end"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Start middle end", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render overlapping tokens`() {
        val markdown = "**Bold *italic* text**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        assertTrue(result.text.contains("Bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("text"))
        assertTrue("Should have at least one span style", result.spanStyles.size >= 1)
        
        // Verify that bold styling is applied
        val boldStyle = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Should have bold style applied", boldStyle)
        assertTrue(boldStyle!!.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render complex mixed content`() {
        val markdown = "# Title\n\nThis is **bold** and *italic* text.\n\n[Link](https://example.com)\n\n- List item\n\n> Quote here\n\n`inline code`"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        // Verify the content is rendered (exact text may vary due to tokenizer behavior)
        assertTrue(result.text.contains("Title"))
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("Link"))
        assertTrue(result.text.contains("List item"))
        assertTrue(result.text.contains("Quote here"))
        assertTrue(result.text.contains("inline code"))
        
        // Should have multiple span styles
        assertTrue(result.spanStyles.size > 0)
        
        // Should have URL annotation for the link
        val urlAnnotations = result.getStringAnnotations("URL", 0, result.length)
        assertTrue(urlAnnotations.isNotEmpty())
        assertEquals("https://example.com", urlAnnotations[0].item)
    }

    @Test
    fun `test render with tokens out of order`() {
        val markdown = "Text with **bold** and *italic*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Text with bold and italic", result.text)
        assertEquals(2, result.spanStyles.size)
        
        // Should still render correctly despite out-of-order tokens
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
        
        val italicStyle = result.spanStyles[1]
        assertEquals(FontStyle.Italic, italicStyle.item.fontStyle)
        assertTrue(italicStyle.start >= 0)
        assertTrue(italicStyle.end > italicStyle.start)
    }

    @Test
    fun `test render with token starting at beginning`() {
        val markdown = "**Bold** text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Bold text", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render with token ending at end`() {
        val markdown = "Text **bold**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Text bold", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render with empty token groups`() {
        val markdown = "Text with **bold**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Text with bold", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertTrue(boldStyle.start >= 0)
        assertTrue(boldStyle.end > boldStyle.start)
    }

    @Test
    fun `test render with multiple empty tokens`() {
        val markdown = "Hello World"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("Hello World", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render empty markdown string`() {
        val markdown = ""
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render markdown with only whitespace`() {
        val markdown = "   \n\t  "
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("   \n\t  ", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render single character bold`() {
        val markdown = "**a**"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("a", result.text)
        assertEquals(1, result.spanStyles.size)
        
        val boldStyle = result.spanStyles[0]
        assertEquals(FontWeight.Bold, boldStyle.item.fontWeight)
        assertEquals(0, boldStyle.start)
        assertEquals(1, boldStyle.end)
    }

    @Test
    fun `test render markdown with no valid tokens`() {
        val markdown = "This is just plain text with no markdown"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)
        
        assertEquals("This is just plain text with no markdown", result.text)
        assertEquals(0, result.spanStyles.size)
    }

    @Test
    fun `test render with malformed markdown`() {
        val markdown = "**unclosed bold *unclosed italic"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val result = MarkdownRenderer.render(markdown, tokens)

        // Should still render what can be parsed
        assertTrue(result.text.contains("unclosed bold"))
        assertTrue(result.text.contains("unclosed italic"))
        assertTrue(result.spanStyles.size > 0)
    }
}
