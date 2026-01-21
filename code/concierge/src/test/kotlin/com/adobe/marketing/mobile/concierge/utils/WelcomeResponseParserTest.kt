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

package com.adobe.marketing.mobile.concierge.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WelcomeResponseParserTest {

    @Test
    fun `parseWelcomeData should parse valid JSON with all fields`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "Explore what you can do with Adobe apps.",
            "welcome.subheading": "Choose an option or tell us what interests you.",
            "welcome.examples": [
                {
                    "text": "I'd like to explore templates.",
                    "image": "https://example.com/image1.png",
                    "backgroundColor": "#FF5733"
                },
                {
                    "text": "Show me trending designs.",
                    "image": "https://example.com/image2.png",
                    "backgroundColor": "#33FF57"
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Explore what you can do with Adobe apps.", result?.heading)
        assertEquals("Choose an option or tell us what interests you.", result?.subheading)
        assertEquals(2, result?.prompts?.size)
        
        val firstPrompt = result?.prompts?.get(0)
        assertEquals("I'd like to explore templates.", firstPrompt?.text)
        assertEquals("https://example.com/image1.png", firstPrompt?.imageUrl)
        assertEquals("#FF5733", firstPrompt?.backgroundColor)
    }

    @Test
    fun `parseWelcomeData should parse JSON with only heading`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "Welcome to our service"
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Welcome to our service", result?.heading)
        assertNull("Subheading should be null", result?.subheading)
        assertEquals(0, result?.prompts?.size)
    }

    @Test
    fun `parseWelcomeData should parse JSON with prompts without images`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "How can I help?",
            "welcome.examples": [
                {
                    "text": "Tell me about features"
                },
                {
                    "text": "Show pricing"
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals(2, result?.prompts?.size)
        assertEquals("Tell me about features", result?.prompts?.get(0)?.text)
        assertNull("Image URL should be null", result?.prompts?.get(0)?.imageUrl)
        assertNull("Background color should be null", result?.prompts?.get(0)?.backgroundColor)
    }

    @Test
    fun `parseWelcomeData should return null for empty JSON`() {
        // Arrange
        val json = ""

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNull("Result should be null for empty JSON", result)
    }

    @Test
    fun `parseWelcomeData should return null for blank JSON`() {
        // Arrange
        val json = "   \n\t  "

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNull("Result should be null for blank JSON", result)
    }

    @Test
    fun `parseWelcomeData should return null for invalid JSON`() {
        // Arrange
        val json = "{ invalid json structure"

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNull("Result should be null for invalid JSON", result)
    }

    @Test
    fun `parseWelcomeData should handle empty examples array`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "Welcome",
            "welcome.examples": []
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals(0, result?.prompts?.size)
    }

    @Test
    fun `parseWelcomeData should skip prompts without text`() {
        // Arrange
        val json = """
        {
            "welcome.examples": [
                {
                    "image": "https://example.com/image.png"
                },
                {
                    "text": "Valid prompt"
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals(1, result?.prompts?.size)
        assertEquals("Valid prompt", result?.prompts?.get(0)?.text)
    }

    @Test
    fun `parseWelcomeData should handle empty string values`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "",
            "welcome.subheading": "",
            "welcome.examples": [
                {
                    "text": "",
                    "image": "",
                    "backgroundColor": ""
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertNull("Heading should be null for empty string", result?.heading)
        assertNull("Subheading should be null for empty string", result?.subheading)
        assertEquals(0, result?.prompts?.size) // Empty text prompts are filtered out
    }

    @Test
    fun `parseWelcomeData should handle mixed valid and invalid prompts`() {
        // Arrange
        val json = """
        {
            "welcome.examples": [
                {
                    "text": "First prompt",
                    "image": "https://example.com/1.png"
                },
                {
                    "image": "https://example.com/2.png"
                },
                {
                    "text": "Second prompt"
                },
                {
                    "text": ""
                },
                {
                    "text": "Third prompt",
                    "backgroundColor": "#123456"
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals(3, result?.prompts?.size)
        assertEquals("First prompt", result?.prompts?.get(0)?.text)
        assertEquals("Second prompt", result?.prompts?.get(1)?.text)
        assertEquals("Third prompt", result?.prompts?.get(2)?.text)
    }

    @Test
    fun `parseWelcomeData should return empty WelcomeData for empty object`() {
        // Arrange
        val json = "{}"

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertNull("Heading should be null", result?.heading)
        assertNull("Subheading should be null", result?.subheading)
        assertEquals(0, result?.prompts?.size)
    }

    @Test
    fun `parseWelcomeData should handle special characters in text`() {
        // Arrange
        val json = """
        {
            "welcome.heading": "Welcome to \"Adobe\" Apps!",
            "welcome.subheading": "Let's get started & explore",
            "welcome.examples": [
                {
                    "text": "Show me templates with 'quotes' & special chars"
                }
            ]
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Welcome to \"Adobe\" Apps!", result?.heading)
        assertEquals("Let's get started & explore", result?.subheading)
        assertEquals(1, result?.prompts?.size)
        assertEquals("Show me templates with 'quotes' & special chars", result?.prompts?.get(0)?.text)
    }

    @Test
    fun `parseWelcomeData should handle unexpected JSON types gracefully`() {
        // Arrange - welcome.examples is a string instead of array
        val json = """
        {
            "welcome.heading": "Welcome",
            "welcome.examples": "not an array"
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertEquals("Welcome", result?.heading)
        assertEquals(0, result?.prompts?.size)
    }

    @Test
    fun `parseWelcomeData should handle null values in JSON`() {
        // Arrange
        val json = """
        {
            "welcome.heading": null,
            "welcome.subheading": "Test",
            "welcome.examples": null
        }
        """.trimIndent()

        // Act
        val result = WelcomeResponseParser.parseWelcomeData(json)

        // Assert
        assertNotNull("Result should not be null", result)
        assertNull("Heading should be null", result?.heading)
        assertEquals("Test", result?.subheading)
        assertEquals(0, result?.prompts?.size)
    }
}
