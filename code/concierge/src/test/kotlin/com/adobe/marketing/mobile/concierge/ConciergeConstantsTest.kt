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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConciergeConstantsTest {

    @Test
    fun `data handoff event source constants have the published wire values`() {
        assertEquals("com.adobe.eventSource.dataHandoff", ConciergeConstants.EventSource.DATA_HANDOFF)
        assertEquals("com.adobe.eventSource.dataHandoffDelivery", ConciergeConstants.EventSource.DATA_HANDOFF_DELIVERY)
    }

    @Test
    fun `data handoff event data keys have the published wire values`() {
        assertEquals("routingHint", ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT)
        assertEquals("xdmFields", ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS)
    }

    @Test
    fun `data handoff response keys have the published wire values`() {
        assertEquals("accepted", ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED)
        assertEquals("rejectReason", ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON)
    }

    @Test
    fun `data handoff delivery keys have the published wire values`() {
        assertEquals("delivered", ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERED)
        assertEquals("deliveryErrorCode", ConciergeConstants.DataHandoff.DeliveryEventData.Key.DELIVERY_ERROR_CODE)
        assertEquals("routingHint", ConciergeConstants.DataHandoff.DeliveryEventData.Key.ROUTING_HINT)
        assertEquals("xdmFields", ConciergeConstants.DataHandoff.DeliveryEventData.Key.XDM_FIELDS)
    }

    @Test
    fun `reserved xdm keys contain exactly identityMap`() {
        assertEquals(setOf("identityMap"), ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS)
    }

    @Test
    fun `data handoff delivery error codes have the published wire values`() {
        assertEquals("forwarding_not_implemented", ConciergeConstants.DataHandoff.DeliveryErrorCode.NOT_IMPLEMENTED)
        assertEquals("unreachable", ConciergeConstants.DataHandoff.DeliveryErrorCode.UNREACHABLE)
        assertEquals("timeout", ConciergeConstants.DataHandoff.DeliveryErrorCode.TIMEOUT)
        assertEquals("unknown", ConciergeConstants.DataHandoff.DeliveryErrorCode.UNKNOWN)
    }
}
