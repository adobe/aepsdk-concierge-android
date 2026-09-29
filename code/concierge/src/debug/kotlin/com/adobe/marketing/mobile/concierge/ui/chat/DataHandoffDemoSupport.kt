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

import android.app.Application
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.json.JSONObject
import kotlin.random.Random

/**
 * Builds a [ConciergeChatViewModel] wired with [MockDataHandoffConversationService] so a demo can
 * drive [com.adobe.marketing.mobile.concierge.Concierge.sendDataHandoff] through the real
 * event dispatch -> forwarder -> ViewModel pipeline without reaching Brand Concierge.
 */
fun createMockDataHandoffChatViewModel(application: Application): ConciergeChatViewModel =
    ConciergeChatViewModel(
        application,
        AndroidSpeechCapturing(application),
        MockDataHandoffConversationService()
    )

/**
 * Routing hints the mock conversation service treats as failure simulations rather than forwarding
 * as routing data. They let the demo show what a host app's user is actually left looking at when a
 * handoff fails after it was accepted - the SDK renders no error UI for those turns, so only a
 * failing handoff makes that visible.
 */
object MockDataHandoffRoutingHints {
    /** The service streams a mid-stream error frame: rejected with `DELIVERY_FAILED`. */
    const val STREAM_ERROR = "demo-stream-error"

    /** The service completes with nothing renderable: rejected with `EMPTY_RESPONSE`. */
    const val EMPTY_RESPONSE = "demo-empty-response"

    /** The service accepts and then stays silent: rejected with `DELIVERY_TIMEOUT`. */
    const val SILENT = "demo-silent-service"
}

/**
 * Fake conversation service for the data handoff demo. Returns a canned, successful response for
 * [sendDataHandoff] after a random 1-3s delay (simulating real network latency without a backend),
 * unless the routing hint is one of [MockDataHandoffRoutingHints], in which case it simulates that
 * failure instead; [chat] and [sendFeedback] are minimally stubbed since the demo never exercises
 * them.
 */
private class MockDataHandoffConversationService : ConversationService {

    override fun sendDataHandoff(
        routingHint: String,
        xdmFields: Map<String, Any>
    ): Flow<ParsedConversationMessage> = when (routingHint) {
        MockDataHandoffRoutingHints.STREAM_ERROR -> flow {
            delay(NETWORK_LATENCY_FLOOR_MS)
            emit(
                ParsedConversationMessage(
                    messageContent = "mock service error detail",
                    state = ConversationState.ERROR,
                    conversationId = MOCK_CONVERSATION_ID
                )
            )
            // The SDK stops collecting on an error frame; hold the flow open so the demo exercises
            // that cancellation rather than a normal completion.
            awaitCancellation()
        }

        MockDataHandoffRoutingHints.EMPTY_RESPONSE -> flow {
            delay(NETWORK_LATENCY_FLOOR_MS)
            emit(
                ParsedConversationMessage(
                    messageContent = "",
                    state = ConversationState.COMPLETED,
                    conversationId = MOCK_CONVERSATION_ID
                )
            )
        }

        MockDataHandoffRoutingHints.SILENT -> flow {
            // Past the first-chunk cap, so the SDK fails the turn before this emission is reached.
            delay(ConciergeConstants.DataHandoff.FIRST_CHUNK_TIMEOUT_MS * 2)
            emit(
                ParsedConversationMessage(
                    messageContent = "unreachable - the first-chunk cap already failed this turn",
                    state = ConversationState.COMPLETED,
                    conversationId = MOCK_CONVERSATION_ID
                )
            )
        }

        else -> flow {
            delay(Random.nextLong(NETWORK_LATENCY_FLOOR_MS, 3001L))
            emit(
                ParsedConversationMessage(
                    messageContent = "Mocked response for routingHint=\"$routingHint\"\nxdmFields: " +
                        JSONObject(xdmFields).toString(2),
                    state = ConversationState.COMPLETED,
                    conversationId = MOCK_CONVERSATION_ID
                )
            )
        }
    }

    override fun chat(message: String): Flow<ParsedConversationMessage> = flow {
        emit(ParsedConversationMessage(messageContent = "", state = ConversationState.COMPLETED))
    }

    override suspend fun sendFeedback(feedback: Feedback): Boolean = true

    override fun cleanup() = Unit

    private companion object {
        const val MOCK_CONVERSATION_ID = "mock-data-handoff-conversation"
        const val NETWORK_LATENCY_FLOOR_MS = 1000L
    }
}
