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

package com.adobe.marketing.mobile.concierge.ui.components.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.network.CtaButton as NetworkCtaButton
import com.adobe.marketing.mobile.concierge.network.MultimodalElement
import com.adobe.marketing.mobile.concierge.ui.state.ChatMessage
import com.adobe.marketing.mobile.concierge.ui.state.MessageContent
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeThemeLoader
import com.adobe.marketing.mobile.concierge.utils.image.DefaultImageProvider
import com.adobe.marketing.mobile.concierge.utils.image.LocalImageProvider

// ── Sample cards ────────────────────────────────────────────────────────────

private val sampleCards = listOf(
    MultimodalElement(
        id = "card-1",
        url = "https://picsum.photos/id/10/190/190",
        content = mapOf(
            "productName" to "Product One",
            "productDescription" to "A short subtitle line",
            "productPrice" to "\$49.99",
            "productPageURL" to "https://example.com/1"
        )
    ),
    MultimodalElement(
        id = "card-2",
        url = "https://picsum.photos/id/20/190/190",
        content = mapOf(
            "productName" to "Product Two",
            "productDescription" to "Another subtitle",
            "productPrice" to "\$89.99",
            "productWasPrice" to "\$119.99",
            "productBadge" to "Sale",
            "productPageURL" to "https://example.com/2"
        )
    ),
    MultimodalElement(
        id = "card-3",
        url = "https://picsum.photos/id/30/190/190",
        content = mapOf(
            "productName" to "Product Three Long Title That May Wrap",
            "productDescription" to "Subtitle goes here for context",
            "productPrice" to "\$24.95",
            "productPageURL" to "https://example.com/3"
        )
    )
)

// ── Sample CTAs ──────────────────────────────────────────────────────────────

private val ctaShop = NetworkCtaButton(label = "Shop All", url = "https://example.com/shop")
private val ctaLearn = NetworkCtaButton(label = "Learn More", url = "https://example.com/learn")
private val ctaGetStarted = NetworkCtaButton(label = "Get Started", url = "https://example.com/start")
private val ctaCompare = NetworkCtaButton(label = "Compare Options", url = "https://example.com/compare")
private val ctaContact = NetworkCtaButton(label = "Contact Support", url = "https://example.com/contact")

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Builds the sequence of [ChatMessage]s for one conversation turn.
 * [userText] is the user query; [botText] is the assistant reply; [elements] are
 * the ordered element messages exactly as [ConciergeChatViewModel.appendOrderedElementMessages]
 * would append them (each CTA becomes a standalone message, all cards are one batched Mixed).
 */
@OptIn(ExperimentalStdlibApi::class)
private fun turn(
    userText: String,
    botText: String,
    elements: List<MessageContent>,
    baseTimestamp: Long,
    interactionId: String
): List<ChatMessage> = buildList {
    add(ChatMessage(content = MessageContent.Text(userText), isFromUser = true, timestamp = baseTimestamp))
    add(ChatMessage(content = MessageContent.Text(botText), isFromUser = false, timestamp = baseTimestamp + 1, sseComplete = true, interactionId = interactionId))
    elements.forEachIndexed { i, content ->
        add(ChatMessage(content = content, isFromUser = false, timestamp = baseTimestamp + 2 + i, sseComplete = true))
    }
}

private fun ctaContent(button: NetworkCtaButton) = MessageContent.CtaButton(button)
private fun cardsContent(cards: List<MultimodalElement>) = MessageContent.Mixed(text = "", multimodalElements = cards)

// ── Scenarios ────────────────────────────────────────────────────────────────
// Each scenario reflects the exact message list the ViewModel produces.
// Cards are always batched into one Mixed message at the first card's array position.

@OptIn(ExperimentalStdlibApi::class)
private val allMessages: List<ChatMessage> = buildList {
    // 1. Text-only response (no ordered elements)
    addAll(turn(
        userText = "What can you help me with?",
        botText = "I can assist with product recommendations, pricing, and support questions.",
        elements = emptyList(),
        baseTimestamp = 100L,
        interactionId = "turn-1"
    ))

    // 2. Single CTA
    addAll(turn(
        userText = "How do I get started?",
        botText = "It's easy! Tap the button below to begin.",
        elements = listOf(ctaContent(ctaGetStarted)),
        baseTimestamp = 200L,
        interactionId = "turn-2"
    ))

    // 3. Three CTAs (no cards) — each in its own message
    addAll(turn(
        userText = "What are my options?",
        botText = "Here are a few ways I can help:",
        elements = listOf(ctaContent(ctaShop), ctaContent(ctaLearn), ctaContent(ctaContact)),
        baseTimestamp = 300L,
        interactionId = "turn-3"
    ))

    // 4. Two product cards only — batched as one carousel message
    addAll(turn(
        userText = "Show me some products",
        botText = "Here are a couple of options that match your search:",
        elements = listOf(cardsContent(sampleCards.take(2))),
        baseTimestamp = 400L,
        interactionId = "turn-4"
    ))

    // 5. Three product cards — batched into a single carousel
    addAll(turn(
        userText = "Show me more products",
        botText = "I found a few more items you might like:",
        elements = listOf(cardsContent(sampleCards)),
        baseTimestamp = 500L,
        interactionId = "turn-5"
    ))

    // 6. CTA before cards [CTA, Card, Card]
    // → text | CTA | cards carousel
    addAll(turn(
        userText = "Can you recommend something?",
        botText = "Sure! You can also browse everything, or check out these specific picks:",
        elements = listOf(ctaContent(ctaShop), cardsContent(sampleCards.take(2))),
        baseTimestamp = 600L,
        interactionId = "turn-6"
    ))

    // 7. Cards before CTA [Card, Card, CTA]
    // → text | cards carousel | CTA
    addAll(turn(
        userText = "I'm looking for deals",
        botText = "These are currently on sale. Want to see the full catalog?",
        elements = listOf(cardsContent(sampleCards.take(2)), ctaContent(ctaShop)),
        baseTimestamp = 700L,
        interactionId = "turn-7"
    ))

    // 8. CTA, one card, CTA [CTA, Card, CTA]
    // Cards batch at first card position (after first CTA).
    // → text | CTA | cards carousel | CTA
    addAll(turn(
        userText = "Give me a mixed layout",
        botText = "Here's a mix of actions and products:",
        elements = listOf(ctaContent(ctaLearn), cardsContent(sampleCards.take(1)), ctaContent(ctaCompare)),
        baseTimestamp = 800L,
        interactionId = "turn-8"
    ))

    // 9. Complex: two CTAs + three cards interleaved [CTA, Card, CTA, Card, Card]
    // Cards batch at first card position (after first CTA).
    // → text | CTA | cards carousel (all 3) | CTA
    addAll(turn(
        userText = "Show me everything",
        botText = "Here's a comprehensive response with both actions and product recommendations:",
        elements = listOf(
            ctaContent(ctaGetStarted),
            cardsContent(sampleCards),          // all 3 cards batched here
            ctaContent(ctaContact)
        ),
        baseTimestamp = 900L,
        interactionId = "turn-9"
    ))
}

// ── Demo composable ──────────────────────────────────────────────────────────

/**
 * Demo screen rendering a series of conversation turns that cover every combination
 * of CTA buttons and product cards in the ordered-elements architecture.
 *
 * Scenarios (in order):
 * 1. Text only
 * 2. Text + 1 CTA
 * 3. Text + 3 CTAs
 * 4. Text + 2 cards
 * 5. Text + 3 cards
 * 6. Text + CTA → cards (CTA first)
 * 7. Text + cards → CTA (cards first)
 * 8. Text + CTA → card → CTA
 * 9. Text + CTA → cards (×3) → CTA (complex mixed)
 */
@Composable
internal fun OrderedElementsDemoScreen() {
    CompositionLocalProvider(LocalImageProvider provides DefaultImageProvider()) {
        ConciergeTheme(theme = ConciergeThemeLoader.default()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5))
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Ordered Elements — All Scenarios",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
                )
                Text(
                    text = "Text only • 1 CTA • 3 CTAs • 2 cards • 3 cards • CTA+cards • cards+CTA • CTA+card+CTA • complex",
                    fontSize = 11.sp,
                    color = Color(0xFF888888),
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp)
                )

                HorizontalDivider(color = Color(0xFFDDDDDD))

                MessageList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4200.dp), // fixed height large enough for all scenarios
                    messages = allMessages
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5, widthDp = 400, heightDp = 4400)
@Composable
internal fun OrderedElementsPreview() {
    OrderedElementsDemoScreen()
}
