/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.cards.ConciergeCardRenderers
import com.adobe.marketing.mobile.concierge.cards.NativeCardController.ContextKeys
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.services.ServiceProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

internal abstract class RendererDemoTestBase {
    protected val dispatcher = StandardTestDispatcher()
    protected lateinit var application: Application

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        application = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_DENIED
        mockkStatic(ServiceProvider::class)
        every { ServiceProvider.getInstance() } returns mockk(relaxed = true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
        unmockkStatic(ServiceProvider::class)
        Dispatchers.resetMain()
    }

    protected fun session(scenario: RendererDemoScenario): RendererDemoSession {
        val noOp: ConciergeCardRenderer = { _, _, _ -> }
        return RendererDemoSession(application, scenario.registrations.associate { it.first to noOp }, scenario)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
internal class RendererDemoScenariosTest(private val scenario: RendererDemoScenario) : RendererDemoTestBase() {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun scenarios(): List<Array<Any>> = RendererDemoScenarios.all.map { arrayOf<Any>(it) }
    }

    @Test
    fun fixtureTraversesParserControllerAndRealViewModel() = runTest(dispatcher) {
        session(scenario).use { sandbox ->
            sandbox.run()
            advanceUntilIdle()

            val observation = sandbox.observation.value
            assertFalse("Must not remain busy: ${scenario.id}", observation.running)
            assertEquals("Exactly one delivery callback: ${scenario.id}", 1, observation.callbacks)
            val actualDelivery = when (val result = observation.result) {
                DataHandoffDeliveryResult.Delivered -> DemoDelivery.DELIVERED.name
                is DataHandoffDeliveryResult.Failed -> result.reason.name
                null -> "MISSING CALLBACK"
            }
            assertEquals("Delivery: ${scenario.id}", scenario.delivery.name, actualDelivery)
            assertEquals("Routing: ${scenario.id}", scenario.routes, demoRoutes(sandbox.viewModel.messages.value))
            assertEquals(
                "Only the local note and resolved response rows remain: ${scenario.id}",
                scenario.routes.size + 1,
                sandbox.viewModel.messages.value.size
            )
            assertFalse("Controller must leave busy state", sandbox.controller.isBusy.value)
            assertEquals(
                "Local note survives completion/rollback",
                1,
                sandbox.viewModel.messages.value.count { it.text.startsWith("Offline fixture:") }
            )
            sandbox.viewModel.messages.value.forEach { message ->
                (message.content as? MessageContent.NativeCard)?.let {
                    assertNotNull(sandbox.controller.renderableCard(it.cardId))
                }
            }
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class RendererDemoLifecycleTest : RendererDemoTestBase() {
    @Test
    fun repeatStateStaleDisposeAndClearAreIsolated() = runTest(dispatcher) {
        val scenario = RendererDemoScenarios.all.single { it.id == "lifecycle" }
        val registeredBefore = ConciergeCardRenderers.rendererFor("productCard")
        val anyRegisteredBefore = ConciergeCardRenderers.hasAnyRenderer
        lateinit var oldSave: (Map<String, Any?>) -> Unit
        session(scenario).use { sandbox ->
            sandbox.run()
            advanceUntilIdle()
            fun cards() = sandbox.viewModel.messages.value.mapNotNull { (it.content as? MessageContent.NativeCard)?.cardId }
            val first = sandbox.controller.renderableCard(cards().single())!!
            oldSave = first.saveState
            first.saveState(mapOf("taps" to 3, "optional" to null))
            advanceUntilIdle()
            assertEquals(3, first.state.value?.get("taps"))
            assertTrue(first.state.value!!.containsKey("optional"))
            first.saveState(mapOf("taps" to Double.NaN))
            advanceUntilIdle()
            assertEquals(3, first.state.value?.get("taps"))
            sandbox.run()
            advanceUntilIdle()
            val second = sandbox.controller.renderableCard(cards().last())!!
            assertNotEquals(first.cardId, second.cardId)
            assertEquals(3, first.state.value?.get("taps"))
            assertNull(second.state.value)
            assertEquals(1, sandbox.observation.value.callbacks)
            assertEquals(false, first.rendererContext(false, false, first.state.value)[ContextKeys.IS_BUSY])
            sandbox.controller.markAllStale()
            assertTrue(first.isStale.value)
            assertTrue(second.isStale.value)
            sandbox.controller.disposeAll()
            cards().forEach { assertNull(sandbox.controller.renderableCard(it)) }
            oldSave(mapOf("taps" to 999))
            advanceUntilIdle()
            assertEquals(3, first.state.value?.get("taps"))
        }
        session(scenario).use { fresh ->
            assertTrue(fresh.viewModel.messages.value.isEmpty())
            oldSave(mapOf("taps" to 999))
            fresh.run()
            advanceUntilIdle()
            val id = (fresh.viewModel.messages.value.last().content as MessageContent.NativeCard).cardId
            assertNull(fresh.controller.renderableCard(id)!!.state.value)
        }
        assertSame(registeredBefore, ConciergeCardRenderers.rendererFor("productCard"))
        assertEquals(anyRegisteredBefore, ConciergeCardRenderers.hasAnyRenderer)
    }

    @Test
    fun fullJsonSurvivesActualParserAndActivation() = runTest(dispatcher) {
        session(RendererDemoScenarios.all.single { it.id == "json" }).use { sandbox ->
            sandbox.run()
            advanceUntilIdle()
            val id = (sandbox.viewModel.messages.value.last().content as MessageContent.NativeCard).cardId
            val element = sandbox.controller.renderableCard(id)!!.element
            assertFalse(element.containsKey("id"))
            val unknown = element["unknown"] as Map<*, *>
            assertEquals(listOf(null, true, false, 42, -7, 2.5, "", "日本語 👟"), unknown["array"])
            val entity = (element["entity_info"] as List<*>).single() as Map<*, *>
            val product = entity["product"] as Map<*, *>
            assertTrue(product.containsKey("extra"))
            assertNull(product["extra"])
        }
    }
}
