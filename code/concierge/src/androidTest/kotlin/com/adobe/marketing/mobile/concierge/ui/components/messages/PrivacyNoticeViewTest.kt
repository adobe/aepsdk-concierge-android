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
import android.content.Intent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrivacyNoticeViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun notice_displaysHeadingAndBodyWithOuterTopSpacing() {
        var expectedInset = 0.dp
        composeTestRule.setContent {
            ConciergeTheme {
                expectedInset = ConciergeStyles.messageBubbleStyle.padding + 16.dp
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(text = "Your privacy matters."),
                    modifier = Modifier.testTag("notice")
                )
            }
        }

        val heading = composeTestRule.onNodeWithText("Privacy Notice")
        heading.assertIsDisplayed().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNodeWithText("Your privacy matters.").assertIsDisplayed()
        val inset = heading.getUnclippedBoundsInRoot().top -
            composeTestRule.onNodeWithTag("notice").getUnclippedBoundsInRoot().top
        assertEquals(expectedInset.value, inset.value, 1f)
    }

    @Test
    fun notice_blankBodyHidesEntireNotice() {
        composeTestRule.setContent {
            ConciergeTheme {
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(text = " \n\t"),
                    modifier = Modifier.testTag("notice")
                )
            }
        }

        composeTestRule.onNodeWithTag("notice").assertDoesNotExist()
        composeTestRule.onNodeWithText("Privacy Notice").assertDoesNotExist()
    }

    @Test
    fun notice_blankTitleKeepsBodyWithoutLinkActions() {
        composeTestRule.setContent {
            ConciergeTheme {
                PrivacyNoticeView(config = PrivacyNoticeConfig(title = " ", text = "Body only."))
            }
        }

        composeTestRule.onNodeWithText(" ").assertDoesNotExist()
        composeTestRule.onNodeWithText("Body only.").assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }

    @Test
    fun notice_customThemePreservesBodyAndLinkColors() {
        val json = """
            {"theme":{
              "--message-concierge-text":"#123456",
              "--message-concierge-link-color":"#654321",
              "--message-concierge-background":"#EEEEEE",
              "--border-radius-card":"20px"
            }}
        """.trimIndent()
        val theme = ConciergeThemeData(
            config = requireNotNull(ThemeParser.parseThemeJson(json)),
            tokens = requireNotNull(ThemeParser.parseThemeTokens(json))
        )
        var expectedBody = Color.Unspecified
        var expectedLink = Color.Unspecified
        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                expectedBody = requireNotNull(ConciergeTheme.colors.conciergeMessageText).copy(alpha = 0.75f)
                expectedLink = requireNotNull(ConciergeTheme.colors.messageConciergeLink)
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(
                        text = "Read {Privacy}.",
                        links = listOf(DisclaimerLink("Privacy", "https://example.com/privacy"))
                    )
                )
            }
        }

        val text = composeTestRule.onNodeWithText("Read Privacy.")
            .fetchSemanticsNode().config[SemanticsProperties.Text].single()
        fun colorAt(index: Int) = text.spanStyles
            .filter { index >= it.start && index < it.end }
            .fold(SpanStyle()) { style, range -> style.merge(range.item) }.color
        assertEquals(expectedBody, colorAt(0))
        assertEquals(expectedLink, colorAt(5))
    }

    @Test
    fun notice_touchAndDeduplicatedAccessibilityLinksOpenExpectedUrls() {
        val opened = mutableListOf<Intent>()
        composeTestRule.setContent {
            val context = LocalContext.current
            val recordingContext = object : ContextWrapper(context) {
                override fun startActivity(intent: Intent) {
                    opened.add(intent)
                }
            }
            ConciergeTheme {
                CompositionLocalProvider(LocalContext provides recordingContext) {
                    PrivacyNoticeView(
                        config = PrivacyNoticeConfig(
                            text = "{Privacy} and {Privacy}, then {Terms}.",
                            links = listOf(
                                DisclaimerLink("Privacy", "https://example.com/privacy"),
                                DisclaimerLink("Terms", "https://example.com/terms")
                            )
                        )
                    )
                }
            }
        }

        val body = composeTestRule.onNodeWithText("Privacy and Privacy, then Terms.")
        val actions = body.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("Open Privacy", "Open Terms"), actions.map { it.label })
        composeTestRule.runOnIdle {
            actions.forEach { assertTrue(it.action()) }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        body.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        body.performTouchInput { click(layouts.single().getBoundingBox(1).center) }
        composeTestRule.runOnIdle {
            assertEquals(
                listOf("https://example.com/privacy", "https://example.com/terms", "https://example.com/privacy"),
                opened.map { it.dataString }
            )
            assertTrue(opened.all { it.action == Intent.ACTION_VIEW })
        }
    }
}
