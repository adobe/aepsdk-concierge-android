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
 * transcript updates and turn results.
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

    /**
     * The turn in flight, from the user's first words (or the agent's, when it speaks first) until
     * its `turn_completed` or the next user turn.
     */
    private var turn: Turn? = null

    /**
     * Turns that ended. Their late messages, such as the worker's second `turn_completed` after a
     * playback error, are dropped.
     */
    private val closedTurnIds = HashSet<String>()

    /** The in-flight turn's assistant text so far. */
    private val pendingAssistantText = StringBuilder()

    /** The in-flight turn's latest UI payload. Each one is a snapshot of the whole turn. */
    private var latestUiPayload: UiPayloadData? = null

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
            fail(VoiceSessionFailure.InvalidCredentials)
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
            fail(VoiceSessionFailure.MicrophonePermissionDenied)
            return
        }

        holdsTransport = true
        transport.listener = transportListener
        // The transport is app code; don't wait forever for it to report back. Started before
        // connect so that a transport reporting synchronously still cancels it.
        startConnectionTimeout(
            CONNECT_TIMEOUT_MILLIS,
            VoiceSessionState.Connecting,
            VoiceSessionFailure.ConnectTimeout(CONNECT_TIMEOUT_MILLIS / 1000)
        )
        try {
            transportContinuesInBackground = transport.continuesInBackground
            transport.connect(VoiceConnection(url, token))
        } catch (e: Exception) {
            failForTransportError("connect", e)
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
                        VoiceSessionFailure.ReconnectTimeout(RECONNECT_TIMEOUT_MILLIS / 1000)
                    )
                }
            }
            is VoiceTransportState.Disconnected -> finish(VoiceSessionState.Ended)
            is VoiceTransportState.Failed -> failForTransportError("reported Failed", transportState.error)
        }
    }

    /**
     * Fails the session with [failure], which also disconnects the transport, if it is still in
     * [waitingState] after [timeoutMillis].
     */
    private fun startConnectionTimeout(timeoutMillis: Long, waitingState: VoiceSessionState, failure: VoiceSessionFailure) {
        cancelConnectionTimeout()
        val timeout = Runnable { if (currentState == waitingState) fail(failure) }
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
     * on when it connects, and `Connected` applies any interruption. If the transport throws, the
     * microphone may be live during an interruption or muted while the chat shows Listening, so the
     * conversation fails.
     */
    private fun setMicrophoneEnabled(enabled: Boolean) {
        if (!holdsTransport || !isConnected) return
        try {
            transport.setMicrophoneEnabled(enabled)
        } catch (e: Exception) {
            failForTransportError("setMicrophoneEnabled($enabled)", e)
        }
    }

    private fun fail(failure: VoiceSessionFailure) {
        if (!isActive) return
        Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice session failed: %s", failure.message)
        finish(VoiceSessionState.Failed(failure))
    }

    private fun failForTransportError(operation: String, error: Throwable) {
        logTransportError(operation, error)
        fail(VoiceSessionFailure.TransportFailed)
    }

    /**
     * The transport is app code, and its errors can carry the server URL or a server response, so
     * their text is logged at debug level only and never tracked.
     */
    private fun logTransportError(operation: String, error: Throwable) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice transport error (%s): %s", operation, error.message)
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
                Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Voice transport failed to disconnect.")
                logTransportError("disconnect", e)
            }
        }
        turn = null
        pendingAssistantText.setLength(0)
        latestUiPayload = null
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
                if (message.data.role == TranscriptDeltaData.Role.USER) {
                    handleUserTranscript(message.data)
                } else {
                    handleAssistantTranscript(message.data)
                }
            }
            is DataChannelMessage.UiPayload -> {
                if (acceptsTurnMessage(message.data.turnId, startsTurn = false)) {
                    latestUiPayload = message.data
                }
            }
            is DataChannelMessage.TurnDone -> handleTurnDone(message.data)
            is DataChannelMessage.SessionNotice -> {
                val notice = message.data
                if (notice is SessionNoticeData.SessionEnded) handleSessionEnded(notice.reason)
            }
            is DataChannelMessage.TextInputError, is DataChannelMessage.StateUpdate -> Unit
        }
    }

    /**
     * Mirrors web's turn handling (`LiveKitVoiceManager`): the user's transcript decides the
     * current turn, and messages for any other turn are dropped.
     */
    private class Turn(
        /** The worker's turn id, null until a message for the turn carries one. */
        var id: String?
    ) {
        var userFinal = false
        var replyStarted = false
    }

    private fun handleUserTranscript(delta: TranscriptDeltaData) {
        if (delta.delta.isNotBlank()) {
            val id = delta.turnId
            if (id != null && id in closedTurnIds) {
                Log.trace(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Dropped a user transcript for a turn that already ended.")
                return
            }
            val current = turn
            if (startsNewTurn(id)) {
                beginTurn(id)
            } else if (current != null && current.id == null) {
                current.id = id
            }
            if (delta.final) turn?.userFinal = true
        }
        // User STT chunks are cumulative snapshots of the current utterance.
        emit(VoiceTranscriptRole.USER, delta.delta, delta.final)
    }

    private fun startsNewTurn(userTurnId: String?): Boolean {
        val current = turn ?: return true
        val currentId = current.id
        if (userTurnId != null && currentId != null) return userTurnId != currentId
        return current.userFinal || current.replyStarted
    }

    private fun handleAssistantTranscript(delta: TranscriptDeltaData) {
        if (!acceptsTurnMessage(delta.turnId, startsTurn = true)) return
        turn?.replyStarted = true
        pendingAssistantText.append(delta.delta)
        val text = pendingAssistantText.toString()
        if (delta.final) pendingAssistantText.setLength(0)
        emit(VoiceTranscriptRole.ASSISTANT, text, delta.final)
    }

    /**
     * Mirrors web's `handleTurnDone`. An error or an empty reply fails the turn; `interrupted`
     * counts as completed.
     */
    private fun handleTurnDone(done: TurnDoneData) {
        if (turn == null || !acceptsTurnMessage(done.turnId, startsTurn = false)) {
            Log.trace(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Dropped a turn_completed for a turn that is not in flight.")
            return
        }
        val fullText = done.fullText.trim()
        if (done.state == TurnDoneData.TurnState.ERROR || fullText.isEmpty()) {
            finishTurn(VoiceTurnResult.Outcome.Failed(VoiceTurnFailure.TURN_FAILED))
        } else {
            finishTurn(VoiceTurnResult.Outcome.Completed(fullText))
        }
    }

    /**
     * Whether an assistant message belongs to the turn in flight. A turn without an id adopts the
     * message's id. With no turn in flight, only a message that [startsTurn] is kept, as the start
     * of a turn the agent opens.
     */
    private fun acceptsTurnMessage(turnId: String?, startsTurn: Boolean): Boolean {
        if (turnId != null && turnId in closedTurnIds) return false
        val current = turn
        if (current == null) {
            if (!startsTurn) return false
            beginTurn(turnId)
            return true
        }
        if (turnId == null) return true
        val currentId = current.id
        if (currentId == null) {
            current.id = turnId
            return true
        }
        return turnId == currentId
    }

    /** Closes the turn in flight, if any, and opens a new one. */
    private fun beginTurn(id: String?) {
        if (turn != null) {
            // The previous turn got no turn_completed, for example because the user interrupted it.
            // Close its reply bubble and drop its UI payload.
            val text = pendingAssistantText.toString()
            if (text.isNotBlank()) emit(VoiceTranscriptRole.ASSISTANT, text, true)
            closeTurn()
        }
        turn = Turn(id)
    }

    private fun finishTurn(outcome: VoiceTurnResult.Outcome) {
        val result = VoiceTurnResult(outcome, latestUiPayload)
        closeTurn()
        listener?.onTurnFinished(this, result)
    }

    private fun closeTurn() {
        turn?.id?.let { closedTurnIds.add(it) }
        turn = null
        pendingAssistantText.setLength(0)
        latestUiPayload = null
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
            EndReason.ERROR, EndReason.WORKER_SHUTDOWN, null -> fail(VoiceSessionFailure.AgentEnded(reason))
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
