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

internal sealed class VoiceSessionState {
    object Connecting : VoiceSessionState() {
        override fun toString(): String = "Connecting"
    }

    /** Connected, with the microphone live. */
    object Listening : VoiceSessionState() {
        override fun toString(): String = "Listening"
    }

    object Ended : VoiceSessionState() {
        override fun toString(): String = "Ended"
    }

    data class Failed(val reason: String) : VoiceSessionState()

    val isTerminal: Boolean
        get() = this is Ended || this is Failed
}

internal enum class VoiceTranscriptRole { USER, ASSISTANT }
