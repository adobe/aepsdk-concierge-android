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
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.VoiceTransportListener
import com.adobe.marketing.mobile.concierge.VoiceTransportState

/**
 * In-memory [VoiceTransport]: records what the session asks for and lets a test deliver the
 * transport's callbacks, without LiveKit or audio.
 */
internal class FakeVoiceTransport(
    /** When true, [connect] reports [VoiceTransportState.Connected] before it returns. */
    private val connectsImmediately: Boolean = true,
    var connectError: Exception? = null
) : VoiceTransport {

    override var listener: VoiceTransportListener? = null
    val connections = mutableListOf<Pair<String, String>>()
    val microphoneChanges = mutableListOf<Boolean>()
    var disconnectCount = 0
        private set

    /** The listener held when [disconnect] was last called. */
    var listenerAtDisconnect: VoiceTransportListener? = null
        private set

    override fun connect(url: String, token: String) {
        connections += url to token
        connectError?.let { throw it }
        if (connectsImmediately) report(VoiceTransportState.Connected)
    }

    override fun setMicrophoneEnabled(enabled: Boolean) {
        microphoneChanges += enabled
    }

    override fun disconnect() {
        disconnectCount++
        listenerAtDisconnect = listener
    }

    fun report(state: VoiceTransportState) {
        listener?.onStateChanged(this, state)
    }

    fun send(json: String) {
        listener?.onDataReceived(this, json.toByteArray(), null)
    }

    fun interruptionBegan() {
        listener?.onAudioInterruptionBegan(this)
    }

    fun interruptionEnded() {
        listener?.onAudioInterruptionEnded(this)
    }
}

internal data class TranscriptEvent(val text: String, val role: VoiceTranscriptRole, val isFinal: Boolean)

internal class RecordingVoiceListener : VoiceSessionListener {
    val states = mutableListOf<VoiceSessionState>()
    val transcripts = mutableListOf<TranscriptEvent>()
    val callbackLoopers = mutableListOf<Looper?>()

    /** Runs after each recorded state, to model a host that reacts inside the callback. */
    var onState: ((VoiceSession, VoiceSessionState) -> Unit)? = null

    override fun onStateChanged(session: VoiceSession, state: VoiceSessionState) {
        callbackLoopers += Looper.myLooper()
        states += state
        onState?.invoke(session, state)
    }

    override fun onTranscriptUpdated(session: VoiceSession, text: String, role: VoiceTranscriptRole, isFinal: Boolean) {
        callbackLoopers += Looper.myLooper()
        transcripts += TranscriptEvent(text, role, isFinal)
    }
}
