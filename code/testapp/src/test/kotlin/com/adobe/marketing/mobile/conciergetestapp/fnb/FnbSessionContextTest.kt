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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FnbSessionContextTest {
    private val updates = mutableListOf<Map<String, Any?>>()
    private val host = FnbHostContext(updateContext = { updates += it })

    @Test
    fun `setting the event updates conciergeContext fnbOrders and null removes only its eventId`() {
        host.setCurrentEvent(1234567L)
        host.setCurrentEvent(1234567L)
        assertEquals(1234567L, host.currentEventId)

        host.setCurrentEvent(null)
        assertNull(host.currentEventId)

        assertEquals(
            listOf(
                mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to 1234567L)))),
                mapOf("_sacramentokings" to mapOf("conciergeContext" to mapOf("fnbOrders" to mapOf("eventId" to null))))
            ),
            updates
        )
    }

    @Test
    fun `clearing the local mirror does not write into SDK context during lifecycle reset`() {
        host.setCurrentEvent(1234567L)

        host.clearLocalContext()

        assertNull(host.currentEventId)
        assertEquals(1, updates.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a negative event id is rejected`() {
        host.setCurrentEvent(-1L)
    }
}
