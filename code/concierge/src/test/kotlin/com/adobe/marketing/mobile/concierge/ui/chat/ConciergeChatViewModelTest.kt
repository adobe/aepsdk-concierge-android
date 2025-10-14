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
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
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
}


