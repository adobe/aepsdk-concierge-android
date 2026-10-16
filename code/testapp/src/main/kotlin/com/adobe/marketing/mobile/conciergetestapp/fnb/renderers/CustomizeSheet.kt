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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartCodec
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CartReducer
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.CustomizeLogic
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.FnbText
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.MenuItem
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.OptionChoice
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.OptionGroup
import com.adobe.marketing.mobile.conciergetestapp.fnb.model.SelectedOption

/**
 * Customize modal for one menu item. A [Dialog] rather than a ModalBottomSheet because the chat
 * itself is hosted in a full-screen Dialog, where nested sheets have inset/IME quirks.
 */
@Composable
fun CustomizeDialog(
    item: MenuItem,
    currencyCode: String,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (selected: List<SelectedOption>, quantity: Int, note: String?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val theme = FnbTheme.current
        val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
        Surface(
            color = theme.colors.sheetBackground,
            shape = RoundedCornerShape(theme.dimens.cardCornerRadius),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(max = maxHeight)
        ) {
            CustomizeContent(
                item = item,
                currencyCode = currencyCode,
                enabled = enabled,
                onConfirm = onConfirm,
                onClose = onDismiss
            )
        }
    }
}

/**
 * Pure Customize layout (no window), so the gallery can render resting and validation-error
 * states inline. Emits [onConfirm] only when every group is satisfied; an invalid tap reveals the
 * error state instead.
 *
 * Layout follows the Figma "Customize your order" frame: title + close, image, name, price,
 * description, one section per option group, quantity, and the ADD TO ORDER CTA.
 */
@Composable
fun CustomizeContent(
    item: MenuItem,
    currencyCode: String,
    enabled: Boolean,
    onConfirm: (selected: List<SelectedOption>, quantity: Int, note: String?) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    initialShowErrors: Boolean = false
) {
    val theme = FnbTheme.current
    val initial = remember(item) { CustomizeLogic.initialSelections(item) }
    var encodedSelections by rememberSaveable(item.id) { mutableStateOf(CartCodec.encodeSelections(initial)) }
    val selections = remember(encodedSelections) { CartCodec.decodeSelections(encodedSelections) ?: initial }
    val rule = item.quantity
    val maxQuantity = CartReducer.maxQuantity(item)
    var quantity by rememberSaveable(item.id) { mutableIntStateOf(rule.default.coerceIn(rule.min, maxQuantity)) }
    var note by rememberSaveable(item.id) { mutableStateOf("") }
    var showErrors by rememberSaveable(item.id) { mutableStateOf(initialShowErrors) }

    val unmet = CustomizeLogic.unmetGroupIds(item, selections)
    val isValid = unmet.isEmpty()
    val lineTotal = CustomizeLogic.unitPriceCents(item, selections) * quantity

    Column(modifier.padding(theme.dimens.contentPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Customize your order",
                color = theme.colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (onClose != null) {
                FnbIconButton(glyph = "✕", contentDescription = "Close", onClick = onClose)
            }
        }
        Spacer(Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            FnbImage(
                url = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(theme.dimens.tileImageHeight * 1.8f)
                    .clip(RoundedCornerShape(theme.dimens.tileCornerRadius))
                    .border(1.dp, theme.colors.cardBorder, RoundedCornerShape(theme.dimens.tileCornerRadius))
            )

            ItemSummary(item, currencyCode)

            item.optionGroups.forEach { group ->
                OptionGroupSection(
                    group = group,
                    selected = selections[group.id].orEmpty(),
                    currencyCode = currencyCode,
                    enabled = enabled,
                    showError = showErrors && group.id in unmet,
                    onToggle = { optionId ->
                        val updated = selections + (group.id to CustomizeLogic.toggle(group, selections[group.id].orEmpty(), optionId))
                        encodedSelections = CartCodec.encodeSelections(updated)
                    }
                )
            }

            item.instructions?.let { config ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(config.label)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(config.maxLength) },
                        enabled = enabled,
                        placeholder = { if (config.placeholder.isNotEmpty()) Text(config.placeholder) },
                        supportingText = { Text("${note.length}/${config.maxLength}") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Quantity", Modifier.weight(1f))
                FnbPillStepper(
                    quantity = quantity,
                    itemName = item.name,
                    enabled = enabled,
                    canIncrement = quantity + rule.step <= maxQuantity,
                    canDecrement = quantity - rule.step >= rule.min,
                    onIncrement = { quantity += rule.step },
                    onDecrement = { quantity -= rule.step }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        FnbPrimaryButton(
            text = item.addToCartLabel.uppercase(),
            enabled = enabled && item.available,
            muted = !isValid,
            onClick = {
                if (isValid) {
                    val cleanNote = item.instructions?.let { FnbText.sanitizeInline(note, it.maxLength) }?.ifEmpty { null }
                    onConfirm(CustomizeLogic.toSelectedOptions(item, selections), quantity, cleanNote)
                } else {
                    showErrors = true
                }
            },
            modifier = Modifier.semantics {
                contentDescription = item.addToCartLabel + ", " + FnbFormat.price(lineTotal, currencyCode)
            }
        )
    }
}

@Composable
private fun ItemSummary(item: MenuItem, currencyCode: String) {
    val theme = FnbTheme.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.name,
                color = theme.colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false)
            )
            item.tag?.let { FnbTag(it) }
            if (item.isAlcohol) FnbTag("21+")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                FnbFormat.price(item.priceCents, currencyCode),
                color = theme.colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            item.wasPriceCents?.let {
                Text(
                    FnbFormat.price(it, currencyCode),
                    color = theme.colors.textSecondary,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
        if (item.description.isNotEmpty()) {
            Text(item.description, color = theme.colors.textSecondary, fontSize = 14.sp)
        }
        if (item.metadata.isNotEmpty()) {
            Text(item.metadata.joinToString(" · "), color = theme.colors.textSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val theme = FnbTheme.current
    Text(
        text = text.uppercase(),
        color = color ?: theme.colors.textSecondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}

@Composable
private fun OptionGroupSection(
    group: OptionGroup,
    selected: Set<String>,
    currencyCode: String,
    enabled: Boolean,
    showError: Boolean,
    onToggle: (String) -> Unit
) {
    val theme = FnbTheme.current
    val errorOrNull = if (showError) theme.colors.error else null
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                SectionLabel(group.title, color = errorOrNull)
                Text(selectionHint(group), color = errorOrNull ?: theme.colors.textSecondary, fontSize = 12.sp)
            }
            RequirementPill(required = group.required, error = showError)
        }
        if (showError) {
            Text(
                text = "Please choose ${group.minSelect}",
                color = theme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            group.options.forEach { option ->
                OptionRow(
                    group = group,
                    option = option,
                    isSelected = option.id in selected,
                    atMax = selected.size >= group.maxSelect,
                    currencyCode = currencyCode,
                    enabled = enabled,
                    showError = showError,
                    onToggle = { onToggle(option.id) }
                )
            }
        }
    }
}

@Composable
private fun RequirementPill(required: Boolean, error: Boolean) {
    val theme = FnbTheme.current
    val color = when {
        error -> theme.colors.error
        required -> theme.colors.textPrimary
        else -> theme.colors.textSecondary
    }
    Text(
        text = if (required) "Required" else "Optional",
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .border(1.dp, if (error) theme.colors.error else theme.colors.cardBorder, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun OptionRow(
    group: OptionGroup,
    option: OptionChoice,
    isSelected: Boolean,
    atMax: Boolean,
    currencyCode: String,
    enabled: Boolean,
    showError: Boolean,
    onToggle: () -> Unit
) {
    val theme = FnbTheme.current
    val shape = RoundedCornerShape(theme.dimens.tileCornerRadius)
    val rowEnabled = enabled && option.available && (group.isSingleSelect || isSelected || !atMax)
    val interaction = if (group.isSingleSelect) {
        Modifier.selectable(selected = isSelected, enabled = rowEnabled, role = Role.RadioButton, onClick = onToggle)
    } else {
        Modifier.toggleable(value = isSelected, enabled = rowEnabled, role = Role.Checkbox, onValueChange = { onToggle() })
    }
    val borderColor = when {
        showError -> theme.colors.error
        isSelected -> theme.colors.accent
        else -> theme.colors.cardBorder
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .border(if (isSelected) 2.dp else 1.dp, borderColor, shape)
            .then(interaction)
            .padding(horizontal = 8.dp)
    ) {
        if (group.isSingleSelect) {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = rowEnabled,
                colors = RadioButtonDefaults.colors(selectedColor = theme.colors.accent),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Checkbox(
                checked = isSelected,
                onCheckedChange = null,
                enabled = rowEnabled,
                colors = CheckboxDefaults.colors(checkedColor = theme.colors.accent),
                modifier = Modifier.padding(8.dp)
            )
        }
        Text(
            text = if (option.available) optionLabel(option, currencyCode) else "${option.label} (Unavailable)",
            color = if (rowEnabled) theme.colors.textPrimary else theme.colors.textSecondary,
            fontSize = 15.sp,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        )
    }
}

/** "Large (+$1.50)", "Sharing (+$3)", or just the label when the option is free. */
internal fun optionLabel(option: OptionChoice, currencyCode: String): String =
    if (option.priceDeltaCents > 0) {
        "${option.label} (+${FnbFormat.priceCompact(option.priceDeltaCents, currencyCode)})"
    } else {
        option.label
    }

private fun selectionHint(group: OptionGroup): String = when {
    group.required && group.minSelect == group.maxSelect -> "Choose ${group.minSelect}"
    group.required -> "Choose ${group.minSelect}–${group.maxSelect}"
    else -> "Select up to ${group.maxSelect}"
}
