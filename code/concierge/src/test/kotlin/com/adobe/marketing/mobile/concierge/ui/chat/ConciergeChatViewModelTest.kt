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
import android.net.Uri
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffEvent
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffEventHandler
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.ConciergeConversationServiceClient
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.card.ProductActionButton
import com.adobe.marketing.mobile.concierge.ui.components.footer.FeedbackState
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureError
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import com.adobe.marketing.mobile.concierge.utils.tryOpenWithSystemHandler
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.Runs
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

        // Scheme mocks for handleLinkClick tests
        mockkStatic(Uri::class)
        mockkStatic(::tryOpenAsAppLink)
        mockkStatic(::tryOpenWithSystemHandler)
        every { Uri.parse(any()) } answers {
            val url = firstArg<String>()
            val scheme = url.substringBefore(':', "").lowercase().ifEmpty { null }
            mockk { every { this@mockk.scheme } returns scheme }
        }
        every { tryOpenAsAppLink(any(), any()) } returns false
        every { tryOpenWithSystemHandler(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        unmockkStatic(Uri::class)
        unmockkStatic(::tryOpenAsAppLink)
        unmockkStatic(::tryOpenWithSystemHandler)
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
    fun `audio level updates flow into Recording state and survive partial transcription updates`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        fakeSpeech.emitSpeechStarted()
        val started = vm.inputState.value
        assertTrue(started is UserInputState.Recording)
        assertEquals(0f, (started as UserInputState.Recording).audioLevel)

        fakeSpeech.emitAudioLevel(0.6f)
        val afterLevel = vm.inputState.value
        assertTrue(afterLevel is UserInputState.Recording)
        assertEquals(0.6f, (afterLevel as UserInputState.Recording).audioLevel)

        // A subsequent partial transcription should not reset the audio level back to 0
        fakeSpeech.emitPartialTranscription("hello")
        val afterPartial = vm.inputState.value
        assertTrue(afterPartial is UserInputState.Recording)
        val rec = afterPartial as UserInputState.Recording
        assertEquals("hello", rec.transcription)
        assertEquals(0.6f, rec.audioLevel)
    }

    @Test
    fun `audio level updates are ignored when not recording`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        // No recording session started; a stray/late level update should have no effect.
        fakeSpeech.emitAudioLevel(0.9f)

        assertTrue(vm.inputState.value is UserInputState.Empty)
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
    fun `data handoff renders local message and streamed response before reporting delivery`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("", mapOf("orderId" to "abc-123"), "Order placed")
        val card = MultimodalElement(id = "product-1", content = mapOf("productName" to "Widget"))
        every { chatClient.sendDataHandoff("", handoff.xdmFields) } returns flow {
            emit(
                ParsedConversationMessage(
                    messageContent = "You may also like this.",
                    state = ConversationState.COMPLETED,
                    orderedElements = listOf(
                        ParsedMultimodalItem.Card(card),
                        ParsedMultimodalItem.Cta(NetworkCtaButton("Shop now", "https://example.com"))
                    )
                )
            )
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            val messages = vm.messages.value
            assertEquals(4, messages.size)
            assertTrue(messages[0].isFromUser)
            assertEquals("Order placed", messages[0].text)
            assertEquals("You may also like this.", messages[1].text)
            assertTrue(messages[2].content is MessageContent.Mixed)
            assertTrue(messages[3].content is MessageContent.CtaButton)
            assertEquals(DataHandoffDeliveryResult.Delivered, deliveryResult)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff event flows through the handler and active session to a correlated accepted response`() = runTest {
        // Exercises the full production chain end-to-end, not each link in isolation:
        // ConciergeDataHandoffEventHandler.handle -> ActiveConciergeDataHandoffForwarder singleton
        // -> this VM's registered forwarder -> enqueueDataHandoff -> streamed delivery -> response.
        mockkStatic(MobileCore::class)
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoffXdm = mapOf("orderId" to "abc-123")
        every { chatClient.sendDataHandoff("buy_now", handoffXdm) } returns flow {
            emit(ParsedConversationMessage("Thanks for your order!", ConversationState.COMPLETED))
        }
        val dispatchedResponses = mutableListOf<Event>()
        every { MobileCore.dispatchEvent(capture(dispatchedResponses)) } returns Unit

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.activateDataHandoffSession()
        try {
            val requestEvent = Event.Builder(
                ConciergeConstants.DataHandoff.EventName.REQUEST,
                ConciergeConstants.EventType.CONCIERGE,
                EventSource.REQUEST_CONTENT
            ).setEventData(
                mapOf(
                    ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT to "buy_now",
                    ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS to handoffXdm
                )
            ).build()

            // Default constructor wires the real ActiveConciergeDataHandoffForwarder singleton.
            ConciergeDataHandoffEventHandler().handle(requestEvent)
            advanceUntilIdle()

            verify(exactly = 1) { chatClient.sendDataHandoff("buy_now", handoffXdm) }
            val response = dispatchedResponses.single()
            assertEquals(true, response.eventData?.get(ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED))
            // The response must correlate back to the request, or the caller's
            // dispatchEventWithResponseCallback never fires.
            assertEquals(requestEvent.uniqueIdentifier, response.responseID)
        } finally {
            vm.deactivateDataHandoffSession()
            unmockkStatic(MobileCore::class)
        }
    }

    @Test
    fun `data handoff completion can immediately retry after delivery`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        every { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Second response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var firstResult: DataHandoffDeliveryResult? = null
        var secondResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(firstHandoff) { result ->
                firstResult = result
                vm.enqueueDataHandoff(secondHandoff) { secondResult = it }
            }
            advanceUntilIdle()

            assertEquals(DataHandoffDeliveryResult.Delivered, firstResult)
            assertEquals(DataHandoffDeliveryResult.Delivered, secondResult)
            verify(exactly = 1) { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) }
            verify(exactly = 1) { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) }
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff reports chat in progress while a chat turn is active`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstTurnCanFinish = CompletableDeferred<Unit>()
        val callOrder = mutableListOf<String>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.chat("First") } returns flow {
            callOrder += "chat"
            emit(ParsedConversationMessage("First response", ConversationState.IN_PROGRESS))
            firstTurnCanFinish.await()
            emit(ParsedConversationMessage("", ConversationState.COMPLETED))
        }
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            callOrder += "handoff"
            emit(ParsedConversationMessage("Handoff response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            vm.processEvent(ChatEvent.SendMessage("First"))
            var deliveryResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            runCurrent()

            assertEquals(listOf("chat"), callOrder)
            assertEquals(listOf("First", "First response"), vm.messages.value.map { it.text })
            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS),
                deliveryResult
            )
            verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }

            firstTurnCanFinish.complete(Unit)
            advanceUntilIdle()

            assertEquals(listOf("chat"), callOrder)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff reports delivery timeout when its active stream does not finish`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            runCurrent()
            advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            runCurrent()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
                deliveryResult
            )
            advanceUntilIdle()
            verify(exactly = 1) { chatClient.sendDataHandoff("checkout", handoff.xdmFields) }
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff slot is free again immediately after a delivery timeout`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        every { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Second response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            var firstResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(firstHandoff) { firstResult = it }
            runCurrent()
            advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            runCurrent()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
                firstResult
            )

            // A retry issued right after the timeout completion must not be spuriously
            // rejected with CHAT_IN_PROGRESS - the reservation must already be released.
            var secondResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(secondHandoff) { secondResult = it }
            advanceUntilIdle()

            assertEquals(DataHandoffDeliveryResult.Delivered, secondResult)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff reports chat in progress when a chat message is waiting`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.chat("First") } returns flow {
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.processEvent(ChatEvent.SendMessage("First"))
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS),
                deliveryResult
            )
            verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }

            advanceUntilIdle()
            verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff cancels its stream when its session deactivates`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val streamStarted = CompletableDeferred<Unit>()
        var streamCancelled = false
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            streamStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                streamCancelled = true
            }
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            runCurrent()
            assertTrue(streamStarted.isCompleted)

            vm.deactivateDataHandoffSession()
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
                deliveryResult
            )
            assertTrue(streamCancelled)
            assertEquals(ChatScreenState.Idle(), vm.state.value)
            assertTrue(vm.messages.value.isEmpty())
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff slot is free again immediately after a deactivated handoff is cancelled`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val streamStarted = CompletableDeferred<Unit>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            streamStarted.complete(Unit)
            awaitCancellation()
        }
        every { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Second response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            var firstResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(firstHandoff) { firstResult = it }
            runCurrent()
            assertTrue(streamStarted.isCompleted)

            // Deactivating force-completes the still-in-flight first handoff; the reservation it
            // held must be released immediately, not only once the stale request later drains.
            vm.deactivateDataHandoffSession()
            vm.activateDataHandoffSession()

            var secondResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(secondHandoff) { secondResult = it }
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
                firstResult
            )
            assertEquals(DataHandoffDeliveryResult.Delivered, secondResult)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff whose service call throws completes with DELIVERY_FAILED and does not break later requests`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } throws
            IllegalStateException("boom")
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            var deliveryResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                deliveryResult
            )
            assertEquals(ChatScreenState.Idle(), vm.state.value)

            // The shared processor coroutine must still be alive for a later request.
            every { chatClient.chat("hello") } returns flow {
                emit(ParsedConversationMessage("hi", ConversationState.COMPLETED))
            }
            vm.processEvent(ChatEvent.SendMessage("hello"))
            advanceUntilIdle()

            assertEquals(listOf("hello", "hi"), vm.messages.value.map { it.text })
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `chat message that throws does not break later requests`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("boom") } throws IllegalStateException("network unavailable")
        every { chatClient.chat("hello") } returns flow {
            emit(ParsedConversationMessage("hi", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(ChatEvent.SendMessage("boom"))
        advanceUntilIdle()

        assertEquals(ChatScreenState.Idle(), vm.state.value)

        vm.processEvent(ChatEvent.SendMessage("hello"))
        advanceUntilIdle()

        assertEquals(listOf("boom", "hello", "hi"), vm.messages.value.map { it.text })
    }

    @Test
    fun `data handoff reports chat in progress while another handoff is active`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstHandoffCanFinish = CompletableDeferred<Unit>()
        val firstHandoff = ConciergeDataHandoffEvent("first", mapOf("orderId" to "1"))
        val secondHandoff = ConciergeDataHandoffEvent("second", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("first", firstHandoff.xdmFields) } returns flow {
            firstHandoffCanFinish.await()
            emit(ParsedConversationMessage("First response", ConversationState.COMPLETED))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        val deliveryResults = mutableListOf<DataHandoffDeliveryResult>()

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(firstHandoff) { deliveryResults += it }
            runCurrent()
            vm.enqueueDataHandoff(secondHandoff) { deliveryResults += it }

            assertEquals(
                listOf(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS)),
                deliveryResults
            )
            verify(exactly = 0) { chatClient.sendDataHandoff("second", secondHandoff.xdmFields) }

            firstHandoffCanFinish.complete(Unit)
            advanceUntilIdle()
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `chat message is not tracked or cleared when the conversation queue is full`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val firstTurnCanFinish = CompletableDeferred<Unit>()
        val dispatchedEvents = mutableListOf<Event>()
        every { chatClient.chat(any()) } answers {
            val message = invocation.args[0] as String
            flow {
                if (message == "First") {
                    firstTurnCanFinish.await()
                }
                emit(ParsedConversationMessage("$message response", ConversationState.COMPLETED))
            }
        }
        val vm = ConciergeChatViewModel(
            app,
            fakeSpeech,
            DefaultImageProvider(),
            chatClient
        ) { dispatchedEvents += it }

        vm.processEvent(ChatEvent.SendMessage("First"))
        runCurrent()
        repeat(64) { index ->
            vm.processEvent(ChatEvent.SendMessage("Queued $index"))
        }
        val eventsBeforeRejectedMessage = dispatchedEvents.size
        vm.onTextStateChanged("Dropped")

        vm.processEvent(ChatEvent.SendMessage("Dropped"))

        assertEquals(eventsBeforeRejectedMessage, dispatchedEvents.size)
        assertEquals(UserInputState.Editing("Dropped"), vm.inputState.value)
        assertTrue(vm.state.value is ChatScreenState.Error)
        assertEquals(listOf("First", ""), vm.messages.value.map { it.text })

        firstTurnCanFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun `data handoff reports no active session without enqueueing a request`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.enqueueDataHandoff(ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))) {
            deliveryResult = it
        }

        assertEquals(
            DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.NO_ACTIVE_SESSION),
            deliveryResult
        )
        verify(exactly = 0) { chatClient.sendDataHandoff(any(), any()) }
    }

    @Test
    fun `data handoff reports empty response after removing its assistant placeholder`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow { }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            assertEquals(listOf("Order placed"), vm.messages.value.map { it.text })
            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.EMPTY_RESPONSE),
                deliveryResult
            )
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff reports service and timeout failures`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val serviceFailure = ConciergeDataHandoffEvent("service-failure", mapOf("orderId" to "1"))
        val timeout = ConciergeDataHandoffEvent("timeout", mapOf("orderId" to "2"))
        every { chatClient.sendDataHandoff("service-failure", serviceFailure.xdmFields) } returns flow {
            throw IllegalStateException("service failure")
        }
        every { chatClient.sendDataHandoff("timeout", timeout.xdmFields) } returns flow {
            awaitCancellation()
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        val deliveryResults = mutableListOf<DataHandoffDeliveryResult>()

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(serviceFailure) { deliveryResults += it }
            advanceUntilIdle()
            vm.enqueueDataHandoff(timeout) { deliveryResults += it }
            runCurrent()
            advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                    DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT)
                ),
                deliveryResults
            )
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff failure does not render an error bubble in chat`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        // localMessage renders immediately; the forward then fails mid-stream.
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            throw IllegalStateException("service failure")
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                deliveryResult
            )
            // Only the local message remains - no generic error bubble, no leftover placeholder.
            assertEquals(listOf("Order placed"), vm.messages.value.map { it.text })
            assertEquals(ChatScreenState.Idle(), vm.state.value)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff timeout does not render an error bubble in chat`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            awaitCancellation()
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            runCurrent()
            advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
                deliveryResult
            )
            // No localMessage was set and the forward timed out, so the transcript stays empty.
            assertTrue(vm.messages.value.isEmpty())
            assertEquals(ChatScreenState.Idle(), vm.state.value)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff that streams an error frame reports failure without an error bubble`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"), "Order placed")
        // A mid-stream ERROR frame (not a thrown exception) - the distinct hasError branch.
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("raw-server-detail", ConversationState.ERROR))
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        var deliveryResult: DataHandoffDeliveryResult? = null

        vm.activateDataHandoffSession()
        try {
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                deliveryResult
            )
            // Only the local message remains: the ERROR frame renders no bubble and leaves no placeholder.
            assertEquals(listOf("Order placed"), vm.messages.value.map { it.text })
            assertEquals(ChatScreenState.Idle(), vm.state.value)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff failure does not remove a prior turn's assistant message`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Previous answer", ConversationState.COMPLETED))
        }
        // No localMessage; the service call throws synchronously, so streamConversation never runs
        // and no placeholder is created for this turn.
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } throws
            IllegalStateException("boom")
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            vm.processEvent(ChatEvent.SendMessage("Hi"))
            advanceUntilIdle()
            assertEquals(listOf("Hi", "Previous answer"), vm.messages.value.map { it.text })

            var deliveryResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_FAILED),
                deliveryResult
            )
            // The failed handoff must not delete the previous turn's assistant answer.
            assertEquals(listOf("Hi", "Previous answer"), vm.messages.value.map { it.text })
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `data handoff timeout removes only its own partial bubble, not a prior turn`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("Previous answer", ConversationState.COMPLETED))
        }
        val handoff = ConciergeDataHandoffEvent("checkout", mapOf("orderId" to "abc-123"))
        // Streams partial content, then stalls until the delivery timeout cancels it.
        every { chatClient.sendDataHandoff("checkout", handoff.xdmFields) } returns flow {
            emit(ParsedConversationMessage("Working on it", ConversationState.IN_PROGRESS))
            awaitCancellation()
        }
        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.activateDataHandoffSession()
        try {
            vm.processEvent(ChatEvent.SendMessage("Hi"))
            advanceUntilIdle()

            var deliveryResult: DataHandoffDeliveryResult? = null
            vm.enqueueDataHandoff(handoff) { deliveryResult = it }
            runCurrent()
            // The partial handoff bubble is present mid-stream, on top of the prior turn.
            assertEquals(listOf("Hi", "Previous answer", "Working on it"), vm.messages.value.map { it.text })

            advanceTimeBy(ConciergeConstants.DataHandoff.DELIVERY_TIMEOUT_MS)
            advanceUntilIdle()

            assertEquals(
                DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT),
                deliveryResult
            )
            // Only the handoff's own partial bubble is removed; the prior turn survives.
            assertEquals(listOf("Hi", "Previous answer"), vm.messages.value.map { it.text })
            assertEquals(ChatScreenState.Idle(), vm.state.value)
        } finally {
            vm.deactivateDataHandoffSession()
        }
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
        assertEquals("Sorry, I encountered an error. Please try again.", err.error)

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

    @Test
    fun `stream ERROR state shows generic copy and returns Idle`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("oops-raw-server-detail", ConversationState.ERROR))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val last = vm.messages.value.last()
        assertTrue(!last.isFromUser)
        // User sees the generic, themeable message.
        assertEquals("Sorry, I encountered an error. Please try again.", last.text)
        assertTrue(!last.text.contains("oops-raw-server-detail"))
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `stream ERROR state reports an ErrorOccurred tracking event exactly once, not double-wrapped`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            emit(ParsedConversationMessage("oops-raw-server-detail", ConversationState.ERROR))
        }
        val dispatchedEvents = mutableListOf<Event>()

        val vm = ConciergeChatViewModel(
            app,
            fakeSpeech,
            DefaultImageProvider(),
            chatClient
        ) { dispatchedEvents += it }
        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val errorEvents = dispatchedEvents.filter {
            it.name == ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED
        }
        assertEquals(1, errorEvents.size)
        val errorMessage = errorEvents.single().eventData?.get("errorMessage") as? String
        // The stream's ERROR state is reported by onParsedMessage directly - not re-wrapped by
        // streamConversation's own generic-exception catch on top of that.
        assertEquals("Conversation error: oops-raw-server-detail", errorMessage)
    }

    @Test
    fun `exception from chat flow shows generic copy`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        every { chatClient.chat("Hi") } returns flow {
            throw RuntimeException("HTTP error: -1 null")
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hi"))
        advanceUntilIdle()

        val last = vm.messages.value.last()
        // The raw exception text is not shown.
        assertEquals("Sorry, I encountered an error. Please try again.", last.text)
        assertTrue(!last.text.contains("HTTP error"))
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
    fun `productActionClick with valid URL opens URL in overlay`() = runTest {
        val vm = ConciergeChatViewModel(app)
        
        val button = ProductActionButton(
            id = "test_button",
            text = "Buy Now",
            url = "https://example.com/product"
        )
        
        vm.processEvent(MessageInteractionEvent.ProductActionClick(button))
        
        // Wait for all coroutines to complete
        advanceUntilIdle()
        
        // Verify that URL is set for in-app overlay
        assertEquals("https://example.com/product", vm.webviewOverlay.value)
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
    fun `productImageClick with valid productPageURL opens URL in overlay`() = runTest {
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
        
        // Verify that URL is set for in-app overlay
        assertEquals("https://example.com/product-page", vm.webviewOverlay.value)
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

    @Test
    fun `webviewOverlay is null initially`() = runTest {
        val vm = ConciergeChatViewModel(app)
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `dismissWebviewOverlay clears webviewOverlay`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.openWebviewOverlay("https://example.com/page")
        advanceUntilIdle()
        assertEquals("https://example.com/page", vm.webviewOverlay.value)

        vm.dismissWebviewOverlay()
        advanceUntilIdle()
        assertNull(vm.webviewOverlay.value)
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
        fun emitAudioLevel(level: Float) { listener?.onAudioLevelChanged(level) }
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
        assertTrue(vm.welcomeConfig.value.showWelcomeCard)
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
    fun `promptSuggestionClick auto-sends message`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("What can you do?"))
        runCurrent()

        // Prompt suggestion should auto-send, so input should be empty and message added
        assertEquals(UserInputState.Empty, vm.inputState.value)
        val messages = vm.messages.value
        assertTrue(messages.isNotEmpty())
        val userMessage = messages.first()
        assertTrue(userMessage.isFromUser)
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

    // ========== Ordered Elements Tests ==========

    @Test
    fun `orderedElements with CTA appends standalone CtaButton message`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()

        every { chatClient.chat("Go") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Tap below",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "Shop All", url = "https://example.com/shop"))
                )
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Go"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + CTA = 3
        assertEquals(3, messages.size)
        assertTrue(messages[1].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Text)
        assertTrue(messages[2].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton)
        val ctaContent = messages[2].content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton
        assertEquals("Shop All", ctaContent.button.label)
    }

    @Test
    fun `orderedElements with cards appends separate Mixed card message`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val card = MultimodalElement(id = "card-1")

        every { chatClient.chat("Products") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Here they are",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(ParsedMultimodalItem.Card(card))
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Products"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + carousel = 3
        assertEquals(3, messages.size)
        assertTrue(messages[1].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Text)
        val mixed = messages[2].content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Mixed
        assertEquals(1, mixed.multimodalElements?.size)
    }

    @Test
    fun `orderedElements interleaves CTA before cards preserving array order`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val card = MultimodalElement(id = "card-1")

        every { chatClient.chat("Mix") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Here you go",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "Shop", url = "https://example.com")),
                    ParsedMultimodalItem.Card(card)
                )
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Mix"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + CTA + carousel = 4
        assertEquals(4, messages.size)
        assertTrue(messages[2].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton)
        assertTrue(messages[3].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Mixed)
    }

    @Test
    fun `multiple CTAs each produce a separate CtaButton message`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()

        every { chatClient.chat("Options") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Choose one",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "A", url = "https://example.com/a")),
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "B", url = "https://example.com/b"))
                )
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Options"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + CTA1 + CTA2 = 4
        assertEquals(4, messages.size)
        val cta1 = messages[2].content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton
        val cta2 = messages[3].content as com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton
        assertEquals("A", cta1.button.label)
        assertEquals("B", cta2.button.label)
    }

    @Test
    fun `empty orderedElements falls back to legacy Mixed behavior`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()
        val elements = listOf(MultimodalElement(id = "card-1"))

        every { chatClient.chat("Hello") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Here are products",
                state = ConversationState.COMPLETED,
                multimodalElements = elements,
                orderedElements = emptyList()
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + Mixed = 2 (legacy path)
        assertEquals(2, messages.size)
        assertTrue(messages[1].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.Mixed)
    }

    @Test
    fun `text message has no interactionId when orderedElements contains a CTA`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()

        every { chatClient.chat("Connect") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "I'll connect you with an agent.",
                state = ConversationState.COMPLETED,
                interactionId = "interaction-123",
                orderedElements = listOf(
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "Chat now", url = "https://example.com/chat"))
                )
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Connect"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + CTA = 3
        assertEquals(3, messages.size)
        // text message must NOT have interactionId so feedback controls are suppressed
        assertNull(messages[1].interactionId)
        assertTrue(messages[2].content is com.adobe.marketing.mobile.concierge.ui.state.MessageContent.CtaButton)
    }

    @Test
    fun `text message retains interactionId when orderedElements contains only cards`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()

        every { chatClient.chat("Products") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "Here are some products.",
                state = ConversationState.COMPLETED,
                interactionId = "interaction-456",
                orderedElements = listOf(ParsedMultimodalItem.Card(MultimodalElement(id = "card-1")))
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Products"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + text + carousel = 3; text message retains interactionId for feedback
        assertEquals(3, messages.size)
        assertEquals("interaction-456", messages[1].interactionId)
    }

    @Test
    fun `orderedElements with empty text removes placeholder and shows only CTA`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>()

        every { chatClient.chat("Chat") } returns flow {
            emit(ParsedConversationMessage(
                messageContent = "",
                state = ConversationState.COMPLETED,
                orderedElements = listOf(
                    ParsedMultimodalItem.Cta(NetworkCtaButton(label = "Chat now", url = "https://example.com/chat"))
                )
            ))
        }

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)
        vm.processEvent(ChatEvent.SendMessage("Chat"))
        advanceUntilIdle()

        val messages = vm.messages.value
        // user + CTA only — no empty text bubble
        assertEquals(2, messages.size)
        assertTrue(messages[1].content is MessageContent.CtaButton)
        val cta = messages[1].content as MessageContent.CtaButton
        assertEquals("Chat now", cta.button.label)
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
        
        assertEquals("Custom Welcome Header", vm.welcomeConfig.value.welcomeHeader)
        assertEquals("Custom Subheader", vm.welcomeConfig.value.subHeader)
    }

    @Test
    fun `updateWelcomeConfigFromTheme with null does not change config`() = runTest {
        val fakeSpeech = FakeSpeechCapturing()
        val chatClient = mockk<ConciergeConversationServiceClient>(relaxed = true)

        val vm = ConciergeChatViewModel(app, fakeSpeech, chatClient)

        val originalConfig = vm.welcomeConfig.value

        vm.updateWelcomeConfigFromTheme(null)

        // Config should remain unchanged
        assertEquals(originalConfig, vm.welcomeConfig.value)
    }

    // ========== handleLinkClick Tests ==========

    @Test
    fun `handleLinkClick with blank URL does nothing`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("   ", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
        verify(exactly = 0) { tryOpenAsAppLink(any(), any()) }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick host callback returning true prevents any further handling`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("tel:+15555550100", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, handleLink = { true })

        verify(exactly = 0) { tryOpenAsAppLink(any(), any()) }
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with tel scheme calls tryOpenWithSystemHandler and does not open overlay`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("tel:+15555550100", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify { tryOpenWithSystemHandler(app, "tel:+15555550100") }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with geo scheme calls tryOpenWithSystemHandler`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("geo:0,0?q=1+Apple+Park+Way", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify { tryOpenWithSystemHandler(app, "geo:0,0?q=1+Apple+Park+Way") }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with percent-encoded store-locator geo URL calls tryOpenWithSystemHandler`() = runTest {
        val geoUrl = "geo:0,0?q=The%20Mall%20At%20Robinson%2C%20Pittsburgh%2C%20PA%2015205-4834"
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick(geoUrl, ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify { tryOpenWithSystemHandler(app, geoUrl) }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with mailto scheme calls tryOpenWithSystemHandler`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("mailto:user@example.com", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify { tryOpenWithSystemHandler(app, "mailto:user@example.com") }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with https URL opens overlay when not an App Link`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("https://example.com", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
        assertEquals("https://example.com", vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with https URL handled as App Link does not open overlay`() = runTest {
        every { tryOpenAsAppLink(any(), any()) } returns true

        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("https://example.com", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick with blocked scheme is silently dropped`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("javascript:alert(1)", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, null)

        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
        assertNull(vm.webviewOverlay.value)
    }

    @Test
    fun `handleLinkClick host callback returning false falls through to normal routing`() = runTest {
        val vm = ConciergeChatViewModel(app)
        vm.handleLinkClick("tel:+15555550100", ConciergeConstants.TrackingEvent.LinkClickOrigin.INLINE, handleLink = { false })

        verify { tryOpenWithSystemHandler(app, "tel:+15555550100") }
        assertNull(vm.webviewOverlay.value)
    }
}
