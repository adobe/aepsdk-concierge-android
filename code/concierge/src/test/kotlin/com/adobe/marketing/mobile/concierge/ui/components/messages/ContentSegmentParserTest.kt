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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import com.adobe.marketing.mobile.concierge.utils.markdown.MarkdownToken
import com.adobe.marketing.mobile.concierge.utils.markdown.TokenType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ContentSegmentParser.createSegments].
 */
class ContentSegmentParserTest {

    // Helper to create a minimal list MarkdownToken for testing.
    private fun listToken(start: Int, end: Int): MarkdownToken =
        MarkdownToken(type = TokenType.LIST, start = start, end = end, groups = emptyList())

    // -----------------------------------------------------------------------
    // Empty / no-list cases
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments with no list tokens returns single Text segment`() {
        val text = "Hello world"
        val segments = ContentSegmentParser.createSegments(text, emptyList())

        assertEquals(1, segments.size)
        val segment = segments[0] as ContentSegment.Text
        assertEquals(text, segment.content)
        assertEquals(0, segment.startIndex)
        assertEquals(text.length, segment.endIndex)
    }

    @Test
    fun `createSegments with empty text and no list tokens returns one Text segment`() {
        val segments = ContentSegmentParser.createSegments("", emptyList())

        assertEquals(1, segments.size)
        val segment = segments[0] as ContentSegment.Text
        assertEquals("", segment.content)
    }

    // -----------------------------------------------------------------------
    // Single list token positioning
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments with list token at very start returns only List segment`() {
        val text = "- item one"
        val token = listToken(start = 0, end = text.length)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        assertEquals(1, segments.size)
        assertTrue(segments[0] is ContentSegment.List)
    }

    @Test
    fun `createSegments with list token at end returns Text then List`() {
        val text = "Intro text\n- item"
        val token = listToken(start = 11, end = text.length)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        assertEquals(2, segments.size)
        assertTrue(segments[0] is ContentSegment.Text)
        assertTrue(segments[1] is ContentSegment.List)
        assertEquals("Intro text", (segments[0] as ContentSegment.Text).content)
    }

    @Test
    fun `createSegments with list token in middle returns Text List Text`() {
        val text = "Before\n- item\nAfter"
        val token = listToken(start = 7, end = 14)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        assertEquals(3, segments.size)
        assertTrue(segments[0] is ContentSegment.Text)
        assertTrue(segments[1] is ContentSegment.List)
        assertTrue(segments[2] is ContentSegment.Text)
        assertEquals("Before", (segments[0] as ContentSegment.Text).content)
        assertEquals("After", (segments[2] as ContentSegment.Text).content)
    }

    // -----------------------------------------------------------------------
    // Multiple list tokens
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments with multiple consecutive list tokens returns all List segments`() {
        val text = "- item one\n- item two\n- item three"
        val tokens = listOf(
            listToken(start = 0, end = 10),
            listToken(start = 11, end = 21),
            listToken(start = 22, end = text.length)
        )
        val segments = ContentSegmentParser.createSegments(text, tokens)

        // Whitespace-only gaps between list items are trimmed and suppressed
        val listSegments = segments.filterIsInstance<ContentSegment.List>()
        assertEquals(3, listSegments.size)
    }

    @Test
    fun `createSegments with text between list tokens returns interleaved segments`() {
        val text = "Intro\n- first item\nMiddle\n- second item\nOutro"
        val tokens = listOf(
            listToken(start = 6, end = 18),
            listToken(start = 26, end = 39)
        )
        val segments = ContentSegmentParser.createSegments(text, tokens)

        val textSegments = segments.filterIsInstance<ContentSegment.Text>()
        val listSegments = segments.filterIsInstance<ContentSegment.List>()
        assertEquals(3, textSegments.size)
        assertEquals(2, listSegments.size)
    }

    // -----------------------------------------------------------------------
    // Token sorting
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments sorts tokens by start index when provided out of order`() {
        val text = "A\n- first\nB\n- second"
        val tokens = listOf(
            listToken(start = 12, end = text.length), // second token provided first
            listToken(start = 2, end = 9)              // first token provided second
        )
        val segments = ContentSegmentParser.createSegments(text, tokens)

        // Regardless of input order, first segment should be Text "A"
        assertTrue(segments[0] is ContentSegment.Text)
        assertEquals("A", (segments[0] as ContentSegment.Text).content)
    }

    // -----------------------------------------------------------------------
    // Whitespace trimming
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments trims whitespace-only text before list token`() {
        val text = "   \n- item"
        val token = listToken(start = 4, end = text.length)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        // The text before the token is only whitespace; after trim it's empty → no Text segment
        assertEquals(1, segments.size)
        assertTrue(segments[0] is ContentSegment.List)
    }

    @Test
    fun `createSegments trims whitespace-only trailing text after last list token`() {
        val text = "- item\n   "
        val token = listToken(start = 0, end = 6)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        // Trailing text is only whitespace → no Text segment appended
        assertEquals(1, segments.size)
        assertTrue(segments[0] is ContentSegment.List)
    }

    // -----------------------------------------------------------------------
    // List segment wraps the token
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments wraps each token in a singleton List segment`() {
        val text = "Text\n- item"
        val token = listToken(start = 5, end = text.length)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        val listSegment = segments.filterIsInstance<ContentSegment.List>().first()
        assertEquals(1, listSegment.tokens.size)
        assertEquals(token, listSegment.tokens[0])
    }

    // -----------------------------------------------------------------------
    // Edge cases
    // -----------------------------------------------------------------------

    @Test
    fun `createSegments with empty text and list tokens returns only List segment`() {
        // A token spanning position 0–0 in empty text
        val token = listToken(start = 0, end = 0)
        val segments = ContentSegmentParser.createSegments("", listOf(token))

        // No text before or after → only the List segment
        assertEquals(1, segments.size)
        assertTrue(segments[0] is ContentSegment.List)
    }

    @Test
    fun `createSegments Text segment preserves start and end indices`() {
        val text = "Hello\n- item"
        val token = listToken(start = 6, end = text.length)
        val segments = ContentSegmentParser.createSegments(text, listOf(token))

        val textSegment = segments[0] as ContentSegment.Text
        assertEquals(0, textSegment.startIndex)
        assertEquals(6, textSegment.endIndex)
    }
}
