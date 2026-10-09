/*
 * Copyright 2025 Adobe. All rights reserved.
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

interface SpeechCaptureListener {
    fun onSpeechStarted()
    fun onSpeechEnded()
    fun onPartialTranscription(text: String)
    fun onTranscriptionResult(text: String)
    fun onError(error: SpeechCaptureError)
    /** Reports the current input level, normalized to 0f (silence) .. 1f (loud). */
    fun onAudioLevelChanged(level: Float)
}

/**
 * Abstraction for speech capturing engines.
 * Single-listener model to keep lifecycle simple.
 */
interface SpeechCapturing {
    fun isAvailable(): Boolean
    /**
     * Replaces the callback receiver. Detaching with null is also used during identity reset.
     * Engines should invalidate active recognition callbacks and queued deliveries on detach,
     * so an old capture cannot deliver results to a subsequently attached listener.
     */
    fun setListener(listener: SpeechCaptureListener?)
    fun startCapture()
    /** Ends normal capture while allowing its final transcription to be delivered. */
    fun endCapture()
    fun release()
}

