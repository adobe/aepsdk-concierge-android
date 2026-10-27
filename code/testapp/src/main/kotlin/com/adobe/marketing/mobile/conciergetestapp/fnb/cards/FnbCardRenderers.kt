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

package com.adobe.marketing.mobile.conciergetestapp.fnb.cards

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.adobe.marketing.mobile.concierge.Concierge
import com.adobe.marketing.mobile.concierge.ConciergeCardRenderer
import com.adobe.marketing.mobile.concierge.ConciergeDataHandoffRejectReason
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionSender
import com.adobe.marketing.mobile.conciergetestapp.cards.CardGate
import com.adobe.marketing.mobile.conciergetestapp.cards.CardUnavailable
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardContent
import com.adobe.marketing.mobile.conciergetestapp.cards.SimpleCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.cards.mapElementOrNull
import com.adobe.marketing.mobile.conciergetestapp.cards.savedCardState
import com.adobe.marketing.mobile.conciergetestapp.fnb.FnbSessionContext
import com.adobe.marketing.mobile.conciergetestapp.fnb.FnbXdm
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CheckoutUrlPolicy
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.LocationsMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.CartSummaryContent
import com.adobe.marketing.mobile.conciergetestapp.fnb.renderers.MenuContent
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.id
import com.adobe.marketing.mobile.conciergetestapp.common.PayloadReads.long
import com.adobe.marketing.mobile.conciergetestapp.parking.ParkingEventsMapper
import com.adobe.marketing.mobile.services.Log
import java.util.concurrent.atomic.AtomicBoolean

/** Exact element types emitted by the current BCOS F&B flow. */
object FnbCardTypes {
    const val LOCATIONS = "kings.fnblocation"
    const val MENU = "kings.menu"
    /** BCOS `add_to_cart` post-hook; `entity_info` is the raw tapin2 `cart/add` order. */
    const val CART = "kings.fnbcart"
    const val PAYLOAD_KEY = "entity_info"
}

/** Sends one data handoff; [Concierge.sendDataHandoff] outside tests. */
internal fun interface HandoffTransport {
    fun send(
        routingHint: String,
        xdmFields: Map<String, Any>,
        localMessage: String?,
        completion: (accepted: Boolean, reason: ConciergeDataHandoffRejectReason?) -> Unit
    )
}

/**
 * Registers the F&B card renderers with [Concierge.registerCardRenderer]. Call once at app start,
 * before a chat opens; a later call replaces the earlier renderers.
 *
 * Card actions become data handoffs with a routing hint and app-defined XDM. Menus and carts share
 * one [FnbOrderStore]: only the newest menu takes orders, and the current and checked-out orders
 * are kept in XDM context for Brand Concierge.
 *
 * @param openCheckout opens an allowlisted checkout URL; returns false when it couldn't. Held for
 * the life of the process, so it must not capture an Activity.
 */
fun registerFnbCardRenderers(
    locationOptions: LocationOptions = LocationOptions(),
    menuOptions: MenuOptions = MenuOptions(),
    cartOptions: CartOptions = CartOptions.STAGE,
    openCheckout: (String) -> Boolean
) {
    // A lambda, not `Concierge::registerCardRenderer`: Compose lowers the @Composable renderer type,
    // and a function reference to it is adapted with the unlowered signature (NoSuchMethodError).
    registerFnbCardRenderers(locationOptions, menuOptions, cartOptions, openCheckout) { type, renderer ->
        Concierge.registerCardRenderer(type, renderer)
    }
}

internal fun registerFnbCardRenderers(
    locationOptions: LocationOptions,
    menuOptions: MenuOptions,
    cartOptions: CartOptions,
    openCheckout: (String) -> Boolean,
    orders: FnbOrderStore = processOrders,
    register: (String, ConciergeCardRenderer) -> Unit
) {
    fun sender(type: String) = fnbActionSender(type, cartOptions.allowedCheckoutHosts, openCheckout)

    register(FnbCardTypes.LOCATIONS, locationRenderer(locationOptions, sender(FnbCardTypes.LOCATIONS), orders))
    register(FnbCardTypes.MENU, menuRenderer(menuOptions, sender(FnbCardTypes.MENU), orders))
    register(FnbCardTypes.CART, cartRenderer(cartOptions, sender(FnbCardTypes.CART), orders))
}

private fun locationRenderer(
    options: LocationOptions,
    actions: CardActionSender,
    orders: FnbOrderStore
): ConciergeCardRenderer =
    @Composable { element, context, saveState ->
        val model = remember(element) { mapElementOrNull(element) { LocationsMapper.mapElement(element, options) } }
        if (model == null) {
            CardUnavailable()
        } else {
            val cardId = context["cardId"] as? String
            val sessionId = context["sessionId"] as? String
            val eventId = long(element["eventId"])?.takeIf { it >= 0 }
            val isStale = CardGate.from(context).isStale
            if (cardId != null) {
                LaunchedEffect(cardId, sessionId, eventId, isStale) {
                    if (!isStale) orders.locationsShown(cardId, sessionId, eventId)
                }
            }
            val state = savedCardState(context)
            val runtime = remember(context["cardId"], model) { SimpleCardRuntime(model, actions, state, saveState) }
            LaunchedEffect(runtime, state) { runtime.adopt(state) }
            SimpleCardContent(runtime, CardGate.from(context))
        }
    }

private fun menuRenderer(options: MenuOptions, actions: CardActionSender, orders: FnbOrderStore): ConciergeCardRenderer =
    @Composable { element, context, saveState ->
        val model = remember(element) {
            mapElementOrNull(element) { CatalogMenuMapper.map(element[FnbCardTypes.PAYLOAD_KEY], options) }
        }
        if (model == null) {
            CardUnavailable()
        } else {
            val cardId = context["cardId"] as? String
            val sessionId = context["sessionId"] as? String
            val orderState by orders.state.collectAsState()
            if (cardId != null) LaunchedEffect(cardId, sessionId) { orders.menuShown(cardId, sessionId) }
            val state = savedCardState(context)
            val runtime = remember(cardId, model) {
                MenuCardRuntime(
                    model, actions, state, saveState
                )
            }
            LaunchedEffect(runtime, state) { runtime.adopt(state) }
            MenuContent(
                runtime,
                CardGate.from(context),
                active = cardId != null && orders.isActionableMenu(orderState, cardId, sessionId)
            )
        }
    }

private fun cartRenderer(options: CartOptions, actions: CardActionSender, orders: FnbOrderStore): ConciergeCardRenderer =
    @Composable { element, context, saveState ->
        val model = remember(element) {
            mapElementOrNull(element) {
                CartSummaryMapper.map(element[FnbCardTypes.PAYLOAD_KEY], options, entityId = element["entityId"]?.toString())
            }
        }
        if (model == null) {
            CardUnavailable()
        } else {
            val cardId = context["cardId"] as? String
            val sessionId = context["sessionId"] as? String
            val orderState by orders.state.collectAsState()
            if (cardId != null) LaunchedEffect(cardId) { orders.cartShown(cardId, sessionId, model.orderId, model.isPaid) }
            val state = savedCardState(context)
            val runtime = remember(cardId, model) {
                CartCardRuntime(
                    model, actions, options.allowedCheckoutHosts, state, saveState,
                    isOrderClosed = { orders.state.value.isOrderSubmitted(model.orderId) },
                    onCheckedOut = { orders.checkedOut(model.orderId) }
                )
            }
            LaunchedEffect(runtime, state) { runtime.adopt(state) }
            CartSummaryContent(runtime, CardGate.from(context), orderClosed = orderState.isOrderSubmitted(model.orderId))
        }
    }

/** Process-wide, so re-registering renderers (e.g. on Activity recreation) keeps the order state. */
private val processOrders by lazy {
    FnbOrderStore(
        publishContext = ::publishOrderContext,
        publishEventContext = FnbSessionContext::setCurrentEvent,
        onSessionChanged = FnbSessionContext::clearLocalContext
    )
}

internal fun resetFnbContextForIdentityChange() {
    processOrders.identityReset()
    FnbSessionContext.clearLocalContext()
}

/** Mirrors the store's order patches into the XDM context sent with every Concierge turn. */
private fun publishOrderContext(patch: Map<String, Any?>) {
    try {
        Concierge.updateXDMContext(patch)
    } catch (e: IllegalArgumentException) {
        Log.warning(LOG_TAG, SELF_TAG, "Could not update XDM context with the order state: ${e.message}")
    }
}

/**
 * Maps card actions of [type] cards to app-local checkout or a data handoff, and reports the
 * outcome on the main thread.
 *
 * Every action needs a non-blank `actionId` (the local retry key); it is never sent in XDM.
 * Malformed actions and checkout URLs outside [allowedCheckoutHosts] report
 * [CardActionOutcome.INVALID] without sending anything.
 */
internal fun fnbActionSender(
    type: String,
    allowedCheckoutHosts: Set<String>,
    openCheckout: (String) -> Boolean,
    transport: HandoffTransport = HandoffTransport { routingHint, xdmFields, localMessage, completion ->
        Concierge.sendDataHandoff(routingHint, xdmFields, localMessage) { accepted, reason -> completion(accepted, reason) }
    },
    postToMain: (() -> Unit) -> Unit = ::postToMainThread
) = CardActionSender { action, onResult ->
    val operation = action["operation"] as? String
    val actionId = (action["actionId"] as? String)?.takeIf(String::isNotBlank)
    if (actionId == null) {
        Log.warning(LOG_TAG, SELF_TAG, "Rejected action for $type: missing actionId (operation=$operation)")
        onResult(CardActionOutcome.INVALID)
        return@CardActionSender
    }
    if (operation == FnbOperations.OPEN_CHECKOUT) {
        val opened = openAllowedCheckout(action["url"] as? String, allowedCheckoutHosts, openCheckout, actionId)
        onResult(if (opened) CardActionOutcome.ACCEPTED else CardActionOutcome.INVALID)
        return@CardActionSender
    }

    val handoff = try {
        handoffFor(operation, actionId, action)
    } catch (e: IllegalArgumentException) {
        null
    }
    if (handoff == null) {
        Log.warning(LOG_TAG, SELF_TAG, "Rejected action $actionId for $type: invalid fields (operation=$operation)")
        onResult(CardActionOutcome.INVALID)
        return@CardActionSender
    }

    val localMessage = (action["message"] as? String)?.takeIf(String::isNotBlank)
    val completed = AtomicBoolean(false)
    try {
        transport.send(handoff.routingHint, handoff.xdmFields, localMessage) { accepted, reason ->
            val outcome = CardActionOutcome.ofHandoff(accepted, reason)
            if (!accepted) {
                Log.debug(LOG_TAG, SELF_TAG, "Handoff ${handoff.routingHint} ($actionId) not accepted: $reason -> $outcome")
            }
            if (completed.compareAndSet(false, true)) postToMain { onResult(outcome) }
        }
    } catch (e: Exception) {
        // It may have thrown after the handoff went out, so treat it as possibly delivered.
        Log.warning(LOG_TAG, SELF_TAG, "Could not send handoff ${handoff.routingHint} ($actionId): ${e.message}")
        if (completed.compareAndSet(false, true)) onResult(CardActionOutcome.UNCONFIRMED)
    }
}

private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

private fun postToMainThread(block: () -> Unit) {
    mainHandler.post(block)
}

/** Card action `operation` values; each doubles as the routing hint, except [SHOW_RESTAURANTS] (`view-menu`). */
internal object FnbOperations {
    const val ADD_TO_CART = "add-to-cart"
    const val REMOVE_CART_ITEM = "remove-from-cart"
    const val SHOW_RESTAURANTS = "showRestaurants"

    /** Handled in the app; never sent to BCOS. */
    const val OPEN_CHECKOUT = "openCheckout"
}

private const val LOG_TAG = "ConciergeTestApp"
private const val SELF_TAG = "FnbCardRenderers"

internal class Handoff(val routingHint: String, val xdmFields: Map<String, Any>)

/**
 * Builds the handoff for [operation], or null when a required field is missing. An action without
 * an `operation` is a simple-card press: its `intent` is the routing hint.
 */
internal fun handoffFor(operation: String?, actionId: String, action: Map<String, Any?>): Handoff? = when (operation) {
    FnbOperations.ADD_TO_CART -> (action["xdmFields"] as? Map<*, *>)
        ?.let(::jsonObject)
        ?.takeIf { xdm ->
            val namespace = xdm[FnbXdm.NAMESPACE] as? Map<*, *>
            val actions = namespace?.get(FnbXdm.CONCIERGE_ACTION) as? Map<*, *>
            val addToCart = actions?.get("addToCart") as? Map<*, *>
            (addToCart?.get("products") as? List<*>)?.isNotEmpty() == true
        }
        ?.let { Handoff(FnbOperations.ADD_TO_CART, it) }
    FnbOperations.REMOVE_CART_ITEM -> {
        val locationId = action["locationId"]?.let(::id)?.toLongOrNull()?.takeIf { it >= 0 }
        val productId = action["productId"]?.let(::id)?.toLongOrNull()?.takeIf { it >= 0 }
        if (locationId == null || productId == null) {
            null
        } else {
            val product = buildMap<String, Any> {
                put("locationId", locationId)
                put("productId", productId)
                (action["modifier"] as? String)?.trim()?.takeIf(String::isNotEmpty)?.let { put("modifier", it) }
            }
            Handoff(FnbOperations.REMOVE_CART_ITEM, FnbXdm.action("removeFromCart", mapOf("products" to listOf(product))))
        }
    }
    FnbOperations.SHOW_RESTAURANTS -> action["locationId"]?.let(::id)?.let { locationId ->
        Handoff(LocationsMapper.INTENT_SHOW_MENU, FnbXdm.action("viewMenu", mapOf("locationId" to locationId)))
    }
    null -> (action["intent"] as? String)?.takeIf(String::isNotBlank)?.let { intent ->
        val parameters = action["parameters"] as? Map<*, *> ?: emptyMap<Any?, Any?>()
        if (intent == LocationsMapper.INTENT_SHOW_MENU) {
            parameters["locationId"]?.let(::id)?.let { locationId ->
                Handoff(intent, FnbXdm.action("viewMenu", mapOf("locationId" to locationId)))
            }
        } else if (intent == ParkingEventsMapper.INTENT_SHOW_PARKING) {
            parameters["eventId"]?.let(::id)?.let { eventId ->
                Handoff(intent, FnbXdm.action("searchParking", mapOf("eventId" to eventId)))
            }
        } else {
            Handoff(intent, mapOf("actionId" to actionId, "parameters" to jsonObject(parameters)))
        }
    }
    else -> null
}

private fun openAllowedCheckout(
    url: String?,
    allowedHosts: Set<String>,
    open: (String) -> Boolean,
    actionId: String
): Boolean {
    val normalizedUrl = url?.trim()
    if (normalizedUrl == null || !CheckoutUrlPolicy.isAllowed(normalizedUrl, allowedHosts)) {
        Log.warning(LOG_TAG, SELF_TAG, "Blocked checkout $actionId: URL missing or host not allowed")
        return false
    }
    return try {
        open(normalizedUrl).also { opened ->
            if (!opened) Log.warning(LOG_TAG, SELF_TAG, "Checkout $actionId could not be opened")
        }
    } catch (e: Exception) {
        Log.warning(LOG_TAG, SELF_TAG, "Checkout $actionId could not be opened: ${e.message}")
        false
    }
}

private fun jsonObject(value: Map<*, *>): Map<String, Any> {
    val result = LinkedHashMap<String, Any>()
    value.forEach { (key, item) ->
        val name = key as? String ?: throw IllegalArgumentException("XDM object keys must be strings")
        if (item != null) result[name] = jsonValue(item)
    }
    return result
}

private fun jsonValue(value: Any): Any = when (value) {
    is Map<*, *> -> jsonObject(value)
    is List<*> -> value.mapNotNull { it?.let(::jsonValue) }
    is String, is Number, is Boolean -> value
    else -> throw IllegalArgumentException("Unsupported card action JSON value: ${value::class.java.name}")
}
