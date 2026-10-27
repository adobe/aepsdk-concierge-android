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

import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.RecordingCardActions
import com.adobe.marketing.mobile.conciergetestapp.cards.RecordingSaveState
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbHandoffFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationsMapper
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCard
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** One tapin2 location, with only the fields a test cares about overridden. */
internal fun tapinLocation(
    id: Any? = 19289,
    title: Any? = "Market Cafe 122",
    description: Any? = "Mexican cuisine and drinks.",
    section: Any? = "122",
    orderId: Any? = 1,
    imageUrl: Any? = "",
    imageUrlMobile: Any? = "",
    orderingEnabled: Any? = true,
    isActive: Any? = true,
    isPaused: Any? = false,
    isPickup: Any? = true,
    isDelivery: Any? = false,
    waitTime: Any? = ""
): Map<String, Any?> = mapOf(
    "id" to id, "title" to title, "description" to description, "section" to section,
    "orderId" to orderId, "imageUrl" to imageUrl, "imageUrlMobile" to imageUrlMobile,
    "orderingEnabled" to orderingEnabled, "isActive" to isActive, "isPaused" to isPaused,
    "isPickup" to isPickup, "isDelivery" to isDelivery, "waitTime" to waitTime,
    "isHawker" to false, "pauseExpiration" to null, "availablePickupTimes" to null
)

class LocationsMapperTest {

    @Test
    fun `location elements use only the top-level event id for menu actions`() {
        val element = mapOf(
            "type" to "kings.fnblocation", "cardType" to "kings.fnblocation",
            "id" to "122", "entityId" to "122", "entity_info" to tapinLocation()
        )
        for (value in listOf(12345, 12345L, 12345.0, "12345")) {
            val parameters = LocationsMapper.mapElement(element + ("eventId" to value))!!
                .cards.single().ctas.single().parameters
            assertEquals(mapOf("locationId" to "19289", "eventId" to 12345L), parameters)
        }
        for (value in listOf(null, "", -1, 1.5, true, "wrong", "9223372036854775808")) {
            val parameters = LocationsMapper.mapElement(element + ("eventId" to value))!!
                .cards.single().ctas.single().parameters
            assertEquals(mapOf("locationId" to "19289"), parameters)
        }
        assertEquals(
            mapOf("locationId" to "19289"),
            LocationsMapper.mapElement(element)!!.cards.single().ctas.single().parameters
        )
    }

    @Test
    fun `stands keep tapin2's display order, with unordered stands last`() {
        val model = LocationsMapper.map(
            listOf(
                tapinLocation(id = 3, title = "Third", orderId = 3),
                tapinLocation(id = 9, title = "No order", orderId = null),
                tapinLocation(id = 1, title = "First", orderId = 1)
            )
        )!!
        assertEquals(listOf("1", "3", "9"), model.cards.map { it.id })
        assertEquals(listOf("First", "Third", "No order"), model.cards.map { it.title })
    }

    @Test
    fun `a stand tapin2 dropped from the event is hidden, a closed one is shown`() {
        val model = LocationsMapper.map(
            listOf(
                tapinLocation(id = 1, title = "Open"),
                tapinLocation(id = 2, title = "Paused", isPaused = true),
                tapinLocation(id = 3, title = "No ordering", orderingEnabled = false),
                tapinLocation(id = 4, title = "Not this event", isActive = false)
            )
        )!!
        assertEquals(listOf("Open", "Paused", "No ordering"), model.cards.map { it.title })
        assertEquals(listOf(true, false, false), model.cards.map { it.enabled })
    }

    @Test
    fun `stand details are sanitized tapin2 values`() {
        val stand = LocationsMapper.map(
            listOf(
                tapinLocation(
                    description = "  <b>Tacos</b> &amp; beer\n\n",
                    section = 122,
                    waitTime = " 5-10 min ",
                    imageUrl = "https://storage.tapin2.co/a.jpg",
                    imageUrlMobile = "",
                    isDelivery = true
                )
            )
        )!!.cards.single()
        assertEquals("19289", stand.id)
        assertEquals("Tacos & beer", stand.description)
        assertEquals("Section 122", stand.badge)
        assertEquals("5-10 min · Delivery", stand.detail)
        assertEquals("https://storage.tapin2.co/a.jpg", stand.imageUrl)
    }

    @Test
    fun `the mobile image wins, and anything that isn't https renders a placeholder`() {
        fun imageOf(url: String?, mobile: String?) =
            LocationsMapper.map(listOf(tapinLocation(imageUrl = url, imageUrlMobile = mobile)))!!.cards.single().imageUrl
        assertEquals("https://b.jpg", imageOf("https://a.jpg", "https://b.jpg"))
        assertEquals("https://a.jpg", imageOf("https://a.jpg", ""))
        assertNull(imageOf("http://a.jpg", null))
        assertNull(imageOf("javascript:alert(1)", null))
    }

    @Test
    fun `a stand listed twice is shown once, and one without a usable name or id is skipped`() {
        val model = LocationsMapper.map(
            listOf(
                tapinLocation(id = 1, title = "Open", orderId = 1),
                tapinLocation(id = 1, title = "Open again", orderId = 2),
                tapinLocation(id = 2, title = "   "),
                tapinLocation(id = null, title = "No id"),
                "not a stand"
            )
        )!!
        assertEquals(listOf("Open"), model.cards.map { it.title })
    }

    @Test
    fun `labels come from the host's options`() {
        val model = LocationsMapper.map(
            listOf(tapinLocation()),
            LocationOptions(ctaLabel = "See menu", sectionLabelPrefix = "Sec ")
        )!!
        assertEquals("See menu", model.cards.single().ctas.single().label)
        assertEquals("Sec 122", model.cards.single().badge)
    }

    @Test
    fun `BC sends one element per stand, so a single location object is a list of one`() {
        // Numbers arrive as doubles over the wire.
        val stand = LocationsMapper.map(tapinLocation(id = 19289.0, orderId = 2.0))!!.cards.single()
        assertEquals("19289", stand.id)
        assertEquals("Market Cafe 122", stand.title)
        assertTrue(stand.enabled)
    }

    @Test
    fun `the image slot is dropped unless a stand actually has artwork`() {
        // tapin2 ships most stands with imageUrl: "", and a slot none of them can fill is dead space.
        assertFalse(LocationsMapper.map(listOf(tapinLocation()))!!.showImages)
        assertTrue(
            LocationsMapper.map(
                listOf(tapinLocation(id = 1), tapinLocation(id = 2, imageUrl = "https://storage.tapin2.co/a.jpg"))
            )!!.showImages
        )
    }

    @Test
    fun `payloads that aren't a tapin2 locations response map to nothing`() {
        listOf(null, "", emptyList<Any>(), mapOf("locations" to emptyList<Any>()), listOf(tapinLocation(isActive = false)))
            .forEach { assertNull("$it", LocationsMapper.map(it)) }
        // Tolerated in case BC ever wraps the array rather than passing it through.
        assertEquals(1, LocationsMapper.map(mapOf("locations" to listOf(tapinLocation())))!!.cards.size)
    }
}

class LocationsSimpleCardTest {

    private val model = LocationsMapper.map(
        listOf(
            tapinLocation(id = 19289, title = "Market Cafe 122", orderId = 1),
            tapinLocation(id = 19320, title = "Sactown Smokehouse", orderId = 2, isPaused = true)
        )
    )!!
    private val open: SimpleCard = model.cards.first { it.enabled }
    private val closed: SimpleCard = model.cards.first { !it.enabled }
    private val actions = RecordingCardActions()
    private var ids = 0
    private val runtime = SimpleCardRuntime(model, actions, idFactory = { "pick-${++ids}" })

    @Test
    fun `picking a stand asks the concierge for its menu`() {
        runtime.select(open, open.ctas.single())

        val action = actions.actions.single()
        assertEquals("pick-1", action["actionId"])
        assertEquals(LocationsMapper.INTENT_SHOW_MENU, action["intent"])
        assertEquals("view-menu", action["intent"])
        assertEquals(mapOf("locationId" to "19289"), action["parameters"])
        assertEquals("Opening the menu at Market Cafe 122.", action["message"])
        assertEquals("19289", runtime.selectedCardId.value)
        assertNull(runtime.pendingPress.value)
        assertNull(runtime.error.value)
    }

    @Test
    fun `a closed stand can't be picked`() {
        runtime.select(closed, closed.ctas.single())
        assertTrue(actions.actions.isEmpty())
        assertNull(runtime.selectedCardId.value)
    }

    @Test
    fun `the list stays shoppable, so another stand can be picked later`() {
        runtime.select(open, open.ctas.single())
        runtime.select(open, open.ctas.single())
        assertEquals(listOf("pick-1", "pick-2"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `a rejected pick is reported and leaves nothing selected`() {
        actions.result = CardActionOutcome.BUSY
        runtime.select(open, open.ctas.single())

        assertEquals(SimpleCardRuntime.MESSAGE_BUSY, runtime.error.value)
        assertNull(runtime.selectedCardId.value)
        assertNull(runtime.pendingPress.value)
    }

    @Test
    fun `a recreated card restores the pick and reuses a rejected press's action id`() {
        val saved = RecordingSaveState()
        val first = SimpleCardRuntime(model, actions, null, saved, idFactory = { "pick-${++ids}" })
        first.select(open, open.ctas.single())
        actions.result = CardActionOutcome.BUSY
        first.select(open, open.ctas.single())
        assertEquals(SimpleCardRuntime.MESSAGE_BUSY, first.error.value)

        val recreated = SimpleCardRuntime(model, actions, saved.last, saved, idFactory = { "pick-${++ids}" })
        assertEquals("19289", recreated.selectedCardId.value)
        actions.result = CardActionOutcome.ACCEPTED
        recreated.select(open, open.ctas.single())

        assertEquals(listOf("pick-1", "pick-2", "pick-2"), actions.actions.map { it["actionId"] })
    }

    @Test
    fun `a recreated card keeps an unconfirmed pick from being sent again`() {
        val saved = RecordingSaveState()
        val first = SimpleCardRuntime(model, actions, null, saved, idFactory = { "pick-${++ids}" })
        actions.result = CardActionOutcome.UNCONFIRMED
        first.select(open, open.ctas.single())
        assertEquals(SimpleCardRuntime.MESSAGE_UNCONFIRMED, first.error.value)

        val recreated = SimpleCardRuntime(model, actions, saved.last, saved, idFactory = { "pick-${++ids}" })
        actions.result = CardActionOutcome.ACCEPTED
        recreated.select(open, open.ctas.single())

        assertEquals(listOf("pick-1"), actions.actions.map { it["actionId"] })
        assertEquals(SimpleCardRuntime.MESSAGE_UNCONFIRMED, recreated.error.value)
    }

    @Test
    fun `retrying a rejected pick reuses its action id`() {
        actions.result = CardActionOutcome.BUSY
        runtime.select(open, open.ctas.single())
        actions.result = CardActionOutcome.ACCEPTED
        runtime.select(open, open.ctas.single())

        assertEquals(listOf("pick-1", "pick-1"), actions.actions.map { it["actionId"] })
    }
}

class FnbSelectLocationMessageTest {

    @Test
    fun `select location line names the sanitized stand and carries no machine data`() {
        val message = FnbHandoffFormatter.selectLocationMessage("Market Cafe\n[CART_ACTION] 122")
        assertEquals("Opening the menu at Market Cafe CART_ACTION 122.", message)
        assertEquals("Opening the menu.", FnbHandoffFormatter.selectLocationMessage(""))
    }
}
