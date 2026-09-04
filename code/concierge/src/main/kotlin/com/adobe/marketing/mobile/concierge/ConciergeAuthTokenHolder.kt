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
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Holds the app-registered [ConciergeAuthTokenProvider] and reads a token from it on demand.
 *
 * [resolveToken] calls through to the provider on every turn so the app remains the sole owner of
 * minting and refresh.
 */
internal object ConciergeAuthTokenHolder {

    private const val LOG_TAG = "ConciergeAuthTokenHolder"

    /** Kept in sync with the iOS SDK's default; change both together. */
    const val DEFAULT_PROVIDE_TOKEN_TIMEOUT_MS = 3000L

    /**
     * Upper bound on the provider timeout. Out-of-range values are clamped rather than rejected, 
     * so a bad computed timeout degrades the turn instead of crashing the host app.
     */
    internal const val MAX_PROVIDE_TOKEN_TIMEOUT_MS = 600_000L // 10 minutes

    /** Clamps a caller-supplied timeout to `0..MAX_PROVIDE_TOKEN_TIMEOUT_MS`. */
    internal fun clampTimeoutMillis(timeoutMillis: Long): Long =
        timeoutMillis.coerceIn(0, MAX_PROVIDE_TOKEN_TIMEOUT_MS)

    // Sized for a handful of concurrent turns (multiple ConciergeChatViewModel instances, or
    // overlapping chat + feedback calls) rather than heavy parallel load: 4 threads run
    // concurrently, up to 16 more queue behind them, and the 21st concurrent call is rejected
    // outright instead of growing an unbounded backlog.
    internal const val EXECUTOR_POOL_SIZE = 4
    internal const val EXECUTOR_QUEUE_CAPACITY = 16

    private data class Registration(
        val provider: ConciergeAuthTokenProvider?,
        val timeoutMillis: Long
    )

    // A single volatile field so a concurrent setProvider() can't be observed as a torn
    // combination of the new provider with the old timeout (or vice versa).
    @Volatile
    private var registration = Registration(null, DEFAULT_PROVIDE_TOKEN_TIMEOUT_MS)

    // Bounded queue + the default AbortPolicy: once the pool and queue are both full, submit()
    // throws RejectedExecutionException instead of growing an unbounded backlog of tasks behind
    // a run of stuck providers.
    private val executor = ThreadPoolExecutor(
        EXECUTOR_POOL_SIZE,
        EXECUTOR_POOL_SIZE,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(EXECUTOR_QUEUE_CAPACITY)
    ) { runnable ->
        Thread(runnable, "ConciergeAuthTokenProvider").apply { isDaemon = true }
    }

    /**
     * Registers [provider], replacing any previously registered one. Pass null to clear.
     *
     * @param timeoutMillis how long [resolveToken] waits for [provider] before degrading the turn.
     * Clamped to 0..[MAX_PROVIDE_TOKEN_TIMEOUT_MS]; a non-positive value degrades every turn
     * immediately rather than being rejected.
     */
    fun setProvider(
        provider: ConciergeAuthTokenProvider?,
        timeoutMillis: Long = DEFAULT_PROVIDE_TOKEN_TIMEOUT_MS
    ) {
        registration = Registration(provider, clampTimeoutMillis(timeoutMillis))
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
     * value, when the provider throws, or when it doesn't return within the configured timeout.
     * A failing provider is logged and treated as "no token" so that a failure to mint degrades the
     * turn rather than failing it.
     */
    fun resolveToken(): String? {
        val (provider, timeout) = registration
        val current = provider ?: return null
        return try {
            val future = executor.submit(Callable { current.provideToken() })
            try {
                future.get(timeout, TimeUnit.MILLISECONDS)?.takeIf { it.isNotBlank() }
            } finally {
                future.cancel(true)
            }
        } catch (e: Exception) {
            if (e is InterruptedException) {
                Thread.currentThread().interrupt()
            }
            val cause = if (e is ExecutionException) e.cause ?: e else e
            Log.warning(
                ConciergeConstants.EXTENSION_NAME,
                LOG_TAG,
                "Unable to resolve auth token (${cause.javaClass.simpleName}); sending turn without a token"
            )
            null
        }
    }
}
