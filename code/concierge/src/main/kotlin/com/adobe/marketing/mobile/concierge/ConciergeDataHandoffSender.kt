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

package com.adobe.marketing.mobile.concierge

import com.adobe.marketing.mobile.AdobeCallbackWithError
import com.adobe.marketing.mobile.AdobeError
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.MobileCore

/**
 * Builds and dispatches the request [Event] backing [Concierge.sendDataHandoff], translating the
 * extension's response event — or a dispatch failure/timeout — into a single
 * [ConciergeDataHandoffCallback] call.
 */
internal object ConciergeDataHandoffSender {

    fun send(
        routingHint: String,
        xdmFields: Map<String, Any>,
        localMessage: String?,
        completion: ConciergeDataHandoffCallback?
    ) {
        val event = Event.Builder(
            ConciergeConstants.DataHandoff.EventName.REQUEST,
            ConciergeConstants.EventType.CONCIERGE,
            EventSource.REQUEST_CONTENT
        ).setEventData(
            ConciergeDataHandoffEvent(routingHint, xdmFields, localMessage).toEventData()
        ).build()

        MobileCore.dispatchEventWithResponseCallback(
            event,
            ConciergeConstants.DataHandoff.RESPONSE_TIMEOUT_MS,
            object : AdobeCallbackWithError<Event> {
                override fun call(responseEvent: Event) {
                    val keys = ConciergeConstants.DataHandoff.ResponseKey
                    val accepted = responseEvent.eventData?.get(keys.ACCEPTED) as? Boolean ?: false
                    val rawReason = responseEvent.eventData?.get(keys.REJECT_REASON) as? String
                    val rejectReason = ConciergeDataHandoffRejectReason.fromRawValue(rawReason)
                        ?: if (accepted) null else ConciergeDataHandoffRejectReason.NO_RESPONSE
                    completion?.onResult(accepted, rejectReason)
                }

                override fun fail(error: AdobeError) {
                    completion?.onResult(false, ConciergeDataHandoffRejectReason.NO_RESPONSE)
                }
            }
        )
    }
}
