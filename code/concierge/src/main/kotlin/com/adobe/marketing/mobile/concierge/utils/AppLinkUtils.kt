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

package com.adobe.marketing.mobile.concierge.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

private val ALLOWED_URL_SCHEMES = setOf("https", "http")
private val BLOCKED_URL_SCHEMES = setOf("javascript", "file", "content", "intent", "data")
private const val NATIVE_APP_URL_SCHEME = "https"
private val APPROVED_DOMAIN_STATES = setOf(
    DomainVerificationUserState.DOMAIN_STATE_VERIFIED,
    DomainVerificationUserState.DOMAIN_STATE_SELECTED
)

/**
 * Returns true if the URL has an http or https scheme.
 */
internal fun isAllowedUrlScheme(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return Uri.parse(url)?.scheme?.lowercase() in ALLOWED_URL_SCHEMES
}

/**
 * Returns true if the URL has a scheme that must never be opened or loaded
 * (javascript, file, content, intent, data).
 */
internal fun isBlockedUrlScheme(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return Uri.parse(url)?.scheme?.lowercase() in BLOCKED_URL_SCHEMES
}

/**
 * Opens a URL using the system handler via [Intent.ACTION_VIEW].
 * Intended for non-http/https schemes such as tel:, mailto:, geo:, sms:, and custom deep links.
 * Requires [Intent.FLAG_ACTIVITY_NEW_TASK] since the caller may not be an Activity context.
 */
internal fun tryOpenWithSystemHandler(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "tryOpenWithSystemHandler failed: ${e.message}")
    }
}

/**
 * Opens an http/https URL in the host app when the host app is the approved handler for it
 * (e.g., listed in the domain's assetlinks.json). Returns true if the host app was started; false
 * if it does not handle this URL (caller should continue its fallback).
 *
 * The launch targets only the host package, and the host app must declare an activity whose
 * intent filter matches the full URL, including its path. On Android 12 (API 31) and above, the
 * domain (or a matching wildcard domain) must also be verified or user-selected for the host app,
 * and the user must not have turned off link handling for it. On Android 11 and below, the host
 * app must be the default handler for the URL.
 */
internal fun tryOpenAsAppLink(context: Context, url: String): Boolean {
    if (!isAllowedUrlScheme(url)) return false

    return try {
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return false
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            setPackage(context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val approved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.isLinkHandlingApproved(host)
        } else {
            context.isResolvedHandlerFor(url)
        }
        if (!approved || !context.hasActivityFor(intent)) return false
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "tryOpenAsAppLink failed: ${e.message}")
        false
    }
}

/**
 * Attempts to open an https URL in an installed non-browser app that handles it. Returns true if
 * an app was started; false if no non-browser app handles the URL, the scheme is not https, or the
 * device is below Android 11 (API 30), where [Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER] is
 * unavailable. Android may show a chooser when several non-browser apps handle the URL. Callers
 * should fall back to the WebView.
 */
internal fun tryOpenInNativeApp(context: Context, url: String): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false
    if (Uri.parse(url)?.scheme?.lowercase() != NATIVE_APP_URL_SCHEME) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
        })
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "tryOpenInNativeApp failed: ${e.message}")
        false
    }
}

@RequiresApi(Build.VERSION_CODES.S)
private fun Context.isLinkHandlingApproved(host: String): Boolean {
    val manager = getSystemService(DomainVerificationManager::class.java) ?: return false
    val userState = try {
        manager.getDomainVerificationUserState(packageName) ?: return false
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "getDomainVerificationUserState failed: ${e.message}")
        return false
    }
    if (!userState.isLinkHandlingAllowed) return false
    val states = userState.hostToStateMap
    return hostCandidates(host).any { states[it] in APPROVED_DOMAIN_STATES }
}

/** The host itself, then each wildcard form that can declare it (e.g. `*.example.com`). */
private fun hostCandidates(host: String): Sequence<String> = sequence {
    yield(host)
    var dot = host.indexOf('.')
    while (dot >= 0 && dot < host.lastIndex) {
        yield("*" + host.substring(dot))
        dot = host.indexOf('.', dot + 1)
    }
}

private fun Context.hasActivityFor(intent: Intent): Boolean =
    packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

private fun Context.isResolvedHandlerFor(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName == packageName
}
