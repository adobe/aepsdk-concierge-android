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

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.VoiceConnection
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.VoiceTransportListener
import com.adobe.marketing.mobile.concierge.VoiceTransportState
import com.adobe.marketing.mobile.services.Log

/**
 * One voice conversation over the app's [VoiceTransport]: the microphone permission check, the
 * connection lifecycle, audio interruptions, and turning the voice agent's data messages into
 * transcript updates.
 *
 * The transport is shared by every conversation in the app. Only one conversation runs at a time
 * (see [VoiceSessionOwnership]), and a session claims the transport (becomes its listener) only for
 * the time it is connected, and releases it before disconnecting. Callbacks
 * from any other transport, or after the release, are dropped. Lifecycle and transcript work runs
 * on the main thread; [state] may be read, and [stop] called, from any thread.
 *
 * The listener is held until the session reaches a terminal state and is then released, so a
 * host that forgets the session after [stop] does not leak its UI.
 *
 * Every conversation ends normally [MAX_SESSION_MILLIS] after it first reports `Connected`,
 * whether or not it runs in the background.
 */
internal class VoiceSessionController(
    private val transport: VoiceTransport,
    listener: VoiceSessionListener,
    private val hasMicrophonePermission: () -> Boolean
) : VoiceSession {

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var currentState: VoiceSessionState = VoiceSessionState.Connecting

    // The fields below are confined to the main thread.
    private var listener: VoiceSessionListener? = listener
    private val pendingAssistantText = StringBuilder()

    /** True from `connect` until this session disconnects the transport. */
    private var holdsTransport = false

    /**
     * True from an audio interruption (another app taking the audio) until it ends, in any active
     * state. Tracked apart from the session state, so a `Connected` that arrives during the
     * interruption doesn't leave the microphone live. The transport has no microphone getter: the
     * microphone is live once connected, and only interruptions change it after that.
     */
    private var isAudioInterrupted = false

    /** Runs while waiting for `Connected`, first after `connect` and again while reconnecting. */
    private var connectionTimeout: Runnable? = null

    /** Ends the conversation at its maximum length; started by the first `Connected` only. */
    private var sessionLengthLimit: Runnable? = null

    /** The transport's [VoiceTransport.continuesInBackground], read when this session connected it. */
    @Volatile
    private var transportContinuesInBackground = false

    private val transportListener = object : VoiceTransportListener {
        override fun onStateChanged(transport: VoiceTransport, state: VoiceTransportState) {
            onMain { if (ownsCallback(transport)) handleTransportState(state) }
        }

        override fun onDataReceived(transport: VoiceTransport, data: ByteArray, topic: String?) {
            onMain {
                if (!ownsCallback(transport)) return@onMain
                val message = DataChannelMessageParser.parse(data)
                if (message == null) {
                    Log.trace(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Dropped an unrecognized data-channel message.")
                    return@onMain
                }
                handle(message)
            }
        }

        override fun onAudioInterruptionBegan(transport: VoiceTransport) {
            onMain {
                if (!ownsCallback(transport) || isAudioInterrupted) return@onMain
                isAudioInterrupted = true
                setMicrophoneEnabled(false)
            }
        }

        override fun onAudioInterruptionEnded(transport: VoiceTransport) {
            onMain {
                if (!ownsCallback(transport) || !isAudioInterrupted) return@onMain
                isAudioInterrupted = false
                setMicrophoneEnabled(true)
            }
        }
    }

    override val state: VoiceSessionState
        get() = currentState

    override val continuesInBackground: Boolean
        get() = transportContinuesInBackground && isActive

    private val isActive: Boolean
        get() = !currentState.isTerminal

    /** Listening, or restoring a connection that was live; the microphone may be changed. */
    private val isConnected: Boolean
        get() = currentState == VoiceSessionState.Listening || currentState == VoiceSessionState.Reconnecting

    /** Always deferred, including validation failures, so the caller can keep the session first. */
    override fun start(url: String, token: String) {
        mainHandler.post { begin(url, token) }
    }

    override fun stop() {
        onMain { finish(VoiceSessionState.Ended) }
    }

    private fun begin(url: String, token: String) {
        if (!isActive) return
        listener?.onStateChanged(this, VoiceSessionState.Connecting)
        if (!isActive) return

        if (!isValidServerUrl(url) || token.isBlank()) {
            fail(FailureReasons.INVALID_CONNECTION)
            return
        }
        // ConciergeChatViewModel already checks the permission before requesting the session. This
        // second check is a safety net for any caller that starts a session without going through
        // it, and for a permission revoked while the session was being requested.
        // This runs in a posted Runnable, so nothing may escape: report failures instead.
        val permissionGranted = try {
            hasMicrophonePermission()
        } catch (e: Exception) {
            false
        }
        if (!permissionGranted) {
            fail(FailureReasons.MICROPHONE_PERMISSION)
            return
        }

        holdsTransport = true
        transport.listener = transportListener
        // The transport is app code; don't wait forever for it to report back. Started before
        // connect so that a transport reporting synchronously still cancels it.
        startConnectionTimeout(
            CONNECT_TIMEOUT_MILLIS,
            VoiceSessionState.Connecting,
            "The voice connection was not established within ${CONNECT_TIMEOUT_MILLIS / 1000} seconds."
        )
        try {
            transportContinuesInBackground = transport.continuesInBackground
            transport.connect(VoiceConnection(url, token))
        } catch (e: Exception) {
            fail(e.message ?: FailureReasons.CONNECT_FAILED)
        }
    }

    /** True when a transport callback belongs to this session's live connection. */
    private fun ownsCallback(sender: VoiceTransport): Boolean =
        sender === transport && holdsTransport && isActive

    private fun handleTransportState(transportState: VoiceTransportState) {
        when (transportState) {
            is VoiceTransportState.Connected -> when (currentState) {
                VoiceSessionState.Connecting -> {
                    cancelConnectionTimeout()
                    startSessionLengthLimit()
                    setState(VoiceSessionState.Listening)
                    // The transport turned the microphone on; an interruption that began while
                    // connecting turns it off again.
                    if (isAudioInterrupted) setMicrophoneEnabled(false)
                }
                VoiceSessionState.Reconnecting -> {
                    // The transport keeps the microphone as it was; an interruption mute stays.
                    cancelConnectionTimeout()
                    setState(VoiceSessionState.Listening)
                }
                else -> Unit
            }
            is VoiceTransportState.Reconnecting -> {
                if (currentState != VoiceSessionState.Listening) return
                setState(VoiceSessionState.Reconnecting)
                // setState may have run a listener that ended the session.
                if (currentState == VoiceSessionState.Reconnecting) {
                    startConnectionTimeout(
                        RECONNECT_TIMEOUT_MILLIS,
                        VoiceSessionState.Reconnecting,
                        "The voice connection was not restored within ${RECONNECT_TIMEOUT_MILLIS / 1000} seconds."
                    )
                }
            }
            is VoiceTransportState.Disconnected -> finish(VoiceSessionState.Ended)
            is VoiceTransportState.Failed ->
                fail(transportState.error.message ?: FailureReasons.CONNECT_FAILED)
        }
    }

    /**
     * Fails the session with [reason], which also disconnects the transport, if it is still in
     * [waitingState] after [timeoutMillis].
     */
    private fun startConnectionTimeout(timeoutMillis: Long, waitingState: VoiceSessionState, reason: String) {
        cancelConnectionTimeout()
        val timeout = Runnable { if (currentState == waitingState) fail(reason) }
        connectionTimeout = timeout
        mainHandler.postDelayed(timeout, timeoutMillis)
    }

    private fun cancelConnectionTimeout() {
        connectionTimeout?.let(mainHandler::removeCallbacks)
        connectionTimeout = null
    }

    /**
     * Ends the conversation [MAX_SESSION_MILLIS] from now, as a normal end that also disconnects
     * the transport, so a forgotten conversation cannot hold the room indefinitely. Not restarted
     * by a reconnect.
     */
    private fun startSessionLengthLimit() {
        val limit = Runnable {
            Log.debug(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice session reached its maximum length.")
            finish(VoiceSessionState.Ended)
        }
        sessionLengthLimit = limit
        mainHandler.postDelayed(limit, MAX_SESSION_MILLIS)
    }

    /**
     * Changes the microphone once connected. Before that it is left alone: the transport turns it
     * on when it connects, and `Connected` applies any interruption.
     */
    private fun setMicrophoneEnabled(enabled: Boolean) {
        if (!holdsTransport || !isConnected) return
        try {
            transport.setMicrophoneEnabled(enabled)
        } catch (e: Exception) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                SELF_TAG,
                "Failed to set microphone enabled=%s: %s",
                enabled,
                e.message
            )
        }
    }

    private fun fail(reason: String) {
        if (!isActive) return
        Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice session failed: %s", reason)
        finish(VoiceSessionState.Failed(reason))
    }

    /**
     * Moves to a terminal state. The transport is released first, so it is free again by the time
     * the listener hears about the end.
     */
    private fun finish(terminalState: VoiceSessionState) {
        if (!isActive) return
        cancelConnectionTimeout()
        sessionLengthLimit?.let(mainHandler::removeCallbacks)
        sessionLengthLimit = null
        if (holdsTransport) {
            holdsTransport = false
            if (transport.listener === transportListener) {
                transport.listener = null
            }
            try {
                transport.disconnect()
            } catch (e: Exception) {
                Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice transport failed to disconnect: %s", e.message)
            }
        }
        pendingAssistantText.setLength(0)
        setState(terminalState)
    }

    private fun setState(newState: VoiceSessionState) {
        if (!isActive || currentState == newState) return
        currentState = newState
        val listener = listener
        if (newState.isTerminal) this.listener = null
        listener?.onStateChanged(this, newState)
    }

    private fun handle(message: DataChannelMessage) {
        when (message) {
            is DataChannelMessage.TranscriptDelta -> {
                val delta = message.data
                if (delta.role == TranscriptDeltaData.Role.USER) {
                    // User STT chunks are cumulative snapshots of the current utterance.
                    emit(VoiceTranscriptRole.USER, delta.delta, delta.final)
                } else {
                    pendingAssistantText.append(delta.delta)
                    val text = pendingAssistantText.toString()
                    if (delta.final) pendingAssistantText.setLength(0)
                    emit(VoiceTranscriptRole.ASSISTANT, text, delta.final)
                }
            }
            is DataChannelMessage.TurnDone -> {
                val fullText = message.data.fullText.trim()
                val text = if (fullText.isEmpty()) pendingAssistantText.toString() else fullText
                pendingAssistantText.setLength(0)
                emit(VoiceTranscriptRole.ASSISTANT, text, true)
            }
            is DataChannelMessage.SessionNotice -> {
                val notice = message.data
                if (notice is SessionNoticeData.SessionEnded) handleSessionEnded(notice.reason)
            }
            is DataChannelMessage.UiPayload, is DataChannelMessage.StateUpdate -> Unit
        }
    }

    /**
     * The voice agent ended the session. Only its timeouts are a normal end. An error, a shutdown or
     * a reason this SDK doesn't know is a failure, so the user sees a message and it is tracked.
     */
    private fun handleSessionEnded(reason: EndReason?) {
        when (reason) {
            EndReason.ROOM_EMPTY_TIMEOUT,
            EndReason.USER_JOIN_TIMEOUT,
            EndReason.USER_INACTIVITY_TIMEOUT -> finish(VoiceSessionState.Ended)
            EndReason.ERROR, EndReason.WORKER_SHUTDOWN, null ->
                fail("The voice agent ended the session (${reason?.wireValue ?: "unknown reason"}).")
        }
    }

    private fun emit(role: VoiceTranscriptRole, text: String, isFinal: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && !isFinal) return
        listener?.onTranscriptUpdated(this, trimmed, role, isFinal)
    }

    private fun onMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) action() else mainHandler.post(action)
    }

    internal object FailureReasons {
        const val INVALID_CONNECTION = "Voice connection requires a valid server URL and a non-empty token."
        const val MICROPHONE_PERMISSION = "Microphone permission (RECORD_AUDIO) has not been granted."
        const val CONNECT_FAILED = "Failed to connect to the voice session."
    }

    internal companion object {
        /** How long the transport may take to report `Connected` after `connect` before the session fails. */
        const val CONNECT_TIMEOUT_MILLIS = 20_000L

        /** How long a lost connection may take to come back before the session fails. */
        const val RECONNECT_TIMEOUT_MILLIS = 30_000L

        /** How long a conversation may last from its first `Connected` before it ends normally. */
        const val MAX_SESSION_MILLIS = 60 * 60_000L

        private const val SELF_TAG = "VoiceSessionController"
        /** Encrypted schemes only: the access token travels on this connection. */
        private val SUPPORTED_SCHEMES = setOf("wss", "https")

        private fun isValidServerUrl(url: String): Boolean {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme?.lowercase() ?: return false
            return scheme in SUPPORTED_SCHEMES && !parsed.host.isNullOrEmpty()
        }
    }
}
