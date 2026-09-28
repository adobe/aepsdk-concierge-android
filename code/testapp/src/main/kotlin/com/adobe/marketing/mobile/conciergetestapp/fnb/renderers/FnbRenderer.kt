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

package com.adobe.marketing.mobile.conciergetestapp.fnb.renderers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler

/*
 * PLACEHOLDER renderer contract. The SDK's customer-renderer registry is not available yet; this
 * file mirrors the expected shape and is the single swap point when it lands (change these types
 * to the SDK's, widget bodies stay unchanged).
 *
 * Everything under `fnb/` compiles against public Concierge SDK API + AndroidX Compose only, so
 * the folder can be copied verbatim into a customer app.
 */

/**
 * One element of `response.multimodalElements.elements[]` as BCOS sends it. All meaningful data
 * is in [entityInfo] (`entity_info`); [entityId] is the business entity (a product id for
 * `catalogItemCard`, the venue for `cartBar`).
 */
@Immutable
data class FnbElement(
    val id: String,
    val entityId: String,
    val type: String,
    val cardType: String,
    val entityInfo: Map<String, Any?>
) {
    /** The raw element shape the mappers read (`{id, entityId, type, cardType, entity_info}`). */
    fun asMap(): Map<String, Any?> =
        mapOf("id" to id, "entityId" to entityId, "type" to type, "cardType" to cardType, "entity_info" to entityInfo)

    companion object {
        /** Parses one raw element map; returns null when `id` or `type` is missing. */
        @Suppress("UNCHECKED_CAST")
        fun fromMap(raw: Map<String, Any?>): FnbElement? {
            val id = raw["id"]?.toString()?.takeIf { it.isNotBlank() } ?: return null
            val type = (raw["type"] as? String)?.takeIf { it.isNotBlank() } ?: return null
            return FnbElement(
                id = id,
                entityId = raw["entityId"]?.toString().orEmpty(),
                type = type,
                cardType = (raw["cardType"] as? String).orEmpty(),
                entityInfo = (raw["entity_info"] as? Map<String, Any?>).orEmpty()
            )
        }
    }
}

/**
 * @property groupKey stable key for one rendered group in one message (the SDK should derive it
 * from the message `interactionId` plus the first element id); keys saveable widget state.
 * @property elements the consecutive elements of one message that this renderer claimed, in order.
 * @property isInteractive false for historical/stale messages so they render read-only.
 */
@Immutable
data class FnbRenderContext(
    val groupKey: String,
    val elements: List<FnbElement>,
    val isInteractive: Boolean = true
)

interface FnbRenderer {
    /** Element `type`s this renderer claims (e.g. `catalogItemCard`, `cartBar`). */
    val elementTypes: Set<String>

    /**
     * Requires `LocalImageProvider` (provided by `ConciergeChat`) and reads theme values from the
     * enclosing `ConciergeTheme`.
     */
    @Composable
    fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier)
}

/** The menu: all `catalogItemCard` elements of a message plus its `cartBar`, as one widget. */
object MenuFnbRenderer : FnbRenderer {
    override val elementTypes: Set<String> = setOf("catalogItemCard", "cartBar")

    @Composable
    override fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier) {
        MenuRenderer(context = context, onAction = onAction, modifier = modifier)
    }
}

object CartFnbRenderer : FnbRenderer {
    override val elementTypes: Set<String> = setOf("cartView")

    @Composable
    override fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier) {
        CartSummaryRenderer(context = context, onAction = onAction, modifier = modifier)
    }
}

/**
 * Customer-side lookup, standing in for the future SDK registry. [group] shows the dispatch the
 * SDK needs: consecutive elements claimed by the same renderer are handed over together, because
 * the menu (tabs, grid, shared cart, footer) is one widget built from many cards.
 */
object FnbRenderers {
    val all: List<FnbRenderer> = listOf(MenuFnbRenderer, CartFnbRenderer)

    fun forType(type: String): FnbRenderer? = all.firstOrNull { type in it.elementTypes }

    /** Splits a message's elements into renderer groups; unclaimed elements are dropped. */
    fun group(elements: List<FnbElement>): List<Pair<FnbRenderer, List<FnbElement>>> {
        val groups = mutableListOf<Pair<FnbRenderer, MutableList<FnbElement>>>()
        for (element in elements) {
            val renderer = forType(element.type) ?: continue
            val last = groups.lastOrNull()
            if (last != null && last.first === renderer) last.second += element else groups += renderer to mutableListOf(element)
        }
        return groups
    }
}
