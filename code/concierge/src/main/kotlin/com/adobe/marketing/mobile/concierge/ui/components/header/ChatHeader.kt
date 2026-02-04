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

package com.adobe.marketing.mobile.concierge.ui.components.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.adobe.marketing.mobile.concierge.ConciergeConstants
import com.adobe.marketing.mobile.concierge.R
import com.adobe.marketing.mobile.concierge.ui.theme.ConciergeStyles

/**
 * Header component for the chat interface with title, subtitle, and close button.
 * @param modifier Modifier for the header
 * @param onClose Callback when the close button is pressed
 */
@Composable
internal fun ChatHeader(
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val style = ConciergeStyles.headerStyle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(style.padding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = ConciergeConstants.ChatHeader.TITLE,
                style = style.titleStyle,
                fontWeight = style.titleFontWeight,
                color = style.titleColor
            )
            Text(
                text = ConciergeConstants.ChatHeader.SUBTITLE,
                style = style.subtitleStyle,
                color = style.subtitleColor
            )
        }

        // Close button
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(style.iconSize)
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = "Close chat",
                tint = style.iconColor
            )
        }
    }
}
