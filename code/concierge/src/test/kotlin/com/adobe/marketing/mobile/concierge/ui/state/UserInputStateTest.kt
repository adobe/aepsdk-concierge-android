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

package com.adobe.marketing.mobile.concierge.ui.state

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserInputStateTest {

    // ========== Empty State Tests ==========

    @Test
    fun `Empty is singleton object`() {
        val state1 = UserInputState.Empty
        val state2 = UserInputState.Empty
        
        assertTrue(state1 === state2)
    }

    @Test
    fun `Empty is instance of UserInputState`() {
        val state = UserInputState.Empty
        
        assertTrue(state is UserInputState)
    }

    @Test
    fun `Empty state equals itself`() {
        assertEquals(UserInputState.Empty, UserInputState.Empty)
    }

    @Test
    fun `Empty state is different from other states`() {
        assertNotEquals(UserInputState.Empty, UserInputState.Recording())
        assertNotEquals(UserInputState.Empty, UserInputState.Editing())
        assertNotEquals(UserInputState.Empty, UserInputState.Error("error"))
    }

    // ========== Recording State Tests ==========

    @Test
    fun `Recording creates with default empty transcription`() {
        val state = UserInputState.Recording()
        
        assertEquals("", state.transcription)
    }

    @Test
    fun `Recording creates with custom transcription`() {
        val state = UserInputState.Recording("Hello world")
        
        assertEquals("Hello world", state.transcription)
    }

    @Test
    fun `Recording is instance of UserInputState`() {
        val state = UserInputState.Recording()
        
        assertTrue(state is UserInputState)
    }

    @Test
    fun `Recording supports copy with updated transcription`() {
        val original = UserInputState.Recording("Hello")
        val updated = original.copy(transcription = "Hello world")
        
        assertEquals("Hello", original.transcription)
        assertEquals("Hello world", updated.transcription)
    }

    @Test
    fun `Recording with same transcription are equal`() {
        val state1 = UserInputState.Recording("test")
        val state2 = UserInputState.Recording("test")
        
        assertEquals(state1, state2)
    }

    @Test
    fun `Recording with different transcription are not equal`() {
        val state1 = UserInputState.Recording("test1")
        val state2 = UserInputState.Recording("test2")
        
        assertNotEquals(state1, state2)
    }

    @Test
    fun `Recording handles empty string transcription`() {
        val state = UserInputState.Recording("")
        
        assertEquals("", state.transcription)
    }

    @Test
    fun `Recording handles multiline transcription`() {
        val text = "Line 1\nLine 2\nLine 3"
        val state = UserInputState.Recording(text)
        
        assertEquals(text, state.transcription)
    }

    @Test
    fun `Recording handles special characters in transcription`() {
        val text = "Hello! How are you? #test @user"
        val state = UserInputState.Recording(text)
        
        assertEquals(text, state.transcription)
    }

    @Test
    fun `Recording handles unicode in transcription`() {
        val text = "こんにちは 🎉 Émojis"
        val state = UserInputState.Recording(text)
        
        assertEquals(text, state.transcription)
    }

    // ========== Editing State Tests ==========

    @Test
    fun `Editing creates with default empty content and false pending`() {
        val state = UserInputState.Editing()
        
        assertEquals("", state.content)
        assertFalse(state.isPendingTranscription)
    }

    @Test
    fun `Editing creates with custom content`() {
        val state = UserInputState.Editing(content = "My message")
        
        assertEquals("My message", state.content)
        assertFalse(state.isPendingTranscription)
    }

    @Test
    fun `Editing creates with pending transcription flag`() {
        val state = UserInputState.Editing(
            content = "Partial text",
            isPendingTranscription = true
        )
        
        assertEquals("Partial text", state.content)
        assertTrue(state.isPendingTranscription)
    }

    @Test
    fun `Editing is instance of UserInputState`() {
        val state = UserInputState.Editing()
        
        assertTrue(state is UserInputState)
    }

    @Test
    fun `Editing supports copy with updated content`() {
        val original = UserInputState.Editing("Hello")
        val updated = original.copy(content = "Hello world")
        
        assertEquals("Hello", original.content)
        assertEquals("Hello world", updated.content)
    }

    @Test
    fun `Editing supports copy with updated pending flag`() {
        val original = UserInputState.Editing("test", isPendingTranscription = false)
        val updated = original.copy(isPendingTranscription = true)
        
        assertFalse(original.isPendingTranscription)
        assertTrue(updated.isPendingTranscription)
    }

    @Test
    fun `Editing with same values are equal`() {
        val state1 = UserInputState.Editing("test", true)
        val state2 = UserInputState.Editing("test", true)
        
        assertEquals(state1, state2)
    }

    @Test
    fun `Editing with different content are not equal`() {
        val state1 = UserInputState.Editing("test1")
        val state2 = UserInputState.Editing("test2")
        
        assertNotEquals(state1, state2)
    }

    @Test
    fun `Editing with different pending flag are not equal`() {
        val state1 = UserInputState.Editing("test", false)
        val state2 = UserInputState.Editing("test", true)
        
        assertNotEquals(state1, state2)
    }

    @Test
    fun `Editing handles long content`() {
        val longText = "A".repeat(5000)
        val state = UserInputState.Editing(longText)
        
        assertEquals(longText, state.content)
    }

    @Test
    fun `Editing handles multiline content`() {
        val text = "Line 1\nLine 2\nLine 3"
        val state = UserInputState.Editing(text)
        
        assertEquals(text, state.content)
    }

    @Test
    fun `Editing handles special characters in content`() {
        val text = "Special chars: !@#$%^&*()"
        val state = UserInputState.Editing(text)
        
        assertEquals(text, state.content)
    }

    // ========== Error State Tests ==========

    @Test
    fun `Error creates with message`() {
        val state = UserInputState.Error("Something went wrong")
        
        assertEquals("Something went wrong", state.message)
    }

    @Test
    fun `Error is instance of UserInputState`() {
        val state = UserInputState.Error("error")
        
        assertTrue(state is UserInputState)
    }

    @Test
    fun `Error supports copy with updated message`() {
        val original = UserInputState.Error("Error 1")
        val updated = original.copy(message = "Error 2")
        
        assertEquals("Error 1", original.message)
        assertEquals("Error 2", updated.message)
    }

    @Test
    fun `Error with same message are equal`() {
        val state1 = UserInputState.Error("error")
        val state2 = UserInputState.Error("error")
        
        assertEquals(state1, state2)
    }

    @Test
    fun `Error with different messages are not equal`() {
        val state1 = UserInputState.Error("error1")
        val state2 = UserInputState.Error("error2")
        
        assertNotEquals(state1, state2)
    }

    @Test
    fun `Error handles empty message`() {
        val state = UserInputState.Error("")
        
        assertEquals("", state.message)
    }

    @Test
    fun `Error handles network error message`() {
        val state = UserInputState.Error("Network connection failed")
        
        assertEquals("Network connection failed", state.message)
    }

    @Test
    fun `Error handles permission error message`() {
        val state = UserInputState.Error("Microphone permission denied")
        
        assertEquals("Microphone permission denied", state.message)
    }

    @Test
    fun `Error handles long error message`() {
        val longMessage = "A".repeat(1000)
        val state = UserInputState.Error(longMessage)
        
        assertEquals(longMessage, state.message)
    }

    // ========== Sealed Class Hierarchy Tests ==========

    @Test
    fun `all state types are UserInputState subtypes`() {
        val states = listOf(
            UserInputState.Empty,
            UserInputState.Recording(),
            UserInputState.Editing(),
            UserInputState.Error("error")
        )
        
        states.forEach { state ->
            assertTrue(state is UserInputState)
        }
    }

    @Test
    fun `sealed class supports when expressions`() {
        val states = listOf(
            UserInputState.Empty,
            UserInputState.Recording("test"),
            UserInputState.Editing("content"),
            UserInputState.Error("error")
        )
        
        states.forEach { state ->
            when (state) {
                is UserInputState.Empty -> assertTrue(true)
                is UserInputState.Recording -> assertTrue(true)
                is UserInputState.Editing -> assertTrue(true)
                is UserInputState.Error -> assertTrue(true)
            }
        }
    }

    @Test
    fun `different state types are distinct`() {
        val empty: UserInputState = UserInputState.Empty
        val recording: UserInputState = UserInputState.Recording()
        val editing: UserInputState = UserInputState.Editing()
        val error: UserInputState = UserInputState.Error("error")
        
        assertFalse(empty is UserInputState.Recording)
        assertFalse(recording is UserInputState.Editing)
        assertFalse(editing is UserInputState.Error)
        assertFalse(error is UserInputState.Empty)
    }

    // ========== State Transition Tests ==========

    @Test
    fun `transition from Empty to Recording`() {
        val empty = UserInputState.Empty
        val recording = UserInputState.Recording()
        
        assertNotEquals(empty, recording)
        assertTrue(recording is UserInputState.Recording)
    }

    @Test
    fun `transition from Recording to Editing`() {
        val recording = UserInputState.Recording("Recorded text")
        val editing = UserInputState.Editing(
            content = recording.transcription,
            isPendingTranscription = true
        )
        
        assertEquals("Recorded text", editing.content)
        assertTrue(editing.isPendingTranscription)
    }

    @Test
    fun `transition to Error from any state`() {
        val states = listOf(
            UserInputState.Empty,
            UserInputState.Recording("test"),
            UserInputState.Editing("content")
        )
        
        states.forEach { state ->
            val errorState = UserInputState.Error("Error occurred")
            assertNotEquals(state, errorState)
            assertTrue(errorState is UserInputState.Error)
        }
    }

    @Test
    fun `Recording can update transcription progressively`() {
        val state1 = UserInputState.Recording("Hello")
        val state2 = state1.copy(transcription = "Hello world")
        val state3 = state2.copy(transcription = "Hello world, how")
        
        assertEquals("Hello", state1.transcription)
        assertEquals("Hello world", state2.transcription)
        assertEquals("Hello world, how", state3.transcription)
    }

    @Test
    fun `Editing can toggle pending transcription flag`() {
        val state1 = UserInputState.Editing("text", isPendingTranscription = true)
        val state2 = state1.copy(isPendingTranscription = false)
        
        assertTrue(state1.isPendingTranscription)
        assertFalse(state2.isPendingTranscription)
    }
}
