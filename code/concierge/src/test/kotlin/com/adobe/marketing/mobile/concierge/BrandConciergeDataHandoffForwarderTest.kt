/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
class BrandConciergeDataHandoffForwarderTest {

    @Test
    fun `forward sends the routing hint and XDM fields to the conversation service`() = runTest {
        val service = RecordingConversationService()
        val forwarder = BrandConciergeDataHandoffForwarder(service, this)
        val handoff = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123")
        )

        forwarder.forward(handoff)
        advanceUntilIdle()

        assertEquals("buy_now", service.routingHint)
        assertEquals(mapOf("orderId" to "abc-123"), service.xdmFields)
    }

    @Test
    fun `forward absorbs an asynchronous conversation service failure`() = runTest {
        val service = FailingConversationService()
        val forwarder = BrandConciergeDataHandoffForwarder(service, this)

        forwarder.forward(
            ConciergeDataHandoffEvent(
                routingHint = "buy_now",
                xdmFields = mapOf("orderId" to "abc-123")
            )
        )
        advanceUntilIdle()

        assertTrue(service.sendDataHandoffCalled)
    }

    private class RecordingConversationService : ConversationService {
        var routingHint: String? = null
        var xdmFields: Map<String, Any>? = null

        override fun chat(message: String): Flow<ParsedConversationMessage> = emptyFlow()

        override fun sendDataHandoff(
            routingHint: String,
            xdmFields: Map<String, Any>
        ): Flow<ParsedConversationMessage> {
            this.routingHint = routingHint
            this.xdmFields = xdmFields
            return emptyFlow()
        }

        override suspend fun sendFeedback(feedback: Feedback): Boolean = true

        override fun cleanup() = Unit
    }

    private class FailingConversationService : ConversationService {
        var sendDataHandoffCalled = false

        override fun chat(message: String): Flow<ParsedConversationMessage> = emptyFlow()

        override fun sendDataHandoff(
            routingHint: String,
            xdmFields: Map<String, Any>
        ): Flow<ParsedConversationMessage> {
            sendDataHandoffCalled = true
            return flow { throw IOException("network failure") }
        }

        override suspend fun sendFeedback(feedback: Feedback): Boolean = true

        override fun cleanup() = Unit
    }
}
