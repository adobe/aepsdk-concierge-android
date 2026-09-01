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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConciergeAuthTokenHolderTest {

    @After
    fun tearDown() {
        ConciergeAuthTokenHolder.setProvider(null)
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
