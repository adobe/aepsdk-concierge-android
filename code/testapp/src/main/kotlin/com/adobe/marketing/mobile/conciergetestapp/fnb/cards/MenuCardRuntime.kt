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

import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionOutcome
import com.adobe.marketing.mobile.conciergetestapp.cards.CardActionSender
import com.adobe.marketing.mobile.conciergetestapp.cards.CardStateSaver
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbHandoffFormatter
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartCodec
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/** Progress of the menu's ADD TO CART handoff. [Submitting] locks the menu; [Added] and [Unconfirmed] close it. */
sealed interface SubmitState {
    object Idle : SubmitState
    object Submitting : SubmitState

    /** The items were added; the menu is closed. */
    object Added : SubmitState

    /** The handoff started but its outcome is unknown; its items are never resubmitted. */
    object Unconfirmed : SubmitState
    data class Failed(val message: String) : SubmitState
}

/**
 * State for one `kings.menu` element. Cart edits stay local until ADD TO CART hands the cart off as
 * one `add-to-cart` action. A retry of the same unchanged cart after a never-started rejection
 * reuses its local `submitId`.
 *
 * Once ADD TO CART is accepted or its outcome is unknown, this menu is read-only for good: a later
 * order needs a newer menu. Whether this is the newest menu is the app's concern (see
 * [FnbOrderStore]), not this runtime's.
 *
 * The draft, submit status and submit id are saved through [saveState] and restored from
 * [savedState]. The in-flight items are saved as submitting before sending, so a recreated card
 * stays locked until the outcome is adopted. Accepted or unconfirmed items leave the draft; a
 * handoff that never started restores it.
 *
 */
class MenuCardRuntime internal constructor(
    val model: MenuUiModel,
    private val actions: CardActionSender,
    savedState: Map<String, Any?>? = null,
    saveState: (Map<String, Any?>) -> Unit = {},
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    companion object {
        const val MESSAGE_BUSY = "Please wait for the current reply, then try again."
        const val MESSAGE_UNAVAILABLE = "Chat isn't available right now."
        const val MESSAGE_FAILED = "Couldn't send your order. Try again."
        const val MESSAGE_ADDED = "Added to your cart."
        const val MESSAGE_UNCONFIRMED = "We couldn't confirm those items were added. Ask me to show your cart."
        const val MESSAGE_READ_ONLY = "This menu is no longer active."

        internal const val STATUS_DRAFT = "draft"
        internal const val STATUS_SUBMITTING = "submitting"
        internal const val STATUS_ADDED = "added"
        internal const val STATUS_UNCONFIRMED = "unconfirmed"

        private const val STATUS = "status"
        private const val CART = "cart"
        private const val SUBMIT_ID = "submitId"
    }

    private val saver = CardStateSaver(savedState, saveState)
    private val menuItems = model.categories.flatMap { it.items }.distinctBy { it.id }.associateBy { it.id }
    private val cartState = MutableStateFlow(Cart())
    private val submitStateFlow = MutableStateFlow<SubmitState>(SubmitState.Idle)
    private var pendingSubmitId: String? = null
    private val selectedCategoryState = MutableStateFlow(0)
    private val customizingItemState = MutableStateFlow<String?>(null)
    private val rowScrollPositions = HashMap<String, RowScroll>()

    val cart: StateFlow<Cart> = cartState.asStateFlow()
    val submitState: StateFlow<SubmitState> = submitStateFlow.asStateFlow()
    val selectedCategory: StateFlow<Int> = selectedCategoryState.asStateFlow()
    val customizingItemId: StateFlow<String?> = customizingItemState.asStateFlow()

    data class RowScroll(val index: Int = 0, val offset: Int = 0)

    init {
        restore(savedState)
    }

    /** False while sending, and for good once an add was accepted or its outcome is unknown. */
    val isEditable: Boolean
        get() = model.orderingAvailable && !isClosed && submitStateFlow.value != SubmitState.Submitting

    /** ADD TO CART was accepted or may have been; this menu takes no more orders. */
    val isClosed: Boolean
        get() = submitStateFlow.value == SubmitState.Added || submitStateFlow.value == SubmitState.Unconfirmed

    /** Adopts [state] when another instance of this card saved it after this one. */
    fun adopt(state: Map<String, Any?>?) {
        if (saver.adopt(state)) restore(state)
    }

    fun selectCategory(index: Int) {
        selectedCategoryState.value = index
    }

    fun openCustomization(itemId: String?) {
        customizingItemState.value = itemId
    }

    fun rowScroll(categoryId: String): RowScroll = rowScrollPositions[categoryId] ?: RowScroll()

    fun saveRowScroll(categoryId: String, scroll: RowScroll) {
        rowScrollPositions[categoryId] = scroll
    }

    fun updateCart(cart: Cart) {
        if (!isEditable || cartState.value == cart) return
        // A changed cart is a new logical order, so it must not reuse the rejected submit's id.
        pendingSubmitId = null
        cartState.value = cart
        // A failure notice stays until the next edit.
        submitStateFlow.value = SubmitState.Idle
        saveDraft()
    }

    /** Hands off the cart as `conciergeAction.addToCart`. Ignored when empty or not editable. */
    fun submit() {
        val menu = model
        val lines = cartState.value.lines
        if (lines.isEmpty() || !isEditable) return

        val action = FnbAction.SubmitCart(
            submitId = pendingSubmitId ?: idFactory().also { pendingSubmitId = it },
            locationId = menu.locationId,
            locationName = menu.locationName,
            lines = lines
        )
        val xdmFields = FnbHandoffFormatter.addToCartContext(action, menuItems)
            ?: run {
                submitStateFlow.value = SubmitState.Failed(MESSAGE_FAILED)
                return
            }
        submitStateFlow.value = SubmitState.Submitting
        // Saved before sending: a recreated card must not offer these items again while they are in flight.
        save(STATUS_SUBMITTING)
        val onResult: (CardActionOutcome) -> Unit = { outcome ->
            when {
                outcome == CardActionOutcome.ACCEPTED -> {
                    finish(SubmitState.Added)
                    save(STATUS_ADDED)
                }
                outcome.neverStarted -> {
                    submitStateFlow.value = SubmitState.Failed(
                        when (outcome) {
                            CardActionOutcome.BUSY -> MESSAGE_BUSY
                            CardActionOutcome.UNAVAILABLE -> MESSAGE_UNAVAILABLE
                            else -> MESSAGE_FAILED
                        }
                    )
                    saveDraft()
                }
                else -> {
                    finish(SubmitState.Unconfirmed)
                    save(STATUS_UNCONFIRMED)
                }
            }
        }
        try {
            actions.send(
                mapOf(
                    "operation" to FnbOperations.ADD_TO_CART,
                    "actionId" to action.submitId,
                    "xdmFields" to xdmFields,
                    "message" to FnbHandoffFormatter.addToCartMessage(action)
                ),
                onResult
            )
        } catch (e: Exception) {
            // A throwing sender may already have submitted the order.
            if (submitStateFlow.value == SubmitState.Submitting) onResult(CardActionOutcome.UNCONFIRMED)
        }
    }

    private fun finish(state: SubmitState) {
        cartState.value = Cart()
        customizingItemState.value = null
        pendingSubmitId = null
        submitStateFlow.value = state
    }

    private fun saveDraft() = save(STATUS_DRAFT)

    private fun save(status: String) {
        saver.save(
            mapOf(
                STATUS to status,
                CART to CartCodec.encodeLines(cartState.value.lines),
                SUBMIT_ID to pendingSubmitId
            )
        )
    }

    private fun restore(state: Map<String, Any?>?) {
        cartState.value = Cart(CartCodec.decodeLines(state?.get(CART)))
        pendingSubmitId = state?.get(SUBMIT_ID) as? String
        submitStateFlow.value = when (state?.get(STATUS)) {
            STATUS_SUBMITTING -> SubmitState.Submitting
            STATUS_ADDED -> SubmitState.Added
            STATUS_UNCONFIRMED -> SubmitState.Unconfirmed
            else -> SubmitState.Idle
        }
    }
}
