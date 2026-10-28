/*
 * Copyright 2026 Adobe. All rights reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy at http://www.apache.org/licenses/LICENSE-2.0
 */

package com.adobe.marketing.mobile.concierge.ui.chat

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.DataHandoffDeliveryResult
import com.adobe.marketing.mobile.concierge.cards.NativeCardController.ContextKeys
import com.adobe.marketing.mobile.concierge.ui.components.messages.LocalNativeCardController
import com.adobe.marketing.mobile.concierge.ui.components.messages.MessageList
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

/** Debug diagnostics inherit the host's Material theme; only the live transcript uses ConciergeTheme. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RendererEdgeCasesDemoScreen(onBack: () -> Unit) {
    val application = LocalContext.current.applicationContext as Application
    val scenarios = RendererDemoScenarios.all
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var generation by remember { mutableIntStateOf(0) }
    val scenario = scenarios[selected]
    val session = remember(scenario.id, generation) {
        val renderers = linkedMapOf<String, ConciergeCardRenderer>()
        scenario.registrations.forEach { (type, label) ->
            renderers[type] = { element, context, save -> DemoNativeRenderer(label, element, context, save) }
        }
        RendererDemoSession(application, renderers, scenario)
    }
    DisposableEffect(session) { onDispose { session.close() } }
    val messages by session.viewModel.messages.collectAsState()
    val observation by session.observation.collectAsState()
    val busy by session.controller.isBusy.collectAsState()
    var action by remember(session) { mutableStateOf("No interaction yet") }
    var discarded by remember(session) { mutableStateOf(false) }
    val observedRoutes = demoRoutes(messages)
    val result = when (val completion = observation.result) {
        null -> if (observation.running) "Running" else "Not run"
        DataHandoffDeliveryResult.Delivered -> "DELIVERED"
        is DataHandoffDeliveryResult.Failed -> completion.reason.name
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Renderer edge cases") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Offline · ${scenarios.size} static cases · no backend", style = MaterialTheme.typography.bodyMedium)
                Text("${selected + 1}/${scenarios.size} · ${scenario.group}", style = MaterialTheme.typography.labelLarge)
                Text(scenario.title, style = MaterialTheme.typography.titleMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selected--; generation = 0 },
                        enabled = selected > 0 && !observation.running
                    ) { Text("Previous") }
                    TextButton(
                        onClick = { selected++; generation = 0 },
                        enabled = selected < scenarios.lastIndex && !observation.running
                    ) { Text("Next") }
                    Button(
                        onClick = { session.run(); tab = 1 },
                        enabled = !observation.running && !discarded
                    ) { Text(if (observation.result == null) "Run" else "Repeat") }
                }
            }
            TabRow(selectedTabIndex = tab) {
                listOf("Cases", "Transcript", "Payload").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { tab = index }, text = { Text(label) })
                }
            }
            when (tab) {
                0 -> LazyColumn(Modifier.weight(1f)) {
                    item {
                        Text(
                            "Choose a fixture, then Run. Each selection starts an isolated session. " +
                                "Repeat keeps prior turns; Clear starts over. Expectations are not success assertions.",
                            Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    itemsIndexed(scenarios, key = { _, item -> item.id }) { index, item ->
                        if (index == 0 || item.group != scenarios[index - 1].group) {
                            Text(
                                item.group, Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        TextButton(
                            onClick = { selected = index; generation = 0; tab = 1 },
                            enabled = !observation.running,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = { Text("Expected: ${item.delivery} · ${item.routes.ifEmpty { listOf("no response rows") }}") },
                                trailingContent = { if (index == selected) Text("Selected") }
                            )
                        }
                        HorizontalDivider()
                    }
                }
                1 -> {
                    Column(
                        Modifier.fillMaxWidth().weight(0.48f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Expected", style = MaterialTheme.typography.titleSmall)
                        Text(scenario.expectation, style = MaterialTheme.typography.bodyMedium)
                        Text("${scenario.delivery} · routes ${scenario.routes}", style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider()
                        Text("Observed: $result", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Callbacks this run: ${observation.callbacks} · busy: $busy\n" +
                                "Parser messages: ${observation.parserMessages} · raw candidates: ${observation.rawCandidates}\n" +
                                "Transcript rows (all turns): ${messages.size} · response routes: $observedRoutes",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(action, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { generation++ }, enabled = !observation.running) { Text("Clear") }
                            OutlinedButton(
                                onClick = { session.controller.markAllStale(); action = "Locally marked stale; no identity event dispatched" },
                                enabled = !observation.running && !discarded && observedRoutes.contains(DemoRoute.NATIVE)
                            ) { Text("Mark stale") }
                        }
                        OutlinedButton(
                            onClick = {
                                session.controller.disposeAll()
                                discarded = true
                                action = "Controller discarded locally; transcript retained to exercise SDK unavailable fallback. Clear to run again."
                            },
                            enabled = !observation.running && !discarded && observedRoutes.contains(DemoRoute.NATIVE)
                        ) { Text("Discard controller (local simulation)") }
                        Text(
                            "Internal VM handoff boundary, not public API/Event Hub validation. " +
                                "Links, CTAs, suggestions, images and feedback are intercepted/no-op; speech is disabled. " +
                                "Throwing composables and unbounded stress are excluded: Compose renderer exceptions cannot be caught here.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    HorizontalDivider()
                    Box(Modifier.fillMaxWidth().weight(0.52f)) {
                        if (messages.isEmpty()) {
                            Text("No transcript yet. Tap Run.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            ConciergeTheme {
                                CompositionLocalProvider(
                                    LocalNativeCardController provides if (discarded) null else session.controller,
                                    LocalImageProvider provides session.images
                                ) {
                                    key(session, discarded) {
                                        MessageList(
                                            modifier = Modifier.fillMaxSize(),
                                            messages = messages,
                                            onActionClick = { action = "Product action intercepted locally: $it" },
                                            onImageClick = { action = "Image tap intercepted locally" },
                                            onSuggestionClick = { action = "Suggestion intercepted locally: $it" },
                                            handleLink = { url, _ -> action = "Link intercepted locally: $url" },
                                            onCtaButtonClick = { action = "CTA intercepted locally: ${it.label}" },
                                            onFeedback = { action = "Feedback intercepted locally; not submitted" }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> key(scenario.id) {
                    val payload = remember(scenario.id) {
                        scenario.frames().mapIndexed { index, wire ->
                            "Frame ${index + 1} · ${wire.length} characters\n" +
                                wire.take(12_000) +
                                if (wire.length > 12_000) "\n[Display truncated at 12,000 characters; entire fixture is parsed.]" else ""
                        }.joinToString("\n\n").ifEmpty { "[No frames: service completes without an emission.]" } +
                            if (scenario.throwAfterFrames) "\n\nThen throws synthetic IOException." else ""
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Exact wire fixture (bounded display)", style = MaterialTheme.typography.titleSmall)
                        Text("Private lookup registrations, in order: ${scenario.registrations}", style = MaterialTheme.typography.bodySmall)
                        SelectionContainer {
                            Text(payload, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoNativeRenderer(
    label: String,
    element: Map<String, Any?>,
    context: Map<String, Any?>,
    saveState: (Map<String, Any?>) -> Unit
) {
    val saved = context[ContextKeys.STATE] as? Map<*, *>
    val count = (saved?.get("taps") as? Number)?.toInt() ?: 0
    val stale = context[ContextKeys.IS_STALE] == true
    val busy = context[ContextKeys.IS_BUSY] == true
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                "Demo renderer placeholder (app-owned, not SDK schema validation)\n" +
                    "type=${element["type"]} · element id=${element["id"] ?: "(missing)"}\n" +
                    "entity_info: ${element["entity_info"]?.javaClass?.simpleName ?: "missing/null"}",
                style = MaterialTheme.typography.bodySmall
            )
            SelectionContainer {
                Text(
                    "cardId=${context[ContextKeys.CARD_ID]}\n" +
                        "session=${context[ContextKeys.SESSION_ID]}\n" +
                        "conversation=${context[ContextKeys.CONVERSATION_ID]}\n" +
                        "interaction=${context[ContextKeys.INTERACTION_ID]}\n" +
                        "busy=$busy · stale=$stale · saved taps=$count",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = { saveState(mapOf("taps" to count + 1, "optional" to null)) }, enabled = !stale && !busy) {
                Text("Save local state")
            }
            OutlinedButton(onClick = { saveState(mapOf("taps" to Double.NaN)) }, enabled = !stale && !busy) {
                Text("Try invalid saved state (NaN)")
            }
            Text(
                "Expected: SDK ignores non-finite saved state; the observed saved taps above should not change.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
