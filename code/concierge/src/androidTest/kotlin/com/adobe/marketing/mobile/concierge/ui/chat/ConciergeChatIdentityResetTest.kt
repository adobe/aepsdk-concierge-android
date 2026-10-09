/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.adobe.marketing.mobile.*
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ConciergeChatIdentityResetTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val repository = ConciergeStateRepository.instance
    private val events = mutableListOf<Event>()
    private val store = ViewModelStore()
    private val api = IdentityApi()
    private val delayedParticipant = Any()
    private lateinit var scenario: ActivityScenario<ComponentActivity>
    private lateinit var vm: ConciergeChatViewModel
    private lateinit var request: Event
    private var originalAccessibilityFlags = 0

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        MobileCore.setApplication(application)
        automation.serviceInfo = automation.serviceInfo.apply {
            originalAccessibilityFlags = flags
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        scenario = ActivityScenario.launch(ComponentActivity::class.java)
        scenario.onActivity { activity ->
            repository.clear()
            repository.updateConfiguration(SharedStateResult(SharedStateStatus.SET, mapOf(
                "concierge.server" to "https://example.com",
                "concierge.configId" to "config"
            )))
            repository.updateIdentity(api, Event.Builder("Identity", EventType.HUB, EventSource.SHARED_STATE).build())
            vm = ConciergeChatViewModel(
                application, SilentSpeech(), DefaultImageProvider(), IdleService(),
                dispatch = events::add
            )
            store.put("chat", vm)
            activity.setContent {
                ConciergeTheme {
                    ConciergeChat(viewModel = vm, surfaces = listOf("web://example.com")) { show ->
                        Button(onClick = show) { Text("Open test chat") }
                    }
                }
            }
        }
        await("rendered production host trigger") { nativeNode { it.text?.toString() == "Open test chat" } != null }
    }

    @After
    fun cleanup() {
        if (::scenario.isInitialized) {
            scenario.onActivity {
                it.setContent {}
                store.clear()
                repository.unregisterResetParticipant(delayedParticipant)
                repository.clear()
            }
            scenario.close()
        }
        automation.serviceInfo = automation.serviceInfo.apply { flags = originalAccessibilityFlags }
    }

    private fun beginReset() {
        scenario.onActivity {
            repository.registerResetParticipant(delayedParticipant)
            request = Event.Builder("Reset", EventType.GENERIC_IDENTITY, EventSource.REQUEST_RESET).build()
            repository.beginIdentityReset(request) { }
        }
    }

    private fun completeReset() {
        scenario.onActivity {
            api.ecid = "fresh-ecid"
            repository.completeIdentityReset(api, Event.Builder(
                "Complete", EventType.EDGE_IDENTITY, EventSource.RESET_COMPLETE
            ).inResponseToEvent(request).build())
        }
    }

    private fun releaseDelayedParticipant() {
        scenario.onActivity {
            repository.acknowledgeIdentityReset(
                delayedParticipant, repository.state.value.resetGeneration, false, null, false
            )
            repository.unregisterResetParticipant(delayedParticipant)
        }
    }

    private fun setConfigurationAvailable(available: Boolean) {
        scenario.onActivity {
            repository.updateConfiguration(if (available) SharedStateResult(SharedStateStatus.SET, mapOf(
                "concierge.server" to "https://example.com",
                "concierge.configId" to "config"
            )) else null)
        }
    }

    private fun setIdentityAvailable(available: Boolean) {
        scenario.onActivity {
            api.hasIdentity = available
            repository.updateIdentity(api, Event.Builder(
                "Identity publication", EventType.HUB, EventSource.SHARED_STATE
            ).build())
        }
    }

    private fun dialogRoots(): List<View> {
        var roots = emptyList<View>()
        scenario.onActivity { activity ->
            roots = WindowInspector.getGlobalWindowViews().filter {
                it !== activity.window.decorView && it.isAttachedToWindow && it.windowVisibility == View.VISIBLE
            }
        }
        return roots
    }

    private data class Composer(val windowId: Int, val node: AccessibilityNodeInfo)

    private fun composer(): Composer? {
        for (window in automation.windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() != instrumentation.targetContext.packageName) continue
            val node = findNode(root) { it.className?.toString() == "android.widget.EditText" } ?: continue
            if (!node.refresh()) continue
            return Composer(window.id, node)
        }
        return null
    }

    private fun nativeNode(predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        for (window in automation.windows) {
            val root = window.root ?: continue
            if (root.packageName?.toString() != instrumentation.targetContext.packageName) continue
            findNode(root, predicate)?.let { return it }
        }
        return null
    }

    private fun findNode(root: AccessibilityNodeInfo, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo? {
        if (predicate(root)) return root
        for (index in 0 until root.childCount) {
            val child = root.getChild(index) ?: continue
            findNode(child, predicate)?.let { return it }
        }
        return null
    }

    private fun clickNative(predicate: (AccessibilityNodeInfo) -> Boolean) {
        var node: AccessibilityNodeInfo? = requireNotNull(nativeNode(predicate))
        while (node != null && node.actionList.none { it.id == AccessibilityNodeInfo.ACTION_CLICK }) {
            node = node.parent
        }
        val clickable = requireNotNull(node) { "No clickable native accessibility ancestor" }
        assertTrue("Native click rejected: $clickable", clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK))
    }

    private fun await(description: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        do {
            instrumentation.waitForIdleSync()
            if (condition()) return
            SystemClock.sleep(25)
        } while (SystemClock.elapsedRealtime() < deadline)
        fail("Timed out waiting for $description; dialog roots=${dialogRoots().size}, composer=${composer()?.node}")
    }

    private fun assertTracking(opened: Int, closed: Int) {
        scenario.onActivity {
            assertEquals(opened, events.count { it.name == ConciergeConstants.TrackingEvent.Name.CHAT_OPENED })
            assertEquals(closed, events.count { it.name == ConciergeConstants.TrackingEvent.Name.CHAT_CLOSED })
        }
    }

    private fun assertRetainedDisabledWindow(root: View, windowId: Int) {
        assertSame(root, dialogRoots().single())
        val field = requireNotNull(composer())
        assertEquals(windowId, field.windowId)
        assertFalse(field.node.isEnabled)
        assertTrue(field.node.actionList.none { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT })
        assertEquals("", field.node.text?.toString().orEmpty())
        assertTracking(1, 0)
    }

    @Test
    fun mountedDialogKeepsWindowAndTrackingAcrossIdentityReset() {
        clickNative { it.text?.toString() == "Open test chat" }
        await("mounted production dialog and enabled composer") {
            dialogRoots().size == 1 && composer()?.node?.isEnabled == true
        }
        val originalRoot = dialogRoots().single()
        val originalWindowId = requireNotNull(composer()).windowId
        val draft = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "old draft")
        }
        assertTrue(requireNotNull(composer()).node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, draft))
        await("draft rendered in actual composer") { composer()?.node?.text?.toString() == "old draft" }
        assertTracking(1, 0)
        beginReset()
        await("composer disabled and old draft cleared after identity reset") {
            composer()?.node?.let { !it.isEnabled && it.text?.toString().orEmpty().isEmpty() } == true
        }
        assertRetainedDisabledWindow(originalRoot, originalWindowId)
        SystemClock.sleep(300)
        assertRetainedDisabledWindow(originalRoot, originalWindowId)
        completeReset()
        await("resolved identity while delayed participant retains admission gate") {
            repository.state.value.experienceCloudId == "fresh-ecid" && repository.state.value.resetInProgress
        }
        assertRetainedDisabledWindow(originalRoot, originalWindowId)
        releaseDelayedParticipant()
        await("reenabled production composer") { composer()?.node?.isEnabled == true }
        assertEquals("", requireNotNull(composer()).node.text?.toString().orEmpty())
        assertSame(originalRoot, dialogRoots().single())
        assertEquals(originalWindowId, requireNotNull(composer()).windowId)
        val freshDraft = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "fresh draft")
        }
        assertTrue(requireNotNull(composer()).node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, freshDraft))
        await("reenabled composer accepts fresh native text input") {
            composer()?.node?.text?.toString() == "fresh draft"
        }
        assertTracking(1, 0)
        clickNative { it.contentDescription?.toString() == "Close chat" }
        await("explicitly dismissed dialog") { dialogRoots().isEmpty() && composer() == null }
        assertTracking(1, 1)
    }

    @Test
    fun hiddenDirectOpenDuringResetCannotCreateOrDeferDialog() {
        assertTrue(dialogRoots().isEmpty())
        beginReset()
        await("identity reset boundary") { repository.state.value.experienceCloudId == null }
        scenario.onActivity {
            vm.openConcierge()
            assertFalse(vm.isConciergeActive.value)
        }
        instrumentation.waitForIdleSync()
        assertTrue(dialogRoots().isEmpty())
        completeReset()
        await("resolved identity while delayed participant keeps reset pending") {
            repository.state.value.experienceCloudId == "fresh-ecid" && repository.state.value.resetInProgress
        }
        scenario.onActivity {
            vm.openConcierge()
            assertFalse(vm.isConciergeActive.value)
        }
        instrumentation.waitForIdleSync()
        assertTrue(dialogRoots().isEmpty())
        assertTracking(0, 0)
        releaseDelayedParticipant()
        await("ready host trigger without an automatically opened dialog") {
            !repository.state.value.resetInProgress &&
                nativeNode { it.text?.toString() == "Open test chat" } != null
        }
        assertTrue(dialogRoots().isEmpty())
        assertTracking(0, 0)
    }

    @Test
    fun coldRequestedOpenWaitsForConfigurationAndIdentityBeforeFirstPresentation() {
        setConfigurationAvailable(false)
        setIdentityAvailable(false)
        scenario.onActivity {
            vm.openConcierge()
            assertTrue(vm.isConciergeActive.value)
        }
        await("cold requested host stays unavailable") {
            nativeNode { it.text?.toString() == "Open test chat" } == null
        }
        assertTrue(dialogRoots().isEmpty())
        assertTrue(composer() == null)
        assertTracking(0, 0)
        setConfigurationAvailable(true)
        instrumentation.waitForIdleSync()
        assertTrue(dialogRoots().isEmpty())
        assertTracking(0, 0)
        setIdentityAvailable(true)
        await("retained cold request mounts once initial readiness is satisfied") {
            dialogRoots().size == 1 && composer()?.node?.isEnabled == true
        }
        assertTracking(1, 0)
        clickNative { it.contentDescription?.toString() == "Close chat" }
        await("explicitly closed first presentation") { dialogRoots().isEmpty() }
        assertTracking(1, 1)
    }

    @Test
    fun ordinaryConfigurationAndIdentityLossStillRemovePresentedDialog() {
        clickNative { it.text?.toString() == "Open test chat" }
        await("initial ready dialog") { dialogRoots().size == 1 && composer()?.node?.isEnabled == true }
        val initialRoot = dialogRoots().single()
        assertTracking(1, 0)
        setConfigurationAvailable(false)
        await("ordinary configuration loss removes dialog") { dialogRoots().isEmpty() && composer() == null }
        assertTracking(1, 1)
        scenario.onActivity { assertTrue(vm.isConciergeActive.value) }
        setConfigurationAvailable(true)
        await("ordinary restored configuration presents retained request") {
            dialogRoots().size == 1 && composer()?.node?.isEnabled == true
        }
        assertNotSame(initialRoot, dialogRoots().single())
        assertTracking(2, 1)
        setIdentityAvailable(false)
        await("ordinary identity loss removes dialog") { dialogRoots().isEmpty() && composer() == null }
        assertTracking(2, 2)
        setIdentityAvailable(true)
        await("ordinary restored identity presents retained request") {
            dialogRoots().size == 1 && composer()?.node?.isEnabled == true
        }
        assertTracking(3, 2)
        clickNative { it.contentDescription?.toString() == "Close chat" }
        await("explicitly closed restored presentation") { dialogRoots().isEmpty() }
        assertTracking(3, 3)
    }

    private class SilentSpeech : SpeechCapturing {
        override fun isAvailable() = false
        override fun setListener(listener: SpeechCaptureListener?) {}
        override fun startCapture() {}
        override fun endCapture() {}
        override fun release() {}
    }

    private class IdleService : ConversationService {
        override fun chat(message: String, xdmFields: Map<String, Any>, sessionId: String?): Flow<ParsedConversationMessage> = emptyFlow()
        override fun sendDataHandoff(routingHint: String, xdmFields: Map<String, Any>, sessionId: String?): Flow<ParsedConversationMessage> = emptyFlow()
        override suspend fun sendFeedback(feedback: Feedback) = true
        override fun cleanup() {}
    }

    private class IdentityApi : ExtensionApi() {
        var ecid = "initial-ecid"
        var hasIdentity = true
        override fun getXDMSharedState(name: String, event: Event?, barrier: Boolean, resolution: SharedStateResolution) =
            SharedStateResult(SharedStateStatus.SET, mapOf("identityMap" to
                if (hasIdentity) mapOf("ECID" to listOf(mapOf("id" to ecid))) else emptyMap<String, Any>()))
        override fun registerEventListener(type: String, source: String, listener: ExtensionEventListener) {}
        override fun dispatch(event: Event) {}
        override fun startEvents() {}
        override fun stopEvents() {}
        override fun createSharedState(data: Map<String, Any>, event: Event?) {}
        override fun createPendingSharedState(event: Event?): SharedStateResolver? = null
        override fun getSharedState(name: String, event: Event?, barrier: Boolean, resolution: SharedStateResolution): SharedStateResult? = null
        override fun createXDMSharedState(data: Map<String, Any>, event: Event?) {}
        override fun createPendingXDMSharedState(event: Event?): SharedStateResolver? = null
        override fun unregisterExtension() {}
        override fun getHistoricalEvents(requests: Array<EventHistoryRequest>, enforceOrder: Boolean, handler: EventHistoryResultHandler<Int>) {}
        override fun getHistoricalEvents(requests: Array<EventHistoryRequest>, enforceOrder: Boolean, callback: AdobeCallbackWithError<Array<EventHistoryResult>>) {}
        override fun recordHistoricalEvent(event: Event, callback: AdobeCallbackWithError<Boolean>) {}
    }
}
