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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import java.nio.charset.StandardCharsets

/**
 * URL scheme validation for the WebView sheet content.
 * Only http and https are allowed to load inside the WebView; if the host app is a verified
 * App Link handler for the domain, the URL is forwarded to the app instead.
 * All other schemes are forwarded to the system unless explicitly blocked.
 * Blocked schemes: javascript, file, content, intent, data.
 * Internal for unit testing.
 */
internal object WebViewSheetContentSchemes {
    private val allowedSchemes = setOf("https", "http")
    internal val blockedSchemes = setOf("javascript", "file", "content", "intent", "data")

    private fun scheme(url: String) = Uri.parse(url).scheme?.lowercase() ?: ""

    fun isAllowedScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return scheme(url) in allowedSchemes
    }

    fun isBlockedScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return scheme(url) in blockedSchemes
    }
}

/**
 * WebView sheet content shown inside [WebviewOverlayDialog].
 *
 * @param url The URL to load (http/https only)
 * @param onDismiss Callback when the sheet is dismissed
 * @param modifier Optional [Modifier] for the container
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebViewSheetContent(
    url: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(AndroidColor.TRANSPARENT)
                    isScrollbarFadingEnabled = true
                    scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
                    webViewClient = SecureSheetWebViewClient(context)
                    applySecureSettings(settings)
                    setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN,
                            MotionEvent.ACTION_UP -> parent?.requestDisallowInterceptTouchEvent(true)
                        }
                        false
                    }
                }
            },
            update = { webView ->
                webView.setBackgroundColor(AndroidColor.TRANSPARENT)
                if (webView.url != url && WebViewSheetContentSchemes.isAllowedScheme(url)) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Applies security-hardened WebSettings for the sheet WebView.
 * - Enables DOM storage (localStorage/sessionStorage) so page scripts can use getItem/setItem.
 * - Disables file and content URL access to prevent local file inclusion.
 * - Enables Safe Browsing on API 26+ when available.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun applySecureSettings(settings: WebSettings) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
    settings.defaultTextEncodingName = StandardCharsets.UTF_8.name()
    settings.databaseEnabled = false
    @Suppress("DEPRECATION")
    settings.allowFileAccessFromFileURLs = false
    @Suppress("DEPRECATION")
    settings.allowUniversalAccessFromFileURLs = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        settings.safeBrowsingEnabled = true
    }
}

/**
 * WebViewClient that handles URL navigation inside the sheet WebView.
 * http/https URLs are forwarded to the host app if it is a verified App Link handler,
 * otherwise loaded in the WebView.
 * Non-web schemes are forwarded to the system unless explicitly blocked.
 * Dangerous schemes (javascript, file, content, intent, data) are blocked.
 */
private class SecureSheetWebViewClient(private val context: Context) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean = handleUrl(request.url?.toString())

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = handleUrl(url)

    // Safety net for data: URLs that bypass shouldOverrideUrlLoading on some WebView versions.
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (url != null && url.startsWith("data:", ignoreCase = true)) {
            view.stopLoading()
            if (view.canGoBack()) view.goBack() else view.loadUrl("about:blank")
            return
        }
        super.onPageStarted(view, url, favicon)
    }

    private fun handleUrl(url: String?): Boolean {
        if (url == null) return true
        if (WebViewSheetContentSchemes.isBlockedScheme(url)) return true
        if (WebViewSheetContentSchemes.isAllowedScheme(url)) {
            // http/https: forward to host app if it is a verified App Link handler,
            // otherwise let the WebView load the page.
            return tryOpenAsAppLink(context, url)
        }
        // All other schemes (e.g. mailto:, tel:, myapp://): forward to the system.
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) { }
        return true
    }
}
