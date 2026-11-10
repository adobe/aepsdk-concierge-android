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

package com.adobe.marketing.mobile.concierge.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Allows one voice conversation at a time across the app.
 *
 * Every chat has its own view model, and several can be alive together (a chat in one activity
 * stays alive while another activity opens a second chat on top), usually with the same transport.
 * A second conversation on it would take the transport's listener, so the first conversation's
 * transcript would appear in the second chat, and ending either one would hang up the other's
 * call. So a chat claims ownership when the user starts voice, before its conversation touches the
 * transport, and keeps it until the conversation ends. Other chats can't start voice meanwhile.
 */
internal class VoiceSessionOwnership {

    /** Held by the chat whose conversation owns voice. Releasing it frees voice for the other chats. */
    inner class Claim {
        /** Frees voice for the other chats. Calling it again has no effect. */
        fun release() {
            owner.compareAndSet(this, null)
        }
    }

    private val owner = MutableStateFlow<Claim?>(null)

    /** The claim of the conversation that owns voice right now, or null when voice is free. */
    val currentClaim: StateFlow<Claim?> = owner.asStateFlow()

    /** Returns a claim, or null when another conversation already owns voice. */
    fun claim(): Claim? {
        val claim = Claim()
        return claim.takeIf { owner.compareAndSet(null, it) }
    }

    companion object {
        /** Shared by every chat in the app. */
        val shared = VoiceSessionOwnership()
    }
}
