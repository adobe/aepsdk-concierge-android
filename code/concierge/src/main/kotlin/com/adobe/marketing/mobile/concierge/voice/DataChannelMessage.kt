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

/**
 * Kotlin mirror of the web client's data-channel message contract. The worker on the other end is
 * the same backend process regardless of client platform, so wire names are copied verbatim and
 * must not drift.
 *
 * The worker sends type-based messages (user STT `transcript_delta`, `session_notice`) and
 * event-based messages (assistant `text_delta`, `turn_completed`, ...), which
 * [DataChannelMessageParser] normalizes into these cases.
 */
internal sealed class DataChannelMessage {
    data class TranscriptDelta(val data: TranscriptDeltaData) : DataChannelMessage()
    data class TurnDone(val data: TurnDoneData) : DataChannelMessage()
    data class UiPayload(val data: UiPayloadData) : DataChannelMessage()
    data class SessionNotice(val data: SessionNoticeData) : DataChannelMessage()

    /** A coarse worker state signal (`turn_started`, `state_update`, `response_metadata`, `tts_hint`). */
    data class StateUpdate(val state: String) : DataChannelMessage()
}

/**
 * An incremental transcript chunk. User chunks are cumulative STT snapshots; assistant chunks are
 * increments normalized from the worker's `text_delta` events.
 */
internal data class TranscriptDeltaData(
    val role: Role,
    val turnId: String?,
    val delta: String,
    val sequence: Int,
    /** Last chunk of the turn's transcript. Assistant `text_delta` chunks are always non-final. */
    val final: Boolean
) {
    enum class Role { USER, ASSISTANT }
}

/** The completed assistant turn (normalized from `turn_completed`). */
internal data class TurnDoneData(
    val fullText: String,
    val state: TurnState,
    val turnId: String?
) {
    enum class TurnState(val wireValue: String) {
        COMPLETED("completed"),
        INTERRUPTED("interrupted"),
        ERROR("error");

        companion object {
            fun fromWire(value: String): TurnState? = values().firstOrNull { it.wireValue == value }
        }
    }
}

/** Rich turn content. Parsed for contract completeness; only `promptSuggestions` is typed. */
internal data class UiPayloadData(val promptSuggestions: List<String>)

/**
 * The worker's terminal reason for a session, validated against the known set. An unknown value
 * from a newer worker parses to `null` rather than a falsely narrowed value.
 */
internal enum class EndReason(val wireValue: String) {
    ROOM_EMPTY_TIMEOUT("room_empty_timeout"),
    USER_JOIN_TIMEOUT("user_join_timeout"),
    WORKER_SHUTDOWN("worker_shutdown"),
    USER_INACTIVITY_TIMEOUT("user_inactivity_timeout"),
    ERROR("error");

    companion object {
        fun fromWire(value: String): EndReason? = values().firstOrNull { it.wireValue == value }
    }
}

/** A worker session notice. An unrecognized `kind` parses to `null` so the caller drops it. */
internal sealed class SessionNoticeData {
    data class UserSilenceWarning(val silenceSeconds: Double) : SessionNoticeData()
    data class SessionEnded(val reason: EndReason?) : SessionNoticeData()
}
