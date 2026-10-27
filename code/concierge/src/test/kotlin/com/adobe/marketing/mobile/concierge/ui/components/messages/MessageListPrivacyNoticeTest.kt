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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageListPrivacyNoticeTest {

    @Test
    fun `notice is placed at the first user authored basic text message`() {
        val messages = listOf(
            message("handoff", isFromUser = false),
            mixedMessage("non-basic user content", isFromUser = true),
            message("first query", isFromUser = true),
            message("later query", isFromUser = true)
        )

        assertEquals(2, privacyNoticeIndex(messages, PrivacyNoticeConfig(text = "Notice")))
    }

    @Test
    fun `no notice is placed without body text or a user query`() {
        val agentOnly = listOf(message("handoff", isFromUser = false))

        assertEquals(-1, privacyNoticeIndex(agentOnly, PrivacyNoticeConfig(text = " \n ")))
        assertEquals(-1, privacyNoticeIndex(agentOnly, PrivacyNoticeConfig(text = "Notice")))
        assertEquals(-1, privacyNoticeIndex(agentOnly, null))
    }

    private fun message(text: String, isFromUser: Boolean) = ChatMessage(
        content = MessageContent.Text(text),
        isFromUser = isFromUser,
        timestamp = 0L
    )

    private fun mixedMessage(text: String, isFromUser: Boolean) = ChatMessage(
        content = MessageContent.Mixed(text),
        isFromUser = isFromUser,
        timestamp = 0L
    )
}
