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

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FeedbackBackdropTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val feedback = mutableStateOf<Feedback?>(null)
    private var submittedFeedback: Feedback? = null
    private lateinit var scenario: ActivityScenario<ComponentActivity>

    @After
    fun cleanup() {
        if (::scenario.isInitialized) scenario.close()
    }

    @Test
    @SdkSuppress(minSdkVersion = 31)
    fun modal_blursAndBlocksBackground_untilDismissed() {
        showChat("modal")
        val headerBounds = Rect()
        requireNotNull(nodeWithText("Concierge")).getBoundsInScreen(headerBounds)
        val originalEdges = edgeEnergy(headerBounds)
        assertTrue("Header must have measurable text contrast", originalEdges > 0)

        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("modal feedback") { nodeWithText("Your feedback is appreciated") != null }
        assertNull("Background must not be accessible", nodeWithText("Concierge"))
        await("blurred background text") { edgeEnergy(headerBounds) < originalEdges * 0.25 }

        var cancel = requireNotNull(nodeWithText("Cancel"))
        while (!cancel.isClickable) cancel = requireNotNull(cancel.parent)
        assertTrue(cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))

        await("restored background") { nodeWithText("Concierge") != null }
        assertNull(nodeWithText("Your feedback is appreciated"))
        await("restored text sharpness") { edgeEnergy(headerBounds) > originalEdges * 0.8 }
    }

    @Test
    fun action_doesNotApplyInlineModalBackdrop() {
        showChat("action")
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.NEGATIVE) }
        instrumentation.waitForIdleSync()

        assertNotNull(nodeWithText("Concierge"))
        assertNull(nodeWithText("Your feedback is appreciated"))
    }

    @Test
    fun modal_notesKeyboard_backdropDismissesAndSubmissionPreservesNotes() {
        showChat("modal")
        val backdropBounds = Rect()
        requireNotNull(nodeWithText("Concierge")).getBoundsInScreen(backdropBounds)
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("notes editor") { findNode { it.isEditable } != null }
        assertTrue(requireNotNull(findNode { it.isEditable }).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("notes keyboard") { isKeyboardVisible() }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Review this feedback")
        }
        assertTrue(requireNotNull(findNode { it.isEditable }).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        await("entered notes") { findNode { it.isEditable }?.text?.toString() == "Review this feedback" }
        val visibleBounds = Rect()
        scenario.onActivity { it.window.decorView.getWindowVisibleDisplayFrame(visibleBounds) }
        for (label in listOf("Submit", "Cancel")) {
            val bounds = Rect()
            requireNotNull(nodeWithText(label)).getBoundsInScreen(bounds)
            assertTrue("$label must remain above the keyboard", visibleBounds.contains(bounds))
        }

        val eventTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(
                eventTime, SystemClock.uptimeMillis(), action,
                backdropBounds.exactCenterX(), backdropBounds.exactCenterY(), 0
            )
            try {
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
        await("dismissed notes keyboard") { !isKeyboardVisible() }
        assertNotNull(nodeWithText("Your feedback is appreciated"))
        assertEquals("Review this feedback", findNode { it.isEditable }?.text?.toString())

        var submit = requireNotNull(nodeWithText("Submit"))
        while (!submit.isClickable) submit = requireNotNull(submit.parent)
        assertTrue(submit.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("feedback submission") { submittedFeedback != null }
        assertEquals("Review this feedback", submittedFeedback?.notes)
    }

    private fun isKeyboardVisible(): Boolean {
        var visible = false
        scenario.onActivity {
            visible = ViewCompat.getRootWindowInsets(it.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }

    private fun showChat(displayMode: String) {
        val json = """{"behavior":{"feedback":{"displayMode":"$displayMode"}}}"""
        val theme = ConciergeThemeData(
            config = requireNotNull(ThemeParser.parseThemeJson(json)),
            tokens = requireNotNull(ThemeParser.parseThemeTokens(json))
        )
        val input = MutableStateFlow<UserInputState>(UserInputState.Empty)
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            activity.setContent {
                ConciergeTheme(theme = theme) {
                    CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                        ConciergeChat(
                            messages = emptyList(),
                            chatState = ChatScreenState.Idle(feedback.value),
                            isInputEmpty = true,
                            inputStateFlow = input,
                            hasAudioPermission = true,
                            showWelcomeCard = true,
                            welcomeConfig = WelcomeConfig(),
                            isReturningUser = false,
                            onTextChanged = {},
                            onEvent = { event ->
                                if (event == FeedbackEvent.DismissFeedbackDialog) feedback.value = null
                                if (event is FeedbackEvent.SubmitFeedback) {
                                    submittedFeedback = event.feedback
                                    feedback.value = null
                                }
                            },
                            onPermissionResult = {},
                            onClose = {}
                        )
                    }
                }
            }
        }
        await("chat header") { nodeWithText("Concierge") != null }
    }

    private fun nodeWithText(text: String): AccessibilityNodeInfo? = findNode { it.text?.toString() == text }

    private fun findNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return null
        fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            if (predicate(node)) return node
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                find(child)?.let { return it }
            }
            return null
        }
        return find(root)
    }

    private fun edgeEnergy(bounds: Rect): Double {
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            var energy = 0.0
            for (y in bounds.top until bounds.bottom) {
                for (x in bounds.left until bounds.right - 1) {
                    val left = screenshot.getPixel(x, y)
                    val right = screenshot.getPixel(x + 1, y)
                    energy += kotlin.math.abs(Color.red(left) - Color.red(right))
                    energy += kotlin.math.abs(Color.green(left) - Color.green(right))
                    energy += kotlin.math.abs(Color.blue(left) - Color.blue(right))
                }
            }
            return energy
        } finally {
            screenshot.recycle()
        }
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        do {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(50)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Timed out waiting for $description")
    }
}
