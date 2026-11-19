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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.click
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.DisclaimerLink
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.PrivacyNoticePresentation
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PrivacyNoticeViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val restorationNotice = PrivacyNoticeConfig(
        text = "First privacy line.\nSecond privacy line.\nThird privacy line.\nFourth privacy line.\nFinal privacy line.",
        presentation = PrivacyNoticePresentation.COLLAPSIBLE,
        collapsedMaxLines = 3
    )

    @Test
    fun notice_withoutSummaryKeepsCollapseControlAfterSavedStateRestoration() {
        val restoration = StateRestorationTester(composeTestRule)
        restoration.setContent {
            ConciergeTheme {
                PrivacyNoticeView(config = restorationNotice, handleLink = noOpLink)
            }
        }

        assertRestorationNoticeLineCount(3)
        composeTestRule.onNodeWithText("Read more").performClick()
        assertRestorationNoticeLineCount(5)
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed()

        restoration.emulateSavedInstanceStateRestore()

        assertRestorationNoticeLineCount(5)
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed().performClick()
        assertRestorationNoticeLineCount(3)
        composeTestRule.onNodeWithText("Read more").assertIsDisplayed()
    }

    @Test
    fun notice_withoutSummaryKeepsCollapseControlAfterLazyItemDisposal() {
        var noticeInComposition = false
        composeTestRule.setContent {
            ConciergeTheme {
                LazyColumn(Modifier.fillMaxSize().testTag("transcript")) {
                    item(key = "privacy-notice") {
                        DisposableEffect(Unit) {
                            noticeInComposition = true
                            onDispose { noticeInComposition = false }
                        }
                        PrivacyNoticeView(config = restorationNotice, handleLink = noOpLink)
                    }
                    items(100) { index ->
                        Text("Message $index", modifier = Modifier.height(72.dp))
                    }
                }
            }
        }

        assertRestorationNoticeLineCount(3)
        composeTestRule.onNodeWithText("Read more").performClick()
        assertRestorationNoticeLineCount(5)
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed()

        composeTestRule.onNodeWithTag("transcript").performScrollToIndex(80)
        composeTestRule.runOnIdle { assertFalse("Notice must actually leave composition", noticeInComposition) }
        composeTestRule.onNodeWithTag("transcript").performScrollToIndex(0)
        composeTestRule.runOnIdle { assertTrue(noticeInComposition) }

        assertRestorationNoticeLineCount(5)
        composeTestRule.onNodeWithText("Show less").assertIsDisplayed().performClick()
        assertRestorationNoticeLineCount(3)
        composeTestRule.onNodeWithText("Read more").assertIsDisplayed()
    }

    private fun assertRestorationNoticeLineCount(expected: Int) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(restorationNotice.text)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(expected, layouts.single().lineCount)
    }

    @Test
    fun notice_summaryExpandsAndCollapsesWithoutMovingFollowingContentAboveIt() {
        composeTestRule.setContent {
            ConciergeTheme {
                Column {
                    PrivacyNoticeView(
                        config = PrivacyNoticeConfig(
                            text = "Full notice.\nAdditional details.\nRead {Privacy}.",
                            summary = "Short summary.",
                            presentation = PrivacyNoticePresentation.COLLAPSIBLE,
                            links = listOf(DisclaimerLink("Privacy", "https://example.com/privacy"))
                        ),
                        handleLink = noOpLink
                    )
                    Text("Following message")
                }
            }
        }

        composeTestRule.onNodeWithText("Short summary.").assertIsDisplayed()
        val collapsedTop = composeTestRule.onNodeWithText("Following message").getUnclippedBoundsInRoot().top
        composeTestRule.onNodeWithText("Read more").performClick()
        composeTestRule.onNodeWithText("Short summary.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Full notice.\nAdditional details.\nRead Privacy.").assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions))
        assertTrue(composeTestRule.onNodeWithText("Following message").getUnclippedBoundsInRoot().top > collapsedTop)
        composeTestRule.onNodeWithText("Show less").performClick()
        composeTestRule.onNodeWithText("Short summary.").assertIsDisplayed()
        assertEquals(
            collapsedTop.value,
            composeTestRule.onNodeWithText("Following message").getUnclippedBoundsInRoot().top.value,
            1f
        )
    }

    @Test
    fun notice_withoutSummaryOnlyOffersExpansionWhenPreviewOverflows() {
        val longText = "First line.\nSecond line.\nThird line.\nFourth line."
        composeTestRule.setContent {
            ConciergeTheme {
                Column {
                    PrivacyNoticeView(
                        config = PrivacyNoticeConfig(
                            title = "Short",
                            text = "Fits.",
                            presentation = PrivacyNoticePresentation.COLLAPSIBLE
                        ),
                        handleLink = noOpLink
                    )
                    PrivacyNoticeView(
                        config = PrivacyNoticeConfig(
                            title = "Long",
                            text = longText,
                            collapsedMaxLines = 2,
                            presentation = PrivacyNoticePresentation.COLLAPSIBLE
                        ),
                        handleLink = noOpLink
                    )
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText(longText)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(2, layouts.single().lineCount)
        composeTestRule.onNodeWithText("Read more").performClick()
        layouts.clear()
        composeTestRule.onNodeWithText(longText)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertEquals(4, layouts.single().lineCount)
        composeTestRule.onNodeWithText("Show less").performClick()
        composeTestRule.onNodeWithText("Read more").assertIsDisplayed()
    }

    @Test
    fun notice_inlineRemainsFullyVisibleEvenWithSummary() {
        composeTestRule.setContent {
            ConciergeTheme {
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(text = "Full notice.", summary = "Summary."),
                    handleLink = noOpLink
                )
            }
        }
        composeTestRule.onNodeWithText("Full notice.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Summary.").assertDoesNotExist()
        composeTestRule.onNodeWithText("Read more").assertDoesNotExist()
    }

    @Test
    fun notice_displaysHeadingAndBodyWithOuterTopSpacing() {
        var expectedInset = 0.dp
        composeTestRule.setContent {
            ConciergeTheme {
                expectedInset = ConciergeStyles.messageBubbleStyle.padding + 16.dp
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(text = "Your privacy matters."),
                    handleLink = noOpLink,
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
                    handleLink = noOpLink,
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
                PrivacyNoticeView(config = PrivacyNoticeConfig(title = " ", text = "Body only."), handleLink = noOpLink)
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
              "--color-text":"#0A0B0C",
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
        var expectedTitle = Color.Unspecified
        composeTestRule.setContent {
            ConciergeTheme(theme = theme) {
                expectedBody = requireNotNull(ConciergeTheme.colors.conciergeMessageText).copy(alpha = 0.75f)
                expectedLink = requireNotNull(ConciergeTheme.colors.messageConciergeLink)
                expectedTitle = ConciergeTheme.colors.onSurface
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(
                        title = "Legal",
                        text = "Read {Privacy}.",
                        links = listOf(DisclaimerLink("Privacy", "https://example.com/privacy"))
                    ),
                    handleLink = noOpLink
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
        val titleLayouts = mutableListOf<TextLayoutResult>()
        composeTestRule.onNodeWithText("Legal")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
        assertEquals(expectedTitle, titleLayouts.single().layoutInput.style.color)
    }

    @Test
    fun notice_touchAndDeduplicatedAccessibilityLinksUseSharedHandler() {
        val routed = mutableListOf<Pair<String, String>>()
        composeTestRule.setContent {
            ConciergeTheme {
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(
                        text = "{Privacy} and {Privacy}, then {Terms}.",
                        links = listOf(
                            DisclaimerLink("Privacy", "https://example.com/privacy"),
                            DisclaimerLink("Terms", "https://example.com/terms")
                        )
                    ),
                    handleLink = { url, origin -> routed.add(url to origin) }
                )
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
                listOf("https://example.com/privacy", "https://example.com/terms", "https://example.com/privacy")
                    .map { it to ConciergeConstants.TrackingEvent.LinkClickOrigin.PRIVACY_NOTICE },
                routed
            )
        }
    }

    @Test
    fun notice_invalidLinkUrlRendersPlainTextWithoutActions() {
        val routed = mutableListOf<String>()
        composeTestRule.setContent {
            ConciergeTheme {
                PrivacyNoticeView(
                    config = PrivacyNoticeConfig(
                        text = "See {Policy}.",
                        links = listOf(DisclaimerLink("Policy", "https://example.com/a b"))
                    ),
                    handleLink = { url, _ -> routed.add(url) }
                )
            }
        }

        val body = composeTestRule.onNodeWithText("See Policy.")
        body.assertIsDisplayed().assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
        val layouts = mutableListOf<TextLayoutResult>()
        body.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        body.performTouchInput { click(layouts.single().getBoundingBox(5).center) }
        composeTestRule.runOnIdle { assertTrue(routed.isEmpty()) }
    }

    private val noOpLink: (String, String) -> Unit = { _, _ -> }
}
