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
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.time.ExperimentalTime

@ExperimentalTime
class ConciergeStateRepositoryTest {
    private lateinit var repository: ConciergeStateRepository
    private lateinit var mockApi: ExtensionApi
    private lateinit var mockEvent: Event

    @Before
    fun setup() {
        // Create a new instance for each test to ensure isolation
        repository = ConciergeStateRepository.instance
        repository.clear()
        
        mockApi = mockk(relaxed = true)
        mockEvent = mockk(relaxed = true)
    }

    // ========== Initial State Tests ==========

    @Test
    fun `initial state has null experienceCloudId and not ready`() = runTest {
        val state = repository.state.first()
        
        assertNull(state.experienceCloudId)
        assertFalse(state.configurationReady)
        assertNull(state.conciergeSurfaces)
        assertNull(state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    // ========== updateExperienceCloudId Tests ==========

    @Test
    fun `updateExperienceCloudId updates ECID from shared state`() = runTest {
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

        repository.updateExperienceCloudId(mockApi, event)

        val state = repository.state.first()
        assertEquals("test-ecid-12345", state.experienceCloudId)
    }

    @Test
    fun `updateExperienceCloudId handles empty ECID list`() = runTest {
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

        repository.updateExperienceCloudId(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
    }

    @Test
    fun `updateExperienceCloudId handles empty ECID string`() = runTest {
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

        repository.updateExperienceCloudId(mockApi, event)

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
    }

    // ========== updateConfiguration Tests ==========

    @Test
    fun `updateConfiguration sets configuration ready with valid config`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1", "surface2")
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
        assertEquals(listOf("surface1", "surface2"), state.conciergeSurfaces)
    }

    @Test
    fun `updateConfiguration handles null configuration`() = runTest {
        repository.updateConfiguration(null)

        val state = repository.state.first()
        assertFalse(state.configurationReady)
        assertEquals(emptyList<String>(), state.conciergeSurfaces)
        assertEquals("", state.conciergeServer)
        assertEquals("", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles empty configuration value`() = runTest {
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns emptyMap()

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertFalse(state.configurationReady)
        assertEquals(emptyList<String>(), state.conciergeSurfaces)
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
        assertEquals(emptyList<String>(), state.conciergeSurfaces)
        assertEquals("", state.conciergeServer)
        assertEquals("", state.conciergeConfigId)
    }

    @Test
    fun `updateConfiguration handles missing concierge server`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1")
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertNull(state.conciergeServer)
        assertEquals("test-config-123", state.conciergeConfigId)
        assertEquals(listOf("surface1"), state.conciergeSurfaces)
    }

    @Test
    fun `updateConfiguration handles missing configId`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.surfaces" to listOf("surface1")
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("test-server.com", state.conciergeServer)
        assertNull(state.conciergeConfigId)
        assertEquals(listOf("surface1"), state.conciergeSurfaces)
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
        assertNull(state.conciergeSurfaces)
    }

    @Test
    fun `updateConfiguration handles empty surfaces list`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to emptyList<String>()
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertNull(state.conciergeSurfaces)
    }

    @Test
    fun `updateConfiguration handles surfaces with null values`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1", null, "surface2")
        )

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns configMap

        repository.updateConfiguration(sharedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        val surfaces = state.conciergeSurfaces
        if (surfaces != null) {
            assertTrue(surfaces.contains("surface1"))
            assertTrue(surfaces.contains("surface2"))
            // Surfaces should not contain null values after filtering
            assertFalse(surfaces.any { it == null })
        }
    }

    @Test
    fun `updateConfiguration handles empty server string`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1")
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
            "concierge.configId" to "",
            "concierge.surfaces" to listOf("surface1")
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
            "concierge.configId" to "initial-config",
            "concierge.surfaces" to listOf("surface1")
        )
        val initialStateResult = mockk<SharedStateResult>()
        every { initialStateResult.value } returns initialConfigMap
        repository.updateConfiguration(initialStateResult)

        val updatedConfigMap = mapOf<String?, Any?>(
            "concierge.server" to "updated-server.com",
            "concierge.configId" to "updated-config",
            "concierge.surfaces" to listOf("surface2", "surface3")
        )
        val updatedStateResult = mockk<SharedStateResult>()
        every { updatedStateResult.value } returns updatedConfigMap
        repository.updateConfiguration(updatedStateResult)

        val state = repository.state.first()
        assertTrue(state.configurationReady)
        assertEquals("updated-server.com", state.conciergeServer)
        assertEquals("updated-config", state.conciergeConfigId)
        assertEquals(listOf("surface2", "surface3"), state.conciergeSurfaces)
    }

    // ========== clear Tests ==========

    @Test
    fun `clear resets state to initial values`() = runTest {
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1")
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
        repository.updateExperienceCloudId(mockApi, event)

        repository.clear()

        val state = repository.state.first()
        assertNull(state.experienceCloudId)
        assertFalse(state.configurationReady)
        assertNull(state.conciergeSurfaces)
        assertNull(state.conciergeServer)
        assertNull(state.conciergeConfigId)
    }

    // ========== StateFlow Emission Tests ==========

    @Test
    fun `state flow emits updates when configuration changes`() = runTest {
        val emissions = mutableListOf<ConciergeState>()
        
        val configMap = mapOf<String?, Any?>(
            "concierge.server" to "test-server.com",
            "concierge.configId" to "test-config-123",
            "concierge.surfaces" to listOf("surface1")
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

        repository.updateExperienceCloudId(mockApi, event)
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
                ConciergeConstants.SharedState.Configuration.CONCIERGE_CONFIG_ID to "test-config",
                ConciergeConstants.SharedState.Configuration.CONCIERGE_SURFACES to listOf("surface1")
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
        assertEquals(listOf("surface1"), state.conciergeSurfaces)
    }

    @Test
    fun `default consent value is set correctly on initial state`() = runTest {
        // Given - Fresh repository instance
        repository.clear()

        // Then
        assertEquals(ConciergeConstants.ConsentValues.DEFAULT_VALUE, repository.state.value.consent)
        assertEquals(ConciergeConstants.ConsentValues.UNKNOWN_VALUE, repository.state.value.consent)
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

