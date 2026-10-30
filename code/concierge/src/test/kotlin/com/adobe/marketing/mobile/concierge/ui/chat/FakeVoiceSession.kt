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

package com.adobe.marketing.mobile.concierge.ui.chat

import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.VoiceSessionBootstrap
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.voice.VoiceSession
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionListener
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionState
import com.adobe.marketing.mobile.concierge.voice.VoiceTranscriptRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * Stands in for the session factory: records each session Brand Concierge creates, with the
 * transport it was given, and lets a test drive the session's callbacks without LiveKit or audio.
 */
internal class FakeVoiceSessionFactory {
    val sessions = mutableListOf<FakeVoiceSession>()

    /** Runs inside [VoiceSession.start] before it returns, to model a session that calls back synchronously. */
    var onStart: ((FakeVoiceSession) -> Unit)? = null

    val lastSession: FakeVoiceSession?
        get() = sessions.lastOrNull()

    /** The url and token of every session that was started. */
    val connections: List<Pair<String, String>>
        get() = sessions.mapNotNull { it.connection }

    fun create(transport: VoiceTransport, listener: VoiceSessionListener): VoiceSession =
        FakeVoiceSession(transport, listener) { onStart?.invoke(it) }.also { sessions.add(it) }
}

/**
 * A session that follows the contract: starts [VoiceSessionState.Connecting], reports on the
 * calling thread, and [stop] is idempotent and reports [VoiceSessionState.Ended] synchronously.
 */
internal class FakeVoiceSession(
    val transport: VoiceTransport,
    private val listener: VoiceSessionListener,
    private val onStart: (FakeVoiceSession) -> Unit
) : VoiceSession {
    override var state: VoiceSessionState = VoiceSessionState.Connecting
        private set
    var connection: Pair<String, String>? = null
        private set
    var stopCallCount = 0
        private set

    override fun start(url: String, token: String) {
        connection = url to token
        onStart(this)
    }

    override fun stop() {
        stopCallCount++
        report(VoiceSessionState.Ended)
    }

    /** Moves to [newState] and reports it, unless the session already ended or failed. */
    fun report(newState: VoiceSessionState) {
        if (state.isTerminal) return
        state = newState
        listener.onStateChanged(this, newState)
    }

    /**
     * Calls the listener with [newState] regardless of the session's own state, to model a late
     * or misbehaving callback that the host must drop on its own.
     */
    fun deliverUnchecked(newState: VoiceSessionState) {
        listener.onStateChanged(this, newState)
    }

    fun transcript(text: String, role: VoiceTranscriptRole, isFinal: Boolean = false) {
        listener.onTranscriptUpdated(this, text, role, isFinal)
    }
}

/**
 * [ConversationService] whose voice bootstrap is driven by the test: each call opens a stream the
 * test can [emitBootstrap] into, [completeBootstrap], or [failBootstrap].
 */
internal class FakeVoiceConversationService : ConversationService {
    private var bootstrap = Channel<VoiceSessionBootstrap>(Channel.UNLIMITED)

    val bootstrapSessionIds = mutableListOf<String?>()
    val chatMessages = mutableListOf<String>()

    /** What a chat turn streams back; completes empty unless a test needs a turn to stay open. */
    var chatResponse: Flow<ParsedConversationMessage> = emptyFlow()
    var activeBootstrapCollectors = 0
        private set
    var cancelledBootstraps = 0
        private set

    override fun chat(
        message: String,
        xdmFields: Map<String, Any>,
        sessionId: String?
    ): Flow<ParsedConversationMessage> {
        chatMessages.add(message)
        return chatResponse
    }

    override fun sendDataHandoff(
        routingHint: String,
        xdmFields: Map<String, Any>,
        sessionId: String?
    ): Flow<ParsedConversationMessage> = emptyFlow()

    override suspend fun sendFeedback(feedback: Feedback): Boolean = true

    override fun bootstrapVoiceSession(sessionId: String?): Flow<VoiceSessionBootstrap> {
        bootstrapSessionIds.add(sessionId)
        val channel = Channel<VoiceSessionBootstrap>(Channel.UNLIMITED).also { bootstrap = it }
        return flow {
            activeBootstrapCollectors++
            try {
                for (value in channel) emit(value)
            } catch (e: CancellationException) {
                cancelledBootstraps++
                throw e
            } finally {
                activeBootstrapCollectors--
            }
        }
    }

    fun emitBootstrap(url: String = "wss://voice.example.com", token: String = "token-1") {
        bootstrap.trySend(VoiceSessionBootstrap(url, token))
    }

    fun completeBootstrap() {
        bootstrap.close()
    }

    fun failBootstrap(error: Throwable) {
        bootstrap.close(error)
    }

    override fun cleanup() = Unit
}
