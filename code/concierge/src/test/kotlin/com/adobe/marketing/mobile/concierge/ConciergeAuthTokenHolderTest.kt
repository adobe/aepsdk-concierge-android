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
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConciergeAuthTokenHolderTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.debug(any(), any(), any()) } just Runs
        every { Log.warning(any(), any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        ConciergeAuthTokenHolder.setProvider(null)
        unmockkStatic(Log::class)
    }

    @Test
    fun `resolveToken returns null when no provider is set`() {
        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns the value supplied by the provider`() {
        ConciergeAuthTokenHolder.setProvider(provider = { "token-abc" })

        assertEquals("token-abc", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null after the provider is cleared`() {
        ConciergeAuthTokenHolder.setProvider(provider = { "token-abc" })
        ConciergeAuthTokenHolder.setProvider(null)

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns the value from the most recently set provider`() {
        ConciergeAuthTokenHolder.setProvider(provider = { "first" })
        ConciergeAuthTokenHolder.setProvider(provider = { "second" })

        assertEquals("second", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider returns null`() {
        ConciergeAuthTokenHolder.setProvider(provider = { null })

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider returns a blank token`() {
        ConciergeAuthTokenHolder.setProvider(provider = { "   " })

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider throws`() {
        ConciergeAuthTokenHolder.setProvider(provider = { throw IllegalStateException("mint failed") })

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider does not return within the timeout`() {
        ConciergeAuthTokenHolder.setProvider(
            provider = {
                Thread.sleep(200)
                "too-late"
            },
            timeoutMillis = 50L
        )

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken respects a custom timeout`() {
        ConciergeAuthTokenHolder.setProvider(
            provider = {
                Thread.sleep(50)
                "token-abc"
            },
            timeoutMillis = 500L
        )

        assertEquals("token-abc", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `default timeout matches the value shared with iOS`() {
        assertEquals(3000L, ConciergeAuthTokenHolder.DEFAULT_PROVIDE_TOKEN_TIMEOUT_MS)
    }

    @Test
    fun `max timeout matches the value shared with iOS`() {
        assertEquals(600_000L, ConciergeAuthTokenHolder.MAX_PROVIDE_TOKEN_TIMEOUT_MS)
    }

    @Test
    fun `clampTimeoutMillis clamps a non-positive value to zero`() {
        assertEquals(0L, ConciergeAuthTokenHolder.clampTimeoutMillis(0L))
        assertEquals(0L, ConciergeAuthTokenHolder.clampTimeoutMillis(-1L))
        assertEquals(0L, ConciergeAuthTokenHolder.clampTimeoutMillis(Long.MIN_VALUE))
    }

    @Test
    fun `clampTimeoutMillis clamps an excessive value to the max`() {
        assertEquals(
            ConciergeAuthTokenHolder.MAX_PROVIDE_TOKEN_TIMEOUT_MS,
            ConciergeAuthTokenHolder.clampTimeoutMillis(Long.MAX_VALUE)
        )
    }

    @Test
    fun `clampTimeoutMillis leaves an in-range value unchanged`() {
        assertEquals(1500L, ConciergeAuthTokenHolder.clampTimeoutMillis(1500L))
    }

    @Test
    fun `setProvider with a non-positive timeout degrades every turn immediately instead of throwing`() {
        ConciergeAuthTokenHolder.setProvider(
            provider = {
                Thread.sleep(50)
                "too-late"
            },
            timeoutMillis = 0L
        )

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken degrades gracefully when the executor pool and queue are both saturated`() {
        val poolSize = ConciergeAuthTokenHolder.EXECUTOR_POOL_SIZE
        val totalCapacity = poolSize + ConciergeAuthTokenHolder.EXECUTOR_QUEUE_CAPACITY

        val blockLatch = CountDownLatch(1)
        val runningLatch = CountDownLatch(poolSize)
        ConciergeAuthTokenHolder.setProvider(
            provider = {
                runningLatch.countDown()
                blockLatch.await(5, TimeUnit.SECONDS)
                "token"
            },
            timeoutMillis = 5000L
        )

        // Fill the pool + bounded queue, all blocked on the latch.
        val saturatingThreads = List(totalCapacity) {
            Thread { ConciergeAuthTokenHolder.resolveToken() }.also { it.start() }
        }
        // Deterministically confirm the pool itself is saturated (all threads actively running)
        // before allowing the brief buffer below for the remaining calls to land in the queue.
        assertTrue(
            "All $poolSize pool threads should be running the provider",
            runningLatch.await(2, TimeUnit.SECONDS)
        )
        Thread.sleep(200)

        val start = System.currentTimeMillis()
        val result = ConciergeAuthTokenHolder.resolveToken()
        val elapsed = System.currentTimeMillis() - start

        assertNull("21st concurrent call should degrade to null once pool and queue are full", result)
        assertTrue("Rejection should fail fast rather than wait out the timeout", elapsed < 2000L)

        blockLatch.countDown()
        saturatingThreads.forEach { it.join(6000) }
    }

    @Test
    fun `resolveToken never logs the token value`() {
        ConciergeAuthTokenHolder.setProvider(provider = { "super-secret-token" })

        ConciergeAuthTokenHolder.resolveToken()

        verify(exactly = 0) { Log.debug(any(), any(), match { it.contains("super-secret-token") }) }
        verify(exactly = 0) { Log.warning(any(), any(), match { it.contains("super-secret-token") }) }
    }

    @Test
    fun `resolveToken logs only the exception class name when the provider throws, never the exception message`() {
        ConciergeAuthTokenHolder.setProvider(provider = {
            throw IllegalStateException("token was super-secret-token")
        })

        ConciergeAuthTokenHolder.resolveToken()

        verify {
            Log.warning(any(), any(), match {
                it.contains("IllegalStateException") && !it.contains("super-secret-token")
            })
        }
    }

    @Test
    fun `resolveToken invokes the provider on every call so the token is never cached`() {
        var callCount = 0
        ConciergeAuthTokenHolder.setProvider(provider = {
            callCount++
            "token-$callCount"
        })

        assertEquals("token-1", ConciergeAuthTokenHolder.resolveToken())
        assertEquals("token-2", ConciergeAuthTokenHolder.resolveToken())
        assertEquals("token-3", ConciergeAuthTokenHolder.resolveToken())
        assertEquals(3, callCount)
    }
}
