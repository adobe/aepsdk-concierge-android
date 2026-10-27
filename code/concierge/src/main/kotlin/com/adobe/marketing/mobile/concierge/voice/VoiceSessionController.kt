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
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.VoiceTransportListener
import com.adobe.marketing.mobile.concierge.VoiceTransportState
import com.adobe.marketing.mobile.services.Log

/**
 * One voice conversation over the app's [VoiceTransport]: the microphone permission check, the
 * connection lifecycle, audio interruptions, and turning the voice agent's data messages into
 * transcript updates.
 *
 * The transport is shared by every conversation in the app, so a session claims it (becomes its
 * listener) only for the time it is connected, and releases it before disconnecting. Callbacks
 * from any other transport, or after the release, are dropped. Lifecycle and transcript work runs
 * on the main thread; [state] may be read, and [stop] called, from any thread.
 *
 * The listener is held until the session reaches a terminal state and is then released, so a
 * host that forgets the session after [stop] does not leak its UI.
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
     * The transport has no microphone getter: the microphone is live once connected, and only this
     * session changes it after that.
     */
    private var isMicrophoneEnabled = false
    private var audioInterrupted = false
    private var micWasEnabledBeforeInterruption = false

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
                if (!ownsCallback(transport) || currentState != VoiceSessionState.Listening || audioInterrupted) {
                    return@onMain
                }
                audioInterrupted = true
                micWasEnabledBeforeInterruption = isMicrophoneEnabled
                if (micWasEnabledBeforeInterruption) setMicrophoneEnabled(false)
            }
        }

        override fun onAudioInterruptionEnded(transport: VoiceTransport) {
            onMain {
                if (!ownsCallback(transport) || !audioInterrupted) return@onMain
                audioInterrupted = false
                if (micWasEnabledBeforeInterruption) setMicrophoneEnabled(true)
                micWasEnabledBeforeInterruption = false
            }
        }
    }

    override val state: VoiceSessionState
        get() = currentState

    private val isActive: Boolean
        get() = !currentState.isTerminal

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
        try {
            transport.connect(url, token)
        } catch (e: Exception) {
            fail(e.message ?: FailureReasons.CONNECT_FAILED)
        }
    }

    /** True when a transport callback belongs to this session's live connection. */
    private fun ownsCallback(sender: VoiceTransport): Boolean =
        sender === transport && holdsTransport && isActive

    private fun handleTransportState(transportState: VoiceTransportState) {
        when (transportState) {
            is VoiceTransportState.Connected -> {
                if (currentState != VoiceSessionState.Connecting) return
                isMicrophoneEnabled = true
                setState(VoiceSessionState.Listening)
            }
            is VoiceTransportState.Disconnected -> finish(VoiceSessionState.Ended)
            is VoiceTransportState.Failed ->
                fail(transportState.error.message ?: FailureReasons.CONNECT_FAILED)
        }
    }

    private fun setMicrophoneEnabled(enabled: Boolean) {
        if (!holdsTransport || !isActive) return
        isMicrophoneEnabled = enabled
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
                if (message.data is SessionNoticeData.SessionEnded) stop()
            }
            is DataChannelMessage.UiPayload, is DataChannelMessage.StateUpdate -> Unit
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

    private companion object {
        const val SELF_TAG = "VoiceSessionController"
        val SUPPORTED_SCHEMES = setOf("ws", "wss", "http", "https")

        fun isValidServerUrl(url: String): Boolean {
            val parsed = Uri.parse(url)
            val scheme = parsed.scheme?.lowercase() ?: return false
            return scheme in SUPPORTED_SCHEMES && !parsed.host.isNullOrEmpty()
        }
    }
}
