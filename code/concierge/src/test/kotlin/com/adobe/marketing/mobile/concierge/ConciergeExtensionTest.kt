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

    // ========== Consent Event Detection Tests ==========

    @Test
    fun `isConsentSharedStateEvent returns true for consent shared state event`() {
        // Given
        val event = createConsentSharedStateEvent()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertTrue(result)
    }

    @Test
    fun `isConsentSharedStateEvent returns false for wrong event type`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.CONFIGURATION,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.Consent.EXTENSION_NAME)
        ).build()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertFalse(result)
    }

    @Test
    fun `isConsentSharedStateEvent returns false for wrong event source`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.RESPONSE_CONTENT
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.Consent.EXTENSION_NAME)
        ).build()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertFalse(result)
    }

    @Test
    fun `isConsentSharedStateEvent returns false for wrong stateowner`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to "com.adobe.wrong.extension")
        ).build()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertFalse(result)
    }

    @Test
    fun `isConsentSharedStateEvent returns false when stateowner is null`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertFalse(result)
    }

    @Test
    fun `isConsentSharedStateEvent returns false when event data is null`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).build()

        // When
        val result = extension.run { event.isConsentSharedStateEvent() }

        // Then
        assertFalse(result)
    }

    // ========== Identity Event Detection Tests ==========

    @Test
    fun `isIdentitySharedStateEvent returns true for identity shared state event`() {
        // Given
        val event = Event.Builder(
            "Test Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        // When
        val result = extension.run { event.isIdentitySharedStateEvent() }

        // Then
        assertTrue(result)
    }

    @Test
    fun `isIdentitySharedStateEvent returns false for consent event`() {
        // Given
        val event = createConsentSharedStateEvent()

        // When
        val result = extension.run { event.isIdentitySharedStateEvent() }

        // Then
        assertFalse(result)
    }

    // ========== Event Processing Tests ==========

    @Test
    fun `processEvent calls updateConsent for consent shared state event`() {
        // Given
        val event = createConsentSharedStateEvent()

        // When
        extension.processEvent(event)

        // Then
        verify(exactly = 1) { mockStateRepository.updateConsent(mockApi, event) }
    }

    @Test
    fun `processEvent calls updateExperienceCloudId for identity shared state event`() {
        // Given
        val event = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        // When
        extension.processEvent(event)

        // Then
        verify(exactly = 1) { mockStateRepository.updateExperienceCloudId(mockApi, event) }
    }

    @Test
    fun `processEvent calls updateConfiguration for configuration response event`() {
        // Given
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

        // When
        extension.processEvent(event)

        // Then
        verify(exactly = 1) { mockStateRepository.updateConfiguration(mockConfigState) }
    }

    @Test
    fun `processEvent handles multiple consent events sequentially`() {
        // Given
        val event1 = createConsentSharedStateEvent()
        val event2 = createConsentSharedStateEvent()
        val event3 = createConsentSharedStateEvent()

        // When
        extension.processEvent(event1)
        extension.processEvent(event2)
        extension.processEvent(event3)

        // Then
        verify(exactly = 3) { mockStateRepository.updateConsent(mockApi, any()) }
    }

    @Test
    fun `processEvent does not throw exception for unknown event type`() {
        // Given
        val event = Event.Builder(
            "Unknown Event",
            EventType.ANALYTICS,
            EventSource.REQUEST_CONTENT
        ).build()

        // When/Then - should not throw
        extension.processEvent(event)
    }

    @Test
    fun `processEvent prioritizes identity over consent when checking conditions`() {
        // Given - This tests the if-else chain order
        val identityEvent = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        // When
        extension.processEvent(identityEvent)

        // Then - identity handler is called, not consent
        verify(exactly = 1) { mockStateRepository.updateExperienceCloudId(mockApi, identityEvent) }
        verify(exactly = 0) { mockStateRepository.updateConsent(any(), any()) }
    }

    // ========== Integration Tests ==========

    @Test
    fun `consent event followed by identity event processes both correctly`() {
        // Given
        val consentEvent = createConsentSharedStateEvent()
        val identityEvent = Event.Builder(
            "Identity Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.EdgeIdentity.EXTENSION_NAME)
        ).build()

        // When
        extension.processEvent(consentEvent)
        extension.processEvent(identityEvent)

        // Then
        verify(exactly = 1) { mockStateRepository.updateConsent(mockApi, consentEvent) }
        verify(exactly = 1) { mockStateRepository.updateExperienceCloudId(mockApi, identityEvent) }
    }

    @Test
    fun `readyForEvent always returns true`() {
        // Given
        val event = createConsentSharedStateEvent()

        // When
        val result = extension.readyForEvent(event)

        // Then
        assertTrue(result)
    }

    // ========== Helper Methods ==========

    private fun createConsentSharedStateEvent(): Event {
        return Event.Builder(
            "Consent Shared State Event",
            EventType.HUB,
            EventSource.SHARED_STATE
        ).setEventData(
            mapOf("stateowner" to ConciergeConstants.SharedState.Consent.EXTENSION_NAME)
        ).build()
    }
}

