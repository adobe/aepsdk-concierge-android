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

package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ConciergeChatViewModelTrackingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        mockkStatic(ServiceProvider::class)
        val mockServiceProvider = mockk<ServiceProvider>(relaxed = true)
        every { ServiceProvider.getInstance() } returns mockServiceProvider
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        Dispatchers.resetMain()
    }

    // MARK: - sessionInitialized

    @Test
    fun `sessionInitialized is dispatched on construction`() = runTest {
        val dispatched = mutableListOf<Event>()
        makeViewModel(dispatch = { dispatched.add(it) })

        val event = dispatched.singleOrNull {
            it.name == ConciergeConstants.TrackingEvent.Name.SESSION_INITIALIZED
        }
        assertTrue("Expected sessionInitialized event", event != null)
    }

    @Test
    fun `sessionInitialized dispatched exactly once per instance`() = runTest {
        val dispatched = mutableListOf<Event>()
        makeViewModel(dispatch = { dispatched.add(it) })

        val count = dispatched.count {
            it.name == ConciergeConstants.TrackingEvent.Name.SESSION_INITIALIZED
        }
        assertEquals(1, count)
    }

    // MARK: - querySubmitted

    @Test
    fun `querySubmitted carries the user message text`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("What tools do you offer?"))

        val event = dispatched.single {
            it.name == ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED
        }
        assertEquals(
            "What tools do you offer?",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.QUERY)
        )
    }

    @Test
    fun `blank message does not dispatch querySubmitted`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })
        dispatched.clear()

        vm.processEvent(ChatEvent.SendMessage("   "))

        assertTrue(dispatched.none {
            it.name == ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED
        })
    }

    // MARK: - promptSuggestionClicked

    @Test
    fun `promptSuggestionClicked fires before querySubmitted`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })
        dispatched.clear()

        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("Tell me about Premiere"))

        val names = dispatched.map { it.name }
        val suggestionIdx = names.indexOf(ConciergeConstants.TrackingEvent.Name.PROMPT_SUGGESTION_CLICKED)
        val queryIdx = names.indexOf(ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED)
        assertTrue("promptSuggestionClicked should precede querySubmitted", suggestionIdx < queryIdx)
    }

    @Test
    fun `promptSuggestionClicked carries suggestion text`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })
        dispatched.clear()

        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("Tell me about Premiere"))

        val event = dispatched.single {
            it.name == ConciergeConstants.TrackingEvent.Name.PROMPT_SUGGESTION_CLICKED
        }
        assertEquals(
            "Tell me about Premiere",
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.SUGGESTION)
        )
    }

    // MARK: - responseStarted & responseCompleted

    @Test
    fun `responseStarted fires once even across multiple IN_PROGRESS chunks`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Hel", ConversationState.IN_PROGRESS, interactionId = "int-1"))
            emit(ParsedConversationMessage("lo", ConversationState.IN_PROGRESS, interactionId = "int-1"))
            emit(ParsedConversationMessage("Hello", ConversationState.COMPLETED, interactionId = "int-1"))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val startedEvents = dispatched.filter {
            it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED
        }
        assertEquals("responseStarted should fire exactly once", 1, startedEvents.size)
    }

    @Test
    fun `responseCompleted fires once when stream finishes`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Hello", ConversationState.COMPLETED, interactionId = "int-1"))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val completedEvents = dispatched.filter {
            it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_COMPLETED
        }
        assertEquals(1, completedEvents.size)
    }

    @Test
    fun `responseStarted and responseCompleted carry conversationId and interactionId`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage(
                "Hello", ConversationState.IN_PROGRESS,
                conversationId = "conv-99", interactionId = "int-77"
            ))
            emit(ParsedConversationMessage(
                "Hello world", ConversationState.COMPLETED,
                conversationId = "conv-99", interactionId = "int-77"
            ))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val started = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED }
        assertEquals("int-77", started.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID))

        val completed = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_COMPLETED }
        assertEquals("conv-99", completed.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.CONVERSATION_ID))
        assertEquals("int-77", completed.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID))
    }

    @Test
    fun `responseStarted flag resets between turns`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat(any()) } returns flow {
            emit(ParsedConversationMessage("Reply", ConversationState.IN_PROGRESS, interactionId = "int-1"))
            emit(ParsedConversationMessage("Reply done", ConversationState.COMPLETED, interactionId = "int-1"))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Turn 1"))
        advanceUntilIdle()
        vm.processEvent(ChatEvent.SendMessage("Turn 2"))
        advanceUntilIdle()

        val startedCount = dispatched.count { it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED }
        assertEquals("responseStarted should fire once per turn", 2, startedCount)
    }

    @Test
    fun `cards-only response still fires paired responseStarted and responseCompleted`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val card = ParsedMultimodalItem.Card(
            MultimodalElement(id = "c1", content = mapOf("productName" to "Photoshop"))
        )
        every { chatClient.chat("show cards") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(card),
                interactionId = "int-cards"
            ))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("show cards"))
        advanceUntilIdle()

        val started = dispatched.count { it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_STARTED }
        val completed = dispatched.count { it.name == ConciergeConstants.TrackingEvent.Name.RESPONSE_COMPLETED }
        assertEquals("responseStarted should fire even when messageContent is blank but cards exist", 1, started)
        assertEquals("responseCompleted should remain paired with responseStarted", 1, completed)
    }

    // MARK: - chatOpened & chatClosed

    @Test
    fun `trackChatClosed before trackChatOpened emits duration 0`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        vm.trackChatClosed()

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.CHAT_CLOSED }
        assertEquals(
            0L,
            event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.DURATION_MILLIS)
        )
    }

    // MARK: - cardsRendered

    @Test
    fun `cardsRendered fires with single displayMode for one card`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val cardElement = MultimodalElement(
            id = "card-1",
            content = mapOf("productName" to "Photoshop", "productPageURL" to "https://adobe.com/ps")
        )
        every { chatClient.chat("show cards") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(ParsedMultimodalItem.Card(cardElement))
            ))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("show cards"))
        advanceUntilIdle()

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.CARDS_RENDERED }
        assertEquals("single", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.DISPLAY_MODE))
    }

    @Test
    fun `cardsRendered fires with carousel displayMode for multiple cards`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val cards = (1..3).map { i ->
            ParsedMultimodalItem.Card(
                MultimodalElement(id = "card-$i", content = mapOf("productName" to "Product $i"))
            )
        }
        every { chatClient.chat("show carousel") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "",
                state = ConversationState.COMPLETED,
                orderedElements = cards
            ))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("show carousel"))
        advanceUntilIdle()

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.CARDS_RENDERED }
        assertEquals("carousel", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.DISPLAY_MODE))
        @Suppress("UNCHECKED_CAST")
        val elements = event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ELEMENTS) as? List<*>
        assertEquals(3, elements?.size)
    }

    // MARK: - feedbackSubmitted

    @Test
    fun `feedbackSubmitted carries all payload fields`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        val feedback = Feedback(
            interactionId = "int-123",
            feedbackType = FeedbackType.NEGATIVE,
            selectedCategories = listOf("Incorrect", "Unhelpful"),
            notes = "Did not match my query"
        )
        vm.processEvent(FeedbackEvent.SubmitFeedback(feedback))

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.FEEDBACK_SUBMITTED }
        assertEquals("negative", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.FEEDBACK_TYPE))
        assertEquals("int-123", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.INTERACTION_ID))
        assertEquals("Did not match my query", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.NOTES))
        @Suppress("UNCHECKED_CAST")
        val options = event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.SELECTED_OPTIONS) as? List<String>
        assertEquals(listOf("Incorrect", "Unhelpful"), options)
    }

    @Test
    fun `feedbackSubmitted maps positive feedbackType correctly`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        vm.processEvent(FeedbackEvent.SubmitFeedback(
            Feedback(interactionId = "int-1", feedbackType = FeedbackType.POSITIVE)
        ))

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.FEEDBACK_SUBMITTED }
        assertEquals("positive", event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.FEEDBACK_TYPE))
    }

    // MARK: - errorOccurred

    @Test
    fun `errorOccurred fires on stream ERROR state`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("oops", ConversationState.ERROR))
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        assertTrue(dispatched.any { it.name == ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED })
    }

    @Test
    fun `errorOccurred fires on exception thrown by chat flow`() = runTest {
        val dispatched = mutableListOf<Event>()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            throw RuntimeException("network down")
        }
        val vm = makeViewModel(chatClient = chatClient, dispatch = { dispatched.add(it) })

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        assertTrue(dispatched.any { it.name == ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED })
    }

    // MARK: - cardClicked

    @Test
    fun `cardClicked fires for product image click with element data`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        val element = MultimodalElement(
            id = "e1",
            content = mapOf(
                "productName" to "Photoshop",
                "productPageURL" to "https://adobe.com/ps",
                "productPrice" to "$9.99"
            )
        )
        vm.processEvent(MessageInteractionEvent.ProductImageClick(element))

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.CARD_CLICKED }
        @Suppress("UNCHECKED_CAST")
        val dict = event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ELEMENT) as? Map<String, Any>
        assertEquals("Photoshop", dict?.get("productName"))
        assertEquals("https://adobe.com/ps", dict?.get("productPageURL"))
    }

    @Test
    fun `cardClicked fires for product action button click`() = runTest {
        val dispatched = mutableListOf<Event>()
        val vm = makeViewModel(dispatch = { dispatched.add(it) })

        val button = ProductActionButton(id = "btn-1", text = "Buy Now", url = "https://adobe.com/buy")
        vm.processEvent(MessageInteractionEvent.ProductActionClick(button))

        val event = dispatched.single { it.name == ConciergeConstants.TrackingEvent.Name.CARD_CLICKED }
        @Suppress("UNCHECKED_CAST")
        val dict = event.eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ELEMENT) as? Map<String, Any>
        assertEquals("Buy Now", dict?.get("productName"))
        assertEquals("https://adobe.com/buy", dict?.get("productPageURL"))
    }

    // MARK: - Helpers

    private fun makeViewModel(
        chatClient: ConciergeConversationServiceClient = mockk(relaxed = true),
        dispatch: ((Event) -> Unit)? = null
    ): ConciergeChatViewModel {
        return ConciergeChatViewModel(
            app,
            FakeSpeechCapturing(),
            mockk<ImageProvider>(relaxed = true),
            chatClient,
            dispatch
        )
    }

    private class FakeSpeechCapturing : SpeechCapturing {
        private var listener: SpeechCaptureListener? = null
        override fun isAvailable() = true
        override fun setListener(listener: SpeechCaptureListener?) { this.listener = listener }
        override fun startCapture() { listener?.onSpeechStarted() }
        override fun endCapture() { listener?.onSpeechEnded() }
        override fun release() {}
    }
}
