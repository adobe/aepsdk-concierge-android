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

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.Event
import org.junit.Assert.assertEquals
import org.junit.Test

class ConciergeTrackingEventTest {

    // MARK: - Common assertion helpers

    private fun assertCommonEventProperties(
        event: Event,
        expectedName: String,
        expectedXDMType: String
    ) {
        assertEquals(expectedName, event.name)
        assertEquals(ConciergeConstants.EventType.CONCIERGE, event.type)
        assertEquals(ConciergeConstants.EventSource.NOTIFICATION, event.source)
        assertEquals(
            expectedXDMType,
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.EVENT_TYPE)
        )
    }

    // MARK: - sessionInitialized

    @Test
    fun `sessionInitialized produces correct event`() {
        val event = ConciergeTrackingEvent.SessionInitialized.toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.SESSION_INITIALIZED,
            ConciergeConstants.TrackingEvent.XDMType.SESSION_INITIALIZED
        )
        assertEquals(1, event.eventData?.size)
    }

    // MARK: - querySubmitted

    @Test
    fun `querySubmitted carries query text`() {
        val event = ConciergeTrackingEvent.QuerySubmitted("What tools do you offer?").toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED,
            ConciergeConstants.TrackingEvent.XDMType.QUERY_SUBMITTED
        )
        assertEquals(
            "What tools do you offer?",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.QUERY)
        )
    }

    // MARK: - promptSuggestionClicked

    @Test
    fun `promptSuggestionClicked carries suggestion text`() {
        val event = ConciergeTrackingEvent.PromptSuggestionClicked("Tell me about Photoshop").toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.PROMPT_SUGGESTION_CLICKED,
            ConciergeConstants.TrackingEvent.XDMType.PROMPT_SUGGESTION_CLICKED
        )
        assertEquals(
            "Tell me about Photoshop",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.SUGGESTION)
        )
    }

    // MARK: - cardClicked

    @Test
    fun `cardClicked carries element dict`() {
        val element = mapOf(
            "productName" to "Adobe Photoshop",
            "productPageURL" to "https://adobe.com/photoshop"
        )
        val event = ConciergeTrackingEvent.CardClicked(element).toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.CARD_CLICKED,
            ConciergeConstants.TrackingEvent.XDMType.CARD_CLICKED
        )
        @Suppress("UNCHECKED_CAST")
        val eventElement = event.eventData?.get(
            ConciergeConstants.TrackingEvent.EventData.Key.ELEMENT
        ) as? Map<String, Any>
        assertEquals("Adobe Photoshop", eventElement?.get("productName"))
        assertEquals("https://adobe.com/photoshop", eventElement?.get("productPageURL"))
    }

    // MARK: - responseStarted

    @Test
    fun `responseStarted carries conversationId and interactionId`() {
        val event = ConciergeTrackingEvent.ResponseStarted("conv-123", "int-456").toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED,
            ConciergeConstants.TrackingEvent.XDMType.RESPONSE_STARTED
        )
        assertEquals(
            "conv-123",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.CONVERSATION_ID)
        )
        assertEquals(
            "int-456",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID)
        )
    }

    // MARK: - responseCompleted

    @Test
    fun `responseCompleted carries conversationId and interactionId`() {
        val event = ConciergeTrackingEvent.ResponseCompleted("conv-123", "int-456").toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.RESPONSE_COMPLETED,
            ConciergeConstants.TrackingEvent.XDMType.RESPONSE_COMPLETED
        )
        assertEquals(
            "conv-123",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.CONVERSATION_ID)
        )
        assertEquals(
            "int-456",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID)
        )
    }

    // MARK: - cardsRendered

    @Test
    fun `cardsRendered single card sets displayMode to single`() {
        val elements = listOf(mapOf("productName" to "Product A"))
        val event = ConciergeTrackingEvent.CardsRendered("single", elements).toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.CARDS_RENDERED,
            ConciergeConstants.TrackingEvent.XDMType.CARDS_RENDERED
        )
        assertEquals(
            "single",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.DISPLAY_MODE)
        )
        @Suppress("UNCHECKED_CAST")
        val eventElements = event.eventData?.get(
            ConciergeConstants.TrackingEvent.EventData.Key.ELEMENTS
        ) as? List<Map<String, Any>>
        assertEquals(1, eventElements?.size)
    }

    @Test
    fun `cardsRendered multiple cards sets displayMode to carousel`() {
        val elements = listOf(
            mapOf("productName" to "Product A"),
            mapOf("productName" to "Product B")
        )
        val event = ConciergeTrackingEvent.CardsRendered("carousel", elements).toEvent()

        assertEquals(
            "carousel",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.DISPLAY_MODE)
        )
        @Suppress("UNCHECKED_CAST")
        val eventElements = event.eventData?.get(
            ConciergeConstants.TrackingEvent.EventData.Key.ELEMENTS
        ) as? List<Map<String, Any>>
        assertEquals(2, eventElements?.size)
    }

    // MARK: - feedbackSubmitted

    @Test
    fun `feedbackSubmitted carries all payload fields`() {
        val event = ConciergeTrackingEvent.FeedbackSubmitted(
            conversationId = "conv-123",
            interactionId = "int-456",
            feedbackType = "negative",
            selectedOptions = listOf("Incorrect information", "Not relevant"),
            notes = "Response did not address pricing"
        ).toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.FEEDBACK_SUBMITTED,
            ConciergeConstants.TrackingEvent.XDMType.FEEDBACK_SUBMITTED
        )
        assertEquals(
            "conv-123",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.CONVERSATION_ID)
        )
        assertEquals(
            "int-456",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID)
        )
        assertEquals(
            "negative",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.FEEDBACK_TYPE)
        )
        @Suppress("UNCHECKED_CAST")
        val options = event.eventData?.get(
            ConciergeConstants.TrackingEvent.EventData.Key.SELECTED_OPTIONS
        ) as? List<String>
        assertEquals(listOf("Incorrect information", "Not relevant"), options)
        assertEquals(
            "Response did not address pricing",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.NOTES)
        )
    }

    // MARK: - errorOccurred

    @Test
    fun `errorOccurred carries error message`() {
        val event = ConciergeTrackingEvent.ErrorOccurred("Server was unreachable.").toEvent()

        assertCommonEventProperties(
            event,
            ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED,
            ConciergeConstants.TrackingEvent.XDMType.ERROR_OCCURRED
        )
        assertEquals(
            "Server was unreachable.",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ERROR_MESSAGE)
        )
    }

    // MARK: - Shared Event Hub contract

    @Test
    fun `all events share the same type and source`() {
        val allEvents = listOf(
            ConciergeTrackingEvent.SessionInitialized,
            ConciergeTrackingEvent.QuerySubmitted("q"),
            ConciergeTrackingEvent.PromptSuggestionClicked("s"),
            ConciergeTrackingEvent.CardClicked(emptyMap()),
            ConciergeTrackingEvent.ResponseStarted("c", "i"),
            ConciergeTrackingEvent.ResponseCompleted("c", "i"),
            ConciergeTrackingEvent.CardsRendered("single", emptyList()),
            ConciergeTrackingEvent.FeedbackSubmitted("c", "i", "positive", emptyList(), ""),
            ConciergeTrackingEvent.ErrorOccurred("err")
        ).map { it.toEvent() }

        allEvents.forEach { event ->
            assertEquals(ConciergeConstants.EventType.CONCIERGE, event.type)
            assertEquals(ConciergeConstants.EventSource.NOTIFICATION, event.source)
        }
    }
}
