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
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.FrameLayout
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.concierge.ui.config.WelcomeConfig
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatScreenState
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackEvent
import com.adobe.marketing.mobile.concierge.ui.state.FeedbackType
import com.adobe.marketing.mobile.concierge.ui.state.UserInputState
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.state.MessageInteractionEvent
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
    private var closeCalls = 0
    private var hostBottom = 0f
    private val backgroundEvents = mutableListOf<ChatEvent>()
    private lateinit var inputModeManager: InputModeManager
    private val composerInput = MutableStateFlow<UserInputState>(UserInputState.Empty)
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
    fun modal_blocksBackgroundCloseButtonUntilDismissed() {
        showChat("modal")
        val closeBounds = Rect()
        requireNotNull(findNode { it.contentDescription?.toString() == "Close chat" })
            .getBoundsInScreen(closeBounds)
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.NEGATIVE) }
        await("modal feedback") { nodeWithText("Your feedback is appreciated") != null }
        tap(closeBounds.exactCenterX(), closeBounds.exactCenterY())
        instrumentation.waitForIdleSync()
        assertEquals(0, closeCalls)
        var cancel = requireNotNull(nodeWithText("Cancel"))
        while (!cancel.isClickable) cancel = requireNotNull(cancel.parent)
        assertTrue(cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("restored background close button") {
            findNode { it.contentDescription?.toString() == "Close chat" } != null
        }
        tap(closeBounds.exactCenterX(), closeBounds.exactCenterY())
        await("background close callback") { closeCalls == 1 }
    }

    @Test
    fun modal_clearsComposerFocusAndPreventsKeyboardTraversalIntoBackground() {
        showChat("modal", messages = conversation())
        assertTrue(requireNotNull(findNode { it.isEditable }).performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("composer keyboard") { isKeyboardVisible() }
        val draft = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Original draft")
        }
        assertTrue(requireNotNull(findNode { it.isEditable }).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, draft))
        await("composer draft") { composerInput.value is UserInputState.Editing }
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.NEGATIVE) }
        await("modal without composer focus") {
            nodeWithText("Your feedback is appreciated") != null && !isKeyboardVisible()
        }
        repeat(12) {
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_A)
            instrumentation.waitForIdleSync()
            assertEquals(UserInputState.Editing("Original draft"), composerInput.value)
            assertNull(findNode { it.contentDescription?.toString() == "Close chat" })
        }
    }

    @Test
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun modal_populatedConversation_blocksTabAndDpadActivationOfBackgroundControls() {
        // Leave no enabled modal actions so keyboard activation can only target the background.
        showChat(
            "modal",
            messages = conversation(),
            feedbackConfig = ""","showCancelButton":false,"showCloseButton":false""",
            themeConfig = ""","arrays":{"feedback.positive.options":[]},"components":{"feedback":{"positiveNotesEnabled":false}}"""
        )
        await("background suggestion") { nodeWithText("Background suggestion") != null }
        assertNotNull(nodeWithText("Background CTA"))
        scenario.onActivity { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("modal feedback") { nodeWithText("Your feedback is appreciated") != null }
        for (direction in listOf(
            KeyEvent.KEYCODE_TAB, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT
        )) {
            repeat(12) { target ->
                repeat(target + 1) { instrumentation.sendKeyDownUpSync(direction) }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
                instrumentation.waitForIdleSync()
                scenario.onActivity {
                    assertTrue("Background callbacks must be blocked: $backgroundEvents", backgroundEvents.isEmpty())
                    assertEquals(0, closeCalls)
                    assertNotNull(feedback.value)
                }
            }
        }
        scenario.onActivity { feedback.value = null }
        await("restored background CTA") { nodeWithText("Background CTA") != null }
        repeat(20) { target ->
            if (backgroundEvents.isEmpty()) {
                repeat(target + 1) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB) }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
                instrumentation.waitForIdleSync()
            }
        }
        assertTrue("Keyboard activation must reach restored background controls", backgroundEvents.isNotEmpty())
        scenario.onActivity { backgroundEvents.clear() }
        for (text in listOf("Background CTA", "Background suggestion")) {
            var button = requireNotNull(nodeWithText(text))
            while (!button.isClickable) button = requireNotNull(button.parent)
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        await("restored background callbacks") { backgroundEvents.size == 2 }
        assertTrue(backgroundEvents[0] is MessageInteractionEvent.CtaButtonClick)
        assertTrue(backgroundEvents[1] is MessageInteractionEvent.PromptSuggestionClick)
    }

    private fun conversation() = listOf(
        ChatMessage(MessageContent.Text("Earlier answer"), false, 1, sseComplete = true),
        ChatMessage(
            MessageContent.Text("Latest answer"), false, 2, interactionId = "test-id",
            sseComplete = true, feedbackEligible = true,
            ctaButton = CtaButton("Background CTA", "https://example.com"),
            promptSuggestions = listOf("Background suggestion")
        )
    )

    @Test
    @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
    fun modal_keyboardCanSelectCategoryAndSubmitAfterBackdropTap() {
        showChat(
            "modal",
            messages = conversation(),
            feedbackConfig = ""","showCancelButton":false,"showCloseButton":false""",
            themeConfig = ""","arrays":{"feedback.positive.options":["Modal category"]},"components":{"feedback":{"positiveNotesEnabled":false}}"""
        )
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("modal category") { nodeWithText("Modal category") != null }
        val categoryBounds = Rect()
        requireNotNull(nodeWithText("Modal category")).getBoundsInScreen(categoryBounds)
        tap(4 * instrumentation.targetContext.resources.displayMetrics.density, categoryBounds.exactCenterY())
        scenario.onActivity { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        for (target in 1..12) {
            if (submittedFeedback != null) break
            repeat(target) { instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_TAB) }
            instrumentation.waitForIdleSync()
            instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DPAD_CENTER)
            instrumentation.waitForIdleSync()
        }
        await("keyboard feedback submission") { submittedFeedback != null }
        assertEquals(listOf("Modal category"), submittedFeedback?.selectedCategories)
        scenario.onActivity {
            assertNull(feedback.value)
            assertTrue(backgroundEvents.isEmpty())
        }
    }

    @Test
    fun modal_veryShortHeight_keepsSubmitAndCancelInsideViewport() {
        showChat("modal", 150.dp)
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.NEGATIVE) }
        await("short feedback modal") { nodeWithText("Cancel") != null }
        for (text in listOf("Submit", "Cancel")) {
            val bounds = Rect()
            var button = requireNotNull(nodeWithText(text))
            while (!button.isClickable) button = requireNotNull(button.parent)
            button.getBoundsInScreen(bounds)
            assertTrue("$text must be entirely visible in a 150dp host: $bounds, hostBottom=$hostBottom", bounds.bottom <= hostBottom)
            assertTrue(bounds.height() >= 48 * instrumentation.targetContext.resources.displayMetrics.density)
        }
    }

    @Test
    fun modal_notesKeyboard_backdropDismissesAndSubmissionPreservesNotes() {
        verifyNotesKeyboardDismissal()
    }

    @Test
    fun modal_withoutBackDispatcher_mountsAndCancelDismissesFeedback() {
        showChat("modal", withoutBackDispatcher = true)
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("modal without Back dispatcher") { nodeWithText("Cancel") != null }
        var cancel = requireNotNull(nodeWithText("Cancel"))
        while (!cancel.isClickable) cancel = requireNotNull(cancel.parent)
        assertTrue(cancel.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("dismissed modal without Back dispatcher") {
            feedback.value == null && nodeWithText("Concierge") != null
        }
    }

    private fun verifyNotesKeyboardDismissal() {
        showChat("modal")
        scenario.onActivity { feedback.value = Feedback("test-id", FeedbackType.POSITIVE) }
        await("modal feedback") { nodeWithText("Your feedback is appreciated") != null }
        val editorBounds = revealNotesEditor()
        tap(editorBounds.exactCenterX(), editorBounds.exactCenterY())
        await("notes keyboard (editor=$editorBounds)") { isKeyboardVisible() }
        revealNotesEditor()

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "Review this feedback")
        }
        assertTrue(requireNotNull(findNode { it.isEditable }).performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments))
        await("entered notes") { findNode { it.isEditable }?.text?.toString() == "Review this feedback" }
        val resizedEditorBounds = revealNotesEditor()
        val visibleBounds = Rect()
        scenario.onActivity { it.window.decorView.getWindowVisibleDisplayFrame(visibleBounds) }
        assertTrue("Notes must remain usable above the keyboard: $resizedEditorBounds, $visibleBounds",
            visibleBounds.contains(resizedEditorBounds))
        for (label in listOf("Submit", "Cancel")) {
            val bounds = Rect()
            var button = requireNotNull(nodeWithText(label))
            while (!button.isClickable) button = requireNotNull(button.parent)
            button.getBoundsInScreen(bounds)
            assertTrue(
                "$label must remain above the keyboard: button=$bounds, visible=$visibleBounds",
                visibleBounds.contains(bounds)
            )
        }

        val cancelBounds = Rect()
        requireNotNull(nodeWithText("Cancel")).getBoundsInScreen(cancelBounds)
        val backdropInset = instrumentation.targetContext.resources.displayMetrics.density * 4
        // Tap beside the card after keyboard resize, not at a stale background-control position.
        val backdropX = visibleBounds.left + backdropInset
        val backdropY = cancelBounds.exactCenterY()
        tap(backdropX, backdropY)
        await("dismissed notes keyboard") { !isKeyboardVisible() }
        scenario.onActivity { assertNotNull("Backdrop tap must not dismiss feedback", feedback.value) }
        assertEquals("Review this feedback", findNode { it.isEditable }?.text?.toString())

        var submit = requireNotNull(nodeWithText("Submit"))
        while (!submit.isClickable) submit = requireNotNull(submit.parent)
        assertTrue(submit.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        await("feedback submission") { submittedFeedback != null }
        assertEquals("Review this feedback", submittedFeedback?.notes)
    }

    private fun revealNotesEditor(): Rect {
        val bounds = Rect()
        var previousBounds: Rect? = null
        await("visible notes editor") {
            val editor = findNode { it.isEditable }
            editor?.getBoundsInScreen(bounds)
            val visibleBounds = Rect()
            scenario.onActivity { it.window.decorView.getWindowVisibleDisplayFrame(visibleBounds) }
            if (editor?.isVisibleToUser == true &&
                visibleBounds.contains(bounds) &&
                bounds.height() >= 48 * instrumentation.targetContext.resources.displayMetrics.density
            ) {
                val settled = bounds == previousBounds
                previousBounds = Rect(bounds)
                settled
            } else {
                previousBounds = null
                if (editor?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id) != true) {
                    findNode { it.isScrollable }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
                instrumentation.uiAutomation.waitForIdle(100, 5_000)
                false
            }
        }
        return bounds
    }

    private fun tap(x: Float, y: Float) {
        val eventTime = SystemClock.uptimeMillis()
        for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
            val event = MotionEvent.obtain(
                eventTime, SystemClock.uptimeMillis(), action,
                x, y, 0
            ).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            try {
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
        }
    }

    private fun isKeyboardVisible(): Boolean {
        var visible = false
        scenario.onActivity {
            visible = ViewCompat.getRootWindowInsets(it.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        return visible
    }

    private fun showChat(
        displayMode: String,
        height: Dp? = null,
        messages: List<ChatMessage> = emptyList(),
        feedbackConfig: String = "",
        themeConfig: String = "",
        withoutBackDispatcher: Boolean = false
    ) {
        val json = """{"behavior":{"feedback":{"displayMode":"$displayMode"$feedbackConfig}}$themeConfig}"""
        val theme = ConciergeThemeData(
            config = requireNotNull(ThemeParser.parseThemeJson(json)),
            tokens = requireNotNull(ThemeParser.parseThemeTokens(json))
        )
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            val ownerlessView = FrameLayout(activity.applicationContext)
            // Match the production ConciergeChat dialog's keyboard-resize policy.
            @Suppress("DEPRECATION")
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            activity.setContent {
                inputModeManager = LocalInputModeManager.current
                ConciergeTheme(theme = theme) {
                    CompositionLocalProvider(
                        LocalImageProvider provides DefaultImageProvider(),
                        LocalActivityResultRegistryOwner provides activity,
                        LocalView provides if (withoutBackDispatcher) ownerlessView else LocalView.current,
                        LocalContext provides if (withoutBackDispatcher) activity.applicationContext else LocalContext.current
                    ) {
                        if (withoutBackDispatcher) assertNull(LocalOnBackPressedDispatcherOwner.current)
                        Box(modifier = (if (height != null) Modifier.height(height) else Modifier)
                            .onGloballyPositioned { hostBottom = it.boundsInWindow().bottom }) {
                        ConciergeChat(
                            messages = messages,
                            chatState = ChatScreenState.Idle(feedback.value),
                            isInputEmpty = true,
                            inputStateFlow = composerInput,
                            hasAudioPermission = true,
                            showWelcomeCard = true,
                            welcomeConfig = WelcomeConfig(),
                            isReturningUser = false,
                            onTextChanged = { composerInput.value = UserInputState.Editing(it) },
                            onEvent = { event ->
                                if (event !is FeedbackEvent.SubmitFeedback && event != FeedbackEvent.DismissFeedbackDialog) {
                                    backgroundEvents.add(event)
                                }
                                if (event == FeedbackEvent.DismissFeedbackDialog) feedback.value = null
                                if (event is FeedbackEvent.SubmitFeedback) {
                                    submittedFeedback = event.feedback
                                    feedback.value = null
                                }
                            },
                            onPermissionResult = {},
                            onClose = { closeCalls++ }
                        )
                        }
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
            if (!node.refresh()) return null
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
        val visibleBounds = Rect()
        var imeInsets: WindowInsetsCompat? = null
        var decorHeight = 0
        scenario.onActivity {
            it.window.decorView.getWindowVisibleDisplayFrame(visibleBounds)
            imeInsets = ViewCompat.getRootWindowInsets(it.window.decorView)
            decorHeight = it.window.decorView.height
        }
        val inputMethod = instrumentation.uiAutomation.executeShellCommand("dumpsys input_method")
        val keyboardState = android.os.ParcelFileDescriptor.AutoCloseInputStream(inputMethod)
            .bufferedReader().useLines { lines ->
                lines.filter { it.contains("mInputShown") || it.contains("mIsInputViewShown") }.joinToString()
            }
        fail(
            "Timed out waiting for $description; visibleBounds=$visibleBounds, decorHeight=$decorHeight, hostBottom=$hostBottom, " +
                "imeVisible=${imeInsets?.isVisible(WindowInsetsCompat.Type.ime())}, " +
                "imeInsets=${imeInsets?.getInsets(WindowInsetsCompat.Type.ime())}, " +
                "editorFocused=${findNode { it.isEditable }?.isFocused}, $keyboardState"
        )
    }
}
