/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.conciergetestapp.parking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParkWhizBookingTest {
    @Test
    fun `booking is recorded before the exact status handoff is sent`() {
        for (value in listOf("118734521", "\"118734521\"")) {
            val calls = mutableListOf<String>()
            val result = handleParkWhizBooking(
                """{"status":"success","bookingId":$value}""",
                record = { id ->
                    calls.add("record:$id")
                    true
                },
                sendHandoff = { hint, fields ->
                    calls.add(hint)
                    assertEquals(
                        mapOf(
                            "_sacramentokings" to mapOf(
                                "conciergeAction" to mapOf(
                                    "parkingHandoff" to mapOf("bookingId" to "118734521")
                                )
                            )
                        ),
                        fields
                    )
                }
            )
            assertEquals("118734521", result)
            assertEquals(listOf("record:118734521", "parking-booking-status"), calls)
        }
    }

    @Test
    fun `duplicate callback does not send another status handoff`() {
        val recorded = mutableSetOf<String>()
        var sends = 0
        val record: (String) -> Boolean = recorded::add
        val send: (String, Map<String, Any>) -> Unit = { _, _ -> sends++ }
        val payload = """{"status":"success","bookingId":"118734521"}"""

        assertEquals("118734521", handleParkWhizBooking(payload, record, send))
        assertNull(handleParkWhizBooking(payload, record, send))
        assertEquals(1, sends)
    }

    @Test
    fun `missing or malformed booking does not record or send a handoff`() {
        for (payload in listOf("invalid", "{}", """{"bookingId":null}""", """{"bookingId":""}""")) {
            assertNull(
                handleParkWhizBooking(
                    payload,
                    record = { error("Unexpected booking") },
                    sendHandoff = { _, _ -> error("Unexpected handoff") }
                )
            )
        }
    }
}
