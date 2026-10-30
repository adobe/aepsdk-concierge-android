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

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataChannelMessageParserTest {

    private fun parse(json: String) = DataChannelMessageParser.parse(JSONObject(json))

    private fun parseDelta(json: String) = (parse(json) as DataChannelMessage.TranscriptDelta).data

    private fun parseTurnDone(json: String) = (parse(json) as DataChannelMessage.TurnDone).data

    private fun parseNotice(json: String) = (parse(json) as DataChannelMessage.SessionNotice).data

    // Type-based: transcript_delta (user STT)

    @Test
    fun transcriptDelta_userTypeBased_parsesFields() {
        val delta = parseDelta(
            """{"type":"transcript_delta","data":{"role":"user","delta":"hello","sequence":2,"final":true,"turnId":"turn_1"}}"""
        )
        assertEquals(TranscriptDeltaData.Role.USER, delta.role)
        assertEquals("hello", delta.delta)
        assertEquals(2, delta.sequence)
        assertTrue(delta.final)
        assertEquals("turn_1", delta.turnId)
    }

    @Test
    fun transcriptDelta_assistantRole_typeBased() {
        val delta = parseDelta("""{"type":"transcript_delta","data":{"role":"assistant","delta":"hi"}}""")
        assertEquals(TranscriptDeltaData.Role.ASSISTANT, delta.role)
        assertEquals("missing sequence coerces to 0", 0, delta.sequence)
        assertFalse("missing final coerces to false", delta.final)
    }

    @Test
    fun transcriptDelta_missingData_returnsNull() {
        assertNull(parse("""{"type":"transcript_delta"}"""))
    }

    @Test
    fun transcriptDelta_emptyTurnId_becomesNull() {
        assertNull(parseDelta("""{"type":"transcript_delta","data":{"delta":"x","turnId":""}}""").turnId)
    }

    @Test
    fun transcriptDelta_nonStringValues_areNotCoerced() {
        val delta = parseDelta("""{"type":"transcript_delta","data":{"delta":42,"turnId":7,"final":"true"}}""")
        assertEquals("", delta.delta)
        assertNull(delta.turnId)
        assertFalse(delta.final)
    }

    // Event-based: assistant turn

    @Test
    fun textDelta_eventBased_isAssistantNonFinal() {
        val delta = parseDelta("""{"event":"text_delta","delta":"world","sequence":3,"turnId":"turn_9"}""")
        assertEquals(TranscriptDeltaData.Role.ASSISTANT, delta.role)
        assertEquals("world", delta.delta)
        assertEquals(3, delta.sequence)
        assertFalse(delta.final)
        assertEquals("turn_9", delta.turnId)
    }

    @Test
    fun turnCompleted_eventBased_parsesTurnDone() {
        val turn = parseTurnDone("""{"event":"turn_completed","fullText":"The answer.","state":"completed","turnId":"turn_9"}""")
        assertEquals(TurnDoneData("The answer.", TurnDoneData.TurnState.COMPLETED, "turn_9"), turn)
    }

    @Test
    fun turnCompleted_missingState_defaultsToCompleted() {
        assertEquals(TurnDoneData.TurnState.COMPLETED, parseTurnDone("""{"event":"turn_completed","fullText":"x"}""").state)
    }

    @Test
    fun turnCompleted_unknownState_defaultsToCompleted() {
        assertEquals(
            TurnDoneData.TurnState.COMPLETED,
            parseTurnDone("""{"event":"turn_completed","fullText":"x","state":"brand_new"}""").state
        )
    }

    @Test
    fun turnCompleted_interruptedState() {
        assertEquals(
            TurnDoneData.TurnState.INTERRUPTED,
            parseTurnDone("""{"event":"turn_completed","fullText":"x","state":"interrupted"}""").state
        )
    }

    @Test
    fun uiPayload_eventBased_parsesPromptSuggestions() {
        assertEquals(
            DataChannelMessage.UiPayload(UiPayloadData(listOf("a", "b"))),
            parse("""{"event":"ui_payload","promptSuggestions":["a","b"],"sources":[]}""")
        )
    }

    @Test
    fun uiPayload_nonStringSuggestion_dropsList() {
        assertEquals(
            DataChannelMessage.UiPayload(UiPayloadData(emptyList())),
            parse("""{"event":"ui_payload","promptSuggestions":["a",1]}""")
        )
    }

    @Test
    fun stateEvents_mapToStateUpdate() {
        mapOf(
            """{"event":"turn_started"}""" to "turn_started",
            """{"event":"state_update","state":"listening"}""" to "listening",
            """{"event":"state_update"}""" to "state_update",
            """{"event":"response_metadata"}""" to "response_metadata",
            """{"event":"tts_hint"}""" to "tts_hint",
            """{"type":"agent_state"}""" to "agent_state"
        ).forEach { (json, expected) ->
            assertEquals(json, DataChannelMessage.StateUpdate(expected), parse(json))
        }
    }

    @Test
    fun unknownEvent_returnsNull() {
        assertNull(parse("""{"event":"not_a_real_event"}"""))
    }

    // Session notice

    @Test
    fun sessionNotice_sessionEnded_knownReason() {
        assertEquals(
            SessionNoticeData.SessionEnded(EndReason.ROOM_EMPTY_TIMEOUT),
            parseNotice("""{"type":"session_notice","data":{"kind":"session_ended","reason":"room_empty_timeout"}}""")
        )
    }

    @Test
    fun sessionNotice_sessionEnded_unknownReason_isNull() {
        assertEquals(
            SessionNoticeData.SessionEnded(null),
            parseNotice("""{"type":"session_notice","data":{"kind":"session_ended","reason":"brand_new_reason"}}""")
        )
    }

    @Test
    fun sessionNotice_userSilenceWarning() {
        assertEquals(
            SessionNoticeData.UserSilenceWarning(12.0),
            parseNotice("""{"type":"session_notice","data":{"kind":"user_silence_warning","silenceSeconds":12}}""")
        )
    }

    @Test
    fun sessionNotice_unknownKind_returnsNull() {
        assertNull(parse("""{"type":"session_notice","data":{"kind":"something_else"}}"""))
    }

    // Fallbacks

    @Test
    fun noTypeOrEvent_returnsNull() {
        assertNull(parse("""{"foo":"bar"}"""))
    }

    @Test
    fun parseFromBytes_decodesJson() {
        val bytes = """{"event":"turn_completed","fullText":"done","state":"completed"}""".toByteArray()
        assertEquals("done", (DataChannelMessageParser.parse(bytes) as DataChannelMessage.TurnDone).data.fullText)
    }

    @Test
    fun parseFromBytes_nonJson_returnsNull() {
        assertNull(DataChannelMessageParser.parse(byteArrayOf(0x00, 0x01, 0x02)))
    }

    @Test
    fun parseFromBytes_jsonArray_returnsNull() {
        assertNull(DataChannelMessageParser.parse("[1,2]".toByteArray()))
    }
}
