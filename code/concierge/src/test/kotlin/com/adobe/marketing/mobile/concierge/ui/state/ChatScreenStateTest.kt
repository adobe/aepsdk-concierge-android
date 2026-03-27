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

package com.adobe.marketing.mobile.concierge.ui.state

import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatScreenStateTest {

    // ========== MessageContent.Text Tests ==========

    @Test
    fun `MessageContent Text creates with correct text value`() {
        // Given
        val expectedText = "Hello, World!"

        // When
        val content = MessageContent.Text(expectedText)

        // Then
        assertEquals(expectedText, content.text)
    }

    @Test
    fun `MessageContent Text handles empty string`() {
        // Given
        val emptyText = ""

        // When
        val content = MessageContent.Text(emptyText)

        // Then
        assertEquals(emptyText, content.text)
    }

    @Test
    fun `MessageContent Text handles multiline text`() {
        // Given
        val multilineText = "Line 1\nLine 2\nLine 3"

        // When
        val content = MessageContent.Text(multilineText)

        // Then
        assertEquals(multilineText, content.text)
    }

    // ========== MessageContent.Mixed Tests ==========

    @Test
    fun `MessageContent Mixed creates with text and null multimodal elements`() {
        // Given
        val expectedText = "Some text content"

        // When
        val content = MessageContent.Mixed(
            text = expectedText,
            multimodalElements = null
        )

        // Then
        assertEquals(expectedText, content.text)
        assertNull(content.multimodalElements)
    }

    @Test
    fun `MessageContent Mixed creates with text and empty multimodal elements list`() {
        // Given
        val expectedText = "Some text content"
        val emptyElements = emptyList<MultimodalElement>()

        // When
        val content = MessageContent.Mixed(
            text = expectedText,
            multimodalElements = emptyElements
        )

        // Then
        assertEquals(expectedText, content.text)
        assertNotNull(content.multimodalElements)
        assertTrue(content.multimodalElements!!.isEmpty())
    }

    @Test
    fun `MessageContent Mixed creates with text and multimodal elements`() {
        // Given
        val expectedText = "Check out this image"
        val elements = listOf(
            MultimodalElement(
                id = "img-1",
                url = "https://example.com/image.png",
                alttext = "Example image",
                width = 800,
                height = 600
            ),
            MultimodalElement(
                id = "img-2",
                url = "https://example.com/image2.png",
                alttext = "Another example image"
            )
        )

        // When
        val content = MessageContent.Mixed(
            text = expectedText,
            multimodalElements = elements
        )

        // Then
        assertEquals(expectedText, content.text)
        assertNotNull(content.multimodalElements)
        assertEquals(2, content.multimodalElements!!.size)
        assertEquals("img-1", content.multimodalElements!![0].id)
        assertEquals("https://example.com/image.png", content.multimodalElements!![0].url)
        assertEquals("img-2", content.multimodalElements!![1].id)
        assertEquals("https://example.com/image2.png", content.multimodalElements!![1].url)

    }

    @Test
    fun `MessageContent Mixed handles empty text with multimodal elements`() {
        // Given
        val emptyText = ""
        val elements = listOf(
            MultimodalElement(
                id = "img-1",
                url = "https://example.com/image.jpg",
                alttext = null
            )
        )

        // When
        val content = MessageContent.Mixed(
            text = emptyText,
            multimodalElements = elements
        )

        // Then
        assertEquals(emptyText, content.text)
        assertEquals(1, content.multimodalElements!!.size)
    }

    @Test
    fun `MessageContent Mixed with single multimodal element`() {
        // Given
        val text = "Single element"
        val singleElement = listOf(
            MultimodalElement(
                id = "img-single",
                url = "https://example.com/single.jpg",
                alttext = "Single",
                content = mapOf("format" to "jpeg")
            )
        )

        // When
        val content = MessageContent.Mixed(
            text = text,
            multimodalElements = singleElement
        )

        // Then
        assertEquals(text, content.text)
        assertEquals(1, content.multimodalElements!!.size)
        assertEquals("img-single", content.multimodalElements!![0].id)
        assertEquals("https://example.com/single.jpg", content.multimodalElements!![0].url)
    }

    @Test
    fun `MessageContent Mixed handles complex multimodal elements with content`() {
        // Given
        val text = "Product showcase"
        val complexElements = listOf(
            MultimodalElement(
                id = "prod-1",
                url = "https://example.com/product1.jpg",
                alttext = "Product 1",
                content = mapOf(
                    "productId" to "123",
                    "price" to "99.99",
                    "inStock" to "true"
                )
            ),
            MultimodalElement(
                id = "prod-2",
                url = "https://example.com/product2.jpg",
                alttext = "Product 2",
                content = mapOf(
                    "productId" to "456",
                    "price" to "149.99",
                    "inStock" to "false"
                )
            )
        )

        // When
        val content = MessageContent.Mixed(
            text = text,
            multimodalElements = complexElements
        )

        // Then
        assertEquals(text, content.text)
        assertEquals(2, content.multimodalElements!!.size)
        assertEquals("123", content.multimodalElements!![0].content["productId"])
        assertEquals("456", content.multimodalElements!![1].content["productId"])
    }

    // ========== ChatMessage.getText() Tests ==========

    @Test
    fun `ChatMessage getText returns text from Text content`() {
        // Given
        val expectedText = "User message"
        val message = ChatMessage(
            content = MessageContent.Text(expectedText),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(expectedText, actualText)
    }

    @Test
    fun `ChatMessage getText returns text from Mixed content with null elements`() {
        // Given
        val expectedText = "Mixed content without elements"
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = expectedText,
                multimodalElements = null
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(expectedText, actualText)
    }

    @Test
    fun `ChatMessage getText returns text from Mixed content with multimodal elements`() {
        // Given
        val expectedText = "Check out these products"
        val elements = listOf(
            MultimodalElement(
                id = "prod-img",
                url = "https://example.com/product.jpg",
                alttext = "Product"
            )
        )
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = expectedText,
                multimodalElements = elements
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(expectedText, actualText)
    }

    @Test
    fun `ChatMessage getText handles empty text in Text content`() {
        // Given
        val emptyText = ""
        val message = ChatMessage(
            content = MessageContent.Text(emptyText),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(emptyText, actualText)
    }

    @Test
    fun `ChatMessage getText handles empty text in Mixed content`() {
        // Given
        val emptyText = ""
        val elements = listOf(
            MultimodalElement(
                id = "img-1",
                url = "https://example.com/image.jpg",
                alttext = "Image"
            )
        )
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = emptyText,
                multimodalElements = elements
            ),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(emptyText, actualText)
    }

    @Test
    fun `ChatMessage getText with all optional fields populated`() {
        // Given
        val expectedText = "Complete message"
        val citations = listOf(
            Citation(
                title = "Doc 1",
                url = "https://example.com/doc1"
            )
        )
        val message = ChatMessage(
            content = MessageContent.Text(expectedText),
            isFromUser = false,
            timestamp = System.currentTimeMillis(),
            citations = citations,
            uniqueCitations = citations,
            interactionId = "interaction-123",
            promptSuggestions = listOf("Suggestion 1", "Suggestion 2"),
            feedbackState = FeedbackState.Positive
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(expectedText, actualText)
        assertEquals(citations, message.citations)
        assertEquals("interaction-123", message.interactionId)
        assertEquals(2, message.promptSuggestions.size)
    }

    @Test
    fun `ChatMessage with Text content preserves all properties`() {
        // Given
        val text = "Test message"
        val timestamp = 1234567890L
        val citations = listOf(
            Citation(title = "Citation 1", url = "https://example.com/1")
        )
        val promptSuggestions = listOf("What is this?", "Tell me more")

        // When
        val message = ChatMessage(
            content = MessageContent.Text(text),
            isFromUser = true,
            timestamp = timestamp,
            citations = citations,
            uniqueCitations = citations,
            interactionId = "int-123",
            promptSuggestions = promptSuggestions,
            feedbackState = FeedbackState.None
        )

        // Then
        assertEquals(text, message.text)
        assertEquals(true, message.isFromUser)
        assertEquals(timestamp, message.timestamp)
        assertEquals(citations, message.citations)
        assertEquals("int-123", message.interactionId)
        assertEquals(promptSuggestions, message.promptSuggestions)
        assertEquals(FeedbackState.None, message.feedbackState)
    }

    @Test
    fun `ChatMessage with Mixed content preserves all properties`() {
        // Given
        val text = "Mixed message"
        val elements = listOf(
            MultimodalElement(
                id = "img-1",
                url = "https://example.com/img.jpg",
                alttext = "Image",
                content = mapOf("key" to "value")
            )
        )
        val timestamp = 9876543210L
        val citations = listOf(
            Citation(title = "Citation 2", url = "https://example.com/2")
        )

        // When
        val message = ChatMessage(
            content = MessageContent.Mixed(text = text, multimodalElements = elements),
            isFromUser = false,
            timestamp = timestamp,
            citations = citations,
            uniqueCitations = citations,
            interactionId = "int-456",
            promptSuggestions = emptyList(),
            feedbackState = FeedbackState.Negative
        )

        // Then
        assertEquals(text, message.text)
        assertEquals(false, message.isFromUser)
        assertEquals(timestamp, message.timestamp)
        assertEquals(citations, message.citations)
        assertEquals("int-456", message.interactionId)
        assertTrue(message.promptSuggestions.isEmpty())
        assertEquals(FeedbackState.Negative, message.feedbackState)
        val mixedContent = message.content as MessageContent.Mixed
        assertEquals(1, mixedContent.multimodalElements!!.size)
        assertEquals("img-1", mixedContent.multimodalElements!![0].id)
    }

    @Test
    fun `ChatMessage getText with multiline text content`() {
        // Given
        val multilineText = """
            This is line 1
            This is line 2
            This is line 3
        """.trimIndent()

        val message = ChatMessage(
            content = MessageContent.Text(multilineText),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(multilineText, actualText)
        assertTrue(actualText.contains("\n"))
    }

    @Test
    fun `ChatMessage getText with special characters`() {
        // Given
        val specialText = "Special chars: @#$%^&*()_+-=[]{}|;':\",./<>?"

        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = specialText,
                multimodalElements = null
            ),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(specialText, actualText)
    }

    @Test
    fun `ChatMessage getText with Unicode characters`() {
        // Given
        val unicodeText = "Unicode: 你好 🌟 café"

        val message = ChatMessage(
            content = MessageContent.Text(unicodeText),
            isFromUser = true,
            timestamp = System.currentTimeMillis()
        )

        // When
        val actualText = message.text

        // Then
        assertEquals(unicodeText, actualText)
    }

    // ========== MessageContent.CtaButton Tests ==========

    @Test
    fun `MessageContent CtaButton creates with button`() {
        val button = NetworkCtaButton(label = "Shop All", url = "https://example.com/shop")
        val content = MessageContent.CtaButton(button)
        assertEquals("Shop All", content.button.label)
        assertEquals("https://example.com/shop", content.button.url)
    }

    @Test
    fun `ChatMessage getText returns label from CtaButton content`() {
        val button = NetworkCtaButton(label = "Learn More", url = "https://example.com/learn")
        val message = ChatMessage(
            content = MessageContent.CtaButton(button),
            isFromUser = false,
            timestamp = System.currentTimeMillis()
        )
        assertEquals("Learn More", message.text)
    }

    @Test
    fun `MessageContent CtaButton is distinct from Text and Mixed`() {
        val textContent = MessageContent.Text("Hello")
        val mixedContent = MessageContent.Mixed(text = "Mixed")
        val ctaContent = MessageContent.CtaButton(NetworkCtaButton(label = "Go", url = "https://example.com"))

        assertTrue(textContent is MessageContent.Text)
        assertTrue(mixedContent is MessageContent.Mixed)
        assertTrue(ctaContent is MessageContent.CtaButton)
    }

    // ========== FeedbackType Tests ==========

    @Test
    fun `FeedbackType has POSITIVE value`() {
        // Given & When
        val feedbackType = FeedbackType.POSITIVE

        // Then
        assertEquals(FeedbackType.POSITIVE, feedbackType)
    }

    @Test
    fun `FeedbackType has NEGATIVE value`() {
        // Given & When
        val feedbackType = FeedbackType.NEGATIVE

        // Then
        assertEquals(FeedbackType.NEGATIVE, feedbackType)
    }

    @Test
    fun `FeedbackType enum has exactly two values`() {
        // Given & When
        val values = FeedbackType.values()

        // Then
        assertEquals(2, values.size)
        assertTrue(values.contains(FeedbackType.POSITIVE))
        assertTrue(values.contains(FeedbackType.NEGATIVE))
    }

    // ========== Feedback Data Class Tests ==========

    @Test
    fun `Feedback creates with all required fields`() {
        // Given
        val interactionId = "interaction-123"
        val feedbackType = FeedbackType.POSITIVE

        // When
        val feedback = Feedback(
            interactionId = interactionId,
            feedbackType = feedbackType
        )

        // Then
        assertEquals(interactionId, feedback.interactionId)
        assertEquals(feedbackType, feedback.feedbackType)
        assertTrue(feedback.selectedCategories.isEmpty())
        assertEquals("", feedback.notes)
        assertNull(feedback.conversationId)
    }

    @Test
    fun `Feedback creates with all optional fields populated`() {
        // Given
        val interactionId = "interaction-456"
        val feedbackType = FeedbackType.NEGATIVE
        val categories = listOf("Inaccurate", "Not helpful", "Other")
        val notes = "The response was not relevant to my question."
        val conversationId = "conv-789"

        // When
        val feedback = Feedback(
            interactionId = interactionId,
            feedbackType = feedbackType,
            selectedCategories = categories,
            notes = notes,
            conversationId = conversationId
        )

        // Then
        assertEquals(interactionId, feedback.interactionId)
        assertEquals(feedbackType, feedback.feedbackType)
        assertEquals(categories, feedback.selectedCategories)
        assertEquals(notes, feedback.notes)
        assertEquals(conversationId, feedback.conversationId)
    }

    @Test
    fun `Feedback with POSITIVE type and empty categories`() {
        // Given
        val feedback = Feedback(
            interactionId = "int-1",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = emptyList()
        )

        // Then
        assertEquals(FeedbackType.POSITIVE, feedback.feedbackType)
        assertTrue(feedback.selectedCategories.isEmpty())
    }

    @Test
    fun `Feedback with NEGATIVE type and multiple categories`() {
        // Given
        val categories = listOf("Category 1", "Category 2", "Category 3")
        val feedback = Feedback(
            interactionId = "int-2",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = categories
        )

        // Then
        assertEquals(FeedbackType.NEGATIVE, feedback.feedbackType)
        assertEquals(3, feedback.selectedCategories.size)
        assertEquals(categories, feedback.selectedCategories)
    }

    @Test
    fun `Feedback with notes handles empty string`() {
        // Given
        val feedback = Feedback(
            interactionId = "int-3",
            feedbackType = FeedbackType.POSITIVE,
            notes = ""
        )

        // Then
        assertEquals("", feedback.notes)
    }

    @Test
    fun `Feedback with notes handles multiline text`() {
        // Given
        val multilineNotes = """
            Line 1: This is good
            Line 2: But could be better
            Line 3: Overall satisfied
        """.trimIndent()

        val feedback = Feedback(
            interactionId = "int-4",
            feedbackType = FeedbackType.POSITIVE,
            notes = multilineNotes
        )

        // Then
        assertEquals(multilineNotes, feedback.notes)
        assertTrue(feedback.notes.contains("\n"))
    }

    @Test
    fun `Feedback with null conversationId`() {
        // Given
        val feedback = Feedback(
            interactionId = "int-5",
            feedbackType = FeedbackType.NEGATIVE,
            conversationId = null
        )

        // Then
        assertNull(feedback.conversationId)
    }

    @Test
    fun `Feedback with non-null conversationId`() {
        // Given
        val conversationId = "conv-unique-123"
        val feedback = Feedback(
            interactionId = "int-6",
            feedbackType = FeedbackType.POSITIVE,
            conversationId = conversationId
        )

        // Then
        assertEquals(conversationId, feedback.conversationId)
    }

    // ========== FeedbackEvent Tests ==========

    @Test
    fun `FeedbackEvent ThumbsUp creates with interactionId`() {
        // Given
        val interactionId = "interaction-123"

        // When
        val event = FeedbackEvent.ThumbsUp(interactionId)

        // Then
        assertEquals(interactionId, event.interactionId)
        // Type checks: event is FeedbackEvent (sealed class) and ChatEvent (parent)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `FeedbackEvent ThumbsDown creates with interactionId`() {
        // Given
        val interactionId = "interaction-456"

        // When
        val event = FeedbackEvent.ThumbsDown(interactionId)

        // Then
        assertEquals(interactionId, event.interactionId)
        // Type checks: event is FeedbackEvent (sealed class) and ChatEvent (parent)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `FeedbackEvent SubmitFeedback creates with feedback object`() {
        // Given
        val feedback = Feedback(
            interactionId = "int-789",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful", "Accurate"),
            notes = "Great response!"
        )

        // When
        val event = FeedbackEvent.SubmitFeedback(feedback)

        // Then
        assertEquals(feedback, event.feedback)
        assertEquals("int-789", event.feedback.interactionId)
        assertEquals(FeedbackType.POSITIVE, event.feedback.feedbackType)
        // Type checks: event is FeedbackEvent (sealed class) and ChatEvent (parent)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `FeedbackEvent SubmitFeedback with NEGATIVE feedback`() {
        // Given
        val feedback = Feedback(
            interactionId = "int-negative",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = listOf("Inaccurate", "Not helpful"),
            notes = "Response was incorrect.",
            conversationId = "conv-001"
        )

        // When
        val event = FeedbackEvent.SubmitFeedback(feedback)

        // Then
        assertEquals(feedback, event.feedback)
        assertEquals(FeedbackType.NEGATIVE, event.feedback.feedbackType)
        assertEquals(2, event.feedback.selectedCategories.size)
        assertEquals("Response was incorrect.", event.feedback.notes)
        assertEquals("conv-001", event.feedback.conversationId)
    }

    @Test
    fun `FeedbackEvent SubmitFeedback with minimal feedback`() {
        // Given
        val minimalFeedback = Feedback(
            interactionId = "int-minimal",
            feedbackType = FeedbackType.POSITIVE
        )

        // When
        val event = FeedbackEvent.SubmitFeedback(minimalFeedback)

        // Then
        assertEquals(minimalFeedback, event.feedback)
        assertTrue(event.feedback.selectedCategories.isEmpty())
        assertEquals("", event.feedback.notes)
        assertNull(event.feedback.conversationId)
    }

    @Test
    fun `FeedbackEvent DismissFeedbackDialog is object singleton`() {
        // Given & When
        val event1 = FeedbackEvent.DismissFeedbackDialog
        val event2 = FeedbackEvent.DismissFeedbackDialog

        // Then
        assertTrue(event1 === event2) // Same instance
        // Type check: object is ChatEvent (parent class)
        assertTrue(event1 is ChatEvent)
    }

    @Test
    fun `FeedbackEvent sealed class hierarchy is correct`() {
        // Given - explicitly type as FeedbackEvent to test hierarchy
        val thumbsUp: FeedbackEvent = FeedbackEvent.ThumbsUp("int-1")
        val thumbsDown: FeedbackEvent = FeedbackEvent.ThumbsDown("int-2")
        val submitFeedback: FeedbackEvent = FeedbackEvent.SubmitFeedback(
            Feedback("int-3", FeedbackType.POSITIVE)
        )
        val dismiss: FeedbackEvent = FeedbackEvent.DismissFeedbackDialog

        // Then - All should be instances of ChatEvent (parent class)
        assertTrue(thumbsUp is ChatEvent)
        assertTrue(thumbsDown is ChatEvent)
        assertTrue(submitFeedback is ChatEvent)
        assertTrue(dismiss is ChatEvent)
    }

    @Test
    fun `FeedbackEvent ThumbsUp with empty interactionId`() {
        // Given
        val emptyId = ""

        // When
        val event = FeedbackEvent.ThumbsUp(emptyId)

        // Then
        assertEquals(emptyId, event.interactionId)
    }

    @Test
    fun `FeedbackEvent ThumbsDown with long interactionId`() {
        // Given
        val longId = "interaction-" + "a".repeat(1000)

        // When
        val event = FeedbackEvent.ThumbsDown(longId)

        // Then
        assertEquals(longId, event.interactionId)
        assertEquals(1012, event.interactionId.length) // "interaction-" (12) + 1000 'a's
    }

    @Test
    fun `FeedbackEvent can be used in when expression`() {
        // Given
        val events = listOf<FeedbackEvent>(
            FeedbackEvent.ThumbsUp("int-1"),
            FeedbackEvent.ThumbsDown("int-2"),
            FeedbackEvent.SubmitFeedback(Feedback("int-3", FeedbackType.POSITIVE)),
            FeedbackEvent.DismissFeedbackDialog
        )

        // When & Then
        events.forEach { event ->
            when (event) {
                is FeedbackEvent.ThumbsUp -> assertNotNull(event.interactionId)
                is FeedbackEvent.ThumbsDown -> assertNotNull(event.interactionId)
                is FeedbackEvent.SubmitFeedback -> assertNotNull(event.feedback)
                is FeedbackEvent.DismissFeedbackDialog -> assertTrue(true)
            }
        }
    }

    @Test
    fun `Feedback data class supports copy with modified fields`() {
        // Given
        val original = Feedback(
            interactionId = "int-original",
            feedbackType = FeedbackType.POSITIVE,
            selectedCategories = listOf("Good"),
            notes = "Original notes",
            conversationId = "conv-1"
        )

        // When
        val modified = original.copy(
            notes = "Modified notes",
            selectedCategories = listOf("Good", "Helpful")
        )

        // Then
        assertEquals("int-original", modified.interactionId)
        assertEquals(FeedbackType.POSITIVE, modified.feedbackType)
        assertEquals(listOf("Good", "Helpful"), modified.selectedCategories)
        assertEquals("Modified notes", modified.notes)
        assertEquals("conv-1", modified.conversationId)
        // Original should be unchanged
        assertEquals("Original notes", original.notes)
        assertEquals(listOf("Good"), original.selectedCategories)
    }

    // ========== MessageInteractionEvent Tests ==========

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick creates with suggestion`() {
        // Given
        val suggestion = "What are the latest features?"

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(suggestion)

        // Then
        assertEquals(suggestion, event.suggestion)
        // Type check: event is ChatEvent (parent class)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick with empty suggestion`() {
        // Given
        val emptySuggestion = ""

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(emptySuggestion)

        // Then
        assertEquals(emptySuggestion, event.suggestion)
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick with multiline suggestion`() {
        // Given
        val multilineSuggestion = """
            How do I:
            - Configure the app
            - Set up authentication
            - Deploy to production
        """.trimIndent()

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(multilineSuggestion)

        // Then
        assertEquals(multilineSuggestion, event.suggestion)
        assertTrue(event.suggestion.contains("\n"))
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick with long suggestion`() {
        // Given
        val longSuggestion = "Can you explain " + "the details ".repeat(100) + "about this feature?"

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(longSuggestion)

        // Then
        assertEquals(longSuggestion, event.suggestion)
        assertTrue(event.suggestion.length > 1000)
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick with special characters`() {
        // Given
        val specialSuggestion = "What about @tags, #hashtags, and symbols: $%^&*()?"

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(specialSuggestion)

        // Then
        assertEquals(specialSuggestion, event.suggestion)
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick with Unicode characters`() {
        // Given
        val unicodeSuggestion = "¿Cómo funciona? 你好 🚀"

        // When
        val event = MessageInteractionEvent.PromptSuggestionClick(unicodeSuggestion)

        // Then
        assertEquals(unicodeSuggestion, event.suggestion)
    }

    @Test
    fun `MessageInteractionEvent ProductActionClick creates with button`() {
        // Given
        val button = ProductActionButton(
            id = "btn-1",
            text = "View Product",
            url = "https://example.com/product/123"
        )

        // When
        val event = MessageInteractionEvent.ProductActionClick(button)

        // Then
        assertEquals(button, event.button)
        assertEquals("btn-1", event.button.id)
        assertEquals("View Product", event.button.text)
        assertEquals("https://example.com/product/123", event.button.url)
        // Type check: event is ChatEvent (parent class)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `MessageInteractionEvent ProductActionClick with null url`() {
        // Given
        val button = ProductActionButton(
            id = "btn-no-url",
            text = "Contact Us",
            url = null
        )

        // When
        val event = MessageInteractionEvent.ProductActionClick(button)

        // Then
        assertEquals(button, event.button)
        assertNull(event.button.url)
    }

    @Test
    fun `MessageInteractionEvent ProductActionClick with empty text`() {
        // Given
        val button = ProductActionButton(
            id = "btn-empty",
            text = "",
            url = "https://example.com"
        )

        // When
        val event = MessageInteractionEvent.ProductActionClick(button)

        // Then
        assertEquals("", event.button.text)
    }

    @Test
    fun `MessageInteractionEvent ProductImageClick creates with element`() {
        // Given
        val element = MultimodalElement(
            id = "img-1",
            url = "https://example.com/image.jpg",
            width = 800,
            height = 600,
            alttext = "Product image"
        )

        // When
        val event = MessageInteractionEvent.ProductImageClick(element)

        // Then
        assertEquals(element, event.element)
        assertEquals("img-1", event.element.id)
        assertEquals("https://example.com/image.jpg", event.element.url)
        assertEquals(800, event.element.width)
        assertEquals(600, event.element.height)
        // Type check: event is ChatEvent (parent class)
        assertTrue(event is ChatEvent)
    }

    @Test
    fun `MessageInteractionEvent ProductImageClick with minimal element`() {
        // Given
        val element = MultimodalElement(
            id = "img-minimal",
            url = null,
            alttext = null
        )

        // When
        val event = MessageInteractionEvent.ProductImageClick(element)

        // Then
        assertEquals(element, event.element)
        assertNull(event.element.url)
        assertNull(event.element.alttext)
    }

    @Test
    fun `MessageInteractionEvent ProductImageClick with thumbnail data`() {
        // Given
        val element = MultimodalElement(
            id = "img-thumb",
            url = "https://example.com/full.jpg",
            thumbnailUrl = "https://example.com/thumb.jpg",
            thumbnailWidth = 200,
            thumbnailHeight = 150,
            width = 1920,
            height = 1080
        )

        // When
        val event = MessageInteractionEvent.ProductImageClick(element)

        // Then
        assertEquals("https://example.com/thumb.jpg", event.element.thumbnailUrl)
        assertEquals(200, event.element.thumbnailWidth)
        assertEquals(150, event.element.thumbnailHeight)
    }

    @Test
    fun `MessageInteractionEvent ProductImageClick with content metadata`() {
        // Given
        val element = MultimodalElement(
            id = "img-meta",
            url = "https://example.com/product.jpg",
            content = mapOf(
                "productId" to "prod-123",
                "price" to 99.99,
                "inStock" to true
            )
        )

        // When
        val event = MessageInteractionEvent.ProductImageClick(element)

        // Then
        assertEquals("prod-123", event.element.content["productId"])
        assertEquals(99.99, event.element.content["price"])
        assertEquals(true, event.element.content["inStock"])
    }

    @Test
    fun `MessageInteractionEvent sealed class hierarchy is correct`() {
        // Given - explicitly type as MessageInteractionEvent to test hierarchy
        val promptClick: MessageInteractionEvent = MessageInteractionEvent.PromptSuggestionClick("test")
        val productAction: MessageInteractionEvent = MessageInteractionEvent.ProductActionClick(
            ProductActionButton("btn-1", "Click me")
        )
        val imageClick: MessageInteractionEvent = MessageInteractionEvent.ProductImageClick(
            MultimodalElement("img-1")
        )

        // Then - All should be instances of ChatEvent (parent class)
        assertTrue(promptClick is ChatEvent)
        assertTrue(productAction is ChatEvent)
        assertTrue(imageClick is ChatEvent)
    }

    @Test
    fun `MessageInteractionEvent can be used in when expression`() {
        // Given
        val events = listOf<MessageInteractionEvent>(
            MessageInteractionEvent.PromptSuggestionClick("suggestion"),
            MessageInteractionEvent.ProductActionClick(ProductActionButton("btn-1", "text")),
            MessageInteractionEvent.ProductImageClick(MultimodalElement("img-1"))
        )

        // When & Then
        events.forEach { event ->
            when (event) {
                is MessageInteractionEvent.PromptSuggestionClick -> assertNotNull(event.suggestion)
                is MessageInteractionEvent.ProductActionClick -> assertNotNull(event.button)
                is MessageInteractionEvent.ProductImageClick -> assertNotNull(event.element)
            }
        }
    }

    @Test
    fun `MessageInteractionEvent PromptSuggestionClick multiple instances with different suggestions`() {
        // Given
        val suggestions = listOf(
            "How do I get started?",
            "What are the pricing options?",
            "Can you explain feature X?",
            "Show me examples"
        )

        // When
        val events = suggestions.map { MessageInteractionEvent.PromptSuggestionClick(it) }

        // Then
        assertEquals(4, events.size)
        events.forEachIndexed { index, event ->
            assertEquals(suggestions[index], event.suggestion)
        }
    }

    @Test
    fun `ProductActionButton creates with all fields`() {
        // Given & When
        val button = ProductActionButton(
            id = "action-123",
            text = "Buy Now",
            url = "https://store.example.com/checkout"
        )

        // Then
        assertEquals("action-123", button.id)
        assertEquals("Buy Now", button.text)
        assertEquals("https://store.example.com/checkout", button.url)
    }

    @Test
    fun `ProductActionButton with default null url`() {
        // Given & When
        val button = ProductActionButton(
            id = "action-no-url",
            text = "Learn More"
        )

        // Then
        assertEquals("action-no-url", button.id)
        assertEquals("Learn More", button.text)
        assertNull(button.url)
    }

    @Test
    fun `ProductActionButton data class supports copy`() {
        // Given
        val original = ProductActionButton(
            id = "btn-original",
            text = "Original Text",
            url = "https://original.com"
        )

        // When
        val modified = original.copy(text = "Modified Text")

        // Then
        assertEquals("btn-original", modified.id)
        assertEquals("Modified Text", modified.text)
        assertEquals("https://original.com", modified.url)
        // Original should be unchanged
        assertEquals("Original Text", original.text)
    }

    @Test
    fun `MessageInteractionEvent different types are distinct`() {
        // Given
        val suggestion = "test"
        val button = ProductActionButton("btn-1", "text")
        val element = MultimodalElement("img-1")

        val event1 = MessageInteractionEvent.PromptSuggestionClick(suggestion)
        val event2 = MessageInteractionEvent.ProductActionClick(button)
        val event3 = MessageInteractionEvent.ProductImageClick(element)

        // Then - Verify they are different types by counting
        val events: List<MessageInteractionEvent> = listOf(event1, event2, event3)
        assertEquals(3, events.size)
        
        // Count each type to verify distinctness
        val promptCount = events.count { it is MessageInteractionEvent.PromptSuggestionClick }
        val actionCount = events.count { it is MessageInteractionEvent.ProductActionClick }
        val imageCount = events.count { it is MessageInteractionEvent.ProductImageClick }
        
        assertEquals(1, promptCount)
        assertEquals(1, actionCount)
        assertEquals(1, imageCount)
    }
}
