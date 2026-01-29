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

package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
class ConciergeChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var mockServiceProvider: ServiceProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = mockk(relaxed = true)
        // Default: grant audio permission
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        
        // Mock ServiceProvider and UriService
        mockkStatic(ServiceProvider::class)
        mockServiceProvider = mockk<ServiceProvider>(relaxed = true)
        every { ServiceProvider.getInstance() } returns mockServiceProvider
        every { mockServiceProvider.uriService } returns mockk(relaxed = true)
        every { mockServiceProvider.uriService.openUri(any()) } returns true
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `mic start with permission starts capture and sets Recording state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(MicEvent.StartRecording)

        assertTrue(fakeSpeech.startCalled)
        assertTrue(vm.inputState.value is UserInputState.Recording)
    }

    @Test
    fun `mic start without permission sets Error and does not start capture`() = runTest {
        // Deny permission before creating VM, so initial state reflects it
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED

        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(MicEvent.StartRecording)

        assertTrue(vm.inputState.value is UserInputState.Error)
        assertTrue(!fakeSpeech.startCalled)
    }

    @Test
    fun `speech listener updates input state for partial and final results`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        // Simulate callbacks from the speech engine
        fakeSpeech.emitSpeechStarted()
        assertTrue(vm.inputState.value is UserInputState.Recording)

        fakeSpeech.emitPartialTranscription("hello wor")
        val recordingState = vm.inputState.value
        assertTrue(recordingState is UserInputState.Recording)
        val rec = recordingState as UserInputState.Recording
        assertEquals("hello wor", rec.transcription)

        fakeSpeech.emitTranscriptionResult("hello world")
        val editingState = vm.inputState.value
        assertTrue(editingState is UserInputState.Editing)
        val editing = editingState as UserInputState.Editing
        assertEquals("hello world", editing.content)

        // Blank result transitions to Empty
        fakeSpeech.emitTranscriptionResult("")
        assertTrue(vm.inputState.value is UserInputState.Empty)

        // Error maps to Error state
        fakeSpeech.emitError(SpeechCaptureError.Permission("denied"))
        val errorState = vm.inputState.value
        assertTrue(errorState is UserInputState.Error)
        val err = errorState as UserInputState.Error
        assertTrue(err.message.contains("Microphone permission"))
    }

    @Test
    fun `stop recording transitions to Editing when transcription exists`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        fakeSpeech.emitSpeechStarted()
        fakeSpeech.emitPartialTranscription("draft text")
        vm.processEvent(MicEvent.StopRecording(isCancelled = false, isError = false))

        val stateAfterStop = vm.inputState.value
        assertTrue(stateAfterStop is UserInputState.Editing)
        val editing = stateAfterStop as UserInputState.Editing
        assertEquals("draft text", editing.content)
    }

    @Test
    fun `stop recording transitions to Empty when no transcription`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        fakeSpeech.emitSpeechStarted()
        fakeSpeech.emitPartialTranscription("")
        vm.processEvent(MicEvent.StopRecording(isCancelled = false, isError = false))

        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    @Test
    fun `send message adds user and streams assistant updates to completion`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()

        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Hel", ConversationState.IN_PROGRESS))
            emit(ParsedConversationMessage("lo", ConversationState.IN_PROGRESS))
            // the "completed" message contains the full text
            emit(ParsedConversationMessage("Hello", ConversationState.COMPLETED))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(ChatEvent.SendMessage("Hi"))

        // Run coroutines launched on viewModelScope(Main)
        advanceUntilIdle()

        val messages = vm.messages.value
        // Expect user + assistant
        assertEquals(2, messages.size)
        assertTrue(messages[0].isFromUser)
        assertEquals("Hi", messages[0].text)
        assertTrue(!messages[1].isFromUser)
        assertEquals("Hello", messages[1].text)

        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `blank COMPLETED does not overwrite assistant and transitions to Idle`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()

        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("You're welcome!", ConversationState.IN_PROGRESS))
            // final completed with empty message
            emit(ParsedConversationMessage("", ConversationState.COMPLETED))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val messages = vm.messages.value
        assertEquals(2, messages.size)
        assertTrue(!messages[1].isFromUser)
        // Expect the last non-blank content to remain
        assertEquals("You're welcome!", messages[1].text)
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `mic stop calls endCapture`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(MicEvent.StopRecording(isCancelled = false, isError = false))
        assertTrue(fakeSpeech.endCalled)
    }

    @Test
    fun `stop recording when not recording keeps input state unchanged`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        // Initial state is Empty
        assertTrue(vm.inputState.value is UserInputState.Empty)
        vm.processEvent(MicEvent.StopRecording(isCancelled = false, isError = false))
        // Still Empty (no forced transition)
        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    @Test
    fun `hasAudioPermission initial and refresh reflects permission changes`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        // Granted from setup
        assertTrue(vm.hasAudioPermission.value)

        // Deny and refresh
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        vm.refreshPermissionStatus()
        assertTrue(!vm.hasAudioPermission.value)

        // Grant back and refresh
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED
        vm.refreshPermissionStatus()
        assertTrue(vm.hasAudioPermission.value)
    }

    @Test
    fun `ChatEvent Error and Reset update chat state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(ChatEvent.Error("bad"))
        val err = vm.state.value as ChatScreenState.Error
        assertEquals("bad", err.error)

        vm.processEvent(ChatEvent.Reset)
        assertTrue(vm.state.value is ChatScreenState.Idle)
        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    @Test
    fun `blank sendMessage is ignored`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(ChatEvent.SendMessage(""))

        assertEquals(0, vm.messages.value.size)
        assertTrue(vm.state.value is ChatScreenState.Idle)
        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    @Test
    fun `send non-blank sets Processing and clears input`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        // Return a flow that never emits, so state remains Processing until we advance time
        every { chatClient.chat("Hello") } returns flow { }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        // Put some text state before sending to ensure it clears
        vm.onTextStateChanged("temp")

        vm.processEvent(ChatEvent.SendMessage("Hello"))

        // Immediately after send: Processing and input cleared
        assertTrue(vm.state.value is ChatScreenState.Processing)
        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    // TODO: Need to revisit tests oncce we finalize how to surface errors
    @Test
    fun `stream ERROR state adds error chat message and returns Idle`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("oops", ConversationState.ERROR))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val last = vm.messages.value.last()
        assertTrue(!last.isFromUser)
        assertTrue(last.text.startsWith("Sorry, I encountered an error:"))
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }
    
    // TODO: Need to revisit tests oncce we finalize how to surface errors
    @Test
    fun `exception from chat flow handled as conversation error`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            throw RuntimeException("boom")
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val last = vm.messages.value.last()
        assertTrue(last.text.startsWith("Sorry, I encountered an error:"))
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `initial assistant message is created before any stream chunk`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        // Flow with no emissions; assistant message should still be created when coroutine starts
        every { chatClient.chat("Hi") } returns flow { }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hi"))

        // Process initial tasks so the coroutine runs to the point of creating assistant message
        runCurrent()

        val messages = vm.messages.value
        // User + initial assistant (empty text)
        assertEquals(2, messages.size)
        assertTrue(!messages[1].isFromUser)
        assertEquals("", messages[1].text)
    }

    @Test
    fun `productActionClick with valid URL calls openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val button = ProductActionButton(
            id = "test_button",
            text = "Buy Now",
            url = "https://example.com/product"
        )
        
        vm.processEvent(MessageInteractionEvent.ProductActionClick(button))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was called with the correct URL
        verify { mockServiceProvider.uriService.openUri("https://example.com/product") }
    }

    @Test
    fun `productActionClick with null URL does not call openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val button = ProductActionButton(
            id = "test_button",
            text = "Buy Now",
            url = null
        )
        
        vm.processEvent(MessageInteractionEvent.ProductActionClick(button))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was not called
        verify(exactly = 0) { mockServiceProvider.uriService.openUri(any()) }
    }

    @Test
    fun `productActionClick with empty URL does not call openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val button = ProductActionButton(
            id = "test_button",
            text = "Buy Now",
            url = ""
        )
        
        vm.processEvent(MessageInteractionEvent.ProductActionClick(button))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was not called
        verify(exactly = 0) { mockServiceProvider.uriService.openUri(any()) }
    }

    @Test
    fun `productImageClick with valid productPageURL calls openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val element = MultimodalElement(
            id = "test_element",
            content = mapOf(
                "productPageURL" to "https://example.com/product-page",
                "productName" to "Test Product"
            )
        )
        
        vm.processEvent(MessageInteractionEvent.ProductImageClick(element))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was called with the correct URL
        verify { mockServiceProvider.uriService.openUri("https://example.com/product-page") }
    }

    @Test
    fun `productImageClick with null productPageURL does not call openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val element = MultimodalElement(
            id = "test_element",
            content = mapOf(
                "productName" to "Test Product"
                // productPageURL is missing
            )
        )
        
        vm.processEvent(MessageInteractionEvent.ProductImageClick(element))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was not called
        verify(exactly = 0) { mockServiceProvider.uriService.openUri(any()) }
    }

    @Test
    fun `productImageClick with empty productPageURL does not call openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val element = MultimodalElement(
            id = "test_element",
            content = mapOf(
                "productPageURL" to "",
                "productName" to "Test Product"
            )
        )
        
        vm.processEvent(MessageInteractionEvent.ProductImageClick(element))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was not called
        verify(exactly = 0) { mockServiceProvider.uriService.openUri(any()) }
    }

    @Test
    fun `productImageClick with invalid productPageURL does not call openUri`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val element = MultimodalElement(
            id = "test_element",
            content = mapOf(
                "productPageURL" to 1234,
                "productName" to "Test Product"
            )
        )
        
        vm.processEvent(MessageInteractionEvent.ProductImageClick(element))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that openUri was not called
        verify(exactly = 0) { mockServiceProvider.uriService.openUri(any()) }
    }

    /**
     * Simple controllable fake for SpeechCapturing.
     */
    private class FakeSpeechCapturing : SpeechCapturing {
        private var listener: SpeechCaptureListener? = null
        var startCalled: Boolean = false
            private set
        var endCalled: Boolean = false
            private set
        var released: Boolean = false
            private set

        override fun isAvailable(): Boolean = true

        override fun setListener(listener: SpeechCaptureListener?) {
            this.listener = listener
        }

        override fun startCapture() {
            startCalled = true
            listener?.onSpeechStarted()
        }

        override fun endCapture() {
            endCalled = true
            listener?.onSpeechEnded()
        }

        override fun release() {
            released = true
        }

        fun emitSpeechStarted() { listener?.onSpeechStarted() }
        fun emitPartialTranscription(text: String) { listener?.onPartialTranscription(text) }
        fun emitTranscriptionResult(text: String) { listener?.onTranscriptionResult(text) }
        fun emitError(error: SpeechCaptureError) { listener?.onError(error) }
    }

    @Test
    fun `sources are parsed and included in chat messages`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        
        val testSources = listOf(
            Citation(
                title = "Adobe Firefly - Free Generative AI for creatives",
                url = "https://www.adobe.com/products/firefly.html",
                citationNumber = 1,
                startIndex = 66,
                endIndex = 78  // "Adobe Firefly" - corrected to actual text length
            ),
            Citation(
                title = "AI painting Generator - Adobe Firefly",
                url = "https://www.adobe.com/products/firefly/features/ai-painting-generator.html",
                citationNumber = 2,
                startIndex = 90,  // "AI painting" - corrected position
                endIndex = 101    // corrected to actual text length
            )
        )
        
        val parsedMessage = ParsedConversationMessage(
            messageContent = "Here are some popular types to look for: Adobe Firefly is a great tool for AI painting generation.",
            state = ConversationState.COMPLETED,
            sources = testSources
        )
        
        every { chatClient.chat("test") } returns flow { emit(parsedMessage) }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("test"))

        // Wait for all coroutines to complete
        advanceUntilIdle()

        val messages = vm.messages.value
        assertEquals(2, messages.size) // User message + assistant message
        
        val assistantMessage = messages[1]
        assertTrue(!assistantMessage.isFromUser)
        assertNotNull(assistantMessage.citations)
        assertEquals(2, assistantMessage.citations?.size)
        
        val citations = assistantMessage.citations!!
        assertEquals("Adobe Firefly - Free Generative AI for creatives", citations[0].title)
        assertEquals("AI painting Generator - Adobe Firefly", citations[1].title)
        assertEquals(1, citations[0].citationNumber)
        assertEquals(2, citations[1].citationNumber)
    }

    // ========== Welcome Card Tests ==========

    @Test
    fun `welcome card is shown initially when config allows`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        assertTrue(vm.showWelcomeCard.value)
        assertTrue(vm.welcomeConfig.showWelcomeCard)
    }

    @Test
    fun `dismissWelcomeCard hides the welcome card`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        assertTrue(vm.showWelcomeCard.value)
        vm.dismissWelcomeCard()
        assertTrue(!vm.showWelcomeCard.value)
    }

    @Test
    fun `sending first message dismisses welcome card`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("First message") } returns flow {
            emit(ParsedConversationMessage("Response", ConversationState.COMPLETED))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        assertTrue(vm.showWelcomeCard.value)
        
        vm.processEvent(ChatEvent.SendMessage("First message"))
        advanceUntilIdle()
        
        assertTrue(!vm.showWelcomeCard.value)
    }

    // ========== Feedback Tests ==========

    @Test
    fun `thumbsUp event shows positive feedback dialog`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.ThumbsUp("test-interaction-id"))
        
        val state = vm.state.value as ChatScreenState.Idle
        assertNotNull(state.feedback)
        assertEquals("test-interaction-id", state.feedback?.interactionId)
        assertEquals(com.adobe.marketing.mobile.concierge.ui.state.FeedbackType.POSITIVE, state.feedback?.feedbackType)
    }

    @Test
    fun `thumbsDown event shows negative feedback dialog`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.ThumbsDown("test-interaction-id"))
        
        val state = vm.state.value as ChatScreenState.Idle
        assertNotNull(state.feedback)
        assertEquals("test-interaction-id", state.feedback?.interactionId)
        assertEquals(com.adobe.marketing.mobile.concierge.ui.state.FeedbackType.NEGATIVE, state.feedback?.feedbackType)
    }

    @Test
    fun `dismissFeedbackDialog clears feedback state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.ThumbsUp("test-id"))
        val stateWithFeedback = vm.state.value as ChatScreenState.Idle
        assertNotNull(stateWithFeedback.feedback)
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.DismissFeedbackDialog)
        
        val stateAfterDismiss = vm.state.value as ChatScreenState.Idle
        assertNull(stateAfterDismiss.feedback)
    }

    @Test
    fun `submitFeedback sends feedback and updates message state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        coEvery { chatClient.sendFeedback(any()) } returns true
        
        // Create a message flow with an interactionId
        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Response",
                state = ConversationState.COMPLETED,
                interactionId = "interaction-123"
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        // Send a message to create an assistant message with an interactionId
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()
        
        val feedback = com.adobe.marketing.mobile.concierge.ui.state.Feedback(
            interactionId = "interaction-123",
            feedbackType = com.adobe.marketing.mobile.concierge.ui.state.FeedbackType.POSITIVE,
            selectedCategories = listOf("Helpful", "Accurate"),
            notes = "Great response!"
        )
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.SubmitFeedback(feedback))
        advanceUntilIdle()
        
        // Verify feedback was sent
        coVerify { chatClient.sendFeedback(any()) }
        
        // Verify feedback dialog is dismissed
        val state = vm.state.value as ChatScreenState.Idle
        assertNull(state.feedback)
        
        // Verify message was updated with feedback state
        val messages = vm.messages.value
        val assistantMessage = messages.last()
        assertEquals(FeedbackState.Positive, assistantMessage.feedbackState)
    }

    @Test
    fun `submitFeedback with negative feedback updates message state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        coEvery { chatClient.sendFeedback(any()) } returns true
        
        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Response",
                state = ConversationState.COMPLETED,
                interactionId = "interaction-456"
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()
        
        val feedback = com.adobe.marketing.mobile.concierge.ui.state.Feedback(
            interactionId = "interaction-456",
            feedbackType = com.adobe.marketing.mobile.concierge.ui.state.FeedbackType.NEGATIVE,
            selectedCategories = listOf("Unhelpful"),
            notes = "Not relevant"
        )
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.SubmitFeedback(feedback))
        advanceUntilIdle()
        
        val messages = vm.messages.value
        val assistantMessage = messages.last()
        assertEquals(FeedbackState.Negative, assistantMessage.feedbackState)
    }

    // ========== Prompt Suggestion Tests ==========

    @Test
    fun `promptSuggestionClick sets text in input field`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("What can you do?"))
        
        val inputState = vm.inputState.value as UserInputState.Editing
        assertEquals("What can you do?", inputState.content)
    }

    // ========== Text Input State Tests ==========

    @Test
    fun `onTextStateChanged with empty text sets Empty state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.onTextStateChanged("")
        
        assertTrue(vm.inputState.value is UserInputState.Empty)
    }

    @Test
    fun `onTextStateChanged with non-empty text sets Editing state`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.onTextStateChanged("Hello")
        
        val state = vm.inputState.value as UserInputState.Editing
        assertEquals("Hello", state.content)
    }

    // ========== Open/Close Concierge Tests ==========

    @Test
    fun `openConcierge sets isConciergeActive to true`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        assertTrue(!vm.isConciergeActive.value)
        
        vm.openConcierge()
        
        assertTrue(vm.isConciergeActive.value)
    }

    @Test
    fun `closeConcierge sets isConciergeActive to false`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        vm.openConcierge()
        assertTrue(vm.isConciergeActive.value)
        
        vm.closeConcierge()
        
        assertTrue(!vm.isConciergeActive.value)
    }

    // ========== Multimodal Content Tests ==========

    @Test
    fun `message with multimodal elements uses Mixed content type`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        
        val multimodalElements = listOf(
            MultimodalElement(
                id = "element-1",
                url = "https://example.com/image.jpg",
                content = mapOf("type" to "image")
            )
        )
        
        every { chatClient.chat("Show me products") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Here are some products",
                state = ConversationState.COMPLETED,
                multimodalElements = multimodalElements
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Show me products"))
        advanceUntilIdle()
        
        val messages = vm.messages.value
        val assistantMessage = messages.last()
        
        assertTrue(assistantMessage.content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Mixed)
        val mixedContent = assistantMessage.content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Mixed
        assertEquals("Here are some products", mixedContent.text)
        assertEquals(1, mixedContent.multimodalElements?.size)
    }

    @Test
    fun `message without multimodal elements uses Text content type`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        
        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Hi there",
                state = ConversationState.COMPLETED,
                multimodalElements = emptyList()
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()
        
        val messages = vm.messages.value
        val assistantMessage = messages.last()
        
        assertTrue(assistantMessage.content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Text)
        val textContent = assistantMessage.content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Text
        assertEquals("Hi there", textContent.text)
    }

    // ========== Prompt Suggestions in Messages Tests ==========

    @Test
    fun `message includes prompt suggestions when provided`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        
        val suggestions = listOf("Tell me more", "Show examples", "Explain further")
        
        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Hi! How can I help?",
                state = ConversationState.COMPLETED,
                promptSuggestions = suggestions
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()
        
        val messages = vm.messages.value
        val assistantMessage = messages.last()
        
        assertEquals(3, assistantMessage.promptSuggestions.size)
        assertEquals("Tell me more", assistantMessage.promptSuggestions[0])
        assertEquals("Show examples", assistantMessage.promptSuggestions[1])
        assertEquals("Explain further", assistantMessage.promptSuggestions[2])
    }

    // ========== Conversation ID Tests ==========

    @Test
    fun `conversationId is captured and used in feedback`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        // Capture the feedback argument
        val feedbackSlot = slot<com.adobe.marketing.mobile.concierge.ui.state.Feedback>()
        coEvery { chatClient.sendFeedback(capture(feedbackSlot)) } returns true
        
        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Response",
                state = ConversationState.COMPLETED,
                conversationId = "conv-123",
                interactionId = "interaction-456"
            ))
        }
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()
        
        val feedback = com.adobe.marketing.mobile.concierge.ui.state.Feedback(
            interactionId = "interaction-456",
            feedbackType = com.adobe.marketing.mobile.concierge.ui.state.FeedbackType.POSITIVE
        )
        
        vm.processEvent(com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent.SubmitFeedback(feedback))
        advanceUntilIdle()
        
        // Verify the captured feedback includes the conversationId
        assertEquals("conv-123", feedbackSlot.captured.conversationId)
    }

    // ========== Theme Config Tests ==========

    @Test
    fun `updateWelcomeConfigFromTheme updates welcome config`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        val themeConfig = com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig(
            name = "Test Brand",
            text = com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTextStrings(
                welcomeHeading = "Custom Welcome Header",
                welcomeSubheading = "Custom Subheader"
            )
        )
        
        vm.updateWelcomeConfigFromTheme(themeConfig)
        
        assertEquals("Custom Welcome Header", vm.welcomeConfig.welcomeHeader)
        assertEquals("Custom Subheader", vm.welcomeConfig.subHeader)
    }

    @Test
    fun `updateWelcomeConfigFromTheme with null does not change config`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        
        val originalConfig = vm.welcomeConfig
        
        vm.updateWelcomeConfigFromTheme(null)
        
        // Config should remain unchanged
        assertEquals(originalConfig, vm.welcomeConfig)
    }
}


