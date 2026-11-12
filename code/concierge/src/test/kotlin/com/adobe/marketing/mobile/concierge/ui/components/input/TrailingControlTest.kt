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

package com.adobe.marketing.mobile.concierge.ui.components.input

import org.junit.Assert.assertEquals
import org.junit.Test

class TrailingControlTest {

    private fun control(
        isRecording: Boolean = false,
        hasText: Boolean = false,
        showMic: Boolean = true,
        isVoiceAvailable: Boolean = true,
        isVoiceSessionActive: Boolean = false,
        isVoiceConnecting: Boolean = false
    ) = trailingControl(
        isRecording = isRecording,
        hasText = hasText,
        showMic = showMic,
        isVoiceAvailable = isVoiceAvailable,
        isVoiceSessionActive = isVoiceSessionActive,
        isVoiceConnecting = isVoiceConnecting
    )

    @Test
    fun `recording shows the recording controls`() {
        assertEquals(TrailingControl.RECORDING, control(isRecording = true))
        assertEquals(TrailingControl.RECORDING, control(isRecording = true, hasText = true))
        assertEquals(TrailingControl.RECORDING, control(isRecording = true, isVoiceAvailable = false))
    }

    @Test
    fun `empty composer shows the voice button when voice is available`() {
        assertEquals(TrailingControl.VOICE, control())
        assertEquals(TrailingControl.VOICE, control(showMic = false))
    }

    @Test
    fun `active or connecting voice keeps the voice button even with text`() {
        assertEquals(TrailingControl.VOICE, control(hasText = true, isVoiceSessionActive = true))
        assertEquals(TrailingControl.VOICE, control(hasText = true, isVoiceConnecting = true))
    }

    @Test
    fun `text shows send when no voice session is running`() {
        assertEquals(TrailingControl.SEND, control(hasText = true))
        assertEquals(TrailingControl.SEND, control(hasText = true, isVoiceAvailable = false))
        assertEquals(TrailingControl.SEND, control(hasText = true, showMic = false, isVoiceAvailable = false))
    }

    @Test
    fun `without voice an empty composer shows the mic or nothing`() {
        assertEquals(TrailingControl.MIC, control(isVoiceAvailable = false))
        assertEquals(TrailingControl.NONE, control(isVoiceAvailable = false, showMic = false))
    }

    @Test
    fun `session flags are ignored when no voice transport is provided`() {
        assertEquals(TrailingControl.MIC, control(isVoiceAvailable = false, isVoiceSessionActive = true))
        assertEquals(TrailingControl.SEND, control(isVoiceAvailable = false, hasText = true, isVoiceConnecting = true))
    }
}
