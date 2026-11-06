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
 *   Brand Concierge runs one conversation at a time across the app, so it never calls [connect]
 *   while an earlier connection is still open.
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
     * [VoiceTransportState.Failed] if the connection cannot be made. If neither arrives within 20
     * seconds, Brand Concierge fails the conversation and calls [disconnect].
     *
     * @param connection where to connect and the credentials to use, from the Concierge service.
     */
    fun connect(connection: VoiceConnection)

    /**
     * Mutes or unmutes the microphone without leaving the room. Brand Concierge mutes it while
     * another app holds the audio (see [VoiceTransportListener.onAudioInterruptionBegan]),
     * including an interruption that began while connecting, and unmutes it afterwards. Called
     * only while connected.
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
     * Called when another app takes the audio for a while, for example when the transport's audio
     * focus is lost transiently (`AUDIOFOCUS_LOSS_TRANSIENT`). Brand Concierge mutes the microphone
     * until [onAudioInterruptionEnded]. A transport that never reports interruptions keeps the
     * microphone live throughout.
     *
     * A phone call rarely gets this far: Android doesn't tell other apps about a ringing call
     * without the `READ_PHONE_STATE` permission, and answering opens the call screen, which moves
     * the app to the background and so ends the conversation.
     *
     * If the audio will not come back (for example, `AUDIOFOCUS_LOSS`), report
     * [VoiceTransportState.Failed] instead: the chat has no unmute control, so a muted
     * conversation would look live while the agent hears nothing.
     */
    fun onAudioInterruptionBegan(transport: VoiceTransport)

    /** Called when the audio returns after [onAudioInterruptionBegan]. */
    fun onAudioInterruptionEnded(transport: VoiceTransport)
}

/**
 * Where and how a [VoiceTransport] connects, from the Concierge service. Brand Concierge creates
 * it; later releases may add properties.
 *
 * @property url the real-time media server URL.
 * @property token the access token for the room.
 */
class VoiceConnection internal constructor(
    val url: String,
    val token: String
)

/** The state of a [VoiceTransport] connection. */
sealed class VoiceTransportState {
    /** Connected, with the microphone live. Also reported when a reconnect succeeds. */
    object Connected : VoiceTransportState() {
        override fun toString(): String = "Connected"
    }

    /**
     * The connection was lost and the transport is restoring it. Report [Connected] once it is
     * back, or [Failed] if it gives up. Brand Concierge keeps the conversation open meanwhile, and
     * fails it if the connection is not back within 30 seconds.
     */
    object Reconnecting : VoiceTransportState() {
        override fun toString(): String = "Reconnecting"
    }

    /** The connection ended without an error, for example because the room closed. */
    object Disconnected : VoiceTransportState() {
        override fun toString(): String = "Disconnected"
    }

    /** The connection could not be made, or was lost because of [error]. */
    data class Failed(val error: Throwable) : VoiceTransportState()
}
