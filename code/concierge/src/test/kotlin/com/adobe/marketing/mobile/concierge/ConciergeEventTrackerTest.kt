/*
  Copyright 2026 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.MobileCore
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConciergeEventTrackerTest {

    @Test
    fun `conversationEnded is never forwarded to Edge even with tracking enabled`() {
        val notification = ConciergeTrackingEvent.ConversationEnded(
            123L, "old-session", "old-conversation", true
        ).toEvent()
        ConciergeEventTracker.trackEvent(notification)
        verify(exactly = 0) { MobileCore.dispatchEvent(any()) }
    }

    @Before
    fun setup() {
        mockkStatic(MobileCore::class)
        every { MobileCore.dispatchEvent(any()) } returns Unit
        ConciergeEventTracker.enableTracking(true)
    }

    @After
    fun tearDown() {
        ConciergeEventTracker.enableTracking(false)
        unmockkStatic(MobileCore::class)
    }

    @Test
    fun `querySubmitted XDM context is not forwarded to Edge`() {
        val xdmFields = mapOf("loyalty" to mapOf("tier" to "gold"))
        val notification = ConciergeTrackingEvent.QuerySubmitted("hello", xdmFields).toEvent()
        val dispatchedEvents = mutableListOf<Event>()

        ConciergeEventTracker.trackEvent(notification)
        verify(exactly = 1) { MobileCore.dispatchEvent(capture(dispatchedEvents)) }

        val edgeEvent = dispatchedEvents.single()
        assertEquals(EventType.EDGE, edgeEvent.type)
        assertEquals(EventSource.REQUEST_CONTENT, edgeEvent.source)
        assertEquals(
            mapOf(
                ConciergeConstants.TrackingEvent.EventData.Key.EVENT_TYPE to
                    ConciergeConstants.TrackingEvent.XDMType.QUERY_SUBMITTED
            ),
            edgeEvent.eventData?.get("data")
        )
    }
}
