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

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Formats a vendor timestamp such as `2026-12-13T18:00:00.000-08:00` for display.
 *
 * The offset in the string is the venue's, so it is kept and used for display: a fan in another
 * time zone still sees the local tip-off time printed on their ticket. A timestamp without an
 * offset is read in the device's zone.
 *
 * Hand-rolled rather than `java.time` (minSdk 21, no desugaring) or `SimpleDateFormat` (its `X`
 * offset pattern needs API 24).
 */
internal object EventTime {

    private val DATE_TIME = Regex("""^(\d{4})-(\d{2})-(\d{2})[Tt ](\d{2}):(\d{2})(?::\d{2})?(?:\.\d+)?""")
    private val OFFSET = Regex("""[Zz]|[+-]\d{2}:?\d{2}""")
    private val MONTHS = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    private val DAYS = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    /** "Sat, Dec 13 · 6:00 PM", or null when [raw] isn't a timestamp this can read. */
    fun format(raw: Any?): String? {
        val text = (raw as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = DATE_TIME.find(text) ?: return null
        val offset = text.substring(match.range.last + 1).trim()
        if (offset.isNotEmpty() && !OFFSET.matches(offset)) return null
        val (year, month, day, hour, minute) = match.destructured.toList().map { it.toInt() }
        if (month !in 1..12 || day !in 1..31 || hour !in 0..23 || minute !in 0..59) return null

        val zone = zoneOf(offset)
        val calendar = Calendar.getInstance(zone, Locale.US).apply {
            isLenient = false
            clear()
            set(year, month - 1, day, hour, minute)
        }
        val weekday = runCatching { DAYS[calendar.get(Calendar.DAY_OF_WEEK) - 1] }.getOrNull() ?: return null

        val hour12 = when (hour % 12) { 0 -> 12; else -> hour % 12 }
        val meridiem = if (hour < 12) "AM" else "PM"
        return "$weekday, ${MONTHS[month - 1]} $day \u00b7 $hour12:${"%02d".format(Locale.US, minute)} $meridiem"
    }

    /** `Z`, `-08:00` or `-0800` as a fixed-offset zone; blank means the device's own zone. */
    private fun zoneOf(offset: String): TimeZone = when {
        offset.isEmpty() -> TimeZone.getDefault()
        offset.equals("Z", ignoreCase = true) -> TimeZone.getTimeZone("GMT")
        else -> TimeZone.getTimeZone("GMT" + offset.replace(":", ""))
    }
}
