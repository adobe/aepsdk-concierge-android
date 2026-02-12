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

class WebviewOverlaySchemesTest {

    @Test
    fun `isAllowedScheme returns true for https URL`() {
        assertTrue(WebviewOverlaySchemes.isAllowedScheme("https://example.com"))
        assertTrue(WebviewOverlaySchemes.isAllowedScheme("HTTPS://example.com/path"))
    }

    @Test
    fun `isAllowedScheme returns true for http URL`() {
        assertTrue(WebviewOverlaySchemes.isAllowedScheme("http://example.com"))
        assertTrue(WebviewOverlaySchemes.isAllowedScheme("HTTP://example.com/path?q=1"))
    }

    @Test
    fun `isAllowedScheme returns false for null`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme(null))
    }

    @Test
    fun `isAllowedScheme returns false for blank string`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme(""))
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("   "))
    }

    @Test
    fun `isAllowedScheme returns false for file scheme`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("file:///android_asset/index.html"))
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("file:///data/local/tmp/file.html"))
    }

    @Test
    fun `isAllowedScheme returns false for content scheme`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("content://com.example.provider/path"))
    }

    @Test
    fun `isAllowedScheme returns false for javascript scheme`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("javascript:alert(1)"))
    }

    @Test
    fun `isAllowedScheme returns false for intent scheme`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("intent://example.com#Intent;end"))
    }

    @Test
    fun `isAllowedScheme returns false for unknown scheme`() {
        assertFalse(WebviewOverlaySchemes.isAllowedScheme("custom-scheme://example.com"))
    }
}
