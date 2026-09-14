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
import org.junit.Assert.assertFalse
import org.junit.Test

class NotImplementedDataHandoffForwarderTest {

    @Test
    fun `forward reports not delivered with the not-implemented error code synchronously`() {
        val result = ConciergeDataHandoffEvent(
            routingHint = "buy_now",
            xdmFields = mapOf("orderId" to "abc-123")
        )

        var calledDelivered: Boolean? = null
        var calledErrorCode: String? = null
        NotImplementedDataHandoffForwarder.forward(result) { delivered, errorCode ->
            calledDelivered = delivered
            calledErrorCode = errorCode
        }

        assertFalse(requireNotNull(calledDelivered))
        assertEquals(ConciergeConstants.DataHandoff.DeliveryErrorCode.NOT_IMPLEMENTED, calledErrorCode)
    }
}
