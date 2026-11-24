/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class ConciergeConversationServiceClientTimeTest {

    @Test
    fun timeFieldsUseDeviceFormatterAndDstOffset() {
        val india = createXdmTime(Date(1_705_322_096_789L), TimeZone.getTimeZone("Asia/Kolkata"))
        assertEquals("2024-01-15T12:34:56.789Z", india.timestamp)
        assertEquals("2024-01-15T18:04:56.789+05:30", india.localTime)
        assertEquals(-330, india.localTimezoneOffset)

        val pacific = TimeZone.getTimeZone("America/Los_Angeles")
        val before = createXdmTime(Date(1_710_064_799_999L), pacific)
        assertEquals("2024-03-10T09:59:59.999Z", before.timestamp)
        assertEquals("2024-03-10T01:59:59.999-08:00", before.localTime)
        assertEquals(480, before.localTimezoneOffset)

        val after = createXdmTime(Date(1_710_064_800_000L), pacific)
        assertEquals("2024-03-10T10:00:00.000Z", after.timestamp)
        assertEquals("2024-03-10T03:00:00.000-07:00", after.localTime)
        assertEquals(420, after.localTimezoneOffset)
    }
}
