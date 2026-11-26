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

import com.adobe.marketing.mobile.services.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds the [VoiceTransport] the app registered with [Concierge.setVoiceTransport].
 *
 * Every chat observes it, so a new value reaches the chats already open as well as later ones. A
 * running conversation keeps the transport it started with.
 */
internal object ConciergeVoiceTransportHolder {

    private const val LOG_TAG = "ConciergeVoiceTransportHolder"

    private val _transport = MutableStateFlow<VoiceTransport?>(null)

    /** The registered transport, or null when the app offers no voice. */
    val transport: StateFlow<VoiceTransport?> = _transport.asStateFlow()

    /** Replaces the registered transport. Pass null to clear it. */
    fun set(transport: VoiceTransport?) {
        _transport.value = transport
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            LOG_TAG,
            if (transport == null) "Voice transport cleared" else "Voice transport registered"
        )
    }
}
