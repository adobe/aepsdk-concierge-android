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

import android.content.Context
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import com.adobe.marketing.mobile.concierge.utils.tryOpenWithSystemHandler
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebViewSheetContentClientTest {

    private lateinit var context: Context
    private lateinit var view: WebView

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        view = mockk(relaxed = true)

        mockkStatic(Uri::class)
        mockkStatic(::tryOpenAsAppLink)
        mockkStatic(::tryOpenWithSystemHandler)

        every { Uri.parse(any()) } answers {
            val url = firstArg<String>()
            val scheme = url.substringBefore(':', "").lowercase().ifEmpty { null }
            mockk { every { this@mockk.scheme } returns scheme }
        }
        every { tryOpenAsAppLink(any(), any()) } returns false
        every { tryOpenWithSystemHandler(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkStatic(::tryOpenAsAppLink)
        unmockkStatic(::tryOpenWithSystemHandler)
    }

    // ---- shouldOverrideUrlLoading (String overload) ----

    @Test
    fun `URL with no scheme calls tryOpenWithSystemHandler`() {
        // Note: the Uri.parse mock in setUp extracts the scheme via substringBefore(':', "").
        // When no colon is present (no scheme delimiter), substringBefore returns the
        // missingDelimiterValue "" which then becomes null via ifEmpty — correctly matching
        // real Android where Uri.parse("example.com/page").scheme == null.
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "example.com/page")

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "example.com/page") }
    }

    @Test
    fun `blocked javascript scheme returns true and takes no action`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "javascript:alert(1)")

        assertTrue(result)
        verify(exactly = 0) { tryOpenAsAppLink(any(), any()) }
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    @Test
    fun `blocked file scheme returns true and takes no action`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "file:///data/local/tmp/file.html")

        assertTrue(result)
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    @Test
    fun `http URL that is an App Link returns true`() {
        every { tryOpenAsAppLink(any(), any()) } returns true

        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "http://example.com/page")

        assertTrue(result)
        verify { tryOpenAsAppLink(context, "http://example.com/page") }
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    @Test
    fun `http URL that is not an App Link returns false to load in WebView`() {
        every { tryOpenAsAppLink(any(), any()) } returns false

        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "http://example.com/page")

        assertFalse(result)
        verify { tryOpenAsAppLink(context, "http://example.com/page") }
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    @Test
    fun `https URL that is not an App Link returns false to load in WebView`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "https://example.com/page")

        assertFalse(result)
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    @Test
    fun `tel scheme calls tryOpenWithSystemHandler and returns true`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "tel:+15555550100")

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "tel:+15555550100") }
        verify(exactly = 0) { tryOpenAsAppLink(any(), any()) }
    }

    @Test
    fun `geo scheme calls tryOpenWithSystemHandler and returns true`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "geo:0,0?q=1+Apple+Park+Way")

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "geo:0,0?q=1+Apple+Park+Way") }
    }

    @Test
    fun `mailto scheme calls tryOpenWithSystemHandler and returns true`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "mailto:user@example.com")

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "mailto:user@example.com") }
    }

    @Test
    fun `custom app scheme calls tryOpenWithSystemHandler and returns true`() {
        val client = SecureSheetWebViewClient(context)
        @Suppress("DEPRECATION")
        val result = client.shouldOverrideUrlLoading(view, "myapp://screen/detail")

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "myapp://screen/detail") }
    }

    // ---- shouldOverrideUrlLoading (WebResourceRequest overload) ----

    @Test
    fun `WebResourceRequest with tel URL calls tryOpenWithSystemHandler`() {
        // Uri.parse returns a mock from setUp's any() stub (scheme = "tel" already set).
        // Stub toString() separately to avoid MockK's internal use of toString() during mock construction.
        val telUri = Uri.parse("tel:+15555550100")
        every { telUri.toString() } returns "tel:+15555550100"
        val request = mockk<WebResourceRequest> {
            every { url } returns telUri
        }

        val client = SecureSheetWebViewClient(context)
        val result = client.shouldOverrideUrlLoading(view, request)

        assertTrue(result)
        verify { tryOpenWithSystemHandler(context, "tel:+15555550100") }
    }

    @Test
    fun `WebResourceRequest with null URL returns true`() {
        val request = mockk<WebResourceRequest>()
        every { request.url } returns null

        val client = SecureSheetWebViewClient(context)
        val result = client.shouldOverrideUrlLoading(view, request)

        assertTrue(result)
        verify(exactly = 0) { tryOpenWithSystemHandler(any(), any()) }
    }

    // ---- onPageStarted ----

    @Test
    fun `onPageStarted with data URL stops loading`() {
        every { view.canGoBack() } returns false

        val client = SecureSheetWebViewClient(context)
        client.onPageStarted(view, "data:text/html,<script>alert(1)</script>", null)

        verify { view.stopLoading() }
        verify { view.loadUrl("about:blank") }
    }

    @Test
    fun `onPageStarted with data URL navigates back when history exists`() {
        every { view.canGoBack() } returns true

        val client = SecureSheetWebViewClient(context)
        client.onPageStarted(view, "data:text/html,test", null)

        verify { view.stopLoading() }
        verify { view.goBack() }
        verify(exactly = 0) { view.loadUrl(any()) }
    }

    @Test
    fun `onPageStarted with data URL is case-insensitive`() {
        every { view.canGoBack() } returns false

        val client = SecureSheetWebViewClient(context)
        client.onPageStarted(view, "DATA:text/html,test", null)

        verify { view.stopLoading() }
    }

    @Test
    fun `onPageStarted with normal URL does not stop loading`() {
        val client = SecureSheetWebViewClient(context)
        client.onPageStarted(view, "https://example.com", null)

        verify(exactly = 0) { view.stopLoading() }
    }

    @Test
    fun `onPageStarted with null URL does not stop loading`() {
        val client = SecureSheetWebViewClient(context)
        client.onPageStarted(view, null, null)

        verify(exactly = 0) { view.stopLoading() }
    }
}
