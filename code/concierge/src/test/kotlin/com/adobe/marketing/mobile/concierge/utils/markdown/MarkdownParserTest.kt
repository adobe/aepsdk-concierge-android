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
import androidx.compose.ui.text.font.FontWeight
import org.junit.Test
import org.junit.Assert.*

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
    fun `test markdown within list items`() {
        val markdown = """
            - This is **bold** text in a list item
            - This item has *italic* text
            - This item has `inline code` 
            - This item has a [link](https://example.com)
            - This item has **bold** and *italic* and `code` together
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that list structure is preserved with bullet points
        assertTrue(result.text.contains("• This is"))
        assertTrue(result.text.contains("• This item has"))
        
        // Check that markdown within list items is processed
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("inline code"))
        assertTrue(result.text.contains("link"))
        
        // Check that markdown syntax is removed
        assertFalse(result.text.contains("**bold**"))
        assertFalse(result.text.contains("*italic*"))
        assertFalse(result.text.contains("`inline code`"))
        assertFalse(result.text.contains("[link]("))
        
        // Check that URL annotations are added for links
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse(annotations.isEmpty())
        assertEquals("https://example.com", annotations[0].item)
        
        // Check that span styles are applied (bold, italic, code)
        assertTrue(result.spanStyles.size > 0)
    }

    @Test
    fun `test complex nested markdown`() {
        val markdown = """
            # Main Heading
            
            This is a paragraph with **bold** and *italic* text.
            
            - List item with `inline code`
            - Another item with [a link](https://example.com)
            - Item with **bold** and *italic* together
            
            > This is a blockquote with **bold** text
            
            ## Sub Heading
            
            More content here.
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that all elements are properly processed
        assertTrue(result.text.contains("Main Heading"))
        assertTrue(result.text.contains("Sub Heading"))
        assertTrue(result.text.contains("• List item with"))
        assertTrue(result.text.contains("• Another item with"))
        assertTrue(result.text.contains("• Item with"))
        assertTrue(result.text.contains("This is a blockquote with"))
        
        // Check that nested markdown is processed
        assertTrue(result.text.contains("bold"))
        assertTrue(result.text.contains("italic"))
        assertTrue(result.text.contains("inline code"))
        assertTrue(result.text.contains("a link"))
        
        // Check that markdown syntax is removed
        assertFalse("Found **bold** in result: '${result.text}'", result.text.contains("**bold**"))
        assertFalse("Found *italic* in result: '${result.text}'", result.text.contains("*italic*"))
        assertFalse("Found `inline code` in result: '${result.text}'", result.text.contains("`inline code`"))
        assertFalse("Found [a link]( in result: '${result.text}'", result.text.contains("[a link]("))
        
        // Check that URL annotations are added
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse(annotations.isEmpty())
        assertEquals("https://example.com", annotations[0].item)
        
        // Check that span styles are applied
        assertTrue(result.spanStyles.size > 0)
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
    fun `test mixed content message with a numbered list`() {
        val beachMessage = """
            Looking for a beach trip for your summer getaway? Here are some fantastic beach destinations to consider:

            1. **[Miami](https://example.com/destinations/miami)** - Known for its vibrant nightlife and beautiful sandy shores, Miami is perfect for those who want to soak up the sun and enjoy a lively atmosphere.

            2. **[Fort Lauderdale](https://example.com/destinations/fort-lauderdale)** - Famous for its boating canals and stunning beaches, Fort Lauderdale offers a relaxed beach vibe along with plenty of water activities.

            3. **[Destin](https://example.com/destinations/destin)** - Renowned for its emerald waters and sugar-white sand beaches, Destin is a great choice for families and beach lovers looking for fun in the sun.

            4. **[Pensacola](https://example.com/destinations/pensacola)** - With its rich history and beautiful beaches, Pensacola provides a mix of cultural attractions and outdoor activities, making it a diverse getaway option.

            5. **[Naples](https://example.com/destinations/naples/)** - Enjoy over ten miles of stunning white sand beaches and clear Gulf waters, along with a vibrant dining scene and family-friendly activities.

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
        assertTrue("Should contain Miami URL", urls.any { it.contains("miami") })
        assertTrue("Should contain Fort Lauderdale URL", urls.any { it.contains("fort-lauderdale") })
        assertTrue("Should contain Destin URL", urls.any { it.contains("destin") })
        assertTrue("Should contain Pensacola URL", urls.any { it.contains("pensacola") })
        assertTrue("Should contain Naples URL", urls.any { it.contains("naples") })
        
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

    @Test
    fun `test parse complex nested list with mixed content`() {
        val markdown = """
            # Travel Destinations
            
            Here are some amazing places to visit:
            
            1. **Europe**
               - Paris - Known for the Eiffel Tower
               - London - Famous for Big Ben
               - Rome - Home to the Colosseum
                 - Vatican City - Smallest country
                 - Sistine Chapel - Beautiful art
               - Barcelona - Gaudi's architecture
            
            2. **Asia**
               - Tokyo - Modern metropolis
               - Bangkok - Vibrant street life
               - Singapore - Clean and green
                 - Marina Bay Sands - Iconic hotel
                 - Gardens by the Bay - Nature park
            
            3. **Americas**
               - New York - The Big Apple
               - San Francisco - Golden Gate Bridge
               - Rio de Janeiro - Carnival city
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that heading is processed
        assertTrue(result.text.contains("Travel Destinations"))
        
        // Check that list items are processed with preserved numbers
        assertTrue(result.text.contains("1. Europe"))
        assertTrue(result.text.contains("2. Asia"))
        assertTrue(result.text.contains("3. Americas"))
        
        // Check nested items
        assertTrue(result.text.contains("• Paris"))
        assertTrue(result.text.contains("• London"))
        assertTrue(result.text.contains("• Rome"))
        assertTrue(result.text.contains("• Tokyo"))
        assertTrue(result.text.contains("• Bangkok"))
        assertTrue(result.text.contains("• New York"))
        
        // Check deeply nested items
        assertTrue(result.text.contains("• Vatican City"))
        assertTrue(result.text.contains("• Sistine Chapel"))
        assertTrue(result.text.contains("• Marina Bay Sands"))
        assertTrue(result.text.contains("• Gardens by the Bay"))
        
        // Check that bold text is processed
        assertTrue(result.text.contains("Europe"))
        assertTrue(result.text.contains("Asia"))
        assertTrue(result.text.contains("Americas"))
        
        // Check that markdown syntax is removed
        assertFalse(result.text.contains("**Europe**"))
        assertFalse(result.text.contains("**Asia**"))
        assertFalse(result.text.contains("**Americas**"))
        
        // Check that span styles are applied
        assertTrue(result.spanStyles.size > 0)
        
        val boldStyle = result.spanStyles.find { it.item.fontWeight == FontWeight.Bold }
        assertNotNull("Should have bold style", boldStyle)
    }

    @Test
    fun `test parse list with code blocks and inline code`() {
        val markdown = """
            Programming Languages:
            
            - **Python** - Great for beginners
              - Uses `print()` for output
              - Has `if __name__ == "__main__":` pattern
            - **JavaScript** - Web development
              - Uses `console.log()` for output
              - Has `const` and `let` keywords
            - **Java** - Enterprise applications
              - Uses `System.out.println()` for output
              - Has `public static void main()` method
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check list structure
        assertTrue(result.text.contains("• Python"))
        assertTrue(result.text.contains("• JavaScript"))
        assertTrue(result.text.contains("• Java"))
        
        // Check nested items
        assertTrue(result.text.contains("• Uses"))
        assertTrue(result.text.contains("• Has"))
        
        // Check that inline code is processed
        assertTrue(result.text.contains("print()"))
        assertTrue(result.text.contains("console.log()"))
        assertTrue(result.text.contains("System.out.println()"))
        
        // Check that markdown syntax is removed
        assertFalse(result.text.contains("`print()`"))
        assertFalse(result.text.contains("`console.log()`"))
        assertFalse(result.text.contains("`System.out.println()`"))
        
        // Check that code styling is applied
        val codeStyle = result.spanStyles.find { it.item.fontFamily == FontFamily.Monospace }
        assertNotNull("Should have code style", codeStyle)
    }

    @Test
    fun `test parse list with links and mixed formatting`() {
        val markdown = """
            Useful Resources:
            
            - [Official Documentation](https://docs.example.com) - Always check this first
            - [Community Forum](https://forum.example.com) - Get help from others
            - API Reference - [Link here](https://api.example.com) for details
            - Important: Read the [Getting Started Guide](https://guide.example.com)
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that basic content is present
        assertTrue("Should contain Official Documentation", result.text.contains("Official Documentation"))
        assertTrue("Should contain Community Forum", result.text.contains("Community Forum"))
        assertTrue("Should contain API Reference", result.text.contains("API Reference"))
        assertTrue("Should contain Important", result.text.contains("Important"))
        
        // Check that URL annotations are added
        val annotations = result.getStringAnnotations("URL", 0, result.length)
        assertFalse("Should have URL annotations", annotations.isEmpty())
        
        val urls = annotations.map { it.item }
        assertTrue("Should contain docs URL", urls.any { it.contains("docs.example.com") })
        assertTrue("Should contain forum URL", urls.any { it.contains("forum.example.com") })
        assertTrue("Should contain api URL", urls.any { it.contains("api.example.com") })
        assertTrue("Should contain guide URL", urls.any { it.contains("guide.example.com") })
    }

    @Test
    fun `test parse list with blockquotes and mixed elements`() {
        val markdown = """
            Project Status:
            
            - Completed Tasks
              - User authentication system
              - Database integration
              - API endpoints
                > "The API is working perfectly" - Lead Developer
            - In Progress
              - UI improvements
              - Performance optimization
            - Planned
              - Mobile app development
              - Advanced features
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that basic content is present
        assertTrue("Should contain Completed Tasks", result.text.contains("Completed Tasks"))
        assertTrue("Should contain In Progress", result.text.contains("In Progress"))
        assertTrue("Should contain Planned", result.text.contains("Planned"))
        
        // Check nested items
        assertTrue("Should contain User authentication system", result.text.contains("User authentication system"))
        assertTrue("Should contain Database integration", result.text.contains("Database integration"))
        assertTrue("Should contain API endpoints", result.text.contains("API endpoints"))
        assertTrue("Should contain UI improvements", result.text.contains("UI improvements"))
        assertTrue("Should contain Performance optimization", result.text.contains("Performance optimization"))
        assertTrue("Should contain Mobile app development", result.text.contains("Mobile app development"))
        assertTrue("Should contain Advanced features", result.text.contains("Advanced features"))
        
        // Check that blockquote is processed
        assertTrue(result.text.contains("The API is working perfectly"))
        assertTrue(result.text.contains("Lead Developer"))
    }

    @Test
    fun `test parse empty and malformed list items`() {
        val markdown = """
            Test List:
            
            - 
            - Valid item
            - 
            - Another valid item
            -Item without space
            - Final item
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that valid items are processed
        assertTrue(result.text.contains("• Valid item"))
        assertTrue(result.text.contains("• Another valid item"))
        assertTrue(result.text.contains("• Final item"))
        
        // Check that empty items are handled
        assertTrue(result.text.contains("• "))
        
        // Check that malformed items are not processed
        assertFalse(result.text.contains("•Item without space"))
    }

    @Test
    fun `test parse list with special characters and unicode`() {
        val markdown = """
            Special Characters Test:
            
            - Item with émojis 🚀 ✨ 🎉
            - Item with symbols: @#$%^&*()
            - Item with quotes: "Hello" and 'World'
            - Item with dashes: -- and ---
            - Item with underscores: __test__ and _test_
        """.trimIndent()
        
        val result = MarkdownParser.parse(markdown)
        
        // Check that all items are processed
        assertTrue(result.text.contains("• Item with émojis"))
        assertTrue(result.text.contains("• Item with symbols"))
        assertTrue(result.text.contains("• Item with quotes"))
        assertTrue(result.text.contains("• Item with dashes"))
        assertTrue(result.text.contains("• Item with underscores"))
        
        // Check that special characters are preserved
        assertTrue(result.text.contains("🚀 ✨ 🎉"))
        assertTrue(result.text.contains("@#$%^&*()"))
        assertTrue(result.text.contains("\"Hello\" and 'World'"))
        assertTrue(result.text.contains("-- and ---"))
        assertTrue(result.text.contains("__test__ and _test_"))
    }
}
