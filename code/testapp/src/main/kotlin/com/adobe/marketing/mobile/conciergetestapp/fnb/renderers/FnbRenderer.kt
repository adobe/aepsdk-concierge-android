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
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions

/*
 * PLACEHOLDER renderer contract. The SDK's customer-renderer registry is not available yet; this
 * file mirrors the expected shape and is the single swap point when it lands (change these types
 * to the SDK's, widget bodies stay unchanged).
 *
 * Everything under `fnb/` compiles against public Concierge SDK API + AndroidX Compose only, so
 * the folder can be copied verbatim into a customer app.
 */

/**
 * One element of `response.multimodalElements.elements[]`: BC's thin envelope around a
 * customer/vendor API response.
 *
 * - [rendererId]: which customer-owned renderer draws it (resolved by [FnbRendererRegistry]).
 * - [entityId]: the entity the element is about (tapin2 `location.id` for a menu, order `id` for
 *   a cart).
 * - [payload]: the tapin2 response body, passed through largely untouched (the products array for
 *   a menu, the `cart/add` order object for a cart). BC owns only the envelope; the renderer owns
 *   interpreting the body.
 *
 * The payload is carried in `entity_info`.
 */
@Immutable
data class FnbElement(
    val id: String,
    val entityId: String,
    val rendererId: String,
    val payload: Any?
) {
    fun asMap(): Map<String, Any?> =
        mapOf(Keys.ID to id, Keys.ENTITY_ID to entityId, Keys.RENDERER_ID to rendererId, Keys.PAYLOAD to payload)

    object Keys {
        const val ID = "id"
        const val ENTITY_ID = "entityId"
        const val RENDERER_ID = "rendererId"
        const val PAYLOAD = "entity_info"
    }

    companion object {
        /** Parses one raw element map; returns null when `id` or `rendererId` is missing. */
        fun fromMap(raw: Map<String, Any?>): FnbElement? {
            val id = raw[Keys.ID]?.toString()?.takeIf { it.isNotBlank() } ?: return null
            val rendererId = (raw[Keys.RENDERER_ID] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return FnbElement(
                id = id,
                entityId = raw[Keys.ENTITY_ID]?.toString().orEmpty(),
                rendererId = rendererId,
                payload = raw[Keys.PAYLOAD]
            )
        }
    }
}

/**
 * @property elementKey stable key for this element in this message (the SDK should derive it from
 * the message `interactionId` plus the element `id`); keys saveable widget state.
 * @property isInteractive false for historical/stale messages so they render read-only.
 */
@Immutable
data class FnbRenderContext(
    val elementKey: String,
    val element: FnbElement,
    val isInteractive: Boolean = true
)

/** A customer-owned renderer: interprets one element's tapin2 payload and draws it. */
interface FnbRenderer {
    val rendererId: String

    /**
     * Requires `LocalImageProvider` (provided by `ConciergeChat`) and reads theme values from the
     * enclosing `ConciergeTheme`.
     */
    @Composable
    fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier)
}

/** Well-known renderer ids BC stamps on F&B elements. */
object FnbRendererIds {
    /** Payload: tapin2 `GET /v2/venues/{venueId}/locations/{locationId}/products` response (array). */
    const val MENU = "fnb.menu"

    /** Payload: tapin2 `POST /v2/cart/add` response (the order), also after remove/update. */
    const val CART = "fnb.cart"
}

class MenuFnbRenderer(private val options: MenuOptions = MenuOptions()) : FnbRenderer {
    override val rendererId: String = FnbRendererIds.MENU

    @Composable
    override fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier) {
        MenuRenderer(context = context, onAction = onAction, modifier = modifier, options = options)
    }
}

class CartFnbRenderer(private val options: CartOptions = CartOptions()) : FnbRenderer {
    override val rendererId: String = FnbRendererIds.CART

    @Composable
    override fun Content(context: FnbRenderContext, onAction: FnbActionHandler, modifier: Modifier) {
        CartSummaryRenderer(context = context, onAction = onAction, modifier = modifier, options = options)
    }
}

/**
 * Registry keyed by `rendererId`, standing in for the SDK's. At render time the SDK resolves each
 * element's renderer here and runs it against that element; elements whose `rendererId` isn't
 * registered fall through to the SDK's own rendering (or are skipped).
 */
class FnbRendererRegistry(renderers: List<FnbRenderer>) {
    private val byId: Map<String, FnbRenderer> = renderers.associateBy { it.rendererId }

    fun resolve(rendererId: String): FnbRenderer? = byId[rendererId]

    fun resolve(element: FnbElement): FnbRenderer? = resolve(element.rendererId)

    companion object {
        val DEFAULT = FnbRendererRegistry(listOf(MenuFnbRenderer(), CartFnbRenderer()))
    }
}
