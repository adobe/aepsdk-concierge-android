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

package com.adobe.marketing.mobile.conciergetestapp.fnb.action

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Deep links that let the SDK's out-of-the-box product cards drive the F&B flow without a custom
 * renderer. BCOS renders each tapin2 location as a `productCard` whose `productPageURL` (card tap)
 * and `primary.url` (CTA) are a [locationUrl]. The SDK hands card taps to the host's
 * `ConciergeChat(handleLink = …)`; the host passes them to [handle], which turns a location link
 * into [FnbAction.SelectLocation] and returns true so the SDK doesn't open it.
 *
 * Host wiring: `ConciergeChat(viewModel, surfaces, handleLink = { url -> FnbLinks.handle(url, fnbHandler) })`.
 *
 * Format: `{scheme}://fnb/location?locationId={tapin2 location.id}&title={location.title}`. The
 * scheme is the host app's; BCOS must emit the same one.
 */
object FnbLinks {

    const val DEFAULT_SCHEME = "conciergetestapp"
    private const val HOST = "fnb"
    private const val PATH = "/location"
    private const val MAX_TITLE_LENGTH = 80

    fun locationUrl(locationId: String, title: String, scheme: String = DEFAULT_SCHEME): String =
        "$scheme://$HOST$PATH?locationId=${encode(locationId)}&title=${encode(title.take(MAX_TITLE_LENGTH))}"

    /** Returns the selection for an F&B location link on [scheme], or null for any other URL. */
    fun parseLocation(url: String, scheme: String = DEFAULT_SCHEME): FnbAction.SelectLocation? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        if (!uri.scheme.equals(scheme, ignoreCase = true) || uri.host != HOST || uri.path != PATH) return null
        val params = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
            val parts = pair.split('=', limit = 2)
            if (parts.size == 2) decode(parts[0]) to decode(parts[1]) else null
        }.toMap()
        val locationId = params["locationId"]?.trim()?.takeIf { id -> id.isNotEmpty() && id.all { it.isDigit() } } ?: return null
        return FnbAction.SelectLocation(locationId = locationId, title = params["title"].orEmpty().take(MAX_TITLE_LENGTH))
    }

    /**
     * `handleLink` adapter: dispatches location links to [handler] and returns true; returns false
     * for everything else so the SDK's default link handling applies.
     */
    fun handle(url: String, handler: FnbActionHandler, scheme: String = DEFAULT_SCHEME, onResult: (FnbActionResult) -> Unit = {}): Boolean {
        val selection = parseLocation(url, scheme) ?: return false
        handler.onAction(selection, onResult)
        return true
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault("")
}
