/*
  Copyright 2025 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SSEParserTest {
    @Test
    fun `processEvents emits DataReceived then EventReceived in order`() = runTest {
        val input = buildString {
            appendLine(": comment to ignore")
            appendLine("data: hello")
            appendLine("data: world")
            appendLine("") // end first event
            appendLine("event: delta")
            appendLine("data: chunk1")
            appendLine("") // end typed event
            appendLine("") // end with extra blank line
        }

        val reader = BufferedReader(StringReader(input))
        val parser = SSEParser()

        val collected = mutableListOf<StreamingEvent>()
        parser.processEvents(reader) { event ->
            collected.add(event)
        }

        // We expect 2 emissions:
        // DataReceived for first (hello\nworld),
        // EventReceived for second (delta, chunk1)
        assertEquals(2, collected.size)

        val first = collected[0]
        assertTrue(first is StreamingEvent.DataReceived)
        assertEquals("hello\nworld", (first as StreamingEvent.DataReceived).data)

        val second = collected[1]
        assertTrue(second is StreamingEvent.EventReceived)
        val ev = second as StreamingEvent.EventReceived
        assertEquals("delta", ev.eventType)
        assertEquals("chunk1", ev.data)
    }

    @Test(expected = IOException::class)
    fun `processEvents propagates IOException thrown by reader`() = runTest {
        val throwingReader = BufferedReader(object : Reader() {
            override fun read(cbuf: CharArray, off: Int, len: Int): Int {
                throw IOException("boom from reader")
            }
            override fun close() { /* no-op */ }
        })

        val parser = SSEParser()

        parser.processEvents(throwingReader) {
            throw AssertionError("onEvent should not be called when reader throws")
        }
    }

    @Test
    fun `processEvents emits final event without trailing delimiter`() = runTest {
        val input = "data: tail only" // no trailing blank line
        val reader = BufferedReader(StringReader(input))
        val parser = SSEParser()

        val collected = mutableListOf<StreamingEvent>()
        parser.processEvents(reader) { event ->
            collected.add(event)
        }

        assertEquals(1, collected.size)
        val first = collected[0]
        assertTrue(first is StreamingEvent.DataReceived)
        assertEquals("tail only", (first as StreamingEvent.DataReceived).data)
    }

    @Test
    fun `processEvents with only comments and blanks emits nothing`() = runTest {
        val input = buildString {
            appendLine(": comment")
            appendLine("")
            appendLine(": another")
            appendLine("")
        }

        val reader = BufferedReader(StringReader(input))
        val parser = SSEParser()

        val collected = mutableListOf<StreamingEvent>()
        parser.processEvents(reader) { event ->
            collected.add(event)
        }

        assertTrue(collected.isEmpty())
    }

    @Test
    fun `processEvents emits typed event with empty data`() = runTest {
        val input = buildString {
            appendLine("event: ping")
            appendLine("")
        }

        val reader = BufferedReader(StringReader(input))
        val parser = SSEParser()

        val collected = mutableListOf<StreamingEvent>()
        parser.processEvents(reader) { event ->
            collected.add(event)
        }

        assertEquals(1, collected.size)
        val only = collected.first()
        assertTrue(only is StreamingEvent.EventReceived)
        val ev = only as StreamingEvent.EventReceived
        assertEquals("ping", ev.eventType)
        assertEquals("", ev.data)
    }

    @Test
    fun `feed + blank line = flushes one event with concatenated data`() {
        val parser = SSEParser()

        // Simulate two data lines and a blank line delimiter
        val events1 = parser.feed("data: first line")
        val events2 = parser.feed("data: second line")
        val events3 = parser.feed("") // flush marker

        assertTrue(events1.isEmpty())
        assertTrue(events2.isEmpty())
        assertEquals(1, events3.size)

        val event = events3.first()
        assertEquals(null, event.eventType)
        assertEquals("first line\nsecond line", event.data)
    }

    @Test
    fun `unknown field is ignored and no event is flushed`() {
        val parser = SSEParser()
        parser.feed("foo: bar")
        val events = parser.feed("")
        assertTrue(events.isEmpty())
    }

    @Test
    fun `retry invalid value is ignored without crash`() {
        val parser = SSEParser()
        parser.feed("retry: not-a-number")
        parser.feed("data: hi")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("hi", event.data)
        assertEquals(null, event.retry)
    }

    @Test
    fun `retry valid value is parsed when event flushes`() {
        val parser = SSEParser()
        parser.feed("retry: 3000")
        parser.feed("data: hi")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("hi", event.data)
        assertEquals(3000, event.retry)
    }

    @Test
    fun `id field is captured`() {
        val parser = SSEParser()
        parser.feed("id: 42")
        parser.feed("data: foo")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("42", event.id)
        assertEquals("foo", event.data)
    }

    @Test
    fun `CRLF is stripped from line endings`() {
        val parser = SSEParser()
        parser.feed("data: hi\r")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("hi", event.data)
    }

    @Test
    fun `BOM is ignored only on first line`() {
        val parser = SSEParser()
        parser.feed("\uFEFFdata: hello")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("hello", event.data)
    }

    @Test
    fun `finish() returns null when no pending event`() {
        val parser = SSEParser()
        val last = parser.finish()
        assertEquals(null, last)
    }

    @Test
    fun `eventType resets after flush`() {
        val parser = SSEParser()
        parser.feed("event: delta")
        parser.feed("data: one")
        val first = parser.feed("")
        assertEquals(1, first.size)
        assertEquals("delta", first.first().eventType)

        parser.feed("data: two")
        val second = parser.feed("")
        assertEquals(1, second.size)
        assertEquals(null, second.first().eventType)
        assertEquals("two", second.first().data)
    }
    @Test
    fun `event field sets eventType and data is captured`() {
        val parser = SSEParser()
        parser.feed("event: message")
        parser.feed("data: hello")
        val events = parser.feed("")

        assertEquals(1, events.size)
        val event = events.first()
        assertEquals("message", event.eventType)
        assertEquals("hello", event.data)
    }

    @Test
    fun `finish() flushes trailing event without delimiter`() {
        val parser = SSEParser()
        parser.feed("data: partial")
        val last = parser.finish()
        requireNotNull(last)
        assertEquals("partial", last.data)
        assertEquals(null, last.eventType)
    }
}
