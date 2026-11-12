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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Parses a raw data-channel JSON frame into a typed [DataChannelMessage], mirroring the web
 * client's `parseDataChannelMessage`. Handles both on-wire formats:
 *
 * - Type-based (user STT / notices): `{ "type": "transcript_delta", "data": { ... } }`
 * - Event-based (assistant turn messages): `{ "event": "turn_completed", "fullText": "..." }`
 *
 * Returns `null` for a message that matches no known format so callers drop it rather than act
 * on a malformed shape. Values are type-checked rather than coerced (a numeric `delta` is not
 * turned into a string).
 */
internal object DataChannelMessageParser {

    fun parse(data: ByteArray): DataChannelMessage? {
        val raw = try {
            JSONObject(String(data, Charsets.UTF_8))
        } catch (e: JSONException) {
            return null
        }
        return parse(raw)
    }

    fun parse(raw: JSONObject): DataChannelMessage? {
        (raw.opt("type") as? String)?.let { return buildTypeMessage(it, raw) }
        (raw.opt("event") as? String)?.let { return buildEventMessage(it, raw) }
        return null
    }

    private fun buildTypeMessage(type: String, raw: JSONObject): DataChannelMessage? =
        when (type) {
            "transcript_delta" -> {
                val payload = raw.opt("data") as? JSONObject
                payload?.let {
                    DataChannelMessage.TranscriptDelta(
                        TranscriptDeltaData(
                            role = if (it.opt("role") == "assistant") {
                                TranscriptDeltaData.Role.ASSISTANT
                            } else {
                                TranscriptDeltaData.Role.USER
                            },
                            turnId = asTurnId(it.opt("turnId")),
                            delta = asString(it.opt("delta")),
                            sequence = asInt(it.opt("sequence")),
                            final = it.opt("final") == true
                        )
                    )
                }
            }
            "session_notice" -> parseSessionNotice(raw.opt("data"))?.let {
                DataChannelMessage.SessionNotice(it)
            }
            // Other type-based messages (e.g. state carriers) surface as a coarse state update.
            else -> DataChannelMessage.StateUpdate(type)
        }

    private fun buildEventMessage(event: String, raw: JSONObject): DataChannelMessage? =
        when (event) {
            // Assistant transcript chunk; completion arrives via turn_completed.
            "text_delta" -> DataChannelMessage.TranscriptDelta(
                TranscriptDeltaData(
                    role = TranscriptDeltaData.Role.ASSISTANT,
                    turnId = asTurnId(raw.opt("turnId")),
                    delta = asString(raw.opt("delta")),
                    sequence = asInt(raw.opt("sequence")),
                    final = false
                )
            )
            "turn_completed" -> DataChannelMessage.TurnDone(
                TurnDoneData(
                    fullText = asString(raw.opt("fullText")),
                    state = TurnDoneData.TurnState.fromWire(asString(raw.opt("state"), "completed"))
                        ?: TurnDoneData.TurnState.COMPLETED,
                    turnId = asTurnId(raw.opt("turnId"))
                )
            )
            "ui_payload" -> DataChannelMessage.UiPayload(
                UiPayloadData(asStringList(raw.opt("promptSuggestions")))
            )
            "turn_started" -> DataChannelMessage.StateUpdate("turn_started")
            "state_update" -> DataChannelMessage.StateUpdate(asString(raw.opt("state"), "state_update"))
            "response_metadata", "tts_hint" -> DataChannelMessage.StateUpdate(event)
            else -> null
        }

    private fun parseSessionNotice(raw: Any?): SessionNoticeData? {
        val data = raw as? JSONObject ?: return null
        return when (data.opt("kind") as? String) {
            "user_silence_warning" -> SessionNoticeData.UserSilenceWarning(asDouble(data.opt("silenceSeconds")))
            "session_ended" -> SessionNoticeData.SessionEnded(
                (data.opt("reason") as? String)?.let { EndReason.fromWire(it) }
            )
            else -> null
        }
    }

    private fun asString(value: Any?, fallback: String = ""): String = value as? String ?: fallback

    /** Anything that is not a non-empty string becomes null; an empty id would match across turns. */
    private fun asTurnId(value: Any?): String? = (value as? String)?.takeIf { it.isNotEmpty() }

    private fun asInt(value: Any?, fallback: Int = 0): Int = (value as? Number)?.toInt() ?: fallback

    private fun asDouble(value: Any?, fallback: Double = 0.0): Double =
        (value as? Number)?.toDouble() ?: fallback

    /** The whole list is dropped when any element is not a string, matching the other clients. */
    private fun asStringList(value: Any?): List<String> {
        val array = value as? JSONArray ?: return emptyList()
        val result = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val item = array.opt(i) as? String ?: return emptyList()
            result.add(item)
        }
        return result
    }
}
