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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.adobe.marketing.mobile.conciergetestapp.cards.CardGate
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryLine
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartSummaryUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.CartCardRuntime

/**
 * Cart view (Figma "Here's your current cart"): venue header, one row per order line with
 * "Remove", server totals, "Proceed to checkout" and "Show restaurants".
 *
 * The card shows tapin2's order as sent. Removing a line only asks the concierge for it: the line
 * is marked pending, and the updated order arrives as a new cart card. Requests live in [runtime].
 * [gate] disables the handoff buttons while a turn runs, and checkout too once the card is stale.
 * Once checkout opened for this order ([orderClosed]), only checkout can be reopened.
 */
@Composable
fun CartSummaryContent(
    runtime: CartCardRuntime,
    gate: CardGate = CardGate.OPEN,
    orderClosed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val model = runtime.model
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)
    val pending by runtime.pendingRemovals.collectAsState()
    val unconfirmed by runtime.unconfirmed.collectAsState()
    val requestInFlight by runtime.requestInFlight.collectAsState()
    val error by runtime.error.collectAsState()
    val canContinue = !requestInFlight && gate.canSend && !orderClosed
    val canAct = canContinue && !model.isPaid

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
                    canRemove = canAct && line.removable && line.itemId !in pending &&
                        CartCardRuntime.removalKey(model.orderId, line.itemId) !in unconfirmed,
                    onRemove = { runtime.remove(line) }
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
            // Opening checkout proves neither payment nor fulfillment, so the status stays pending.
            if (orderClosed) {
                Text(
                    "Order submitted. Status pending.",
                    color = theme.colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            // Checkout needs no concierge turn, but shares the runtime's one-request-at-a-time gate.
            FnbPrimaryButton(
                text = model.checkoutLabel,
                enabled = !requestInFlight && gate.canEdit,
                onClick = { runtime.checkout() }
            )
        }
        model.showRestaurantsLabel?.let { label ->
            FnbSecondaryButton(
                text = label,
                enabled = canContinue && !model.isEmpty && CartCardRuntime.showRestaurantsKey(model.orderId) !in unconfirmed,
                onClick = runtime::showRestaurants
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
