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

import org.junit.Test
import org.junit.Assert.*

class MarkdownParserTest {

    @Test
    fun `test tokenization works correctly`() {
        val markdown = "# Main Heading\n## Sub Heading\n### Photoshop\n**bold** *italic*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        // Should have tokens for headings, bold, and italic
        assertTrue(tokens.size >= 4)
        
        // Check that we have heading tokens
        val headingTokens = tokens.filter { it.type == TokenType.HEADING }
        assertEquals(3, headingTokens.size)
        
        // Check that we have bold and italic tokens
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        assertTrue(boldTokens.isNotEmpty())
        assertTrue(italicTokens.isNotEmpty())
    }

    @Test
    fun `test heading level 3 tokenization`() {
        val markdown = "### Photoshop"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(1, tokens.size)
        assertEquals(TokenType.HEADING, tokens[0].type)
        assertEquals(3, tokens[0].groups[0].length) // Should have 3 hash symbols
        assertEquals("Photoshop", tokens[0].groups[1])
    }

    @Test
    fun `test complex markdown tokenization`() {
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
        
        // Should have tokens for various markdown elements
        val headingTokens = tokens.filter { it.type == TokenType.HEADING }
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        val linkTokens = tokens.filter { it.type == TokenType.LINK }
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        val blockquoteTokens = tokens.filter { it.type == TokenType.BLOCKQUOTE }
        val inlineCodeTokens = tokens.filter { it.type == TokenType.INLINE_CODE }
        
        // Verify we have the expected tokens
        assertTrue("Should have heading tokens", headingTokens.isNotEmpty())
        assertTrue("Should have bold tokens", boldTokens.isNotEmpty())
        assertTrue("Should have italic tokens", italicTokens.isNotEmpty())
        assertTrue("Should have link tokens", linkTokens.isNotEmpty())
        assertTrue("Should have list tokens", listTokens.isNotEmpty())
        assertTrue("Should have blockquote tokens", blockquoteTokens.isNotEmpty())
        assertTrue("Should have inline code tokens", inlineCodeTokens.isNotEmpty())
        
        // Check specific heading levels
        val h1Tokens = headingTokens.filter { it.groups[0].length == 1 }
        val h2Tokens = headingTokens.filter { it.groups[0].length == 2 }
        val h3Tokens = headingTokens.filter { it.groups[0].length == 3 }
        
        assertTrue("Should have H1 tokens", h1Tokens.isNotEmpty())
        assertTrue("Should have H2 tokens", h2Tokens.isNotEmpty())
        assertTrue("Should have H3 tokens", h3Tokens.isNotEmpty())
        
        // Check that H3 token contains "Photoshop"
        val photoshopToken = h3Tokens.find { it.groups[1] == "Photoshop" }
        assertNotNull("Should have Photoshop H3 token", photoshopToken)
    }

    @Test
    fun `test empty string tokenization`() {
        val tokens = MarkdownTokenizer.tokenize("")
        assertEquals(0, tokens.size)
    }

    @Test
    fun `test plain text tokenization`() {
        val plainText = "This is just plain text without any markdown formatting."
        val tokens = MarkdownTokenizer.tokenize(plainText)
        assertEquals(0, tokens.size)
    }

    @Test
    fun `test link tokenization`() {
        val markdown = "[Click here](https://example.com)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(1, tokens.size)
        assertEquals(TokenType.LINK, tokens[0].type)
        assertEquals("Click here", tokens[0].groups[0])
        assertEquals("https://example.com", tokens[0].groups[1])
    }

    @Test
    fun `test code block tokenization`() {
        val markdown = "```\nfun test() {}\n```"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        // Debug: check what we actually get
        if (tokens.isEmpty()) {
            fail("Expected at least one token, but got none")
        }
        
        // Just verify we get some tokens for code blocks
        val codeBlockTokens = tokens.filter { it.type == TokenType.CODE_BLOCK }
        assertTrue("Should have at least one code block token", codeBlockTokens.isNotEmpty())
        
        // Check that the first code block token has content
        val codeContent = codeBlockTokens[0].groups[0]
        assertTrue("Code content should not be empty", codeContent.isNotEmpty())
    }

    @Test
    fun `test inline code tokenization`() {
        val markdown = "Use `println()` function"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(1, tokens.size)
        assertEquals(TokenType.INLINE_CODE, tokens[0].type)
        assertEquals("println()", tokens[0].groups[0])
    }

    @Test
    fun `test list tokenization`() {
        val markdown = "- First item\n- Second item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(2, tokens.size)
        tokens.forEach { token ->
            assertEquals(TokenType.LIST, token.type)
            assertTrue(token.groups[0].isNotEmpty())
        }
    }

    @Test
    fun `test blockquote tokenization`() {
        val markdown = "> This is a quote"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(1, tokens.size)
        assertEquals(TokenType.BLOCKQUOTE, tokens[0].type)
        assertEquals("This is a quote", tokens[0].groups[0])
    }

    @Test
    fun `test bold and italic tokenization`() {
        val markdown = "**bold** and *italic*"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        assertEquals(2, tokens.size)
        val boldToken = tokens.find { it.type == TokenType.BOLD }
        val italicToken = tokens.find { it.type == TokenType.ITALIC }
        
        assertNotNull(boldToken)
        assertNotNull(italicToken)
        assertEquals("bold", boldToken!!.groups[0])
        assertEquals("italic", italicToken!!.groups[0])
    }
}