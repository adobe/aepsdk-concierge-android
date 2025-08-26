package com.adobe.marketing.mobile.concierge.network

import androidx.annotation.VisibleForTesting
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.HttpConnecting
import com.adobe.marketing.mobile.services.Log
import java.io.BufferedReader
import java.io.IOException

/**
 * Represents different types of streaming events that can be received.
 */
internal sealed class StreamingEvent {
    data class Started(val connection: HttpConnecting) : StreamingEvent()
    data class DataReceived(val data: String) : StreamingEvent()
    data class EventReceived(val eventType: String, val data: String) : StreamingEvent()
    data class Error(val exception: Exception) : StreamingEvent()
    data class Closed(val reason: String?) : StreamingEvent()
}

internal class SSEParser {
    private companion object {
        private const val FIELD_DATA = "data"
        private const val FIELD_EVENT = "event"
        private const val FIELD_ID = "id"
        private const val FIELD_RETRY = "retry"
        private const val TAG = "SSEParser"
    }

    /**
     * Represents a parsed Server-Sent Event.
     */
    data class SSEEvent(
        val eventType: String? = null,
        val data: String = "",
        val id: String? = null,
        val retry: Int? = null
    ) {
        /**
         * Check if this event has a specific event type.
         * @return true if eventType is non-null and non-blank, false otherwise
         */
        fun hasEventType(): Boolean = !eventType.isNullOrBlank()
    }

    private var eventType: String? = null
    private val data = StringBuilder()
    private var id: String? = null
    private var retry: Int? = null
    private var isFirstLine: Boolean = true

    /**
     * Processes Server-Sent Events from a BufferedReader, emitting StreamingEvents as they're parsed.
     * This method encapsulates the entire SSE processing workflow.
     *
     * @param reader The BufferedReader containing SSE data
     * @param onEvent Suspend callback invoked for each parsed StreamingEvent
     * @throws IOException if there's an error reading from the stream
     */
    @Throws(IOException::class)
    suspend fun processEvents(
        reader: BufferedReader,
        onEvent: suspend (StreamingEvent) -> Unit
    ) {
        try {
            reader.useLines { lines ->
                lines.forEach { line ->
                    try {
                        val events = feed(line)
                        events.forEach { sseEvent ->
                            val streamingEvent = convertToStreamingEvent(sseEvent)
                            onEvent(streamingEvent)
                        }
                    } catch (e: Exception) {
                        Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "SSE feed error: ${e.message}")
                        // Continue processing other lines
                    }
                }
            }

            // Process any remaining event
            finish()?.let { lastEvent ->
                val streamingEvent = convertToStreamingEvent(lastEvent)
                onEvent(streamingEvent)
            }
        } catch (e: IOException) {
            Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "SSE processing error: ${e.message}")
            throw e
        }
    }

    /**
     * Feeds a single raw line (without line separators). Returns zero or more events
     * when a blank line delimiting an event is encountered.
     */
    @VisibleForTesting
    internal fun feed(rawLine: String): List<SSEEvent> {
        val line = normalizeLine(rawLine)

        if (line.isEmpty()) {
            val flushed = flushEvent()
            return if (flushed != null) listOf(flushed) else emptyList()
        }

        if (line[0] == ':') return emptyList()

        val idx = line.indexOf(':')
        val field: String
        val value: String
        if (idx == -1) {
            field = line
            value = ""
        } else {
            field = line.substring(0, idx).trim()
            val v = line.substring(idx + 1)
            value = if (v.startsWith(' ')) v.substring(1) else v
        }

        when (field) {
            FIELD_DATA -> with(data) {
                if (isNotEmpty()) append('\n')
                append(value)
            }
            FIELD_EVENT -> eventType = value
            FIELD_ID -> id = value
            FIELD_RETRY -> {
                val parsed = value.toIntOrNull()
                if (parsed == null) {
                    Log.warning(ConciergeConstants.EXTENSION_NAME, TAG, "Invalid retry value: $value")
                } else {
                    retry = parsed
                }
            }
            else -> {
                Log.trace(ConciergeConstants.EXTENSION_NAME, TAG, "Ignoring unknown SSE field: $field")
            }
        }

        return emptyList()
    }

    /** Flushes the current accumulated event, if any. */
    @VisibleForTesting
    internal fun finish(): SSEEvent? = flushEvent()

    private fun flushEvent(): SSEEvent? {
        return if (data.isNotEmpty() || eventType != null) {
            SSEEvent(eventType, data.toString(), id, retry).also {
                eventType = null
                data.setLength(0)
                id = null
                retry = null
            }
        } else null
    }
    
    /**
     * Converts a raw SSEEvent to a StreamingEvent with proper defaults.
     */
    private fun convertToStreamingEvent(sseEvent: SSEEvent): StreamingEvent {
        return when {
            sseEvent.hasEventType() -> StreamingEvent.EventReceived(
                sseEvent.eventType!!,
                sseEvent.data
            )
            else -> StreamingEvent.DataReceived(sseEvent.data)
        }
    }

    /**
     * Normalizes a single line: strips a trailing '\r' (for CRLF) and removes a leading BOM on
     * the very first line only. Does not trim spaces to preserve data content.
     */
    private fun normalizeLine(input: String): String {
        var s = if (input.isNotEmpty() && input.last() == '\r') input.dropLast(1) else input
        if (isFirstLine && s.isNotEmpty() && s[0] == '\uFEFF') {
            s = s.substring(1)
        }
        isFirstLine = false
        return s
    }
}

