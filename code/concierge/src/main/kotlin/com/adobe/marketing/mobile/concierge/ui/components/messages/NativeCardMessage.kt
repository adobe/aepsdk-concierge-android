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

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.adobe.marketing.mobile.concierge.cards.NativeCardController
import com.adobe.marketing.mobile.concierge.ui.components.image.rememberIsIconConfigured
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/** The native cards of the chat being rendered; provided by `ConciergeChat`. */
internal val LocalNativeCardController = staticCompositionLocalOf<NativeCardController?> { null }

internal const val NATIVE_CARD_TEST_TAG = "NativeCard"
internal const val UNAVAILABLE_CARD_TEST_TAG = "NativeCardUnavailable"

private const val UNAVAILABLE_CARD_TEXT = "This content isn't available right now."

/**
 * One native card transcript row: the host owns the outer insets (aligned with agent messages),
 * and the renderer owns everything inside at its natural height.
 */
@Composable
internal fun NativeCardMessage(cardId: String, modifier: Modifier = Modifier) {
    val controller = LocalNativeCardController.current
    val card = remember(controller, cardId) { controller?.renderableCard(cardId) }
    val style = ConciergeStyles.messageBubbleStyle
    val rawIconName = ConciergeTheme.tokens?.assets?.icons?.company?.takeIf { it.isNotEmpty() }
    val hasCompanyIcon = rememberIsIconConfigured(rawIconName)
    val startInset = if (hasCompanyIcon) style.agentIconSize + style.agentIconSpacing else style.padding

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startInset, end = style.padding, top = style.padding, bottom = style.padding)
    ) {
        if (controller == null || card == null) {
            UnavailableCard()
        } else {
            RegisteredRendererCard(controller, card)
        }
    }
}

@Composable
private fun RegisteredRendererCard(controller: NativeCardController, card: NativeCardController.RenderableCard) {
    val isBusy by controller.isBusy.collectAsState()
    val isStale by card.isStale.collectAsState()
    val state by card.state.collectAsState()
    val context = remember(card, isBusy, isStale, state) { card.rendererContext(isBusy, isStale, state) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(NATIVE_CARD_TEST_TAG)
            .animateContentSize()
    ) {
        // Compose can't catch exceptions thrown during composition; renderers must not throw.
        card.renderer(card.element, context, card.saveState)
    }
}

@Composable
private fun UnavailableCard() {
    val style = ConciergeStyles.messageBubbleStyle
    Card(
        modifier = Modifier.testTag(UNAVAILABLE_CARD_TEST_TAG),
        colors = CardDefaults.cardColors(containerColor = style.botMessageBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = style.elevation),
        shape = style.shape
    ) {
        Text(
            text = UNAVAILABLE_CARD_TEXT,
            style = style.textStyle,
            color = style.botMessageTextColor,
            modifier = Modifier.padding(style.innerPadding)
        )
    }
}
