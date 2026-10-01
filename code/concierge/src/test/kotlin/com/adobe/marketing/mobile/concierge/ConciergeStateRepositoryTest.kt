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

import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import com.adobe.marketing.mobile.services.NamedCollection
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ConciergeStateRepositoryTest {
    private lateinit var repository: ConciergeStateRepository
    private lateinit var mockApi: ExtensionApi
    private lateinit var mockEvent: Event
    private lateinit var sessionManager: ConciergeSessionManager
    private var sessionId = "session-1"

    @Before
    fun setup() {
        // Create a new instance for each test to ensure isolation
        sessionId = "session-1"
        sessionManager = mockk()
        every { sessionManager.getSessionId() } answers { sessionId }
        repository = ConciergeStateRepository(sessionManager = sessionManager)
        
        mockApi = mockk(relaxed = true)
        mockEvent = mockk(relaxed = true)
    }

    @Test
    fun `updateXDMContext recursively merges nested maps`() {
        repository.updateXDMContext(mapOf("fan" to mapOf("seatSection" to "112", "seatRow" to "A")))
        repository.updateXDMContext(mapOf("fan" to mapOf("seatRow" to "B")))

        assertEquals(
            mapOf("fan" to mapOf("seatSection" to "112", "seatRow" to "B")),
            repository.snapshotXDMContext("session-1")
        )
    }

    @Test
    fun `updateXDMContext null removes nested key even below a missing object`() {
        repository.updateXDMContext(mapOf("fan" to mapOf("seatSection" to null)))

        assertEquals(mapOf("fan" to emptyMap<String, Any>()), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `updateXDMContext null removes key and preserves sibling values`() {
        repository.updateXDMContext(mapOf("fan" to mapOf("seatSection" to "112", "seatRow" to "A")))
        repository.updateXDMContext(mapOf("fan" to mapOf("seatSection" to null)))

        assertEquals(mapOf("fan" to mapOf("seatRow" to "A")), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `updateXDMContext arrays replace previous values`() {
        repository.updateXDMContext(mapOf("items" to listOf("a", "b")))
        repository.updateXDMContext(mapOf("items" to listOf("c")))

        assertEquals(mapOf("items" to listOf("c")), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `updateXDMContext preserves nulls inside arrays and maps within arrays`() {
        val fields = mapOf("items" to listOf(null, mapOf("value" to null)))

        repository.updateXDMContext(fields)

        assertEquals(fields, repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `updateXDMContext rejects reserved identityMap key without changing state`() {
        try {
            repository.updateXDMContext(mapOf("identityMap" to mapOf("ECID" to emptyList<Any>())))
            throw AssertionError("Expected reserved key to be rejected")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("identityMap"))
        }

        assertEquals(emptyMap<String, Any>(), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `updateXDMContext rejects unsupported values without changing state`() {
        try {
            repository.updateXDMContext(mapOf("value" to Any()))
            throw AssertionError("Expected unsupported value to be rejected")
        } catch (exception: IllegalArgumentException) {
            assertTrue(exception.message.orEmpty().contains("Unsupported value type"))
        }

        assertEquals(emptyMap<String, Any>(), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `first session preserves context set before its first request`() {
        repository.updateXDMContext(mapOf("loggedIn" to true))

        assertEquals(mapOf("loggedIn" to true), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `same session id preserves held context across turns`() {
        repository.updateXDMContext(mapOf("loggedIn" to true))
        repository.snapshotXDMContext("session-1")
        repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "gold")))

        assertEquals(
            mapOf("loggedIn" to true, "loyalty" to mapOf("tier" to "gold")),
            repository.snapshotXDMContext("session-1")
        )
    }

    @Test
    fun `new session id clears all held context`() {
        repository.updateXDMContext(mapOf("loggedIn" to true))
        repository.snapshotXDMContext("session-1")

        assertEquals(emptyMap<String, Any>(), repository.snapshotXDMContext("session-2"))
    }

    @Test
    fun `update after rollover replaces stale context and survives the next turn`() {
        repository.updateXDMContext(mapOf("loggedIn" to true))
        repository.snapshotXDMContext("session-1")
        sessionId = "session-2"
        repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "gold")))

        assertEquals(
            mapOf("loyalty" to mapOf("tier" to "gold")),
            repository.snapshotXDMContext("session-2")
        )
    }

    @Test
    fun `context update resolves expiry before the first request`() {
        repository.updateXDMContext(mapOf("old" to true))
        sessionId = "session-2"
        repository.updateXDMContext(mapOf("fresh" to true))

        assertEquals(mapOf("fresh" to true), repository.snapshotXDMContext("session-2"))
    }

    @Test
    fun `snapshot is isolated from caller mutation`() {
        repository.updateXDMContext(mapOf("loyalty" to mapOf("tier" to "gold")))
        val snapshot = repository.snapshotXDMContext("session-1")
        @Suppress("UNCHECKED_CAST")
        (snapshot["loyalty"] as MutableMap<String, Any>)["tier"] = "changed"

        assertEquals(mapOf("loyalty" to mapOf("tier" to "gold")), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `real inactivity rollover retains context re-established before the next send`() {
        var now = 1_000_000L
        var storedId: String? = null
        var storedTimestamp = 0L
        val store = mockk<NamedCollection>()
        every { store.getString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, null) } answers { storedId }
        every { store.getLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, 0L) } answers { storedTimestamp }
        every { store.setString(ConciergeConstants.DataStoreKeys.KEY_SESSION_ID, any()) } answers {
            storedId = secondArg()
        }
        every { store.setLong(ConciergeConstants.DataStoreKeys.KEY_SESSION_TIMESTAMP, any()) } answers {
            storedTimestamp = secondArg()
        }
        val manager = ConciergeSessionManager(store) { now }
        val subject = ConciergeStateRepository(sessionManager = manager)
        subject.updateXDMContext(mapOf("stale" to true))
        val oldId = manager.getSessionId()

        now += ConciergeSessionManager.SESSION_TIMEOUT_MS + 1
        subject.updateXDMContext(mapOf("fresh" to true))
        val newId = manager.getSessionId()
        assertTrue(oldId != newId)
        assertEquals(mapOf("fresh" to true), subject.snapshotXDMContext(newId))

        now += 100
        subject.updateXDMContext(mapOf("another" to true))
        assertEquals(now - 100, storedTimestamp)
        now += ConciergeSessionManager.SESSION_TIMEOUT_MS
        assertEquals(emptyMap<String, Any>(), subject.snapshotXDMContext(manager.getSessionId()))
    }

    @Test
    fun `invalid patch after expiry leaves previous state and session association unchanged`() {
        repository.updateXDMContext(mapOf("retained" to true))
        sessionId = "session-2"
        assertThrows(IllegalArgumentException::class.java) {
            repository.updateXDMContext(mapOf("invalid" to Any()))
        }
        assertEquals(mapOf("retained" to true), repository.snapshotXDMContext("session-1"))
    }

    @Test
    fun `merge validates bounded input and returns an isolated result`() {
        val cyclic = linkedMapOf<String, Any>()
        cyclic["self"] = cyclic
        assertThrows(IllegalArgumentException::class.java) {
            repository.mergeXdmFields(emptyMap(), mapOf("value" to cyclic))
        }
        val nested = linkedMapOf<String, Any>("tier" to "gold")
        val result = repository.mergeXdmFields(mapOf("loyalty" to nested), mapOf("active" to true))
        nested["tier"] = "changed"
        assertEquals(mapOf("loyalty" to mapOf("tier" to "gold"), "active" to true), result)
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state has null experienceCloudId and not ready`() = runTest {
        val state = repository.state.first()
        
        assertNull(state.experienceCloudId)
        assertFalse(state.configurationReady)
        assertNull(state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    // ========== updateIdentity Tests ==========

    @Test
    fun `updateIdentity updates ECID from shared state`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String?, Any?>(
            "identityMap" to mapOf(
                "ECID" to listOf(
                    mapOf("id" to "test-ecid-12345")
                )
            )
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertEquals("test-ecid-12345", state.experienceCloudId)
    }

    @Test
    fun `updateIdentity handles empty ECID list`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String?, Any?>(
            "identityMap" to mapOf(
                "ECID" to emptyList<Map<String, String>>()
            )
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
    }

    @Test
    fun `updateIdentity handles empty ECID string`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String?, Any?>(
            "identityMap" to mapOf(
                "ECID" to listOf(
                    mapOf("id" to "")
                )
            )
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
    }

    @Test
    fun `updateIdentity stores full identityMap verbatim with all namespaces`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val identityMap = mapOf(
            "ECID" to listOf(
                mapOf("id" to "test-ecid-12345", "authenticatedState" to "ambiguous", "primary" to false)
            ),
            "hashedEmail" to listOf(
                mapOf("id" to "5e884898da28047151d0e56f8dc62927", "authenticatedState" to "authenticated", "primary" to false)
            ),
            "CustomNamespace" to listOf(
                mapOf("id" to "custom-id-999", "authenticatedState" to "authenticated", "primary" to false)
            )
        )
        val xdmSharedState = mapOf<String?, Any?>("identityMap" to identityMap)

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertEquals("test-ecid-12345", state.experienceCloudId)
        assertEquals(identityMap, state.identityMap)
    }

    @Test
    fun `updateIdentity sets null identityMap when identityMap absent`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns mapOf<String?, Any?>()
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
        assertNull(state.identityMap)
    }

    @Test
    fun `updateIdentity sets null identityMap when identityMap is empty`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        // identityMap key present but empty: the isNotEmpty guard drops it to null.
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns mapOf<String?, Any?>("identityMap" to emptyMap<String, Any?>())
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
        assertNull(state.identityMap)
    }

    // ========== updateConfiguration Tests ==========

    @Test
    fun `updateConfiguration sets configuration ready with valid config`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
        assertNull(state.conciergeRegion)
    }

    @Test
    fun `updateConfiguration sets region when present in config`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.region" to "va6"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("va6", state.conciergeRegion)
    }

    @Test
    fun `updateConfiguration treats empty region string as absent`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.region" to ""
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertNull(state.conciergeRegion)
    }

    @Test
    fun `updateConfiguration handles null configuration`() = runTest {
        repository.updateConfiguration(null)

        val state = repository.state.first()
        assertFalse(state.configurationReady)
        assertEquals("", state.conciergeServer)
        assertEquals("", state.conciergeConfigId)
        assertNull(state.conciergeRegion)
    }

    @Test
    fun `updateConfiguration handles empty configuration value`() = runTest {
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns emptyMap()

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertFalse(state.configurationReady)
        assertEquals("", state.conciergeServer)
        assertEquals("", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles null value in SharedStateResult`() = runTest {
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns null

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertFalse(state.configurationReady)
        assertEquals("", state.conciergeServer)
        assertEquals("", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles missing concierge server`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.configId" to "test-config-123"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertNull(state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles missing configId`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles missing surfaces`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles empty server string`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "",
            "concierge.configId" to "test-config-123"
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertNull(state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles empty configId string`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to ""
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration updates existing configuration`() = runTest {
        val initialConfigMap = mapOf<String?, Any?>(
            "concierge.server" to "initial-server.com",
            "concierge.configId" to "initial-config"
        )
        val initialStateResult = mockk<SharedStateResult>()
        every { initialStateResult.value } returns initialConfigMap
        repository.updateConfiguration(initialStateResult)

        val updatedConfigMap = mapOf<String?, Any?>(
            "concierge.server" to "updated-server.com",
            "concierge.configId" to "updated-config"
        )
        val updatedStateResult = mockk<SharedStateResult>()
        every { updatedStateResult.value } returns updatedConfigMap
        repository.updateConfiguration(updatedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("updated-server.com", state.conciergeServer)
        assertEquals("updated-config", state.conciergeConfigId)
    }

    // ========== clear Tests ==========

    @Test
    fun `clear resets state to initial values`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123"
        )
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap
        repository.updateConfiguration(sharedStateResult)

        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()
        val xdmSharedState = mapOf<String?, Any?>(
            "identityMap" to mapOf(
                "ECID" to listOf(
                    mapOf("id" to "test-ecid")
                )
            )
        )
        val ecidStateResult = mockk<SharedStateResult>()
        every { ecidStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns ecidStateResult
        repository.updateIdentity(mockApi, event)

        repository.clear()

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
        assertNull(state.identityMap)
        assertFalse(state.configurationReady)
        assertNull(state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    // ========== Surfaces (API parameter) tests ==========

    @Test
    fun `getSurfaces returns empty list when not set`() = runTest {
        assertEquals(emptyList<String>(), repository.getSurfaces())
    }

    @Test
    fun `getSurfaces returns surfaces when set`() = runTest {
        repository.setSurfaces(listOf("api-surface-1", "api-surface-2"))
        assertEquals(listOf("api-surface-1", "api-surface-2"), repository.getSurfaces())
    }

    @Test
    fun `getSurfaces returns empty when session surfaces cleared`() = runTest {
        repository.setSurfaces(listOf("api-surface"))
        repository.setSurfaces(null)
        assertEquals(emptyList<String>(), repository.getSurfaces())
    }

    @Test
    fun `setSurfaces with empty list clears surfaces`() = runTest {
        repository.setSurfaces(listOf("api-surface"))
        repository.setSurfaces(emptyList())
        assertEquals(emptyList<String>(), repository.getSurfaces())
    }

    @Test
    fun `clear resets surfaces`() = runTest {
        repository.setSurfaces(listOf("api-surface"))
        repository.clear()
        assertEquals(emptyList<String>(), repository.getSurfaces())
    }

    // ========== StateFlow Emission Tests ==========

    @Test
    fun `state flow emits updates when configuration changes`() = runTest {
        val emissions = mutableListOf<ConciergeState>()
        
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123"
        )
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap
        
        repository.updateConfiguration(sharedStateResult)
        emissions.add(repository.state.first())

        assertEquals(1, emissions.size)
        assertTrue(emissions[0].configurationReady)
        assertEquals("test-server.com", emissions[0].conciergeServer)
    }

    @Test
    fun `state flow emits updates when ECID changes`() = runTest {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String?, Any?>(
            "identityMap" to mapOf(
                "ECID" to listOf(
                    mapOf("id" to "updated-ecid")
                )
            )
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        repository.updateIdentity(mockApi, event)
        val state = repository.state.first()

        assertEquals("updated-ecid", state.experienceCloudId)
    }

    // ========== Singleton Tests ==========

    @Test
    fun `instance returns singleton instance`() {
        val instance1 = ConciergeStateRepository.instance
        val instance2 = ConciergeStateRepository.instance
        
        assertTrue(instance1 === instance2)
    }

    // ========== Consent Tests ==========

    @Test
    fun `updateConsent with valid consent in (y) updates state to in`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("y")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.IN_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with valid consent out (n) updates state to out`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("n")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.OUT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with valid consent unknown (u) updates state to unknown`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("u")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.UNKNOWN_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with null shared state uses default value`() = runTest {
        // Given
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns null

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with empty shared state value uses default value`() = runTest {
        // Given
        val emptySharedState = SharedStateResult(SharedStateStatus.SET, null)
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns emptySharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with missing consents key uses default value`() = runTest {
        // Given
        val sharedState = SharedStateResult(
            SharedStateStatus.SET,
            mutableMapOf<String?, Any?>(
                "someOtherKey" to "value"
            )
        )
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with missing collect key uses default value`() = runTest {
        // Given
        val sharedState = SharedStateResult(
            SharedStateStatus.SET,
            mutableMapOf<String?, Any?>(
                ConciergeConstants.SharedState.Consent.CONSENTS to mutableMapOf<String?, Any?>(
                    "someOtherKey" to "value"
                )
            )
        )
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with missing val key uses default value`() = runTest {
        // Given
        val sharedState = SharedStateResult(
            SharedStateStatus.SET,
            mutableMapOf<String?, Any?>(
                ConciergeConstants.SharedState.Consent.CONSENTS to mutableMapOf<String?, Any?>(
                    ConciergeConstants.SharedState.Consent.COLLECT to mutableMapOf<String?, Any?>(
                        "someOtherKey" to "value"
                    )
                )
            )
        )
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with invalid consent value uses default value`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("invalid-value")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent with empty string consent value uses default value`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
    }

    @Test
    fun `updateConsent calls getXDMSharedState with correct parameters`() = runTest {
        // Given
        val consentSharedState = createConsentSharedState("y")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        verify(exactly = 1) {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        }
    }

    @Test
    fun `updateConsent preserves other state properties when updating consent`() = runTest {
        // Given - Set up initial state
        val configState = SharedStateResult(
            SharedStateStatus.SET,
            mutableMapOf<String?, Any?>(
                ConciergeConstants.SharedState.Configuration.CONCIERGE_SERVER to "test-server",
                ConciergeConstants.SharedState.Configuration.CONCIERGE_CONFIG_ID to "test-config"
            )
        )
        repository.updateConfiguration(configState)

        val consentSharedState = createConsentSharedState("n")
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.Consent.EXTENSION_NAME,
                mockEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns consentSharedState

        // When
        repository.updateConsent(mockApi, mockEvent)

        // Then
        val state = repository.state.value
        assertEquals(ConciergeConstants.ConsentValues.OUT_VALUE, state.consent)
        assertEquals("test-server", state.conciergeServer)
        assertEquals("test-config", state.conciergeConfigId)
    }

    @Test
    fun `default consent value is set correctly on initial state`() = runTest {
        // Given - Fresh repository instance
        repository.clear()

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
        assertEquals(ConciergeConstants.ConsentValues.IN_VALUE, repository.state.value.consent)
    }

    // ========== Helper Methods ==========

    private fun createConsentSharedState(consentValue: String): SharedStateResult {
        return SharedStateResult(
            SharedStateStatus.SET,
            mutableMapOf<String?, Any?>(
                ConciergeConstants.SharedState.Consent.CONSENTS to mutableMapOf<String?, Any?>(
                    ConciergeConstants.SharedState.Consent.COLLECT to mutableMapOf<String?, Any?>(
                        ConciergeConstants.SharedState.Consent.VAL to consentValue
                    )
                )
            )
        )
    }
}
