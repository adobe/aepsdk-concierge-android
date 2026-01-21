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
        // Arrange
        val citations = emptyList<Citation>()

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertTrue("Result should be empty", result.isEmpty())
    }

    @Test
    fun `createUniqueSources should filter out citations without citation numbers`() {
        // Arrange
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Source 2", "http://example.com/2", citationNumber = null),
            Citation("Source 3", "http://example.com/3", citationNumber = 3),
            Citation("Source 4", "http://example.com/4", citationNumber = null)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertEquals(2, result.size)
        assertEquals(1, result[0].citationNumber)
        assertEquals(3, result[1].citationNumber)
    }

    @Test
    fun `createUniqueSources should deduplicate by citation number`() {
        // Arrange
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Duplicate 1", "http://example.com/duplicate", citationNumber = 1),
            Citation("Source 2", "http://example.com/2", citationNumber = 2),
            Citation("Another Duplicate 1", "http://example.com/another", citationNumber = 1)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertEquals(2, result.size)
        assertEquals("Source 1", result[0].title) // Should take first occurrence
        assertEquals("Source 2", result[1].title)
    }

    @Test
    fun `createUniqueSources should sort by citation number`() {
        // Arrange
        val citations = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3),
            Citation("Source 1", "http://example.com/1", citationNumber = 1),
            Citation("Source 5", "http://example.com/5", citationNumber = 5),
            Citation("Source 2", "http://example.com/2", citationNumber = 2)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertEquals(4, result.size)
        assertEquals(1, result[0].citationNumber)
        assertEquals(2, result[1].citationNumber)
        assertEquals(3, result[2].citationNumber)
        assertEquals(5, result[3].citationNumber)
    }

    @Test
    fun `createUniqueSources should handle single citation`() {
        // Arrange
        val citations = listOf(
            Citation("Single Source", "http://example.com/single", citationNumber = 1)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
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
        // Arrange
        val text = "This is a test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 23)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("This is a test sentence[^1].", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle multiple citations`() {
        // Arrange
        val text = "First part. Second part. Third part."
        val sources = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3, endIndex = 36),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, endIndex = 23),
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 11)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("First part.[^1] Second part[^2]. Third part.[^3]", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations without citation numbers`() {
        // Arrange
        val text = "Test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = null, endIndex = 14)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("Test sentence.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations without endIndex`() {
        // Arrange
        val text = "Test sentence."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = null)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("Test sentence.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should skip citations with endIndex beyond text length`() {
        // Arrange
        val text = "Short text."
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 100)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("Short text.", result) // No changes
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle empty sources list`() {
        // Arrange
        val text = "Test sentence."
        val sources = emptyList<Citation>()

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("Test sentence.", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle empty text`() {
        // Arrange
        val text = ""
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 0)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("[^1]", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should handle citation at start of text`() {
        // Arrange
        val text = "Text"
        val sources = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 0)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("[^1]Text", result)
    }

    @Test
    fun `insertCitationNumbersInMarkdown should maintain order with reverse-sorted sources`() {
        // Arrange
        val text = "One Two Three"
        val sources = listOf(
            Citation("Source 3", "http://example.com/3", citationNumber = 3, endIndex = 13),
            Citation("Source 2", "http://example.com/2", citationNumber = 2, endIndex = 7),
            Citation("Source 1", "http://example.com/1", citationNumber = 1, endIndex = 3)
        )

        // Act
        val result = CitationUtils.insertCitationNumbersInMarkdown(text, sources)

        // Assert
        assertEquals("One[^1] Two[^2] Three[^3]", result)
    }

    @Test
    fun `createUniqueSources should handle all citations with null citation numbers`() {
        // Arrange
        val citations = listOf(
            Citation("Source 1", "http://example.com/1", citationNumber = null),
            Citation("Source 2", "http://example.com/2", citationNumber = null)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertTrue("Result should be empty when all citations have null numbers", result.isEmpty())
    }

    @Test
    fun `createUniqueSources should preserve all properties of first occurrence`() {
        // Arrange
        val citations = listOf(
            Citation("First", "http://first.com", citationNumber = 1, startIndex = 0, endIndex = 10),
            Citation("Duplicate", "http://duplicate.com", citationNumber = 1, startIndex = 20, endIndex = 30)
        )

        // Act
        val result = CitationUtils.createUniqueSources(citations)

        // Assert
        assertEquals(1, result.size)
        assertEquals("First", result[0].title)
        assertEquals("http://first.com", result[0].url)
        assertEquals(0, result[0].startIndex)
        assertEquals(10, result[0].endIndex)
    }
}
