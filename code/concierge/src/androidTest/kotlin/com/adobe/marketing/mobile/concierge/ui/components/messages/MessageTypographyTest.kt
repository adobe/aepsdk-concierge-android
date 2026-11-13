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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTypography
import com.adobe.marketing.mobile.concierge.ui.theme.ThemeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MessageTypographyTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun responseLineSpacing_matchesMaterialText_atAccessibilityFontScales() {
        val scale = mutableStateOf(1f)
        val prefix = mutableStateOf("")
        val size = mutableStateOf(16)
        val lineHeight = mutableStateOf("24px")
        // At the fixed 2x density, 160dp reproduces CI's 320px-wide viewport.
        val width = mutableStateOf(160)
        val text = "One\nTwo"
        var reference: TextLayoutResult? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, scale.value)) {
                val json = """{"theme":{"--message-font-size":"${size.value}px","--line-height-body":"${lineHeight.value}"}}"""
                ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json))) {
                    val bodyStyle = ConciergeStyles.messageBubbleStyle.textStyle
                    Column(Modifier.width(width.value.dp)) {
                        ConciergeResponse(text = "${prefix.value}$text")
                        Text(
                            text = text,
                            modifier = Modifier.testTag("typography-reference"),
                            style = bodyStyle,
                            onTextLayout = { reference = it }
                        )
                    }
                }
            }
        }
        for (viewportWidth in listOf(160, 320)) {
            for (fontScale in listOf(1f, 1.3f, 1.5f, 2f)) {
                for (fontSize in listOf(12, 16, 20)) {
                    for (blockPrefix in listOf("", "- ", "1. ", "> ")) {
                        for (height in listOf("24px", "1.5")) {
                            rule.runOnIdle {
                                width.value = viewportWidth
                                scale.value = fontScale
                                size.value = fontSize
                                prefix.value = blockPrefix
                                lineHeight.value = height
                            }
                            val actual = layout(text)
                            val expected = checkNotNull(reference)
                            val case = "width=$viewportWidth scale=$fontScale size=$fontSize prefix=$blockPrefix lineHeight=$height"
                            assertEquals(case, 2, actual.lineCount)
                            assertEquals(case, 2, expected.lineCount)
                            assertEquals(case, expected.layoutInput.text.text, actual.layoutInput.text.text)
                            assertEquals(case, fontSize.sp, actual.layoutInput.style.fontSize)
                            assertEquals(
                                case, expected.lastBaseline - expected.firstBaseline,
                                actual.lastBaseline - actual.firstBaseline, 1.5f
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun responseBaseStyle_doesNotApplyBodyLineHeightToHeadingsOrCode() {
        val markdown = mutableStateOf("# Heading")
        val json = """{"theme":{"--message-font-size":"12px","--line-height-body":"24px"}}"""
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(2f, 1.5f)) {
                ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json))) {
                    ConciergeResponse(text = markdown.value)
                }
            }
        }
        for ((content, text, fontSize) in listOf(
            Triple("# Heading", "Heading", 26.sp),
            Triple("```\nCode\nMore code\n```", "Code\nMore code", 16.sp),
            Triple("- # Heading", "Heading", 26.sp),
            Triple("> # Heading", "Heading", 26.sp)
        )) {
            rule.runOnIdle { markdown.value = content }
            val result = layout(text)
            assertEquals(12.sp, result.layoutInput.style.fontSize)
            assertEquals(TextUnit.Unspecified, result.layoutInput.style.lineHeight)
            assertTrue(result.layoutInput.text.paragraphStyles.isEmpty())
            assertEquals(fontSize, result.layoutInput.text.spanStyles.single().item.fontSize)
        }
    }

    @Test
    fun mixedBodyHeadingAndCode_onlyStylesBodyParagraphs() {
        rule.setContent {
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(
                """{"theme":{"--message-font-size":"16px","--line-height-body":"24px"}}"""
            ))) {
                ConciergeResponse(text = "Before\n\n# Heading\n\n```\nCode\nMore code\n```\n\nAfter")
            }
        }
        val result = rule.onNodeWithText("Before", substring = true)
        val layouts = mutableListOf<TextLayoutResult>()
        result.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts)
        val text = layouts.single().layoutInput.text
        assertEquals("Before\n\nHeading\n\nCode\nMore code\n\nAfter", text.text)
        for (body in listOf("Before", "After")) {
            val offset = text.text.indexOf(body)
            assertEquals(24.sp, text.paragraphStyles.single { offset in it.start until it.end }.item.lineHeight)
        }
        for (special in listOf("Heading", "Code", "More code")) {
            val offset = text.text.indexOf(special)
            assertTrue(text.paragraphStyles.none { offset in it.start until it.end })
        }
    }

    @Test
    fun programmaticAbsoluteLineHeight_takesPrecedence_andOverflowFallsBack() {
        val typography = mutableStateOf(ConciergeTypography(fontSize = 16.0, lineHeight = 1.5))
        rule.setContent {
            ConciergeTheme(theme = theme(ConciergeThemeTokens(typography = typography.value))) {
                ConciergeResponse(text = "First\nSecond")
            }
        }
        val defaultLineHeight = layout("First\nSecond").layoutInput.text.paragraphStyles.single().item.lineHeight
        rule.runOnIdle {
            typography.value = ConciergeTypography(fontSize = 16.0, lineHeight = 2.0, lineHeightPx = 24.0)
        }
        assertEquals(24.sp, layout("First\nSecond").layoutInput.text.paragraphStyles.single().item.lineHeight)
        rule.runOnIdle {
            typography.value = ConciergeTypography(fontSize = 16.0, lineHeight = 1e38)
        }
        assertEquals(defaultLineHeight, layout("First\nSecond").layoutInput.text.paragraphStyles.single().item.lineHeight)
    }

    @Test
    fun absoluteLineHeight_staysFixedAcrossFontChanges_andSupportsMultiplierChanges() {
        val json = mutableStateOf("""{"theme":{"--message-font-size":"16px","--line-height-body":"24px"}}""")
        var expectedPx = 0f
        rule.setContent {
            expectedPx = with(LocalDensity.current) { 24.sp.toPx() }
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json.value))) {
                ConciergeResponse(text = "First\nSecond")
            }
        }
        for (size in listOf(16, 20)) {
            rule.runOnIdle {
                json.value = """{"theme":{"--message-font-size":"${size}px","--line-height-body":"24px"}}"""
            }
            val result = layout("First\nSecond")
            assertEquals(24.sp, result.layoutInput.text.paragraphStyles.single().item.lineHeight)
            assertEquals(expectedPx, result.lastBaseline - result.firstBaseline, 1.5f)
        }
        rule.runOnIdle {
            json.value = """{"theme":{"--message-font-size":"20px","--line-height-body":"2.0"}}"""
        }
        assertEquals(40.sp, layout("First\nSecond").layoutInput.text.paragraphStyles.single().item.lineHeight)
    }

    @Test
    fun headingsAndFencedCode_keepDefaultSpacing_withBodyLineHeightEnabled() {
        val content = mutableStateOf("# Heading")
        val json = mutableStateOf("""{"theme":{"--message-font-size":"12px"}}""")
        rule.setContent {
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json.value))) {
                ConciergeResponse(text = content.value)
            }
        }
        for ((markdown, rendered) in listOf(
            "# Heading" to "Heading",
            "```\nCode\nMore code\n```" to "Code\nMore code",
            "> # Heading" to "Heading",
            "- # Heading" to "Heading"
        )) {
            rule.runOnIdle {
                content.value = markdown
                json.value = """{"theme":{"--message-font-size":"12px"}}"""
            }
            val baseline = layout(rendered)
            rule.runOnIdle {
                json.value = """{"theme":{"--message-font-size":"12px","--line-height-body":"1.0"}}"""
            }
            val styled = layout(rendered)
            assertTrue(styled.layoutInput.text.paragraphStyles.isEmpty())
            assertEquals(baseline.size, styled.size)
            assertEquals(baseline.firstBaseline, styled.firstBaseline, 0.01f)
            assertEquals(baseline.lastBaseline, styled.lastBaseline, 0.01f)
        }
    }

    @Test
    fun paragraphListAndQuote_keepLinkStylesAndClicks_afterThemeChange() {
        val json = mutableStateOf(
            """{"theme":{"--message-font-size":"18px","--message-font-weight":"500","--message-concierge-link-font-weight":"800","--message-concierge-link-underline":"false","--message-concierge-link-color":"#BB6DFF"}}"""
        )
        val clicked = mutableListOf<String>()
        val links = listOf("" to "Paragraph", "- " to "List", "> " to "Quote")
        rule.setContent {
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json.value))) {
                Column {
                    links.forEach { (prefix, label) ->
                        ConciergeResponse(
                            text = "$prefix[$label](https://example.com/$label)",
                            handleLink = { url, _ -> clicked.add(url) }
                        )
                    }
                }
            }
        }
        links.forEach { (_, label) ->
            val span = linkStyle(label)
            assertEquals(18.sp, span.fontSize)
            assertEquals(FontWeight.ExtraBold, span.fontWeight)
            assertEquals(Color(0xFFBB6DFF), span.color)
            assertEquals(TextDecoration.None, span.textDecoration)
        }
        rule.runOnIdle {
            json.value = """{"theme":{"--message-font-size":"20px","--message-font-weight":"600","--message-concierge-link-font-weight":"700","--message-concierge-link-underline":"true","--message-concierge-link-color":"#123456"}}"""
        }
        links.forEach { (_, label) ->
            val span = linkStyle(label)
            assertEquals(20.sp, span.fontSize)
            assertEquals(FontWeight.Bold, span.fontWeight)
            assertEquals(Color(0xFF123456), span.color)
            assertEquals(TextDecoration.Underline, span.textDecoration)
            val result = layout(label)
            val url = result.layoutInput.text.getStringAnnotations("URL", 0, label.length).single()
            rule.onNodeWithText(label).performTouchInput {
                click(result.getBoundingBox(url.start).center)
            }
        }
        rule.runOnIdle {
            assertEquals(links.map { "https://example.com/${it.second}" }, clicked)
        }
    }

    @Test
    fun paragraphListAndQuote_applyConfiguredLineHeight_toActualLayout() {
        val prefix = mutableStateOf("")
        val json = mutableStateOf("""{"theme":{"--message-font-size":"32px","--line-height-body":"1.5"}}""")
        var expectedLineHeightPx = 0f
        rule.setContent {
            expectedLineHeightPx = with(LocalDensity.current) { 48.sp.toPx() }
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json.value))) {
                ConciergeResponse(text = "${prefix.value}First\nSecond")
            }
        }
        for (blockPrefix in listOf("", "- ", "> ")) {
            rule.runOnIdle { prefix.value = blockPrefix }
            val result = layout("First\nSecond")
            assertEquals(2, result.lineCount)
            assertEquals(48.sp, result.layoutInput.text.paragraphStyles.single().item.lineHeight)
            assertEquals(expectedLineHeightPx, result.lastBaseline - result.firstBaseline, 1.5f)
        }
        rule.runOnIdle { json.value = """{"theme":{"--message-font-size":"32px"}}""" }
        assertTrue(layout("First\nSecond").layoutInput.text.paragraphStyles.isEmpty())
    }

    @Test
    fun messageFontSizeOnly_scalesInheritedLineHeight() {
        var bodyStyle: TextStyle? = null
        var result: TextLayoutResult? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(
                    """{"theme":{"--message-font-size":"32px"}}"""
                ))) {
                    bodyStyle = ConciergeStyles.messageBubbleStyle.textStyle
                    Text(
                        text = "First\nSecond",
                        style = checkNotNull(bodyStyle),
                        onTextLayout = { result = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            assertEquals(32.sp, bodyStyle?.fontSize)
            assertEquals(48.sp, bodyStyle?.lineHeight)
            assertEquals(48f, checkNotNull(result).lastBaseline - result!!.firstBaseline, 1.5f)
        }
    }

    @Test
    fun invalidProgrammaticTypography_usesDefaults_forPlainTextAndLinks() {
        val typography = mutableStateOf<ConciergeTypography?>(null)
        rule.setContent {
            ConciergeTheme(theme = theme(ConciergeThemeTokens(typography = typography.value))) {
                Column {
                    ConciergeResponse(text = "Plain response")
                    ConciergeResponse(text = "[Store](https://example.com)")
                }
            }
        }
        val defaultBody = layout("Plain response").layoutInput.text.spanStyles.single().item
        val defaultLink = linkStyle("Store")
        val invalidTypography = listOf(
            ConciergeTypography(fontWeight = 0),
            ConciergeTypography(fontWeight = -1),
            ConciergeTypography(fontWeight = 1001),
            ConciergeTypography(linkFontWeight = 0),
            ConciergeTypography(linkFontWeight = -1),
            ConciergeTypography(linkFontWeight = 1001)
        ) + listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, 1e100, 1e-100).map {
            ConciergeTypography(fontSize = it)
        }
        for (invalid in invalidTypography) {
            rule.runOnIdle { typography.value = invalid }
            assertEquals(invalid.toString(), defaultBody, layout("Plain response").layoutInput.text.spanStyles.single().item)
            assertEquals(invalid.toString(), defaultLink, linkStyle("Store"))
        }
        rule.runOnIdle {
            typography.value = ConciergeTypography(fontSize = 22.0, fontWeight = 1, linkFontWeight = 1000)
        }
        assertEquals(22.sp, layout("Plain response").layoutInput.text.spanStyles.single().item.fontSize)
        assertEquals(FontWeight(1), layout("Plain response").layoutInput.text.spanStyles.single().item.fontWeight)
        assertEquals(FontWeight(1000), linkStyle("Store").fontWeight)
    }

    @Test
    fun invalidCssTypography_usesDefaults_inProductionResponse() {
        val json = mutableStateOf("""{"theme":{}}""")
        rule.setContent {
            ConciergeTheme(theme = theme(ThemeParser.parseThemeTokens(json.value))) {
                Column {
                    ConciergeResponse(text = "Plain response")
                    ConciergeResponse(text = "**[Store](https://example.com)**")
                }
            }
        }
        val defaultBody = layout("Plain response").layoutInput.text.spanStyles.single().item
        val defaultLink = linkStyle("Store")
        for (invalid in listOf("0", "-1", "1001", "heavy", "NaN", "Infinity")) {
            rule.runOnIdle {
                json.value = """{"theme":{"--message-font-size":"-1px","--message-font-weight":"$invalid","--message-concierge-link-font-weight":"$invalid"}}"""
            }
            assertEquals(defaultBody, layout("Plain response").layoutInput.text.spanStyles.single().item)
            assertEquals(defaultLink, linkStyle("Store"))
            assertEquals(FontWeight.Bold, linkStyle("Store").fontWeight)
        }
    }

    private fun theme(tokens: ConciergeThemeTokens?) = ConciergeThemeData(ConciergeThemeConfig(), tokens)

    private fun layout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        val node = rule.onNode(hasText(text) and hasTestTag("typography-reference").not()).fetchSemanticsNode()
        assertTrue(node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results))
        return results.single()
    }

    private fun linkStyle(text: String): SpanStyle {
        val result = layout(text).layoutInput.text
        val url = result.getStringAnnotations("URL", 0, result.length).single()
        assertEquals("https://example.com${if (text == "Store") "" else "/$text"}", url.item)
        return result.spanStyles.single { it.start == url.start && it.end == url.end }.item
    }
}
