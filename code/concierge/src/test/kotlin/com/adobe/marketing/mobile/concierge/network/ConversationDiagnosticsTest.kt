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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDiagnosticsTest {

    private fun message(
        text: String,
        state: ConversationState = ConversationState.IN_PROGRESS,
        parts: List<ConversationPart>? = null
    ) = ParsedConversationMessage(
        messageContent = text,
        state = state,
        parts = parts ?: listOf(ConversationPart.Text(text))
    )

    @Test
    fun `first event reports the text as new`() {
        val diagnostics = ConversationDiagnostics()

        val line = diagnostics.describe(message("Hello"))

        assertTrue(line, line.contains("event=1"))
        assertTrue(line, line.contains("text=NEW"))
    }

    @Test
    fun `re-sent identical text is reported as a repeat`() {
        val diagnostics = ConversationDiagnostics()
        diagnostics.describe(message("Hello there"))

        val line = diagnostics.describe(message("Hello there"))

        assertTrue(line, line.contains("text=REPEAT"))
    }

    @Test
    fun `text that extends the previous text is reported as cumulative growth`() {
        val diagnostics = ConversationDiagnostics()
        diagnostics.describe(message("Hello"))

        val line = diagnostics.describe(message("Hello there"))

        assertTrue(line, line.contains("text=GROWTH"))
        assertTrue(line, line.contains("+6"))
    }

    @Test
    fun `text unrelated to the previous text is reported as diverged`() {
        val diagnostics = ConversationDiagnostics()
        diagnostics.describe(message("Here are some shoes"))

        val line = diagnostics.describe(message("What width do you need?"))

        assertTrue(line, line.contains("text=DIVERGED"))
    }

    @Test
    fun `describe lists part kinds in order`() {
        val diagnostics = ConversationDiagnostics()

        val line = diagnostics.describe(
            message(
                "Here are some shoes",
                parts = listOf(
                    ConversationPart.Text("Here are some shoes"),
                    ConversationPart.Card(MultimodalElement(id = "card-1")),
                    ConversationPart.Text("What width?"),
                    ConversationPart.Suggestions(listOf("Wide"))
                )
            )
        )

        assertTrue(line, line.contains("parts=[Text, Card, Text, Suggestions]"))
        assertTrue(line, line.contains("textParts=2"))
    }

    @Test
    fun `chunk splits oversized payloads to survive the logcat line limit`() {
        val payload = "x".repeat(7000)

        val chunks = ConversationDiagnostics.chunk(payload, maxChunkSize = 3000)

        assertEquals(3, chunks.size)
        assertTrue(chunks[0].startsWith("[1/3]"))
        assertTrue(chunks[2].startsWith("[3/3]"))
        assertEquals(payload, chunks.joinToString("") { it.substringAfter("] ") })
    }

    @Test
    fun `chunk returns a single unnumbered chunk when the payload fits`() {
        val chunks = ConversationDiagnostics.chunk("short", maxChunkSize = 3000)

        assertEquals(listOf("short"), chunks)
    }
}
