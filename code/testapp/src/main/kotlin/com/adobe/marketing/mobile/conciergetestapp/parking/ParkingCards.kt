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

import androidx.compose.runtime.Immutable
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCard
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardCta
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardList
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadText
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.id
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.name
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.objectSequence

/**
 * Element `type`s Brand Concierge stamps on parking elements. Each element is
 * `{"type", "id", "entityId", "entity_info": <vendor response body, passed through untouched>}`.
 */
object ParkingCardTypes {
    /** `entity_info`: an event the fan can park for — `{ event_id, name, start_time, end_time }`. */
    const val EVENTS = "kings.parkingevents"
}

/** Host-tunable copy for the parking cards. */
@Immutable
data class ParkingEventOptions(
    val ctaLabel: String = "See parking",
    val pendingCtaLabel: String = "Finding lots\u2026"
)

/**
 * Maps a `kings.parkingevents` element's payload into a [SimpleCardList]: the upcoming games the
 * fan can buy parking for. The payload is the vendor's raw body — BC emits one element per event,
 * so it is usually a single object, but an array of them is read the same way:
 *
 * `[{ event_id, name, start_time, end_time }]`
 *
 * These events carry no artwork, so the cards are drawn without an image slot; the tip-off time
 * takes the detail line. Picking one asks the concierge for that event's lots.
 */
object ParkingEventsMapper {

    object Keys {
        const val EVENT_ID = "event_id"
        const val NAME = "name"
        const val START_TIME = "start_time"
    }

    /** Intent BC resolves to the parking-lot lookup for one event. */
    const val INTENT_SHOW_PARKING = "search-parking"

    const val MAX_EVENTS = 20
    private const val MAX_RAW_EVENTS = 500

    /** Returns null when the payload holds no readable event. */
    fun map(payload: Any?, options: ParkingEventOptions = ParkingEventOptions()): SimpleCardList? {
        val seen = HashSet<String>()
        val cards = mutableListOf<SimpleCard>()
        for (entry in objectSequence(rootArray(payload), MAX_RAW_EVENTS)) {
            if (cards.size >= MAX_EVENTS) break
            val eventId = id(entry[Keys.EVENT_ID]) ?: continue
            val title = name(entry[Keys.NAME])
            if (title.isEmpty()) continue
            if (!seen.add(eventId)) continue

            cards += SimpleCard(
                id = eventId,
                title = title,
                detail = EventTime.format(entry[Keys.START_TIME]),
                ctas = listOf(
                    SimpleCardCta(
                        label = options.ctaLabel,
                        intentName = INTENT_SHOW_PARKING,
                        parameters = mapOf("eventId" to eventId),
                        localMessage = ParkingHandoffFormatter.selectEventMessage(title),
                        pendingLabel = options.pendingCtaLabel
                    )
                )
            )
        }
        if (cards.isEmpty()) return null
        // The vendor returns events in date order; keep it rather than re-sorting parsed dates.
        return SimpleCardList(cards = cards, showImages = false)
    }

    private fun rootArray(payload: Any?): Any? = when (payload) {
        is List<*> -> payload
        // BC emits one element per event, so the payload is usually a single event object.
        is Map<*, *> -> (payload["events"] as? List<*>)
            ?: payload.takeIf { it[Keys.EVENT_ID] != null }?.let { listOf(it) }
        else -> null
    }
}

/** The transcript line a parking card shows when an event is picked; display-only, never sent. */
object ParkingHandoffFormatter {
    private const val MAX_TITLE_LENGTH = 60

    fun selectEventMessage(title: String): String {
        val safeTitle = PayloadText.sanitizeInline(title.filterNot { it == '[' || it == ']' }, MAX_TITLE_LENGTH)
        return if (safeTitle.isEmpty()) "Finding parking for this game." else "Finding parking for $safeTitle."
    }
}
