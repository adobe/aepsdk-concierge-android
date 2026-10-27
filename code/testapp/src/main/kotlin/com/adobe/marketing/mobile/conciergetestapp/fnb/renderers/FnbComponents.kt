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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.conciergetestapp.cards.CardImage
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal object FnbFormat {
    fun price(cents: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String =
        formatPrice(cents, currencyCode, locale, compact = false)

    /** Like [price] but drops a zero fraction: "$3", "$1.50". */
    fun priceCompact(cents: Long, currencyCode: String, locale: Locale = Locale.getDefault()): String =
        formatPrice(cents, currencyCode, locale, compact = true)

    private fun formatPrice(cents: Long, currencyCode: String, locale: Locale, compact: Boolean): String {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        if (compact && cents % 100 == 0L) {
            format.minimumFractionDigits = 0
            format.maximumFractionDigits = 0
        }
        return format.format(BigDecimal.valueOf(cents, 2))
    }
}

/** Loads [url] through the host's `LocalImageProvider`; shows a flat placeholder until ready. */
@Composable
internal fun FnbImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    CardImage(url, contentDescription, modifier.background(theme.colors.tileImagePlaceholder))
}

@Composable
internal fun FnbTag(text: String, modifier: Modifier = Modifier) {
    val colors = FnbTheme.current.colors
    Text(
        text = text,
        color = colors.tagText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .background(colors.tagBackground, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
internal fun FnbStatusDot(online: Boolean, modifier: Modifier = Modifier) {
    val colors = FnbTheme.current.colors
    Box(
        modifier
            .size(8.dp)
            .background(if (online) colors.statusOnline else colors.statusOffline, CircleShape)
    )
}

/** Borderless glyph button (e.g. a dialog close "✕") with a 48dp touch target. */
@Composable
internal fun FnbIconButton(glyph: String, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Text(glyph, color = theme.colors.textSecondary, fontSize = 18.sp)
    }
}

/** Quantity stepper inside a bordered pill: `− n +`, minimum 1. */
@Composable
internal fun FnbPillStepper(
    quantity: Int,
    itemName: String,
    enabled: Boolean,
    canIncrement: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    canDecrement: Boolean = quantity > 1
) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.tileCornerRadius)
    @Composable
    fun Glyph(glyph: String, description: String, active: Boolean, onClick: () -> Unit) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable(enabled = active, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
        ) {
            Text(
                glyph,
                color = if (active) theme.colors.textPrimary else theme.colors.disabled,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, theme.colors.cardBorder, shape)
            .padding(horizontal = 8.dp)
    ) {
        Glyph("−", "Remove one $itemName", enabled && canDecrement, onDecrement)
        Text(
            text = quantity.toString(),
            color = theme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .semantics { contentDescription = "Quantity $quantity" }
        )
        Glyph("+", "Add one $itemName", enabled && canIncrement, onIncrement)
    }
}

/**
 * The menu tile's quantity control, in a compact chip with its own surface, border, and shadow so
 * the count stays readable over any photo: a round `+` when [quantity] is 0, otherwise a `− n +`
 * pill. The buttons are drawn at [FnbDimens.stepperSize]; Compose still extends their touch
 * targets to the 48dp minimum.
 */
@Composable
internal fun FnbTileStepper(
    quantity: Int,
    itemName: String,
    enabled: Boolean,
    canIncrement: Boolean,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = FnbTheme.current
    val size = theme.dimens.stepperSize
    val shape = RoundedCornerShape(percent = 50)

    @Composable
    fun Glyph(glyph: String, description: String, active: Boolean, onClick: () -> Unit) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .clickable(enabled = active, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = description }
        ) {
            Text(
                glyph,
                color = if (active) theme.colors.stepperContent else theme.colors.disabled,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .shadow(2.dp, shape)
            .background(theme.colors.stepperBackground, shape)
            .border(1.dp, theme.colors.cardBorder, shape)
    ) {
        if (quantity > 0) {
            Glyph("−", "Remove one $itemName", enabled, onDecrement)
            Text(
                text = quantity.toString(),
                color = theme.colors.textPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(min = 16.dp)
                    .semantics { contentDescription = "$quantity $itemName" }
            )
        }
        Glyph("+", "Add one $itemName", enabled && canIncrement, onIncrement)
    }
}

@Composable
internal fun FnbPrimaryButton(
    text: String,
    enabled: Boolean,
    muted: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = FnbTheme.current
    val active = enabled && !muted
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(theme.dimens.buttonHeight.coerceAtLeast(48.dp))
            .clip(RoundedCornerShape(theme.dimens.buttonCornerRadius))
            .background(if (active) theme.colors.accent else theme.colors.disabled)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Text(text, color = theme.colors.onAccent, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

/** Outlined companion to [FnbPrimaryButton] (e.g. "Show restaurants"). */
@Composable
internal fun FnbSecondaryButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.buttonCornerRadius)
    val color = if (enabled) theme.colors.accent else theme.colors.disabled
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(theme.dimens.buttonHeight.coerceAtLeast(48.dp))
            .clip(shape)
            .border(1.dp, color, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold)
    }
}

/** Small accent text action with a 48dp touch target (e.g. a cart line's "Remove"). */
@Composable
internal fun FnbLinkButton(text: String, contentDescription: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Text(
            text,
            color = if (enabled) theme.colors.accent else theme.colors.disabled,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
