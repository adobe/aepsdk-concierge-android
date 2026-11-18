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
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationsMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FnbOrderStoreTest {
    private val published = mutableListOf<Map<String, Any?>>()
    private val store = FnbOrderStore { published += it }

    private fun actionable(cardId: String, sessionId: String, isLatest: Boolean = true) =
        store.isActionableMenu(store.state.value, cardId, sessionId, isLatest)

    @Test
    fun `only the latest menu is actionable, whatever order the menus display in`() {
        // Compose can display the newer menu first; the SDK's isLatest decides, not display order.
        store.menuShown("menu-2", "s1")
        store.menuShown("menu-1", "s1")

        assertTrue(actionable("menu-2", "s1", isLatest = true))
        assertFalse(actionable("menu-1", "s1", isLatest = false))
        assertTrue("rendering a menu doesn't change order context", published.isEmpty())
    }

    @Test
    fun `a menu that becomes latest again after a rollback is actionable again`() {
        store.menuShown("menu-1", "s1")
        store.menuShown("menu-2", "s1")

        assertFalse(actionable("menu-1", "s1", isLatest = false))
        assertTrue(actionable("menu-1", "s1", isLatest = true))
    }

    @Test
    fun `the first unpaid cart becomes the current order and is published`() {
        store.cartShown("cart-1", "s1", "98765", isPaid = false, isLatest = true)
        store.cartShown("cart-2", "s1", "98765", isPaid = false, isLatest = true)

        assertEquals("98765", store.currentOrderId)
        assertEquals(
            listOf(mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("currentOrder" to mapOf("orderId" to 98765L)))))),
            published
        )
    }

    @Test
    fun `an older cart can't claim the current order, whichever displays first`() {
        store.cartShown("cart-old", "s1", "100", isPaid = false, isLatest = false)
        store.cartShown("cart-new", "s1", "200", isPaid = false, isLatest = true)
        store.cartShown("cart-old", "s1", "100", isPaid = false, isLatest = false)

        assertEquals("200", store.currentOrderId)
        assertEquals(1, published.size)
    }

    @Test
    fun `locations publish event context once per card and never on recomposition`() {
        val events = mutableListOf<Long?>()
        val locationStore = FnbOrderStore(publishContext = {}, publishEventContext = events::add)

        locationStore.locationsShown("locations-1", "s1", 12345L, isLatest = true)
        locationStore.locationsShown("locations-1", "s1", 12345L, isLatest = true)
        locationStore.locationsShown("locations-1", "s1", 99999L, isLatest = true)

        assertEquals(listOf(12345L), events)
    }

    @Test
    fun `location renderer context publishes the event to held XDM before later actions`() {
        val updates = mutableListOf<Map<String, Any?>>()
        val host = FnbHostContext(updateContext = updates::add)
        val locationStore = FnbOrderStore(publishEventContext = host::setCurrentEvent, publishContext = {})
        val element = mapOf("rendererContext" to mapOf("eventId" to 36840.0))

        locationStore.locationsShown("locations-1", "s1", LocationsMapper.elementEventId(element), isLatest = true)

        assertEquals(
            listOf(mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to 36840L))))),
            updates
        )
    }

    @Test
    fun `a latest location without an event clears the previous held event`() {
        val updates = mutableListOf<Map<String, Any?>>()
        val host = FnbHostContext(updateContext = updates::add)
        val locationStore = FnbOrderStore(publishEventContext = host::setCurrentEvent, publishContext = {})

        locationStore.locationsShown("locations-1", "s1", 36840L, isLatest = true)
        locationStore.locationsShown("older", "s1", null, isLatest = false)
        assertEquals(36840L, host.currentEventId)
        locationStore.locationsShown("locations-2", "s1", null, isLatest = true)

        assertNull(host.currentEventId)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to null)))),
            updates.last()
        )

        locationStore.locationsShown("locations-3", "s1", 36841L, isLatest = true)
        locationStore.locationsShown("locations-4", "s1", -1L, isLatest = true)
        assertNull(host.currentEventId)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to null)))),
            updates.last()
        )
    }

    @Test
    fun `an older locations card that displays later doesn't overwrite the event`() {
        val events = mutableListOf<Long?>()
        val locationStore = FnbOrderStore(publishContext = {}, publishEventContext = events::add)

        locationStore.locationsShown("locations-new", "s1", 2L, isLatest = true)
        locationStore.locationsShown("locations-old", "s1", 1L, isLatest = false)
        assertEquals(listOf(2L), events)

        // Rolled back to the older card: it is latest now, so it publishes on its next display.
        locationStore.locationsShown("locations-old", "s1", 1L, isLatest = true)
        assertEquals(listOf(2L, 1L), events)
    }

    @Test
    fun `a previously displayed location restores its event when displayed as latest again`() {
        val events = mutableListOf<Long?>()
        val locationStore = FnbOrderStore(publishContext = {}, publishEventContext = events::add)

        locationStore.locationsShown("locations-old", "s1", 1L, isLatest = true)
        locationStore.locationsShown("locations-new", "s1", 2L, isLatest = true)
        locationStore.locationsShown("locations-old", "s1", 1L, isLatest = true)

        assertEquals(listOf(1L, 2L, 1L), events)
    }

    @Test
    fun `location receipt leaves the following menu active across repeated state reads`() {
        val events = mutableListOf<Long?>()
        val store = FnbOrderStore(publishEventContext = events::add, publishContext = {})
        store.locationsShown("locations-1", "s1", 12345L, isLatest = true)
        store.menuShown("menu-1", "s1")

        repeat(3) {
            store.locationsShown("locations-1", "s1", 12345L, isLatest = true)
            store.menuShown("menu-1", "s1")
            assertTrue(store.isActionableMenu(store.state.value, "menu-1", "s1", isLatest = true))
        }
        assertEquals(listOf(12345L), events)
    }

    @Test
    fun `paid, blank and redisplayed carts don't change the current order`() {
        store.cartShown("cart-1", "s1", "1", isPaid = true, isLatest = true)
        store.cartShown("cart-2", "s1", " ", isPaid = false, isLatest = true)
        assertNull(store.currentOrderId)

        store.cartShown("cart-3", "s1", "2", isPaid = false, isLatest = true)
        store.cartShown("cart-1", "s1", "3", isPaid = false, isLatest = true)
        assertEquals("2", store.currentOrderId)
    }

    @Test
    fun `non-numeric order identifiers are not published as fnbOrders`() {
        store.cartShown("cart-1", "s1", "order-guid", isPaid = false, isLatest = true)

        assertNull(store.currentOrderId)
        assertTrue(published.isEmpty())
    }

    @Test
    fun `an old cart cannot replace the current order or close the newest menu`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "200", isPaid = false, isLatest = true)
        store.menuShown("menu-2", "s1")
        store.cartShown("cart-old", "s1", "100", isPaid = false, isLatest = false)

        assertEquals("200", store.currentOrderId)
        assertTrue(actionable("menu-2", "s1"))

        store.checkedOut("100")

        assertEquals("200", store.currentOrderId)
        assertTrue(actionable("menu-2", "s1"))
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
    }

    @Test
    fun `checkout moves the current order to past orders and closes every displayed menu`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "98765", isPaid = false, isLatest = true)
        published.clear()

        store.checkedOut("98765")

        assertNull(store.currentOrderId)
        assertEquals(listOf("98765"), store.state.value.submittedOrderIds)
        assertFalse(actionable("menu-1", "s1"))
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
        store.cartShown("cart-1", "s1", "100", isPaid = false, isLatest = true)
        store.checkedOut("100")
        store.menuShown("menu-2", "s1")
        store.cartShown("cart-2", "s1", "200", isPaid = false, isLatest = true)
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
        store.cartShown("cart-1", "s1", "100", isPaid = false, isLatest = true)
        store.checkedOut("100")
        store.menuShown("menu-2", "s1")
        val before = published.size

        store.cartShown("cart-2", "s1", "100", isPaid = false, isLatest = true)
        store.checkedOut("100")

        assertNull(store.currentOrderId)
        assertTrue("reopening checkout keeps the newer menu open", actionable("menu-2", "s1"))
        assertEquals(before, published.size)
    }

    @Test
    fun `a card from a new session resets the store`() {
        store.menuShown("menu-1", "s1")
        store.cartShown("cart-1", "s1", "100", isPaid = false, isLatest = true)
        store.checkedOut("100")
        store.cartShown("cart-2", "s1", "200", isPaid = false, isLatest = true)

        store.menuShown("menu-2", "s2")

        assertNull(store.currentOrderId)
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
        assertTrue(actionable("menu-2", "s2"))
        store.menuShown("menu-1", "s1")
        assertTrue("an older session's card can't rebind the store", actionable("menu-2", "s2"))
        assertFalse(actionable("menu-1", "s1"))
    }

    @Test
    fun `reused card ids are scoped to their session`() {
        store.menuShown("menu-1", "s1")

        store.menuShown("menu-1", "s2")

        assertTrue(actionable("menu-1", "s2"))
        assertFalse(actionable("menu-1", "s1"))
    }

    @Test
    fun `identity reset clears local order state and rejects cards from the old session`() {
        val events = mutableListOf<Long?>()
        val store = FnbOrderStore(
            publishContext = { published += it },
            publishEventContext = events::add
        )
        store.locationsShown("locations-1", "s1", 12345L, isLatest = true)
        store.cartShown("cart-1", "s1", "100", isPaid = false, isLatest = true)

        store.identityReset()
        store.locationsShown("locations-old", "s1", 98765L, isLatest = true)

        assertNull(store.currentOrderId)
        assertTrue(store.state.value.submittedOrderIds.isEmpty())
        assertEquals(listOf(12345L), events)
    }

    @Test
    fun `identity reset rejects a previously seen location with no session id`() {
        val events = mutableListOf<Long?>()
        val store = FnbOrderStore(publishEventContext = events::add, publishContext = {})

        store.locationsShown("locations-old", null, 12345L, isLatest = true)
        store.identityReset()
        store.locationsShown("locations-old", null, 12345L, isLatest = true)

        assertEquals(listOf(12345L), events)
    }
}
