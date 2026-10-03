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

import com.adobe.marketing.mobile.services.Log

/** Admits a programmatic user message into a chat session. Returns null on success. */
internal fun interface ConciergeMessageSender {
    fun send(message: String): ConciergeSendMessageRejectReason?
}

/**
 * Routes [Concierge.sendMessage] to the chat host currently rendered by the app, so the message
 * joins the same transcript and request queue as a typed message. Holds at most one active sender;
 * the most recently registered chat host wins.
 */
internal object ActiveConciergeMessageSender {
    private const val SELF_TAG = "ActiveConciergeMessageSender"

    private var activeSender: ConciergeMessageSender? = null

    internal fun register(sender: ConciergeMessageSender) {
        synchronized(this) { activeSender = sender }
    }

    internal fun unregister(sender: ConciergeMessageSender) {
        synchronized(this) {
            if (activeSender === sender) activeSender = null
        }
    }

    fun send(message: String, completion: ConciergeSendMessageCallback?) {
        val reason = validate(message) ?: run {
            val sender = synchronized(this) { activeSender }
            if (sender == null) {
                ConciergeSendMessageRejectReason.NO_ACTIVE_SESSION
            } else {
                try {
                    sender.send(message)
                } catch (e: Exception) {
                    Log.warning(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "Message sender threw: ${e.message}")
                    ConciergeSendMessageRejectReason.DELIVERY_FAILED
                }
            }
        }
        if (reason != null) {
            Log.debug(ConciergeConstants.EXTENSION_NAME, SELF_TAG, "sendMessage rejected: $reason")
        }
        completion?.onResult(reason == null, reason)
    }

    private fun validate(message: String): ConciergeSendMessageRejectReason? = when {
        message.isBlank() -> ConciergeSendMessageRejectReason.EMPTY_MESSAGE
        message.length > ConciergeConstants.SendMessage.MAX_MESSAGE_LENGTH ->
            ConciergeSendMessageRejectReason.MESSAGE_TOO_LONG
        else -> null
    }
}
