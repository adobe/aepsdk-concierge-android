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

import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidSpeechCapturingTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @RelaxedMockK
    private lateinit var manager: SpeechToTextManager

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(Job() + testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `forwards events from manager to SpeechCaptureListener`() {
        // Capture the proxy listener set on the manager during init
        var attached: SpeechCaptureListener? = null
        every { manager.setListener(any()) } answers { attached = firstArg() }

        val capturing = AndroidSpeechCapturing(manager, testScope)
        val recorder = RecordingCaptureListener()
        capturing.setListener(recorder)

        attached?.onSpeechStarted()
        attached?.onPartialTranscription("hi")
        attached?.onTranscriptionResult("hello")
        attached?.onSpeechEnded()
        attached?.onError(SpeechCaptureError.NoMatch)

        // Run dispatched coroutines posted to the provided scope
        testScope.testScheduler.advanceUntilIdle()

        assertEquals(1, recorder.startedCount)
        assertEquals(1, recorder.endedCount)
        assertEquals(listOf("hi"), recorder.partialResults)
        assertEquals(listOf("hello"), recorder.finalResults)
        assertEquals(1, recorder.errors.size)
    }

    @Test
    fun `startCapture and endCapture delegate to manager`() {
        val capturing = AndroidSpeechCapturing(manager, testScope)
        every { manager.startListening() } just Runs
        every { manager.stopListening() } just Runs

        capturing.startCapture()
        capturing.endCapture()

        verify { manager.startListening() }
        verify { manager.stopListening() }
    }

    @Test
    fun `release clears listener and releases manager`() {
        val capturing = AndroidSpeechCapturing(manager, testScope)
        every { manager.setListener(null) } just Runs
        every { manager.release() } just Runs

        capturing.release()

        verify { manager.setListener(null) }
        verify { manager.release() }
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


