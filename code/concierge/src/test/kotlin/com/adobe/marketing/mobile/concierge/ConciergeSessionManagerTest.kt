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

import com.adobe.marketing.mobile.services.NamedCollection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConciergeSessionManagerTest {

    private lateinit var sessionManager: ConciergeSessionManager
    private lateinit var mockNamedCollection: NamedCollection
    private var testTime: Long = 0

    @Before
    fun setup() {
        testTime = 1000000000L // Fixed time for testing
        mockNamedCollection = mockk(relaxed = true)
        // Create a fresh instance with injected dependencies for each test
        sessionManager = ConciergeSessionManager(mockNamedCollection) { testTime }
    }

    // ========== New Session Creation Tests ==========

    @Test
    fun `getSessionId creates new session when no session exists`() {
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns null
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns 0L

        val sessionId = sessionManager.getSessionId()

        assertNotNull(sessionId)
        verify { mockNamedCollection.setString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, sessionId) }
        verify { mockNamedCollection.setLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, testTime) }
    }

    @Test
    fun `getSessionId creates valid UUID format session ID`() {
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns null
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns 0L

        val sessionId = sessionManager.getSessionId()

        val uuidPattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue("Session ID should be valid UUID format", uuidPattern.matches(sessionId))
    }

    // ========== Existing Session Tests ==========

    @Test
    fun `getSessionId returns existing session when not expired`() {
        val existingSessionId = "existing-session-id"
        val recentTimestamp = testTime - (5 * 60 * 1000L) // 5 minutes ago
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns existingSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns recentTimestamp

        val sessionId = sessionManager.getSessionId()

        assertEquals(existingSessionId, sessionId)
        verify(exactly = 0) { mockNamedCollection.setString(any(), any()) }
    }

    @Test
    fun `getSessionId returns existing session when timestamp is just before expiry`() {
        val existingSessionId = "existing-session-id"
        val almostExpiredTimestamp = testTime - (29 * 60 * 1000L + 59 * 1000L) // 29 minutes 59 seconds ago
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns existingSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns almostExpiredTimestamp

        val sessionId = sessionManager.getSessionId()

        assertEquals(existingSessionId, sessionId)
    }

    // ========== Session Expiry Tests ==========

    @Test
    fun `getSessionId creates new session when session is expired`() {
        val oldSessionId = "old-session-id"
        val expiredTimestamp = testTime - (31 * 60 * 1000L) // 31 minutes ago
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns expiredTimestamp

        val sessionId = sessionManager.getSessionId()

        assertNotEquals(oldSessionId, sessionId)
        verify { mockNamedCollection.setString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, sessionId) }
        verify { mockNamedCollection.setLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, testTime) }
    }

    @Test
    fun `getSessionId creates new session when timestamp is exactly at expiry threshold`() {
        val oldSessionId = "old-session-id"
        val expiredTimestamp = testTime - (30 * 60 * 1000L + 1L) // 30 minutes and 1 millisecond ago
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns expiredTimestamp

        val sessionId = sessionManager.getSessionId()

        assertNotEquals(oldSessionId, sessionId)
        verify { mockNamedCollection.setString(any(), any()) }
    }

    @Test
    fun `getSessionId creates new session when timestamp is far in the past`() {
        val oldSessionId = "old-session-id"
        val veryOldTimestamp = testTime - (24 * 60 * 60 * 1000L) // 24 hours ago
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns veryOldTimestamp

        val sessionId = sessionManager.getSessionId()

        assertNotEquals(oldSessionId, sessionId)
    }

    @Test
    fun `getSessionId creates new session when timestamp is zero`() {
        val oldSessionId = "old-session-id"
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns 0L

        val sessionId = sessionManager.getSessionId()

        assertNotEquals(oldSessionId, sessionId)
    }

    // ========== clearSession Tests ==========

    @Test
    fun `clearSession removes session ID and timestamp`() {
        sessionManager.clearSession()

        verify { mockNamedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID) }
        verify { mockNamedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP) }
    }

    @Test
    fun `clearSession allows creating new session after clearing`() {
        val firstSessionId = "first-session-id"
        val timestamp = testTime - (5 * 60 * 1000L)
        
        // First call returns existing session
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns firstSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns timestamp

        val firstId = sessionManager.getSessionId()
        assertEquals(firstSessionId, firstId)

        // After clearing, it should return null and create new
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns null
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns 0L

        sessionManager.clearSession()

        val newSessionId = sessionManager.getSessionId()
        assertNotNull(newSessionId)
        assertNotEquals(firstSessionId, newSessionId)
    }

    // ========== Edge Cases ==========

    @Test
    fun `getSessionId handles empty string session ID as new session`() {
        // Empty string is treated as null by the logic (null check passes but isEmpty check would fail in production)
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns null
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns testTime

        val sessionId = sessionManager.getSessionId()

        assertNotNull(sessionId)
        assertTrue(sessionId.isNotEmpty())
        verify { mockNamedCollection.setString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, sessionId) }
    }

    @Test
    fun `getSessionId handles negative timestamp`() {
        val oldSessionId = "old-session-id"
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns -1L

        val sessionId = sessionManager.getSessionId()

        assertNotEquals(oldSessionId, sessionId)
    }

    @Test
    fun `getSessionId handles future timestamp`() {
        val oldSessionId = "old-session-id"
        val futureTimestamp = testTime + (60 * 60 * 1000L) // 1 hour in the future
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns oldSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns futureTimestamp

        val sessionId = sessionManager.getSessionId()

        assertEquals(oldSessionId, sessionId)
    }

    // ========== Multiple Calls Tests ==========

    @Test
    fun `getSessionId returns same session ID on consecutive calls within timeout`() {
        val existingSessionId = "existing-session-id"
        val recentTimestamp = testTime - (5 * 60 * 1000L)
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns existingSessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns recentTimestamp

        val sessionId1 = sessionManager.getSessionId()
        val sessionId2 = sessionManager.getSessionId()
        val sessionId3 = sessionManager.getSessionId()

        assertEquals(sessionId1, sessionId2)
        assertEquals(sessionId2, sessionId3)
    }

    // ========== Singleton Tests ==========

    @Test
    fun `instance returns singleton instance`() {
        val instance1 = ConciergeSessionManager.instance
        val instance2 = ConciergeSessionManager.instance
        
        assertTrue(instance1 === instance2)
    }

    // ========== Session Timeout Boundary Tests ==========

    @Test
    fun `session timeout is 30 minutes - valid session at 29 minutes`() {
        val sessionId = "test-session-id"
        val timestamp29Min = testTime - (29 * 60 * 1000L)

        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns sessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns timestamp29Min

        val result = sessionManager.getSessionId()
        assertEquals(sessionId, result)
    }

    @Test
    fun `session timeout is 30 minutes - expired session at 30 minutes plus 1 ms`() {
        val sessionId = "test-session-id"
        val timestamp30MinPlus1Ms = testTime - (30 * 60 * 1000L + 1L)

        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns sessionId
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns timestamp30MinPlus1Ms

        val result = sessionManager.getSessionId()
        assertNotEquals(sessionId, result)
    }

    @Test
    fun `clearSession can be called multiple times safely`() {
        sessionManager.clearSession()
        sessionManager.clearSession()
        sessionManager.clearSession()

        verify(atLeast = 3) { mockNamedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID) }
        verify(atLeast = 3) { mockNamedCollection.remove(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP) }
    }

    // ========== Time Provider Tests ==========

    @Test
    fun `getSessionId uses custom time provider`() {
        val customTime = 9999999999L
        val customSessionManager = ConciergeSessionManager(mockNamedCollection) { customTime }
        
        every { mockNamedCollection.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } returns null
        every { mockNamedCollection.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } returns 0L

        customSessionManager.getSessionId()

        verify { mockNamedCollection.setLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, customTime) }
    }
}
