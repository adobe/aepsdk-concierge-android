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

import org.junit.Test
import org.junit.Assert.*

class MarkdownTokenizerTest {

    @Test
    fun `test tokenize simple unordered list`() {
        val markdown = "- First item\n- Second item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Check first item
        val firstItem = listTokens[0]
        assertEquals("First item", firstItem.groups[0])
        assertEquals("-", firstItem.groups[1])
        assertEquals(0, firstItem.indentationLevel)
        
        // Check second item
        val secondItem = listTokens[1]
        assertEquals("Second item", secondItem.groups[0])
        assertEquals("-", secondItem.groups[1])
        assertEquals(0, secondItem.indentationLevel)
    }

    @Test
    fun `test tokenize simple ordered list`() {
        val markdown = "1. First item\n2. Second item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Check first item
        val firstItem = listTokens[0]
        assertEquals("First item", firstItem.groups[0])
        assertEquals("1.", firstItem.groups[1])
        assertEquals(0, firstItem.indentationLevel)
        
        // Check second item
        val secondItem = listTokens[1]
        assertEquals("Second item", secondItem.groups[0])
        assertEquals("2.", secondItem.groups[1])
        assertEquals(0, secondItem.indentationLevel)
    }

    @Test
    fun `test tokenize nested unordered list`() {
        val markdown = "- First item\n  - Nested item\n- Second item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check first item (level 0)
        val firstItem = listTokens[0]
        assertEquals("First item", firstItem.groups[0])
        assertEquals("-", firstItem.groups[1])
        assertEquals(0, firstItem.indentationLevel)
        
        // Check nested item (level 1)
        val nestedItem = listTokens[1]
        assertEquals("Nested item", nestedItem.groups[0])
        assertEquals("-", nestedItem.groups[1])
        assertEquals(1, nestedItem.indentationLevel)
        
        // Check second item (level 0)
        val secondItem = listTokens[2]
        assertEquals("Second item", secondItem.groups[0])
        assertEquals("-", secondItem.groups[1])
        assertEquals(0, secondItem.indentationLevel)
    }

    @Test
    fun `test tokenize deeply nested list`() {
        val markdown = "- Level 0\n  - Level 1\n    - Level 2\n      - Level 3"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(4, listTokens.size)
        
        // Check indentation levels
        assertEquals(0, listTokens[0].indentationLevel)
        assertEquals(1, listTokens[1].indentationLevel)
        assertEquals(2, listTokens[2].indentationLevel)
        assertEquals(3, listTokens[3].indentationLevel)
        
        // Check content
        assertEquals("Level 0", listTokens[0].groups[0])
        assertEquals("Level 1", listTokens[1].groups[0])
        assertEquals("Level 2", listTokens[2].groups[0])
        assertEquals("Level 3", listTokens[3].groups[0])
    }

    @Test
    fun `test tokenize list with bullet points`() {
        val markdown = "• First item\n• Second item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Check first item
        val firstItem = listTokens[0]
        assertEquals("First item", firstItem.groups[0])
        assertEquals("•", firstItem.groups[1])
        assertEquals(0, firstItem.indentationLevel)
        
        // Check second item
        val secondItem = listTokens[1]
        assertEquals("Second item", secondItem.groups[0])
        assertEquals("•", secondItem.groups[1])
        assertEquals(0, secondItem.indentationLevel)
    }

    @Test
    fun `test tokenize mixed list types`() {
        val markdown = "1. Ordered item\n- Unordered item\n2. Another ordered item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check ordered items
        assertEquals("1.", listTokens[0].groups[1])
        assertEquals("2.", listTokens[2].groups[1])
        
        // Check unordered item
        assertEquals("-", listTokens[1].groups[1])
    }

    @Test
    fun `test tokenize list with markdown content`() {
        val markdown = "- This is **bold** text\n- This has *italic* text\n- This has `code`"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check that list content is captured correctly
        assertEquals("This is **bold** text", listTokens[0].groups[0])
        assertEquals("This has *italic* text", listTokens[1].groups[0])
        assertEquals("This has `code`", listTokens[2].groups[0])
        
        // Check that inline markdown tokens are also captured
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        val codeTokens = tokens.filter { it.type == TokenType.INLINE_CODE }
        
        assertEquals(1, boldTokens.size)
        assertEquals(1, italicTokens.size)
        assertEquals(1, codeTokens.size)
    }

    @Test
    fun `test tokenize list with links`() {
        val markdown = "- [Link text](https://example.com)\n- Another item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        val linkTokens = tokens.filter { it.type == TokenType.LINK }
        
        assertEquals(2, listTokens.size)
        assertEquals(1, linkTokens.size)
        
        // Check that list content includes the link
        assertEquals("[Link text](https://example.com)", listTokens[0].groups[0])
        assertEquals("Another item", listTokens[1].groups[0])
    }

    @Test
    fun `test tokenize empty list items`() {
        val markdown = "- Empty item\n- Another item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Check that valid items are captured
        assertEquals("Empty item", listTokens[0].groups[0])
        assertEquals("Another item", listTokens[1].groups[0])
    }

    @Test
    fun `test tokenize list with extra spaces`() {
        val markdown = "  - Indented item\n    - More indented"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Check indentation levels
        assertEquals(1, listTokens[0].indentationLevel) // 2 spaces = level 1
        assertEquals(2, listTokens[1].indentationLevel) // 4 spaces = level 2
    }

    @Test
    fun `test tokenize complex nested list with mixed content`() {
        val markdown = """
            - Main item
              - Nested **bold** item
              - Nested *italic* item
                - Deeply nested `code` item
            - Another main item
        """.trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(5, listTokens.size)
        
        // Check indentation levels
        assertEquals(0, listTokens[0].indentationLevel) // Main item
        assertEquals(1, listTokens[1].indentationLevel) // Nested bold
        assertEquals(1, listTokens[2].indentationLevel) // Nested italic
        assertEquals(2, listTokens[3].indentationLevel) // Deeply nested code
        assertEquals(0, listTokens[4].indentationLevel) // Another main
        
        // Check that inline markdown is also tokenized
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        val codeTokens = tokens.filter { it.type == TokenType.INLINE_CODE }
        
        assertEquals(1, boldTokens.size)
        assertEquals(1, italicTokens.size)
        assertEquals(1, codeTokens.size)
    }

    @Test
    fun `test tokenize list with no items`() {
        val markdown = "This is not a list"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(0, listTokens.size)
    }

    @Test
    fun `test tokenize list with malformed syntax`() {
        val markdown = "-Item without space\n- Valid item"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        // Only the valid item should be tokenized
        assertEquals(1, listTokens.size)
        assertEquals("Valid item", listTokens[0].groups[0])
    }

    @Test
    fun `test tokenize list with tabs instead of spaces`() {
        val markdown = "-\tItem with tab\n- Item with space"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(2, listTokens.size)
        
        // Both should be tokenized as level 0 since tabs aren't handled for indentation
        assertEquals(0, listTokens[0].indentationLevel)
        assertEquals(0, listTokens[1].indentationLevel)
    }

    @Test
    fun `test tokenize list with multiple spaces in indentation`() {
        val markdown = "- Level 0\n    - Level 2 (4 spaces)\n  - Level 1 (2 spaces)"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check indentation levels
        assertEquals(0, listTokens[0].indentationLevel)
        assertEquals(2, listTokens[1].indentationLevel) // 4 spaces = level 2
        assertEquals(1, listTokens[2].indentationLevel) // 2 spaces = level 1
    }

    @Test
    fun `test tokenize multi-line list items`() {
        val markdown = """- First item that spans
multiple lines of text
- Second item on one line
- Third item that also
spans multiple lines""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check that multi-line content is captured
        assertEquals("First item that spans\nmultiple lines of text", listTokens[0].groups[0])
        assertEquals("Second item on one line", listTokens[1].groups[0])
        assertEquals("Third item that also\nspans multiple lines", listTokens[2].groups[0])
    }
    
    @Test
    fun `test tokenize list items separated by blank lines`() {
        val markdown = """- First item

- Second item after blank line
- Third item consecutive""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // First item should not include the blank line
        assertEquals("First item", listTokens[0].groups[0])
        assertEquals("Second item after blank line", listTokens[1].groups[0])
        assertEquals("Third item consecutive", listTokens[2].groups[0])
    }
    
    @Test
    fun `test tokenize list with bold header and continuation`() {
        val markdown = """- **Fit and Comfort:** Try shoes on with trail socks and jog in them if possible to ensure a secure, comfortable fit.""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(1, listTokens.size)
        
        // Check that the full content including text after bold is captured
        assertEquals("**Fit and Comfort:** Try shoes on with trail socks and jog in them if possible to ensure a secure, comfortable fit.", listTokens[0].groups[0])
        
        // Check that bold token is also found within the list item
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        assertEquals(1, boldTokens.size)
        assertEquals("Fit and Comfort:", boldTokens[0].groups[0])
    }
    
    @Test
    fun `test bold and italic tokens do not interfere`() {
        val markdown = "This has **bold** and *italic* text"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        
        assertEquals(1, boldTokens.size)
        assertEquals(1, italicTokens.size)
        
        assertEquals("bold", boldTokens[0].groups[0])
        assertEquals("italic", italicTokens[0].groups[0])
    }
    
    @Test
    fun `test italic does not match double asterisks`() {
        val markdown = "**Bold text** should not be matched as italic"
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val boldTokens = tokens.filter { it.type == TokenType.BOLD }
        val italicTokens = tokens.filter { it.type == TokenType.ITALIC }
        
        assertEquals(1, boldTokens.size)
        assertEquals(0, italicTokens.size)
        
        assertEquals("Bold text", boldTokens[0].groups[0])
    }
    
    @Test
    fun `test last list item with blank line and trailing text`() {
        val markdown = """- First item
- Second item
- **Last item** with content that goes on

Some trailing text after blank line""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Check that the last list item captures content up to the blank line
        assertEquals("**Last item** with content that goes on", listTokens[2].groups[0])
    }
    
    @Test
    fun `test list items separated by blank lines`() {
        val markdown = """- **First item:** Content here

- **Second item:** More content

- **Third item:** Even more content

Trailing text""".trimIndent()
        
        val tokens = MarkdownTokenizer.tokenize(markdown)
        
        val listTokens = tokens.filter { it.type == TokenType.LIST }
        assertEquals(3, listTokens.size)
        
        // Each list item should capture its full content (not cut off by blank lines between items)
        assertEquals("**First item:** Content here", listTokens[0].groups[0])
        assertEquals("**Second item:** More content", listTokens[1].groups[0])
        assertEquals("**Third item:** Even more content", listTokens[2].groups[0])
    }
}
