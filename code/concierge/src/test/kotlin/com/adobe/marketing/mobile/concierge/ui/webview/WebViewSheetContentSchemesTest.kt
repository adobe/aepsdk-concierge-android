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

import android.net.Uri
import com.adobe.marketing.mobile.concierge.utils.isAllowedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.isBlockedUrlScheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebViewSheetContentSchemesTest {

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val url = firstArg<String>()
            val scheme = url.substringBefore(':', "").lowercase().ifEmpty { null }
            mockk<Uri> { every { this@mockk.scheme } returns scheme }
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun `isAllowedUrlScheme returns true for https URL`() {
        assertTrue(isAllowedUrlScheme("https://example.com"))
        assertTrue(isAllowedUrlScheme("HTTPS://example.com/path"))
    }

    @Test
    fun `isAllowedUrlScheme returns true for http URL`() {
        assertTrue(isAllowedUrlScheme("http://example.com"))
        assertTrue(isAllowedUrlScheme("HTTP://example.com/path?q=1"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for null`() {
        assertFalse(isAllowedUrlScheme(null))
    }

    @Test
    fun `isAllowedUrlScheme returns false for blank string`() {
        assertFalse(isAllowedUrlScheme(""))
        assertFalse(isAllowedUrlScheme("   "))
    }

    @Test
    fun `isAllowedUrlScheme returns false for file scheme`() {
        assertFalse(isAllowedUrlScheme("file:///android_asset/index.html"))
        assertFalse(isAllowedUrlScheme("file:///data/local/tmp/file.html"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for content scheme`() {
        assertFalse(isAllowedUrlScheme("content://com.example.provider/path"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for javascript scheme`() {
        assertFalse(isAllowedUrlScheme("javascript:alert(1)"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for intent scheme`() {
        assertFalse(isAllowedUrlScheme("intent://example.com#Intent;end"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for data scheme`() {
        assertFalse(isAllowedUrlScheme("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `isAllowedUrlScheme returns false for unknown scheme`() {
        assertFalse(isAllowedUrlScheme("custom-scheme://example.com"))
    }

    @Test
    fun `isBlockedUrlScheme returns true for dangerous schemes`() {
        assertTrue(isBlockedUrlScheme("javascript:alert(1)"))
        assertTrue(isBlockedUrlScheme("file:///data/local/tmp/file.html"))
        assertTrue(isBlockedUrlScheme("content://com.example.provider/path"))
        assertTrue(isBlockedUrlScheme("intent://example.com#Intent;end"))
        assertTrue(isBlockedUrlScheme("data:text/html,<script>alert(1)</script>"))
    }

    @Test
    fun `isBlockedUrlScheme returns false for system schemes`() {
        assertFalse(isBlockedUrlScheme("mailto:user@example.com"))
        assertFalse(isBlockedUrlScheme("tel:+15555550100"))
        assertFalse(isBlockedUrlScheme("sms:+15555550100"))
        assertFalse(isBlockedUrlScheme("myapp://screen/detail"))
    }

    @Test
    fun `isBlockedUrlScheme returns false for http and https`() {
        assertFalse(isBlockedUrlScheme("http://example.com"))
        assertFalse(isBlockedUrlScheme("https://example.com"))
    }

    @Test
    fun `isBlockedUrlScheme returns false for null and blank`() {
        assertFalse(isBlockedUrlScheme(null))
        assertFalse(isBlockedUrlScheme(""))
        assertFalse(isBlockedUrlScheme("   "))
    }
}
