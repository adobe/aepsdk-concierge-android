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
 * Uses a single listener to report speech lifecycle events, partial/final results,
 * and errors via [SpeechCaptureError].
 */
internal class SpeechToTextManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "SpeechToTextManager"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var listener: SpeechCaptureListener? = null

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
            listener?.onSpeechEnded()
        }

        override fun onError(error: Int) {
            Log.error(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Speech recognition error: $error"
            )
            val mapped = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> SpeechCaptureError.NoMatch
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechCaptureError.Permission()
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechCaptureError.Network()
                SpeechRecognizer.ERROR_CLIENT -> SpeechCaptureError.Client()
                else -> SpeechCaptureError.Unknown(error)
            }
            listener?.onError(mapped)
        }

        override fun onResults(results: Bundle?) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Speech recognition results received"
            )
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { text ->
                listener?.onTranscriptionResult(text)
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "Final transcription: $text"
                )
            } ?: run {
                Log.debug(
                    ConciergeConstants.EXTENSION_NAME,
                    TAG,
                    "No final transcription results found"
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Speech recognition partial results received"
            )
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { partialText ->
                listener?.onPartialTranscription(partialText)
            }
        }

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
            listener?.onSpeechStarted()

        } catch (e: Exception) {
            listener?.onError(SpeechCaptureError.Client(e))
            _isAvailable.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            listener?.onSpeechEnded()
        } catch (e: Exception) {
        } finally {
            listener?.onSpeechEnded()
        }
    }


    fun release() {
        try {
            speechRecognizer?.destroy()
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
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

    fun setListener(listener: SpeechCaptureListener?) {
        this.listener = listener
    }
}