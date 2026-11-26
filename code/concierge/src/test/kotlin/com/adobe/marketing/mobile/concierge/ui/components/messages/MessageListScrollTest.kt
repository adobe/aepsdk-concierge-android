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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageListScrollTest {

    @Test
    fun `without voice only a reply right after the latest user message fills`() {
        val replied = messages(true, false)
        assertTrue(shouldFillRemainingHeight(replied, 1, voiceScrollAnchorIndex = null))
        assertFalse(shouldFillRemainingHeight(replied, 0, voiceScrollAnchorIndex = null))

        assertFalse(shouldFillRemainingHeight(messages(true), 0, voiceScrollAnchorIndex = null))
        assertFalse(shouldFillRemainingHeight(messages(true, false, false), 2, voiceScrollAnchorIndex = null))
    }

    @Test
    fun `during voice the last message fills while only agent bubbles follow the anchor`() {
        // The user's words before the reply starts.
        assertTrue(shouldFillRemainingHeight(messages(false, true), 1, voiceScrollAnchorIndex = 1))
        // A reply in several bubbles.
        val reply = messages(true, false, false)
        assertTrue(shouldFillRemainingHeight(reply, 2, voiceScrollAnchorIndex = 0))
        assertFalse(shouldFillRemainingHeight(reply, 1, voiceScrollAnchorIndex = 0))
        // The agent speaking first.
        assertTrue(shouldFillRemainingHeight(messages(false, false), 1, voiceScrollAnchorIndex = 0))
        // A user bubble after the anchor.
        assertFalse(shouldFillRemainingHeight(messages(false, false, true), 2, voiceScrollAnchorIndex = 0))
    }

    @Test
    fun `scroll targets the voice anchor, each message once`() {
        val list = messages(true, false, false)
        assertEquals(2, scrollTargetIndex(list, voiceScrollAnchorIndex = 2, lastScrolledIndex = 0))
        assertNull(scrollTargetIndex(list, voiceScrollAnchorIndex = 2, lastScrolledIndex = 2))
        assertEquals(0, scrollTargetIndex(list, voiceScrollAnchorIndex = null, lastScrolledIndex = null))
    }

    @Test
    fun `ending voice doesn't jump back to an earlier user message, and a cleared list starts over`() {
        // The agent spoke first in voice (anchor 2); after the conversation the latest user is 0.
        val afterVoice = messages(true, false, false, false)
        assertNull(scrollTargetIndex(afterVoice, voiceScrollAnchorIndex = null, lastScrolledIndex = 2))

        assertEquals(0, scrollTargetIndex(messages(true), voiceScrollAnchorIndex = null, lastScrolledIndex = 2))
    }

    private fun messages(vararg isFromUser: Boolean) = isFromUser.mapIndexed { i, fromUser ->
        ChatMessage(content = MessageContent.Text("m$i"), isFromUser = fromUser, timestamp = i.toLong())
    }
}
