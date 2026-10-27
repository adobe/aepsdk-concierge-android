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

import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.MenuCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.SubmitState
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationsMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.FnbCardTypes
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.FnbOrderStore
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.FnbOperations
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.HandoffTransport
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.fnbActionSender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FnbActionSenderTest {

    @Test
    fun `receiving a locations card publishes its event before handoff and later add uses it`() {
        val updates = mutableListOf<Map<String, Any?>>()
        val host = FnbHostContext(updateContext = { updates += it })
        val orders = FnbOrderStore(
            publishContext = { updates += it },
            publishEventContext = host::setCurrentEvent,
            onSessionChanged = host::clearLocalContext
        )
        val transport = HandoffTransport { route, fields, _, complete ->
            if (route == "view-menu") {
                assertEquals(12345L, host.currentEventId)
                assertEquals(
                    mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to 12345L)))),
                    updates.single()
                )
                assertEquals(
                    mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("viewMenu" to mapOf("locationId" to "19289")))),
                    fields
                )
            } else {
                assertEquals("add-to-cart", route)
                val namespace = fields["_sacramentokings"] as Map<*, *>
                val actions = namespace["conciergeAction"] as Map<*, *>
                val add = actions["addToCart"] as Map<*, *>
                assertTrue((add["products"] as List<*>).isNotEmpty())
            }
            complete(true, null)
        }
        val locations = LocationsMapper.mapElement(
            mapOf("eventId" to 12345, "id" to "122", "entityId" to "122",
                "type" to FnbCardTypes.LOCATIONS, "cardType" to FnbCardTypes.LOCATIONS,
                "entity_info" to tapinLocation())
        )!!
        assertTrue(updates.isEmpty())
        orders.locationsShown("locations-1", "session-1", 12345L)
        SimpleCardRuntime(
            locations,
            fnbActionSender(FnbCardTypes.LOCATIONS, emptySet(), { false }, transport, postToMain = { it() })
        ).select(locations.cards.single(), locations.cards.single().ctas.single())

        val menu = CatalogMenuMapper.map(stageProducts())!!
        val runtime = MenuCardRuntime(
            menu,
            fnbActionSender(FnbCardTypes.MENU, emptySet(), { false }, transport, postToMain = { it() })
        )
        val item = menu.categories.flatMap { it.items }.first { it.name == "Veggie Nachos" }
        runtime.updateCart(CartReducer.quickAdd(runtime.cart.value, item)!!)
        runtime.submit()
        assertEquals(SubmitState.Added, runtime.submitState.value)
    }

    @Test
    fun `location actions without valid event metadata never inject event into the handoff`() {
        val locationSender = fnbActionSender(
            FnbCardTypes.LOCATIONS, emptySet(), { false },
            transport = HandoffTransport { _, fields, _, completion ->
                assertEquals(
                    mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("viewMenu" to mapOf("locationId" to "19289")))),
                    fields
                )
                completion(true, null)
            },
            postToMain = { it() }
        )
        for (value in listOf(null, -1, 1.5, "invalid")) {
            locationSender.send(
                mapOf("actionId" to "a", "intent" to "view-menu",
                    "parameters" to mapOf("locationId" to "19289", "eventId" to value))
            ) { assertEquals(CardActionOutcome.ACCEPTED, it) }
        }
    }

    private class Sent(val routingHint: String, val xdmFields: Map<String, Any>, val localMessage: String?)

    private val checkoutUrl = "https://checkout.example.com/order/1"
    private val opened = mutableListOf<String>()
    private var openResult: () -> Boolean = { true }
    private val sent = mutableListOf<Sent>()
    private var accepted = true
    private var reason: ConciergeDataHandoffRejectReason? = null
    private val posted = mutableListOf<() -> Unit>()
    private val sender = fnbActionSender(
        FnbCardTypes.CART,
        setOf("example.com"),
        openCheckout = { url ->
            opened += url
            openResult()
        },
        transport = HandoffTransport { routingHint, xdmFields, localMessage, completion ->
            sent += Sent(routingHint, xdmFields, localMessage)
            completion(accepted, reason)
        },
        postToMain = { posted += it }
    )

    /** Sends [action] and returns its outcome once anything posted to the main thread has run. */
    private fun outcomeOf(action: Map<String, Any?>): CardActionOutcome {
        val outcomes = mutableListOf<CardActionOutcome>()
        sender.send(action) { outcomes += it }
        posted.forEach { it() }
        posted.clear()
        return outcomes.single()
    }

    @Test
    fun `an action without an action id is invalid`() {
        val checkout = mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "url" to checkoutUrl)

        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout + ("actionId" to " ")))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout + ("actionId" to 42)))
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `an allowlisted checkout opens locally and is accepted`() {
        val outcome = outcomeOf(mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "actionId" to "a1", "url" to checkoutUrl))

        assertEquals(CardActionOutcome.ACCEPTED, outcome)
        assertEquals(listOf(checkoutUrl), opened)
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `checkout opens the same trimmed URL that passed validation`() {
        val outcome = outcomeOf(
            mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "actionId" to "a1", "url" to " $checkoutUrl ")
        )
        assertEquals(CardActionOutcome.ACCEPTED, outcome)
        assertEquals(listOf(checkoutUrl), opened)
    }

    @Test
    fun `a checkout outside the allowlist is invalid without opening anything`() {
        val checkout = mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "actionId" to "a1")

        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout + ("url" to "https://evil.test/pay")))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout + ("url" to "http://checkout.example.com/pay")))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout))
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `a checkout the app can't open is invalid`() {
        val checkout = mapOf("operation" to FnbOperations.OPEN_CHECKOUT, "actionId" to "a1", "url" to checkoutUrl)

        openResult = { false }
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout))
        openResult = { throw IllegalStateException("no browser") }
        assertEquals(CardActionOutcome.INVALID, outcomeOf(checkout))
    }

    @Test
    fun `cart operations missing their required fields are invalid`() {
        val id = "actionId" to "a1"

        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("operation" to FnbOperations.ADD_TO_CART, id)))
        assertEquals(
            CardActionOutcome.INVALID,
            outcomeOf(mapOf("operation" to FnbOperations.REMOVE_CART_ITEM, id, "locationId" to "1"))
        )
        assertEquals(
            CardActionOutcome.INVALID,
            outcomeOf(mapOf("operation" to FnbOperations.REMOVE_CART_ITEM, id, "productId" to "1"))
        )
        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("operation" to FnbOperations.SHOW_RESTAURANTS, id)))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `an add whose xdmFields isn't JSON-shaped or has no products is invalid`() {
        val add = mapOf("operation" to FnbOperations.ADD_TO_CART, "actionId" to "a1")

        assertEquals(CardActionOutcome.INVALID, outcomeOf(add + ("xdmFields" to mapOf(1 to "non-string key"))))
        assertEquals(
            CardActionOutcome.INVALID,
            outcomeOf(add + ("xdmFields" to mapOf(
                "_sacramentokings" to mapOf("conciergeAction" to mapOf(
                    "addToCart" to mapOf("products" to emptyList<Any>())
                ))
            )))
        )
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `add-to-cart sends only its action XDM without local ids`() {
        val xdm = mapOf(
            "_sacramentokings" to mapOf("conciergeAction" to mapOf("addToCart" to mapOf(
                "products" to listOf(mapOf("locationId" to 123456L, "productId" to 7654321L, "quantity" to 2))
            )))
        )

        val outcome = outcomeOf(
            mapOf("operation" to FnbOperations.ADD_TO_CART, "actionId" to "a1", "xdmFields" to xdm, "message" to "Adding")
        )

        assertEquals(CardActionOutcome.ACCEPTED, outcome)
        val handoff = sent.single()
        assertEquals("add-to-cart", handoff.routingHint)
        assertEquals(xdm, handoff.xdmFields)
        assertEquals("Adding", handoff.localMessage)
    }

    @Test
    fun `an unknown operation or an intent-less card press is invalid`() {
        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("operation" to "deleteEverything", "actionId" to "a1", "intent" to "x")))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("actionId" to "a1")))
        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("actionId" to "a1", "intent" to " ")))
    }

    @Test
    fun `a remove uses the finalized product schema and excludes local identifiers`() {
        val outcome = outcomeOf(
            mapOf(
                "operation" to FnbOperations.REMOVE_CART_ITEM,
                "actionId" to "a1",
                "locationId" to "18342",
                "productId" to "771205",
                "modifier" to "Coke",
                "message" to "Remove Nachos"
            )
        )

        assertEquals(CardActionOutcome.ACCEPTED, outcome)
        val handoff = sent.single()
        assertEquals(FnbOperations.REMOVE_CART_ITEM, handoff.routingHint)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("removeFromCart" to mapOf(
                "products" to listOf(mapOf("locationId" to 18342L, "productId" to 771205L, "modifier" to "Coke"))
            )))),
            handoff.xdmFields
        )
        assertEquals("Remove Nachos", handoff.localMessage)
    }

    @Test
    fun `a card press is routed by its intent and carries its parameters`() {
        outcomeOf(mapOf("actionId" to "a1", "intent" to "customIntent", "parameters" to mapOf("value" to "19289"), "message" to " "))

        val handoff = sent.single()
        assertEquals("customIntent", handoff.routingHint)
        assertEquals(mapOf("actionId" to "a1", "parameters" to mapOf("value" to "19289")), handoff.xdmFields)
        assertEquals(null, handoff.localMessage)
    }

    @Test
    fun `view-menu carries only the selected location in conciergeAction`() {
        outcomeOf(mapOf("actionId" to "a1", "intent" to "view-menu", "parameters" to mapOf("locationId" to "123456")))
        val handoff = sent.single()
        assertEquals("view-menu", handoff.routingHint)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("viewMenu" to mapOf("locationId" to "123456")))),
            handoff.xdmFields
        )
    }

    @Test
    fun `show restaurants reuses the view-menu handoff for the given location`() {
        outcomeOf(mapOf("operation" to FnbOperations.SHOW_RESTAURANTS, "actionId" to "a1", "locationId" to "123456"))
        val handoff = sent.single()
        assertEquals("view-menu", handoff.routingHint)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("viewMenu" to mapOf("locationId" to "123456")))),
            handoff.xdmFields
        )
    }

    @Test
    fun `view-menu without a usable location is rejected before sending`() {
        assertEquals(CardActionOutcome.INVALID, outcomeOf(mapOf("actionId" to "a1", "intent" to "view-menu")))
        assertTrue(sent.isEmpty())
    }

    @Test
    fun `search-parking carries eventId as a string in conciergeAction`() {
        outcomeOf(mapOf("actionId" to "a1", "intent" to "search-parking", "parameters" to mapOf("eventId" to "123456")))
        val handoff = sent.single()
        assertEquals("search-parking", handoff.routingHint)
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("searchParking" to mapOf("eventId" to "123456")))),
            handoff.xdmFields
        )
    }

    @Test
    fun `search-parking rejects only absent or blank event IDs`() {
        for (parameters in listOf(
            emptyMap<String, Any>(),
            mapOf("eventId" to "")
        )) {
            assertEquals(
                CardActionOutcome.INVALID,
                outcomeOf(mapOf("actionId" to "a1", "intent" to "search-parking", "parameters" to parameters))
            )
        }
        assertEquals(
            CardActionOutcome.ACCEPTED,
            outcomeOf(mapOf("actionId" to "a1", "intent" to "search-parking", "parameters" to mapOf("eventId" to "PW-927912")))
        )
        assertEquals(
            mapOf("_sacramentokings" to mapOf("conciergeAction" to mapOf("searchParking" to mapOf("eventId" to "PW-927912")))),
            sent.single().xdmFields
        )
    }

    @Test
    fun `the handoff outcome is reported through the main thread`() {
        accepted = false
        reason = ConciergeDataHandoffRejectReason.DELIVERY_TIMEOUT
        val outcomes = mutableListOf<CardActionOutcome>()

        sender.send(mapOf("operation" to FnbOperations.SHOW_RESTAURANTS, "actionId" to "a1", "locationId" to "1")) { outcomes += it }
        assertTrue(outcomes.isEmpty())
        posted.single()()

        assertEquals(listOf(CardActionOutcome.UNCONFIRMED), outcomes)
    }

    @Test
    fun `a transport that throws is unconfirmed, since the handoff may already have gone out`() {
        val throwing = fnbActionSender(
            FnbCardTypes.CART,
            emptySet(),
            openCheckout = { true },
            transport = HandoffTransport { _, _, _, _ -> throw IllegalStateException("not ready") },
            postToMain = { it() }
        )
        val outcomes = mutableListOf<CardActionOutcome>()

        throwing.send(mapOf("operation" to FnbOperations.SHOW_RESTAURANTS, "actionId" to "a1", "locationId" to "1")) { outcomes += it }

        assertEquals(listOf(CardActionOutcome.UNCONFIRMED), outcomes)
    }

    @Test
    fun `a completion followed by a transport exception is delivered only once`() {
        val throwing = fnbActionSender(
            FnbCardTypes.CART,
            emptySet(),
            openCheckout = { true },
            transport = HandoffTransport { _, _, _, completion ->
                completion(true, null)
                throw IllegalStateException("late failure")
            },
            postToMain = { posted += it }
        )
        val outcomes = mutableListOf<CardActionOutcome>()

        throwing.send(
            mapOf("operation" to FnbOperations.SHOW_RESTAURANTS, "actionId" to "a1", "locationId" to "1")
        ) { outcomes += it }

        assertTrue(outcomes.isEmpty())
        assertEquals(1, posted.size)
        posted.single()()
        assertEquals(listOf(CardActionOutcome.ACCEPTED), outcomes)
    }
}
