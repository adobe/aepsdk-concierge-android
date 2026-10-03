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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionResult
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryUiModel
import java.util.concurrent.atomic.AtomicBoolean

/** `fnb.cart` renderer entry point: maps the element payload (the tapin2 order) once, then renders [CartSummaryContent]. */
@Composable
fun CartSummaryRenderer(
    context: FnbRenderContext,
    onAction: FnbActionHandler,
    modifier: Modifier = Modifier,
    options: CartOptions = CartOptions()
) {
    val model = remember(context.element, options) {
        CartSummaryMapper.map(context.element.payload, options, entityId = context.element.entityId)
    }
    if (model == null) {
        CartUnavailable(modifier)
    } else {
        CartSummaryContent(context.elementKey, model, context.isInteractive, onAction, modifier)
    }
}

/**
 * Cart view (Figma "Here's your current cart"): venue header, one row per order line with
 * "Remove", server totals, "Proceed to checkout" and "Show more restaurants".
 *
 * The card shows tapin2's order as sent. Removing a line only requests it from BC; the line is
 * marked pending, and the updated order arrives as a new cart element. Older cart elements render
 * with `isInteractive = false`.
 */
@Composable
fun CartSummaryContent(
    elementId: String,
    model: CartSummaryUiModel,
    isInteractive: Boolean,
    onAction: FnbActionHandler,
    modifier: Modifier = Modifier
) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)
    // Comma-joined item ids with a pending removal; survives the element scrolling off-screen.
    var pendingRemovals by rememberSaveable(elementId) { mutableStateOf("") }
    var busy by rememberSaveable(elementId) { mutableStateOf(false) }
    var error by rememberSaveable(elementId) { mutableStateOf<String?>(null) }
    val pending = remember(pendingRemovals) { pendingRemovals.split(',').filter { it.isNotEmpty() }.toSet() }
    val canAct = isInteractive && !busy && !model.isPaid

    fun send(action: FnbAction, onAccepted: () -> Unit = {}) {
        val handled = AtomicBoolean(false)
        busy = true
        error = null
        onAction.onAction(action) { result ->
            if (!handled.compareAndSet(false, true)) return@onAction
            busy = false
            when (result) {
                FnbActionResult.Accepted -> onAccepted()
                is FnbActionResult.Rejected -> error = result.message
            }
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
            .padding(theme.dimens.contentPadding)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FnbStatusDot(online = true)
            Text(
                text = model.venueName.ifEmpty { "Your order" },
                color = theme.colors.accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            model.orderCode?.let {
                Text("Order #$it", color = theme.colors.textSecondary, fontSize = 12.sp)
            }
        }

        if (model.isEmpty) {
            Text("Your cart is empty.", color = theme.colors.textSecondary, fontSize = 14.sp)
        } else {
            model.lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = theme.colors.cardBorder)
                CartLineRow(
                    line = line,
                    currencyCode = model.currencyCode,
                    removing = line.itemId in pending,
                    canRemove = canAct && line.removable && line.itemId !in pending,
                    onRemove = {
                        send(FnbAction.RemoveCartItem(model.orderId, line.itemId, line.title)) {
                            pendingRemovals = (pending + line.itemId).joinToString(",")
                        }
                    }
                )
            }
            HorizontalDivider(color = theme.colors.cardBorder)
            Totals(model)
        }

        error?.let {
            Text(
                it,
                color = theme.colors.error,
                fontSize = 13.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }

        if (model.isPaid) {
            Text(
                "Paid. Your order is being prepared.",
                color = theme.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        } else if (!model.isEmpty && model.checkoutUrl != null) {
            FnbPrimaryButton(
                text = model.checkoutLabel,
                enabled = canAct,
                onClick = { send(FnbAction.Checkout(model.orderId, model.checkoutUrl)) }
            )
        }
        model.showMoreLabel?.let { label ->
            FnbSecondaryButton(
                text = label,
                enabled = isInteractive && !busy,
                onClick = { send(FnbAction.ShowMoreRestaurants(model.orderId)) }
            )
        }
    }
}

@Composable
private fun CartLineRow(
    line: CartSummaryLine,
    currencyCode: String,
    removing: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    val theme = FnbTheme.current
    val textColor = if (removing) theme.colors.textSecondary else theme.colors.textPrimary
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${line.title} × ${line.quantity}",
                color = textColor,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(FnbFormat.price(line.subtotalCents, currencyCode), color = textColor, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = cartLineDetail(line),
                color = theme.colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (removing) {
                Text(
                    "Removing…",
                    color = theme.colors.textSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            } else if (line.removable) {
                FnbLinkButton(
                    text = "Remove",
                    contentDescription = "Remove ${line.title}",
                    enabled = canRemove,
                    onClick = onRemove
                )
            }
        }
    }
}

/** "No modifiers · Market Cafe 122", "Coke · note: light ice · Local Eats 118". */
internal fun cartLineDetail(line: CartSummaryLine): String =
    listOfNotNull(
        line.modifiersSummary.ifEmpty { null },
        line.note?.let { "note: $it" },
        line.locationName.ifEmpty { null }
    ).joinToString(" · ")

@Composable
private fun Totals(model: CartSummaryUiModel) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TotalRow("Subtotal", model.subtotalCents, model.currencyCode)
        if (model.discountCents > 0) TotalRow("Discount", -model.discountCents, model.currencyCode)
        if (model.feesCents > 0) TotalRow("Fees", model.feesCents, model.currencyCode)
        if (model.tipCents > 0) TotalRow("Tip", model.tipCents, model.currencyCode)
        TotalRow("Tax", model.taxCents, model.currencyCode)
        TotalRow("Total", model.totalCents, model.currencyCode, emphasized = true)
    }
}

@Composable
private fun TotalRow(label: String, cents: Long, currencyCode: String, emphasized: Boolean = false) {
    val theme = FnbTheme.current
    val color = if (emphasized) theme.colors.textPrimary else theme.colors.textSecondary
    val weight = if (emphasized) FontWeight.Bold else FontWeight.Normal
    Row {
        Text(label, color = color, fontWeight = weight, fontSize = if (emphasized) 15.sp else 13.sp, modifier = Modifier.weight(1f))
        Text(
            if (cents < 0) "−" + FnbFormat.price(-cents, currencyCode) else FnbFormat.price(cents, currencyCode),
            color = color,
            fontWeight = weight,
            fontSize = if (emphasized) 15.sp else 13.sp
        )
    }
}

@Composable
private fun CartUnavailable(modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
            .padding(theme.dimens.contentPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Text("Cart unavailable", color = theme.colors.textSecondary)
    }
}
