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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        ConciergeAuthTokenHolder.setProvider { "token-abc" }

        assertEquals("token-abc", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null after the provider is cleared`() {
        ConciergeAuthTokenHolder.setProvider { "token-abc" }
        ConciergeAuthTokenHolder.setProvider(null)

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns the value from the most recently set provider`() {
        ConciergeAuthTokenHolder.setProvider { "first" }
        ConciergeAuthTokenHolder.setProvider { "second" }

        assertEquals("second", ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider returns null`() {
        ConciergeAuthTokenHolder.setProvider { null }

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider returns a blank token`() {
        ConciergeAuthTokenHolder.setProvider { "   " }

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider throws`() {
        ConciergeAuthTokenHolder.setProvider { throw IllegalStateException("mint failed") }

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken returns null when the provider does not return within the timeout`() {
        ConciergeAuthTokenHolder.setProvider {
            Thread.sleep(1000)
            "too-late"
        }

        assertNull(ConciergeAuthTokenHolder.resolveToken())
    }

    @Test
    fun `resolveToken never logs the token value`() {
        ConciergeAuthTokenHolder.setProvider { "super-secret-token" }

        ConciergeAuthTokenHolder.resolveToken()

        verify(exactly = 0) { Log.debug(any(), any(), match { it.contains("super-secret-token") }) }
        verify(exactly = 0) { Log.warning(any(), any(), match { it.contains("super-secret-token") }) }
    }

    @Test
    fun `resolveToken logs only the exception class name when the provider throws, never the exception message`() {
        ConciergeAuthTokenHolder.setProvider {
            throw IllegalStateException("token was super-secret-token")
        }

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
        ConciergeAuthTokenHolder.setProvider {
            callCount++
            "token-$callCount"
        }

        assertEquals("token-1", ConciergeAuthTokenHolder.resolveToken())
        assertEquals("token-2", ConciergeAuthTokenHolder.resolveToken())
        assertEquals("token-3", ConciergeAuthTokenHolder.resolveToken())
        assertEquals(3, callCount)
    }
}
