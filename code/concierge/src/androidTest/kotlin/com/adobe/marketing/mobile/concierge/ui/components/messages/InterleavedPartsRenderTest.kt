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

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.concierge.network.ConversationResponseParser
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.ConversationPartsRenderer
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a response whose parts the backend interleaved — text, product cards, a follow-up
 * question, then prompt suggestions — and asserts the screen shows them in that order.
 *
 * The message list is built from the raw payload through the production parser and renderer, so
 * this covers the whole client path from JSON to pixels for a payload shape the live service does
 * not send yet.
 */
class InterleavedPartsRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val interleavedPayload = """
        {
          "handle": [
            {
              "type": "brand-concierge:conversation",
              "payload": [
                {
                  "interactionId": "interaction-1",
                  "response": {
                    "message": "Here are some shoes:\n\nWhat width do you need?",
                    "multimodalElements": {
                      "elements": [
                        { "type": "text", "text": "Here are some shoes:" },
                        {
                          "id": "card-1",
                          "entity_info": {
                            "productName": "Aero Runner 5",
                            "productPrice": "$144.99"
                          }
                        },
                        { "type": "text", "text": "What width do you need?" }
                      ]
                    },
                    "promptSuggestions": ["Medium (standard)", "Wide"]
                  },
                  "state": "completed"
                }
              ]
            }
          ]
        }
    """.trimIndent()

    @Test
    fun interleavedResponse_rendersTextThenCardsThenTextThenSuggestions() {
        val parsed = ConversationResponseParser.parseConversationData(interleavedPayload).first()
        val renderables = ConversationPartsRenderer.buildRenderables(parsed.parts)
        val suggestions = ConversationPartsRenderer.suggestionsOf(parsed.parts)

        // Text, cards, text — suggestions attach to the last message rather than forming one.
        assertEquals(3, renderables.size)

        val messages = renderables.mapIndexed { index, content ->
            ChatMessage(
                content = content,
                isFromUser = false,
                timestamp = 1_000L + index,
                sseComplete = true,
                promptSuggestions = if (index == renderables.lastIndex) suggestions else emptyList()
            )
        }

        composeTestRule.setContent {
            ConciergeTheme {
                CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
                    MessageList(messages = messages)
                }
            }
        }

        composeTestRule.onNodeWithText("Here are some shoes:").assertIsDisplayed()
        composeTestRule.onNodeWithText("Aero Runner 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("What width do you need?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Wide").assertIsDisplayed()

        val introTop = composeTestRule.onNodeWithText("Here are some shoes:").getUnclippedBoundsInRoot().top
        val cardTop = composeTestRule.onNodeWithText("Aero Runner 5").getUnclippedBoundsInRoot().top
        val followUpTop = composeTestRule.onNodeWithText("What width do you need?").getUnclippedBoundsInRoot().top
        val suggestionTop = composeTestRule.onNodeWithText("Wide").getUnclippedBoundsInRoot().top

        assertTrue("intro text should render above the cards", introTop < cardTop)
        assertTrue("follow-up question should render below the cards", cardTop < followUpTop)
        assertTrue("suggestions should render below the follow-up question", followUpTop < suggestionTop)

        captureScreenshot()
    }

    private fun captureScreenshot() {
        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        FileOutputStream(File(dir, "interleaved-parts.png")).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
