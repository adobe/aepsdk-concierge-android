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

/**
 * Tests for the MarkdownParser functionality
 */
class MarkdownParserTest {

    @Test
    fun `test bold text parsing`() {
        val markdown = "This is **bold** text"
        val result = MarkdownParser.parse(markdown)
        
        // Check that the text contains the bold content
        assertTrue(result.text.contains("bold"))
    }

    @Test
    fun `test italic text parsing`() {
        val markdown = "This is *italic* text"
        val result = MarkdownParser.parse(markdown)
        
        assertTrue(result.text.contains("italic"))
    }

    @Test
    fun `test heading parsing`() {
        val markdown = "# Main Heading\n## Sub Heading"
        val result = MarkdownParser.parse(markdown)
        
        assertTrue(result.text.contains("Main Heading"))
        assertTrue(result.text.contains("Sub Heading"))
    }

    @Test
    fun `test link parsing`() {
        val markdown = "[Click here](https://example.com)"
        val result = MarkdownParser.parse(markdown)
        
        assertTrue(result.text.contains("Click here"))
        
        // Check that string annotations are added for links
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse(annotations.isEmpty())
        assertEquals("https://example.com", annotations[0].item)
    }

    @Test
    fun `test code block parsing`() {
        val markdown = "```\nfun test() {}\n\n```"
        val result = MarkdownParser.parse(markdown)
        
        assertTrue(result.text.contains("fun test() {}"))
    }

    @Test
    fun `test inline code parsing`() {
        val markdown = "Use `println()` function"
        val result = MarkdownParser.parse(markdown)

        assertTrue(result.text.contains("println()"))
    }

    @Test
    fun `test list parsing`() {
        val markdown = "- First item\n- Second item"
        val result = MarkdownParser.parse(markdown)

        assertTrue(result.text.contains("• First item"))
        assertTrue(result.text.contains("• Second item"))
    }

    @Test
    fun `test blockquote parsing`() {
        val markdown = "> This is a quote"
        val result = MarkdownParser.parse(markdown)

        assertTrue(result.text.contains("This is a quote"))
    }

    @Test
    fun `test mixed markdown elements`() {
        val markdown = """
            # Title
            
            This is **bold** and *italic* text.
            
            [Link](https://example.com)
            
            - List item
            - Another item
            
            > Quote here
            
            `inline code`
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)

        assertTrue(result.text.contains("Title"))
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("Link"))
        assertTrue(result.text.contains("List item"))
        assertTrue(result.text.contains("Quote here"))
        assertTrue(result.text.contains("inline code"))
    }

    @Test
    fun `test empty string`() {
        val result = MarkdownParser.parse("")
        assertEquals("", result.text)
    }

    @Test
    fun `test plain text without markdown`() {
        val plainText = "This is just plain text without any markdown formatting."
        val result = MarkdownParser.parse(plainText)

        assertEquals(plainText, result.text)
    }

    @Test
    fun `test real-world beach destinations message`() {
        val beachMessage = """
            Looking for a beach trip for your summer getaway? Here are some fantastic beach destinations to consider:

            1. **[Miami](https://www.southwestvacations.com/destinations/united-states/miami-vacation-packages-mf1)** - Known for its vibrant nightlife and beautiful sandy shores, Miami is perfect for those who want to soak up the sun and enjoy a lively atmosphere.

            2. **[Fort Lauderdale](https://www.southwestvacations.com/destinations/united-states/fort-lauderdale-vacation-packages-fll)** - Famous for its boating canals and stunning beaches, Fort Lauderdale offers a relaxed beach vibe along with plenty of water activities.

            3. **[Destin](https://www.southwestvacations.com/destinations/united-states/south-walton-beach-destin-vacation-packages-ec1)** - Renowned for its emerald waters and sugar-white sand beaches, Destin is a great choice for families and beach lovers looking for fun in the sun.

            4. **[Pensacola](https://www.southwestvacations.com/destinations/united-states/pensacola-vacation-packages-pns)** - With its rich history and beautiful beaches, Pensacola provides a mix of cultural attractions and outdoor activities, making it a diverse getaway option.

            5. **[Naples](https://www.southwestvacations.com/destinations/united-states/naples-vacation-packages-na3/)** - Enjoy over ten miles of stunning white sand beaches and clear Gulf waters, along with a vibrant dining scene and family-friendly activities.

            Each of these destinations offers a unique beach experience, so you can find the perfect spot for your summer escape! If you'd like more information on any of these locations, just let me know!
        """.trimIndent()

        val result = MarkdownParser.parse(beachMessage)

        // Verify bold text is parsed (destinations like Miami, Fort Lauderdale, etc.)
        assertTrue(result.text.contains("Miami"))
        assertTrue(result.text.contains("Fort Lauderdale"))
        assertTrue(result.text.contains("Destin"))
        assertTrue(result.text.contains("Pensacola"))
        assertTrue(result.text.contains("Naples"))
        
        // Verify links are properly parsed and contain URL annotations
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse("Should have URL annotations for links", annotations.isEmpty())
        
        // Verify specific URLs are present
        val urls = annotations.map { it.item }
        assertTrue("Should contain Miami URL", urls.any { it.contains("miami-vacation-packages") })
        assertTrue("Should contain Fort Lauderdale URL", urls.any { it.contains("fort-lauderdale-vacation-packages") })
        assertTrue("Should contain Destin URL", urls.any { it.contains("destin-vacation-packages") })
        assertTrue("Should contain Pensacola URL", urls.any { it.contains("pensacola-vacation-packages") })
        assertTrue("Should contain Naples URL", urls.any { it.contains("naples-vacation-packages") })
        
        // Verify the text flows naturally without markdown syntax
        assertFalse("Should not contain raw markdown syntax", result.text.contains("**[Miami]("))
        assertFalse("Should not contain raw markdown syntax", result.text.contains("**["))
        assertFalse("Should not contain raw markdown syntax", result.text.contains(")**"))
    }

    @Test
    fun `test simple bold link combination`() {
        val simpleMessage = "**[Miami](https://example.com)**"
        val result = MarkdownParser.parse(simpleMessage)
        
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        
        // Should have URL annotation
        assertFalse("Should have URL annotation for simple bold link", annotations.isEmpty())
        assertEquals("https://example.com", annotations[0].item)
        
        // Should contain the text without markdown
        assertTrue(result.text.contains("Miami"))
        assertFalse(result.text.contains("**[Miami]("))
    }
}
