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

package com.adobe.marketing.mobile.concierge.network

/**
 * Describes the shape of an incoming conversation stream, one line per payload.
 *
 * The interesting question when a response carries several text parts is how each payload's text
 * relates to the one before it: the backend may re-send the whole message every time, extend it,
 * or send a genuinely different string. [TextRelation] names that relationship so a log can be
 * read without diffing long message bodies by hand.
 *
 * One instance tracks one conversation turn; it holds the previous message so it can classify the
 * next one, and is not safe to share across concurrent streams.
 */
internal class ConversationDiagnostics {

    private var previousMessage: String? = null
    private var eventIndex = 0

    /**
     * How a payload's message text relates to the previous payload's text.
     */
    private enum class TextRelation {
        /** Nothing preceded it. */
        NEW,

        /** Byte-identical to the previous payload — the backend re-sent the same text. */
        REPEAT,

        /** Starts with the previous text and adds to it — a cumulative snapshot. */
        GROWTH,

        /** The previous text starts with this one — the message got shorter. */
        TRUNCATED,

        /** Unrelated to the previous text — a separate text part rather than a continuation. */
        DIVERGED,

        /** No text in this payload. */
        EMPTY
    }

    /**
     * Builds a one-line summary of a parsed payload. Call once per payload, in arrival order.
     */
    fun describe(parsed: ParsedConversationMessage): String {
        eventIndex++
        val message = parsed.messageContent
        val relation = classify(message)
        val delta = when (relation) {
            TextRelation.GROWTH -> " +${message.length - (previousMessage?.length ?: 0)}"
            TextRelation.TRUNCATED -> " -${(previousMessage?.length ?: 0) - message.length}"
            else -> ""
        }

        val kinds = parsed.parts.joinToString(", ") { part ->
            when (part) {
                is ConversationPart.Text -> "Text"
                is ConversationPart.Card -> "Card"
                is ConversationPart.Cta -> "Cta"
                is ConversationPart.Suggestions -> "Suggestions"
            }
        }

        if (message.isNotEmpty()) previousMessage = message

        return "event=$eventIndex state=${parsed.state} text=$relation$delta " +
            "chars=${message.length} parts=[$kinds] " +
            "textParts=${parsed.parts.count { it is ConversationPart.Text }} " +
            "cards=${parsed.parts.count { it is ConversationPart.Card }} " +
            "ctas=${parsed.parts.count { it is ConversationPart.Cta }} " +
            "suggestions=${parsed.promptSuggestions.size} " +
            "interactionId=${parsed.interactionId ?: "-"}"
    }

    private fun classify(message: String): TextRelation {
        val previous = previousMessage
        return when {
            message.isEmpty() -> TextRelation.EMPTY
            previous == null -> TextRelation.NEW
            message == previous -> TextRelation.REPEAT
            message.startsWith(previous) -> TextRelation.GROWTH
            previous.startsWith(message) -> TextRelation.TRUNCATED
            else -> TextRelation.DIVERGED
        }
    }

    companion object {
        /**
         * Android's log buffer drops anything past roughly 4KB on a single line, which silently
         * truncates raw payloads mid-JSON. Splitting into numbered chunks keeps the whole payload
         * readable and makes it obvious when pieces are missing.
         */
        fun chunk(value: String, maxChunkSize: Int = 3000): List<String> {
            if (value.length <= maxChunkSize) return listOf(value)
            val total = (value.length + maxChunkSize - 1) / maxChunkSize
            return (0 until total).map { index ->
                val start = index * maxChunkSize
                val end = minOf(start + maxChunkSize, value.length)
                "[${index + 1}/$total] ${value.substring(start, end)}"
            }
        }
    }
}
