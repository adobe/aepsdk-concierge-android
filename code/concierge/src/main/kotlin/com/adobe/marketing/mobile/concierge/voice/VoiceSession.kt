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

/** One voice conversation, from [start] until it reaches a terminal [state]. */
internal interface VoiceSession {
    /** The current state; safe to read from any thread. */
    val state: VoiceSessionState

    /**
     * Whether the conversation may keep running while the app is in the background: the transport
     * [continues in the background][com.adobe.marketing.mobile.concierge.VoiceTransport.continuesInBackground]
     * (read when the session connected it), the session has already called `connect`, and it has
     * not ended. Safe to read from any thread.
     */
    val continuesInBackground: Boolean

    /**
     * Starts the conversation and returns immediately. Invalid credentials, a missing microphone
     * permission and connection failures are reported to the listener as [VoiceSessionState.Failed].
     */
    fun start(url: String, token: String)

    /**
     * Ends the session; the end is reported as [VoiceSessionState.Ended]. Calling it again, or
     * after the session has already ended or failed, has no effect.
     */
    fun stop()
}

/** Receives updates for one [VoiceSession]. Every method is called on the main thread. */
internal interface VoiceSessionListener {
    /** Called whenever the session's state changes. [state] is also `session.state`. */
    fun onStateChanged(session: VoiceSession, state: VoiceSessionState)

    /**
     * Called whenever a transcript turn is updated. [text] is the turn's full text so far, not a
     * delta. [isFinal] closes the turn; the next update for the same role starts a new one.
     */
    fun onTranscriptUpdated(session: VoiceSession, text: String, role: VoiceTranscriptRole, isFinal: Boolean)
}

/**
 * The lifecycle of a [VoiceSession]: [Connecting], then [Listening] (moving to [Reconnecting] and
 * back while the transport restores a lost connection), then [Ended] or [Failed], which are
 * terminal.
 */
internal sealed class VoiceSessionState {
    object Connecting : VoiceSessionState() {
        override fun toString(): String = "Connecting"
    }

    /** Connected, with the microphone live. */
    object Listening : VoiceSessionState() {
        override fun toString(): String = "Listening"
    }

    /** The transport lost the connection and is restoring it; the conversation stays open. */
    object Reconnecting : VoiceSessionState() {
        override fun toString(): String = "Reconnecting"
    }

    object Ended : VoiceSessionState() {
        override fun toString(): String = "Ended"
    }

    /** Could not start, or the connection was lost. The failure is for logging and tracking, not for display. */
    data class Failed(val failure: VoiceSessionFailure) : VoiceSessionState()

    val isTerminal: Boolean
        get() = this is Ended || this is Failed
}

/**
 * Why a voice session failed. Each case has a fixed [message], which is logged and sent to Edge
 * with `ErrorOccurred`. Error text from the transport or the network is never part of it: a
 * transport's error can carry the server URL or a server response.
 */
internal sealed class VoiceSessionFailure(val message: String) {
    /** The credentials had no `wss`/`https` server URL or no token. */
    object InvalidCredentials :
        VoiceSessionFailure("Voice connection requires a valid server URL and a non-empty token.")

    object MicrophonePermissionDenied :
        VoiceSessionFailure("Microphone permission (RECORD_AUDIO) has not been granted.")

    /** The transport reported neither `Connected` nor `Failed` in time. */
    data class ConnectTimeout(val seconds: Long) :
        VoiceSessionFailure("The voice connection was not established within $seconds seconds.")

    /** The transport did not restore a lost connection in time. */
    data class ReconnectTimeout(val seconds: Long) :
        VoiceSessionFailure("The voice connection was not restored within $seconds seconds.")

    /** The voice agent ended the session with an error, a shutdown, or a reason this SDK doesn't know (null). */
    data class AgentEnded(val reason: EndReason?) :
        VoiceSessionFailure("The voice agent ended the session (${reason?.wireValue ?: "unknown reason"}).")

    /** The transport threw from `connect` or reported `Failed`. */
    object TransportFailed : VoiceSessionFailure("The voice transport reported a failure.")

    /** The Concierge service returned no voice credentials. */
    object BootstrapFailed : VoiceSessionFailure("The Concierge service did not return voice session credentials.")
}

internal enum class VoiceTranscriptRole { USER, ASSISTANT }
