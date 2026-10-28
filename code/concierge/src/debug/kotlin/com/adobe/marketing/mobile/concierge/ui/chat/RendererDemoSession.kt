/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.ViewModelStore
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffEvent
import com.adobe.marketing.mobile.concierge.ConciergeSessionManager
import com.adobe.marketing.mobile.concierge.ConciergeStateRepository
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.cards.NativeCardController
import com.adobe.marketing.mobile.concierge.network.ConversationResponseParser
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCaptureListener
import com.adobe.marketing.mobile.concierge.ui.stt.SpeechCapturing
import com.adobe.marketing.mobile.concierge.utils.image.ImageProvider
import com.adobe.marketing.mobile.services.NamedCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

internal data class RendererDemoObservation(
    val running: Boolean = false,
    val callbacks: Int = 0,
    val result: DataHandoffDeliveryResult? = null,
    val parserMessages: Int = 0,
    val rawCandidates: Int = 0
)

/** One owned, disposable sandbox; neither the global registry nor global identity state is changed. */
internal class RendererDemoSession(
    application: Application,
    renderers: Map<String, ConciergeCardRenderer>,
    val scenario: RendererDemoScenario
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store = ViewModelStore()
    private val sessions = ConciergeSessionManager(DemoMemoryCollection())
    val observation = MutableStateFlow(RendererDemoObservation())
    val images = object : ImageProvider {
        override fun getCached(url: String): Bitmap? = null
        override suspend fun get(url: String): Bitmap = throw IOException("Demo image loading is disabled")
        override fun clear() = Unit
    }
    val controller = NativeCardController(
        scope = scope,
        rendererLookup = { renderers[it] },
        anyRendererRegistered = { renderers.isNotEmpty() }
    )
    private val service = object : ConversationService {
        override fun chat(message: String, xdmFields: Map<String, Any>, sessionId: String?) = responses()
        override fun sendDataHandoff(routingHint: String, xdmFields: Map<String, Any>, sessionId: String?) = responses()
        private fun responses(): Flow<ParsedConversationMessage> = flow {
            for (wire in scenario.frames()) {
                val parsed = ConversationResponseParser.parseConversationData(wire)
                observation.value = observation.value.let {
                    it.copy(
                        parserMessages = it.parserMessages + parsed.size,
                        rawCandidates = it.rawCandidates + parsed.sumOf { message -> message.rawElements.size }
                    )
                }
                for (message in parsed) emit(message)
            }
            if (scenario.throwAfterFrames) throw IOException("Synthetic offline failure after content")
        }
        override suspend fun sendFeedback(feedback: Feedback) = false
        override fun cleanup() = Unit
    }
    val viewModel = ConciergeChatViewModel(
        application = application,
        speechCapturing = object : SpeechCapturing {
            override fun isAvailable() = false
            override fun setListener(listener: SpeechCaptureListener?) = Unit
            override fun startCapture() = Unit
            override fun endCapture() = Unit
            override fun release() = Unit
        },
        imageProvider = images,
        chatService = service,
        stateRepository = ConciergeStateRepository(sessionManager = sessions, resetWarningScope = scope),
        sessionManager = sessions,
        sessionDispatcher = Dispatchers.Main.immediate,
        dispatch = {},
        nativeCardController = controller
    ).also {
        store.put("renderer-demo", it)
        it.activateDataHandoffSession()
    }
    private var turn = 0
    private var closed = false

    fun run() {
        if (closed || observation.value.running) return
        turn++
        observation.value = RendererDemoObservation(running = true)
        viewModel.enqueueDataHandoff(
            ConciergeDataHandoffEvent(
                routingHint = scenario.id,
                xdmFields = emptyMap(),
                localMessage = "Offline fixture: ${scenario.title} · turn $turn"
            )
        ) { result ->
            observation.value = observation.value.let {
                it.copy(running = false, callbacks = it.callbacks + 1, result = result)
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        store.clear()
        scope.cancel()
    }
}

internal fun demoRoutes(messages: List<ChatMessage>): List<DemoRoute> =
    messages.mapNotNull { message ->
        // Local notes are agent Text rows too; they must never count as a successful response.
        if (message.text.startsWith("Offline fixture:")) return@mapNotNull null
        when (message.content) {
            is MessageContent.NativeCard -> DemoRoute.NATIVE
            is MessageContent.Mixed -> DemoRoute.BUILT_IN
            is MessageContent.CtaButton -> DemoRoute.CTA
            is MessageContent.Text -> if (message.text.isNotBlank()) DemoRoute.TEXT else null
        }
    }

private class DemoMemoryCollection : NamedCollection {
    private val values = mutableMapOf<String, Any?>()
    override fun setInt(key: String, value: Int) { values[key] = value }
    override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue
    override fun setString(key: String, value: String?) { values[key] = value }
    override fun getString(key: String, defaultValue: String?) = values[key] as? String ?: defaultValue
    override fun setDouble(key: String, value: Double) { values[key] = value }
    override fun getDouble(key: String, defaultValue: Double) = values[key] as? Double ?: defaultValue
    override fun setLong(key: String, value: Long) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long) = values[key] as? Long ?: defaultValue
    override fun setFloat(key: String, value: Float) { values[key] = value }
    override fun getFloat(key: String, defaultValue: Float) = values[key] as? Float ?: defaultValue
    override fun setBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue
    override fun setMap(key: String, value: MutableMap<String, String>?) { values[key] = value?.toMap() }
    @Suppress("UNCHECKED_CAST")
    override fun getMap(key: String): MutableMap<String, String>? = (values[key] as? Map<String, String>)?.toMutableMap()
    override fun contains(key: String) = values.containsKey(key)
    override fun remove(key: String) { values.remove(key) }
    override fun removeAll() = values.clear()
}
