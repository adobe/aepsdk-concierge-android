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

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.test.ComposeTestUtils
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal object MessageListLifecycleHarness {
    val messages = mutableStateOf<List<ChatMessage>>(emptyList())
    @Volatile var listState: LazyListState? = null
    @Volatile var activityCreations = 0

    fun reset() {
        messages.value = emptyList()
        listState = null
        activityCreations = 0
    }
}

class MessageListLifecycleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        synchronized(MessageListLifecycleHarness) { MessageListLifecycleHarness.activityCreations++ }
        setContent {
            val listState = rememberLazyListState()
            SideEffect { MessageListLifecycleHarness.listState = listState }
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(
                        modifier = Modifier.fillMaxSize(),
                        messages = MessageListLifecycleHarness.messages.value,
                        listState = listState
                    )
                }
            }
        }
    }
}

class MessageListLifecycleTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @After
    fun resetHarness() {
        MessageListLifecycleHarness.reset()
    }

    private fun history(): List<ChatMessage> = List(16) { index ->
        ComposeTestUtils.createTextMessage(
            "History $index. Previous conversation content.",
            timestamp = index.toLong()
        ).copy(turnId = "history-$index")
    }

    private fun populateConversation() {
        val turn = "handoff-turn"
        MessageListLifecycleHarness.messages.value = history() + listOf(
            ComposeTestUtils.createTextMessage("Opening the menu", timestamp = 20).copy(turnId = turn),
            ComposeTestUtils.createTextMessage("Choose an item", timestamp = 21).copy(turnId = turn)
        )
    }

    private fun state(): LazyListState = requireNotNull(MessageListLifecycleHarness.listState)

    private fun position(scenario: ActivityScenario<MessageListLifecycleActivity>): Pair<Int, Int> {
        var position = -1 to -1
        scenario.onActivity { position = state().firstVisibleItemIndex to state().firstVisibleItemScrollOffset }
        return position
    }

    /** Waits for a list state other than [previous] that has laid out and finished scrolling. */
    private fun awaitFreshSettledState(previous: LazyListState?) = awaitActivity {
        val current = MessageListLifecycleHarness.listState
        current != null && current !== previous && current.layoutInfo.viewportSize.height > 0 &&
            !current.isScrollInProgress
    }

    private fun awaitActivity(predicate: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (System.nanoTime() < deadline) {
            instrumentation.waitForIdleSync()
            if (predicate()) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for activity lifecycle condition")
    }

    private fun scrollToHistory(
        scenario: ActivityScenario<MessageListLifecycleActivity>,
        anchorIndex: Int
    ): Pair<Int, Int> {
        scenario.onActivity { activity ->
            activity.lifecycleScope.launch {
                state().scrollToItem(anchorIndex - 12)
            }
        }
        awaitActivity { state().firstVisibleItemIndex < anchorIndex }
        return state().firstVisibleItemIndex to state().firstVisibleItemScrollOffset
    }

    @Test
    fun rotationPreservesManualHistoryPositionAndTurnUpdatesDoNotPullViewport() {
        MessageListLifecycleHarness.reset()
        populateConversation()
        val scenario = ActivityScenario.launch(MessageListLifecycleActivity::class.java)
        try {
            awaitActivity { MessageListLifecycleHarness.listState != null }
            val anchorIndex = history().size
            awaitActivity { state().firstVisibleItemIndex == anchorIndex }
            val manualPosition = scrollToHistory(scenario, anchorIndex)
            val original = state()
            scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            awaitActivity {
                MessageListLifecycleHarness.activityCreations > 1 &&
                    scenarioOrientation(scenario) == Configuration.ORIENTATION_LANDSCAPE
            }
            awaitFreshSettledState(original)

            assertEquals(manualPosition, position(scenario))
            MessageListLifecycleHarness.messages.value =
                MessageListLifecycleHarness.messages.value.dropLast(1) +
                    ComposeTestUtils.createTextMessage("Choose an item, with streamed details", timestamp = 22)
                        .copy(turnId = "handoff-turn")
            instrumentation.waitForIdleSync()
            assertEquals(manualPosition, position(scenario))
        } finally {
            scenario.close()
        }
    }

    @Test
    fun backgroundResumePreservesBrowsePositionAndActivityReopenAnchorsCurrentTurn() {
        MessageListLifecycleHarness.reset()
        populateConversation()
        var scenario = ActivityScenario.launch(MessageListLifecycleActivity::class.java)
        try {
            val anchorIndex = history().size
            awaitActivity { state().firstVisibleItemIndex == anchorIndex }
            val manualPosition = scrollToHistory(scenario, anchorIndex)

            scenario.moveToState(Lifecycle.State.CREATED)
            MessageListLifecycleHarness.messages.value =
                MessageListLifecycleHarness.messages.value +
                    ComposeTestUtils.createTextMessage("A later row in the same handoff", timestamp = 23)
                        .copy(turnId = "handoff-turn")
            scenario.moveToState(Lifecycle.State.RESUMED)
            instrumentation.waitForIdleSync()
            assertEquals(manualPosition, position(scenario))

            scenario.close()
            MessageListLifecycleHarness.listState = null
            scenario = ActivityScenario.launch(MessageListLifecycleActivity::class.java)
            awaitFreshSettledState(null)
            awaitActivity { state().firstVisibleItemIndex == anchorIndex }
            assertEquals(anchorIndex, position(scenario).first)
        } finally {
            scenario.close()
        }
    }

    private fun scenarioOrientation(scenario: ActivityScenario<MessageListLifecycleActivity>): Int {
        var orientation = Configuration.ORIENTATION_UNDEFINED
        scenario.onActivity { orientation = it.resources.configuration.orientation }
        return orientation
    }
}
