/*
 * Copyright 2025 Adobe, Inc. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.components.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.LineHeightStyle
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeTheme

/**
 * A text input field for chat messages with support for different states.
 *
 * Uses [BasicTextField] rather than Material's `OutlinedTextField` so the field imposes neither a
 * 56dp minimum height nor the ~16dp internal content padding that the outlined field bakes in --
 * both of which fought the input-bar spec (a 56dp pill with `padding: 16px 12px` around a 24dp
 * row). The field now contributes only the text's own height, letting the pill's padding and the
 * 24dp action icons drive its size, and its text starts flush so the leading gap is exactly the
 * spec's 4dp (or the pill's 12dp edge padding when no leading icon is shown).
 *
 * @param modifier Modifier for the composable
 * @param value The current text value
 * @param onValueChange Callback when the text value changes
 * @param isEnabled Whether the field is enabled
 * @param placeholder Default placeholder text
 */
@Composable
internal fun ChatTextField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    isEnabled: Boolean,
    placeholder: String = "Type a message..."
) {
    val style = ConciergeStyles.chatTextFieldStyle
    val focusManager = LocalFocusManager.current
    val disableMultiline = ConciergeTheme.behavior?.disableMultiline ?: true

    // Strip the font's built-in vertical padding and center the text within its line box, so the
    // glyphs sit on the row's shared center line (aligned with the leading icon and action buttons)
    // rather than riding low on the baseline. Applied to both the input text and the placeholder.
    val centeredTextStyle = remember(style.textStyle) {
        style.textStyle.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both
            )
        )
    }
    val effectiveTextStyle = remember(centeredTextStyle, isEnabled, style.disabledAlpha) {
        if (isEnabled) {
            centeredTextStyle
        } else {
            centeredTextStyle.copy(color = centeredTextStyle.color.copy(alpha = style.disabledAlpha))
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .testTag("ChatTextField")
            .animateContentSize(),
        enabled = isEnabled,
        singleLine = disableMultiline,
        maxLines = if (disableMultiline) 1 else style.maxLines,
        textStyle = effectiveTextStyle,
        cursorBrush = SolidColor(effectiveTextStyle.color),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus()
            }
        ),
        decorationBox = { innerTextField ->
            // No content padding -- the pill's padding and the leading-icon/action-button gaps in
            // ChatInputPanel supply all the spacing the spec calls for.
            Box(contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = style.placeholderTextColor,
                        style = centeredTextStyle
                    )
                }
                innerTextField()
            }
        }
    )
}
