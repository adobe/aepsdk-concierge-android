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

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.test.core.app.ApplicationProvider
import io.mockk.CapturingSlot
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import io.mockk.slot
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpeechToTextManagerTest {

    private lateinit var recognizer: SpeechRecognizer
    private lateinit var internalSpeechListener: CapturingSlot<RecognitionListener>
    private lateinit var testListener: RecordingCaptureListener

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkStatic(SpeechRecognizer::class)
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true

        recognizer = mockk(relaxed = true)
        internalSpeechListener = CapturingSlot()
        every { recognizer.setRecognitionListener(capture(internalSpeechListener)) } just Runs
        every { SpeechRecognizer.createSpeechRecognizer(any()) } returns recognizer

        testListener = RecordingCaptureListener()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `startListening invokes SpeechRecognizer and notifies Started`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        verify { recognizer.startListening(any<Intent>()) }
        assertEquals(1, testListener.startedCount)
    }

    @Test
    fun `onResults emits final transcription`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        val bundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("hello world"))
        }
        internalSpeechListener.captured.onResults(bundle)

        assertEquals(listOf("hello world"), testListener.finalResults)
    }

    @Test
    fun `onPartialResults emits partial transcription`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        val bundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("partial"))
        }
        internalSpeechListener.captured.onPartialResults(bundle)

        assertEquals(listOf("partial"), testListener.partialResults)
    }

    @Test
    fun `onError maps to SpeechCaptureError variants`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        internalSpeechListener.captured.onError(SpeechRecognizer.ERROR_NO_MATCH)
        internalSpeechListener.captured.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        internalSpeechListener.captured.onError(SpeechRecognizer.ERROR_NETWORK)
        internalSpeechListener.captured.onError(SpeechRecognizer.ERROR_CLIENT)
        internalSpeechListener.captured.onError(999)

        assertTrue(testListener.errors.any { it is SpeechCaptureError.NoMatch })
        assertTrue(testListener.errors.any { it is SpeechCaptureError.Permission })
        assertTrue(testListener.errors.any { it is SpeechCaptureError.Network })
        assertTrue(testListener.errors.any { it is SpeechCaptureError.Client })
        assertTrue(testListener.errors.any { it is SpeechCaptureError.Unknown && it.code == 999 })
    }

    @Test
    fun `stopListening notifies Ended and stops recognizer`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()
        sttManager.stopListening()

        // onEnd is invoked at least once
        assertTrue(testListener.endedCount >= 1)
        verify { recognizer.stopListening() }
    }

    @Test
    fun `release destroys SpeechRecognizer`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.release()

        verify { recognizer.destroy() }
    }

    @Test
    fun `init with recognition unavailable does not initialize and start returns early`() {
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns false
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        verify(exactly = 0) { recognizer.startListening(any()) }
        assertEquals(0, testListener.startedCount)
    }

    @Test
    fun `recheck availability on start initializes and starts when available`() {
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returnsMany listOf(false, true)
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()

        verify { recognizer.startListening(any<Intent>()) }
        assertEquals(1, testListener.startedCount)
    }

    @Test
    fun `createSpeechRecognizer throws, isAvailable becomes false`() {
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true
        every { SpeechRecognizer.createSpeechRecognizer(any()) } throws RuntimeException("boom")

        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())

        // Manager should mark availability false after failing to initialize
        assertEquals(false, sttManager.isAvailable.value)
    }

    @Test
    fun `startListening builds intent with expected extras`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        val intentSlot = slot<Intent>()
        every { recognizer.startListening(capture(intentSlot)) } just Runs

        sttManager.startListening()

        verify { recognizer.startListening(any()) }
        val intent = intentSlot.captured
        assertEquals(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL))
        assertEquals(true, intent.getBooleanExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false))
        assertEquals(1, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, -1))
        assertEquals(false, intent.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true))
        assertEquals(ApplicationProvider.getApplicationContext<android.content.Context>().packageName, intent.getStringExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE))
    }

    @Test
    fun `onResults with no matches emits nothing`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.startListening()

        internalSpeechListener.captured.onResults(Bundle())

        assertTrue(testListener.finalResults.isEmpty())
    }

    @Test
    fun `onPartialResults forwards only the first string`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.startListening()

        val bundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("first", "second"))
        }
        internalSpeechListener.captured.onPartialResults(bundle)

        assertEquals(listOf("first"), testListener.partialResults)
    }

    @Test
    fun `onError maps network timeout to Network`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.startListening()

        internalSpeechListener.captured.onError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT)

        assertTrue(testListener.errors.any { it is SpeechCaptureError.Network })
    }

    @Test
    fun `startListening throws client error and marks unavailable`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        every { recognizer.startListening(any()) } throws RuntimeException("fail")

        sttManager.startListening()

        assertTrue(testListener.errors.any { it is SpeechCaptureError.Client })
        assertEquals(false, sttManager.isAvailable.value)
    }

    @Test
    fun `onEndOfSpeech notifies Ended once`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.startListening()

        internalSpeechListener.captured.onEndOfSpeech()

        assertEquals(1, testListener.endedCount)
    }

    @Test
    fun `stopListening notifies Ended twice`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.startListening()

        sttManager.stopListening()

        assertEquals(2, testListener.endedCount)
        verify { recognizer.stopListening() }
    }

    @Test
    fun `setListener null prevents forwarding`() {
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns true
        // Manually trigger callbacks safely (no forwarding expected)
        // Create a new manager to ensure listener is attached then null it
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)
        sttManager.setListener(null)
        sttManager.startListening()
        internalSpeechListener.captured.onResults(Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("data"))
        })

        assertTrue(testListener.finalResults.isEmpty())
    }

    @Test
    fun `replacing listener forwards only to the latest`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        val first = RecordingCaptureListener()
        val second = RecordingCaptureListener()
        sttManager.setListener(first)
        sttManager.setListener(second)
        sttManager.startListening()

        val bundle = Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf("hello"))
        }
        internalSpeechListener.captured.onResults(bundle)

        assertTrue(first.finalResults.isEmpty())
        assertEquals(listOf("hello"), second.finalResults)
    }

    @Test
    fun `release without initialization does not destroy recognizer`() {
        every { SpeechRecognizer.isRecognitionAvailable(any()) } returns false
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())

        sttManager.release()

        verify(exactly = 0) { recognizer.destroy() }
    }

    @Test
    fun `multiple start and stop calls are safe`() {
        val sttManager = SpeechToTextManager(ApplicationProvider.getApplicationContext())
        sttManager.setListener(testListener)

        sttManager.startListening()
        sttManager.startListening()
        verify(exactly = 2) { recognizer.startListening(any()) }

        sttManager.stopListening()
        sttManager.stopListening()
        verify(exactly = 2) { recognizer.stopListening() }
        // Each stop triggers two Ended callbacks
        assertEquals(4, testListener.endedCount)
    }

    private class RecordingCaptureListener : SpeechCaptureListener {
        var startedCount: Int = 0
        var endedCount: Int = 0
        val partialResults = mutableListOf<String>()
        val finalResults = mutableListOf<String>()
        val errors = mutableListOf<SpeechCaptureError>()

        override fun onSpeechStarted() { startedCount++ }
        override fun onSpeechEnded() { endedCount++ }
        override fun onPartialTranscription(text: String) { partialResults.add(text) }
        override fun onTranscriptionResult(text: String) { finalResults.add(text) }
        override fun onError(error: SpeechCaptureError) { errors.add(error) }
    }
}


