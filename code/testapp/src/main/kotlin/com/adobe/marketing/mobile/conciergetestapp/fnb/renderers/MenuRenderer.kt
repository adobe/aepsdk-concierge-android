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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.conciergetestapp.cards.CardGate
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.Cart
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartBarConfig
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuCategory
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuUiModel
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.MenuCardRuntime
import com.adobe.marketing.mobile.conciergetestapp.fnb.cards.SubmitState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/**
 * The `kings.menu` card. It has no vertical scroll of its own: the chat transcript is the only
 * vertical scroll container, so the card is as tall as its content. Each category is one row with
 * a horizontal carousel of items; the tab row at the top scrolls the chat to a category's row.
 * ADD TO CART sits at the bottom and locks the menu while its items are being sent. Once they were
 * added (or may have been), or when [active] is false because a newer menu or a checkout replaced
 * it, the menu is read-only and says so above the button.
 *
 * The cart and submit state live in [runtime]; ADD TO CART sends one `add-to-cart` action. [gate]
 * disables ADD TO CART while a turn runs, and every control once the card is stale.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuContent(
    runtime: MenuCardRuntime,
    gate: CardGate = CardGate.OPEN,
    active: Boolean = true,
    modifier: Modifier = Modifier
) {
    val model = runtime.model
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.cardCornerRadius)

    // Display only: edits read the runtime's current cart, so taps between frames aren't lost.
    val cart by runtime.cart.collectAsState()
    val submitState by runtime.submitState.collectAsState()
    val customizeItemId by runtime.customizingItemId.collectAsState()
    val selectedCategory by runtime.selectedCategory.collectAsState()

    val readOnly = !active || submitState == SubmitState.Added || submitState == SubmitState.Unconfirmed
    val canEdit = runtime.isEditable && gate.canEdit && active

    val itemsById = remember(model) { model.categories.flatMap { it.items }.distinctBy { it.id }.associateBy { it.id } }
    val rowRequesters = remember(model) { model.categories.map { BringIntoViewRequester() } }
    val scope = rememberCoroutineScope()
    // Taller than the screen, so the chat aligns the row's top with its own top instead of
    // scrolling just far enough to reveal the row.
    val jumpRect = with(LocalDensity.current) {
        Rect(Offset.Zero, Size(1f, LocalConfiguration.current.screenHeightDp.dp.toPx() * 2))
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(theme.colors.cardBackground)
            .border(1.dp, theme.colors.cardBorder, shape)
    ) {
        LocationHeader(model)

        if (model.categories.size > 1) {
            CategoryTabs(model.categories, selectedCategory) { index ->
                runtime.selectCategory(index)
                scope.launch { rowRequesters[index].bringIntoView(jumpRect) }
            }
        }

        model.categories.forEachIndexed { index, category ->
            val reserveMetadataLine = category.items.any { it.metadata.isNotEmpty() }
            key(category.id) {
                CategoryRow(
                    category = category,
                    initialScroll = runtime.rowScroll(category.id),
                    onScrolled = { runtime.saveRowScroll(category.id, it) },
                    modifier = Modifier
                        .bringIntoViewRequester(rowRequesters[index])
                        .padding(top = theme.dimens.gridSpacing / 2, bottom = theme.dimens.gridSpacing / 2)
                ) { item ->
                    MenuTile(
                        item = item,
                        quantity = cart.quantityFor(item.id),
                        currencyCode = model.currencyCode,
                        enabled = canEdit,
                        reserveMetadataLine = reserveMetadataLine,
                        actions = TileActions(
                            onOpen = { runtime.openCustomization(item.id) },
                            onIncrement = {
                                CartReducer.quickAdd(runtime.cart.value, item)?.let(runtime::updateCart)
                                    ?: runtime.openCustomization(item.id)
                            },
                            onDecrement = { runtime.updateCart(CartReducer.decrement(runtime.cart.value, item)) }
                        ),
                        modifier = Modifier.width(theme.dimens.tileWidth)
                    )
                }
            }
        }

        MenuFooter(
            cartBar = model.cartBar,
            cart = cart,
            submitState = submitState,
            readOnly = readOnly,
            sendEnabled = canEdit && gate.canSend,
            onSubmit = runtime::submit
        )
    }

    customizeItemId?.let { itemsById[it] }?.let { item ->
        CustomizeDialog(
            item = item,
            currencyCode = model.currencyCode,
            enabled = canEdit,
            onDismiss = { runtime.openCustomization(null) },
            onConfirm = { selected, quantity, note ->
                runtime.updateCart(CartReducer.add(runtime.cart.value, item, selected, quantity, note))
                runtime.openCustomization(null)
            }
        )
    }
}

@Composable
private fun CategoryTabs(categories: List<MenuCategory>, selected: Int, onSelect: (Int) -> Unit) {
    val theme = FnbTheme.current
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = theme.dimens.contentPadding),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        itemsIndexed(categories, key = { _, category -> category.id }) { index, category ->
            CategoryTab(label = category.label, selected = index == selected, onClick = { onSelect(index) })
        }
    }
}

/** Submit outcome, the read-only notice, and the ADD TO CART button. */
@Composable
private fun MenuFooter(
    cartBar: CartBarConfig,
    cart: Cart,
    submitState: SubmitState,
    readOnly: Boolean,
    sendEnabled: Boolean,
    onSubmit: () -> Unit
) {
    val theme = FnbTheme.current
    Column(Modifier.padding(theme.dimens.contentPadding)) {
        submitNotice(submitState)?.let { (text, color) ->
            Text(
                text = text,
                color = color,
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        if (readOnly) {
            Text(
                text = MenuCardRuntime.MESSAGE_READ_ONLY,
                color = theme.colors.textSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        val submitting = submitState == SubmitState.Submitting
        if (!(cartBar.hideWhenEmpty && cart.isEmpty && !submitting)) FnbPrimaryButton(
            text = when {
                submitting -> "SENDING…"
                cartBar.showCount -> "${cartBar.label.uppercase()} (${cart.totalQuantity})"
                else -> cartBar.label.uppercase()
            },
            enabled = sendEnabled && cartBar.enabled && !cart.isEmpty,
            onClick = onSubmit
        )
    }
}

@Composable
private fun submitNotice(state: SubmitState): Pair<String, Color>? {
    val colors = FnbTheme.current.colors
    return when (state) {
        is SubmitState.Failed -> state.message to colors.error
        SubmitState.Added -> MenuCardRuntime.MESSAGE_ADDED to colors.textPrimary
        SubmitState.Unconfirmed -> MenuCardRuntime.MESSAGE_UNCONFIRMED to colors.textPrimary
        else -> null
    }
}

/** A menu tile's tap targets: open customization, and the quantity stepper. */
private class TileActions(val onOpen: () -> Unit, val onIncrement: () -> Unit, val onDecrement: () -> Unit)

/**
 * One category: its name, then its items as a horizontal carousel. The carousel only takes
 * horizontal drags, so vertical swipes over it still scroll the chat. It starts at
 * [initialScroll] and reports where it settles, so the position outlives this composition.
 */
@Composable
private fun CategoryRow(
    category: MenuCategory,
    initialScroll: MenuCardRuntime.RowScroll,
    onScrolled: (MenuCardRuntime.RowScroll) -> Unit,
    modifier: Modifier = Modifier,
    tile: @Composable (MenuItem) -> Unit
) {
    val theme = FnbTheme.current
    val listState = rememberLazyListState(initialScroll.index, initialScroll.offset)
    val currentOnScrolled by rememberUpdatedState(onScrolled)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { !it }
            .collect { currentOnScrolled(MenuCardRuntime.RowScroll(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = category.label,
            color = theme.colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(horizontal = theme.dimens.contentPadding)
                .semantics { heading() }
        )
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(theme.dimens.gridSpacing),
            contentPadding = PaddingValues(horizontal = theme.dimens.contentPadding)
        ) {
            items(category.items, key = { it.id }, contentType = { "tile" }) { item -> tile(item) }
        }
    }
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
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
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

/**
 * One item in a category carousel: a square photo with the quantity chip in its corner, then the
 * name (two lines reserved so tiles line up), optional metadata, and the price.
 *
 * @param reserveMetadataLine keeps a metadata line even when this item has none, so tiles in a
 * category where some items have metadata still line up.
 */
@Composable
private fun MenuTile(
    item: MenuItem,
    quantity: Int,
    currencyCode: String,
    enabled: Boolean,
    reserveMetadataLine: Boolean,
    actions: TileActions,
    modifier: Modifier = Modifier
) {
    val theme = FnbTheme.current
    val soldOut = !item.available
    Column(
        modifier
            .clip(RoundedCornerShape(theme.dimens.tileCornerRadius))
            .background(theme.colors.tileBackground)
            .clickable(enabled = enabled && !soldOut, onClickLabel = "Customize ${item.name}", onClick = actions.onOpen)
    ) {
        TileImage(item, quantity, enabled, actions)
        TileDetails(item, currencyCode, reserveMetadataLine)
    }
}

/** The square photo with its tags and, unless sold out with none in the cart, the stepper. */
@Composable
private fun TileImage(item: MenuItem, quantity: Int, enabled: Boolean, actions: TileActions) {
    val soldOut = !item.available
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        FnbImage(url = item.imageUrl, contentDescription = null, modifier = Modifier.matchParentSize())
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(6.dp)) {
            if (soldOut) FnbTag("Sold out")
            item.tag?.let { FnbTag(it) }
            if (item.isAlcohol) FnbTag("21+")
        }
        if (!soldOut || quantity > 0) FnbTileStepper(
            quantity = quantity,
            itemName = item.name,
            enabled = enabled,
            canIncrement = !soldOut && quantity < CartReducer.maxQuantity(item),
            onIncrement = actions.onIncrement,
            onDecrement = actions.onDecrement,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
        )
    }
}

/** Name (two lines reserved so tiles line up), optional metadata, and the price. */
@Composable
private fun TileDetails(item: MenuItem, currencyCode: String, reserveMetadataLine: Boolean) {
    val theme = FnbTheme.current
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 8.dp)
    ) {
        Text(
            text = item.name,
            color = if (item.available) theme.colors.textPrimary else theme.colors.textSecondary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (reserveMetadataLine) {
            Text(
                text = item.metadata.joinToString(" · "),
                color = theme.colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                FnbFormat.price(item.priceCents, currencyCode),
                color = theme.colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
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
