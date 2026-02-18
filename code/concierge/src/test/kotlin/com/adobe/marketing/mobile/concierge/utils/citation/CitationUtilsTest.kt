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

package com.adobe.marketing.mobile.concierge.utils.citation

import com.adobe.marketing.mobile.concierge.network.Citation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CitationUtilsTest {

    @Test
    fun `createUniqueSources should return empty list for empty input`() {
        val citations = emptyList<Citation>()

        val result = CitationUtils.createUniqueSources(citations)

        assertTrue("Result should be empty", result.isEmpty())
    }

    @Test
    fun `createUniqueSources should filter out citations without citation numbers`() {
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Source 2", "http://example.com/2", citationNumber = null),
            Citation("Source 3", "http://example.com/3", citationNumber = 3),
            Citation("Source 4", "http://example.com/4", citationNumber = null)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertEquals(2, result.size)
        assertEquals(1, result[0].citationNumber)
        assertEquals(3, result[1].citationNumber)
    }

    @Test
    fun `createUniqueSources should deduplicate by citation number`() {
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Duplicate 1", "http://example.com/duplicate", citationNumber = 1),
            Citation("Source 2", "http://example.com/2", citationNumber = 2),
            Citation("Another Duplicate 1", "http://example.com/another", citationNumber = 1)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertEquals(2, result.size)
        assertEquals("Source 1", result[0].title) // Should take first occurrence
        assertEquals("Source 2", result[1].title)
    }

    @Test
    fun `createUniqueSources should sort by citation number`() {
        val citations = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3),
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Source 5", "http://example.com/5", citationNumber = 5),
            Citation("Source 2", "http://example.com/2", citationNumber = 2)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertEquals(4, result.size)
        assertEquals(1, result[0].citationNumber)
        assertEquals(2, result[1].citationNumber)
        assertEquals(3, result[2].citationNumber)
        assertEquals(5, result[3].citationNumber)
    }

    @Test
    fun `createUniqueSources should handle single citation`() {
        val citations = listOf(
            Citation("Single Source", "http://example.com/single", citationNumber = 1)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertEquals(1, result.size)
        assertEquals("Single Source", result[0].title)
    }

    @Test
    fun `getCitationSymbol should format citation number correctly`() {
        // Act & Assert
        assertEquals("[^1]", CitationUtils.getCitationSymbol(1))
        assertEquals("[^2]", CitationUtils.getCitationSymbol(2))
        assertEquals("[^10]", CitationUtils.getCitationSymbol(10))
        assertEquals("[^99]", CitationUtils.getCitationSymbol(99))
    }

    @Test
    fun `insertCitationNumbersInMarkdown should insert citations at correct positions`() {
        val text = "This is a test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 23)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("This is a test sentence[^1].", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle multiple citations`() {
        val text = "First part. Second part. Third part."
        val sources = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3, endIndex = 36),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, endIndex = 23),
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 11)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("First part.[^1] Second part[^2]. Third part.[^3]", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations without citation numbers`() {
        val text = "Test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = null, endIndex = 14)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("Test sentence.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations without endIndex`() {
        val text = "Test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = null)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("Test sentence.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations with endIndex beyond text length`() {
        val text = "Short text."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 100)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("Short text.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle empty sources list`() {
        val text = "Test sentence."
        val sources = emptyList<Citation>()

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("Test sentence.", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle empty text`() {
        val text = ""
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 0)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("[^1]", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle citation at start of text`() {
        val text = "Text"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 0)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("[^1]Text", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should maintain order with reverse-sorted sources`() {
        val text = "One Two Three"
        val sources = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3, endIndex = 13),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, endIndex = 7),
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 3)
        )

        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        assertEquals("One[^1] Two[^2] Three[^3]", result)
    }

    @Test
    fun `createUniqueSources should handle all citations with null citation numbers`() {
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = null),
            Citation("Source 2", "http://example.com/2", citationNumber = null)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertTrue("Result should be empty when all citations have null numbers", result.isEmpty())
    }

    @Test
    fun `createUniqueSources should preserve all properties of first occurrence`() {
        val citations = listOf(
            Citation("First", "http://first.com", citationNumber = 1, startIndex = 0, endIndex = 10),
            Citation("Duplicate", "http://duplicate.com", citationNumber = 1, startIndex = 20, endIndex = 30)
        )

        val result = CitationUtils.createUniqueSources(citations)

        assertEquals(1, result.size)
        assertEquals("First", result[0].title)
        assertEquals("http://first.com", result[0].url)
        assertEquals(0, result[0].startIndex)
        assertEquals(10, result[0].endIndex)
    }
}
