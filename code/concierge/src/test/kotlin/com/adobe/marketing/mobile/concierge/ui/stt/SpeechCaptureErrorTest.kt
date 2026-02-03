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

package com.adobe.marketing.mobile.concierge.ui.stt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechCaptureErrorTest {

    // ========== NoMatch Tests ==========

    @Test
    fun `NoMatch is singleton object`() {
        val error1 = SpeechCaptureError.NoMatch
        val error2 = SpeechCaptureError.NoMatch
        
        assertTrue(error1 === error2)
    }

    @Test
    fun `NoMatch is instance of SpeechCaptureError`() {
        val error = SpeechCaptureError.NoMatch
        
        assertTrue(error is SpeechCaptureError)
    }

    @Test
    fun `NoMatch equals itself`() {
        assertEquals(SpeechCaptureError.NoMatch, SpeechCaptureError.NoMatch)
    }

    @Test
    fun `NoMatch is different from other error types`() {
        assertNotEquals(SpeechCaptureError.NoMatch, SpeechCaptureError.Client())
        assertNotEquals(SpeechCaptureError.NoMatch, SpeechCaptureError.Permission())
        assertNotEquals(SpeechCaptureError.NoMatch, SpeechCaptureError.Network())
        assertNotEquals(SpeechCaptureError.NoMatch, SpeechCaptureError.Unknown(0))
    }

    // ========== Client Error Tests ==========

    @Test
    fun `Client creates with null cause by default`() {
        val error = SpeechCaptureError.Client()
        
        assertNull(error.cause)
    }

    @Test
    fun `Client creates with custom cause`() {
        val exception = IllegalStateException("Invalid state")
        val error = SpeechCaptureError.Client(exception)
        
        assertEquals(exception, error.cause)
    }

    @Test
    fun `Client is instance of SpeechCaptureError`() {
        val error = SpeechCaptureError.Client()
        
        assertTrue(error is SpeechCaptureError)
    }

    @Test
    fun `Client with same cause are equal`() {
        val exception = RuntimeException("test")
        val error1 = SpeechCaptureError.Client(exception)
        val error2 = SpeechCaptureError.Client(exception)
        
        assertEquals(error1, error2)
    }

    @Test
    fun `Client with different causes are not equal`() {
        val error1 = SpeechCaptureError.Client(RuntimeException("error1"))
        val error2 = SpeechCaptureError.Client(RuntimeException("error2"))
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Client with null and non-null cause are not equal`() {
        val error1 = SpeechCaptureError.Client()
        val error2 = SpeechCaptureError.Client(RuntimeException())
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Client supports copy`() {
        val original = SpeechCaptureError.Client()
        val newCause = IllegalArgumentException()
        val updated = original.copy(cause = newCause)
        
        assertNull(original.cause)
        assertEquals(newCause, updated.cause)
    }

    @Test
    fun `Client handles various exception types`() {
        val exceptions = listOf(
            RuntimeException("Runtime error"),
            IllegalStateException("Illegal state"),
            NullPointerException("Null pointer"),
            Exception("Generic exception")
        )
        
        exceptions.forEach { exception ->
            val error = SpeechCaptureError.Client(exception)
            assertEquals(exception, error.cause)
        }
    }

    // ========== Permission Error Tests ==========

    @Test
    fun `Permission creates with null message by default`() {
        val error = SpeechCaptureError.Permission()
        
        assertNull(error.message)
    }

    @Test
    fun `Permission creates with custom message`() {
        val error = SpeechCaptureError.Permission("Microphone permission denied")
        
        assertEquals("Microphone permission denied", error.message)
    }

    @Test
    fun `Permission is instance of SpeechCaptureError`() {
        val error = SpeechCaptureError.Permission()
        
        assertTrue(error is SpeechCaptureError)
    }

    @Test
    fun `Permission with same message are equal`() {
        val error1 = SpeechCaptureError.Permission("denied")
        val error2 = SpeechCaptureError.Permission("denied")
        
        assertEquals(error1, error2)
    }

    @Test
    fun `Permission with different messages are not equal`() {
        val error1 = SpeechCaptureError.Permission("message1")
        val error2 = SpeechCaptureError.Permission("message2")
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Permission supports copy`() {
        val original = SpeechCaptureError.Permission("Original")
        val updated = original.copy(message = "Updated")
        
        assertEquals("Original", original.message)
        assertEquals("Updated", updated.message)
    }

    @Test
    fun `Permission handles empty message`() {
        val error = SpeechCaptureError.Permission("")
        
        assertEquals("", error.message)
    }

    @Test
    fun `Permission handles long message`() {
        val longMessage = "A".repeat(500)
        val error = SpeechCaptureError.Permission(longMessage)
        
        assertEquals(longMessage, error.message)
    }

    @Test
    fun `Permission handles multiline message`() {
        val message = "Permission denied.\nPlease grant microphone access."
        val error = SpeechCaptureError.Permission(message)
        
        assertEquals(message, error.message)
    }

    // ========== Network Error Tests ==========

    @Test
    fun `Network creates with null message by default`() {
        val error = SpeechCaptureError.Network()
        
        assertNull(error.message)
    }

    @Test
    fun `Network creates with custom message`() {
        val error = SpeechCaptureError.Network("Connection timeout")
        
        assertEquals("Connection timeout", error.message)
    }

    @Test
    fun `Network is instance of SpeechCaptureError`() {
        val error = SpeechCaptureError.Network()
        
        assertTrue(error is SpeechCaptureError)
    }

    @Test
    fun `Network with same message are equal`() {
        val error1 = SpeechCaptureError.Network("timeout")
        val error2 = SpeechCaptureError.Network("timeout")
        
        assertEquals(error1, error2)
    }

    @Test
    fun `Network with different messages are not equal`() {
        val error1 = SpeechCaptureError.Network("timeout")
        val error2 = SpeechCaptureError.Network("connection failed")
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Network supports copy`() {
        val original = SpeechCaptureError.Network("Original")
        val updated = original.copy(message = "Updated")
        
        assertEquals("Original", original.message)
        assertEquals("Updated", updated.message)
    }

    @Test
    fun `Network handles various network error messages`() {
        val messages = listOf(
            "Connection timeout",
            "No internet connection",
            "Server unreachable",
            "DNS resolution failed"
        )
        
        messages.forEach { msg ->
            val error = SpeechCaptureError.Network(msg)
            assertEquals(msg, error.message)
        }
    }

    // ========== Unknown Error Tests ==========

    @Test
    fun `Unknown creates with code and null message`() {
        val error = SpeechCaptureError.Unknown(500)
        
        assertEquals(500, error.code)
        assertNull(error.message)
    }

    @Test
    fun `Unknown creates with code and message`() {
        val error = SpeechCaptureError.Unknown(404, "Not found")
        
        assertEquals(404, error.code)
        assertEquals("Not found", error.message)
    }

    @Test
    fun `Unknown is instance of SpeechCaptureError`() {
        val error = SpeechCaptureError.Unknown(0)
        
        assertTrue(error is SpeechCaptureError)
    }

    @Test
    fun `Unknown with same code and message are equal`() {
        val error1 = SpeechCaptureError.Unknown(100, "test")
        val error2 = SpeechCaptureError.Unknown(100, "test")
        
        assertEquals(error1, error2)
    }

    @Test
    fun `Unknown with different codes are not equal`() {
        val error1 = SpeechCaptureError.Unknown(100)
        val error2 = SpeechCaptureError.Unknown(200)
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Unknown with different messages are not equal`() {
        val error1 = SpeechCaptureError.Unknown(100, "message1")
        val error2 = SpeechCaptureError.Unknown(100, "message2")
        
        assertNotEquals(error1, error2)
    }

    @Test
    fun `Unknown supports copy with updated code`() {
        val original = SpeechCaptureError.Unknown(100)
        val updated = original.copy(code = 200)
        
        assertEquals(100, original.code)
        assertEquals(200, updated.code)
    }

    @Test
    fun `Unknown supports copy with updated message`() {
        val original = SpeechCaptureError.Unknown(100, "Original")
        val updated = original.copy(message = "Updated")
        
        assertEquals("Original", original.message)
        assertEquals("Updated", updated.message)
    }

    @Test
    fun `Unknown handles negative error codes`() {
        val error = SpeechCaptureError.Unknown(-1, "Negative code")
        
        assertEquals(-1, error.code)
        assertEquals("Negative code", error.message)
    }

    @Test
    fun `Unknown handles zero error code`() {
        val error = SpeechCaptureError.Unknown(0)
        
        assertEquals(0, error.code)
    }

    @Test
    fun `Unknown handles large error codes`() {
        val error = SpeechCaptureError.Unknown(99999)
        
        assertEquals(99999, error.code)
    }

    @Test
    fun `Unknown handles various Android SpeechRecognizer error codes`() {
        // Common Android SpeechRecognizer error codes
        val errorCodes = mapOf(
            1 to "ERROR_NETWORK_TIMEOUT",
            2 to "ERROR_NETWORK",
            3 to "ERROR_AUDIO",
            4 to "ERROR_SERVER",
            5 to "ERROR_CLIENT",
            6 to "ERROR_SPEECH_TIMEOUT",
            7 to "ERROR_NO_MATCH",
            8 to "ERROR_RECOGNIZER_BUSY",
            9 to "ERROR_INSUFFICIENT_PERMISSIONS"
        )
        
        errorCodes.forEach { (code, description) ->
            val error = SpeechCaptureError.Unknown(code, description)
            assertEquals(code, error.code)
            assertEquals(description, error.message)
        }
    }

    // ========== Sealed Interface Hierarchy Tests ==========

    @Test
    fun `all error types are SpeechCaptureError subtypes`() {
        val errors = listOf(
            SpeechCaptureError.NoMatch,
            SpeechCaptureError.Client(),
            SpeechCaptureError.Permission(),
            SpeechCaptureError.Network(),
            SpeechCaptureError.Unknown(0)
        )
        
        errors.forEach { error ->
            assertTrue(error is SpeechCaptureError)
        }
    }

    @Test
    fun `sealed interface supports when expressions`() {
        val errors = listOf(
            SpeechCaptureError.NoMatch,
            SpeechCaptureError.Client(RuntimeException()),
            SpeechCaptureError.Permission("denied"),
            SpeechCaptureError.Network("timeout"),
            SpeechCaptureError.Unknown(500, "error")
        )
        
        errors.forEach { error ->
            when (error) {
                is SpeechCaptureError.NoMatch -> assertTrue(true)
                is SpeechCaptureError.Client -> assertTrue(true)
                is SpeechCaptureError.Permission -> assertTrue(true)
                is SpeechCaptureError.Network -> assertTrue(true)
                is SpeechCaptureError.Unknown -> assertTrue(true)
            }
        }
    }

    @Test
    fun `different error types are distinct`() {
        val noMatch: SpeechCaptureError = SpeechCaptureError.NoMatch
        val client: SpeechCaptureError = SpeechCaptureError.Client()
        val permission: SpeechCaptureError = SpeechCaptureError.Permission()
        val network: SpeechCaptureError = SpeechCaptureError.Network()
        val unknown: SpeechCaptureError = SpeechCaptureError.Unknown(0)
        
        assertFalse(noMatch is SpeechCaptureError.Client)
        assertFalse(client is SpeechCaptureError.Permission)
        assertFalse(permission is SpeechCaptureError.Network)
        assertFalse(network is SpeechCaptureError.Unknown)
        assertFalse(unknown is SpeechCaptureError.NoMatch)
    }

    // ========== Use Case Tests ==========

    @Test
    fun `can handle no speech detected scenario`() {
        val error = SpeechCaptureError.NoMatch
        
        when (error) {
            is SpeechCaptureError.NoMatch -> assertTrue(true)
            else -> throw AssertionError("Should be NoMatch")
        }
    }

    @Test
    fun `can handle microphone permission denied scenario`() {
        val error = SpeechCaptureError.Permission("User denied microphone permission")
        
        when (error) {
            is SpeechCaptureError.Permission -> {
                assertEquals("User denied microphone permission", error.message)
            }
            else -> throw AssertionError("Should be Permission error")
        }
    }

    @Test
    fun `can handle network connectivity issues`() {
        val error = SpeechCaptureError.Network("No internet connection")
        
        when (error) {
            is SpeechCaptureError.Network -> {
                assertEquals("No internet connection", error.message)
            }
            else -> throw AssertionError("Should be Network error")
        }
    }

    @Test
    fun `can handle client-side errors with cause`() {
        val cause = IllegalStateException("Recognizer not available")
        val error = SpeechCaptureError.Client(cause)
        
        when (error) {
            is SpeechCaptureError.Client -> {
                assertEquals(cause, error.cause)
            }
            else -> throw AssertionError("Should be Client error")
        }
    }

    @Test
    fun `can handle unknown Android errors`() {
        val error = SpeechCaptureError.Unknown(7, "ERROR_NO_MATCH")
        
        when (error) {
            is SpeechCaptureError.Unknown -> {
                assertEquals(7, error.code)
                assertEquals("ERROR_NO_MATCH", error.message)
            }
            else -> throw AssertionError("Should be Unknown error")
        }
    }

    @Test
    fun `can map error types to user-friendly messages`() {
        val errors = mapOf(
            SpeechCaptureError.NoMatch to "No speech detected. Please try again.",
            SpeechCaptureError.Permission("denied") to "Microphone access is required.",
            SpeechCaptureError.Network("timeout") to "Network connection issue.",
            SpeechCaptureError.Client() to "Speech recognition is unavailable.",
            SpeechCaptureError.Unknown(0) to "An unknown error occurred."
        )
        
        errors.forEach { (error, _) ->
            val message = when (error) {
                is SpeechCaptureError.NoMatch -> "No speech detected. Please try again."
                is SpeechCaptureError.Permission -> "Microphone access is required."
                is SpeechCaptureError.Network -> "Network connection issue."
                is SpeechCaptureError.Client -> "Speech recognition is unavailable."
                is SpeechCaptureError.Unknown -> "An unknown error occurred."
            }
            assertTrue(message.isNotEmpty())
        }
    }
}
