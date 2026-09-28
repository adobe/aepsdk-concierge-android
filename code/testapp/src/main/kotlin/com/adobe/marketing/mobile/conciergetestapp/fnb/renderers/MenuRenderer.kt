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
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbAction
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionHandler
import com.adobe.marketing.mobile.conciergetestapp.fnb.action.FnbActionResult
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartCodec
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuCategory
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CatalogMenuMapper
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuOptions
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

/**
 * Renderer entry point: maps the message's `catalogItemCard` + `cartBar` elements once, then
 * renders [MenuContent].
 */
@Composable
fun MenuRenderer(
    context: FnbRenderContext,
    onAction: FnbActionHandler,
    modifier: Modifier = Modifier,
    options: MenuOptions = MenuOptions()
) {
    val model = remember(context.elements, options) { CatalogMenuMapper.map(context.elements.map { it.asMap() }, options) }
    if (model == null) {
        MenuUnavailable(modifier)
    } else {
        MenuContent(
            elementId = context.groupKey,
            model = model,
            isInteractive = context.isInteractive,
            onAction = onAction,
            modifier = modifier
        )
    }
}

internal sealed interface SubmitState {
    object Idle : SubmitState
    object Submitting : SubmitState
    object Sent : SubmitState
    data class Failed(val message: String) : SubmitState
}

private val SubmitStateSaver = Saver<SubmitState, String>(
    save = { state ->
        when (state) {
            SubmitState.Sent -> "sent"
            is SubmitState.Failed -> "failed:" + state.message
            // The send callback can't survive process death, so an in-flight submit restores idle.
            else -> "idle"
        }
    },
    restore = { saved ->
        when {
            saved == "sent" -> SubmitState.Sent
            saved.startsWith("failed:") -> SubmitState.Failed(saved.removePrefix("failed:"))
            else -> SubmitState.Idle
        }
    }
)

@Immutable
private data class MenuLayout(val headerIndices: List<Int>, val itemsById: Map<String, MenuItem>)

/**
 * The menu card: a bounded-height inline card (a nested unbounded LazyColumn would crash inside
 * the chat's LazyColumn) with its own scroll and ADD TO CART pinned at the bottom. Cart and
 * submit state are saveable and keyed by [elementId], so they survive the message scrolling out
 * of the transcript.
 *
 * @param height overrides the theme's screen-height fraction.
 */
@Composable
fun MenuContent(
    elementId: String,
    model: MenuUiModel,
    isInteractive: Boolean,
    onAction: FnbActionHandler,
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)
    val cardHeight = height ?: (LocalConfiguration.current.screenHeightDp * theme.dimens.cardHeightFraction).dp

    var encodedCart by rememberSaveable(elementId) { mutableStateOf("") }
    val cart = remember(encodedCart) { CartCodec.decode(encodedCart) }
    var submitState by rememberSaveable(elementId, stateSaver = SubmitStateSaver) { mutableStateOf<SubmitState>(SubmitState.Idle) }
    var customizeItemId by rememberSaveable(elementId) { mutableStateOf<String?>(null) }

    fun updateCart(updated: Cart) {
        encodedCart = CartCodec.encode(updated)
        if (submitState is SubmitState.Failed || submitState == SubmitState.Sent) submitState = SubmitState.Idle
    }

    // The menu stays usable after a submit: the fan can keep adding to the same order.
    val canEdit = isInteractive &&
        model.orderingAvailable &&
        submitState != SubmitState.Submitting

    val layout = remember(model) {
        var index = 0
        val headers = model.categories.map { category ->
            val header = index
            index += 1 + (category.items.size + 1) / 2
            header
        }
        MenuLayout(headers, model.categories.flatMap { it.items }.distinctBy { it.id }.associateBy { it.id })
    }
    val listState = rememberLazyListState()
    val tabsState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    // A tapped tab stays selected until the user drags the list: short trailing categories can't
    // scroll their header to the top, so scroll position alone can't select them.
    var tappedCategory by remember(layout) { mutableStateOf<Int?>(null) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { if (it is DragInteraction.Start) tappedCategory = null }
    }
    val scrolledCategory by remember(layout) {
        derivedStateOf {
            activeCategoryIndex(
                headerIndices = layout.headerIndices,
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                visibleItemIndices = listState.layoutInfo.visibleItemsInfo.map { it.index },
                atEnd = !listState.canScrollForward && listState.canScrollBackward
            )
        }
    }
    val activeCategory = tappedCategory ?: scrolledCategory
    LaunchedEffect(activeCategory) {
        if (model.categories.isNotEmpty()) tabsState.animateScrollToItem(activeCategory)
    }

    Column(
        modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
    ) {
        LocationHeader(model)

        LazyRow(
            state = tabsState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = theme.dimens.contentPadding),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            itemsIndexed(model.categories, key = { _, category -> category.id }) { index, category ->
                CategoryTab(
                    label = category.label,
                    selected = index == activeCategory,
                    onClick = {
                        tappedCategory = index
                        scope.launch { listState.animateScrollToItem(layout.headerIndices[index]) }
                    }
                )
            }
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(theme.dimens.gridSpacing),
            contentPadding = PaddingValues(theme.dimens.contentPadding),
            modifier = Modifier.weight(1f)
        ) {
            model.categories.forEach { category ->
                categorySection(category) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(theme.dimens.gridSpacing)) {
                        row.forEach { item ->
                            MenuTile(
                                item = item,
                                quantity = cart.quantityFor(item.id),
                                currencyCode = model.currencyCode,
                                enabled = canEdit,
                                onOpen = { customizeItemId = item.id },
                                onIncrement = {
                                    CartReducer.quickAdd(cart, item)?.let(::updateCart) ?: run { customizeItemId = item.id }
                                },
                                onDecrement = { updateCart(CartReducer.decrement(cart, item)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Column(Modifier.padding(theme.dimens.contentPadding)) {
            (submitState as? SubmitState.Failed)?.let { failed ->
                Text(
                    text = failed.message,
                    color = theme.colors.error,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            if (submitState == SubmitState.Sent) {
                Text(
                    text = "✓ Added to your order. Keep shopping or ask to check out.",
                    color = theme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            val cartBar = model.cartBar
            if (!(cartBar.hideWhenEmpty && cart.isEmpty && submitState != SubmitState.Submitting)) FnbPrimaryButton(
                text = when {
                    submitState == SubmitState.Submitting -> "SENDING…"
                    cartBar.showCount -> "${cartBar.label.uppercase()} (${cart.totalQuantity})"
                    else -> cartBar.label.uppercase()
                },
                enabled = canEdit && cartBar.enabled && !cart.isEmpty,
                onClick = {
                    val action = FnbAction.SubmitCart(
                        submitId = UUID.randomUUID().toString(),
                        venueId = model.venueId,
                        eventId = model.eventId,
                        locationId = model.locationId,
                        locationName = model.locationName,
                        lines = cart.lines
                    )
                    val handled = AtomicBoolean(false)
                    submitState = SubmitState.Submitting
                    onAction.onAction(action) { result ->
                        if (!handled.compareAndSet(false, true)) return@onAction
                        submitState = when (result) {
                            FnbActionResult.Accepted -> {
                                encodedCart = ""
                                SubmitState.Sent
                            }
                            is FnbActionResult.Rejected -> SubmitState.Failed(result.message)
                        }
                    }
                }
            )
        }
    }

    customizeItemId?.let { layout.itemsById[it] }?.let { item ->
        CustomizeDialog(
            item = item,
            currencyCode = model.currencyCode,
            enabled = canEdit,
            onDismiss = { customizeItemId = null },
            onConfirm = { selected, quantity, note ->
                updateCart(CartReducer.add(cart, item, selected, quantity, note))
                customizeItemId = null
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categorySection(
    category: MenuCategory,
    rowContent: @Composable (List<MenuItem>) -> Unit
) {
    item(key = "h:${category.id}", contentType = "header") {
        val theme = FnbTheme.current
        Text(
            text = category.label,
            color = theme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
    }
    category.items.chunked(2).forEach { row ->
        item(key = "r:${category.id}:${row.first().id}", contentType = "row") { rowContent(row) }
    }
}

/**
 * Category highlighted for the current scroll position: the last category whose header is at or
 * above the first visible item, except at the end of the list, where the last category with a
 * visible header wins (otherwise short trailing categories could never be highlighted).
 */
internal fun activeCategoryIndex(
    headerIndices: List<Int>,
    firstVisibleItemIndex: Int,
    visibleItemIndices: List<Int>,
    atEnd: Boolean
): Int {
    if (headerIndices.isEmpty()) return 0
    if (atEnd) {
        val lastVisibleHeader = headerIndices.indexOfLast { it in visibleItemIndices }
        if (lastVisibleHeader >= 0) return lastVisibleHeader
    }
    return headerIndices.indexOfLast { it <= firstVisibleItemIndex }.coerceAtLeast(0)
}

@Composable
private fun LocationHeader(model: MenuUiModel) {
    val theme = FnbTheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(theme.dimens.contentPadding)
    ) {
        FnbStatusDot(online = model.orderingAvailable)
        Text(
            text = model.locationName.ifEmpty { "Menu" },
            color = theme.colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (!model.orderingAvailable) {
            Text("Ordering unavailable", color = theme.colors.textSecondary, fontSize = 12.sp)
        } else {
            model.waitTime?.let { Text(it, color = theme.colors.textSecondary, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun CategoryTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.tabCornerRadius)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) theme.colors.accent else theme.colors.tabBackground)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) theme.colors.onAccent else theme.colors.tabText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MenuTile(
    item: MenuItem,
    quantity: Int,
    currencyCode: String,
    enabled: Boolean,
    onOpen: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val theme = FnbTheme.current
    val soldOut = !item.available
    Column(
        modifier
            .clip(RoundedCornerShape(theme.dimens.tileCornerRadius))
            .background(theme.colors.tileBackground)
            .clickable(enabled = enabled && !soldOut, onClickLabel = "Customize ${item.name}", onClick = onOpen)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(theme.dimens.tileImageHeight)
        ) {
            FnbImage(url = item.imageUrl, contentDescription = null, modifier = Modifier.matchParentSize())
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(6.dp)) {
                if (soldOut) FnbTag("Sold out")
                item.tag?.let { FnbTag(it) }
                if (item.isAlcohol) FnbTag("21+")
            }
            if (!soldOut || quantity > 0) FnbStepper(
                quantity = quantity,
                itemName = item.name,
                enabled = enabled,
                canIncrement = !soldOut && quantity < CartReducer.maxQuantity(item),
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            )
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.name,
                color = if (soldOut) theme.colors.textSecondary else theme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (item.metadata.isNotEmpty()) {
                Text(
                    text = item.metadata.joinToString(" · "),
                    color = theme.colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(FnbFormat.price(item.priceCents, currencyCode), color = theme.colors.textPrimary, fontSize = 14.sp)
                item.wasPriceCents?.let {
                    Text(
                        FnbFormat.price(it, currencyCode),
                        color = theme.colors.textSecondary,
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuUnavailable(modifier: Modifier = Modifier) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)
    Text(
        text = "Menu unavailable",
        color = theme.colors.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
            .padding(theme.dimens.contentPadding)
    )
}
