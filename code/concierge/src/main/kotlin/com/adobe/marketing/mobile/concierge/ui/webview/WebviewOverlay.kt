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
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * URL scheme validation for the in-app WebView overlay.
 * Only http and https are allowed; file:, content:, and other schemes are blocked.
 * Internal for unit testing.
 */
internal object WebviewOverlaySchemes {
    private val allowedSchemes = setOf("https", "http")

    fun isAllowedScheme(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val scheme = url.substringBefore(':', "").lowercase()
        return scheme in allowedSchemes
    }
}

/**
 * Fullscreen in-app overlay that displays a URL in a WebView with a close button.
 *
 * @param url The URL to load in the WebView (only http/https are loaded)
 * @param onDismiss Callback when the user closes the overlay
 * @param modifier Optional [Modifier] for the overlay container
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun WebviewOverlay(
    url: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val style = ConciergeStyles.webviewStyle

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(style.contentBackgroundColor)
    ) {
        // Top bar with primary background and close button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(style.topBarBackgroundColor)
                .padding(style.topBarPadding)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(style.closeIconSize)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close",
                    tint = style.topBarContentColor
                )
            }
        }

        // WebView content area
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = {
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = SecureOverlayWebViewClient()
                        applySecureSettings(settings)
                    }
                },
                update = { webView ->
                    if (webView.url != url && WebviewOverlaySchemes.isAllowedScheme(url)) {
                        webView.loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Applies security-hardened WebSettings for the overlay WebView.
 * - Disables file and content URL access to prevent local file inclusion.
 * - Disables mixed content (HTTPS page loading HTTP resources).
 * - Enables Safe Browsing on API 26+ when available.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun applySecureSettings(settings: android.webkit.WebSettings) {
    settings.javaScriptEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    @Suppress("DEPRECATION")
    settings.allowFileAccessFromFileURLs = false
    @Suppress("DEPRECATION")
    settings.allowUniversalAccessFromFileURLs = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        settings.safeBrowsingEnabled = true
    }
}

/**
 * WebViewClient that restricts navigation to http/https only, blocking file:, content:, and other schemes.
 */
private class SecureOverlayWebViewClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean = !WebviewOverlaySchemes.isAllowedScheme(request.url?.toString())

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = !WebviewOverlaySchemes.isAllowedScheme(url)
}
