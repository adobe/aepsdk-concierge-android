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

package com.adobe.marketing.mobile.concierge

/**
 * The real-time audio and data connection that a voice conversation runs over, provided by the app.
 *
 * Brand Concierge runs the conversation itself: it fetches the connection credentials, checks the
 * microphone permission, reads the voice agent's messages and renders the transcript. The transport
 * only carries audio and data, so Brand Concierge does not depend on any real-time media SDK. The
 * test app contains a complete LiveKit implementation to copy.
 *
 * Pass a transport to `ConciergeChat(...)`, or set `ConciergeChatView.voiceTransport`. Without one,
 * the voice button is hidden.
 *
 * Implementations must follow these rules:
 * - Brand Concierge calls every method on the main thread. Each method returns immediately and
 *   reports its outcome to [listener].
 * - One instance serves every conversation: [connect] can be called again after [disconnect]. Keep
 *   it for the lifetime of the app (for example, in your `Application`), not in an `Activity`.
 * - Brand Concierge sets [listener] before each [connect] and clears it before [disconnect].
 * - After [disconnect], report nothing more about that connection, even if a new connection has
 *   started since.
 * - Listener methods can be called on any thread.
 */
interface VoiceTransport {
    /** Receives the connection's state changes, data messages and audio interruptions. */
    var listener: VoiceTransportListener?

    /**
     * Joins the voice agent's room, publishes the microphone and plays the agent's audio.
     *
     * Report [VoiceTransportState.Connected] once the microphone is live, or
     * [VoiceTransportState.Failed] if the connection cannot be made.
     *
     * @param url the real-time media server URL from the Concierge service.
     * @param token the access token for the room, from the Concierge service.
     */
    fun connect(url: String, token: String)

    /**
     * Mutes or unmutes the microphone without leaving the room. Brand Concierge mutes it while
     * another app or a phone call holds the audio (see
     * [VoiceTransportListener.onAudioInterruptionBegan]), and unmutes it afterwards.
     */
    fun setMicrophoneEnabled(enabled: Boolean)

    /**
     * Leaves the room and releases the microphone. Safe to call at any time, including while
     * connecting or when not connected.
     */
    fun disconnect()
}

/** Receives updates from a [VoiceTransport]. Methods can be called on any thread. */
interface VoiceTransportListener {
    /** Called when the connection's state changes. */
    fun onStateChanged(transport: VoiceTransport, state: VoiceTransportState)

    /**
     * Called for every data message the voice agent sends.
     *
     * @param data the message payload, unchanged.
     * @param topic the message topic, or null if the transport has none.
     */
    fun onDataReceived(transport: VoiceTransport, data: ByteArray, topic: String?)

    /**
     * Called when another app or a phone call takes the audio, for example when the transport's
     * audio focus is lost. Brand Concierge mutes the microphone until
     * [onAudioInterruptionEnded]. A transport that never reports interruptions keeps the
     * microphone live throughout.
     */
    fun onAudioInterruptionBegan(transport: VoiceTransport)

    /** Called when the audio returns after [onAudioInterruptionBegan]. */
    fun onAudioInterruptionEnded(transport: VoiceTransport)
}

/** The state of a [VoiceTransport] connection. */
sealed class VoiceTransportState {
    /** Connected, with the microphone live. */
    object Connected : VoiceTransportState() {
        override fun toString(): String = "Connected"
    }

    /** The connection ended without an error, for example because the room closed. */
    object Disconnected : VoiceTransportState() {
        override fun toString(): String = "Disconnected"
    }

    /** The connection could not be made, or was lost because of [error]. */
    data class Failed(val error: Throwable) : VoiceTransportState()
}
