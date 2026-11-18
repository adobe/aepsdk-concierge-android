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

import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.resetFnbContextForIdentityChange
import java.util.concurrent.atomic.AtomicBoolean

/** Clears app-owned F&B mirrors when the host requests an identity reset. */
object FnbIdentityLifecycle {
    private val registered = AtomicBoolean(false)

    fun register() {
        if (!registered.compareAndSet(false, true)) return
        MobileCore.registerEventListener(EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET) {
            resetFnbContextForIdentityChange()
        }
    }
}
