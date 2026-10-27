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
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionController.FailureReasons
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole.ASSISTANT
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole.USER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException
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
            "wss://" to token,
            "not a url" to token
        ).forEach { (invalidUrl, invalidToken) ->
            permissionChecks = 0
            val transport = FakeVoiceTransport()
            val listener = RecordingVoiceListener()
            val session = start(transport, listener, invalidUrl, invalidToken)
            val failed = VoiceSessionState.Failed(FailureReasons.INVALID_CONNECTION)
            assertEquals("$invalidUrl / $invalidToken", listOf(VoiceSessionState.Connecting, failed), listener.states)
            assertEquals(failed, session.state)
            assertEquals(0, permissionChecks)
            assertTrue(transport.connections.isEmpty())
            assertNull(transport.listener)
        }
    }

    @Test
    fun permissionDenied_failsWithoutTouchingTheTransport() {
        permissionGranted = false
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        val failed = VoiceSessionState.Failed(FailureReasons.MICROPHONE_PERMISSION)
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
        assertEquals(VoiceSessionState.Failed(FailureReasons.MICROPHONE_PERMISSION), session.state)
        assertTrue(transport.connections.isEmpty())
    }

    @Test
    fun throwingConnect_failsAndReleasesTheTransport() {
        val transport = FakeVoiceTransport(connectError = IllegalStateException("connect boom"))
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        assertEquals(VoiceSessionState.Failed("connect boom"), session.state)
        assertEquals(listOf(VoiceSessionState.Connecting, VoiceSessionState.Failed("connect boom")), listener.states)
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
    fun transportFailure_reportsTheReasonAndReleasesTheTransport() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        transport.report(VoiceTransportState.Failed(IOException("network lost")))
        assertEquals(VoiceSessionState.Failed("network lost"), session.state)
        assertEquals(1, transport.disconnectCount)
        assertNull(transport.listener)
    }

    @Test
    fun transportFailureWithoutMessage_reportsTheDefaultReason() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.report(VoiceTransportState.Failed(RuntimeException()))
        assertEquals(VoiceSessionState.Failed(FailureReasons.CONNECT_FAILED), session.state)
    }

    @Test
    fun transportCallbacksFromBackgroundThread_areDeliveredOnMain() {
        listOf(false, true).forEach { failed ->
            val transport = FakeVoiceTransport()
            val listener = RecordingVoiceListener()
            val session = start(transport, listener)
            val state = if (failed) VoiceTransportState.Failed(IOException("lost")) else VoiceTransportState.Disconnected
            runOnBackgroundThread { transport.report(state) }
            val expected = if (failed) VoiceSessionState.Failed("lost") else VoiceSessionState.Ended
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
        transport.send("""{"event":"text_delta","delta":"two"}""")

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
        transport.send("""{"type":"transcript_delta","data":{"delta":" hello ","final":false}}""")
        transport.send("""{"type":"transcript_delta","data":{"delta":" hello world ","final":true}}""")
        assertEquals(
            listOf(TranscriptEvent("hello", USER, false), TranscriptEvent("hello world", USER, true)),
            listener.transcripts
        )
    }

    @Test
    fun assistantTranscript_accumulatesAndUsesAuthoritativeFinalText() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.send("""{"event":"text_delta","delta":" Hello"}""")
        transport.send("""{"event":"text_delta","delta":" world "}""")
        transport.send("""{"event":"turn_completed","fullText":" Corrected reply "}""")
        transport.send("""{"event":"text_delta","delta":" Next turn"}""")
        transport.send("""{"event":"turn_completed"}""")
        assertEquals(
            listOf(
                TranscriptEvent("Hello", ASSISTANT, false),
                TranscriptEvent("Hello world", ASSISTANT, false),
                TranscriptEvent("Corrected reply", ASSISTANT, true),
                TranscriptEvent("Next turn", ASSISTANT, false),
                TranscriptEvent("Next turn", ASSISTANT, true)
            ),
            listener.transcripts
        )
    }

    @Test
    fun assistantTypeBasedFinal_closesTurn() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        transport.send("""{"type":"transcript_delta","data":{"role":"assistant","delta":"first","final":true}}""")
        transport.send("""{"event":"text_delta","delta":"second"}""")
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
        transport.send("""{"type":"transcript_delta","data":{"delta":"  "}}""")
        transport.send("""{"type":"transcript_delta","data":{"delta":"  ","final":true}}""")
        assertEquals(listOf(TranscriptEvent("", USER, true)), listener.transcripts)
    }

    @Test
    fun dataFromBackgroundThread_isDeliveredOnMain() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        start(transport, listener)
        runOnBackgroundThread { transport.send("""{"event":"text_delta","delta":"hello"}""") }
        assertEquals(listOf(TranscriptEvent("hello", ASSISTANT, false)), listener.transcripts)
        assertTrue(listener.callbackLoopers.all { it == Looper.getMainLooper() })
    }

    @Test
    fun sessionEndedNotice_endsTheSession() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.send("""{"type":"session_notice","data":{"kind":"session_ended","reason":"user_inactivity_timeout"}}""")
        assertEquals(VoiceSessionState.Ended, session.state)
        assertEquals(1, transport.disconnectCount)
    }

    @Test
    fun ignoredMessages_doNotEmitTranscriptOrChangeState() {
        val transport = FakeVoiceTransport()
        val listener = RecordingVoiceListener()
        val session = start(transport, listener)
        listOf(
            """{"event":"ui_payload","promptSuggestions":["one"]}""",
            """{"event":"turn_started"}""",
            """{"type":"session_notice","data":{"kind":"user_silence_warning"}}""",
            """{"event":"unknown"}"""
        ).forEach(transport::send)
        transport.listener!!.onDataReceived(transport, byteArrayOf(0, 1, 2), null)
        assertTrue(listener.transcripts.isEmpty())
        assertEquals(VoiceSessionState.Listening, session.state)
    }

    // Audio interruptions

    @Test
    fun audioInterruption_mutesThenRestoresTheMicrophone() {
        val transport = FakeVoiceTransport()
        val session = start(transport)
        transport.interruptionBegan()
        // A second loss (e.g. transient followed by permanent) must not forget the original state.
        transport.interruptionBegan()
        transport.interruptionEnded()
        assertEquals(listOf(false, true), transport.microphoneChanges)
        assertEquals(VoiceSessionState.Listening, session.state)
    }

    @Test
    fun audioInterruptionEnded_withoutBegan_isIgnored() {
        val transport = FakeVoiceTransport()
        start(transport)
        transport.interruptionEnded()
        assertTrue(transport.microphoneChanges.isEmpty())
    }

    @Test
    fun audioInterruption_beforeListening_isIgnored() {
        val transport = FakeVoiceTransport(connectsImmediately = false)
        start(transport)
        transport.interruptionBegan()
        transport.report(VoiceTransportState.Connected)
        transport.interruptionEnded()
        assertTrue(transport.microphoneChanges.isEmpty())
    }
}
