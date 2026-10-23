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

package com.adobe.marketing.mobile.concierge.ui.theme

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyNoticeConfigTest {

    @Test
    fun `missing and null fields use privacy notice defaults`() {
        val missing = ThemeParser.parseThemeTokens("""{"behavior":{}}""")!!
        val explicitNulls = ThemeParser.parseThemeTokens(
            """{"behavior":{"privacyNotice":{"title":null,"text":null,"links":null}}}"""
        )!!
        val partial = ThemeParser.parseThemeTokens(
            """{"behavior":{"privacyNotice":{"text":"Keep this copy exactly. "}}}"""
        )!!

        assertEquals(PrivacyNoticeConfig(), missing.behavior.privacyNotice)
        assertEquals(PrivacyNoticeConfig(), explicitNulls.behavior.privacyNotice)
        assertEquals(
            PrivacyNoticeConfig(title = "Privacy Notice", text = "Keep this copy exactly. "),
            partial.behavior.privacyNotice
        )
        assertFalse(missing.behavior.privacyNotice.hasContent)
    }

    @Test
    fun `absent behavior and null notice use defaults`() {
        listOf("{}", """{"behavior":{"privacyNotice":null}}""").forEach { json ->
            assertEquals(PrivacyNoticeConfig(), ThemeParser.parseThemeTokens(json)!!.behavior.privacyNotice)
        }
    }

    @Test
    fun `incomplete links are dropped without discarding complete links`() {
        val tokens = ThemeParser.parseThemeTokens(
            """
            {"behavior":{"privacyNotice":{"text":"Read {Privacy}.","links":[
              {},
              {"text":"Missing URL"},
              {"url":"https://example.com"},
              {"text":null,"url":"https://example.com"},
              {"text":"Missing URL","url":null},
              {"text":"Privacy","url":"https://example.com/privacy"}
            ]}}}
            """.trimIndent()
        )!!

        assertEquals(
            listOf(DisclaimerLink("Privacy", "https://example.com/privacy")),
            tokens.behavior.privacyNotice.links
        )
    }

    @Test
    fun `configured title copy and links are retained exactly`() {
        val tokens = ThemeParser.parseThemeTokens(
            """
            {
              "behavior": {
                "privacyNotice": {
                  "title": "Legal information",
                  "text": "Sent to {Company}.\nPlease review.",
                  "links": [
                    {"text": "Company", "url": "https://example.com/privacy"}
                  ]
                }
              }
            }
            """.trimIndent()
        )!!

        assertEquals(
            PrivacyNoticeConfig(
                title = "Legal information",
                text = "Sent to {Company}.\nPlease review.",
                links = listOf(DisclaimerLink("Company", "https://example.com/privacy"))
            ),
            tokens.behavior.privacyNotice
        )
        assertTrue(tokens.behavior.privacyNotice.hasContent)
    }

    @Test
    fun `configured notice survives JSON encode and decode`() {
        val expected = PrivacyNoticeConfig(
            title = "Privacy",
            text = "Sent to {Company}.\nKeep spaces. ",
            links = listOf(DisclaimerLink("Company", "https://example.com/privacy"))
        )
        val noticeJson = JSONObject()
            .put("title", expected.title)
            .put("text", expected.text)
            .put(
                "links",
                JSONArray().put(
                    JSONObject()
                        .put("text", expected.links.single().text)
                        .put("url", expected.links.single().url)
                )
            )
        val themeJson = JSONObject()
            .put("behavior", JSONObject().put("privacyNotice", noticeJson))

        val decoded = ThemeParser.parseThemeTokens(themeJson.toString())!!.behavior.privacyNotice

        assertEquals(expected, decoded)
    }

    @Test
    fun `malformed privacy field types fail theme decoding`() {
        listOf(
            """{"privacyNotice":"invalid"}""",
            """{"privacyNotice":{"title":42}}""",
            """{"privacyNotice":{"text":false}}""",
            """{"privacyNotice":{"links":"invalid"}}""",
            """{"privacyNotice":{"links":[null]}}""",
            """{"privacyNotice":{"links":["invalid"]}}""",
            """{"privacyNotice":{"links":[{"text":42,"url":"https://example.com"}]}}""",
            """{"privacyNotice":{"links":[{"text":"Privacy","url":42}]}}"""
        ).forEach { privacyNotice ->
            assertNull(
                ThemeParser.parseThemeTokens(
                    """{"behavior":$privacyNotice}"""
                )
            )
        }
    }

    @Test
    fun `privacy content predicate ignores whitespace-only body`() {
        assertFalse(PrivacyNoticeConfig(text = " \n\t").hasContent)
        assertTrue(PrivacyNoticeConfig(text = " Notice ").hasContent)
    }
}
