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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.cards.NativeCardController
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import com.adobe.marketing.mobile.concierge.ui.test.ComposeTestUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the MessageList composable.
 */
class MessageListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var listState: LazyListState
    private var rowSpacing = 0
    private val viewportHeight = mutableStateOf(320.dp)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun text(text: String, turn: String, user: Boolean = false) =
        ComposeTestUtils.createTextMessage(text, isFromUser = user).copy(turnId = turn, sseComplete = true)

    private fun history() = List(12) { text("History $it\nEarlier conversation.\nMore history.", "history-$it") }

    private fun showTranscript(
        messages: State<List<ChatMessage>>,
        controller: NativeCardController? = null
    ) {
        composeTestRule.setContent {
            ConciergeTheme {
                listState = rememberLazyListState()
                rowSpacing = with(LocalDensity.current) { ConciergeStyles.messageListStyle.verticalSpacing.roundToPx() }
                CompositionLocalProvider(
                    LocalImageProvider provides DefaultImageProvider(),
                    LocalNativeCardController provides controller
                ) {
                    MessageList(
                        modifier = Modifier.fillMaxWidth().height(viewportHeight.value),
                        messages = messages.value,
                        listState = listState
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun assertAnchoredAt(index: Int) {
        composeTestRule.runOnIdle {
            assertEquals(index, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
    }

    private fun showRestorableTranscript(messages: State<List<ChatMessage>>): StateRestorationTester {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            ConciergeTheme {
                listState = rememberLazyListState()
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(
                        modifier = Modifier.fillMaxWidth().height(viewportHeight.value),
                        messages = messages.value,
                        listState = listState
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        return restorationTester
    }

    @Test
    fun messageList_handoffAndTypedTurnsTopAlignWithNormalSpacing() {
        val messages = mutableStateOf(history())
        showTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle {
            messages.value += listOf(text("Typed prompt", "typed", user = true), text("Reply", "typed"))
        }
        assertAnchoredAt(start)

        composeTestRule.runOnIdle {
            messages.value += listOf(
                text("Opening menu", "handoff"),
                text("Choose an option", "handoff"),
                ChatMessage(MessageContent.CtaButton(CtaButton("Continue", "https://example.com")), false, 0, turnId = "handoff")
            )
        }
        assertAnchoredAt(start + 2)
        composeTestRule.runOnIdle {
            val rows = listState.layoutInfo.visibleItemsInfo.filter { it.index >= start + 2 }
            assertEquals(3, rows.size)
            rows.zipWithNext().forEach { (previous, next) ->
                assertEquals(rowSpacing, next.offset - previous.offset - previous.size)
            }
        }
    }

    @Test
    fun messageList_noLocalMessageKeepsAnchorWhenNativeCardsReplaceThinkingRow() {
        val renderer: ConciergeCardRenderer = { _, _, _ -> Text("Menu card", Modifier.height(48.dp)) }
        var cardSequence = 0
        val controller = NativeCardController(
            scope,
            cardIdGenerator = { "card-${++cardSequence}" },
            rendererLookup = { renderer }
        )
        composeTestRule.runOnUiThread {
            repeat(2) { position ->
                controller.activate(
                    NativeCardController.ActivationRequest(
                        element = mapOf("type" to "custom", "rendererId" to "menu"),
                        rendererId = "menu", turnId = "native-turn", position = position
                    )
                )
            }
        }
        val messages = mutableStateOf(history())
        showTranscript(messages, controller)
        val start = messages.value.size
        composeTestRule.runOnIdle {
            messages.value += text("", "native-turn").copy(sseComplete = false)
        }
        assertAnchoredAt(start)
        composeTestRule.runOnIdle {
            messages.value = messages.value.take(start) + listOf(
                ChatMessage(MessageContent.NativeCard("card-1"), false, 0, turnId = "native-turn"),
                ChatMessage(MessageContent.NativeCard("card-2"), false, 0, turnId = "native-turn"),
                ChatMessage(MessageContent.CtaButton(CtaButton("Continue", "https://example.com")), false, 0, turnId = "native-turn")
            )
        }
        assertAnchoredAt(start)
        composeTestRule.onAllNodesWithText("Menu card").assertCountEquals(2)

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)
        composeTestRule.runOnIdle {
            requireNotNull(controller.renderableCard("card-1")).saveState(mapOf("selected" to true))
        }
        composeTestRule.runOnIdle {
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun messageList_textOnlyHandoffStreamsWithoutOverridingManualScroll() {
        val messages = mutableStateOf(history())
        showTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle { messages.value += text("", "handoff").copy(sseComplete = false) }
        assertAnchoredAt(start)
        composeTestRule.runOnIdle {
            messages.value = messages.value.dropLast(1) + messages.value.last().copy(content = MessageContent.Text("Response"))
        }
        assertAnchoredAt(start)

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)
        composeTestRule.runOnIdle {
            messages.value = messages.value.dropLast(1) + messages.value.last().copy(
                content = MessageContent.Text("Response with more streamed text"),
                promptSuggestions = listOf("Tell me more"), sseComplete = true
            )
        }
        composeTestRule.runOnIdle {
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun messageList_viewportResizeAndPlaceholderReplacementRespectManualScroll() {
        val messages = mutableStateOf(history())
        showTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle {
            viewportHeight.value = 220.dp
            messages.value += text("", "handoff").copy(sseComplete = false)
        }
        assertAnchoredAt(start)

        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)
        composeTestRule.runOnIdle {
            messages.value = messages.value.take(start) + listOf(
                text("Completed response", "handoff"),
                ChatMessage(
                    MessageContent.CtaButton(CtaButton("Continue", "https://example.com")),
                    false, 0, turnId = "handoff"
                )
            )
        }
        composeTestRule.runOnIdle {
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun messageList_restorationPreservesManualScrollAndStillNavigatesNewTurns() {
        val messages = mutableStateOf(history())
        val restorationTester = showRestorableTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle {
            messages.value += listOf(text("Opening menu", "handoff"), text("", "handoff"))
        }
        assertAnchoredAt(start)
        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)

        val original = listState
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.runOnIdle {
            assertNotSame(original, listState)
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
            messages.value = messages.value.dropLast(1) + text("Completed response", "handoff")
        }
        composeTestRule.runOnIdle {
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
            messages.value += text("New request", "next", user = true)
        }
        assertAnchoredAt(start + 2)
    }

    @Test
    fun messageList_restorationCompletesNavigationInterruptedByDisposal() {
        val messages = mutableStateOf(history())
        val shown = mutableStateOf(true)
        var registry = SaveableStateRegistry(null) { true }
        val registryState = mutableStateOf(registry)
        var disposals = 0
        composeTestRule.setContent {
            if (shown.value) {
                CompositionLocalProvider(LocalSaveableStateRegistry provides registryState.value) {
                    ConciergeTheme {
                        listState = rememberLazyListState()
                        DisposableEffect(Unit) { onDispose { disposals++ } }
                        CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                            MessageList(
                                modifier = Modifier.fillMaxWidth().height(viewportHeight.value),
                                messages = messages.value,
                                listState = listState
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { scope.launch { listState.scrollToItem(0) } }
        composeTestRule.waitForIdle()
        val start = messages.value.size
        val original = listState

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.runOnUiThread { messages.value += text("Opening menu", "handoff") }
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.runOnUiThread { assertTrue(original.isScrollInProgress) }
            composeTestRule.runOnUiThread {
                val saved = registry.performSave()
                registry = SaveableStateRegistry(saved) { true }
                shown.value = false
            }
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.runOnUiThread {
                assertEquals(1, disposals)
                registryState.value = registry
                shown.value = true
            }
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
        composeTestRule.runOnIdle { assertNotSame(original, listState) }
        assertAnchoredAt(start)
    }

    @Test
    fun messageList_manualDragCancelsInterruptedNavigationAfterRestoration() {
        val messages = mutableStateOf(history())
        val restorationTester = showRestorableTranscript(messages)
        composeTestRule.runOnIdle { scope.launch { listState.scrollToItem(6) } }
        composeTestRule.waitForIdle()
        val start = messages.value.size

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.runOnIdle { messages.value += text("Opening menu", "handoff") }
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.runOnIdle { assertTrue(listState.isScrollInProgress) }
            composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)
        val original = listState
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.runOnIdle {
            assertNotSame(original, listState)
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun messageList_turnArrivingDuringDragDoesNotMoveViewportAfterRestoration() {
        val messages = mutableStateOf(history())
        val restorationTester = showRestorableTranscript(messages)
        val start = messages.value.size
        val list = composeTestRule.onNode(hasScrollAction())

        list.performTouchInput {
            down(center)
            moveBy(Offset(0f, 150f))
        }
        composeTestRule.runOnIdle { messages.value += text("Opening menu", "handoff") }
        composeTestRule.waitForIdle()
        list.performTouchInput {
            moveBy(Offset(0f, 150f))
            up()
        }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)

        val original = listState
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.runOnIdle {
            assertNotSame(original, listState)
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun messageList_newTurnNavigatesDespiteRepeatedTextAndReusedNumericIndexAfterReset() {
        val messages = mutableStateOf(history())
        showTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle { messages.value += text("Opening menu", "first") }
        assertAnchoredAt(start)
        composeTestRule.runOnIdle { messages.value += text("Opening menu", "second") }
        assertAnchoredAt(start + 1)

        composeTestRule.runOnIdle { messages.value = emptyList() }
        composeTestRule.runOnIdle {
            messages.value = history() + text("Opening menu", "after-reset")
        }
        assertAnchoredAt(start)
    }

    @Test
    fun messageList_rollbackAndReappearingTurnDoNotNavigateBackward() {
        val messages = mutableStateOf(history())
        showTranscript(messages)
        val start = messages.value.size
        composeTestRule.runOnIdle { messages.value += text("", "handoff").copy(sseComplete = false) }
        assertAnchoredAt(start)
        composeTestRule.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        assertTrue(before.first < start)

        fun assertUnmoved() = composeTestRule.runOnIdle {
            assertEquals(before, listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset)
        }
        composeTestRule.runOnIdle { messages.value = messages.value.dropLast(1) }
        assertUnmoved()
        composeTestRule.runOnIdle { messages.value += text("Menu card", "handoff") }
        assertUnmoved()
        composeTestRule.runOnIdle { messages.value = messages.value.dropLast(1) }
        assertUnmoved()

        composeTestRule.runOnIdle { messages.value += text("Next request", "next", user = true) }
        assertAnchoredAt(start)
    }

    @Test
    fun messageList_noticeAppearsOnFirstSendAndResetsWithHistory() {
        val messages = mutableStateOf(emptyList<ChatMessage>())
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(
                        messages = messages.value,
                        privacyNotice = PrivacyNoticeConfig(text = "Conversation privacy.")
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Privacy Notice").assertDoesNotExist()
        composeTestRule.runOnIdle {
            messages.value = listOf(ComposeTestUtils.createTextMessage("Agent handoff"))
        }
        composeTestRule.onNodeWithText("Privacy Notice").assertDoesNotExist()
        composeTestRule.runOnIdle {
            messages.value = messages.value + ComposeTestUtils.createTextMessage("First query", isFromUser = true)
        }
        composeTestRule.onNodeWithText("Conversation privacy.").assertIsDisplayed()
        composeTestRule.onNodeWithText("First query").assertIsDisplayed()
        val noticeBounds = composeTestRule.onNodeWithText("Conversation privacy.").getUnclippedBoundsInRoot()
        val queryBounds = composeTestRule.onNodeWithText("First query").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(noticeBounds.bottom <= queryBounds.top)

        composeTestRule.runOnIdle {
            messages.value = messages.value + ComposeTestUtils.createTextMessage("Second query", isFromUser = true)
        }
        composeTestRule.onNode(hasScrollAction()).performScrollToNode(hasText("Privacy Notice"))
        composeTestRule.onAllNodesWithText("Privacy Notice").assertCountEquals(1)
        composeTestRule.runOnIdle { messages.value = emptyList() }
        composeTestRule.onNodeWithText("Privacy Notice").assertDoesNotExist()
        composeTestRule.runOnIdle {
            messages.value = listOf(ComposeTestUtils.createTextMessage("New conversation", isFromUser = true))
        }
        composeTestRule.onNodeWithText("Conversation privacy.").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Privacy Notice").assertCountEquals(1)
    }

    @Test
    fun messageList_displaysMessages() {
        val messages = listOf(
            ComposeTestUtils.createTextMessage("Hello", isFromUser = true),
            ComposeTestUtils.createTextMessage("Hi there!", isFromUser = false)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hi there!").assertIsDisplayed()
    }

    @Test
    fun messageList_emptyList_rendersWithoutCrashing() {
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = emptyList())
                }
            }
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun messageList_singleMessage_displaysContent() {
        val messages = listOf(
            ComposeTestUtils.createTextMessage("Single message", isFromUser = false)
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Single message").assertIsDisplayed()
    }

    @Test
    fun messageList_withCitations_displaysMessageContent() {
        val citations = listOf(
            ComposeTestUtils.createCitation(url = "https://example.com/1", title = "Source One"),
            ComposeTestUtils.createCitation(url = "https://example.com/2", title = "Source Two")
        )
        val messages = listOf(
            ComposeTestUtils.createMessageWithCitations(
                text = "Answer with sources [1][2].",
                citations = citations,
                isFromUser = false
            )
        )

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Answer with sources", substring = true).assertIsDisplayed()
        // Citation numbers may be rendered as inline content and not exposed as separate text nodes
    }
}
