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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

/**
 * Manages speech-to-text functionality using Android's SpeechRecognizer API and
 * provides callbacks for speech events and transcription results.
 * @param context The application context
 * @param onSpeechStarted Callback invoked when speech input starts
 * @param onSpeechEnded Callback invoked when speech input ends
 * @param onTranscriptionResult Callback invoked with the transcribed text result
 * @param onSpeechError Callback invoked with error code if speech recognition fails
 */
internal class SpeechToTextManager(
    private val context: Context,
    val onSpeechStarted: () -> Unit = {},
    val onSpeechEnded: () -> Unit = {},
    val onTranscriptionResult: (transcription: String) -> Unit = {},
    val onSpeechError: (recognitionError: Int) -> Unit = {},

    ) {
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isAvailable = mutableStateOf<Boolean>(false)
    val isAvailable: State<Boolean> = _isAvailable

    init {
        _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
        if (_isAvailable.value) {
            initializeSpeechRecognizer()
        }
    }

    private fun initializeSpeechRecognizer() {
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        } catch (e: Exception) {
            _isAvailable.value = false
        }
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            onSpeechEnded()
        }

        override fun onError(error: Int) {
            Log.error(
                ConciergeConstants.EXTENSION_NAME,
                "SpeechRecognizer",
                "Speech recognition error: $error"
            )
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    onTranscriptionResult("")
                }

                else -> {
                    onSpeechError(error)
                }
            }
        }

        override fun onResults(results: Bundle?) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                "SpeechRecognizer",
                "Speech recognition results received"
            )
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { text ->
                onTranscriptionResult(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening() {
        if (!_isAvailable.value) {
            // Recheck availability
            _isAvailable.value = SpeechRecognizer.isRecognitionAvailable(context)
            if (_isAvailable.value) {
                initializeSpeechRecognizer()
            } else {
                return
            }
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // Add these for better compatibility
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

        try {
            speechRecognizer?.startListening(intent)
            onSpeechStarted()

        } catch (e: Exception) {
            onSpeechError(SpeechRecognizer.ERROR_CLIENT)
            _isAvailable.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            onSpeechEnded()
        } catch (e: Exception) {

        } finally {
            onSpeechEnded()
        }
    }


    fun release() {
        try {
            speechRecognizer?.destroy()
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                "SpeechRecognizer",
                "Speech recognizer released"
            )
            _isAvailable.value = false
        } catch (e: Exception) {
            // Handle any errors silently
        } finally {
            speechRecognizer = null
            _isAvailable.value = false
        }
    }
}