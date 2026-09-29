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
    fun `data handoff event name constants have the published wire values`() {
        assertEquals("Concierge Data Handoff Event", ConciergeConstants.DataHandoff.EventName.REQUEST)
        assertEquals("Concierge Data Handoff Event Response", ConciergeConstants.DataHandoff.EventName.RESPONSE)
    }

    @Test
    fun `data handoff event data keys have the published wire values`() {
        assertEquals("routingHint", ConciergeConstants.DataHandoff.EventData.Key.ROUTING_HINT)
        assertEquals("xdmFields", ConciergeConstants.DataHandoff.EventData.Key.XDM_FIELDS)
        assertEquals("localMessage", ConciergeConstants.DataHandoff.EventData.Key.LOCAL_MESSAGE)
    }

    @Test
    fun `data handoff response keys have the published wire values`() {
        assertEquals("accepted", ConciergeConstants.DataHandoff.ResponseKey.ACCEPTED)
        assertEquals("rejectReason", ConciergeConstants.DataHandoff.ResponseKey.REJECT_REASON)
    }

    @Test
    fun `reserved xdm keys contain exactly identityMap`() {
        assertEquals(setOf("identityMap"), ConciergeConstants.DataHandoff.RESERVED_XDM_KEYS)
    }

    @Test
    fun `data handoff reject reasons have the published wire values`() {
        assertEquals("missing_event_data", ConciergeConstants.DataHandoff.RejectReason.MISSING_EVENT_DATA)
        assertEquals("invalid_routing_hint_type", ConciergeConstants.DataHandoff.RejectReason.INVALID_ROUTING_HINT_TYPE)
        assertEquals("missing_xdm_fields", ConciergeConstants.DataHandoff.RejectReason.MISSING_XDM_FIELDS)
        assertEquals("invalid_xdm_fields_type", ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELDS_TYPE)
        assertEquals("empty_xdm_fields", ConciergeConstants.DataHandoff.RejectReason.EMPTY_XDM_FIELDS)
        assertEquals("invalid_xdm_field_key", ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_KEY)
        assertEquals("reserved_key_collision", ConciergeConstants.DataHandoff.RejectReason.RESERVED_KEY_COLLISION)
        assertEquals("invalid_xdm_field_value", ConciergeConstants.DataHandoff.RejectReason.INVALID_XDM_FIELD_VALUE)
    }
}
