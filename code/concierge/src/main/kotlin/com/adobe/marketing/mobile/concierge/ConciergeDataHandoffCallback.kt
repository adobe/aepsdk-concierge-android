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

/**
 * Reports the outcome of a [Concierge.sendDataHandoff] call.
 *
 * Invoked exactly once, on a background thread.
 */
fun interface ConciergeDataHandoffCallback {

    /**
     * @param accepted whether the SDK validated the payload's shape. This is not a
     * delivery-confirmation signal — there is no independent signal today that Brand Concierge
     * received or processed the payload.
     * @param rejectReason a reason code when [accepted] is false (see
     * [ConciergeConstants.DataHandoff.RejectReason]), or null when [accepted] is true.
     */
    fun onResult(accepted: Boolean, rejectReason: String?)
}
