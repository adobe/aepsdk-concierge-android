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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.ConciergeFnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionResult
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbPromptFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.CustomizeContent
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.CartFnbRenderer
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbElement
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderContext
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuFnbRenderer
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbRenderers
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.FnbTheme
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuContent
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject

/**
 * Debug-only gallery for the F&B renderer widgets. Hosts them inside a LazyColumn to reproduce
 * the chat transcript's constraints, with no chat session, registry, or network ordering.
 */
class FnbGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FnbGalleryScreen() }
    }
}

private enum class GalleryTransport(val label: String) {
    ACCEPT("Log + accept"),
    REJECT_BUSY("Log + reject (busy)"),
    CONCIERGE("Concierge.sendMessage")
}

private const val TAG = "FnbGallery"

private val GALLERY_RENDERERS = listOf(
    MenuFnbRenderer(MenuOptions(instructions = MenuOptions.NOTES_ENABLED)),
    CartFnbRenderer(CartOptions.STAGE)
)

@Composable
private fun FnbGalleryScreen() {
    val context = LocalContext.current
    var themeFile by rememberSaveable { mutableStateOf("themeDemo.json") }
    var transport by rememberSaveable { mutableStateOf(GalleryTransport.ACCEPT) }
    var generation by rememberSaveable { mutableIntStateOf(0) }
    val log = remember { mutableStateListOf<String>() }
    val theme = remember(themeFile) { ConciergeThemeLoader.load(context, themeFile) ?: ConciergeThemeLoader.default() }
    val imageProvider = remember { GalleryImageProvider() }
    val catalogStage = remember { loadElements(context, "fnb/tapin2_catalog_elements_stage.json") }
    // Two cards (Veggie Nachos, Fountain Soda) + cartBar: a compact menu for the read-only demo.
    val catalogSample = remember(catalogStage) { catalogStage.filter { it.entityId in setOf("1364190", "1364015") || it.type == "cartBar" } }
    val cartStage = remember { loadElements(context, "fnb/tapin2_cart_view_stage.json") }
    val conciergeHandler = remember {
        ConciergeFnbActionHandler(openUrl = { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
        })
    }

    val handler = remember(transport) {
        FnbActionHandler { action, onResult ->
            val prompt = when (action) {
                is FnbAction.SubmitCart -> FnbPromptFormatter.format(action)
                is FnbAction.RemoveCartItem -> FnbPromptFormatter.formatRemove(action)
                is FnbAction.ShowMoreRestaurants -> FnbPromptFormatter.formatShowMore(action)
                is FnbAction.Checkout -> "Open checkout: ${action.checkoutUrl}"
            }
            Log.d(TAG, prompt)
            log.add(0, "[${transport.label}]\n$prompt")
            when (transport) {
                GalleryTransport.ACCEPT -> onResult(FnbActionResult.Accepted)
                GalleryTransport.REJECT_BUSY -> onResult(
                    FnbActionResult.Rejected("Please wait for the current reply, then try again.", retryable = true)
                )
                GalleryTransport.CONCIERGE -> conciergeHandler.onAction(action) { result ->
                    log.add(0, "Concierge.sendMessage -> $result")
                    onResult(result)
                }
            }
        }
    }

    ConciergeTheme(theme = theme) {
        CompositionLocalProvider(LocalImageProvider provides imageProvider) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(ConciergeTheme.colors.background)
                    .safeDrawingPadding()
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("F&B widget gallery", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ConciergeTheme.colors.onSurface)
                        Text(
                            "Placeholder styling. Figma tokens go in the --fnb-* theme keys.",
                            fontSize = 12.sp,
                            color = ConciergeTheme.colors.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("themeDemo.json", "themeDefault.json").forEach { file ->
                                FilterChip(selected = themeFile == file, onClick = { themeFile = file }, label = { Text(file) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GalleryTransport.values().forEach { option ->
                                FilterChip(
                                    selected = transport == option,
                                    onClick = { transport = option },
                                    label = { Text(option.label, fontSize = 11.sp) }
                                )
                            }
                        }
                        TextButton(onClick = { generation++; log.clear() }) { Text("Reset all widget state") }
                    }
                }

                item { SectionTitle("Menu: sample UI models (MenuContent)") }
                item {
                    MenuContent(
                        elementId = "sample-$generation",
                        model = FnbSampleData.menu,
                        isInteractive = true,
                        onAction = handler
                    )
                }

                item { SectionTitle("Menu: tapin2 stage products as elements (33 catalogItemCard + cartBar)") }
                item { ElementsGroup("catalog-stage-$generation", catalogStage, handler) }

                item { SectionTitle("Menu: location paused (sample models)") }
                item {
                    MenuContent(
                        elementId = "paused-$generation",
                        model = FnbSampleData.menu.copy(orderingAvailable = false),
                        isInteractive = true,
                        onAction = handler,
                        height = 320.dp
                    )
                }

                item { SectionTitle("Menu: read-only (isInteractive = false)") }
                item { ElementsGroup("catalog-readonly-$generation", catalogSample, handler, isInteractive = false) }

                item { SectionTitle("Menu: unmappable elements") }
                item {
                    MenuFnbRenderer().Content(
                        context = FnbRenderContext("empty-$generation", emptyList()),
                        onAction = handler,
                        modifier = Modifier
                    )
                }

                item { SectionTitle("Cart: tapin2 stage cart/add order as cartView") }
                item { ElementsGroup("cart-stage-$generation", cartStage, handler) }

                item { SectionTitle("Cart: paid (isPaidInFull) and stale (isInteractive = false)") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ElementsGroup("cart-paid-$generation", cartStage.map { it.copy(entityInfo = it.entityInfo + ("isPaidInFull" to true)) }, handler)
                        ElementsGroup("cart-stale-$generation", cartStage, handler, isInteractive = false)
                    }
                }

                item { SectionTitle("Customize: resting (Required default met + Optional)") }
                item {
                    GalleryPanel {
                        CustomizeContent(
                            item = FnbSampleData.nachos.copy(id = "item-nachos-resting-$generation"),
                            currencyCode = FnbSampleData.menu.currencyCode,
                            enabled = true,
                            onConfirm = { selected, quantity, note ->
                                log.add(0, "Customize confirm: qty=$quantity options=${selected.map { it.optionId }} note=$note")
                            }
                        )
                    }
                }

                item { SectionTitle("Customize: validation error (required group without default)") }
                item {
                    GalleryPanel {
                        CustomizeContent(
                            item = FnbSampleData.burger.copy(id = "item-burger-error-$generation"),
                            currencyCode = FnbSampleData.menu.currencyCode,
                            enabled = true,
                            onConfirm = { selected, quantity, note ->
                                log.add(0, "Customize confirm: qty=$quantity options=${selected.map { it.optionId }} note=$note")
                            },
                            initialShowErrors = true
                        )
                    }
                }

                item { SectionTitle("Emitted actions (${log.size})") }
                items(log) { entry ->
                    Text(
                        entry,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = ConciergeTheme.colors.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ConciergeTheme.colors.container, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, color = ConciergeTheme.colors.onSurface)
}

@Composable
private fun GalleryPanel(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(FnbTheme.current.colors.sheetBackground, RoundedCornerShape(16.dp))
    ) { content() }
}

/** Renders a message's elements the way the SDK registry would: grouped by claiming renderer. */
@Composable
private fun ElementsGroup(key: String, elements: List<FnbElement>, handler: FnbActionHandler, isInteractive: Boolean = true) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FnbRenderers.group(elements, GALLERY_RENDERERS).forEachIndexed { index, (renderer, group) ->
            renderer.Content(FnbRenderContext("$key-$index", group, isInteractive), handler, Modifier)
        }
    }
}

/** Loads a BCOS response fixture (`{"multimodalElements": {"elements": [...]}}`) into elements. */
@Suppress("UNCHECKED_CAST")
private fun loadElements(context: Context, assetPath: String): List<FnbElement> = try {
    val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
    val root = JSONUtils.toMap(JSONObject(json)).orEmpty()
    val elements = ((root["multimodalElements"] as? Map<String, Any?>)?.get("elements") as? List<*>).orEmpty()
    elements.mapNotNull { (it as? Map<String, Any?>)?.let(FnbElement::fromMap) }
} catch (e: Exception) {
    Log.w(TAG, "Failed to load $assetPath", e)
    emptyList()
}
