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

package com.adobe.marketing.mobile.concierge.ui.components.card

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.adobe.marketing.mobile.concierge.network.ConversationService
import com.adobe.marketing.mobile.concierge.network.ConversationState
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.network.ParsedConversationMessage
import com.adobe.marketing.mobile.concierge.network.ParsedMultimodalItem
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChat
import com.adobe.marketing.mobile.concierge.ui.chat.ConciergeChatViewModel
import com.adobe.marketing.mobile.concierge.ui.state.ChatEvent
import com.adobe.marketing.mobile.concierge.ui.state.Feedback
import com.adobe.marketing.mobile.concierge.ui.stt.AndroidSpeechCapturing
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeLayout
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeProductCardBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeProductCardCtaButtonColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeProductCardSecondaryCtaButtonColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeBehavior
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeMessageColors
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeConfig
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeData
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeTokens
import com.adobe.marketing.mobile.concierge.ui.theme.ProductCardStyle
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Product-card demo. Rather than hand-building card composables, this drives the SDK's real
 * rendering pipeline: it seeds a [ConciergeChatViewModel] with a fake conversation service
 * ([DemoConversationServiceClient]) that returns a canned product-card response, then renders the
 * actual [ConciergeChat]. The cards, carousel, and CTA button are produced by the same parse →
 * ViewModel → RecommendationCards → ExtendedProductCard path a live backend response flows through,
 * so the demo can never drift from production rendering.
 */
@Composable
fun ProductCardDemoScreen() {
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Obtain via viewModel() (not remember) so the ViewModel is owned by the host's ViewModelStore
    // and its onCleared() runs -- releasing speech capture and the chat service on exit.
    val viewModel: ConciergeChatViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                ConciergeChatViewModel(
                    application,
                    AndroidSpeechCapturing(application),
                    DemoConversationServiceClient()
                )
            }
        }
    )

    // Trigger the fake response once so the pipeline renders the product cards on launch.
    LaunchedEffect(Unit) {
        viewModel.processEvent(ChatEvent.SendMessage("Show me some product recommendations"))
    }

    ConciergeTheme(theme = demoProductCardTheme) {
        CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
            ConciergeChat(
                viewModel = viewModel,
                onClose = { (context as? Activity)?.finish() }
            )
        }
    }
}

/**
 * Fake conversation service that returns a canned product-card response instead of hitting the
 * backend. Emits a single COMPLETED message whose ordered elements are product cards, exactly as
 * the real client would after parsing an SSE stream.
 */
private class DemoConversationServiceClient : ConversationService {
    override fun chat(message: String): Flow<ParsedConversationMessage> = flowOf(
        ParsedConversationMessage(
            messageContent = "",
            state = ConversationState.COMPLETED,
            conversationId = "demo-conversation",
            orderedElements = demoProductCards.map { ParsedMultimodalItem.Card(it) }
        )
    )

    override fun sendDataHandoff(
        routingHint: String,
        xdmFields: Map<String, Any>
    ): Flow<ParsedConversationMessage> = chat(routingHint)

    override suspend fun sendFeedback(feedback: Feedback): Boolean = true

    override fun cleanup() = Unit
}

/** Sample product cards demonstrating short, long, and ellipsized descriptions in the real carousel. */
private val demoProductCards = listOf(
    MultimodalElement(
        id = "demo-1",
        url = "https://picsum.photos/id/70/190/190",
        content = mapOf(
            "productName" to "Trail Running Shoes - 2 Lines",
            "productDescription" to "Soft comfort for every mile.\nLightweight mesh, all-day fit.",
            "productPrice" to "\$119.99",
            "primaryText" to "Shop now",
            "primaryUrl" to "https://example.com/buy/trail-running-shoes"
        )
    ),
    MultimodalElement(
        id = "demo-2",
        url = "https://picsum.photos/id/80/190/190",
        content = mapOf(
            "productName" to "Road Running Shoes - 4 Lines",
            "productDescription" to "Soft cushioning for long runs.\nSupport with every stride.\nBreathable mesh feels cool.\nLightweight heel to toe.",
            "productPrice" to "\$129.99",
            "primaryText" to "Shop now",
            "primaryUrl" to "https://example.com/buy/road-running-shoes"
        )
    ),
    MultimodalElement(
        id = "demo-3",
        url = "https://picsum.photos/id/90/190/190",
        content = mapOf(
            "productName" to "Cushioned Running Shoes - 6 Lines",
            "productDescription" to "Soft cushioning for long runs. Support with every stride. Breathable mesh feels cool. Lightweight heel to toe. Comfortable on any route. Made with recycled fibers. Built for a secure fit.",
            "productPrice" to "\$139.99",
            "primaryText" to "Shop now",
            "primaryUrl" to "https://example.com/buy/cushioned-running-shoes"
        )
    ),
    MultimodalElement(
        id = "demo-4",
        url = "https://picsum.photos/id/100/190/190",
        content = mapOf(
            "productName" to "Everyday Running Shoes - Long Description",
            // Intentional line breaks make the six-line ellipsis limit easy to inspect.
            "productDescription" to "Responsive cushioning for daily runs.\nBreathable upper helps keep feet cool.\nDurable outsole grips varied surfaces.\nLightweight design supports quick movement.\nPadded collar adds a secure feel.\nFlexible construction follows each stride.\nReflective details improve low-light visibility.\nMade with partially recycled materials.",
            "productPrice" to "\$149.99",
            "primaryText" to "Shop now",
            "primaryUrl" to "https://example.com/buy/everyday-running-shoes"
        )
    )
)

/**
 * Matches the "Vertical Card - With description" design spec (222x468, 190x190 image, 8dp radius,
 * #E3E3E3 outline, subtle drop shadow) and selects the [ProductCardStyle.PRODUCT_DETAIL] extended
 * card so the CTA button is exercised.
 */
private val demoProductCardTheme = ConciergeThemeData(
    config = ConciergeThemeConfig(),
    tokens = ConciergeThemeTokens(
        behavior = ConciergeThemeBehavior(
            productCard = ConciergeProductCardBehavior(cardStyle = ProductCardStyle.PRODUCT_DETAIL)
        ),
        cssLayout = ConciergeLayout(
            productCardWidth = 222.0,
            productCardMinHeight = 240.0,
            productCardMaxHeight = 468.0,
            productCardDescriptionMaxLines = 6,
            productImageWidth = 190.0,
            productImageHeight = 190.0,
            productCardBorderRadius = 8.0,
            productCardOutlineColor = "#E3E3E3",
            productCardBackgroundColor = "#FFFFFF"
        ),
        colors = ConciergeThemeColors(
            // Give the chat surfaces real colors so the auto-sent user message bubble renders
            // legibly: a blue bubble (via `primary`) with white text (`message.userText`, which
            // otherwise defaults to black).
            primary = "#2563EB",
            onPrimary = "#FFFFFF",
            message = ConciergeMessageColors(userText = "#FFFFFF"),
            productCardCtaButton = ConciergeProductCardCtaButtonColors(
                // Generic red fill, purely for the demo -- not tied to any brand.
                backgroundColor = "#D32F2F",
                textColor = "#FFFFFF"
            ),
            productCardSecondaryCtaButton = ConciergeProductCardSecondaryCtaButtonColors(
                // Outlined counterpart to the demo's red primary CTA.
                backgroundColor = "#00000000",
                textColor = "#D32F2F",
                borderColor = "#D32F2F"
            )
        )
    )
)
