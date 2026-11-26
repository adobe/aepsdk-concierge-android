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

package com.adobe.marketing.mobile.concierge.voice

import android.os.Looper
import com.adobe.marketing.mobile.concierge.VoiceTransportState
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole.ASSISTANT
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole.USER
import com.adobe.marketing.mobile.concierge.voice.VoiceTurnResult.Outcome.Completed
import com.adobe.marketing.mobile.concierge.voice.VoiceTurnResult.Outcome.Failed
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VoiceSessionControllerTest {

    private val url = "wss://voice.example.com"
    private val token = "test-token"
    private var permissionChecks = 0
    private var permissionGranted = true

    private fun session(transport: FakeVoiceTransport, listener: RecordingVoiceListener) =
        VoiceSessionController(transport, listener) {
            permissionChecks++
            permissionGranted
        }

    private fun drainMain() = shadowOf(Looper.getMainLooper()).idle()

    private fun advanceMain(millis: Long) = shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis))

    private fun start(
        transport: FakeVoiceTransport,
        listener: RecordingVoiceListener = RecordingVoiceListener(),
        url: String = this.url,
        token: String = this.token
    ): VoiceSession {
        val session = session(transport, listener)
        session.start(url, token)
        drainMain()
        return session
    }

    private data class ListeningSession(
        val transport: FakeVoiceTransport,
        val listener: RecordingVoiceListener,
        val session: VoiceSession
    )

    private fun listeningSession(): ListeningSession {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        return ListeningSession(transport, listener, start(transport, listener))
    }

    private fun userSays(transport: FakeVoiceTransport, text: String, turnId: String, final: Boolean = true) {
        transport.receive(
            """{"type":"transcript_delta","data":{"role":"user","delta":"$text","final":$final,"turnId":"$turnId"}}"""
        )
    }

    private fun FakeVoiceTransport.sentPrompts(): List<JSONObject> = sent.map { JSONObject(it.first) }

    private fun textInputError(turnId: String?): String {
        val id = turnId?.let { ""","turnId":"$it"""" } ?: ""
        return """{"type":"text_input_error","data":{"reason":"voice_turn_in_progress"$id}}"""
    }

    private fun runOnBackgroundThread(action: () -> Unit) {
        val done = CountDownLatch(1)
        Thread {
            action()
            done.countDown()
        }.start()
        assertTrue(done.await(2, TimeUnit.SECONDS))
        drainMain()
    }

    // Start

    @Test
    fun start_isDeferredUntilTheMainLoopRuns() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = session(transport, listener)
        session.start(url, token)
        assertEquals(VoiceSessionState.Connecting, session.state)
        assertTrue(listener.states.isEmpty())
        assertTrue(transport.connections.isEmpty())
        drainMain()
        assertEquals(VoiceSessionState.Listening, session.state)
    }

    @Test
    fun connect_passesCredentialsAndReportsListeningOnMain() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        assertEquals(listOf(url to token), transport.connections)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening), listener.states)
        assertEquals(VoiceSessionState.Listening, session.state)
        assertTrue(listener.callbackLoopers.all { it == Looper.getMainLooper() })
        assertEquals(1, permissionChecks)
    }

    @Test
    fun invalidCredentials_failWithoutPermissionCheckOrConnect() {
        listOf(
            "" to token,
            url to "",
            url to "  ",
            "ftp://voice.example.com" to token,
            "ws://voice.example.com" to token,
            "http://voice.example.com" to token,
            "wss://" to token,
            "not a url" to token
        ).forEach { (invalidUrl, invalidToken) ->
            permissionChecks = 0
            val transport = FakeVoiceTransport()
            val listener = RecordingVoiceListener()
            val session = start(transport, listener, invalidUrl, invalidToken)
            val failed = VoiceSessionState.Failed(VoiceSessionFailure.InvalidCredentials)
            assertEquals("$invalidUrl / $invalidToken", listOf(VoiceSessionState.Connecting, failed), listener.states)
            assertEquals(failed, session.state)
            assertEquals(0, permissionChecks)
            assertTrue(transport.connections.isEmpty())
            assertNull(transport.listener)
        }
    }

    @Test
    fun connect_failsAndDisconnectsWhenTheTransportNeverReports() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val transportListener = requireNotNull(transport.listener)

        advanceMain(VoiceSessionController.CONNECT_TIMEOUT_MILLIS - 1)
        transport.report(VoiceTransportState.Reconnecting)
        assertEquals(VoiceSessionState.Connecting, session.state)
        advanceMain(1)

        assertEquals(
            VoiceSessionState.Failed(VoiceSessionFailure.ConnectTimeout(20)),
            session.state
        )
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listener)
        transportListener.onStateChanged(transport, VoiceTransportState.Connected)
        assertEquals("a late Connected is ignored", 2, listener.states.size)
    }

    @Test
    fun connect_connectedBeforeTheDeadline_keepsTheSession() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)

        advanceMain(VoiceSessionController.CONNECT_TIMEOUT_MILLIS - 1)
        transport.report(VoiceTransportState.Connected)
        advanceMain(VoiceSessionController.CONNECT_TIMEOUT_MILLIS * 2)

        assertEquals(VoiceSessionState.Listening, session.state)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening), listener.states)
        assertEquals(0, transport.disconnectCount)
    }

    @Test
    fun permissionDenied_failsWithoutTouchingTheTransport() {
        permissionGranted = false
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val failed = VoiceSessionState.Failed(VoiceSessionFailure.MicrophonePermissionDenied)
        assertEquals(failed, session.state)
        assertEquals(listOf(VoiceSessionState.Connecting, failed), listener.states)
        assertTrue(transport.connections.isEmpty())
        assertEquals(0, transport.disconnectCount)
        assertNull(transport.listener)
    }

    @Test
    fun throwingPermissionCheck_isReportedAsMissingPermission() {
        val transport = FakeVoiceTransport()
        val session = VoiceSessionController(transport, RecordingVoiceListener()) {
            throw SecurityException("permission boom")
        }
        session.start(url, token)
        drainMain()
        assertEquals(VoiceSessionState.Failed(VoiceSessionFailure.MicrophonePermissionDenied), session.state)
        assertTrue(transport.connections.isEmpty())
    }

    @Test
    fun throwingConnect_failsAndReleasesTheTransport() {
        val transport = FakeVoiceTransport(connectError = IllegalStateException("connect boom"))
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val failed = VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed)
        assertEquals(failed, session.state)
        assertEquals(listOf(VoiceSessionState.Connecting, failed), listener.states)
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listener)
    }

    @Test
    fun stopBeforeStartRuns_endsWithoutConnecting() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = session(transport, listener)
        session.start(url, token)
        session.stop()
        drainMain()
        assertEquals(listOf<VoiceSessionState>(VoiceSessionState.Ended), listener.states)
        assertTrue(transport.connections.isEmpty())
        assertEquals(0, permissionChecks)
    }

    @Test
    fun stopFromConnectingCallback_doesNotConnect() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener().apply {
            onState = { session, state -> if (state == VoiceSessionState.Connecting) session.stop() }
        }
        start(transport, listener)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Ended), listener.states)
        assertTrue(transport.connections.isEmpty())
    }

    // Transport lifecycle

    @Test
    fun transportFailure_reportsAFixedFailureAndReleasesTheTransport() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        transport.report(VoiceTransportState.Failed(IOException("network lost")))
        assertEquals(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed), session.state)
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listener)
    }

    @Test
    fun transportCallbacksFromBackgroundThread_areDeliveredOnMain() {
        listOf(false, true).forEach { failed ->
            val transport = FakeVoiceTransport()
            val listener = RecordingVoiceListener()
            val session = start(transport, listener)
            val state = if (failed) VoiceTransportState.Failed(IOException("lost")) else VoiceTransportState.Disconnected
            runOnBackgroundThread { transport.report(state) }
            val expected = if (failed) VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed) else VoiceSessionState.Ended
            assertEquals(expected, session.state)
            assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening, expected), listener.states)
            assertTrue(listener.callbackLoopers.all { it == Looper.getMainLooper() })
        }
    }

    @Test
    fun stop_releasesTheTransportBeforeDisconnecting() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        session.stop()
        assertEquals(VoiceSessionState.Ended, session.state)
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listenerAtDisconnect)
        assertNull(transport.listener)
        session.stop()
        assertEquals(1, transport.disconnectCount)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening, VoiceSessionState.Ended), listener.states)
    }

    @Test
    fun stopWhileConnecting_endsAndLateConnectedIsIgnored() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        assertEquals(VoiceSessionState.Connecting, session.state)
        val transportListener = requireNotNull(transport.listener)
        session.stop()
        transportListener.onStateChanged(transport, VoiceTransportState.Connected)
        assertEquals(VoiceSessionState.Ended, session.state)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Ended), listener.states)
    }

    @Test
    fun stopFromBackgroundThread_reportsEndedOnMain() {
        val listener = RecordingVoiceListener()
        val session = start(FakeVoiceTransport(), listener)
        runOnBackgroundThread { session.stop() }
        assertEquals(VoiceSessionState.Ended, session.state)
        assertEquals(VoiceSessionState.Ended, listener.states.last())
        assertTrue(listener.callbackLoopers.all { it == Looper.getMainLooper() })
    }

    @Test
    fun reconnecting_keepsTheSessionAndReturnsToListeningMutedAgainWhileInterrupted() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)

        transport.report(VoiceTransportState.Reconnecting)
        assertEquals(VoiceSessionState.Reconnecting, session.state)
        transport.interruptionBegan()
        transport.report(VoiceTransportState.Connected)
        advanceMain(VoiceSessionController.RECONNECT_TIMEOUT_MILLIS * 2)

        assertEquals(
            listOf(
                VoiceSessionState.Connecting,
                VoiceSessionState.Listening,
                VoiceSessionState.Reconnecting,
                VoiceSessionState.Listening
            ),
            listener.states
        )
        assertEquals(
            "a transport can reconnect with the microphone live, so the interruption mute is applied again",
            listOf(false, false),
            transport.microphoneChanges
        )
        transport.interruptionEnded()
        assertEquals(listOf(false, false, true), transport.microphoneChanges)
        assertEquals(0, transport.disconnectCount)
        assertTrue(transport.listener != null)
    }

    @Test
    fun reconnecting_withoutAnInterruption_leavesTheMicrophoneAlone() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)

        transport.report(VoiceTransportState.Reconnecting)
        transport.report(VoiceTransportState.Connected)

        assertEquals(VoiceSessionState.Listening, session.state)
        assertEquals(emptyList<Boolean>(), transport.microphoneChanges)
    }

    @Test
    fun reconnecting_failsAndDisconnectsWhenNotRestoredInTime() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val transportListener = requireNotNull(transport.listener)

        transport.report(VoiceTransportState.Reconnecting)
        advanceMain(VoiceSessionController.RECONNECT_TIMEOUT_MILLIS - 1)
        assertEquals(VoiceSessionState.Reconnecting, session.state)
        advanceMain(1)

        assertTrue(session.state is VoiceSessionState.Failed)
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listener)
        transportListener.onStateChanged(transport, VoiceTransportState.Connected)
        assertEquals("a late reconnect is ignored", 4, listener.states.size)
    }

    @Test
    fun reconnecting_beforeConnected_isIgnored() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)

        transport.report(VoiceTransportState.Reconnecting)
        assertEquals(VoiceSessionState.Connecting, session.state)
        transport.report(VoiceTransportState.Connected)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening), listener.states)
    }

    @Test
    fun callbacksAfterRelease_areDropped() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val transportListener = requireNotNull(transport.listener)
        session.stop()
        transportListener.onStateChanged(transport, VoiceTransportState.Failed(IOException("late")))
        transportListener.onDataReceived(transport, """{"event":"text_delta","delta":"late"}""".toByteArray(), null)
        transportListener.onAudioInterruptionBegan(transport)
        assertEquals(VoiceSessionState.Ended, session.state)
        assertTrue(listener.transcripts.isEmpty())
        assertTrue(transport.microphoneChanges.isEmpty())
    }

    @Test
    fun callbacksFromAnotherTransport_areDropped() {
        val transport = FakeVoiceTransport()
        val other = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val transportListener = requireNotNull(transport.listener)
        transportListener.onStateChanged(other, VoiceTransportState.Disconnected)
        transportListener.onDataReceived(other, """{"event":"text_delta","delta":"foreign"}""".toByteArray(), null)
        transportListener.onAudioInterruptionBegan(other)
        assertEquals(VoiceSessionState.Listening, session.state)
        assertTrue(listener.transcripts.isEmpty())
        assertTrue(transport.microphoneChanges.isEmpty())
    }

    @Test
    fun oneTransport_carriesConsecutiveSessions() {
        val transport = FakeVoiceTransport()
        val firstListener = RecordingVoiceListener()
        val first = start(transport, firstListener)
        first.stop()

        val secondListener = RecordingVoiceListener()
        val second = start(transport, secondListener)
        transport.receive("""{"event":"text_delta","delta":"two"}""")

        assertEquals(VoiceSessionState.Ended, first.state)
        assertEquals(VoiceSessionState.Listening, second.state)
        assertEquals(listOf(url to token, url to token), transport.connections)
        assertTrue(firstListener.transcripts.isEmpty())
        assertEquals(listOf(TranscriptEvent("two", ASSISTANT, false)), secondListener.transcripts)
    }

    @Test
    fun endedSession_doesNotClearANewerSessionsClaim() {
        val transport = FakeVoiceTransport()
        val first = start(transport)
        val firstTransportListener = requireNotNull(transport.listener)
        first.stop()
        val second = start(transport)
        val secondTransportListener = transport.listener

        firstTransportListener.onStateChanged(transport, VoiceTransportState.Disconnected)
        first.stop()

        assertSame(secondTransportListener, transport.listener)
        assertEquals(VoiceSessionState.Listening, second.state)
    }

    // Transcripts

    @Test
    fun userTranscript_usesCumulativeSnapshots() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.receive("""{"type":"transcript_delta","data":{"delta":" hello ","final":false}}""")
        transport.receive("""{"type":"transcript_delta","data":{"delta":" hello world ","final":true}}""")
        assertEquals(
            listOf(TranscriptEvent("hello", USER, false), TranscriptEvent("hello world", USER, true)),
            listener.transcripts
        )
    }

    @Test
    fun assistantTranscript_accumulatesAndTheTurnEndsWithTheAuthoritativeText() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.receive("""{"event":"text_delta","delta":" Hello"}""")
        transport.receive("""{"event":"text_delta","delta":" world "}""")
        transport.receive("""{"event":"turn_completed","fullText":" Corrected reply "}""")
        transport.receive("""{"event":"text_delta","delta":" Next turn"}""")
        transport.receive("""{"event":"turn_completed"}""")
        assertEquals(
            listOf(
                TranscriptEvent("Hello", ASSISTANT, false),
                TranscriptEvent("Hello world", ASSISTANT, false),
                TranscriptEvent("Next turn", ASSISTANT, false)
            ),
            listener.transcripts
        )
        assertEquals(
            "the turn ends with the authoritative text; an empty one fails the turn",
            listOf(Completed("Corrected reply"), Failed(VoiceTurnFailure.TURN_FAILED)),
            listener.outcomes
        )
    }

    // Turns

    @Test
    fun turnCompleted_carriesTheTurnsLatestUiPayload() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Show me shoes", "t1")
        transport.receive("""{"event":"text_delta","delta":"Here you go","turnId":"t1"}""")
        transport.receive("""{"event":"ui_payload","turnId":"t1","promptSuggestions":["a"]}""")
        transport.receive(
            """{"event":"ui_payload","turnId":"t1","promptSuggestions":["a","b"],"multimodalElements":{"elements":[{"id":"p1"}]}}"""
        )
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Here you go","state":"completed"}""")

        assertEquals(listOf(Completed("Here you go")), listener.outcomes)
        val payload = listener.results.single().uiPayload!!
        assertEquals("the latest snapshot wins", listOf("a", "b"), payload.promptSuggestions)
        assertEquals("p1", (payload.orderedElements.single() as ParsedMultimodalItem.Card).element.id)
    }

    @Test
    fun turnCompleted_withAnErrorOrNoText_failsTheTurnButKeepsThePayloadAndSession() {
        listOf("error" to "Partial", "completed" to "  ").forEach { (state, fullText) ->
            val (transport, listener, session) = listeningSession()
            userSays(transport, "Hi", "t1")
            transport.receive("""{"event":"text_delta","delta":"Partial","turnId":"t1"}""")
            transport.receive("""{"event":"ui_payload","turnId":"t1","promptSuggestions":["a"]}""")
            transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"$fullText","state":"$state"}""")

            assertEquals(state, listOf(Failed(VoiceTurnFailure.TURN_FAILED)), listener.outcomes)
            assertEquals(state, listOf("a"), listener.results.single().uiPayload?.promptSuggestions)
            assertEquals(state, VoiceSessionState.Listening, session.state)
        }
    }

    @Test
    fun turnCompleted_interrupted_countsAsCompleted() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Hello","state":"interrupted"}""")
        assertEquals(listOf(Completed("Hello")), listener.outcomes)
    }

    @Test
    fun turnCompleted_forAnotherTurn_isDropped() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        transport.receive("""{"event":"turn_completed","turnId":"t0","fullText":"Stale"}""")
        transport.receive("""{"event":"ui_payload","turnId":"t0","promptSuggestions":["stale"]}""")
        assertTrue(listener.results.isEmpty())
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Hello"}""")
        assertEquals(listOf(Completed("Hello")), listener.outcomes)
        assertNull(listener.results.single().uiPayload)
    }

    @Test
    fun errorAfterTheTurnCompleted_isDropped() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Hello"}""")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"","state":"error"}""")
        transport.receive("""{"event":"turn_completed","fullText":"","state":"error"}""")
        assertEquals(listOf(Completed("Hello")), listener.outcomes)
    }

    @Test
    fun newUserTurn_closesTheOpenReply_andDropsTheOldTurnsLateMessages() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        transport.receive("""{"event":"text_delta","delta":"Partial","turnId":"t1"}""")
        transport.receive("""{"event":"ui_payload","turnId":"t1","promptSuggestions":["old"]}""")
        userSays(transport, "Wait", "t2", final = false)
        transport.receive("""{"event":"text_delta","delta":" more","turnId":"t1"}""")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Partial more"}""")
        userSays(transport, "Wait", "t2")
        transport.receive("""{"event":"text_delta","delta":"Sure","turnId":"t2"}""")
        transport.receive("""{"event":"turn_completed","turnId":"t2","fullText":"Sure"}""")

        assertEquals(
            listOf(
                TranscriptEvent("Hi", USER, true),
                TranscriptEvent("Partial", ASSISTANT, false),
                TranscriptEvent("Partial", ASSISTANT, true),
                TranscriptEvent("Wait", USER, false),
                TranscriptEvent("Wait", USER, true),
                TranscriptEvent("Sure", ASSISTANT, false)
            ),
            listener.transcripts
        )
        assertEquals(listOf(Completed("Sure")), listener.outcomes)
        assertNull("the interrupted turn's payload is discarded", listener.results.single().uiPayload)
    }

    @Test
    fun assistantTypeBasedFinal_closesTurn() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.receive("""{"type":"transcript_delta","data":{"role":"assistant","delta":"first","final":true}}""")
        transport.receive("""{"event":"text_delta","delta":"second"}""")
        assertEquals(
            listOf(TranscriptEvent("first", ASSISTANT, true), TranscriptEvent("second", ASSISTANT, false)),
            listener.transcripts
        )
    }

    @Test
    fun emptyInterim_isSuppressedButEmptyFinalClosesTurn() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.receive("""{"type":"transcript_delta","data":{"delta":"  "}}""")
        transport.receive("""{"type":"transcript_delta","data":{"delta":"  ","final":true}}""")
        assertEquals(listOf(TranscriptEvent("", USER, true)), listener.transcripts)
    }

    // Awaiting the reply

    @Test
    fun awaitingReply_fromTheUsersFinal_untilAfterTheReplysFirstText() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Show me", "t1", final = false)
        assertTrue("the user is still speaking", listener.awaitingChanges.isEmpty())
        userSays(transport, "Show me shoes", "t1")
        userSays(transport, "Show me shoes", "t1")
        transport.receive("""{"event":"ui_payload","turnId":"t1","promptSuggestions":["a"]}""")
        transport.receive("""{"event":"text_delta","delta":"Here","turnId":"t1"}""")
        transport.receive("""{"event":"text_delta","delta":" you go","turnId":"t1"}""")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"Here you go"}""")

        assertEquals(listOf(AwaitingChange(true, 2), AwaitingChange(false, 4)), listener.awaitingChanges)
    }

    @Test
    fun awaitingReply_endsAfterTheTurnEnds_whenNoReplyStarted() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        transport.receive("""{"event":"turn_completed","turnId":"t1","fullText":"","state":"error"}""")

        assertEquals(
            listOf(AwaitingChange(true, 1), AwaitingChange(false, 1, afterResults = 1)),
            listener.awaitingChanges
        )
    }

    @Test
    fun awaitingReply_forASentText_fromTheUsersWords_untilAfterTheTurnFails() {
        val (transport, listener, session) = listeningSession()
        session.sendText("Red ones")
        val turnId = transport.sentPrompts().single().getString("turn_id")
        userSays(transport, "Red ones", turnId = "w1")
        assertEquals("the echo changes nothing", listOf(AwaitingChange(true, 1)), listener.awaitingChanges)

        transport.receive(textInputError(turnId))

        assertEquals(AwaitingChange(false, 1, afterResults = 1), listener.awaitingChanges.last())
    }

    @Test
    fun awaitingReply_forATextThatFailsToSend_endsAfterTheTurnFails() {
        val (transport, listener, session) = listeningSession()
        transport.sendError = IOException("offline")
        session.sendText("Red ones")
        drainMain()

        assertEquals(
            listOf(AwaitingChange(true, 1), AwaitingChange(false, 1, afterResults = 1)),
            listener.awaitingChanges
        )
    }

    @Test
    fun awaitingReply_endsBeforeANewerTurnsWords_andStartsAgainAtItsFinal() {
        val (transport, listener) = listeningSession()
        userSays(transport, "Hi", "t1")
        userSays(transport, "Wait", "t2", final = false)
        userSays(transport, "Wait", "t2")

        assertEquals(
            listOf(AwaitingChange(true, 1), AwaitingChange(false, 1), AwaitingChange(true, 3)),
            listener.awaitingChanges
        )
    }

    @Test
    fun awaitingReply_endsBeforeTheSessionEnds() {
        listOf("stop", "failure").forEach { end ->
            val (transport, listener, session) = listeningSession()
            userSays(transport, "Hi", "t1")
            if (end == "stop") session.stop() else transport.report(VoiceTransportState.Failed(IOException("lost")))
            drainMain()

            assertEquals(end, 3, listener.states.size)
            assertEquals(end, listOf(AwaitingChange(true, 1), AwaitingChange(false, 1)), listener.awaitingChanges)
        }
    }

    @Test
    fun awaitingReply_isNotReported_forAWithdrawnUtterance_orATurnTheAgentOpens() {
        val (transport, listener) = listeningSession()
        transport.receive("""{"event":"text_delta","delta":"Hi! How can I help?","turnId":"a1"}""")
        transport.receive("""{"event":"turn_completed","turnId":"a1","fullText":"Hi! How can I help?"}""")
        userSays(transport, "Um", "t1", final = false)
        userSays(transport, "", "t1")

        assertEquals(listOf(Completed("Hi! How can I help?")), listener.outcomes)
        assertTrue(listener.awaitingChanges.isEmpty())
    }

    @Test
    fun dataFromBackgroundThread_isDeliveredOnMain() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        runOnBackgroundThread { transport.receive("""{"event":"text_delta","delta":"hello"}""") }
        assertEquals(listOf(TranscriptEvent("hello", ASSISTANT, false)), listener.transcripts)
        assertTrue(listener.callbackLoopers.all { it == Looper.getMainLooper() })
    }

    @Test
    fun sessionEnded_byATimeout_endsNormally() {
        listOf("room_empty_timeout", "user_join_timeout", "user_inactivity_timeout").forEach { reason ->
            val transport = FakeVoiceTransport()
            val session = start(transport)
            transport.receive("""{"type":"session_notice","data":{"kind":"session_ended","reason":"$reason"}}""")
            assertEquals(reason, VoiceSessionState.Ended, session.state)
            assertEquals(reason, 1, transport.disconnectCount)
        }
    }

    @Test
    fun sessionEnded_byAnErrorShutdownOrUnknownReason_fails() {
        listOf(
            """"reason":"error",""" to EndReason.ERROR,
            """"reason":"worker_shutdown",""" to EndReason.WORKER_SHUTDOWN,
            """"reason":"brand_new_reason",""" to null,
            "" to null
        ).forEach { (reasonField, expected) ->
            val transport = FakeVoiceTransport()
            val session = start(transport)
            transport.receive("""{"type":"session_notice","data":{$reasonField"kind":"session_ended"}}""")
            assertEquals(
                reasonField,
                VoiceSessionState.Failed(VoiceSessionFailure.AgentEnded(expected)),
                session.state
            )
            assertEquals(reasonField, 1, transport.disconnectCount)
        }
    }

    // Text prompts

    @Test
    fun sendText_sendsATextPromptOnTheTextInputTopic_andShowsTheUsersWords() {
        val (transport, listener, session) = listeningSession()

        assertTrue(session.sendText("  Red ones "))

        assertEquals(listOf("bc-text-input"), transport.sent.map { it.second })
        val prompt = transport.sentPrompts().single()
        assertEquals("text_prompt", prompt.getString("type"))
        assertEquals("Red ones", prompt.getString("text"))
        assertTrue(prompt.getString("turn_id").startsWith("txt-"))
        assertEquals(listOf(TranscriptEvent("Red ones", USER, true)), listener.transcripts)
        drainMain()
        assertTrue(listener.results.isEmpty())
    }

    @Test
    fun sendText_dropsTheWorkersEcho_andFinishesTheTurnWithTheReply() {
        val (transport, listener, session) = listeningSession()
        session.sendText("Red ones")
        userSays(transport, "Red ones", turnId = "w1")
        transport.receive("""{"event":"text_delta","delta":"Sure","turnId":"w1"}""")
        transport.receive("""{"event":"ui_payload","turnId":"w1","promptSuggestions":["Blue ones"]}""")
        transport.receive("""{"event":"turn_completed","turnId":"w1","fullText":"Sure"}""")

        assertEquals(
            listOf(TranscriptEvent("Red ones", USER, true), TranscriptEvent("Sure", ASSISTANT, false)),
            listener.transcripts
        )
        assertEquals(listOf(Completed("Sure")), listener.outcomes)
        assertEquals(listOf("Blue ones"), listener.results.single().uiPayload?.promptSuggestions)
    }

    @Test
    fun sendText_isRefused_fromTheStartOfSpeechUntilTheReplyStarts() {
        val (transport, _, session) = listeningSession()
        assertFalse("blank text", session.sendText("  "))

        userSays(transport, "I want", turnId = "t1", final = false)
        assertFalse("the user is speaking", session.sendText("Red ones"))
        userSays(transport, "I want shoes", turnId = "t1")
        assertFalse("the turn waits for its reply", session.sendText("Red ones"))
        transport.receive("""{"event":"text_delta","delta":"Sure","turnId":"t1"}""")
        assertTrue("the reply started", session.sendText("Red ones"))
        assertFalse("the text turn waits for its reply", session.sendText("Blue ones"))
        assertEquals(1, transport.sent.size)
    }

    @Test
    fun sendText_isRefusedWhileConnectingOrReconnecting_andAfterTheEnd() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val session = session(transport, RecordingVoiceListener())
        session.start(url, token)
        assertFalse("before connect", session.sendText("Red ones"))
        drainMain()
        assertFalse("connecting", session.sendText("Red ones"))

        transport.report(VoiceTransportState.Connected)
        transport.report(VoiceTransportState.Reconnecting)
        assertFalse("reconnecting", session.sendText("Red ones"))
        transport.report(VoiceTransportState.Connected)
        assertTrue(session.sendText("Red ones"))
        session.stop()
        assertFalse("ended", session.sendText("Blue ones"))
        assertEquals(1, transport.sent.size)
    }

    @Test
    fun textInputError_failsTheTextTurn_andTextCanBeSentAgain() {
        val (transport, listener, session) = listeningSession()
        session.sendText("Red ones")
        val turnId = transport.sentPrompts().single().getString("turn_id")

        transport.receive(textInputError("txt-other"))
        assertTrue("an error for another prompt is dropped", listener.results.isEmpty())
        transport.receive(textInputError(turnId))

        assertEquals(listOf(Failed(VoiceTurnFailure.TURN_FAILED)), listener.outcomes)
        assertEquals(VoiceSessionState.Listening, session.state)
        assertTrue(session.sendText("Red ones"))
    }

    @Test
    fun userSpeechBeforeTheEcho_failsTheTextTurn_andTheLaterErrorIsDropped() {
        val (transport, listener, session) = listeningSession()
        session.sendText("Red ones")
        val turnId = transport.sentPrompts().single().getString("turn_id")

        // The worker publishes the speech transcript before it rejects the prompt.
        userSays(transport, "Actually", turnId = "t1", final = false)
        assertEquals(listOf(Failed(VoiceTurnFailure.TURN_FAILED)), listener.outcomes)
        assertEquals("Actually", listener.transcripts.last().text)

        transport.receive(textInputError(turnId))
        assertEquals(1, listener.outcomes.size)
    }

    @Test
    fun textInputError_withoutATextTurn_isDropped() {
        val (transport, listener, _) = listeningSession()
        userSays(transport, "Hi", turnId = "t1")
        transport.receive(textInputError(null))
        assertTrue(listener.results.isEmpty())
    }

    @Test
    fun sendFailure_failsTheTurnWithTheMessageSendFailedCopy_afterSendTextReturns() {
        listOf<(FakeVoiceTransport) -> Unit>(
            { it.sendError = IOException("offline") },
            { it.sendCompletesImmediately = false }
        ).forEach { configure ->
            val (transport, listener, session) = listeningSession()
            configure(transport)

            assertTrue(session.sendText("Red ones"))
            assertTrue(listener.results.isEmpty())
            if (!transport.sendCompletesImmediately) {
                runOnBackgroundThread { transport.completeSend(IOException("offline")) }
            }
            drainMain()

            assertEquals(listOf(Failed(VoiceTurnFailure.MESSAGE_SEND_FAILED)), listener.outcomes)
            assertEquals(VoiceSessionState.Listening, session.state)
            assertSame(Looper.getMainLooper(), listener.callbackLoopers.last())
        }
    }

    @Test
    fun sendFailure_afterTheTurnMovedOn_isDropped() {
        val (transport, listener, session) = listeningSession()
        transport.sendCompletesImmediately = false
        session.sendText("Red ones")
        transport.receive(textInputError(null))
        transport.completeSend(IOException("offline"))
        drainMain()

        assertEquals(listOf(Failed(VoiceTurnFailure.TURN_FAILED)), listener.outcomes)
    }

    @Test
    fun ignoredMessages_doNotEmitTranscriptOrChangeState() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        listOf(
            """{"event":"ui_payload","promptSuggestions":["one"]}""",
            """{"event":"turn_completed","fullText":"No turn in flight"}""",
            """{"event":"turn_started"}""",
            """{"type":"session_notice","data":{"kind":"user_silence_warning"}}""",
            """{"event":"unknown"}"""
        ).forEach(transport::receive)
        transport.listener!!.onDataReceived(transport, byteArrayOf(0, 1, 2), null)
        assertTrue(listener.transcripts.isEmpty())
        assertTrue(listener.results.isEmpty())
        assertEquals(VoiceSessionState.Listening, session.state)
    }

    // Background and maximum length

    @Test
    fun continuesInBackground_fromConnectUntilTheEnd_whenTheTransportOptsIn() {
        val transport = FakeVoiceTransport(connectsImmediately = false, continuesInBackground = true)
        val session = session(transport, RecordingVoiceListener())
        session.start(url, token)
        assertFalse("not before connect", session.continuesInBackground)

        drainMain()
        assertTrue("connecting", session.continuesInBackground)
        transport.continuesInBackground = false
        transport.report(VoiceTransportState.Connected)
        transport.report(VoiceTransportState.Reconnecting)
        assertTrue("read once, at connect", session.continuesInBackground)

        session.stop()
        assertFalse(session.continuesInBackground)
    }

    @Test
    fun continuesInBackground_isFalseWithoutTheOptInOrBeforeConnect() {
        assertFalse(start(FakeVoiceTransport()).continuesInBackground)

        permissionGranted = false
        val denied = start(FakeVoiceTransport(continuesInBackground = true))
        assertTrue(denied.state is VoiceSessionState.Failed)
        assertFalse(denied.continuesInBackground)
    }

    @Test
    fun maxLength_endsNormallyAndDisconnects_anHourAfterConnected() {
        listOf(false, true).forEach { optedIn ->
            val transport = FakeVoiceTransport(connectsImmediately = false, continuesInBackground = optedIn)
            val listener = RecordingVoiceListener()
            val session = start(transport, listener)
            advanceMain(5_000)
            transport.report(VoiceTransportState.Connected)

            advanceMain(VoiceSessionController.MAX_SESSION_MILLIS - 1)
            assertEquals("optedIn=$optedIn", VoiceSessionState.Listening, session.state)
            advanceMain(1)

            assertEquals(
                "optedIn=$optedIn",
                listOf(VoiceSessionState.Connecting, VoiceSessionState.Listening, VoiceSessionState.Ended),
                listener.states
            )
            assertEquals(1, transport.disconnectCount)
            assertNull(transport.listener)
        }
    }

    @Test
    fun maxLength_isNotRestartedByAReconnect() {
        val transport = FakeVoiceTransport()
        val session = start(transport)

        advanceMain(VoiceSessionController.MAX_SESSION_MILLIS / 2)
        transport.report(VoiceTransportState.Reconnecting)
        advanceMain(10_000)
        transport.report(VoiceTransportState.Connected)
        advanceMain(VoiceSessionController.MAX_SESSION_MILLIS / 2 - 10_000)

        assertEquals(VoiceSessionState.Ended, session.state)
        assertEquals(1, transport.disconnectCount)
    }

    @Test
    fun maxLength_startsAgainForANewConversation() {
        val transport = FakeVoiceTransport()
        val first = start(transport)
        advanceMain(VoiceSessionController.MAX_SESSION_MILLIS / 2)
        first.stop()

        val second = start(transport)
        advanceMain(VoiceSessionController.MAX_SESSION_MILLIS / 2)
        assertEquals(VoiceSessionState.Listening, second.state)
        advanceMain(VoiceSessionController.MAX_SESSION_MILLIS / 2)

        assertEquals(VoiceSessionState.Ended, second.state)
        assertEquals(2, transport.disconnectCount)
    }

    // Audio interruptions

    @Test
    fun audioInterruption_mutesThenRestoresTheMicrophone() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.interruptionBegan()
        // A repeated began mutes once, and one ended resumes.
        transport.interruptionBegan()
        transport.interruptionEnded()
        assertEquals(listOf(false, true), transport.microphoneChanges)
        assertEquals(VoiceSessionState.Listening, session.state)
    }

    @Test
    fun audioInterruption_failedMute_failsTheSession() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.microphoneError = IllegalStateException("mute failed")
        transport.interruptionBegan()
        assertEquals(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed), session.state)
        assertEquals("the microphone is released", 1, transport.disconnectCount)
    }

    @Test
    fun audioInterruption_failedUnmute_failsTheSession() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.interruptionBegan()
        transport.microphoneError = IllegalStateException("unmute failed")
        transport.interruptionEnded()
        assertEquals(listOf(false, true), transport.microphoneChanges)
        assertEquals(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed), session.state)
        assertEquals(1, transport.disconnectCount)
    }

    @Test
    fun audioInterruption_failedMuteOnConnected_failsTheSession() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val session = start(transport)
        transport.interruptionBegan()
        transport.microphoneError = IllegalStateException("mute failed")
        transport.report(VoiceTransportState.Connected)
        assertEquals(VoiceSessionState.Failed(VoiceSessionFailure.TransportFailed), session.state)
        assertEquals(1, transport.disconnectCount)
    }

    @Test
    fun audioInterruptionEnded_withoutBegan_isIgnored() {
        val transport = FakeVoiceTransport()
        start(transport)
        transport.interruptionEnded()
        assertTrue(transport.microphoneChanges.isEmpty())
    }

    @Test
    fun audioInterruption_whileConnecting_mutesOnConnectedAndResumesWhenItEnds() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        val session = start(transport)
        transport.interruptionBegan()
        assertTrue("the microphone is left alone until connected", transport.microphoneChanges.isEmpty())

        transport.report(VoiceTransportState.Connected)
        assertEquals(VoiceSessionState.Listening, session.state)
        assertEquals(listOf(false), transport.microphoneChanges)

        transport.interruptionEnded()
        assertEquals(listOf(false, true), transport.microphoneChanges)
    }

    @Test
    fun audioInterruption_endingWhileConnecting_leavesTheMicrophoneAlone() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        start(transport)
        transport.interruptionBegan()
        transport.interruptionEnded()
        transport.report(VoiceTransportState.Connected)
        transport.interruptionEnded()
        assertTrue(transport.microphoneChanges.isEmpty())
    }
}
