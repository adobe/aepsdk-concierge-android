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
import android.speech.SpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Default Android implementation that wraps [SpeechToTextManager] to conform to [SpeechCapturing].
 */
internal class AndroidSpeechCapturing(
    context: Context,
    sttManagerFactory: SpeechToTextManagerFactory = defaultFactory,
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : SpeechCapturing {
    private var listener: SpeechCaptureListener? = null

    private val manager = sttManagerFactory.create(
        context = context,
        onSpeechStarted = { dispatchOnMain { listener?.onSpeechStarted() } },
        onSpeechEnded = { dispatchOnMain { listener?.onSpeechEnded() } },
        onPartialTranscription = { text -> dispatchOnMain { listener?.onPartialTranscription(text) } },
        onTranscriptionResult = { text -> dispatchOnMain { listener?.onTranscriptionResult(text) } },
        onSpeechError = { code -> dispatchOnMain { listener?.onError(mapError(code)) } }
    )

    override fun startCapture() {
        manager.startListening()
    }

    override fun endCapture() {
        manager.stopListening()
    }

    override fun isAvailable(): Boolean {
        return manager.isAvailable.value
    }

    override fun setListener(listener: SpeechCaptureListener?) {
        this.listener = listener
    }

    override fun release() {
        listener = null
        manager.release()
        // Cancel any pending main thread dispatches
        mainScope.cancel()
    }

    private fun dispatchOnMain(block: () -> Unit) {
        mainScope.launch { block() }
    }

    private fun mapError(code: Int): SpeechCaptureError {
        return when (code) {
            SpeechRecognizer.ERROR_NO_MATCH -> SpeechCaptureError.NoMatch
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechCaptureError.Permission()
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> SpeechCaptureError.Network()
            SpeechRecognizer.ERROR_CLIENT -> SpeechCaptureError.Client()
            else -> SpeechCaptureError.Unknown(code)
        }
    }

    internal fun interface SpeechToTextManagerFactory {
        fun create(
            context: Context,
            onSpeechStarted: () -> Unit,
            onSpeechEnded: () -> Unit,
            onPartialTranscription: (String) -> Unit,
            onTranscriptionResult: (String) -> Unit,
            onSpeechError: (Int) -> Unit
        ): SpeechToTextManager
    }

    private companion object {
        val defaultFactory = SpeechToTextManagerFactory { context, onStarted, onEnded, onPartial, onResult, onError ->
            SpeechToTextManager(
                context = context,
                onSpeechStarted = onStarted,
                onSpeechEnded = onEnded,
                onPartialTranscription = onPartial,
                onTranscriptionResult = onResult,
                onSpeechError = onError
            )
        }
    }
}


