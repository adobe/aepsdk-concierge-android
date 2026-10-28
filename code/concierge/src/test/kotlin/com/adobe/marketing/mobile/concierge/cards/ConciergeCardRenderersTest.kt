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

package com.adobe.marketing.mobile.concierge.cards

import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.After
import org.junit.Test
import java.math.BigDecimal

class ConciergeCardRenderersTest {
    private val menu: ConciergeCardRenderer = { _, _, _ -> Unit }
    private val replacement: ConciergeCardRenderer = { _, _, _ -> Unit }

    @After
    fun tearDown() = ConciergeCardRenderers.clear()

    @Test
    fun `registered types match exactly`() {
        Concierge.registerCardRenderer("acme.menu", menu)

        assertSame(menu, ConciergeCardRenderers.rendererFor("acme.menu"))
        assertNull(ConciergeCardRenderers.rendererFor("acme.Menu"))
        assertNull(ConciergeCardRenderers.rendererFor(null))
        assertTrue(ConciergeCardRenderers.hasAnyRenderer)
    }

    @Test
    fun `the last registration for a type wins`() {
        Concierge.registerCardRenderer("acme.menu", menu)
        Concierge.registerCardRenderer("acme.menu", replacement)

        assertSame(replacement, ConciergeCardRenderers.rendererFor("acme.menu"))
    }

    @Test
    fun `blank types are rejected`() {
        expectThrows<IllegalArgumentException> { Concierge.registerCardRenderer(" ", menu) }
        assertFalse(ConciergeCardRenderers.hasAnyRenderer)
    }

    private inline fun <reified T : Throwable> expectThrows(block: () -> Unit) {
        try {
            block()
            fail("Expected ${T::class.java.simpleName}")
        } catch (e: Throwable) {
            if (e !is T) throw e
        }
    }
}

class CardJsonTest {

    @Test
    fun `copies are deep, read-only, and keep nulls and number types`() {
        val nested = mutableMapOf<String, Any?>("price" to BigDecimal("8.0000"), "none" to null)
        val source = mutableMapOf<String, Any?>(
            "id" to 7,
            "big" to 10_000_000_000L,
            "ratio" to 0.5,
            "flag" to true,
            "nested" to nested,
            "list" to mutableListOf<Any?>(1, null, mapOf("x" to "y"))
        )

        val copy = CardJson.readOnlyCopy(source)
        nested["price"] = "changed"

        assertEquals(7, copy["id"])
        assertEquals(10_000_000_000L, copy["big"])
        assertEquals(BigDecimal("8.0000"), (copy["nested"] as Map<*, *>)["price"])
        assertTrue((copy["nested"] as Map<*, *>).containsKey("none"))
        assertNull((copy["list"] as List<*>)[1])
        try {
            @Suppress("UNCHECKED_CAST")
            (copy as MutableMap<String, Any?>)["id"] = 8
            fail("copy must be read-only")
        } catch (expected: UnsupportedOperationException) {
        }
    }

    @Test
    fun `unsupported values are rejected with their path`() {
        assertRejected(mapOf("a" to Any()), "$.a")
        assertRejected(mapOf("a" to listOf(1, Double.NaN)), "$.a[1]")
        assertRejected(mapOf("a" to mapOf(1 to "x")), "$.a")
    }

    @Test
    fun `oversized values are rejected before copying their contents`() {
        try {
            CardJson.readOnlyCopy(mapOf("items" to List(100_000) { it }))
            fail("Expected the value limit to reject an oversized element")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("value limit"))
        }
    }

    @Test
    fun `toJsonValue converts nulls and nested collections`() {
        val json = CardJson.toJsonValue(mapOf("a" to null, "b" to listOf(1, mapOf("c" to "d")))) as JSONObject
        assertTrue(json.isNull("a"))
        assertEquals("d", (json.getJSONArray("b").get(1) as JSONObject).getString("c"))
        assertTrue(CardJson.toJsonValue(listOf(1)) is JSONArray)
    }

    private fun assertRejected(value: Map<String, Any?>, path: String) {
        try {
            CardJson.readOnlyCopy(value)
            fail("Expected $path to be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue("message '${e.message}' should name $path", e.message!!.startsWith(path))
        }
    }
}
