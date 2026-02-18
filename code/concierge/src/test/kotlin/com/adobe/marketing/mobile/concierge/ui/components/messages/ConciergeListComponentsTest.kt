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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken
import com.adobe.marketing.mobile.concierge.utils.markdown.TokenType
import org.junit.Test
import org.junit.Assert.*

class ConciergeListComponentsTest {

    @Test
    fun `test list marker for unordered list at level 0`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "-"),
            indentationLevel = 0
        )
        
        val listMarker = getListMarker(token)
        assertEquals("• ", listMarker)
    }

    @Test
    fun `test list marker for unordered list at level 1`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "-"),
            indentationLevel = 1
        )
        
        val listMarker = getListMarker(token)
        assertEquals("◦ ", listMarker)
    }

    @Test
    fun `test list marker for unordered list at level 2`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "-"),
            indentationLevel = 2
        )
        
        val listMarker = getListMarker(token)
        assertEquals("• ", listMarker)
    }

    @Test
    fun `test list marker for unordered list at level 3`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "-"),
            indentationLevel = 3
        )
        
        val listMarker = getListMarker(token)
        assertEquals("◦ ", listMarker)
    }

    @Test
    fun `test list marker for ordered list at level 0`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "1."),
            indentationLevel = 0
        )
        
        val listMarker = getListMarker(token)
        assertEquals("1.", listMarker)
    }

    @Test
    fun `test list marker for ordered list at level 1`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "2."),
            indentationLevel = 1
        )
        
        val listMarker = getListMarker(token)
        assertEquals("2.", listMarker)
    }

    @Test
    fun `test list marker for bullet point at level 0`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "•"),
            indentationLevel = 0
        )
        
        val listMarker = getListMarker(token)
        assertEquals("• ", listMarker)
    }

    @Test
    fun `test list marker for bullet point at level 1`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content", "•"),
            indentationLevel = 1
        )
        
        val listMarker = getListMarker(token)
        assertEquals("◦ ", listMarker)
    }

    @Test
    fun `test list marker alternation pattern`() {
        // Test the alternating pattern: • (even levels), ◦ (odd levels)
        val levels = listOf(0, 1, 2, 3, 4, 5)
        val expectedMarkers = listOf("• ", "◦ ", "• ", "◦ ", "• ", "◦ ")
        
        levels.forEachIndexed { index, level ->
            val token = MarkdownToken(
                type = TokenType.LIST,
                start = 0,
                end = 10,
                groups = listOf("Item content", "-"),
                indentationLevel = level
            )
            
            val listMarker = getListMarker(token)
            assertEquals("Level $level should have marker ${expectedMarkers[index]}", 
                expectedMarkers[index], listMarker)
        }
    }

    @Test
    fun `test list marker with different ordered list numbers`() {
        val orderedNumbers = listOf("1.", "2.", "10.", "99.")
        
        orderedNumbers.forEach { number ->
            val token = MarkdownToken(
                type = TokenType.LIST,
                start = 0,
                end = 10,
                groups = listOf("Item content", number),
                indentationLevel = 0
            )
            
            val listMarker = getListMarker(token)
            assertEquals("Ordered list marker should preserve number", number, listMarker)
        }
    }

    @Test
    fun `test list marker with empty groups`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = emptyList(),
            indentationLevel = 0
        )
        
        val listMarker = getListMarker(token)
        assertEquals("• ", listMarker) // Default marker when groups is empty
    }

    @Test
    fun `test list marker with single group`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Item content"),
            indentationLevel = 0
        )
        
        val listMarker = getListMarker(token)
        assertEquals("• ", listMarker) // Default marker when only one group
    }

    @Test
    fun `test list spacing constants`() {
        // Test that the spacing constants are properly defined
        assertNotNull("BASE_INDENTATION should be defined", ListSpacing.BASE_INDENTATION)
        assertNotNull("INDENTATION_PER_LEVEL should be defined", ListSpacing.INDENTATION_PER_LEVEL)
        assertNotNull("END_PADDING should be defined", ListSpacing.END_PADDING)
        
        // Test that values are reasonable
        assertTrue("BASE_INDENTATION should be positive", ListSpacing.BASE_INDENTATION.value > 0)
        assertTrue("INDENTATION_PER_LEVEL should be positive", ListSpacing.INDENTATION_PER_LEVEL > 0)
        assertTrue("END_PADDING should be positive", ListSpacing.END_PADDING.value > 0)
    }

    @Test
    fun `test list item content extraction`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("This is the content", "-"),
            indentationLevel = 0
        )
        
        val content = getListItemContent(token)
        assertEquals("This is the content", content)
    }

    @Test
    fun `test list item content with empty groups`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = emptyList(),
            indentationLevel = 0
        )
        
        val content = getListItemContent(token)
        assertEquals("", content)
    }

    @Test
    fun `test list item content with single group`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Only content"),
            indentationLevel = 0
        )
        
        val content = getListItemContent(token)
        assertEquals("Only content", content)
    }

    @Test
    fun `test list item content with multiple groups`() {
        val token = MarkdownToken(
            type = TokenType.LIST,
            start = 0,
            end = 10,
            groups = listOf("Content", "1.", "Extra"),
            indentationLevel = 0
        )
        
        val content = getListItemContent(token)
        assertEquals("Content", content) // Should take first group
    }

    // Helper functions to test the private logic from ConciergeListComponents
    // These mirror the logic in the actual component for testing purposes
    
    private fun getListMarker(token: MarkdownToken): String {
        val marker = token.groups.getOrNull(1) ?: "•"
        return if (marker.matches(Regex("\\d+\\."))) {
            // For numbered lists, keep the number and period
            marker
        } else {
            // For unordered lists, alternate between filled and unfilled bullet points
            val bulletPoint = if (token.indentationLevel % 2 == 0) "•" else "◦"
            "$bulletPoint "
        }
    }
    
    private fun getListItemContent(token: MarkdownToken): String {
        return token.groups.firstOrNull() ?: ""
    }
}
