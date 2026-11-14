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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.cards.NativeCardController
import com.adobe.marketing.mobile.concierge.network.CtaButton
import com.adobe.marketing.mobile.concierge.ui.components.image.assetBitmapCache
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeIconAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeAssets
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class NativeCardSuggestionAlignmentTest(
    private val icon: String?,
    private val layoutDirection: LayoutDirection
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val imageProvider = object : ImageProvider {
        override fun getCached(url: String): Bitmap? = null
        override suspend fun get(url: String): Bitmap = throw IOException("Image unavailable")
        override fun clear() = Unit
    }

    @After
    fun tearDown() {
        scope.cancel()
        assetBitmapCache.clear()
    }

    private fun setMessages(
        textMessage: ChatMessage,
        nativeMessage: ChatMessage,
        onSuggestionClick: (String) -> Unit = {},
        onCtaButtonClick: (CtaButton) -> Unit = {}
    ) {
        if (icon == "native-test-icon") {
            assetBitmapCache[icon] = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        val theme = ConciergeThemeData(
            config = ConciergeThemeConfig(),
            tokens = ConciergeThemeTokens(
                assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = icon))
            )
        )
        val renderer: ConciergeCardRenderer = { _, _, _ ->
            Box(Modifier.fillMaxWidth().height(40.dp).testTag("NativeTestRenderer"))
        }
        val controller = NativeCardController(
            scope = scope,
            cardIdGenerator = { "native-card" },
            rendererLookup = { type -> renderer.takeIf { type == "example.card" } }
        )
        composeTestRule.runOnUiThread {
            val result = controller.activate(
                NativeCardController.ActivationRequest(
                    element = mapOf("type" to "example.card", "id" to "one"),
                    type = "example.card",
                    turnId = "turn-1",
                    position = 0
                )
            )
            assertTrue(result is NativeCardController.ActivationResult.Activated)
        }
        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalImageProvider provides imageProvider,
                    LocalNativeCardController provides controller
                ) {
                    Column(Modifier.width(360.dp)) {
                        ChatMessageItem(textMessage)
                        ChatMessageItem(
                            nativeMessage,
                            onSuggestionClick = onSuggestionClick,
                            onCtaButtonClick = onCtaButtonClick
                        )
                    }
                }
            }
        }
    }

    private fun assertMatchingLeadingEdges(expected: DpRect, actual: DpRect) {
        if (layoutDirection == LayoutDirection.Ltr) {
            assertEquals(expected.left.value, actual.left.value, 1f)
        } else {
            assertEquals(expected.right.value, actual.right.value, 1f)
        }
    }

    @Test
    fun nativeSuggestionsMatchTextSuggestionsAndKeepActionsUsable() {
        val cta = CtaButton(label = "Open details", url = "https://example.com/details")
        val textMessage = ChatMessage(
            content = MessageContent.Text("Agent reply"),
            isFromUser = false,
            timestamp = 0,
            promptSuggestions = listOf("Text suggestion")
        )
        val nativeMessage = ChatMessage(
            content = MessageContent.NativeCard("native-card"),
            isFromUser = false,
            timestamp = 0,
            promptSuggestions = listOf("Native suggestion", "Second suggestion"),
            ctaButton = cta
        )
        var selectedSuggestion: String? = null
        var selectedCta: CtaButton? = null
        setMessages(
            textMessage,
            nativeMessage,
            onSuggestionClick = { selectedSuggestion = it },
            onCtaButtonClick = { selectedCta = it }
        )

        composeTestRule.onNodeWithTag("NativeTestRenderer").assertIsDisplayed()
        val textSuggestion = composeTestRule.onNodeWithText("Text suggestion")
        val nativeSuggestion = composeTestRule.onNodeWithText("Native suggestion")
        val secondSuggestion = composeTestRule.onNodeWithText("Second suggestion")
        textSuggestion.assertIsDisplayed()
        nativeSuggestion.assertIsDisplayed()
        secondSuggestion.assertIsDisplayed()
        assertMatchingLeadingEdges(textSuggestion.getUnclippedBoundsInRoot(), nativeSuggestion.getUnclippedBoundsInRoot())
        val ctaNode = composeTestRule.onNodeWithTag("CtaButton")
        ctaNode.assertIsDisplayed()
        assertTrue(ctaNode.getUnclippedBoundsInRoot().top >= secondSuggestion.getUnclippedBoundsInRoot().bottom)

        nativeSuggestion.performClick()
        assertEquals("Native suggestion", selectedSuggestion)
        secondSuggestion.performClick()
        assertEquals("Second suggestion", selectedSuggestion)
        ctaNode.performClick()
        assertEquals(cta, selectedCta)
    }

    @Test
    fun nativeCtaWithoutSuggestionsMatchesTextCta() {
        val textCta = CtaButton(label = "Text action", url = "https://example.com/text")
        val nativeCta = CtaButton(label = "Native action", url = "https://example.com/native")
        var selectedCta: CtaButton? = null
        setMessages(
            ChatMessage(
                content = MessageContent.Text("Agent reply"),
                isFromUser = false,
                timestamp = 0,
                ctaButton = textCta
            ),
            ChatMessage(
                content = MessageContent.NativeCard("native-card"),
                isFromUser = false,
                timestamp = 0,
                ctaButton = nativeCta
            ),
            onCtaButtonClick = { selectedCta = it }
        )

        composeTestRule.onNodeWithTag("NativeTestRenderer").assertIsDisplayed()
        val textAction = composeTestRule.onNodeWithText("Text action")
        val nativeAction = composeTestRule.onNodeWithText("Native action")
        textAction.assertIsDisplayed()
        nativeAction.assertIsDisplayed()
        assertMatchingLeadingEdges(textAction.getUnclippedBoundsInRoot(), nativeAction.getUnclippedBoundsInRoot())
        nativeAction.performClick()
        assertEquals(nativeCta, selectedCta)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "icon={0}, direction={1}")
        fun cases(): List<Array<Any?>> =
            listOf(null, "native-test-icon", "missing-native-test-icon", "https://example.invalid/icon.png")
                .flatMap { icon ->
                    listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).map { direction ->
                        arrayOf(icon, direction)
                    }
                }
    }
}
