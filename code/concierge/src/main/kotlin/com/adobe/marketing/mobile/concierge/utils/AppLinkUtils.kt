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

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.services.Log

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
 * Attempts to open a URL as an App Link if the host app is the verified handler
 * (e.g., listed in the domain's assetlinks.json). Returns true if the link was opened;
 * false if the host app does not handle it (caller should fall back to WebView).
 *
 * On Android 12 (API 31) and above, uses [DomainVerificationManager] to check verification
 * state. On Android 11 and below, uses [PackageManager.resolveActivity] to determine
 * the handler.
 */
internal fun tryOpenAsAppLink(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    val host = Uri.parse(url).host ?: return false

    return try {
        val wouldHandle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.isDomainVerifiedOrSelected(host)
        } else {
            context.isResolvedHandlerFor(url)
        }
        if (wouldHandle) {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        }
        wouldHandle
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "tryOpenAsAppLink failed: ${e.message}")
        false
    }
}

private fun Context.isDomainVerifiedOrSelected(host: String): Boolean {
    val manager = getSystemService(DomainVerificationManager::class.java) ?: return false
    val state = try {
        manager.getDomainVerificationUserState(packageName)?.hostToStateMap?.get(host)
            ?: DomainVerificationUserState.DOMAIN_STATE_NONE
    } catch (e: Exception) {
        Log.debug(ConciergeConstants.EXTENSION_NAME, "AppLinkUtils", "getDomainVerificationUserState failed: ${e.message}")
        return false
    }
    return state in setOf(
        DomainVerificationUserState.DOMAIN_STATE_VERIFIED,
        DomainVerificationUserState.DOMAIN_STATE_SELECTED
    )
}

private fun Context.isResolvedHandlerFor(url: String): Boolean {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName == packageName
}
