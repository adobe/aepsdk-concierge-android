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

package com.adobe.marketing.mobile.concierge.network

import com.adobe.marketing.mobile.services.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Robolectric for Android's org.json, whose syntax errors quote the whole input.
@RunWith(RobolectricTestRunner::class)
class ConversationResponseParserVoiceTest {

    @Test
    fun `parseVoiceBootstrap returns credentials for a livekit session`() {
        val result = ConversationResponseParser.parseVoiceBootstrap(
            voiceEvent("""{"type": "livekit_session", "livekitUrl": "wss://lk.example.com", "token": "jwt"}""")
        )
        assertEquals(VoiceSessionBootstrap("wss://lk.example.com", "jwt"), result)
    }

    @Test
    fun `parseVoiceBootstrap ignores other voice payload types`() {
        assertNull(ConversationResponseParser.parseVoiceBootstrap(voiceEvent("""{"type": "done"}""")))
        assertNull(
            ConversationResponseParser.parseVoiceBootstrap(
                voiceEvent("""{"type": "other", "livekitUrl": "wss://lk.example.com", "token": "jwt"}""")
            )
        )
    }

    @Test
    fun `parseVoiceBootstrap requires both url and token`() {
        assertNull(ConversationResponseParser.parseVoiceBootstrap(voiceEvent("""{"type": "livekit_session", "token": "jwt"}""")))
        assertNull(
            ConversationResponseParser.parseVoiceBootstrap(
                voiceEvent("""{"type": "livekit_session", "livekitUrl": "wss://lk.example.com"}""")
            )
        )
        assertNull(
            ConversationResponseParser.parseVoiceBootstrap(
                voiceEvent("""{"type": "livekit_session", "livekitUrl": "", "token": "jwt"}""")
            )
        )
        assertNull(
            ConversationResponseParser.parseVoiceBootstrap(
                voiceEvent("""{"type": "livekit_session", "livekitUrl": 1, "token": "jwt"}""")
            )
        )
    }

    @Test
    fun `parseVoiceBootstrap returns null for text turns, other handles, blank, and invalid input`() {
        val textTurn = """
            {"handle": [{"type": "brand-concierge:conversation", "payload": [{"response": {"message": "Hi"}, "state": "completed"}]}]}
        """.trimIndent()
        val otherHandle = """
            {"handle": [{"type": "other", "payload": [{"response": {"voice": {"type": "livekit_session", "livekitUrl": "wss://x", "token": "t"}}}]}]}
        """.trimIndent()
        assertNull(ConversationResponseParser.parseVoiceBootstrap(textTurn))
        assertNull(ConversationResponseParser.parseVoiceBootstrap(otherHandle))
        assertNull(ConversationResponseParser.parseVoiceBootstrap("  "))
        assertNull(ConversationResponseParser.parseVoiceBootstrap("not json"))
    }

    @Test
    fun `parseVoiceBootstrap returns the first complete session across payloads`() {
        val json = """
            {"handle": [{"type": "brand-concierge:conversation", "payload": [
              {"response": {"voice": {"type": "livekit_session", "token": "partial"}}},
              {"response": {"voice": {"type": "livekit_session", "livekitUrl": "wss://first", "token": "t1"}}},
              {"response": {"voice": {"type": "livekit_session", "livekitUrl": "wss://second", "token": "t2"}}}
            ]}]}
        """.trimIndent()
        assertEquals(VoiceSessionBootstrap("wss://first", "t1"), ConversationResponseParser.parseVoiceBootstrap(json))
    }

    @Test
    fun `parseConversationData is unaffected by a voice payload`() {
        val json = """
            {"handle": [{"type": "brand-concierge:conversation", "payload": [
              {"response": {"message": "Hi", "voice": {"type": "livekit_session", "livekitUrl": "wss://x", "token": "t"}}, "state": "completed"}
            ]}]}
        """.trimIndent()
        val parsed = ConversationResponseParser.parseConversationData(json)
        assertEquals(1, parsed.size)
        assertEquals("Hi", parsed[0].messageContent)
        assertTrue(parsed[0].promptSuggestions.isEmpty())
    }

    @Test
    fun `parseVoiceBootstrap does not log the token of a malformed frame`() {
        val logged = mutableListOf<String>()
        mockkStatic(Log::class)
        try {
            every { Log.warning(any(), any(), any(), *anyVararg()) } answers {
                logged += args.drop(2).flatMap { (it as? Array<*>)?.toList() ?: listOf(it) }.joinToString()
            }
            val truncated = voiceEvent("""{"type": "livekit_session", "livekitUrl": "wss://x", "token": "secret-token"}""")
                .dropLast(3)

            assertNull(ConversationResponseParser.parseVoiceBootstrap(truncated))

            assertEquals(1, logged.size)
            assertFalse(logged.single(), logged.single().contains("secret-token"))
        } finally {
            unmockkStatic(Log::class)
        }
    }

    private fun voiceEvent(voice: String): String = """
        {"handle": [{"type": "brand-concierge:conversation", "payload": [{"response": {"voice": $voice}}]}]}
    """.trimIndent()
}
