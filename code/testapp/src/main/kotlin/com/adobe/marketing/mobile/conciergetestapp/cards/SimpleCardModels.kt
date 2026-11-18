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

package com.adobe.marketing.mobile.conciergetestapp.cards

import androidx.compose.runtime.Immutable

/**
 * A carousel of simple cards: image, text and call-to-action buttons that continue the
 * conversation. It is the shape every "pick one of these" card shares — concession stands,
 * parking events, parking lots — so one renderer and one runtime serve them all.
 *
 * A card type contributes only a mapper from its vendor payload to this model; see
 * [simpleCardRenderer].
 */
@Immutable
data class SimpleCardList(
    val cards: List<SimpleCard>,
    /**
     * Whether to draw the image slot. Decided for the whole list rather than per card, so cards
     * line up even when only some of them have artwork.
     */
    val showImages: Boolean
)

@Immutable
data class SimpleCard(
    /** Vendor id of the thing this card stands for; also the card's identity in the carousel. */
    val id: String,
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    /** Pinned to the bottom-left of the image, as the built-in product card does. */
    val badge: String? = null,
    /** One line above the buttons, in the slot the built-in card gives the price. */
    val detail: String? = null,
    /** False renders the card dimmed and inert: visible, but not something the fan can pick. */
    val enabled: Boolean = true,
    /** One or two buttons. The first also fires when the card itself is tapped. */
    val ctas: List<SimpleCardCta> = emptyList()
)

/**
 * A button on a card. Pressing it asks the concierge to take a turn; it never opens a link.
 *
 * @property intentName structured action for the backend, e.g. `showMenu`.
 * @property parameters structured arguments for [intentName], e.g. the vendor id.
 * @property localMessage short agent-voice line the transcript shows when the press is handed
 * off; display-only, never sent to the backend.
 */
@Immutable
data class SimpleCardCta(
    val label: String,
    val intentName: String,
    val parameters: Map<String, Any?> = emptyMap(),
    val localMessage: String? = null,
    val pendingLabel: String = "Opening…"
)
