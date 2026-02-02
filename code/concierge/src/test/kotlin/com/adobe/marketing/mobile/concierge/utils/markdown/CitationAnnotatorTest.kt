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

package com.adobe.marketing.mobile.concierge.utils.markdown

import com.adobe.marketing.mobile.concierge.network.Citation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationAnnotatorTest {

    // ========== AnnotatedText Data Class Tests ==========

    @Test
    fun `AnnotatedText creates with text and empty sources`() {
        // Given
        val text = "Sample text"
        val sources = emptyList<Citation>()

        // When
        val annotatedText = AnnotatedText(text, sources)

        // Then
        assertEquals(text, annotatedText.text)
        assertTrue(annotatedText.uniqueSources.isEmpty())
    }

    @Test
    fun `AnnotatedText creates with text and sources`() {
        // Given
        val text = "Text with citations"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1)
        )

        // When
        val annotatedText = AnnotatedText(text, sources)

        // Then
        assertEquals(text, annotatedText.text)
        assertEquals(1, annotatedText.uniqueSources.size)
        assertEquals("Source 1", annotatedText.uniqueSources[0].title)
    }

    // ========== CitationAnnotator.annotateText() Tests ==========

    @Test
    fun `annotateText returns original text when sources list is empty`() {
        // Given
        val text = "This is a simple text without citations."
        val sources = emptyList<Citation>()

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text)
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText filters out sources without citation numbers`() {
        // Given
        val text = "Sample text."
        val sources = listOf(
            Citation("Valid", "http://example.com/valid", citationNumber = 1, startIndex = 0, endIndex = 6),
            Citation("Invalid", "http://example.com/invalid", citationNumber = null, startIndex = 7, endIndex = 11)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertTrue(result.text.contains("[^1]"))
        assertEquals(1, result.uniqueSources.size)
        assertEquals("Valid", result.uniqueSources[0].title)
    }

    @Test
    fun `annotateText filters out sources without startIndex`() {
        // Given
        val text = "Sample text."
        val sources = listOf(
            Citation("No Start", "http://example.com", citationNumber = 1, startIndex = null, endIndex = 11)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text) // No changes
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText filters out sources without endIndex`() {
        // Given
        val text = "Sample text."
        val sources = listOf(
            Citation("No End", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = null)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text) // No changes
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText filters out sources with negative startIndex`() {
        // Given
        val text = "Sample text."
        val sources = listOf(
            Citation("Negative", "http://example.com", citationNumber = 1, startIndex = -1, endIndex = 11)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text) // No changes
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText filters out sources where endIndex is not greater than startIndex`() {
        // Given
        val text = "Sample text."
        val sources = listOf(
            Citation("Equal", "http://example.com", citationNumber = 1, startIndex = 5, endIndex = 5),
            Citation("Less", "http://example.com", citationNumber = 2, startIndex = 10, endIndex = 5)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text) // No changes
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText filters out sources where endIndex exceeds text length`() {
        // Given
        val text = "Short."
        val sources = listOf(
            Citation("Beyond", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 100)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(text, result.text) // No changes
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText inserts single citation correctly`() {
        // Given
        val text = "This is a test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 23)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("This is a test sentence[^1].", result.text)
        assertEquals(1, result.uniqueSources.size)
        assertEquals("Source 1", result.uniqueSources[0].title)
    }

    @Test
    fun `annotateText inserts multiple citations correctly`() {
        // Given
        val text = "First part. Second part. Third part."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 11),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 12, endIndex = 23),
            Citation("Source 3", "http://example.com/3", citationNumber = 3, startIndex = 24, endIndex = 36)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("First part.[^1] Second part[^2]. Third part.[^3]", result.text)
        assertEquals(3, result.uniqueSources.size)
    }

    @Test
    fun `annotateText sorts sources by startIndex in descending order for insertion`() {
        // Given
        val text = "One Two Three"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 3),
            Citation("Source 3", "http://example.com/3", citationNumber = 3, startIndex = 8, endIndex = 13),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 4, endIndex = 7)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("One[^1] Two[^2] Three[^3]", result.text)
        assertEquals(3, result.uniqueSources.size)
    }

    @Test
    fun `annotateText creates unique sources list`() {
        // Given
        val text = "Text with duplicate citations."
        val sources = listOf(
            Citation("Source 1 First", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 4),
            Citation("Source 1 Dup", "http://example.com/1-dup", citationNumber = 1, startIndex = 10, endIndex = 14),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 20, endIndex = 29)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        // Should have citations inserted at both positions
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        
        // But unique sources should only have one entry per citation number
        assertEquals(2, result.uniqueSources.size)
        assertEquals(1, result.uniqueSources[0].citationNumber)
        assertEquals(2, result.uniqueSources[1].citationNumber)
        assertEquals("Source 1 First", result.uniqueSources[0].title) // First occurrence
    }

    @Test
    fun `annotateText handles empty text`() {
        // Given
        val text = ""
        val sources = listOf(
            Citation("Source", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 0)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        // endIndex must be > startIndex, so this citation will be filtered out
        assertEquals(text, result.text)
        assertTrue(result.uniqueSources.isEmpty())
    }

    @Test
    fun `annotateText handles citation at start of text`() {
        // Given
        val text = "Beginning of text"
        val sources = listOf(
            Citation("Source", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 9)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        // Citation should be inserted at the endIndex position
        assertTrue(result.text.contains("[^1]"))
        assertEquals(1, result.uniqueSources.size)
        assertEquals("Source", result.uniqueSources[0].title)
    }

    @Test
    fun `annotateText handles citation at end of text`() {
        // Given
        val text = "End of text"
        val sources = listOf(
            Citation("Source", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 11)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("End of text[^1]", result.text)
        assertEquals(1, result.uniqueSources.size)
    }

    @Test
    fun `annotateText handles overlapping citation ranges`() {
        // Given
        val text = "Overlapping citations here."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 11),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 5, endIndex = 16)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        // Both citations should be inserted
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        assertEquals(2, result.uniqueSources.size)
    }

    @Test
    fun `annotateText handles consecutive citations`() {
        // Given
        val text = "Word"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 4),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 0, endIndex = 4)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        // Both citations should be at the same position
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        assertEquals(2, result.uniqueSources.size)
    }

    @Test
    fun `annotateText handles mixed valid and invalid sources`() {
        // Given
        val text = "Test sentence here."
        val sources = listOf(
            Citation("Valid 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 4),
            Citation("Invalid - no number", "http://example.com/2", citationNumber = null, startIndex = 5, endIndex = 13),
            Citation("Valid 2", "http://example.com/3", citationNumber = 2, startIndex = 14, endIndex = 19),
            Citation("Invalid - bad range", "http://example.com/4", citationNumber = 3, startIndex = 100, endIndex = 200)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        assertEquals(2, result.uniqueSources.size)
    }

    @Test
    fun `annotateText handles large citation numbers`() {
        // Given
        val text = "Text with large number."
        val sources = listOf(
            Citation("Source 999", "http://example.com", citationNumber = 999, startIndex = 0, endIndex = 23)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("Text with large number.[^999]", result.text)
        assertEquals(1, result.uniqueSources.size)
        assertEquals(999, result.uniqueSources[0].citationNumber)
    }

    @Test
    fun `annotateText preserves citation properties in unique sources`() {
        // Given
        val text = "Sentence"
        val citation = Citation(
            title = "Important Source",
            url = "https://important.com/doc",
            citationNumber = 5,
            startIndex = 0,
            endIndex = 8
        )
        val sources = listOf(citation)

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(1, result.uniqueSources.size)
        assertEquals("Important Source", result.uniqueSources[0].title)
        assertEquals("https://important.com/doc", result.uniqueSources[0].url)
        assertEquals(5, result.uniqueSources[0].citationNumber)
    }

    @Test
    fun `annotateText with markdown formatting`() {
        // Given
        val text = "**Bold text** with *italic* and `code`."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 13),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 25, endIndex = 33)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        assertEquals(2, result.uniqueSources.size)
    }

    @Test
    fun `annotateText with special characters in text`() {
        // Given
        val text = "Special: @#$%^&*() chars!"
        val sources = listOf(
            Citation("Source", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 24)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals("Special: @#$%^&*() chars[^1]!", result.text)
        assertEquals(1, result.uniqueSources.size)
    }

    @Test
    fun `annotateText with Unicode characters`() {
        // Given
        val text = "Unicode: 你好 café ñ"
        val sources = listOf(
            Citation("Source", "http://example.com", citationNumber = 1, startIndex = 0, endIndex = 8)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("你好"))
        assertEquals(1, result.uniqueSources.size)
    }

    @Test
    fun `annotateText with multiline text`() {
        // Given
        val text = "First line\nSecond line\nThird line"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 0, endIndex = 10),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 11, endIndex = 22)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertTrue(result.text.contains("[^1]"))
        assertTrue(result.text.contains("[^2]"))
        assertTrue(result.text.contains("\n"))
        assertEquals(2, result.uniqueSources.size)
    }

    @Test
    fun `annotateText returns sorted unique sources by citation number`() {
        // Given
        val text = "Text"
        val sources = listOf(
            Citation("Source 5", "http://example.com/5", citationNumber = 5, startIndex = 0, endIndex = 1),
            Citation("Source 1", "http://example.com/1", citationNumber = 1, startIndex = 1, endIndex = 2),
            Citation("Source 3", "http://example.com/3", citationNumber = 3, startIndex = 2, endIndex = 3),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, startIndex = 3, endIndex = 4)
        )

        // When
        val result = CitationAnnotator.annotateText(text, sources)

        // Then
        assertEquals(4, result.uniqueSources.size)
        assertEquals(1, result.uniqueSources[0].citationNumber)
        assertEquals(2, result.uniqueSources[1].citationNumber)
        assertEquals(3, result.uniqueSources[2].citationNumber)
        assertEquals(5, result.uniqueSources[3].citationNumber)
    }
}
