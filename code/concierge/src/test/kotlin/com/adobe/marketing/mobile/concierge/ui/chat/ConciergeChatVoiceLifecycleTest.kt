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

package com.adobe.marketing.mobile.concierge.ui.chat

import android.Manifest
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModelStore
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeState
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.VoiceTransport
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.voice.FakeVoiceTransport
import com.adobe.marketing.mobile.concierge.voice.VoiceSessionState
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.util.ReflectionHelpers
import java.time.Duration

/** How long a voice conversation lasts while [ConciergeChat] is hosted in an activity. */
@RunWith(RobolectricTestRunner::class)
class ConciergeChatVoiceLifecycleTest {

    private val service = FakeVoiceConversationService()
    private val factory = FakeVoiceSessionFactory()
    private val viewModelStore = ViewModelStore()
    private val activities = mutableListOf<ActivityController<ChatHostActivity>>()
    private lateinit var viewModel: ConciergeChatViewModel

    @Before
    fun setUp() {
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(FRAME)
        ConciergeStateRepository.instance.clear()
        mockkObject(ConciergeSessionManager.instance)
        every { ConciergeSessionManager.instance.getSessionId() } returns "session-1"
        every { ConciergeSessionManager.instance.currentSessionIdOrNull() } returns "session-1"
        mockkStatic(ServiceProvider::class)
        every { ServiceProvider.getInstance() } returns mockk(relaxed = true)
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(Manifest.permission.RECORD_AUDIO)
        viewModel = ConciergeChatViewModel(
            app, NoSpeechCapturing(), DefaultImageProvider(), service,
            stateRepository = ConciergeStateRepository(
                ConciergeState(
                    experienceCloudId = "ecid", configurationReady = true,
                    conciergeServer = "https://example.com", conciergeConfigId = "config"
                )
            ),
            sessionDispatcher = Dispatchers.Unconfined,
            appLifecycle = null,
            dispatch = {},
            voiceSessionFactory = factory::create,
            voiceCallbackDispatcher = Dispatchers.Unconfined
        )
        viewModelStore.put("chat", viewModel)
        ChatHostActivity.viewModel = viewModel
        ChatHostActivity.transport = FakeVoiceTransport()
        ChatHostActivity.showChat.value = true
    }

    @After
    fun tearDown() {
        activities.filterNot { it.get().isDestroyed }.forEach { it.pause().stop().destroy() }
        ChatHostActivity.viewModel = null
        ChatHostActivity.transport = null
        viewModelStore.clear()
        ConciergeStateRepository.instance.clear()
        unmockkObject(ConciergeSessionManager.instance)
        unmockkStatic(ServiceProvider::class)
        ShadowChoreographer.setPaused(false)
    }

    private fun launchChat(): ActivityController<ChatHostActivity> =
        Robolectric.buildActivity(ChatHostActivity::class.java).setup().also {
            activities.add(it)
            runFrames()
            assertTrue(viewModel.isVoiceAvailable.value)
        }

    /** Starts voice from the chat and connects it, as the voice button does. */
    private fun startListening(): FakeVoiceSession {
        viewModel.startVoiceSession()
        runFrames()
        service.emitBootstrap()
        runFrames()
        val session = requireNotNull(factory.lastSession)
        session.report(VoiceSessionState.Listening)
        runFrames()
        assertTrue(viewModel.isVoiceSessionActive.value)
        return session
    }

    /**
     * Runs the main looper for a few frames. The choreographer is paused in [setUp], so frames run
     * only as the clock advances; otherwise the chat's endless animations would keep it busy.
     */
    private fun runFrames() = shadowOf(Looper.getMainLooper()).idleFor(FRAME.multipliedBy(5))

    @Test
    fun `voice keeps running when the activity is recreated for a configuration change`() {
        val controller = launchChat()
        val session = startListening()

        recreateForConfigurationChange(controller)

        assertTrue(viewModel.isVoiceSessionActive.value)
        assertEquals(0, session.stopCallCount)
    }

    /**
     * Destroys the activity as Android does for a configuration change, then shows a new one.
     * `ActivityController.recreate()` can't be used: with the choreographer paused, it reaches
     * the new window before its first frame and fails.
     */
    private fun recreateForConfigurationChange(controller: ActivityController<ChatHostActivity>) {
        val activity = controller.get()
        controller.pause().stop()
        ReflectionHelpers.setField(activity, "mChangingConfigurations", true)
        assertTrue(activity.isChangingConfigurations)
        controller.destroy()
        runFrames()
        launchChat()
    }

    @Test
    fun `voice ends when the chat leaves the screen`() {
        launchChat()
        val session = startListening()

        ChatHostActivity.showChat.value = false
        runFrames()

        assertFalse(viewModel.isVoiceSessionActive.value)
        assertEquals(1, session.stopCallCount)
    }

    @Test
    fun `voice ends when the activity finishes`() {
        val controller = launchChat()
        val session = startListening()

        controller.get().finish()
        controller.pause().stop().destroy()
        runFrames()

        assertFalse(viewModel.isVoiceSessionActive.value)
        assertEquals(1, session.stopCallCount)
    }

    /** Shows the chat in direct mode; a recreated instance shows it again, like a host app would. */
    class ChatHostActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContent {
                val vm = viewModel
                if (vm != null && showChat.value) {
                    ConciergeChat(viewModel = vm, onClose = {}, voiceTransport = transport)
                }
            }
        }

        companion object {
            var viewModel: ConciergeChatViewModel? = null
            var transport: VoiceTransport? = null
            val showChat = mutableStateOf(true)
        }
    }

    private companion object {
        val FRAME: Duration = Duration.ofMillis(16)
    }

    private class NoSpeechCapturing : SpeechCapturing {
        override fun isAvailable(): Boolean = false
        override fun setListener(listener: SpeechCaptureListener?) = Unit
        override fun startCapture() = Unit
        override fun endCapture() = Unit
        override fun release() = Unit
    }
}
