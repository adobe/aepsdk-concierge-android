/*
 * Copyright 2026 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.conciergetestapp

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionResult
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderContext
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderers
import com.adobe.marketing.mobile.util.JSONUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Debug-only end-to-end happy path: a chat-style transcript driven by [FnbFlowSimulator] in place
 * of BC/BCOS/tapin2. Menu (stage catalog as BCOS cards) -> modifiers -> ADD TO CART -> updated
 * cart -> remove / show more / checkout. "Show data" reveals the payload at each step.
 */
class FnbFlowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FnbFlowScreen() }
    }
}

private sealed interface FlowMessage {
    data class User(val text: String) : FlowMessage
    data class Agent(val text: String) : FlowMessage
    data class Elements(val key: String, val elements: List<FnbElement>) : FlowMessage
    data class Note(val text: String) : FlowMessage
    data class Payload(val title: String, val body: String) : FlowMessage
}

private const val FLOW_TAG = "FnbFlow"
private const val REPLY_DELAY_MS = 700L

@Composable
private fun FnbFlowScreen() {
    val context = LocalContext.current
    var run by rememberSaveable { mutableIntStateOf(0) }
    val catalogElements = remember { loadFlowElements(context, "fnb/bcos_catalog_stage.json") }
    val catalog = remember(catalogElements) { CatalogMenuMapper.map(catalogElements.map { it.asMap() }) }
    val theme = remember { ConciergeThemeLoader.load(context, "themeDemo.json") ?: ConciergeThemeLoader.default() }
    val imageProvider = remember { GalleryImageProvider() }

    ConciergeTheme(theme = theme) {
        CompositionLocalProvider(LocalImageProvider provides imageProvider) {
            if (catalog == null) {
                Text("Couldn't load bcos_catalog_stage.json", modifier = Modifier.safeDrawingPadding().padding(24.dp))
            } else {
                // Keyed on `run` so "Restart" resets the order, transcript, and widget state.
                androidx.compose.runtime.key(run) {
                    FlowTranscript(catalogElements, catalog, onRestart = { run++ })
                }
            }
        }
    }
}

@Composable
private fun FlowTranscript(catalogElements: List<FnbElement>, catalog: MenuUiModel, onRestart: () -> Unit) {
    val simulator = remember { FnbFlowSimulator(catalog) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showData by rememberSaveable { mutableStateOf(true) }
    var typing by remember { mutableStateOf(false) }
    var menuCount by remember { mutableIntStateOf(1) }
    val messages = remember {
        mutableStateListOf<FlowMessage>(
            FlowMessage.User("I'm in section 122. What can I get to eat?"),
            FlowMessage.Agent("${catalog.locationName} is the closest open stand to section 122. Here's its menu:"),
            FlowMessage.Payload(
                "BCOS catalog elements (${catalogElements.size - 1} catalogItemCard + cartBar), first two shown",
                catalogElements.take(2).joinToString("\n\n") { it.toJson() } + "\n\n… ${catalogElements.size - 2} more"
            ),
            FlowMessage.Elements("menu-0", catalogElements)
        )
    }

    fun reply(block: () -> Unit) {
        typing = true
        scope.launch {
            delay(REPLY_DELAY_MS)
            typing = false
            block()
        }
    }

    val handler = remember {
        FnbActionHandler { action, onResult ->
            when (action) {
                is FnbAction.SubmitCart -> {
                    val message = FnbPromptFormatter.format(action)
                    Log.d(FLOW_TAG, message)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.DETAILS_START).trim())
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        val result = simulator.submit(action)
                        messages += FlowMessage.Payload(
                            "BCOS -> tapin2 POST /v2/cart/add" + if (result.duplicate) " (duplicate submitId, skipped)" else "",
                            JSONObject(result.cartAddRequest).toString(2)
                        )
                        messages += FlowMessage.Agent("Added to your order. Here's your current cart:")
                        messages += FlowMessage.Payload("BCOS cartView element", result.cartView.toJson())
                        messages += FlowMessage.Elements("cart-${messages.size}", listOf(result.cartView))
                        messages += FlowMessage.Agent("Add a drink or dessert, or are you ready to check out?")
                    }
                }
                is FnbAction.RemoveCartItem -> {
                    val message = FnbPromptFormatter.formatRemove(action)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.CART_ACTION_START).trim())
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        val removed = simulator.remove(action.lineId)
                        val cart = simulator.cartView()
                        messages += FlowMessage.Payload("BCOS -> tapin2 remove line ${action.lineId} (schema pending)", "{\"orderId\": ${action.cartId}, \"itemId\": ${action.lineId}}")
                        messages += FlowMessage.Agent(if (removed != null) "Removed $removed. Here's your updated cart:" else "That item was already removed. Here's your cart:")
                        messages += FlowMessage.Payload("BCOS cartView element", cart.toJson())
                        messages += FlowMessage.Elements("cart-${messages.size}", listOf(cart))
                    }
                }
                is FnbAction.ShowMoreRestaurants -> {
                    val message = FnbPromptFormatter.formatShowMore(action)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.CART_ACTION_START).trim())
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        messages += FlowMessage.Agent(
                            "The stage data has one stand near section 122, so here's ${catalog.locationName} again. " +
                                "Your order #${action.cartId} is kept."
                        )
                        messages += FlowMessage.Elements("menu-${menuCount++}", catalogElements)
                    }
                }
                is FnbAction.Checkout -> {
                    // Checkout sends no chat turn; the host opens the URL (a Custom Tab in production).
                    messages += FlowMessage.Note("Host opens checkout (not launched here: the simulated guid isn't a real order):\n${action.checkoutUrl}")
                    onResult(FnbActionResult.Accepted)
                }
            }
        }
    }

    LaunchedEffect(messages.size, typing) {
        // Land on the newest widget (e.g. the updated cart) when the latest reply contains one;
        // otherwise follow the newest message. +1 skips the header item.
        val lastElements = messages.indexOfLast { it is FlowMessage.Elements }
        val lastUser = messages.indexOfLast { it is FlowMessage.User }
        val target = when {
            typing -> messages.size + 1
            lastElements > lastUser -> lastElements + 1
            else -> messages.lastIndex + 1
        }
        listState.animateScrollToItem(target)
    }

    val lastMenuIndex = messages.indexOfLast { it is FlowMessage.Elements && it.elements.any { e -> e.type == "catalogItemCard" } }
    val lastCartIndex = messages.indexOfLast { it is FlowMessage.Elements && it.elements.any { e -> e.type == "cartView" } }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(ConciergeTheme.colors.background)
            .safeDrawingPadding()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("F&B end-to-end flow (simulated)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ConciergeTheme.colors.onSurface)
                Text(
                    "Stage menu as BCOS cards. A fake BC/tapin2 prices the order and returns BCOS cartView elements.",
                    fontSize = 12.sp,
                    color = ConciergeTheme.colors.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = showData, onClick = { showData = !showData }, label = { Text("Show data") })
                    TextButton(onClick = onRestart) { Text("Restart") }
                    simulator.orderId?.let { Text("Order #$it", fontSize = 12.sp, color = ConciergeTheme.colors.onSurfaceVariant) }
                }
            }
        }
        itemsIndexed(messages) { index, message ->
            when (message) {
                is FlowMessage.User -> Bubble(message.text, fromUser = true)
                is FlowMessage.Agent -> Bubble(message.text, fromUser = false)
                is FlowMessage.Note -> Text(
                    message.text,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = ConciergeTheme.colors.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                is FlowMessage.Payload -> if (showData) PayloadBlock(message.title, message.body)
                is FlowMessage.Elements -> {
                    val interactive = index == lastMenuIndex || index == lastCartIndex
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FnbRenderers.group(message.elements).forEachIndexed { groupIndex, (renderer, group) ->
                            renderer.Content(FnbRenderContext("${message.key}-$groupIndex", group, interactive), handler, Modifier)
                        }
                    }
                }
            }
        }
        if (typing) {
            item { Bubble("Concierge is typing…", fromUser = false) }
        }
        item { Spacer(Modifier.padding(24.dp)) }
    }
}

@Composable
private fun Bubble(text: String, fromUser: Boolean) {
    val colors = ConciergeTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .widthIn(max = 320.dp)
                .background(
                    if (fromUser) colors.userMessageBackground ?: colors.primary else colors.conciergeMessageBackground ?: colors.container,
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text,
                fontSize = 14.sp,
                color = if (fromUser) colors.userMessageText ?: colors.onPrimary else colors.conciergeMessageText ?: colors.onSurface
            )
        }
    }
}

@Composable
private fun PayloadBlock(title: String, body: String) {
    val colors = ConciergeTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.container, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Text(body, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = colors.onSurfaceVariant)
    }
}

private fun FnbElement.toJson(): String = JSONObject(asMap()).toString(2)

@Suppress("UNCHECKED_CAST")
private fun loadFlowElements(context: Context, assetPath: String): List<FnbElement> = try {
    val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    val root = JSONUtils.toMap(JSONObject(json)).orEmpty()
    ((root["multimodalElements"] as? Map<String, Any?>)?.get("elements") as? List<*>).orEmpty()
        .mapNotNull { (it as? Map<String, Any?>)?.let(FnbElement::fromMap) }
} catch (e: Exception) {
    Log.w(FLOW_TAG, "Failed to load $assetPath", e)
    emptyList()
}
