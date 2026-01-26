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
import com.adobe.marketing.mobile.ExtensionApi
import com.adobe.marketing.mobile.SharedStateResolution
import com.adobe.marketing.mobile.SharedStateResult
import com.adobe.marketing.mobile.SharedStateStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

