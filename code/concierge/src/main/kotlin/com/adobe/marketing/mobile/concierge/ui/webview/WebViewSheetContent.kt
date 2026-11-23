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
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.utils.isAllowedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.isBlockedUrlScheme
import com.adobe.marketing.mobile.concierge.utils.tryOpenAsAppLink
import com.adobe.marketing.mobile.concierge.utils.tryOpenInNativeApp
import com.adobe.marketing.mobile.concierge.utils.tryOpenWithSystemHandler
import com.adobe.marketing.mobile.services.Log
import java.nio.charset.StandardCharsets

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
    val client = remember(context) { SecureSheetWebViewClient(context) }

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
                    webViewClient = client
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
                if (webView.url != url && isAllowedUrlScheme(url)) {
                    client.onInitialLoad()
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
 * How a navigation inside the sheet WebView started.
 *
 * @property userInitiated The user tapped a link, or this is a server redirect of the link the
 * user opened in the sheet or of a tapped main-frame navigation.
 * @property mainFrame The navigation targets the top-level page rather than an iframe.
 * @property redirect The navigation is a server redirect.
 */
internal data class SheetNavigation(
    val userInitiated: Boolean,
    val mainFrame: Boolean,
    val redirect: Boolean
)

/**
 * WebViewClient that handles URL navigation inside the sheet WebView. Only user-initiated
 * navigations may leave the WebView:
 * - http/https main-frame navigations open the host app when it is the approved App Link handler.
 *   A tapped link (not a redirect) may also open a non-browser app for https (Android 11+).
 *   Everything else, including scripted and iframe navigations, loads in the WebView.
 * - Other non-blocked schemes (e.g. tel:, mailto:, myapp://) go to the system; without a user
 *   gesture they are dropped.
 * - Dangerous schemes (javascript, file, content, intent, data) are always blocked.
 *
 * Android 6.0 (API 23) and lower report no gesture or redirect information, so navigations there
 * keep the earlier behavior: App Links and non-web schemes open, and no other app is tried.
 */
internal class SecureSheetWebViewClient(private val context: Context) : WebViewClient() {
    /** True until the URL loaded by the sheet commits; its server redirects follow the user's tap. */
    private var initialLoadPending = true

    /** Call before loading a new URL into the sheet. */
    fun onInitialLoad() {
        initialLoadPending = true
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest
    ): Boolean {
        val mainFrame = request.isForMainFrame
        val redirect = request.isRedirect
        val userInitiated = request.hasGesture() || (mainFrame && redirect && initialLoadPending)
        return handleUrl(request.url?.toString(), SheetNavigation(userInitiated, mainFrame, redirect))
    }

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        handleUrl(url, LEGACY_NAVIGATION)

    // Safety net for data: URLs that bypass shouldOverrideUrlLoading on some WebView versions.
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        if (url != null && url.startsWith("data:", ignoreCase = true)) {
            view.stopLoading()
            if (view.canGoBack()) view.goBack() else view.loadUrl("about:blank")
            return
        }
        initialLoadPending = false
        super.onPageStarted(view, url, favicon)
    }

    internal fun handleUrl(url: String?, navigation: SheetNavigation): Boolean {
        if (url == null) return true
        if (isBlockedUrlScheme(url)) return true
        if (isAllowedUrlScheme(url)) {
            if (!navigation.userInitiated || !navigation.mainFrame) return false
            if (tryOpenAsAppLink(context, url)) return true
            return !navigation.redirect && tryOpenInNativeApp(context, url)
        }
        if (!navigation.userInitiated) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, TAG, "Dropped a non-web navigation without a user gesture")
            return true
        }
        tryOpenWithSystemHandler(context, url)
        return true
    }

    private companion object {
        const val TAG = "SecureSheetWebViewClient"
        val LEGACY_NAVIGATION = SheetNavigation(userInitiated = true, mainFrame = true, redirect = true)
    }
}
