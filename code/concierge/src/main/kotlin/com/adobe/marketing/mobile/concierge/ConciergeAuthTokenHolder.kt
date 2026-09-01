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

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.CancellationException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Holds the app-registered [ConciergeAuthTokenProvider] and reads a token from it on demand.
 *
 * [resolveToken] calls through to the provider on every turn so the app remains the sole owner of
 * minting and refresh.
 */
internal object ConciergeAuthTokenHolder {

    private const val LOG_TAG = "ConciergeAuthTokenHolder"
    private const val PROVIDE_TOKEN_TIMEOUT_MS = 500L

    @Volatile
    private var provider: ConciergeAuthTokenProvider? = null

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "ConciergeAuthTokenProvider").apply { isDaemon = true }
    }

    /**
     * Registers [provider], replacing any previously registered one. Pass null to clear.
     */
    fun setProvider(provider: ConciergeAuthTokenProvider?) {
        this.provider = provider
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            if (provider == null) "Auth token provider cleared" else "Auth token provider registered"
        )
    }

    /**
     * Returns the token for the turn being built, or null when the turn should be sent without one.
     *
     * Null is returned when no provider is registered, when the provider returns null or a blank
     * value, when the provider throws, or when it doesn't return within [PROVIDE_TOKEN_TIMEOUT_MS].
     * A failing provider is logged and treated as "no token" so that a failure to mint degrades the
     * turn rather than failing it.
     */
    fun resolveToken(): String? {
        val current = provider ?: return null
        return try {
            executor.submit(Callable { current.provideToken() })
                .get(PROVIDE_TOKEN_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                ?.takeIf { it.isNotBlank() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                LOG_TAG,
                "Auth token provider threw ${e.javaClass.simpleName}; sending turn without a token"
            )
            null
        }
    }
}
