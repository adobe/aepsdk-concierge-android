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
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.components.messages.messageKey
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
import com.adobe.marketing.mobile.concierge.ui.state.MicEvent
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTextStrings
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.voice.FakeVoiceTransport
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionFailure
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionOwnership
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionState
import com.adobe.marketing.mobile.concierge.voice.UiPayloadData
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole
import com.adobe.marketing.mobile.concierge.voice.VoiceTurnFailure
import com.adobe.marketing.mobile.concierge.voice.VoiceTurnResult
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    private lateinit var factory: FakeVoiceSessionFactory
    private lateinit var transport: FakeVoiceTransport

    /** Stands in for the transport the app registers with `Concierge.setVoiceTransport`. */
    private lateinit var registeredTransport: MutableStateFlow<VoiceTransport?>
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
        factory = FakeVoiceSessionFactory()
        transport = FakeVoiceTransport()
        registeredTransport = MutableStateFlow(transport)
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
        appLifecycle: Lifecycle? = null,
        // Unconfined delivers session callbacks inline, as Dispatchers.Main.immediate does on the
        // main thread, so synchronous callbacks during start are exercised as in production.
        voiceCallbackDispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
        voiceOwnership: VoiceSessionOwnership = VoiceSessionOwnership()
    ): ConciergeChatViewModel {
        val vm = ConciergeChatViewModel(
            app, speech, DefaultImageProvider(), service,
            stateRepository = repository,
            sessionDispatcher = testDispatcher,
            appLifecycle = appLifecycle,
            dispatch = { dispatched.add(it) },
            voiceTransport = registeredTransport,
            voiceSessionFactory = factory::create,
            voiceCallbackDispatcher = voiceCallbackDispatcher,
            voiceOwnership = voiceOwnership
        )
        viewModels.add(ViewModelStore().apply { put("chat-${viewModels.size}", vm) })
        return vm
    }

    /** Starts voice and delivers credentials, so a live session exists. */
    private fun TestScope.startConnectedSession(vm: ConciergeChatViewModel): FakeVoiceSession {
        vm.startVoiceSession()
        runCurrent()
        service.emitBootstrap()
        runCurrent()
        return requireNotNull(factory.lastSession)
    }

    private fun errorEvents() = dispatched.filter {
        it.name == ConciergeConstants.TrackingEvent.Name.ERROR_OCCURRED
    }

    private fun trackedErrorMessage(): String? =
        errorEvents().single().eventData?.get(ConciergeConstants.TrackingEvent.EventData.Key.ERROR_MESSAGE) as? String

    private fun ConciergeChatViewModel.texts(): List<Pair<Boolean, String>> =
        messages.value.map { it.isFromUser to it.text }

    private fun completed(text: String, payload: UiPayloadData? = null) =
        VoiceTurnResult(VoiceTurnResult.Outcome.Completed(text), payload)

    private fun failed(failure: VoiceTurnFailure, payload: UiPayloadData? = null) =
        VoiceTurnResult(VoiceTurnResult.Outcome.Failed(failure), payload)

    private fun payload(
        suggestions: List<String> = emptyList(),
        sources: List<Citation> = emptyList(),
        elements: List<ParsedMultimodalItem> = emptyList()
    ) = UiPayloadData(turnId = null, sources = sources, promptSuggestions = suggestions, orderedElements = elements)

    private fun trackedEventNames() = dispatched.map { it.name }

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
    fun `voice is unavailable without a transport`() = runTest(testDispatcher) {
        registeredTransport.value = null
        val vm = makeViewModel()
        runCurrent()

        assertFalse(vm.isVoiceAvailable.value)
        assertFalse(vm.voiceEnabled.value)

        vm.startVoiceSession()
        runCurrent()
        vm.assertIdle()
        assertTrue(service.bootstrapSessionIds.isEmpty())
    }

    @Test
    fun `voice is available and enabled with a transport`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        runCurrent()

        assertTrue(vm.isVoiceAvailable.value)
        assertTrue(vm.voiceEnabled.value)
        assertTrue(vm.state.value is ChatScreenState.Idle)
    }

    @Test
    fun `setting and clearing the transport toggles voice in an open chat`() = runTest(testDispatcher) {
        registeredTransport.value = null
        val vm = makeViewModel()
        runCurrent()

        registeredTransport.value = transport
        runCurrent()
        assertTrue(vm.isVoiceAvailable.value)
        assertTrue(vm.voiceEnabled.value)

        registeredTransport.value = null
        runCurrent()
        assertFalse(vm.isVoiceAvailable.value)
        assertFalse(vm.voiceEnabled.value)
    }

    @Test
    fun `clearing the transport mid-conversation keeps it stoppable`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        session.report(VoiceSessionState.Listening)

        registeredTransport.value = null
        runCurrent()
        assertTrue(vm.isVoiceAvailable.value)
        assertTrue(vm.voiceEnabled.value)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)

        vm.toggleVoiceSession()
        runCurrent()
        vm.assertIdle()
        assertEquals(1, session.stopCallCount)
        assertFalse(vm.isVoiceAvailable.value)
    }

    @Test
    fun `a new transport applies to the next conversation`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val first = startConnectedSession(vm)
        val replacement = FakeVoiceTransport()

        registeredTransport.value = replacement
        first.report(VoiceSessionState.Listening)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        vm.stopVoiceSession()
        val second = startConnectedSession(vm)

        assertTrue(first.transport === transport)
        assertTrue(second.transport === replacement)
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
        assertTrue(factory.connections.isEmpty())
    }

    @Test
    fun `bootstrap sends the held xdm context for the resolved session`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "gold")))

        vm.startVoiceSession()
        runCurrent()

        assertEquals(listOf<String?>("session-1"), service.bootstrapSessionIds)
        assertEquals(listOf(mapOf<String, Any>("loyalty" to mapOf("tier" to "gold"))), service.bootstrapXdmFields)
    }

    @Test
    fun `bootstrap starts a session over the transport with the url and token`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        service.emitBootstrap(url = "wss://lk.example.com", token = "jwt-123")
        runCurrent()

        assertEquals(listOf("wss://lk.example.com" to "jwt-123"), factory.connections)
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
    fun `reconnecting keeps the conversation and shows the connecting indicator until listening`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        session.report(VoiceSessionState.Listening)

        session.report(VoiceSessionState.Reconnecting)
        assertTrue(vm.isVoiceConnecting.value)
        assertTrue(vm.isVoiceSessionActive.value)

        session.report(VoiceSessionState.Listening)
        assertFalse(vm.isVoiceConnecting.value)
        assertTrue(vm.isVoiceSessionActive.value)
        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertTrue("no error bubble", vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
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
        assertEquals(VoiceSessionFailure.BootstrapFailed.message, trackedErrorMessage())
        assertTrue(factory.connections.isEmpty())
    }

    @Test
    fun `bootstrap error fails back to idle with an error bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()

        service.failBootstrap(IOException("HTTP 503 from https://edge.example.com/conversations?token=secret"))
        runCurrent()

        vm.assertIdle()
        vm.assertErrorBubble()
        assertEquals(VoiceSessionFailure.BootstrapFailed.message, trackedErrorMessage())
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
    fun `a voice bubble keeps its list key while its text changes`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Yes", VoiceTranscriptRole.USER)
        val key = messageKey(vm.messages.value.single())
        session.transcript("Yes please", VoiceTranscriptRole.USER, isFinal = true)
        assertEquals(key, messageKey(vm.messages.value.single()))

        session.transcript("Yes please", VoiceTranscriptRole.USER, isFinal = true)
        assertEquals(2, vm.messages.value.map(::messageKey).distinct().size)
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

    @Test
    fun `an empty final transcript removes the open bubble and the next bubble becomes the anchor`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Hi there", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        session.transcript("Show me", VoiceTranscriptRole.USER)
        session.transcript("Let me", VoiceTranscriptRole.ASSISTANT)
        assertEquals(1, vm.voiceScrollAnchorIndex.value)

        session.transcript("", VoiceTranscriptRole.USER, isFinal = true)

        assertEquals(listOf(false to "Hi there", false to "Let me"), vm.texts())
        assertNull(vm.voiceScrollAnchorIndex.value)
        session.transcript("Let me check.", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        assertEquals(listOf(false to "Hi there", false to "Let me check."), vm.texts())

        session.transcript("Here", VoiceTranscriptRole.ASSISTANT)
        assertEquals(2, vm.voiceScrollAnchorIndex.value)
    }

    @Test
    fun `the scroll anchor is the latest user bubble and clears when the conversation ends`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Hi there", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        assertEquals(0, vm.voiceScrollAnchorIndex.value)

        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
        session.transcript("Here are", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        session.transcript("some shoes.", VoiceTranscriptRole.ASSISTANT, isFinal = true)
        assertEquals(1, vm.voiceScrollAnchorIndex.value)

        session.report(VoiceSessionState.Ended)
        assertNull(vm.voiceScrollAnchorIndex.value)
    }

    // MARK: - Turn end

    @Test
    fun `turn end renders the reply with sources, then the cards and CTAs with the suggestions, like a typed turn`() =
        runTest(testDispatcher) {
            val vm = makeViewModel()
            val session = startConnectedSession(vm)
            val source = Citation("Example", "https://example.com", citationNumber = 1)
            val card = MultimodalElement(id = "p1", title = "Runner")
            val cta = CtaButton("Shop", "https://example.com/shop")

            session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
            session.transcript("Here are", VoiceTranscriptRole.ASSISTANT)
            dispatched.clear()
            session.turnFinished(
                completed(
                    "Here are some shoes",
                    payload(
                        suggestions = listOf("Red ones", "Blue ones"),
                        sources = listOf(source),
                        elements = listOf(ParsedMultimodalItem.Card(card), ParsedMultimodalItem.Cta(cta))
                    )
                )
            )

            val messages = vm.messages.value
            assertEquals(4, messages.size)
            assertEquals("Here are some shoes", messages[1].text)
            assertEquals(listOf(source), messages[1].citations)
            assertTrue(messages[1].promptSuggestions.isEmpty())
            assertNull("a voice reply has no feedback controls", messages[1].interactionId)
            assertFalse(messages[1].feedbackEligible)
            assertEquals(MessageContent.Mixed(text = "", multimodalElements = listOf(card)), messages[2].content)
            assertEquals(listOf("Red ones", "Blue ones"), messages[2].promptSuggestions)
            assertEquals(MessageContent.CtaButton(cta), messages[3].content)
            assertEquals(listOf(ConciergeConstants.TrackingEvent.Name.CARDS_RENDERED), trackedEventNames())
            assertEquals("the reply shows below the user's words", 0, vm.voiceScrollAnchorIndex.value)
        }

    @Test
    fun `turn end without cards or CTAs puts the suggestions on the reply`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Hi", VoiceTranscriptRole.USER, isFinal = true)
        session.transcript("Hello", VoiceTranscriptRole.ASSISTANT)
        session.turnFinished(completed("Hello!", payload(suggestions = listOf("Shoes"))))
        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)

        assertEquals(listOf(true to "Hi", false to "Hello!", true to "Shoes"), vm.texts())
        assertEquals(listOf("Shoes"), vm.messages.value[1].promptSuggestions)
    }

    @Test
    fun `turn end with only CTAs drops the suggestions, like a typed turn`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        val cta = CtaButton("Shop", "https://example.com/shop")

        session.transcript("Hello", VoiceTranscriptRole.ASSISTANT)
        session.turnFinished(
            completed("Hello!", payload(suggestions = listOf("Shoes"), elements = listOf(ParsedMultimodalItem.Cta(cta))))
        )

        assertEquals(listOf(false to "Hello!", false to "Shop"), vm.texts())
        assertTrue(vm.messages.value.all { it.promptSuggestions.isEmpty() })
    }

    @Test
    fun `a failed turn replaces the reply with the theme's copy and still renders the payload`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.updateVoiceCopyFromTheme(
            ConciergeThemeConfig(text = ConciergeTextStrings(voiceTurnFailed = "Try again", voiceTurnMessageSendFailed = "Not sent"))
        )
        val session = startConnectedSession(vm)

        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
        session.transcript("Here are", VoiceTranscriptRole.ASSISTANT)
        session.turnFinished(failed(VoiceTurnFailure.TURN_FAILED, payload(suggestions = listOf("Red ones"))))
        session.transcript("Hello", VoiceTranscriptRole.USER, isFinal = true)
        session.turnFinished(failed(VoiceTurnFailure.MESSAGE_SEND_FAILED))

        assertEquals(listOf(true to "Shoes", false to "Try again", true to "Hello", false to "Not sent"), vm.texts())
        assertEquals(listOf("Red ones"), vm.messages.value[1].promptSuggestions)
        assertTrue("a failed turn keeps the conversation open", vm.isVoiceSessionActive.value)
        assertTrue(errorEvents().isEmpty())
    }

    @Test
    fun `a failed turn without a reply adds the default copy as a new bubble`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.updateVoiceCopyFromTheme(ConciergeThemeConfig(text = ConciergeTextStrings()))
        val session = startConnectedSession(vm)

        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
        session.turnFinished(failed(VoiceTurnFailure.TURN_FAILED))
        session.transcript("Hello", VoiceTranscriptRole.USER, isFinal = true)

        assertEquals(
            listOf(
                true to "Shoes",
                false to ConciergeChatViewModel.DEFAULT_VOICE_TURN_FAILED_MESSAGE,
                true to "Hello"
            ),
            vm.texts()
        )
    }

    @Test
    fun `turn end rewrites the reply after an earlier bubble was removed`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Sh", VoiceTranscriptRole.USER)
        session.transcript("Here", VoiceTranscriptRole.ASSISTANT)
        session.transcript("", VoiceTranscriptRole.USER, isFinal = true)
        session.turnFinished(completed("Here you go"))

        assertEquals(listOf(false to "Here you go"), vm.texts())
    }

    @Test
    fun `turn end with no reply bubble becomes the scroll anchor when there is none`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.turnFinished(completed("Welcome back"))

        assertEquals(listOf(false to "Welcome back"), vm.texts())
        assertEquals(0, vm.voiceScrollAnchorIndex.value)
    }

    // MARK: - Thinking indicator

    @Test
    fun `the thinking indicator follows the user's final and the reply fills it, like a typed turn`() =
        runTest(testDispatcher) {
            val vm = makeViewModel()
            val session = startConnectedSession(vm)

            session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
            session.awaitingReply(true)
            assertEquals(listOf(true to "Shoes", false to ""), vm.texts())
            assertTrue(vm.messages.value[1].isThinking)
            assertEquals("the user's words stay at the top", 0, vm.voiceScrollAnchorIndex.value)
            val thinking = vm.messages.value[1].localId

            session.transcript("Here", VoiceTranscriptRole.ASSISTANT)
            session.awaitingReply(false)
            assertEquals(listOf(true to "Shoes", false to "Here"), vm.texts())
            session.transcript("Here are shoes", VoiceTranscriptRole.ASSISTANT)
            session.turnFinished(completed("Here are shoes"))

            assertEquals(listOf(true to "Shoes", false to "Here are shoes"), vm.texts())
            assertEquals("the reply takes the indicator's place", thinking, vm.messages.value[1].localId)
            assertEquals(0, vm.voiceScrollAnchorIndex.value)
        }

    @Test
    fun `a failed turn without a reply shows the copy in place of the thinking indicator`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.updateVoiceCopyFromTheme(ConciergeThemeConfig(text = ConciergeTextStrings(voiceTurnFailed = "Try again")))
        val session = startConnectedSession(vm)

        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
        session.awaitingReply(true)
        val thinking = vm.messages.value[1].localId
        session.turnFinished(failed(VoiceTurnFailure.TURN_FAILED))
        session.awaitingReply(false)

        assertEquals(listOf(true to "Shoes", false to "Try again"), vm.texts())
        assertEquals(thinking, vm.messages.value[1].localId)
    }

    @Test
    fun `the thinking indicator is removed when a newer turn replaces it`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)

        session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
        session.awaitingReply(true)
        session.awaitingReply(false)
        session.transcript("Stop", VoiceTranscriptRole.USER)
        session.transcript("Stop", VoiceTranscriptRole.USER, isFinal = true)
        session.awaitingReply(true)
        session.transcript("OK", VoiceTranscriptRole.ASSISTANT)

        assertEquals(listOf(true to "Shoes", true to "Stop", false to "OK"), vm.texts())
        assertEquals(1, vm.voiceScrollAnchorIndex.value)
    }

    @Test
    fun `the thinking indicator is removed when voice stops or fails`() = runTest(testDispatcher) {
        listOf(false, true).forEach { fails ->
            val vm = makeViewModel()
            val session = startConnectedSession(vm)
            session.transcript("Shoes", VoiceTranscriptRole.USER, isFinal = true)
            session.awaitingReply(true)

            if (fails) session.report(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed)) else vm.stopVoiceSession()

            val expected = if (fails) listOf(true to "Shoes", false to ERROR_MESSAGE) else listOf(true to "Shoes")
            assertEquals("fails=$fails", expected, vm.texts())
        }
    }

    // MARK: - Prompt suggestions

    @Test
    fun `suggestion taps during voice go to the voice agent and are tracked with the bootstrap's context`() =
        runTest(testDispatcher) {
            listOf(
                MessageInteractionEvent.PromptSuggestionClick(" Red ones ") to
                    ConciergeConstants.TrackingEvent.Name.PROMPT_SUGGESTION_CLICKED,
                MessageInteractionEvent.WelcomePromptSuggestionClick(" Red ones ") to
                    ConciergeConstants.TrackingEvent.Name.WELCOME_PROMPT_SUGGESTION_CLICKED
            ).forEach { (tap, clickEvent) ->
                val vm = makeViewModel()
                repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "gold")))
                val session = startConnectedSession(vm)
                // A running voice conversation keeps the context it started with.
                repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "platinum")))
                dispatched.clear()

                vm.processEvent(tap)
                runCurrent()

                assertEquals(listOf(" Red ones "), session.sentTexts)
                assertTrue(service.chatMessages.isEmpty())
                assertTrue(vm.state.value is ChatScreenState.VoiceSession)
                assertTrue("the composer is left alone", vm.inputState.value is UserInputState.Empty)
                assertEquals(listOf(clickEvent, ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED), trackedEventNames())
                val query = dispatched.last().eventData
                assertEquals("Red ones", query?.get(ConciergeConstants.TrackingEvent.EventData.Key.QUERY))
                assertEquals(
                    mapOf("tier" to "gold"),
                    (query?.get(ConciergeConstants.TrackingEvent.EventData.Key.XDM_FIELDS) as? Map<*, *>)?.get("loyalty")
                )
                session.report(VoiceSessionState.Ended)
            }
        }

    @Test
    fun `suggestion taps the voice session refuses, or before it exists, are dropped`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        vm.startVoiceSession()
        runCurrent()
        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("Red ones"))
        service.emitBootstrap()
        runCurrent()
        val session = requireNotNull(factory.lastSession)
        session.acceptsText = false
        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("Blue ones"))
        runCurrent()

        assertEquals(listOf("Blue ones"), session.sentTexts)
        assertTrue(service.chatMessages.isEmpty())
        assertFalse(trackedEventNames().contains(ConciergeConstants.TrackingEvent.Name.QUERY_SUBMITTED))
        assertTrue(vm.isVoiceSessionActive.value)
    }

    @Test
    fun `suggestion taps after voice ends stream like a typed message`() = runTest(testDispatcher) {
        val vm = makeViewModel()
        val session = startConnectedSession(vm)
        session.report(VoiceSessionState.Ended)

        vm.processEvent(MessageInteractionEvent.PromptSuggestionClick("Red ones"))
        runCurrent()

        assertEquals(listOf("Red ones"), service.chatMessages)
        assertTrue(session.sentTexts.isEmpty())
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

        session.report(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed))

        vm.assertIdle()
        vm.assertErrorBubble()
        assertEquals(VoiceSessionFailure.TransportFailed.message, trackedErrorMessage())
    }

    // MARK: - One conversation app-wide

    @Test
    fun `another chat cannot start voice while a conversation runs, and its stop does not end it`() = runTest(testDispatcher) {
        val ownership = VoiceSessionOwnership()
        val owner = makeViewModel(voiceOwnership = ownership)
        val other = makeViewModel(voiceOwnership = ownership)
        runCurrent()
        assertTrue(other.voiceEnabled.value)

        owner.startVoiceSession()
        runCurrent()
        assertFalse("voice is claimed before the credentials arrive", other.voiceEnabled.value)
        service.emitBootstrap()
        runCurrent()
        val session = requireNotNull(factory.lastSession)
        session.report(VoiceSessionState.Listening)

        other.startVoiceSession()
        runCurrent()
        assertFalse(other.isVoiceSessionActive.value)
        assertEquals(1, service.bootstrapSessionIds.size)
        other.stopVoiceSession()
        assertEquals(0, session.stopCallCount)
        assertTrue(owner.isVoiceSessionActive.value)
        assertTrue("the owner can still stop", owner.voiceEnabled.value)
        assertFalse(other.voiceEnabled.value)
    }

    @Test
    fun `ending the conversation frees voice for another chat`() = runTest(testDispatcher) {
        val ownership = VoiceSessionOwnership()
        val owner = makeViewModel(voiceOwnership = ownership)
        val other = makeViewModel(voiceOwnership = ownership)
        startConnectedSession(owner)
        other.startVoiceSession()

        owner.stopVoiceSession()
        runCurrent()

        assertTrue(other.voiceEnabled.value)
        other.startVoiceSession()
        runCurrent()
        assertTrue("the ignored start released its request slot", other.isVoiceSessionActive.value)
        assertEquals(2, service.bootstrapSessionIds.size)
        assertFalse(owner.voiceEnabled.value)
    }

    @Test
    fun `clearing the owning view model ends its conversation and frees voice`() = runTest(testDispatcher) {
        val ownership = VoiceSessionOwnership()
        val owner = makeViewModel(voiceOwnership = ownership)
        val other = makeViewModel(voiceOwnership = ownership)
        val session = startConnectedSession(owner)
        runCurrent()
        assertFalse(other.voiceEnabled.value)

        viewModels.removeAt(0).clear()
        runCurrent()

        assertEquals(1, session.stopCallCount)
        assertTrue(other.voiceEnabled.value)
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
        assertTrue(factory.connections.isEmpty())
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

        first.deliverUnchecked(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed))
        first.transcript("stale", VoiceTranscriptRole.USER)

        assertTrue(vm.state.value is ChatScreenState.VoiceSession)
        assertTrue(vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
        assertEquals(0, second.stopCallCount)
    }

    @Test
    fun `synchronous callbacks during start are matched to the new session`() = runTest(testDispatcher) {
        factory.onStart = { session ->
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
    fun `synchronous failure during start returns to idle`() = runTest(testDispatcher) {
        factory.onStart = { session -> session.report(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed)) }
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
    fun `backgrounding while requesting credentials ends the voice session`() = runTest(testDispatcher) {
        val owner = TestAppLifecycleOwner()
        registeredTransport.value = FakeVoiceTransport(continuesInBackground = true)
        val vm = makeViewModel(appLifecycle = owner.lifecycle)
        vm.startVoiceSession()
        runCurrent()

        owner.moveToBackground()
        service.emitBootstrap()
        runCurrent()

        vm.assertIdle()
        assertTrue(factory.connections.isEmpty())
    }

    @Test
    fun `backgrounding ends a conversation that does not continue in the background`() = runTest(testDispatcher) {
        val owner = TestAppLifecycleOwner()
        val vm = makeViewModel(appLifecycle = owner.lifecycle)
        val session = startConnectedSession(vm)
        session.report(VoiceSessionState.Listening)

        owner.moveToBackground()

        vm.assertIdle()
        assertEquals(1, session.stopCallCount)
        assertTrue(errorEvents().isEmpty())
    }

    @Test
    fun `backgrounding keeps a connecting or connected conversation that continues in the background`() =
        runTest(testDispatcher) {
            listOf(false, true).forEach { connected ->
                val owner = TestAppLifecycleOwner()
                val vm = makeViewModel(appLifecycle = owner.lifecycle)
                val session = startConnectedSession(vm)
                session.continuesInBackground = true
                if (connected) session.report(VoiceSessionState.Listening)

                owner.moveToBackground()
                session.report(VoiceSessionState.Listening)
                session.transcript("While locked", VoiceTranscriptRole.USER, isFinal = true)
                owner.moveToForeground()

                assertEquals("connected=$connected", 0, session.stopCallCount)
                assertTrue(vm.state.value is ChatScreenState.VoiceSession)
                assertEquals(listOf(true to "While locked"), vm.texts())
                vm.stopVoiceSession()
            }
        }

    @Test
    fun `a conversation that ends in the background returns to idle without an error bubble`() = runTest(testDispatcher) {
        val owner = TestAppLifecycleOwner()
        val vm = makeViewModel(appLifecycle = owner.lifecycle)
        val session = startConnectedSession(vm)
        session.continuesInBackground = true
        session.report(VoiceSessionState.Listening)
        owner.moveToBackground()

        session.report(VoiceSessionState.Ended)

        vm.assertIdle()
        assertTrue(vm.messages.value.isEmpty())
        assertTrue(errorEvents().isEmpty())
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
