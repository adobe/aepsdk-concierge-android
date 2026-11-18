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

package com.adobe.marketing.mobile.conciergetestapp.parking

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.adobe.marketing.mobile.concierge.Concierge
import org.json.JSONObject

private const val TAG = "ParkWhizBooking"
private const val BRIDGE_NAME = "ParkWhizBridge"
private val ALLOWED_ORIGINS = setOf("https://*.parkwhiz.com")

// The widget calls window.parent.postMessage; in a top-level WebView parent === window, so the
// message lands on the page itself.
private val LISTENER_SCRIPT = """
    (function () {
      if (window.__pwBridgeInstalled) return;
      window.__pwBridgeInstalled = true;
      window.addEventListener('message', function (e) {
        var d = e.data;
        if (typeof d === 'string') { try { d = JSON.parse(d); } catch (_) { return; } }
        if (d && d.source === 'ParkWhiz' && d.action === 'purchase' && d.value && d.value.status === 'success') {
          $BRIDGE_NAME.postMessage(JSON.stringify(d.value));
        }
      });
    })();
""".trimIndent()

/** True for https ParkWhiz URLs (e.g. the "Book on ParkWhiz" checkout widget). */
fun isParkWhizUrl(url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return false
    val host = uri.host?.lowercase() ?: return false
    return uri.scheme.equals("https", ignoreCase = true) &&
        (host == "parkwhiz.com" || host.endsWith(".parkwhiz.com"))
}

/** Extracts the bookingId from a ParkWhiz purchase payload (`{"status":"success","bookingId":123}`). */
internal fun parseBookingId(payload: String): String? = runCatching {
    JSONObject(payload).opt("bookingId")?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}.getOrNull()

/**
 * Accumulates booking IDs and pushes them to Concierge XDM context. The full array is sent each time
 * because `updateXDMContext` replaces arrays rather than appending.
 */
object ParkWhizBookingStore {
    private val bookingIds = linkedSetOf<String>()

    @Synchronized
    fun record(bookingId: String): Boolean {
        if (!bookingIds.add(bookingId)) return false
        Concierge.updateXDMContext(
            mapOf(
                "_sacramentokings" to mapOf(
                    "conciergeContext" to mapOf(
                        "parkingBookings" to mapOf(
                            "pastBookings" to bookingIds.map { mapOf("bookingId" to it) }
                        )
                    )
                )
            )
        )
        Log.d(TAG, "updateXDMContext pastBookings=$bookingIds")
        return true
    }
}

internal fun handleParkWhizBooking(
    payload: String,
    record: (String) -> Boolean = ParkWhizBookingStore::record,
    sendHandoff: (String, Map<String, Any>) -> Unit = { hint, fields ->
        Concierge.sendDataHandoff(routingHint = hint, xdmFields = fields) { accepted, reason ->
            if (!accepted) Log.w(TAG, "Parking booking status handoff failed: $reason")
        }
    }
): String? {
    val bookingId = parseBookingId(payload) ?: return null
    if (!record(bookingId)) return null
    sendHandoff(
        "parking-booking-status",
        mapOf(
            "_sacramentokings" to mapOf(
                "conciergeAction" to mapOf(
                    "parkingHandoff" to mapOf("bookingId" to bookingId)
                )
            )
        )
    )
    return bookingId
}

/** Full-screen WebView that hosts the ParkWhiz checkout and reports completed bookings. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ParkWhizBookingSheet(url: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val onBooked: (String) -> Unit = { payload ->
        Log.d(TAG, "Booking callback: $payload")
        val bookingId = handleParkWhizBooking(payload)
        if (bookingId != null) {
            Toast.makeText(context, "ParkWhiz booking: $bookingId", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Box(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Text("Close")
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        installBridge(this, onBooked)
                        loadUrl(url)
                    }
                },
                onRelease = { it.destroy() }
            )
        }
    }
}

@SuppressLint("RequiresFeature")
private fun installBridge(webView: WebView, onBooked: (String) -> Unit) {
    val canListen = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
    val canInject = WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
    if (!canListen) {
        Log.w(TAG, "WEB_MESSAGE_LISTENER unsupported; booking callback unavailable")
        webView.webViewClient = WebViewClient()
        return
    }
    WebViewCompat.addWebMessageListener(webView, BRIDGE_NAME, ALLOWED_ORIGINS) { _, message, _, _, _ ->
        message.data?.let(onBooked)
    }
    if (canInject) {
        WebViewCompat.addDocumentStartJavaScript(webView, LISTENER_SCRIPT, ALLOWED_ORIGINS)
        webView.webViewClient = WebViewClient()
    } else {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String?) {
                if (url != null && isParkWhizUrl(url)) view.evaluateJavascript(LISTENER_SCRIPT, null)
            }
        }
    }
    Log.d(TAG, "Bridge installed (documentStartScript=$canInject)")
}
