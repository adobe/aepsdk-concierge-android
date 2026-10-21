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

import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.VoiceSessionBootstrap
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.plugin.IVoicePlugin
import com.adobe.marketing.mobile.plugin.IVoiceSession
import com.adobe.marketing.mobile.plugin.IVoiceSessionListener
import com.adobe.marketing.mobile.plugin.VoiceSessionState
import com.adobe.marketing.mobile.plugin.VoiceTranscriptRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow

/**
 * In-memory [IVoicePlugin] for tests: records what Brand Concierge asks for and lets a test drive
 * each session's callbacks, without LiveKit or audio.
 */
internal class FakeVoicePlugin : IVoicePlugin {
    val connections = mutableListOf<Map<String, String>>()
    val sessions = mutableListOf<FakeVoiceSession>()

    /** Runs inside [startSession] before it returns, to model a plugin that calls back synchronously. */
    var onStartSession: ((FakeVoiceSession) -> Unit)? = null

    val lastSession: FakeVoiceSession?
        get() = sessions.lastOrNull()

    override fun startSession(connection: Map<String, String>, listener: IVoiceSessionListener): IVoiceSession {
        connections.add(connection)
        val session = FakeVoiceSession(listener)
        sessions.add(session)
        onStartSession?.invoke(session)
        return session
    }
}

/**
 * A session that follows the contract: starts [VoiceSessionState.Connecting], reports on the
 * calling thread, and [stop] is idempotent and reports [VoiceSessionState.Ended] synchronously.
 */
internal class FakeVoiceSession(private val listener: IVoiceSessionListener) : IVoiceSession {
    override var state: VoiceSessionState = VoiceSessionState.Connecting
        private set
    var stopCallCount = 0
        private set

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
