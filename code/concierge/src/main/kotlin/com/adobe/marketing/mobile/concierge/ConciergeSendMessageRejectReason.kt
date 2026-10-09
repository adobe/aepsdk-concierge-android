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

/** Why the SDK did not admit a [Concierge.sendMessage] call into the active chat session. */
enum class ConciergeSendMessageRejectReason {
    /** The message was empty or contained only whitespace. */
    EMPTY_MESSAGE,

    /** The message exceeded [ConciergeConstants.SendMessage.MAX_MESSAGE_LENGTH] characters. */
    MESSAGE_TOO_LONG,

    /** No rendered Concierge chat session was available to receive the message. */
    NO_ACTIVE_SESSION,

    /**
     * A chat turn or data handoff is active or waiting to run. Programmatic messages are never
     * queued behind other turns; callers can retry after the current turn completes.
     */
    CHAT_IN_PROGRESS,

    /** The chat session could not queue the message (for example, it was shutting down). */
    DELIVERY_FAILED
}
