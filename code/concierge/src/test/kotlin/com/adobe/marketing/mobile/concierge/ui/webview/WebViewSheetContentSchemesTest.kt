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

package com.adobe.marketing.mobile.concierge.ui.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewSheetContentSchemesTest {

    @Test
    fun `isAllowedScheme returns true for https URL`() {
        assertTrue(WebViewSheetContentSchemes.isAllowedScheme("https://example.com"))
        assertTrue(WebViewSheetContentSchemes.isAllowedScheme("HTTPS://example.com/path"))
    }

    @Test
    fun `isAllowedScheme returns true for http URL`() {
        assertTrue(WebViewSheetContentSchemes.isAllowedScheme("http://example.com"))
        assertTrue(WebViewSheetContentSchemes.isAllowedScheme("HTTP://example.com/path?q=1"))
    }

    @Test
    fun `isAllowedScheme returns false for null`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme(null))
    }

    @Test
    fun `isAllowedScheme returns false for blank string`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme(""))
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("   "))
    }

    @Test
    fun `isAllowedScheme returns false for file scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("file:///android_asset/index.html"))
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("file:///data/local/tmp/file.html"))
    }

    @Test
    fun `isAllowedScheme returns false for content scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("content://com.example.provider/path"))
    }

    @Test
    fun `isAllowedScheme returns false for javascript scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("javascript:alert(1)"))
    }

    @Test
    fun `isAllowedScheme returns false for intent scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("intent://example.com#Intent;end"))
    }

    @Test
    fun `isAllowedScheme returns false for data scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `isAllowedScheme returns false for unknown scheme`() {
        assertFalse(WebViewSheetContentSchemes.isAllowedScheme("custom-scheme://example.com"))
    }

    @Test
    fun `isBlockedScheme returns true for dangerous schemes`() {
        assertTrue(WebViewSheetContentSchemes.isBlockedScheme("javascript:alert(1)"))
        assertTrue(WebViewSheetContentSchemes.isBlockedScheme("file:///data/local/tmp/file.html"))
        assertTrue(WebViewSheetContentSchemes.isBlockedScheme("content://com.example.provider/path"))
        assertTrue(WebViewSheetContentSchemes.isBlockedScheme("intent://example.com#Intent;end"))
        assertTrue(WebViewSheetContentSchemes.isBlockedScheme("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `isBlockedScheme returns false for system schemes`() {
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("mailto:user@example.com"))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("tel:+15555550100"))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("sms:+15555550100"))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("myapp://screen/detail"))
    }

    @Test
    fun `isBlockedScheme returns false for http and https`() {
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("http://example.com"))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("https://example.com"))
    }

    @Test
    fun `isBlockedScheme returns false for null and blank`() {
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme(null))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme(""))
        assertFalse(WebViewSheetContentSchemes.isBlockedScheme("   "))
    }
}
