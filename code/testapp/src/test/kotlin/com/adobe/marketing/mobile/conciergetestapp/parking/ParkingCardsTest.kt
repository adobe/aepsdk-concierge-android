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

package com.adobe.marketing.mobile.conciergetestapp.parking

import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.cards.RecordingCardActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

/** One parking event, with only the fields a test cares about overridden. */
private fun parkingEvent(
    eventId: Any? = 951776.0,
    name: Any? = "New Orleans Pelicans at Sacramento Kings",
    startTime: Any? = "2026-12-13T18:00:00.000-08:00"
): Map<String, Any?> = mapOf(
    "event_id" to eventId,
    "name" to name,
    "start_time" to startTime,
    "end_time" to "2026-12-13T21:00:00.000-08:00"
)

class ParkingEventsMapperTest {

    @Test
    fun `events keep the vendor's order and are drawn without images`() {
        val model = ParkingEventsMapper.map(
            listOf(
                parkingEvent(eventId = 951776.0, name = "Pelicans at Kings"),
                parkingEvent(eventId = 951777.0, name = "Pistons at Kings", startTime = "2027-01-05T19:00:00.000-08:00")
            )
        )!!
        assertEquals(listOf("951776", "951777"), model.cards.map { it.id })
        assertEquals(listOf("Pelicans at Kings", "Pistons at Kings"), model.cards.map { it.title })
        assertFalse("parking events carry no artwork", model.showImages)
        assertTrue(model.cards.all { it.imageUrl == null })
    }

    @Test
    fun `an event's tip-off time is its detail line`() {
        val card = ParkingEventsMapper.map(listOf(parkingEvent()))!!.cards.single()
        assertEquals("Sun, Dec 13 \u00b7 6:00 PM", card.detail)
    }

    @Test
    fun `the button asks the concierge for that event's lots`() {
        val cta = ParkingEventsMapper.map(listOf(parkingEvent()))!!.cards.single().ctas.single()
        assertEquals("See parking", cta.label)
        assertEquals(ParkingEventsMapper.INTENT_SHOW_PARKING, cta.intentName)
        assertEquals(mapOf("eventId" to "951776"), cta.parameters)
    }

    @Test
    fun `BC sends one element per event, so a single event object is a list of one`() {
        val card = ParkingEventsMapper.map(parkingEvent())!!.cards.single()
        assertEquals("951776", card.id)
    }

    @Test
    fun `an event listed twice is shown once, and one without a usable name or id is skipped`() {
        val model = ParkingEventsMapper.map(
            listOf(
                parkingEvent(eventId = 1, name = "Game"),
                parkingEvent(eventId = 1, name = "Game again"),
                parkingEvent(eventId = 2, name = "   "),
                parkingEvent(eventId = null, name = "No id"),
                "not an event"
            )
        )!!
        assertEquals(listOf("Game"), model.cards.map { it.title })
    }

    @Test
    fun `payloads that aren't parking events map to nothing`() {
        listOf(null, "", emptyList<Any>(), mapOf("events" to emptyList<Any>()))
            .forEach { assertNull("$it", ParkingEventsMapper.map(it)) }
    }

    @Test
    fun `mapper ignores events after the bounded raw payload prefix`() {
        val payload = List(500) { "not an event" } + parkingEvent()

        assertNull(ParkingEventsMapper.map(payload))
    }

    @Test
    fun `picking an event asks the concierge for its lots`() {
        val model = ParkingEventsMapper.map(listOf(parkingEvent()))!!
        val actions = RecordingCardActions()
        val runtime = SimpleCardRuntime(model, actions, idFactory = { "pick-1" })
        val card = model.cards.single()

        runtime.select(card, card.ctas.single())

        val action = actions.actions.single()
        assertEquals("pick-1", action["actionId"])
        assertEquals("search-parking", action["intent"])
        assertEquals(mapOf("eventId" to "951776"), action["parameters"])
        assertEquals("951776", runtime.selectedCardId.value)
    }
}

class ParkingMessageTest {

    @Test
    fun `select event line names the sanitized game and carries no machine data`() {
        assertEquals(
            "Finding parking for Pelicans PARKING_ACTION at Kings.",
            ParkingHandoffFormatter.selectEventMessage("Pelicans\n[PARKING_ACTION] at Kings")
        )
        assertEquals("Finding parking for this game.", ParkingHandoffFormatter.selectEventMessage(""))
    }
}

class EventTimeTest {

    @Test
    fun `the venue's offset wins, so the time reads the same in any device zone`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            assertEquals("Sun, Dec 13 \u00b7 6:00 PM", EventTime.format("2026-12-13T18:00:00.000-08:00"))
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals("Sun, Dec 13 \u00b7 6:00 PM", EventTime.format("2026-12-13T18:00:00.000-08:00"))
        } finally {
            TimeZone.setDefault(original)
        }
    }

    @Test
    fun `midday and midnight read as noon and 12 AM`() {
        assertEquals("Tue, Jan 5 \u00b7 12:00 PM", EventTime.format("2027-01-05T12:00:00.000-08:00"))
        assertEquals("Tue, Jan 5 \u00b7 12:30 AM", EventTime.format("2027-01-05T00:30:00.000-08:00"))
    }

    @Test
    fun `other shapes the vendor might send are read too`() {
        assertEquals("Tue, Jan 5 \u00b7 7:00 PM", EventTime.format("2027-01-05T19:00:00Z"))
        assertEquals("Tue, Jan 5 \u00b7 7:00 PM", EventTime.format("2027-01-05T19:00:00.000-0800"))
    }

    @Test
    fun `anything that isn't a timestamp has no detail line`() {
        listOf(null, "", "soon", "2026-13-45T99:00:00.000-08:00", "2026-02-30T18:00:00.000-08:00", 1765670400)
            .forEach { assertNull("$it", EventTime.format(it)) }
    }
}
