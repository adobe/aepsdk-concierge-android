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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffEvent
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.plugin.IVoicePlugin
import com.adobe.marketing.mobile.plugin.VoiceSessionState
import com.adobe.marketing.mobile.plugin.VoiceTranscriptRole
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConciergeChatViewModelVoiceSessionTest {

    @get:Rule
    val mainThreadRule = InstantMainThreadRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var service: FakeVoiceConversationService
    private lateinit var plugin: FakeVoicePlugin
    private lateinit var speech: FakeSpeechCapturing
    private lateinit var repository: ConciergeStateRepository
    private val dispatched = mutableListOf<Event>()
    private val viewModels = mutableListOf<ViewModelStore>()

    private val resetRequest = Event.Builder(
        "Reset", EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET
    ).build()

    @Before
    fun setUp() {
        ConciergeStateRepository.instance.clear()
        Dispatchers.setMain(testDispatcher)
        mockkObject(ConciergeSessionManager.instance)
        every { ConciergeSessionManager.instance.getSessionId() } returns "session-1"
        every { ConciergeSessionManager.instance.currentSessionIdOrNull() } returns "session-1"
        app = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
        grantMicrophone(true)
        mockkStatic(ServiceProvider::class)
        every { ServiceProvider.getInstance() } returns mockk(relaxed = true)
        service = FakeVoiceConversationService()
        plugin = FakeVoicePlugin()
        speech = FakeSpeechCapturing()
        repository = ConciergeStateRepository(
            ConciergeState(
                experienceCloudId = "ecid", configurationReady = true,
                conciergeServer = "https://example.com", conciergeConfigId = "config"
            )
        )
        dispatched.clear()
    }

    @After
    fun tearDown() {
        viewModels.forEach { it.clear() }
        viewModels.clear()
        ConciergeStateRepository.instance.clear()
        unmockkObject(ConciergeSessionManager.instance)
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        Dispatchers.resetMain()
    }

    private fun grantMicrophone(granted: Boolean) {
        every { ContextCompat.checkSelfPermission(any(), any()) } returns
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
    }

    private fun makeViewModel(
        voicePlugin: IVoicePlugin? = plugin,
        appLifecycle: Lifecycle? = null,
        // Unconfined delivers plugin callbacks inline, as Dispatchers.Main.immediate does on the
        // main thread, so synchronous callbacks during startSession are exercised as in production.
        voiceCallbackDispatcher: CoroutineDispatcher = Dispatchers.Unconfined
    ): ConciergeChatViewModel {
        val vm = ConciergeChatViewModel(
            app, speech, DefaultImageProvider(), service,
            stateRepository = repository,
            sessionDispatcher = testDispatcher,
            appLifecycle = appLifecycle,
            dispatch = { dispatched.add(it) },
            voicePlugin = voicePlugin,
            voiceCallbackDispatcher = voiceCallbackDispatcher
        )
        viewModels.add(ViewModelStore().apply { put("chat-${viewModels.size}", vm) })
        return vm
    }

    /** Starts voice and delivers credentials, so the plugin holds a live session. */
    private fun TestScope.startConnectedSession(vm: ConciergeChatViewModel): FakeVoiceSession {
        vm.startVoiceSession()
        runCurrent()
        service.emitBootstrap()
        runCurrent()
        return requireNotNull(plugin.lastSession)
    }

    private fun errorEvents() = dispatched.filter {
        it.name == ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED
    }

    private fun ConciergeChatViewModel.texts(): List<Pair<Boolean, String>> =
        messages.value.map { it.isFromUser to it.text }

    private fun ConciergeChatViewModel.assertIdle() {
        assertTrue("expected Idle but was ${state.value}", state.value is ChatScreenState.Idle)
        assertFalse(isVoiceSessionActive.value)
        assertFalse(isVoiceConnecting.value)
    }

    private fun ConciergeChatViewModel.assertErrorBubble() {
        val last = messages.value.last()
        assertFalse(last.isFromUser)
        assertEquals(ERROR_MESSAGE, last.text)
    }

    // MARK: - Availability

    @Test
    fun `voice is unavailable when no plugin is registered`() = runTest(testDispatcher) {
        val vm = makeViewModel(voicePlugin = null)
        runCurrent()

        assertFalse(vm.isVoiceAvailable)
        assertFalse(vm.voiceEnabled.value)

        vm.startVoiceSession()
        runCurrent()
        vm.assertIdle()
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    @Test
    fun `voice is available and enabled when a plugin is registered`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        runCurrent()

        assertTrue(vm.isVoiceAvailable)
        assertTrue(vm.voiceEnabled.value)
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `voice is disabled until configuration is ready`() = runTest(testDispatcher) {
        repository = ConciergeStateRepository(ConciergeState(experienceCloudId = "ecid"))
        val vm = makeViewModel()
        runCurrent()

        assertFalse(vm.voiceEnabled.value)
        vm.startVoiceSession()
        runCurrent()
        vm.assertIdle()
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    // MARK: - Start

    @Test
    fun `start enters the voice session and requests credentials`() = runTest(testDispatcher) {
        val vm = makeViewModel()

        vm.startVoiceSession()

        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertTrue(vm.isVoiceSessionActive.value)
        assertTrue(vm.isVoiceConnecting.value)
        runCurrent()
        assertTrue(vm.voiceEnabled.value)
        assertEquals(listOf<String?>("session-1"), service.bootstrapSessionIds)
        assertTrue(plugin.connections.isEmpty())
    }

    @Test
    fun `bootstrap starts a plugin session with the url and token`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        service.emitBootstrap(url = "wss://lk.example.com", token = "jwt-123")
        runCurrent()

        assertEquals(listOf(mapOf("url" to "wss://lk.example.com", "token" to "jwt-123")), plugin.connections)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertTrue(vm.isVoiceConnecting.value)
    }

    @Test
    fun `listening clears the connecting indicator`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.report(VoiceSessionState.Listening)

        assertFalse(vm.isVoiceConnecting.value)
        assertTrue(vm.isVoiceSessionActive.value)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
    }

    @Test
    fun `callbacks are delivered on the voice callback dispatcher`() = runTest(testDispatcher) {
        val vm = makeViewModel(voiceCallbackDispatcher = testDispatcher)
        val session = startConnectedSession(vm)

        session.report(VoiceSessionState.Listening)
        assertTrue(vm.isVoiceConnecting.value)

        runCurrent()
        assertFalse(vm.isVoiceConnecting.value)
    }

    // MARK: - Bootstrap failures

    @Test
    fun `bootstrap closing without credentials fails back to idle with an error bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        service.completeBootstrap()
        runCurrent()

        vm.assertIdle()
        vm.assertErrorBubble()
        assertEquals(1, errorEvents().size)
        assertTrue(plugin.connections.isEmpty())
    }

    @Test
    fun `bootstrap error fails back to idle with an error bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        service.failBootstrap(IOException("HTTP 503"))
        runCurrent()

        vm.assertIdle()
        vm.assertErrorBubble()
        val errorMessage = errorEvents().single().eventData
            ?.get(ConciergeConstants.TrackingEvent.EventData.Key.ERROR_MESSAGE) as String
        assertTrue(errorMessage, errorMessage.contains("HTTP 503"))
    }

    @Test
    fun `bootstrap closing after the session started keeps the session running`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        service.completeBootstrap()
        runCurrent()

        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertEquals(0, session.stopCallCount)
        assertTrue(errorEvents().isEmpty())
        assertTrue(vm.messages.value.isEmpty())
    }

    // MARK: - Transcripts

    @Test
    fun `transcript updates each role's bubble in place until it is final`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Hel", VoiceTranscriptRole.USER)
        session.transcript("Hello there", VoiceTranscriptRole.USER)
        assertEquals(listOf(true to "Hello there"), vm.texts())

        session.transcript("Hello there!", VoiceTranscriptRole.USER, isFinal = true)
        session.transcript("Hi", VoiceTranscriptRole.ASSISTANT)
        session.transcript("Hi, how can I help?", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        session.transcript("Next", VoiceTranscriptRole.USER)

        assertEquals(
            listOf(true to "Hello there!", false to "Hi, how can I help?", true to "Next"),
            vm.texts()
        )
        assertTrue(vm.messages.value[1].sseComplete)
        assertNull(vm.messages.value[1].interactionId)
    }

    @Test
    fun `interleaved roles keep separate bubbles`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("What's", VoiceTranscriptRole.USER)
        session.transcript("Let me", VoiceTranscriptRole.ASSISTANT)
        session.transcript("What's new?", VoiceTranscriptRole.USER, isFinal = true)
        session.transcript("Let me check.", VoiceTranscriptRole.ASSISTANT)

        assertEquals(listOf(true to "What's new?", false to "Let me check."), vm.texts())
    }

    @Test
    fun `empty transcript text adds no bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("", VoiceTranscriptRole.USER)
        session.transcript("", VoiceTranscriptRole.ASSISTANT, isFinal = true)

        assertTrue(vm.messages.value.isEmpty())
    }

    // MARK: - Session end

    @Test
    fun `session ending on its own returns to idle without an error bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        session.report(VoiceSessionState.Listening)
        session.transcript("Hello", VoiceTranscriptRole.USER, isFinal = true)

        session.report(VoiceSessionState.Ended)

        vm.assertIdle()
        assertEquals(listOf(true to "Hello"), vm.texts())
        assertTrue(errorEvents().isEmpty())
        assertTrue(vm.voiceEnabled.value)
    }

    @Test
    fun `session failure returns to idle with an error bubble and tracks it`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.report(VoiceSessionState.Failed("room disconnected"))

        vm.assertIdle()
        vm.assertErrorBubble()
        assertEquals(
            "room disconnected",
            errorEvents().single().eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ERROR_MESSAGE)
        )
    }

    // MARK: - Stop

    @Test
    fun `stop stops the session and returns to idle`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        vm.stopVoiceSession()

        vm.assertIdle()
        assertEquals(1, session.stopCallCount)
        assertTrue(vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
    }

    @Test
    fun `stop cancels a still open bootstrap`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        startConnectedSession(vm)
        assertEquals(1, service.activeBootstrapCollectors)

        vm.stopVoiceSession()
        runCurrent()

        assertEquals(0, service.activeBootstrapCollectors)
        assertEquals(1, service.cancelledBootstraps)
    }

    @Test
    fun `stop before credentials cancels the bootstrap and never starts a session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        vm.stopVoiceSession()
        service.emitBootstrap()
        runCurrent()

        vm.assertIdle()
        assertEquals(1, service.cancelledBootstraps)
        assertTrue(plugin.connections.isEmpty())
        assertTrue(vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
    }

    @Test
    fun `toggle starts then stops`() = runTest(testDispatcher) {
        val vm = makeViewModel()

        vm.toggleVoiceSession()
        assertTrue(vm.isVoiceSessionActive.value)

        vm.toggleVoiceSession()
        vm.assertIdle()
    }

    // MARK: - Stale and synchronous callbacks

    @Test
    fun `callbacks from an ended session are ignored`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val first = startConnectedSession(vm)
        first.report(VoiceSessionState.Ended)
        val second = startConnectedSession(vm)
        second.report(VoiceSessionState.Listening)

        first.deliverUnchecked(VoiceSessionState.Failed("late"))
        first.transcript("stale", VoiceTranscriptRole.USER)

        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertTrue(vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
        assertEquals(0, second.stopCallCount)
    }

    @Test
    fun `synchronous callbacks during startSession are matched to the new session`() = runTest(testDispatcher) {
        plugin.onStartSession = { session ->
            session.report(VoiceSessionState.Listening)
            session.transcript("Hi", VoiceTranscriptRole.ASSISTANT)
        }
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        assertFalse(vm.isVoiceConnecting.value)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertEquals(listOf(false to "Hi"), vm.texts())

        session.transcript("Hi there", VoiceTranscriptRole.ASSISTANT)
        assertEquals(listOf(false to "Hi there"), vm.texts())
    }

    @Test
    fun `synchronous failure during startSession returns to idle`() = runTest(testDispatcher) {
        plugin.onStartSession = { session -> session.report(VoiceSessionState.Failed("no audio")) }
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        vm.assertIdle()
        vm.assertErrorBubble()
        assertEquals(1, errorEvents().size)
        assertTrue(session.stopCallCount >= 1)
    }

    // MARK: - Exclusivity

    @Test
    fun `start is ignored while a turn is in flight`() = runTest(testDispatcher) {
        service.chatResponse = flow { awaitCancellation() }
        val vm = makeViewModel()
        vm.processEvent(ChatEvent.SendMessage("Hello"))
        runCurrent()
        assertTrue(vm.state.value is ChatScreenState.Processing)
        assertFalse(vm.voiceEnabled.value)

        vm.startVoiceSession()
        runCurrent()

        assertTrue(vm.state.value is ChatScreenState.Processing)
        assertFalse(vm.isVoiceSessionActive.value)
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    @Test
    fun `start is ignored while dictating`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.processEvent(MicEvent.StartRecording)
        runCurrent()
        assertTrue(vm.inputState.value is UserInputState.Recording)
        assertFalse(vm.voiceEnabled.value)

        vm.startVoiceSession()
        runCurrent()

        assertFalse(vm.isVoiceSessionActive.value)
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    @Test
    fun `typed turns and dictation are held off during voice`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        startConnectedSession(vm)

        vm.processEvent(ChatEvent.SendMessage("typed"))
        vm.processEvent(MicEvent.StartRecording)
        runCurrent()

        assertTrue(service.chatMessages.isEmpty())
        assertTrue(vm.messages.value.isEmpty())
        assertFalse(speech.startCalled)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
    }

    @Test
    fun `data handoff is rejected during voice`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.activateDataHandoffSession()
        try {
            startConnectedSession(vm)
            val results = mutableListOf<DataHandoffDeliveryResult>()

            vm.enqueueDataHandoff(ConciergeDataHandoffEvent("hint", emptyMap()), results::add)
            runCurrent()

            assertEquals(
                listOf(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS)),
                results
            )
            assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `chat returns to idle after voice ends so typed turns and handoffs resume`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.activateDataHandoffSession()
        try {
            startConnectedSession(vm).report(VoiceSessionState.Ended)

            vm.processEvent(ChatEvent.SendMessage("typed"))
            runCurrent()
            assertEquals(listOf("typed"), service.chatMessages)

            val results = mutableListOf<DataHandoffDeliveryResult>()
            vm.enqueueDataHandoff(ConciergeDataHandoffEvent("hint", emptyMap()), results::add)
            runCurrent()
            assertFalse(
                results.contains(DataHandoffDeliveryResult.Failed(ConciergeDataHandoffRejectReason.CHAT_IN_PROGRESS))
            )
        } finally {
            vm.deactivateDataHandoffSession()
        }
    }

    @Test
    fun `feedback dialog survives a voice session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.processEvent(FeedbackEvent.ThumbsUp("turn-1"))
        val feedback = Feedback("turn-1", FeedbackType.POSITIVE)

        vm.startVoiceSession()
        assertEquals(ChatScreenState.VoiceSession(feedback), vm.state.value)

        vm.stopVoiceSession()
        assertEquals(ChatScreenState.Idle(feedback), vm.state.value)
    }

    // MARK: - Lifecycle

    @Test
    fun `identity reset ends the voice session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        session.transcript("Hello", VoiceTranscriptRole.USER)

        repository.beginIdentityReset(resetRequest) { }
        runCurrent()

        vm.assertIdle()
        assertEquals(1, session.stopCallCount)
        assertTrue(vm.messages.value.isEmpty())
        assertFalse(vm.voiceEnabled.value)
        assertTrue(errorEvents().isEmpty())
    }

    @Test
    fun `chat reset ends the voice session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        vm.processEvent(ChatEvent.Reset)

        vm.assertIdle()
        assertEquals(1, session.stopCallCount)
    }

    @Test
    fun `clearing the view model ends the voice session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        viewModels.removeAt(0).clear()

        assertEquals(1, session.stopCallCount)
        assertFalse(vm.isVoiceSessionActive.value)
    }

    @Test
    fun `backgrounding the app ends the voice session`() = runTest(testDispatcher) {
        val owner = TestAppLifecycleOwner()
        val vm = makeViewModel(appLifecycle = owner.lifecycle)
        vm.startVoiceSession()
        runCurrent()

        owner.moveToBackground()
        service.emitBootstrap()
        runCurrent()

        vm.assertIdle()
        assertTrue(plugin.connections.isEmpty())
    }

    // MARK: - Microphone permission

    @Test
    fun `start is ignored without microphone permission`() = runTest(testDispatcher) {
        grantMicrophone(false)
        val vm = makeViewModel()

        vm.startVoiceSession()
        runCurrent()

        vm.assertIdle()
        assertFalse(vm.hasAudioPermission.value)
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    @Test
    fun `start proceeds once microphone permission is granted`() = runTest(testDispatcher) {
        grantMicrophone(false)
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()
        assertTrue(service.bootstrapSessionIds.isEmpty())

        grantMicrophone(true)
        vm.startVoiceSession()
        runCurrent()

        assertTrue(vm.isVoiceSessionActive.value)
        assertTrue(vm.hasAudioPermission.value)
        assertEquals(1, service.bootstrapSessionIds.size)
    }

    private class FakeSpeechCapturing : SpeechCapturing {
        private var listener: SpeechCaptureListener? = null
        var startCalled = false
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
            listener?.onSpeechEnded()
        }

        override fun release() = Unit
    }

    private companion object {
        const val ERROR_MESSAGE = "Sorry, I encountered an error. Please try again."
    }
}
