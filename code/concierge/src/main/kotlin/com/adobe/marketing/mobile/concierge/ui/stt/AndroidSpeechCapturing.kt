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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Default Android implementation that wraps [SpeechToTextManager] to conform to [SpeechCapturing].
 */
internal class AndroidSpeechCapturing(
    private val manager: SpeechToTextManager,
    private val mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
) : SpeechCapturing {
    private var listener: SpeechCaptureListener? = null

    constructor(
        context: Context,
        mainScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    ) : this(
        manager = SpeechToTextManager(context = context),
        mainScope = mainScope
    )

    init {
        manager.setListener(object : SpeechCaptureListener {
            override fun onSpeechStarted() {
                dispatchOnMain { listener?.onSpeechStarted() }
            }

            override fun onSpeechEnded() {
                dispatchOnMain { listener?.onSpeechEnded() }
            }

            override fun onPartialTranscription(text: String) {
                dispatchOnMain { listener?.onPartialTranscription(text) }
            }

            override fun onTranscriptionResult(text: String) {
                dispatchOnMain { listener?.onTranscriptionResult(text) }
            }

            override fun onError(error: SpeechCaptureError) {
                dispatchOnMain { listener?.onError(error) }
            }
        })
    }

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
        manager.setListener(null)
        manager.release()
        // Cancel any pending main thread dispatches
        mainScope.cancel()
    }

    private fun dispatchOnMain(block: () -> Unit) {
        mainScope.launch { block() }
    }
}


