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

package com.adobe.marketing.mobile.conciergetestapp.fnb

import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.FnbOrderStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FnbOrderStoreTest {
    private val published = mutableListOf<Map<String, Any?>>()
    private val store = FnbOrderStore { published += it }

    @Test
    fun `a second menu makes the first read-only, and redisplaying the first doesn't take it back`() {
        store.menuShown("menu-1", "s1")
        assertTrue(store.isActionableMenu(store.state.value, "menu-1", "s1"))

        store.menuShown("menu-2", "s1")
        store.menuShown("menu-1", "s1")

        assertFalse(store.isActionableMenu(store.state.value, "menu-1", "s1"))
        assertTrue(store.isActionableMenu(store.state.value, "menu-2", "s1"))
        assertTrue("a menu displayed for the first time is actionable", store.isActionableMenu(store.state.value, "menu-3", "s1"))
        assertTrue("rendering a menu doesn't change order context", published.isEmpty())
    }

    @Test
    fun `the first unpaid cart becomes the current order and is published`() {
        store.cartShown("cart-1", "s1", "98765", isPaid = false)
        store.cartShown("cart-2", "s1", "98765", isPaid = false)

        assertEquals("98765", store.currentOrderId)
        assertEquals(
            listOf(mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("currentOrder" to mapOf("orderId" to 98765L)))))),
            published
        )
    }

    @Test
    fun `locations publish event context once per card and never on recomposition`() {
        val events = mutableListOf<Long>()
        val locationStore = FnbOrderStore(publishContext = {}, publishEventContext = events::add)

        locationStore.locationsShown("locations-1", "s1", 12345L)
        locationStore.locationsShown("locations-1", "s1", 12345L)
        locationStore.locationsShown("locations-1", "s1", 99999L)

        assertEquals(listOf(12345L), events)
    }

    @Test
    fun `location receipt leaves the following menu active across repeated state reads`() {
        val events = mutableListOf<Long>()
        val store = FnbOrderStore(publishEventContext = events::add, publishContext = {})
        store.locationsShown("locations-1", "s1", 12345L)
        store.menuShown("menu-1", "s1")

        repeat(3) {
            store.locationsShown("locations-1", "s1", 12345L)
            store.menuShown("menu-1", "s1")
            assertEquals("menu-1", store.state.value.latestMenuCardId)
            assertTrue(store.isActionableMenu(store.state.value, "menu-1", "s1"))
        }
        assertEquals(listOf(12345L), events)
    }

    @Test
    fun `paid, blank and redisplayed carts don't change the current order`() {
        store.cartShown("cart-1", "s1", "1", isPaid = true)
        store.cartShown("cart-2", "s1", " ", isPaid = false)
        assertNull(store.currentOrderId)

        store.cartShown("cart-3", "s1", "2", isPaid = false)
        store.cartShown("cart-1", "s1", "3", isPaid = false)
        assertEquals("2", store.currentOrderId)
    }

    @Test
    fun `non-numeric order identifiers are not published as fnbOrders`() {
        store.cartShown("cart-1", "s1", "order-guid", isPaid = false)

        assertNull(store.currentOrderId)
        assertTrue(published.isEmpty())
    }

    @Test
    fun `a later old cart cannot replace the current order or close its menu`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "200", isPaid = false)
        store.menuShown("menu-2", "s1")
        store.cartShown("cart-old", "s1", "100", isPaid = false)

        assertEquals("200", store.currentOrderId)
        assertTrue(store.isActionableMenu(store.state.value, "menu-2", "s1"))

        store.checkedOut("100")

        assertEquals("200", store.currentOrderId)
        assertTrue(store.isActionableMenu(store.state.value, "menu-2", "s1"))
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
    }

    @Test
    fun `checkout moves the current order to past orders and closes every menu`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "98765", isPaid = false)
        published.clear()

        store.checkedOut("98765")

        assertNull(store.currentOrderId)
        assertEquals(listOf("98765"), store.state.value.submittedOrderIds)
        assertFalse(store.isActionableMenu(store.state.value, "menu-1", "s1"))
        assertEquals(
            listOf(
                mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf(
                    "currentOrder" to null,
                    "pastOrders" to listOf(mapOf("orderId" to 98765L))
                ))))
            ),
            published
        )
    }

    @Test
    fun `a later order keeps earlier orders for status lookup`() {
        store.cartShown("cart-1", "s1", "100", isPaid = false)
        store.checkedOut("100")
        store.menuShown("menu-2", "s1")
        store.cartShown("cart-2", "s1", "200", isPaid = false)
        store.checkedOut("200")

        assertEquals(listOf("100", "200"), store.state.value.submittedOrderIds)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf(
                "currentOrder" to null,
                "pastOrders" to listOf(mapOf("orderId" to 100L), mapOf("orderId" to 200L))
            )))),
            published.last()
        )
    }

    @Test
    fun `a submitted order's cart never becomes current again, and reopening checkout changes nothing`() {
        store.cartShown("cart-1", "s1", "100", isPaid = false)
        store.checkedOut("100")
        store.menuShown("menu-2", "s1")
        val before = published.size

        store.cartShown("cart-2", "s1", "100", isPaid = false)
        store.checkedOut("100")

        assertNull(store.currentOrderId)
        assertTrue("reopening checkout keeps the newer menu open", store.isActionableMenu(store.state.value, "menu-2", "s1"))
        assertEquals(before, published.size)
    }

    @Test
    fun `a card from a new session resets the store`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "100", isPaid = false)
        store.checkedOut("100")
        store.cartShown("cart-2", "s1", "200", isPaid = false)

        store.menuShown("menu-2", "s2")

        assertNull(store.currentOrderId)
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
        assertTrue(store.isActionableMenu(store.state.value, "menu-2", "s2"))
        store.menuShown("menu-1", "s1")
        assertTrue("an older session's card can't rebind the store", store.isActionableMenu(store.state.value, "menu-2", "s2"))
    }

    @Test
    fun `reused card ids are scoped to their session`() {
        store.menuShown("menu-1", "s1")

        store.menuShown("menu-1", "s2")

        assertTrue(store.isActionableMenu(store.state.value, "menu-1", "s2"))
        assertFalse(store.isActionableMenu(store.state.value, "menu-1", "s1"))
    }

    @Test
    fun `identity reset clears local order state and rejects cards from the old session`() {
        val events = mutableListOf<Long>()
        val store = FnbOrderStore(
            publishContext = { published += it },
            publishEventContext = events::add
        )
        store.locationsShown("locations-1", "s1", 12345L)
        store.cartShown("cart-1", "s1", "100", isPaid = false)

        store.identityReset()
        store.locationsShown("locations-old", "s1", 98765L)

        assertNull(store.currentOrderId)
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
        assertEquals(listOf(12345L), events)
    }
}
