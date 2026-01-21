/*
  Copyright 2025 Adobe. All rights reserved.
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
import com.adobe.marketing.mobile.services.ServiceProvider
import java.util.UUID

/**
 * Manages the Concierge session ID with timeout functionality.
 * 
 * The session ID is stored in NamedCollection along with its creation timestamp.
 * Sessions expire after 30 minutes of inactivity. When a request is made, the
 * session ID is validated and a new one is created if it has expired.
 */
internal class ConciergeSessionManager private constructor() {

    companion object {
        private const val TAG = "ConciergeSessionManager"
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L // 30 minutes in milliseconds

        internal val instance: ConciergeSessionManager by lazy {
            ConciergeSessionManager()
        }
    }

    private val namedCollection = ServiceProvider.getInstance()
        .dataStoreService
        .getNamedCollection(ConciergeConstants.DATA_STORE_NAME)

    /**
     * Gets a valid session ID. If the current session ID is expired or doesn't exist,
     * a new one is created and stored.
     * 
     * @return A valid session ID string
     */
    fun getSessionId(): String {
        val currentSessionId = namedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null)
        val sessionTimestamp = namedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L)
        
        val currentTime = System.currentTimeMillis()
        val isExpired = currentTime - sessionTimestamp > SESSION_TIMEOUT_MS
        
        return if (currentSessionId != null && !isExpired) {
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Using existing session ID: $currentSessionId"
            )
            currentSessionId
        } else {
            val newSessionId = UUID.randomUUID().toString()
            namedCollection.setString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, newSessionId)
            namedCollection.setLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, currentTime)
            Log.debug(
                ConciergeConstants.EXTENSION_NAME,
                TAG,
                "Created new session ID: $newSessionId (expired: $isExpired)"
            )
            newSessionId
        }
    }

    /**
     * Clears the current session ID and timestamp.
     * Useful for testing or when explicitly resetting the session.
     */
    fun clearSession() {
        namedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID)
        namedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP)
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            TAG,
            "Session cleared"
        )
    }
}

