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
import com.adobe.marketing.mobile.concierge.ui.components.card.DemoMultimodalCards
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbLinks
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionResult
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.CartFnbRenderer
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderContext
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererIds
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRendererRegistry
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuFnbRenderer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Debug-only end-to-end happy path: a chat-style transcript driven by [FnbFlowSimulator] and
 * [FnbBcosProjection] in place of BC/BCOS/tapin2. Stands near the section (SDK out-of-the-box
 * product-card carousel) -> pick a stand -> its menu (tapin2-shaped elements) -> modifiers ->
 * ADD TO CART -> updated cart (tapin2 order, across stands) -> remove / show more / checkout.
 * "Show data" reveals the payload at each step.
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
    /** Out-of-the-box SDK product cards (`multimodalElements` JSON), rendered by the SDK itself. */
    data class SdkCards(val key: String, val multimodalElementsJson: String) : FlowMessage
    data class Note(val text: String) : FlowMessage
    data class Payload(val title: String, val body: String) : FlowMessage
}

private const val FLOW_TAG = "FnbFlow"

/** Demo widget options: notes on (tapin2 has `items[].note`), stage checkout host. */
private val FLOW_MENU_OPTIONS = MenuOptions(instructions = MenuOptions.NOTES_ENABLED)
private val FLOW_REGISTRY = FnbRendererRegistry(listOf(MenuFnbRenderer(FLOW_MENU_OPTIONS), CartFnbRenderer(CartOptions.STAGE)))
private const val REPLY_DELAY_MS = 700L

@Composable
private fun FnbFlowScreen() {
    val context = LocalContext.current
    var run by rememberSaveable { mutableIntStateOf(0) }
    val data = remember { loadFlowData(context) }
    val catalog = remember(data) { data?.let { CatalogMenuMapper.map(it.menuFor(it.homeLocation).payload, FLOW_MENU_OPTIONS) } }
    val theme = remember { ConciergeThemeLoader.load(context, "themeDemo.json") ?: ConciergeThemeLoader.default() }
    val imageProvider = remember { GalleryImageProvider() }

    ConciergeTheme(theme = theme) {
        CompositionLocalProvider(LocalImageProvider provides imageProvider) {
            if (data == null || catalog == null) {
                Text("Couldn't load the fnb/ stage fixtures", modifier = Modifier.safeDrawingPadding().padding(24.dp))
            } else {
                // Keyed on `run` so "Restart" resets the order, transcript, and widget state.
                androidx.compose.runtime.key(run) {
                    FlowTranscript(data, catalog, onRestart = { run++ })
                }
            }
        }
    }
}

@Composable
private fun FlowTranscript(data: FlowData, catalog: MenuUiModel, onRestart: () -> Unit) {
    val simulator = remember {
        FnbFlowSimulator(catalog, sessionVenueId = data.venueId, sessionEventId = data.eventId, locations = data.locations.associateBy { it.getLong("id") })
    }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showData by rememberSaveable { mutableStateOf(true) }
    var typing by remember { mutableStateOf(false) }
    var widgetCount by remember { mutableIntStateOf(0) }
    val messages = remember { mutableStateListOf<FlowMessage>(FlowMessage.User("I'm in section 122. What can I get to eat?")) }

    fun showLocations(lead: String) {
        val cards = FnbBcosProjection.locationCards(data.locations)
        messages += FlowMessage.Payload("tapin2 get_section_locations (response: ${data.locations.size} locations)", JSONArray(data.locations).toString(2))
        messages += FlowMessage.Agent(lead)
        messages += FlowMessage.Payload("BCOS productCard elements (SDK out-of-the-box cards; no custom renderer)", cards.toString(2))
        messages += FlowMessage.SdkCards("locations-${widgetCount++}", cards.toString())
    }

    fun showMenu(location: JSONObject) {
        val entries = FnbBcosProjection.productsFor(location, data.stageEntries)
        val element = FnbBcosProjection.menuElement(entries, location.getLong("id"))
        messages += FlowMessage.Payload(
            "tapin2 GET /v2/venues/${data.venueId}/locations/${location.getLong("id")}/products (${entries.size} entries; first shown)",
            entries.firstOrNull()?.toString(2).orEmpty()
        )
        messages += FlowMessage.Agent("Here's the menu at ${location.optString("title")}:")
        messages += FlowMessage.Payload(
            "BC element: {id, entityId, rendererId: \"${element.rendererId}\", entity_info: <the tapin2 response above, untouched>}",
            JSONObject().put("id", element.id).put("entityId", element.entityId).put("rendererId", element.rendererId)
                .put("entity_info", "[… ${entries.size} tapin2 entries …]").toString(2)
        )
        messages += FlowMessage.Elements("menu-${widgetCount++}", listOf(element))
    }

    LaunchedEffect(Unit) { if (messages.size == 1) showLocations("Here are the stands near section 122. Tap one to see its menu:") }

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
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn; ORDER_DETAILS = cart/add body)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        val result = simulator.submit(message)
                        messages += FlowMessage.Payload(
                            "BC -> tapin2 POST /v2/cart/add (submitId stripped; orderId, deliveryMethod added)" +
                                if (result.duplicate) " - duplicate submitId, skipped" else "",
                            result.cartAddRequest.toString(2)
                        )
                        messages += FlowMessage.Agent("Added to your order. Here's your current cart:")
                        messages += FlowMessage.Payload("BC element: fnb.cart (entity_info = the tapin2 cart/add order)", result.cartElement.toJson())
                        messages += FlowMessage.Elements("cart-${messages.size}", listOf(result.cartElement))
                        messages += FlowMessage.Agent("Add a drink or dessert, or are you ready to check out?")
                    }
                }
                is FnbAction.RemoveCartItem -> {
                    val message = FnbPromptFormatter.formatRemove(action)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.CART_ACTION_START).trim())
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        val removed = simulator.remove(message)
                        val cart = simulator.cartElement()
                        messages += FlowMessage.Payload("BC -> tapin2 remove order line (schema pending)", "{\"orderId\": ${action.orderId}, \"itemId\": ${action.itemId}}")
                        messages += FlowMessage.Agent(if (removed != null) "Removed $removed. Here's your updated cart:" else "That item was already removed. Here's your cart:")
                        messages += FlowMessage.Payload("BC element: fnb.cart (entity_info = the tapin2 order)", cart.toJson())
                        messages += FlowMessage.Elements("cart-${messages.size}", listOf(cart))
                    }
                }
                is FnbAction.ShowMoreRestaurants -> {
                    val message = FnbPromptFormatter.formatShowMore(action)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.CART_ACTION_START).trim())
                    messages += FlowMessage.Payload("Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply { showLocations("Here are the stands near section 122. Your order #${action.orderId} is kept.") }
                }
                is FnbAction.SelectLocation -> {
                    val message = FnbPromptFormatter.formatSelectLocation(action)
                    messages += FlowMessage.User(message.substringBefore(FnbPromptFormatter.CART_ACTION_START).trim())
                    messages += FlowMessage.Payload("handleLink -> FnbLinks -> Concierge.sendMessage (full user turn)", message)
                    onResult(FnbActionResult.Accepted)
                    reply {
                        val location = data.locations.firstOrNull { it.getLong("id").toString() == action.locationId }
                        if (location == null) messages += FlowMessage.Agent("I couldn't find that stand.") else showMenu(location)
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
        val lastElements = messages.indexOfLast { it is FlowMessage.Elements || it is FlowMessage.SdkCards }
        val lastUser = messages.indexOfLast { it is FlowMessage.User }
        val target = when {
            typing -> messages.size + 1
            lastElements > lastUser -> lastElements + 1
            else -> messages.lastIndex + 1
        }
        listState.animateScrollToItem(target)
    }

    val lastMenuIndex = messages.indexOfLast { it is FlowMessage.Elements && it.elements.any { e -> e.rendererId == FnbRendererIds.MENU } }
    val lastCartIndex = messages.indexOfLast { it is FlowMessage.Elements && it.elements.any { e -> e.rendererId == FnbRendererIds.CART } }
    val lastLocationsIndex = messages.indexOfLast { it is FlowMessage.SdkCards }

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
                    "Stands as SDK product cards, menus and cart as tapin2-shaped elements. A fake BC/tapin2 answers each turn.",
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
                is FlowMessage.SdkCards -> DemoMultimodalCards(
                    multimodalElementsJson = message.multimodalElementsJson,
                    // Exactly what a host's ConciergeChat(handleLink = …) does with card taps.
                    onLink = { url ->
                        if (index != lastLocationsIndex || !FnbLinks.handle(url, handler)) {
                            messages += FlowMessage.Note(if (index != lastLocationsIndex) "Older card: pick from the latest stands list." else "Not an F&B link: $url")
                        }
                    }
                )
                is FlowMessage.Elements -> {
                    val interactive = index == lastMenuIndex || index == lastCartIndex
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // What the SDK registry does per element: resolve by rendererId, run it on the payload.
                        message.elements.forEach { element ->
                            FLOW_REGISTRY.resolve(element)?.Content(FnbRenderContext("${message.key}-${element.id}", element, interactive), handler, Modifier)
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

/** Stage fixtures: tapin2 products (real stand 19289), dummy section locations, and request ids. */
private class FlowData(
    val stageEntries: List<JSONObject>,
    val locations: List<JSONObject>,
    val venueId: Long,
    val eventId: Long
) {
    val homeLocation: JSONObject get() = locations.first { it.getLong("id") == 19289L }

    fun menuFor(location: JSONObject): FnbElement =
        FnbBcosProjection.menuElement(FnbBcosProjection.productsFor(location, stageEntries), location.getLong("id"))
}

private fun loadFlowData(context: Context): FlowData? = try {
    fun read(path: String) = context.assets.open(path).bufferedReader().use { it.readText() }
    val order = JSONObject(read("fnb/tapin2_stage_cart_add.json"))
    FlowData(
        stageEntries = FnbBcosProjection.jsonList(JSONArray(read("fnb/tapin2_stage_raw.json"))),
        locations = FnbBcosProjection.jsonList(JSONArray(read("fnb/tapin2_section_locations_stage.json"))),
        venueId = order.getLong("venueId"),
        eventId = order.getLong("eventId")
    )
} catch (e: Exception) {
    Log.w(FLOW_TAG, "Failed to load flow fixtures", e)
    null
}
