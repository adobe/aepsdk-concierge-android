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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConciergeExtensionTest {

    private lateinit var extension: ConciergeExtension
    private lateinit var mockApi: ExtensionApi
    private lateinit var mockStateRepository: ConciergeStateRepository

    @Before
    fun setup() {
        mockApi = mockk(relaxed = true)
        extension = ConciergeExtension(mockApi)
        
        // Mock the singleton repository
        mockStateRepository = mockk(relaxed = true)
        mockkObject(ConciergeStateRepository)
        every { ConciergeStateRepository.instance } returns mockStateRepository
    }

    @After
    fun tearDown() {
        unmockkObject(ConciergeStateRepository)
    }

    // ========== Identity Event Detection Tests ==========

    @Test
    fun `isIdentitySharedStateEvent returns true for identity shared state event`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertTrue(result)
    }

    // ========== Event Processing Tests ==========

    @Test
    fun `processEvent calls updateExperienceCloudId for identity shared state event`() {
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        extension.processEvent(event)

        verify(exactly = 1) { mockStateRepository.updateExperienceCloudId(mockApi, event) }
    }

    @Test
    fun `processEvent calls updateConfiguration for configuration response event`() {
        val event = Event.Builder(
            "Config Event",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val mockConfigState: SharedStateResult = mockk(relaxed = true)
        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns mockConfigState

        extension.processEvent(event)

        verify(exactly = 1) { mockStateRepository.updateConfiguration(mockConfigState) }
    }

    @Test
    fun `processEvent does not throw exception for unknown event type`() {
        val event = Event.Builder(
            "Unknown Event",
            EventType.ANALYTICS,
            EventSource.REQUEST_CONTENT
        ).build()

        // When/Then - should not throw
        extension.processEvent(event)
    }

    // ========== Extension Metadata Tests ==========

    @Test
    fun `extension metadata constants are correct`() {
        // Verify the constants used by the extension
        assertEquals("brandconcierge", ConciergeConstants.EXTENSION_NAME)
        assertEquals("BrandConcierge", ConciergeConstants.EXTENSION_FRIENDLY_NAME)
        assertEquals("3.0.2", ConciergeConstants.VERSION)
    }

    // ========== hasValidXdmSharedState Tests ==========

    @Test
    fun `hasValidXdmSharedState returns true for valid non-empty shared state`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String, Any?>(
            "identityMap" to mapOf(
                "ECID" to listOf(
                    mapOf("id" to "test-ecid")
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

        val result = extension.hasValidXdmSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        assertTrue(result)
    }

    @Test
    fun `hasValidXdmSharedState returns false for null shared state result`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns null

        val result = extension.hasValidXdmSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        assertFalse(result)
    }

    @Test
    fun `hasValidXdmSharedState returns false for empty shared state`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns emptyMap()
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        val result = extension.hasValidXdmSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        assertFalse(result)
    }

    @Test
    fun `hasValidXdmSharedState returns false for shared state with null value`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns null
        every {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns sharedStateResult

        val result = extension.hasValidXdmSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        assertFalse(result)
    }

    @Test
    fun `hasValidXdmSharedState uses LAST_SET resolution`() {
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val xdmSharedState = mapOf<String, Any?>("key" to "value")
        val sharedStateResult = mockk<SharedStateResult>()
        every { sharedStateResult.value } returns xdmSharedState
        every {
            mockApi.getXDMSharedState(
                any(),
                any(),
                any(),
                any()
            )
        } returns sharedStateResult

        extension.hasValidXdmSharedState(
            ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
            event
        )

        verify {
            mockApi.getXDMSharedState(
                ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        }
    }

    // ========== Configuration Event Detection Tests ==========

    @Test
    fun `isConfigurationResponse returns true for configuration response event`() {
        val event = Event.Builder(
            "Config Response",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val result = extension.run { event.isConfigurationResponse() }

        assertTrue(result)
    }

    @Test
    fun `isConfigurationResponse returns false for wrong event type`() {
        val event = Event.Builder(
            "Wrong Type",
            EventType.HUB,
            EventSource.RESPONSE_CONTENT
        ).build()

        val result = extension.run { event.isConfigurationResponse() }

        assertFalse(result)
    }

    @Test
    fun `isConfigurationResponse returns false for wrong event source`() {
        val event = Event.Builder(
            "Wrong Source",
            EventType.CONFIGURATION,
            EventSource.REQUEST_CONTENT
        ).build()

        val result = extension.run { event.isConfigurationResponse() }

        assertFalse(result)
    }

    @Test
    fun `isConfigurationResponse returns false for shared state event`() {
        val event = Event.Builder(
            "Shared State",
            EventType.CONFIGURATION,
            EventSource.SHARED_STATE
        ).build()

        val result = extension.run { event.isConfigurationResponse() }

        assertFalse(result)
    }

    // ========== Identity Event Edge Cases ==========

    @Test
    fun `isIdentitySharedStateEvent returns false for wrong event type`() {
        val event = Event.Builder(
            "Wrong Type",
            EventType.CONFIGURATION,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertFalse(result)
    }

    @Test
    fun `isIdentitySharedStateEvent returns false for wrong event source`() {
        val event = Event.Builder(
            "Wrong Source",
            EventType.HUB,
            EventSource.RESPONSE_CONTENT
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertFalse(result)
    }

    @Test
    fun `isIdentitySharedStateEvent returns false for wrong stateowner`() {
        val event = Event.Builder(
            "Wrong Stateowner",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to "com.adobe.wrong.extension")
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertFalse(result)
    }

    @Test
    fun `isIdentitySharedStateEvent returns false when stateowner is null`() {
        val event = Event.Builder(
            "No Stateowner",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertFalse(result)
    }

    @Test
    fun `isIdentitySharedStateEvent returns false when event data is null`() {
        val event = Event.Builder(
            "No Event Data",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertFalse(result)
    }

    // ========== Event Processing with Null/Empty Data Tests ==========

    @Test
    fun `processEvent handles configuration event with null shared state`() {
        val event = Event.Builder(
            "Config Event",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns null

        // When/Then - should not throw
        extension.processEvent(event)

        // Verify it still tries to update with null
        verify(exactly = 1) { mockStateRepository.updateConfiguration(null) }
    }

    @Test
    fun `processEvent handles event with empty event data map`() {
        val event = Event.Builder(
            "Empty Data",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(emptyMap()).build()

        // When/Then - should not throw
        extension.processEvent(event)
    }

    @Test
    fun `processEvent handles event with wrong type in stateowner`() {
        val event = Event.Builder(
            "Wrong Stateowner Type",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to 123) // Integer instead of String
        ).build()

        // When/Then - should not throw
        extension.processEvent(event)
    }

    // ========== Mixed Event Sequence Tests ==========

    @Test
    fun `processEvent handles mixed event sequence correctly`() {
        val identityEvent = Event.Builder(
            "Identity",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val configEvent = Event.Builder(
            "Config",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val mockConfigState: SharedStateResult = mockk(relaxed = true)
        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                configEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns mockConfigState

        extension.processEvent(identityEvent)
        extension.processEvent(configEvent)

        verify(exactly = 1) { mockStateRepository.updateExperienceCloudId(mockApi, identityEvent) }
        verify(exactly = 1) { mockStateRepository.updateConfiguration(mockConfigState) }
    }

    // ========== Edge Case Event Types ==========

    @Test
    fun `processEvent ignores edge events`() {
        val event = Event.Builder(
            "Edge Event",
            EventType.EDGE,
            EventSource.REQUEST_CONTENT
        ).build()

        extension.processEvent(event)

        // Then - no repository methods should be called
        verify(exactly = 0) { mockStateRepository.updateExperienceCloudId(any(), any()) }
        verify(exactly = 0) { mockStateRepository.updateConfiguration(any()) }
    }

    @Test
    fun `processEvent ignores custom events`() {
        val event = Event.Builder(
            "Custom Event",
            EventType.CUSTOM,
            EventSource.NONE
        ).build()

        extension.processEvent(event)

        // Then - no repository methods should be called
        verify(exactly = 0) { mockStateRepository.updateExperienceCloudId(any(), any()) }
        verify(exactly = 0) { mockStateRepository.updateConfiguration(any()) }
    }

    // ========== readyForEvent Tests ==========

    @Test
    fun `readyForEvent returns true for identity event`() {
        val event = Event.Builder(
            "Identity",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val result = extension.readyForEvent(event)

        assertTrue(result)
    }

    @Test
    fun `readyForEvent returns true for configuration event`() {
        val event = Event.Builder(
            "Config",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val result = extension.readyForEvent(event)

        assertTrue(result)
    }

    @Test
    fun `readyForEvent returns true for any event type`() {
        val events = listOf(
            Event.Builder("Analytics", EventType.ANALYTICS, EventSource.REQUEST_CONTENT).build(),
            Event.Builder("Edge", EventType.EDGE, EventSource.REQUEST_CONTENT).build(),
            Event.Builder("Custom", EventType.CUSTOM, EventSource.NONE).build()
        )

        // When/Then
        events.forEach { event ->
            assertTrue("readyForEvent should return true for ${event.type}", extension.readyForEvent(event))
        }
    }

    // ========== Event Data Variations Tests ==========

    @Test
    fun `isIdentitySharedStateEvent handles event with additional data fields`() {
        val event = Event.Builder(
            "Identity with Extra Data",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf(
                "stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME,
                "extra_field" to "extra_value",
                "another_field" to 123
            )
        ).build()

        val result = extension.run { event.isIdentitySharedStateEvent() }

        assertTrue(result)
    }

    @Test
    fun `isConfigurationResponse handles event with event data`() {
        val event = Event.Builder(
            "Config with Data",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).setEventData(
            mapOf("config_key" to "config_value")
        ).build()

        val result = extension.run { event.isConfigurationResponse() }

        assertTrue(result)
    }

    @Test
    fun `processEvent handles multiple identity events in sequence`() {
        val event1 = Event.Builder(
            "Identity Event 1",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val event2 = Event.Builder(
            "Identity Event 2",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        extension.processEvent(event1)
        extension.processEvent(event2)

        verify(exactly = 2) { mockStateRepository.updateExperienceCloudId(mockApi, any()) }
    }

    @Test
    fun `processEvent handles multiple configuration events in sequence`() {
        val event1 = Event.Builder(
            "Config Event 1",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val event2 = Event.Builder(
            "Config Event 2",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val mockConfigState: SharedStateResult = mockk(relaxed = true)
        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                any(),
                false,
                SharedStateResolution.LAST_SET
            )
        } returns mockConfigState

        extension.processEvent(event1)
        extension.processEvent(event2)

        verify(exactly = 2) { mockStateRepository.updateConfiguration(mockConfigState) }
    }

    // ========== Event Type and Source Combination Tests ==========

    @Test
    fun `isIdentitySharedStateEvent returns false for HUB type with wrong source`() {
        val sources = listOf(
            EventSource.REQUEST_CONTENT,
            EventSource.RESPONSE_CONTENT,
            EventSource.ERROR_RESPONSE_CONTENT,
            EventSource.REQUEST_IDENTITY,
            EventSource.RESPONSE_IDENTITY
        )

        sources.forEach { source ->
            val event = Event.Builder(
                "Wrong Source",
                EventType.HUB,
                source
            ).setEventData(
                mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
            ).build()

            val result = extension.run { event.isIdentitySharedStateEvent() }
            assertFalse("Should return false for source: $source", result)
        }
    }

    @Test
    fun `isConfigurationResponse returns false for CONFIGURATION type with wrong source`() {
        val sources = listOf(
            EventSource.REQUEST_CONTENT,
            EventSource.SHARED_STATE,
            EventSource.ERROR_RESPONSE_CONTENT,
            EventSource.NONE
        )

        sources.forEach { source ->
            val event = Event.Builder(
                "Wrong Source",
                EventType.CONFIGURATION,
                source
            ).build()

            val result = extension.run { event.isConfigurationResponse() }
            assertFalse("Should return false for source: $source", result)
        }
    }

    @Test
    fun `processEvent does not call repository for wildcard event`() {
        val event = Event.Builder(
            "Wildcard Event",
            EventType.WILDCARD,
            EventSource.WILDCARD
        ).build()

        extension.processEvent(event)

        verify(exactly = 0) { mockStateRepository.updateExperienceCloudId(any(), any()) }
        verify(exactly = 0) { mockStateRepository.updateConfiguration(any()) }
    }

    // ========== Shared State Access Tests ==========

    @Test
    fun `processEvent calls getSharedState with correct parameters`() {
        val event = Event.Builder(
            "Config Event",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val mockConfigState: SharedStateResult = mockk(relaxed = true)
        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns mockConfigState

        extension.processEvent(event)

        verify {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                event,
                false,
                SharedStateResolution.LAST_SET
            )
        }
    }

    // ========== Robustness Tests ==========

    @Test
    fun `processEvent handles null event data gracefully`() {
        val event = Event.Builder(
            "Null Data Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        extension.processEvent(event)

        verify(exactly = 0) { mockStateRepository.updateExperienceCloudId(any(), any()) }
    }

    @Test
    fun `isIdentitySharedStateEvent returns false when stateowner has wrong type`() {
        val nonStringStateOwners = listOf(
            mapOf("stateowner" to 123),
            mapOf("stateowner" to true),
            mapOf("stateowner" to listOf("value")),
            mapOf("stateowner" to mapOf("key" to "value"))
        )

        nonStringStateOwners.forEach { eventData ->
            val event = Event.Builder(
                "Wrong Type",
                EventType.HUB,
                EventSource.SHARED_STATE
            ).setEventData(eventData).build()

            val result = extension.run { event.isIdentitySharedStateEvent() }
            assertFalse("Should return false for non-string stateowner: ${eventData["stateowner"]}", result)
        }
    }

    @Test
    fun `processEvent handles interleaved identity and configuration events`() {
        val identityEvent = Event.Builder(
            "Identity",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        val configEvent = Event.Builder(
            "Config",
            EventType.CONFIGURATION,
            EventSource.RESPONSE_CONTENT
        ).build()

        val otherEvent = Event.Builder(
            "Other",
            EventType.ANALYTICS,
            EventSource.REQUEST_CONTENT
        ).build()

        val mockConfigState: SharedStateResult = mockk(relaxed = true)
        every {
            mockApi.getSharedState(
                ConciergeConstants.SharedState.Configuration.EXTENSION_NAME,
                configEvent,
                false,
                SharedStateResolution.LAST_SET
            )
        } returns mockConfigState

        extension.processEvent(identityEvent)
        extension.processEvent(otherEvent)
        extension.processEvent(configEvent)
        extension.processEvent(otherEvent)
        extension.processEvent(identityEvent)

        verify(exactly = 2) { mockStateRepository.updateExperienceCloudId(mockApi, identityEvent) }
        verify(exactly = 1) { mockStateRepository.updateConfiguration(mockConfigState) }
    }
}

