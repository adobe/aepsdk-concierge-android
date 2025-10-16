/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.concierge.ui.components.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles
import com.adobe.marketing.mobile.services.Log

/**
 * Composable that displays action buttons for product recommendations.
 */
@Composable
internal fun ProductActionButtons(
    actionButtons: List<ProductActionButton>,
    onActionClick: (ProductActionButton) -> Unit,
    modifier: Modifier = Modifier
) {
    if (actionButtons.isEmpty()) {
        Log.debug(
            ConciergeConstants.EXTENSION_NAME,
            "ProductActionButtons",
            "No action buttons to display"
        )
        return
    }

    val style = ConciergeStyles.productActionButtonsStyle
    val cardStyle = ConciergeStyles.productCardStyle
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(style.spacing),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = cardStyle.captionBottomPadding)
    ) {
        actionButtons.forEach { button ->
            val isPrimary = actionButtons.indexOf(button) == 0
            ProductActionButton(
                button = button,
                isPrimary = isPrimary,
                onClick = { onActionClick(button) },
                modifier = Modifier.weight(1f)
            )
        }
    }

}

/**
 * Common action button composable that can render as either primary or secondary style.
 */
@Composable
private fun ProductActionButton(
    button: ProductActionButton,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val style = ConciergeStyles.productActionButtonsStyle
    
    if (isPrimary) {
        // Primary button - filled blue button
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = style.primaryBackgroundColor,
                contentColor = style.primaryContentColor
            ),
            shape = style.shape,
            modifier = modifier.height(style.height)
                .width(IntrinsicSize.Min)
        ) {
            ProductButtonText(text = button.text)
        }
    } else {
        // Secondary button - outlined white button
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = style.secondaryBackgroundColor,
                contentColor = style.secondaryContentColor
            ),
            shape = style.shape,
            border = BorderStroke(
                width = style.secondaryBorderWidth,
                color = style.secondaryBorderColor
            ),
            modifier = modifier.height(style.height)
                .width(IntrinsicSize.Min)
        ) {
            ProductButtonText(text = button.text)
        }
    }
}

/**
 * Common text component for product action buttons.
 */
@Composable
private fun ProductButtonText(text: String) {
    val style = ConciergeStyles.productActionButtonsStyle
    
    Text(
        text = text,
        textAlign = style.textAlign,
        fontSize = style.fontSize.value.sp,
        style = style.textStyle,
        fontWeight = style.fontWeight,
        maxLines = style.maxLines,
        overflow = style.overflow
    )
}
