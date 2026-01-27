/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TempConversationResponseTest {

    @Test
    fun `ConversationState fromString maps in-progress correctly`() {
        assertEquals(ConversationState.IN_PROGRESS, ConversationState.fromString("in-progress"))
        assertEquals(ConversationState.IN_PROGRESS, ConversationState.fromString("IN-PROGRESS"))
        assertEquals(ConversationState.IN_PROGRESS, ConversationState.fromString("In-Progress"))
    }

    @Test
    fun `ConversationState fromString maps completed correctly`() {
        assertEquals(ConversationState.COMPLETED, ConversationState.fromString("completed"))
        assertEquals(ConversationState.COMPLETED, ConversationState.fromString("COMPLETED"))
        assertEquals(ConversationState.COMPLETED, ConversationState.fromString("Completed"))
    }

    @Test
    fun `ConversationState fromString maps error correctly`() {
        assertEquals(ConversationState.ERROR, ConversationState.fromString("error"))
        assertEquals(ConversationState.ERROR, ConversationState.fromString("ERROR"))
        assertEquals(ConversationState.ERROR, ConversationState.fromString("Error"))
    }

    @Test
    fun `ConversationState fromString maps unknown for invalid values`() {
        assertEquals(ConversationState.UNKNOWN, ConversationState.fromString("invalid"))
        assertEquals(ConversationState.UNKNOWN, ConversationState.fromString("pending"))
        assertEquals(ConversationState.UNKNOWN, ConversationState.fromString("processing"))
        assertEquals(ConversationState.UNKNOWN, ConversationState.fromString(""))
    }

    @Test
    fun `ConversationState fromString handles null input`() {
        assertEquals(ConversationState.UNKNOWN, ConversationState.fromString(null))
    }

    @Test
    fun `ParsedConversationMessage creates with minimal fields`() {
        val message = ParsedConversationMessage(
            messageContent = "Hello",
            state = ConversationState.IN_PROGRESS
        )

        assertEquals("Hello", message.messageContent)
        assertEquals(ConversationState.IN_PROGRESS, message.state)
        assertEquals(null, message.conversationId)
        assertEquals(null, message.interactionId)
        assertTrue(message.promptSuggestions.isEmpty())
        assertTrue(message.multimodalElements.isEmpty())
        assertTrue(message.sources.isEmpty())
    }

    @Test
    fun `ParsedConversationMessage creates with all fields`() {
        val element = MultimodalElement(
            id = "elem-1",
            url = "https://example.com/image.jpg",
            title = "Product"
        )
        val citation = Citation(
            title = "Source",
            url = "https://source.com"
        )

        val message = ParsedConversationMessage(
            messageContent = "Complete message",
            state = ConversationState.COMPLETED,
            conversationId = "conv-123",
            interactionId = "inter-456",
            promptSuggestions = listOf("Suggestion 1", "Suggestion 2"),
            multimodalElements = listOf(element),
            sources = listOf(citation)
        )

        assertEquals("Complete message", message.messageContent)
        assertEquals(ConversationState.COMPLETED, message.state)
        assertEquals("conv-123", message.conversationId)
        assertEquals("inter-456", message.interactionId)
        assertEquals(2, message.promptSuggestions.size)
        assertEquals(1, message.multimodalElements.size)
        assertEquals(1, message.sources.size)
    }

    @Test
    fun `MultimodalElement creates with minimal fields`() {
        val element = MultimodalElement(id = "test-id")

        assertEquals("test-id", element.id)
        assertEquals(null, element.url)
        assertEquals(null, element.width)
        assertEquals(null, element.height)
        assertEquals(null, element.thumbnailUrl)
        assertEquals(null, element.thumbnailWidth)
        assertEquals(null, element.thumbnailHeight)
        assertEquals(null, element.alttext)
        assertEquals(null, element.title)
        assertEquals(null, element.caption)
        assertEquals(null, element.transcript)
        assertTrue(element.content.isEmpty())
    }

    @Test
    fun `MultimodalElement creates with all fields`() {
        val content = mapOf(
            "productName" to "Widget",
            "price" to 99.99,
            "available" to true
        )

        val element = MultimodalElement(
            id = "prod-123",
            url = "https://example.com/product.jpg",
            width = 800,
            height = 600,
            thumbnailUrl = "https://example.com/thumb.jpg",
            thumbnailWidth = 200,
            thumbnailHeight = 150,
            alttext = "Product image",
            title = "Amazing Widget",
            caption = "Best widget ever",
            transcript = "Detailed description",
            content = content
        )

        assertEquals("prod-123", element.id)
        assertEquals("https://example.com/product.jpg", element.url)
        assertEquals(800, element.width)
        assertEquals(600, element.height)
        assertEquals("https://example.com/thumb.jpg", element.thumbnailUrl)
        assertEquals(200, element.thumbnailWidth)
        assertEquals(150, element.thumbnailHeight)
        assertEquals("Product image", element.alttext)
        assertEquals("Amazing Widget", element.title)
        assertEquals("Best widget ever", element.caption)
        assertEquals("Detailed description", element.transcript)
        assertEquals(3, element.content.size)
        assertEquals("Widget", element.content["productName"])
        assertEquals(99.99, element.content["price"])
        assertEquals(true, element.content["available"])
    }

    @Test
    fun `Citation creates with minimal fields`() {
        val citation = Citation(
            title = "Reference",
            url = "https://reference.com"
        )

        assertEquals("Reference", citation.title)
        assertEquals("https://reference.com", citation.url)
        assertEquals(null, citation.citationNumber)
        assertEquals(null, citation.startIndex)
        assertEquals(null, citation.endIndex)
    }

    @Test
    fun `Citation creates with all fields`() {
        val citation = Citation(
            title = "Documentation",
            url = "https://docs.example.com",
            citationNumber = 1,
            startIndex = 10,
            endIndex = 20
        )

        assertEquals("Documentation", citation.title)
        assertEquals("https://docs.example.com", citation.url)
        assertEquals(1, citation.citationNumber)
        assertEquals(10, citation.startIndex)
        assertEquals(20, citation.endIndex)
    }

    @Test
    fun `ConversationApiResponse creates with empty handle`() {
        val response = ConversationApiResponse()
        assertTrue(response.handle.isEmpty())
    }

    @Test
    fun `ConversationApiResponse creates with handles`() {
        val handle = Handle(type = "test-type", payload = listOf(mapOf("key" to "value")))
        val response = ConversationApiResponse(handle = listOf(handle))

        assertEquals(1, response.handle.size)
        assertEquals("test-type", response.handle[0].type)
    }

    @Test
    fun `Handle creates with payload`() {
        val payload = listOf(
            mapOf("field1" to "value1"),
            mapOf("field2" to 123)
        )
        val handle = Handle(type = "brand-concierge:conversation", payload = payload)

        assertEquals("brand-concierge:conversation", handle.type)
        assertEquals(2, handle.payload.size)
        assertEquals("value1", handle.payload[0]["field1"])
        assertEquals(123, handle.payload[1]["field2"])
    }

    @Test
    fun `Handle creates with empty payload by default`() {
        val handle = Handle(type = "test-type")
        assertEquals("test-type", handle.type)
        assertTrue(handle.payload.isEmpty())
    }

    @Test
    fun `ConversationPayload creates with all fields`() {
        val request = ConversationRequest(message = "User query")
        val response = ConversationResponse(
            message = "AI response",
            multimodalElements = emptyList(),
            promptSuggestions = listOf("Follow-up")
        )

        val payload = ConversationPayload(
            conversationId = "conv-123",
            interactionId = "inter-456",
            request = request,
            response = response,
            state = "in-progress"
        )

        assertEquals("conv-123", payload.conversationId)
        assertEquals("inter-456", payload.interactionId)
        assertEquals("User query", payload.request?.message)
        assertEquals("AI response", payload.response?.message)
        assertEquals("in-progress", payload.state)
    }

    @Test
    fun `ConversationPayload creates with null fields`() {
        val payload = ConversationPayload()

        assertEquals(null, payload.conversationId)
        assertEquals(null, payload.interactionId)
        assertEquals(null, payload.request)
        assertEquals(null, payload.response)
        assertEquals(null, payload.state)
    }

    @Test
    fun `ConversationRequest creates with message`() {
        val request = ConversationRequest(message = "Test message")
        assertEquals("Test message", request.message)
    }

    @Test
    fun `ConversationRequest creates with null message`() {
        val request = ConversationRequest()
        assertEquals(null, request.message)
    }

    @Test
    fun `ConversationResponse creates with all fields`() {
        val element = MultimodalElement(id = "elem-1")
        val response = ConversationResponse(
            message = "Response text",
            multimodalElements = listOf(element),
            promptSuggestions = listOf("Next", "More")
        )

        assertEquals("Response text", response.message)
        assertEquals(1, response.multimodalElements.size)
        assertEquals(2, response.promptSuggestions.size)
    }

    @Test
    fun `ConversationResponse creates with empty collections by default`() {
        val response = ConversationResponse(message = "Test")

        assertEquals("Test", response.message)
        assertTrue(response.multimodalElements.isEmpty())
        assertTrue(response.promptSuggestions.isEmpty())
    }

    @Test
    fun `data class equality works for ParsedConversationMessage`() {
        val message1 = ParsedConversationMessage("Hello", ConversationState.IN_PROGRESS)
        val message2 = ParsedConversationMessage("Hello", ConversationState.IN_PROGRESS)
        val message3 = ParsedConversationMessage("Goodbye", ConversationState.IN_PROGRESS)

        assertEquals(message1, message2)
        assertNotEquals(message1, message3)
    }

    @Test
    fun `data class equality works for MultimodalElement`() {
        val element1 = MultimodalElement(id = "1", url = "https://example.com")
        val element2 = MultimodalElement(id = "1", url = "https://example.com")
        val element3 = MultimodalElement(id = "2", url = "https://example.com")

        assertEquals(element1, element2)
        assertNotEquals(element1, element3)
    }

    @Test
    fun `data class equality works for Citation`() {
        val citation1 = Citation("Title", "https://url.com", 1)
        val citation2 = Citation("Title", "https://url.com", 1)
        val citation3 = Citation("Title", "https://url.com", 2)

        assertEquals(citation1, citation2)
        assertNotEquals(citation1, citation3)
    }

    @Test
    fun `data class copy works for ParsedConversationMessage`() {
        val original = ParsedConversationMessage(
            messageContent = "Original",
            state = ConversationState.IN_PROGRESS,
            conversationId = "conv-1"
        )

        val copied = original.copy(messageContent = "Modified")

        assertEquals("Modified", copied.messageContent)
        assertEquals(ConversationState.IN_PROGRESS, copied.state)
        assertEquals("conv-1", copied.conversationId)
        assertNotEquals(original, copied)
    }

    @Test
    fun `data class copy works for MultimodalElement`() {
        val original = MultimodalElement(id = "1", title = "Original Title")
        val copied = original.copy(title = "New Title")

        assertEquals("1", copied.id)
        assertEquals("New Title", copied.title)
        assertNotEquals(original, copied)
    }

    @Test
    fun `data class copy works for Citation`() {
        val original = Citation("Original", "https://example.com", 1)
        val copied = original.copy(title = "Modified")

        assertEquals("Modified", copied.title)
        assertEquals("https://example.com", copied.url)
        assertEquals(1, copied.citationNumber)
        assertNotEquals(original, copied)
    }

    @Test
    fun `MultimodalElement content map can contain various types`() {
        val content = mapOf(
            "string" to "text",
            "int" to 42,
            "double" to 3.14,
            "boolean" to true,
            "list" to listOf(1, 2, 3),
            "map" to mapOf("nested" to "value")
        )

        val element = MultimodalElement(id = "test", content = content)

        assertEquals("text", element.content["string"])
        assertEquals(42, element.content["int"])
        assertEquals(3.14, element.content["double"])
        assertEquals(true, element.content["boolean"])
        assertTrue(element.content["list"] is List<*>)
        assertTrue(element.content["map"] is Map<*, *>)
    }

    @Test
    fun `ParsedConversationMessage with empty strings`() {
        val message = ParsedConversationMessage(
            messageContent = "",
            state = ConversationState.COMPLETED,
            conversationId = "",
            interactionId = ""
        )

        assertEquals("", message.messageContent)
        assertEquals("", message.conversationId)
        assertEquals("", message.interactionId)
    }

    @Test
    fun `Citation with empty strings`() {
        val citation = Citation(title = "", url = "")
        assertEquals("", citation.title)
        assertEquals("", citation.url)
    }

    @Test
    fun `MultimodalElement with empty strings`() {
        val element = MultimodalElement(
            id = "",
            url = "",
            alttext = "",
            title = "",
            caption = "",
            transcript = ""
        )

        assertEquals("", element.id)
        assertEquals("", element.url)
        assertEquals("", element.alttext)
        assertEquals("", element.title)
        assertEquals("", element.caption)
        assertEquals("", element.transcript)
    }

    @Test
    fun `ParsedConversationMessage with large collections`() {
        val suggestions = (1..100).map { "Suggestion $it" }
        val elements = (1..50).map { MultimodalElement(id = "elem-$it") }
        val sources = (1..50).map { Citation(title = "Source $it", url = "https://source$it.com") }

        val message = ParsedConversationMessage(
            messageContent = "Test",
            state = ConversationState.IN_PROGRESS,
            promptSuggestions = suggestions,
            multimodalElements = elements,
            sources = sources
        )

        assertEquals(100, message.promptSuggestions.size)
        assertEquals(50, message.multimodalElements.size)
        assertEquals(50, message.sources.size)
    }

    @Test
    fun `MultimodalElement with dimensions at boundary values`() {
        val element1 = MultimodalElement(
            id = "1",
            width = 0,
            height = 0,
            thumbnailWidth = 0,
            thumbnailHeight = 0
        )

        assertEquals(0, element1.width)
        assertEquals(0, element1.height)
        assertEquals(0, element1.thumbnailWidth)
        assertEquals(0, element1.thumbnailHeight)

        val element2 = MultimodalElement(
            id = "2",
            width = Int.MAX_VALUE,
            height = Int.MAX_VALUE,
            thumbnailWidth = Int.MAX_VALUE,
            thumbnailHeight = Int.MAX_VALUE
        )

        assertEquals(Int.MAX_VALUE, element2.width)
        assertEquals(Int.MAX_VALUE, element2.height)
        assertEquals(Int.MAX_VALUE, element2.thumbnailWidth)
        assertEquals(Int.MAX_VALUE, element2.thumbnailHeight)
    }

    @Test
    fun `Citation with indices at boundary values`() {
        val citation1 = Citation(
            title = "Test",
            url = "https://test.com",
            citationNumber = 0,
            startIndex = 0,
            endIndex = 0
        )

        assertEquals(0, citation1.citationNumber)
        assertEquals(0, citation1.startIndex)
        assertEquals(0, citation1.endIndex)

        val citation2 = Citation(
            title = "Test",
            url = "https://test.com",
            citationNumber = Int.MAX_VALUE,
            startIndex = Int.MAX_VALUE,
            endIndex = Int.MAX_VALUE
        )

        assertEquals(Int.MAX_VALUE, citation2.citationNumber)
        assertEquals(Int.MAX_VALUE, citation2.startIndex)
        assertEquals(Int.MAX_VALUE, citation2.endIndex)
    }
}

