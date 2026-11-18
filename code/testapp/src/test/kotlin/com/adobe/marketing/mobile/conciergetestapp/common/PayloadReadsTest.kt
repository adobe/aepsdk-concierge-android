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

package com.adobe.marketing.mobile.conciergetestapp.common

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayloadReadsTest {

    @Test
    fun `long reads every integral JSON number shape`() {
        assertEquals(42L, PayloadReads.long(42))
        assertEquals(42L, PayloadReads.long(42L))
        assertEquals(42L, PayloadReads.long(42.0))
        assertEquals(-7L, PayloadReads.long(-7.0))
        assertEquals(42L, PayloadReads.long(BigDecimal("42.000")))
        assertEquals(42L, PayloadReads.long(" 42 "))
        assertEquals(9_007_199_254_740_992L, PayloadReads.long(9_007_199_254_740_992.0))
    }

    @Test
    fun `long rejects fractional, non-finite and out-of-range values instead of saturating`() {
        assertNull(PayloadReads.long(42.5))
        assertNull(PayloadReads.long(1e300))
        assertNull(PayloadReads.long(-1e300))
        assertNull(PayloadReads.long(1e19))
        assertNull(PayloadReads.long(Double.NaN))
        assertNull(PayloadReads.long(Double.POSITIVE_INFINITY))
        assertNull(PayloadReads.long(BigDecimal("1e30")))
        assertNull(PayloadReads.long(BigDecimal("1.5")))
        assertNull(PayloadReads.long("4.2"))
        assertNull(PayloadReads.long("99999999999999999999"))
        assertNull(PayloadReads.long(true))
        assertNull(PayloadReads.long(null))
    }

    @Test
    fun `int is null outside the Int range`() {
        assertEquals(7, PayloadReads.int(7L))
        assertEquals(Int.MIN_VALUE, PayloadReads.int(Int.MIN_VALUE.toLong()))
        assertNull(PayloadReads.int(Int.MAX_VALUE.toLong() + 1))
        assertNull(PayloadReads.int(Long.MAX_VALUE))
    }

    @Test
    fun `id is a bounded non-blank string or an integral number`() {
        assertEquals("19289", PayloadReads.id(" 19289 "))
        assertEquals("19289", PayloadReads.id(19289))
        assertEquals("19289", PayloadReads.id(19289.0))
        assertEquals("a".repeat(64), PayloadReads.id("a".repeat(64)))
        assertNull(PayloadReads.id("a".repeat(65)))
        assertNull(PayloadReads.id("  "))
        assertNull(PayloadReads.id(19289.5))
        assertNull(PayloadReads.id(1e300))
        assertNull(PayloadReads.id(null))
    }

    @Test
    fun `bool accepts booleans and true or false strings only`() {
        assertEquals(true, PayloadReads.bool(true))
        assertEquals(true, PayloadReads.bool(" TRUE "))
        assertEquals(false, PayloadReads.bool("false"))
        assertNull(PayloadReads.bool("yes"))
        assertNull(PayloadReads.bool(1))
        assertNull(PayloadReads.bool(null))
    }

    @Test
    fun `obj keeps only string-keyed maps`() {
        val good = mapOf("a" to 1)
        assertEquals(good, PayloadReads.obj(good))
        assertNull(PayloadReads.obj(mapOf(1 to "a")))
        assertNull(PayloadReads.obj(listOf(good)))
    }

    @Test
    fun `objectSequence reads at most maxEntries entries`() {
        val good = mapOf("a" to 1)
        val entries = listOf(good, "x", good, good)
        assertEquals(1, PayloadReads.objectSequence(entries, maxEntries = 2).count())
        assertEquals(3, PayloadReads.objectSequence(entries, maxEntries = 10).count())
        assertEquals(0, PayloadReads.objectSequence(entries, maxEntries = -1).count())
    }

    @Test
    fun `objectSequence skips malformed objects within its raw prefix`() {
        val good = mapOf("a" to 1)
        val entries = listOf(good, mapOf(1 to "a"), "x", good)
        assertEquals(listOf(good), PayloadReads.objectSequence(entries, maxEntries = 3).toList())
        assertEquals(listOf(good, good), PayloadReads.objectSequence(entries, maxEntries = 4).toList())
        assertEquals(emptyList<Map<String, Any?>>(), PayloadReads.objectSequence("not a list", 4).toList())
    }

    @Test
    fun `name sanitizes and clamps, and cents delegates to toCents`() {
        assertEquals("Market Cafe 122", PayloadReads.name(" Market\n Cafe\t122 "))
        assertEquals(PayloadLimits.MAX_NAME_LENGTH + 1, PayloadReads.name("n".repeat(200)).length)
        assertEquals("", PayloadReads.name(42))
        assertEquals(850L, PayloadReads.cents("8.50"))
        assertNull(PayloadReads.cents("1e100000000"))
    }
}
