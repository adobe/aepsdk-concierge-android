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
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
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
 * Fake conversation service for the data handoff demo. Returns a canned, always-successful
 * response for [sendDataHandoff] after a random 1-3s delay (simulating real network latency
 * without a backend); [chat] and [sendFeedback] are minimally stubbed since the demo never
 * exercises them.
 */
private class MockDataHandoffConversationService : ConversationService {

    override fun sendDataHandoff(
        routingHint: String,
        xdmFields: Map<String, Any>
    ): Flow<ParsedConversationMessage> = flow {
        delay(Random.nextLong(1000L, 3001L))
        emit(
            ParsedConversationMessage(
                messageContent = "Mocked response for routingHint=\"$routingHint\"\nxdmFields: " +
                    JSONObject(xdmFields).toString(2),
                state = ConversationState.COMPLETED,
                conversationId = "mock-data-handoff-conversation"
            )
        )
    }

    override fun chat(message: String): Flow<ParsedConversationMessage> = flow {
        emit(ParsedConversationMessage(messageContent = "", state = ConversationState.COMPLETED))
    }

    override suspend fun sendFeedback(feedback: Feedback): Boolean = true

    override fun cleanup() = Unit
}
