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

import android.content.ContextWrapper
import android.content.res.AssetManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.concierge.network.Citation
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.components.image.assetBitmapCache
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.CSSKeyMapper
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeIconAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MessagePaddingRegressionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun clearCache() {
        assetBitmapCache.clear()
    }

    @Test
    fun bundledLocalIconLoadsWithConfiguredContentInset() {
        val base = ConciergeThemeTokens(
            assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = "padding-company"))
        )
        val tokens = CSSKeyMapper.apply("--message-padding", "8px 16px", base)
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        composeTestRule.setContent {
            val baseContext = LocalContext.current
            val context = object : ContextWrapper(baseContext) {
                override fun getAssets(): AssetManager = assets
            }
            ConciergeTheme(theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)) {
                CompositionLocalProvider(LocalContext provides context) {
                    ChatMessageItem(
                        ChatMessage(content = MessageContent.Text("Agent reply"), isFromUser = false, timestamp = 0)
                    )
                }
            }
        }
        composeTestRule.waitUntil(5_000) {
            composeTestRule.onAllNodesWithTag("MessageCompanyIcon").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("MessageCompanyIcon").assertIsDisplayed()
        assertEquals(
            67f,
            composeTestRule.onNodeWithText("Agent reply").getUnclippedBoundsInRoot().left.value,
            1f
        )
    }

    @Test
    fun mixedTextAndFooterMoveTogetherWhileCardsPromptsAndCtasStayPut() {
        val padding = mutableStateOf<String?>(null)
        val base = ConciergeThemeTokens(
            assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = "https://example.invalid/icon.png"))
        )
        val message = ChatMessage(
            content = MessageContent.Mixed(
                text = "Agent reply",
                multimodalElements = listOf(MultimodalElement(id = "p1", title = "Product One"))
            ),
            isFromUser = false,
            timestamp = 0,
            citations = listOf(Citation(url = "https://example.com", title = "Source")),
            promptSuggestions = listOf("Tell me more"),
            ctaButton = CtaButton(label = "Continue", url = "https://example.com")
        )
        composeTestRule.setContent {
            val tokens = padding.value?.let { CSSKeyMapper.apply("--message-padding", it, base) } ?: base
            ConciergeTheme(theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)) {
                CompositionLocalProvider(LocalImageProvider provides FailingPaddingImageProvider) {
                    ChatMessageItem(message)
                }
            }
        }

        fun bounds(text: String) = composeTestRule.onNodeWithText(text).getUnclippedBoundsInRoot()
        val oldText = bounds("Agent reply")
        val oldSources = bounds("Sources")
        val oldProduct = bounds("Product One")
        val oldPrompt = bounds("Tell me more")
        val oldCta = bounds("Continue")
        composeTestRule.runOnIdle { padding.value = "8px 16px" }

        assertEquals(16f, (bounds("Agent reply").left - oldText.left).value, 1f)
        assertEquals(16f, (bounds("Sources").left - oldSources.left).value, 1f)
        assertEquals(oldProduct.left, bounds("Product One").left)
        assertEquals(oldPrompt.left, bounds("Tell me more").left)
        assertEquals(oldCta.left, bounds("Continue").left)
    }

    @Test
    fun footerOnlyMixedMessageReceivesConfiguredLeftPadding() {
        val padding = mutableStateOf<String?>(null)
        val base = ConciergeThemeTokens(
            assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = "https://example.invalid/icon.png"))
        )
        composeTestRule.setContent {
            val tokens = padding.value?.let { CSSKeyMapper.apply("--message-padding", it, base) } ?: base
            ConciergeTheme(theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)) {
                ChatMessageItem(
                    ChatMessage(
                        content = MessageContent.Mixed(""),
                        isFromUser = false,
                        timestamp = 0,
                        citations = listOf(Citation(url = "https://example.com", title = "Source"))
                    )
                )
            }
        }

        val before = composeTestRule.onNodeWithText("Sources").getUnclippedBoundsInRoot()
        composeTestRule.runOnIdle { padding.value = "8px 16px" }
        val after = composeTestRule.onNodeWithText("Sources").getUnclippedBoundsInRoot()
        assertEquals(16f, (after.left - before.left).value, 1f)
        assertEquals(8f, (after.top - before.top).value, 1f)
    }

    @Test
    fun configuredPaddingDoesNotChangeUserBubble() {
        val padding = mutableStateOf<String?>(null)
        composeTestRule.setContent {
            val base = ConciergeThemeTokens()
            val tokens = padding.value?.let { CSSKeyMapper.apply("--message-padding", it, base) } ?: base
            ConciergeTheme(theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)) {
                Box(Modifier.width(360.dp).testTag("message")) {
                    ChatMessageItem(
                        ChatMessage(content = MessageContent.Text("User query"), isFromUser = true, timestamp = 0)
                    )
                }
            }
        }
        val before = composeTestRule.onNodeWithText("User query").getUnclippedBoundsInRoot()
        val oldBubble = composeTestRule.onNodeWithTag("message").getUnclippedBoundsInRoot()
        composeTestRule.runOnIdle { padding.value = "40px 50px" }
        composeTestRule.onNodeWithText("User query").assertIsDisplayed()
        assertEquals(before, composeTestRule.onNodeWithText("User query").getUnclippedBoundsInRoot())
        assertEquals(oldBubble, composeTestRule.onNodeWithTag("message").getUnclippedBoundsInRoot())
    }

    @Test
    fun configuredPaddingDoesNotChangeThinkingBubble() {
        val padding = mutableStateOf<String?>(null)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            val base = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = "https://example.invalid/icon.png"))
            )
            val tokens = padding.value?.let { CSSKeyMapper.apply("--message-padding", it, base) } ?: base
            ConciergeTheme(theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)) {
                CompositionLocalProvider(LocalImageProvider provides FailingPaddingImageProvider) {
                    Box(Modifier.width(360.dp).testTag("message")) {
                        ChatMessageItem(
                            ChatMessage(content = MessageContent.Text(""), isFromUser = false, timestamp = 0)
                        )
                    }
                }
            }
        }
        composeTestRule.mainClock.advanceTimeByFrame()
        val before = composeTestRule.onNodeWithTag("message").getUnclippedBoundsInRoot()
        composeTestRule.runOnIdle { padding.value = "40px 50px" }
        composeTestRule.mainClock.advanceTimeByFrame()
        assertEquals(before, composeTestRule.onNodeWithTag("message").getUnclippedBoundsInRoot())
    }
}
