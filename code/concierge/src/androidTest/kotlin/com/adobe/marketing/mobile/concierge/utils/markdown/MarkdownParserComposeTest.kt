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

package com.adobe.marketing.mobile.concierge.utils.markdown

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.AnnotatedString
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Recomposition tests for [MarkdownParser.parse].
 *
 * These guard the performance fix where the expensive [MarkdownRenderer.render] call was wrapped in
 * `remember`. Without that, the full [AnnotatedString] is rebuilt on every recomposition for every
 * visible message, which during streaming scales as O(messages x chunks) and drives the app into
 * ANR/OOM. We assert behavior via referential identity of the returned [AnnotatedString]:
 * memoized renders return the same instance across recompositions, and a changed input produces a
 * new instance.
 */
class MarkdownParserComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun parse_reusesRenderedResult_acrossRecompositions_whenTextUnchanged() {
        val results = mutableListOf<AnnotatedString>()
        val trigger = mutableStateOf(0)
        val text = "Here are some **men's marathon running shoes** that fit what you're after."

        composeTestRule.setContent {
            ConciergeTheme {
                // Read the trigger so bumping it forces this scope to recompose.
                val tick = trigger.value
                val rendered = MarkdownParser.parse(text)
                SideEffect {
                    // Capture the result of each successful (re)composition.
                    @Suppress("UNUSED_EXPRESSION") tick
                    results.add(rendered)
                }
            }
        }

        composeTestRule.waitForIdle()
        val firstRender = results.first()

        // Force two extra recompositions without changing the markdown input.
        composeTestRule.runOnIdle { trigger.value = 1 }
        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle { trigger.value = 2 }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            // Recompositions actually happened...
            assertTrue(
                "Expected multiple compositions, captured ${results.size}",
                results.size >= 2
            )
            // ...yet every render returned the same memoized instance (render not re-run).
            results.forEach { assertSame(firstRender, it) }
        }
    }

    @Test
    fun parse_producesNewRenderedResult_whenTextChanges() {
        val results = mutableListOf<AnnotatedString>()
        val textState = mutableStateOf("First **bold** response")

        composeTestRule.setContent {
            ConciergeTheme {
                val rendered = MarkdownParser.parse(textState.value)
                SideEffect { results.add(rendered) }
            }
        }

        composeTestRule.waitForIdle()
        val firstRender = results.first()

        // Changing the markdown must invalidate the memoized render (key includes the text).
        composeTestRule.runOnIdle { textState.value = "Second, completely *different* response" }
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            val latestRender = results.last()
            assertNotSame(firstRender, latestRender)
            assertNotEquals(firstRender.text, latestRender.text)
        }
    }
}
