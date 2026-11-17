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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.cards.NativeCardController
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NativeCardMessageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun disposedCardIsNotRenderedAgainWhileItsRowIsStillComposed() {
        val contexts = mutableListOf<Map<String, Any?>>()
        val renderer: ConciergeCardRenderer = { _, context, _ -> contexts += context }
        val controller = NativeCardController(
            scope = scope,
            cardIdGenerator = { "native-card" },
            rendererLookup = { rendererId -> renderer.takeIf { rendererId == "example.card" } }
        )
        composeTestRule.runOnUiThread {
            val result = controller.activate(
                NativeCardController.ActivationRequest(
                    element = mapOf("type" to "custom", "rendererId" to "example.card", "id" to "one"),
                    rendererId = "example.card",
                    turnId = "turn-1",
                    position = 0
                )
            )
            assertTrue(result is NativeCardController.ActivationResult.Activated)
        }
        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalNativeCardController provides controller) {
                    NativeCardMessage(cardId = "native-card")
                }
            }
        }
        composeTestRule.runOnUiThread {
            requireNotNull(controller.renderableCard("native-card")).saveState(mapOf("status" to "draft"))
        }
        composeTestRule.waitForIdle()
        assertEquals(mapOf("status" to "draft"), contexts.last()["state"])
        val renderCountBeforeDisposal = contexts.size

        // The row stays composed, as it does for the frame between disposal and its removal.
        composeTestRule.runOnUiThread { controller.disposeAll() }
        composeTestRule.waitForIdle()

        assertEquals(renderCountBeforeDisposal, contexts.size)
        composeTestRule.onNodeWithTag(NATIVE_CARD_TEST_TAG).assertDoesNotExist()
    }
}
