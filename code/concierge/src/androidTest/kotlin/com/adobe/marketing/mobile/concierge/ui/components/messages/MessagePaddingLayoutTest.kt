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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

internal object FailingPaddingImageProvider : ImageProvider {
    override fun getCached(url: String): Bitmap? = null
    override suspend fun get(url: String): Bitmap = throw IOException("Image unavailable")
    override fun clear() {}
}

@RunWith(Parameterized::class)
class MessagePaddingLayoutTest(
    private val icon: String?,
    private val css: String?,
    private val mixed: Boolean,
    private val rtl: Boolean
) {
    @get:Rule
    val composeTestRule = createComposeRule()

    @After
    fun clearCache() {
        assetBitmapCache.clear()
    }

    @Test
    fun agentContentUsesConfiguredEdgesWithoutChangingOuterOffsets() {
        if (icon == "loaded-padding-icon") {
            assetBitmapCache[icon] = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
        val base = ConciergeThemeTokens(
            assets = ConciergeThemeAssets(icons = ConciergeIconAssets(company = icon))
        )
        val tokens = if (css == null) base else CSSKeyMapper.apply("--message-padding", css, base)
        val theme = ConciergeThemeData(config = ConciergeThemeConfig(), tokens = tokens)
        val message = ChatMessage(
            content = if (mixed) MessageContent.Mixed("Agent reply") else MessageContent.Text("Agent reply"),
            isFromUser = false,
            timestamp = 0
        )
        var density = Density(1f)
        composeTestRule.setContent {
            density = LocalDensity.current
            ConciergeTheme(theme = theme) {
                CompositionLocalProvider(
                    LocalImageProvider provides FailingPaddingImageProvider,
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Box(Modifier.width(360.dp).testTag("message")) {
                        ChatMessageItem(message)
                    }
                }
            }
        }

        val hasIcon = icon == "loaded-padding-icon" || icon?.startsWith("https://") == true
        val configured = tokens.cssLayout?.messagePadding
        val legacyStart = if (hasIcon) 0f else 12f
        val left = configured?.get(3)?.toFloat() ?: legacyStart
        val right = configured?.get(1)?.toFloat() ?: 12f
        val top = configured?.get(0)?.toFloat() ?: if (hasIcon && mixed) 0f else 12f
        val bottom = configured?.get(2)?.toFloat() ?: 12f
        val root = composeTestRule.onNodeWithTag("message").getUnclippedBoundsInRoot()
        val text = composeTestRule.onNodeWithText("Agent reply")
        text.assertIsDisplayed()
        val bounds = text.getUnclippedBoundsInRoot()
        val outerStart = if (hasIcon) 51f else 8f
        val outerLeft = if (rtl) 8f else outerStart
        val outerRight = if (rtl) outerStart else 8f
        if (rtl) {
            assertEquals(outerRight + right, (root.right - bounds.right).value, 1f)
        } else {
            assertEquals(outerLeft + left, (bounds.left - root.left).value, 1f)
        }
        val layouts = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val availableWidth = with(density) { layouts.single().layoutInput.constraints.maxWidth.toDp() }
        assertEquals((root.right - root.left).value - outerLeft - left - outerRight - right, availableWidth.value, 1f)
        assertEquals((if (hasIcon && mixed) 0f else 8f) + top, (bounds.top - root.top).value, 1f)
        val expectedBottom = if (hasIcon && !mixed) {
            maxOf(8f + bottom, 47f - (bounds.bottom - root.top).value)
        } else {
            8f + bottom
        }
        assertEquals(expectedBottom, (root.bottom - bounds.bottom).value, 1f)
        if (!mixed) {
            val iconNode = composeTestRule.onNodeWithTag("MessageCompanyIcon")
            if (hasIcon) iconNode.assertIsDisplayed() else iconNode.assertDoesNotExist()
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "icon={0}, padding={1}, mixed={2}, rtl={3}")
        fun cases(): List<Array<Any?>> {
            val cases = mutableListOf<Array<Any?>>()
            listOf(null, "loaded-padding-icon", "missing-padding-icon", "https://example.invalid/icon.png").forEach { icon ->
                listOf(null, "8px 16px", "0px").forEach { padding ->
                    listOf(false, true).forEach { mixed ->
                        cases.add(arrayOf(icon, padding, mixed, false))
                    }
                }
            }
            listOf(null, "loaded-padding-icon", "https://example.invalid/icon.png").forEach { icon ->
                listOf(false, true).forEach { mixed ->
                    cases.add(arrayOf(icon, "4px 10px 6px 20px", mixed, true))
                }
            }
            return cases
        }
    }
}
